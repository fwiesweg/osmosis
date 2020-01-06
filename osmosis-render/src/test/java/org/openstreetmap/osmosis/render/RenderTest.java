// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.render;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.openstreetmap.osmosis.core.Osmosis;
import org.openstreetmap.osmosis.testutil.AbstractDataTest;

/**
 * Tests the tag transform functionality.
 *
 * @author Brett Henderson
 */
public class RenderTest extends AbstractDataTest {

    /**
     * Tests transforming all tags in a single OSM file.
     *
     * @throws IOException if any file operations fail.
     */
    @Test
    public void testRender() throws IOException {
        File sourceFile;
        File sinkFile;

        // Generate files.
        sourceFile = dataUtils.createDataFile("v0_6/test-in.osm");
        sinkFile = dataUtils.newFile();

        // Append the two source files into the destination file.
        Osmosis.run(
                new String[]{
                    "-q",
                    "--read-xml-0.6",
                    sourceFile.getPath(),
                    "--tag-sort-0.6",
                    "--render-0.6",
                    sinkFile.getPath()
                }
        );

        // Validate that the output file matches the expected result.
        //dataUtils.compareFiles(expectedOutputFile, actualOutputFile);
    }

}
