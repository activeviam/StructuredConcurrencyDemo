package com.activeviam.experiments.gameoflife.biz.tasks.retrieve;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext.ExecutionStage;
import com.activeviam.experiments.gameoflife.task.ATask;

/**
 * This class represents an abstract task that retrieves the initial state of Game Of Life.
 */
public abstract class ARetrieveTask extends ATask<Board> {

	/**
	 * This enum is used to select the implementation of the retrieve task.
	 */
	public enum SourceType {
		/**
		 * Generate a random board of predefined dimensions. See {@link RandomRetrieveTask}.
		 */
		RANDOM
	}

	/**
	 * A class that represents a retrieve task configuration.
	 *
	 * @param type Retrieve task type
	 * @param args Arguments to be passed to the constructor
	 */
	public record SourceConfig(SourceType type, Object[] args) {

	}


	/**
	 * Constructs a new retrieve task instance.
	 *
	 * @param config Retrieve task configuration
	 * @return New retrieve task instance
	 */
	public static ARetrieveTask build(SourceConfig config) {
		//noinspection SwitchStatementWithTooFewBranches
		return switch (config.type) {
			case RANDOM -> RandomRetrieveTask.build(config);
		};
	}

	/**
	 * Notify the context that the retrieving stage has begun. See {@link ExecutionStage}.
	 */
	protected void startRetrieving() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.RETRIEVING);
	}

	/**
	 * Notify the context that the retrieving stage has been done. See {@link ExecutionStage}.
	 */
	protected void stopRetrieving() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.COMPUTING);
	}
}
