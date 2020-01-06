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

    @Override
    protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
        // Get the task arguments.
        String fileName = getStringArgument(
                taskConfig,
                ARG_FILE_NAME,
                getDefaultStringArgument(taskConfig, DEFAULT_FILE_NAME)
        );
        File file = new File(fileName);

        return new SinkManager(taskConfig.getId(), new RenderTask(file), taskConfig.getPipeArgs());
    }

}
