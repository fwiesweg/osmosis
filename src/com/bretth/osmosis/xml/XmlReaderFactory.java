package com.bretth.osmosis.xml;

import java.io.File;
import java.util.Map;

import com.bretth.osmosis.pipeline.RunnableSourceManager;
import com.bretth.osmosis.pipeline.TaskManager;
import com.bretth.osmosis.pipeline.TaskManagerFactory;


/**
 * The task manager factory for an xml reader.
 * 
 * @author Brett Henderson
 */
public class XmlReaderFactory extends TaskManagerFactory {
	private static final String ARG_FILE_NAME = "file";
	private static final String DEFAULT_FILE_NAME = "dump.osm";

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected TaskManager createTaskManagerImpl(String taskId, Map<String, String> taskArgs, Map<String, String> pipeArgs) {
		String fileName;
		File file;
		XmlReader task;
		
		// Get the task arguments.
		fileName = getStringArgument(taskId, taskArgs, ARG_FILE_NAME, DEFAULT_FILE_NAME);
		
		// Create a file object from the file name provided.
		file = new File(fileName);
		
		// Build the task object.
		task = new XmlReader(file);
		
		return new RunnableSourceManager(taskId, task, pipeArgs);
	}
}