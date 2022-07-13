package com.activeviam.experiments.gameoflife.biz;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeWatcher;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask.SinkConfig;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask.SinkType;
import com.activeviam.experiments.gameoflife.biz.tasks.process.ComputeTask;
import com.activeviam.experiments.gameoflife.biz.tasks.process.FilterTask;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask.SourceConfig;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask.SourceType;
import com.activeviam.experiments.gameoflife.task.TaskUtils;
import java.util.Objects;
import java.util.concurrent.Callable;

public class GameOfLifeTaskBuilder {

	private SourceConfig sourceConfig;
	private SinkConfig sinkConfig;
	private Integer numIterations;
	private Integer parallelism;

	public GameOfLifeTaskBuilder withSource(SourceType type, Object... args) {
		this.sourceConfig = new SourceConfig(type, args);
		return this;
	}

	public GameOfLifeTaskBuilder withSink(SinkType type, Object... args) {
		this.sinkConfig = new SinkConfig(type, args);
		return this;
	}

	public GameOfLifeTaskBuilder withIterations(int numIterations) {
		this.numIterations = numIterations;
		return this;
	}

	public GameOfLifeTaskBuilder withParallelism(int parallelism) {
		this.parallelism = parallelism;
		return this;
	}

	public Callable<Void> build() {
		Objects.requireNonNull(this.sourceConfig, "Source is not configured");
		Objects.requireNonNull(this.sinkConfig, "Sink is not configured");
		Objects.requireNonNull(this.numIterations, "Number of iterations is not configured");

		if (this.numIterations < 0) {
			throw new IllegalArgumentException("Number of iterations must not be negative");
		}

		if (this.parallelism == null) {
			this.parallelism = Runtime.getRuntime().availableProcessors();
		}

		if (this.parallelism <= 0) {
			throw new IllegalArgumentException("Parallelism factor must be positive");
		}

		Callable<Board> retrieveTask = ARetrieveTask.build(this.sourceConfig);

		Callable<BoardChunk>[] lastGeneration = buildFilterTasks(retrieveTask, this.parallelism);
		for (int i = 0; i < numIterations; ++i) {
			lastGeneration = buildNextGeneration(lastGeneration);
		}

		Callable<Void> exportTask = AExportTask.build(this.sinkConfig, lastGeneration);

		GameOfLifeContext ctx = new GameOfLifeContext(parallelism, numIterations);
		return () -> GameOfLifeContext.withContext(ctx)
				.call(TaskUtils.withWatcher(exportTask, new GameOfLifeWatcher()));
	}

	private Callable<BoardChunk>[] buildNextGeneration(Callable<BoardChunk>[] lastGeneration) {
		Callable<BoardChunk>[] tasks = new ComputeTask[lastGeneration.length];

		for (int i = 0; i < tasks.length; ++i) {
			Callable<BoardChunk> prev = i > 0 ? lastGeneration[i - 1] : null;
			Callable<BoardChunk> same = lastGeneration[i];
			Callable<BoardChunk> next = i < tasks.length - 1 ? lastGeneration[i + 1] : null;
			tasks[i] = new ComputeTask(prev, same, next, i);
		}

		return tasks;
	}

	private Callable<BoardChunk>[] buildFilterTasks(Callable<Board> retrieveTask, int parallelism) {
		Callable<BoardChunk>[] tasks = new FilterTask[parallelism];

		for (int i = 0; i < parallelism; ++i) {
			tasks[i] = new FilterTask(retrieveTask, i, parallelism);
		}

		return tasks;
	}
}
