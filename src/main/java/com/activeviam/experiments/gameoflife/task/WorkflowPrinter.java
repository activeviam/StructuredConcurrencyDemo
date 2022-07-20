package com.activeviam.experiments.gameoflife.task;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

public class WorkflowPrinter implements Closeable {

	private final PrintWriter writer;

	public WorkflowPrinter(PrintWriter writer) {
		this.writer = writer;
	}

	public void print(ATask<?> resultTask) {
		List<Map<String, Object>> tasks =
				collectTasks(resultTask).stream()
						.map(WorkflowPrinter::processTask)
						.toList();

		Yaml yaml = new Yaml();
		yaml.dumpAll(tasks.iterator(), writer);
	}

	private static Map<String, Object> processTask(ATask<?> task) {
		Map<String, Object> properties = new LinkedHashMap<>();

		String hash = Integer.toHexString(task.hashCode());
		String taskType = task.getClass().getTypeName();
		List<String> dependencies = task.getDependencies().stream()
				.map(Object::hashCode)
				.map(Integer::toHexString)
				.toList();

		properties.put("hash", hash);
		properties.put("taskType", taskType);
		properties.put("dependencies", dependencies);

		return properties;
	}


	private static Set<ATask<?>> collectTasks(ATask<?> resultTask) {
		Set<ATask<?>> tasks = new HashSet<>();
		Queue<ATask<?>> queue = new LinkedList<>();

		tasks.add(resultTask);
		queue.add(resultTask);

		while (!queue.isEmpty()) {
			ATask<?> task = queue.poll();

			for (ATask<?> next : task.getDependencies()) {
				if (!tasks.contains(next)) {
					tasks.add(next);
					queue.add(next);
				}
			}
		}

		return tasks;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
