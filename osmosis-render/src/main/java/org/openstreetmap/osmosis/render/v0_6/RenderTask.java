// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.render.v0_6;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.lang3.tuple.Pair;
import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class RenderTask implements Sink {

    private final File outputFile;
    private final ExtractBoundariesEntityProcessor ep = new ExtractBoundariesEntityProcessor();

    private final Map<Long, Double> relationsWeight = new HashMap<>(5000);
    private final Map<Long, List<Long>> relations = new HashMap<>(5000);
    private final Map<Long, List<Long>> ways = new HashMap<>(50000);
    private final Map<Long, Node> nodes = new HashMap<>(500000);

    private final Document document;
    private final int outputSizeX;
    private final int outputSizeY;

    public RenderTask(File outputFile, int outputSizeX, int outputSizeY) {
        this.outputFile = outputFile;
        this.outputSizeX = outputSizeX;
        this.outputSizeY = outputSizeY;

        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        this.document = domImpl.createDocument(svgNS, "svg", null);
    }

    @Override
    public void process(EntityContainer entityContainer) {
        entityContainer.process(ep);
    }

    @Override
    public void initialize(Map<String, Object> metaData) {
    }
    
    @Override
    public void complete() {
        List<Pair<Double, List<Pair<Double, Double>>>> paths = new ArrayList<>(relations.size() * 2);
        relations.entrySet().forEach(r -> {
                	List<List<Long>> cways = r.getValue().stream() // relation to list of ways
                		.map(ways::get)
                		.filter(w -> w != null)
                		.collect(Collectors.toList());
                	Double weight = relationsWeight.get(r.getKey());
        			weight = weight == null ? 0d : weight;

        			List<Long> orderedNodes = new ArrayList<>();
                	for(int i = 0; i < cways.size(); i++) {
                		List<Long> newPath = null;
                		List<Long> current = cways.get(i);
                		if(current.size() == 0) continue;
                		
                		if(orderedNodes.size() > 0)  {
                			long currentStart = current.get(0);
                			long currentEnd = current.get(current.size() - 1);
                			
                			long fullStart = orderedNodes.get(0);
                			long fullEnd = orderedNodes.get(orderedNodes.size() - 1);
                			                			
                			if(fullEnd == currentStart) {
                				// nothing to do, matches perfectly
                			} else if(fullEnd == currentEnd) {
                				// need to turn the new segment
                				Collections.reverse(current);
                			} else if(fullStart == currentStart) {
                				// need to turn the old segment
                				Collections.reverse(orderedNodes);
                			} else if(fullStart == currentEnd) {
                				// need to reverse both
                				Collections.reverse(orderedNodes);
                				Collections.reverse(current);
                			} else {
                				newPath = current;
                				current = Collections.emptyList();
                			}
                		}
                		
                		orderedNodes.addAll(current);

                		if(newPath != null || i == cways.size() -1 ) {
                			paths.add(Pair.of(weight, orderedNodes.stream()
                				.map(nid -> nodes.get(nid))
                				.filter(n -> n != null)
                				.map(RenderTask::getMeters)
                				.collect(Collectors.toList())));
                			
                			orderedNodes = new ArrayList<>(newPath != null ? newPath : Collections.emptyList());;
                		}
                	}
                });
        
        // I looked for solutions doing this only without intermediate lists,
        // but, seriously, they all suck. As long as there's enough RAM, I don't care.

        double maxX = paths.stream()
                .flatMap(p -> p.getRight().stream())
                .map(p -> p.getLeft())
                .collect(Collectors.maxBy(Comparator.naturalOrder()))
                .get();
        double minX = paths.stream()
        		.flatMap(p -> p.getRight().stream())
                .map(p -> p.getLeft())
                .collect(Collectors.minBy(Comparator.naturalOrder()))
                .get();

        double maxY = paths.stream()
        		.flatMap(p -> p.getRight().stream())
                .map(p -> p.getRight())
                .collect(Collectors.maxBy(Comparator.naturalOrder()))
                .get();
        double minY = paths.stream()
        		.flatMap(p -> p.getRight().stream())
                .map(p -> p.getRight())
                .collect(Collectors.minBy(Comparator.naturalOrder()))
                .get();
        
        double maxWeight = relationsWeight.values().stream()
        		.collect(Collectors.maxBy(Comparator.naturalOrder()))
        		.get();

        double centerX = (maxX + minX) / 2;
        double centerY = (maxY + minY) / 2;

        double scaleX = outputSizeX / (maxX - minX);
        double scaleY = outputSizeY / (maxY - minY);
        double scale = Math.min(scaleX, scaleY);

        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setSVGCanvasSize(new Dimension(outputSizeX, outputSizeY));
        
        Color baseColor = new Color(239, 239, 239);
        Color highlightColor = new Color(23,  62, 125);

        svgGenerator.translate(Math.floor(outputSizeX / 2f), Math.floor(outputSizeY / 2f));

        paths.forEach(path -> {
            Path2D.Double drawPath = new Path2D.Double(); 
            path.getRight().stream()
                    .map(p -> Pair.of((p.getLeft() - centerX) * scale, (p.getRight() - centerY) * scale))
                    .peek(p -> {
                        if (drawPath.getCurrentPoint() == null) {
                            drawPath.moveTo(p.getLeft(), p.getRight());
                        }
                    })
                    .skip(1)
                    .forEach(p -> {
                        drawPath.lineTo(p.getLeft(), p.getRight());
                    });
           svgGenerator.setPaint(blend(baseColor, highlightColor, path.getLeft() / maxWeight));
           svgGenerator.fill(drawPath);
        });

        try ( FileOutputStream fos = new FileOutputStream(outputFile)) {
            svgGenerator.stream(new OutputStreamWriter(fos, Charset.forName("UTF-8")));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void close() {
    }

    private class ExtractBoundariesEntityProcessor implements EntityProcessor {

        @Override
        public void process(BoundContainer bc) {
        }

        @Override
        public void process(NodeContainer nc) {
            Node n = nc.getEntity();
            nodes.put(n.getId(), n);
        }

        @Override
        public void process(WayContainer wc) {
            Way w = wc.getEntity();
            ways.put(w.getId(), w.getWayNodes().stream().map(wp -> wp.getNodeId()).collect(Collectors.toList()));
        }

        @Override
        public void process(RelationContainer rc) {
            Relation r = rc.getEntity();
            relations.put(r.getId(), new ArrayList<>(50));
            double weight = relationsWeight.getOrDefault(r.getId(), 0d);
            try {
            	String weightString = r.getTags().stream()
                    	.filter(t -> "gsg:amount".equals(t.getKey()))
                    	.map(t -> t.getValue())
                    	.findAny()
                    	.orElse("0");
            	weight += Double.parseDouble(weightString);
            } catch(NullPointerException | NumberFormatException ex) {
            }
            relationsWeight.put(r.getId(), weight);
            
            r.getMembers().forEach(rm -> {
                if (rm.getMemberType() != EntityType.Way) {
                    return;
                }

                relations.get(r.getId()).add(rm.getMemberId());
            });
        }
    }
    
    public static Color blend(Color c0, Color c1, double w1) {
    	double w0 = 1 - w1;
    	
        double r = w0 * c0.getRed() + w1 * c1.getRed();
        double g = w0 * c0.getGreen() + w1 * c1.getGreen();
        double b = w0 * c0.getBlue() + w1 * c1.getBlue();

        return new Color((int) r, (int) g, (int) b);
    }
    
    private static final double EARTH_RADIUS = 6378137d;
    private static final double ORIGIN_SHIFT = 2d * Math.PI * EARTH_RADIUS / 2d;
    public static Pair<Double, Double> getMeters(Node n) {
        double meterX = n.getLongitude() * ORIGIN_SHIFT / 180d;
        
        double meterY = Math.log(Math.tan((90d + n.getLatitude()) * Math.PI / 360d)) / (Math.PI / 180d);
        meterY = meterY * ORIGIN_SHIFT / 180d;
        
        return Pair.of(meterX, -meterY);
    }

}
