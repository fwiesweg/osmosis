// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.render.v0_6;

import java.io.File;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

public class RenderTaskFactory extends TaskManagerFactory {

    private static final String ARG_FILE_NAME = "file";
    private static final String DEFAULT_FILE_NAME = "output.svg";

    private static final String ARG_SIZE_X = "sizex";
    private static final String ARG_SIZE_Y = "sizey";
    private static final int DEFAULT_SIZE = 1000;

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        // Get the task arguments.
        String fileName = getStringArgument(
                taskConfig,
                ARG_FILE_NAME,
                getDefaultStringArgument(taskConfig, DEFAULT_FILE_NAME)
        );

        int sizex = getIntegerArgument(taskConfig, ARG_SIZE_X, DEFAULT_SIZE);
        int sizey = getIntegerArgument(taskConfig, ARG_SIZE_Y, DEFAULT_SIZE);
        File file = new File(fileName);

        return new SinkManager(taskConfig.getId(), new RenderTask(file, sizex, sizey), taskConfig.getPipeArgs());
    }

}
