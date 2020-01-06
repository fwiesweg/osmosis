// This software is released into the Public Domain.  See copying.txt for details.
package org.openstreetmap.osmosis.render;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;
import org.openstreetmap.osmosis.render.v0_6.RenderTaskFactory;

public class RenderPlugin implements PluginLoader {

    @Override
    public Map<String, TaskManagerFactory> loadTaskFactories() {
        RenderTaskFactory renderFactory = new org.openstreetmap.osmosis.render.v0_6.RenderTaskFactory();
        Map<String, TaskManagerFactory> tasks = new HashMap<>();
        tasks.put("render-0.6", renderFactory);
        tasks.put("render", renderFactory);
        return tasks;
    }
}
