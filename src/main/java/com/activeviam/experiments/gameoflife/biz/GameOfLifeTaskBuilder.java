package com.activeviam.experiments.gameoflife.biz;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeWatcher;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask.SinkConfig;
import com.activeviam.experiments.gameoflife.biz.tasks.export.AExportTask.SinkType;
import com.activeviam.experiments.gameoflife.biz.tasks.process.ComputeTask;
import com.activeviam.experiments.gameoflife.biz.tasks.process.SplitTask;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask.SourceConfig;
import com.activeviam.experiments.gameoflife.biz.tasks.retrieve.ARetrieveTask.SourceType;
import com.activeviam.experiments.gameoflife.task.ATask;
import com.activeviam.experiments.gameoflife.task.Dependency;
import com.activeviam.experiments.gameoflife.task.TaskUtils;
import java.util.List;
import java.util.Objects;

/**
 * This class is used to build a Game Of Life computation workflow.
 */
public class GameOfLifeTaskBuilder {

	private SourceConfig sourceConfig;
	private SinkConfig sinkConfig;
	private Integer numIterations;
	private Integer parallelism;
	private boolean useWatcher = false;

	/**
	 * Set up source configuration.
	 *
	 * @param type Source type
	 * @param args Arguments to be passed to the constructor
	 * @return This builder
	 */
	public GameOfLifeTaskBuilder withSource(SourceType type, Object... args) {
		this.sourceConfig = new SourceConfig(type, args);
		return this;
	}

	/**
	 * Set up sink configuration.
	 *
	 * @param type Sink type
	 * @param args Arguments to be passed to the constructor
	 * @return This builder
	 */
	public GameOfLifeTaskBuilder withSink(SinkType type, Object... args) {
		this.sinkConfig = new SinkConfig(type, args);
		return this;
	}

	/**
	 * Set up the number of iterations.
	 *
	 * @param numIterations Number of iterations
	 * @return This builder
	 */
	public GameOfLifeTaskBuilder withIterations(int numIterations) {
		this.numIterations = numIterations;
		return this;
	}

	/**
	 * Set up number of parallel computation flows. If not set,
	 * {@link Runtime#availableProcessors() Runtime.getRuntime().availableProcessors()} will be used.
	 *
	 * @param parallelism Number of computation flows
	 * @return This builder
	 */
	public GameOfLifeTaskBuilder withParallelism(int parallelism) {
		this.parallelism = parallelism;
		return this;
	}

	/**
	 * Enable or disable watcher (see {@link GameOfLifeWatcher}).
	 *
	 * @param flag If set, a watcher will be attached
	 * @return This builder
	 */
	public GameOfLifeTaskBuilder useWatcher(boolean flag) {
		this.useWatcher = flag;
		return this;
	}

	/**
	 * Constructs the workflow.
	 *
	 * @return A callable that represents the workflow
	 */
	public ATask<Void> build() {
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

		ATask<Board> retrieveTask = ARetrieveTask.build(this.sourceConfig);

		List<ATask<BoardChunk>> lastGeneration = buildSplitTasks(retrieveTask, this.parallelism);
		for (int i = 0; i < numIterations; ++i) {
			lastGeneration = buildNextGeneration(lastGeneration);
		}

		ATask<Void> exportTask = AExportTask.build(this.sinkConfig, lastGeneration);

		final ATask<Void> resultTask =
				useWatcher
						? TaskUtils.withWatchers(exportTask, new GameOfLifeWatcher())
						: exportTask;

		final GameOfLifeContext ctx = new GameOfLifeContext(parallelism, numIterations);
		return new ATask<>() {
			@Dependency
			private ATask<Void> task = resultTask;

			@Override
			protected Void compute() throws Exception {
				return GameOfLifeContext.withContext(ctx).call(task);
			}
		};
	}

	private List<ATask<BoardChunk>> buildNextGeneration(List<ATask<BoardChunk>> lastGeneration) {
		ComputeTask[] tasks = new ComputeTask[lastGeneration.size()];

		for (int i = 0; i < tasks.length; ++i) {
			ATask<BoardChunk> prev = i > 0 ? lastGeneration.get(i - 1) : null;
			ATask<BoardChunk> same = lastGeneration.get(i);
			ATask<BoardChunk> next = i < tasks.length - 1 ? lastGeneration.get(i + 1) : null;
			tasks[i] = new ComputeTask(prev, same, next, i);
		}

		return List.of(tasks);
	}

	private List<ATask<BoardChunk>> buildSplitTasks(ATask<Board> retrieveTask, int parallelism) {
		SplitTask[] tasks = new SplitTask[parallelism];

		for (int i = 0; i < parallelism; ++i) {
			tasks[i] = new SplitTask(retrieveTask, i, parallelism);
		}

		return List.of(tasks);
	}
}
