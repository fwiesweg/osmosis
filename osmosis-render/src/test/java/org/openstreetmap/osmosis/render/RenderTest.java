// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.render;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.openstreetmap.osmosis.core.Osmosis;
import org.openstreetmap.osmosis.testutil.AbstractDataTest;

/**
 * Tests rendering.
 *
 * @author Florian Wiesweg
 */
public class RenderTest extends AbstractDataTest {

	/**
	 * Renders a simple border.
	 * 
	 * @throws IOException if any file operations fail.
	 */
    @Test
    public void testRender() throws IOException {
        File sourceFile = dataUtils.createDataFile("v0_6/test-in.osm");
        File actualOutputFile = dataUtils.newFile();
        File expectedOutputFile = dataUtils.createDataFile("v0_6/test-out.svg");

        // Append the two source files into the destination file.
        Osmosis.run(
                new String[]{
                    "-q",
                    "--read-xml-0.6",
                    sourceFile.getPath(),
                    "--tag-sort-0.6",
                    "--render-0.6",
                    actualOutputFile.getPath()
                }
        );

        // Validate that the output file matches the expected result.
        dataUtils.compareFiles(expectedOutputFile, actualOutputFile);
    }

}
