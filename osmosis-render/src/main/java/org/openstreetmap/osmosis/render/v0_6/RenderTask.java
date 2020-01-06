// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.render.v0_6;

import java.awt.Color;
import java.awt.Polygon;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
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

    private final Map<Long, List<Long>> relations = new HashMap<>(5000);
    private final Map<Long, List<Long>> ways = new HashMap<>(50000);
    private final Map<Long, Node> nodes = new HashMap<>(500000);

    private final Document document;

    public RenderTask(File outputFile) {
        this.outputFile = outputFile;

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
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setPaint(Color.red);
        relations.values().stream()
                .map(r -> r.stream().map(ways::get).filter(w -> w != null).flatMap(w -> w.stream()))
                .map(r -> {
                    Polygon p = new Polygon();
                    r
                            .map(nid -> nodes.get(nid))
                            .filter(n -> n != null)
                            /*.map(n -> {
                                return Point.fromLatitudeLongitude(n.getLatitude(), n.getLongitude());
                            })
                            .forEach(n -> p.addPoint((int) n.getMeterX() * 1000, (int) n.getMeterY() * 1000));*/
                            .forEach(n -> {
                                p.addPoint((int) (n.getLongitude() * 1000), (int) (n.getLatitude() * 1000));
                            });
                    return p;
                })
                .forEach(svgGenerator::fillPolygon);

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
            r.getMembers().forEach(rm -> {
                if (rm.getMemberType() != EntityType.Way) {
                    return;
                }

                relations.get(r.getId()).add(rm.getMemberId());
            });
        }
    }

}
