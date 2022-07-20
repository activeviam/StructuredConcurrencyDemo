package com.activeviam.experiments.gameoflife;

import com.activeviam.experiments.gameoflife.biz.GameOfLifeTaskBuilder;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask.SinkType;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask.SourceType;
import com.activeviam.experiments.gameoflife.task.ATask;
import com.activeviam.experiments.gameoflife.task.WorkflowPrinter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class WorkflowPrintDemo {

	public static void main(String[] args) {
		try (var printer = new WorkflowPrinter(new PrintWriter(new FileOutputStream("workflow.yaml")))) {
			int parallelism = 4;
			ATask<Void> task =
					new GameOfLifeTaskBuilder()
							.withSource(SourceType.RANDOM, 1000, 1000, 0L)
							.withSink(SinkType.PRETTY, new File("game_of_life_%d.txt".formatted(parallelism)))
							.withIterations(3)
							.withParallelism(parallelism)
							.useWatcher(true)
							.build();

			printer.print(task);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
