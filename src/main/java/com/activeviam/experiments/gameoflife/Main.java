package com.activeviam.experiments.gameoflife;

import com.activeviam.experiments.gameoflife.biz.GameOfLifeTaskBuilder;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask.SinkType;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask.SourceType;
import java.io.File;

/**
 * This is a sample program that uses structured concurrency to simulate Game Of Life.
 */
public class Main {

	/**
	 * The entry point.
	 *
	 * @param args Command line arguments
	 * @throws Exception if something goes wrong :)
	 */
	public static void main(String[] args) throws Exception {
		int[] parallelismValues = new int[]{1, 2, 4, 8, 16, 24, 32};

		for (int parallelism : parallelismValues) {
			new GameOfLifeTaskBuilder()
					.withSource(SourceType.RANDOM, 1000, 1000, 0L)
					.withSink(SinkType.PRETTY, new File("game_of_life_%d.txt".formatted(parallelism)))
					.withIterations(1000)
					.withParallelism(parallelism)
					.build()
					.call();
		}
	}
}
