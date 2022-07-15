package com.activeviam.experiments.gameoflife.biz.tasks.retrieve;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext.ExecutionStage;
import com.activeviam.experiments.gameoflife.task.ATask;

public abstract class ARetrieveTask extends ATask<Board> {

	public enum SourceType {
		RANDOM
	}

	public record SourceConfig(SourceType type, Object[] args) {
	}

	public static ARetrieveTask build(SourceConfig config) {
		//noinspection SwitchStatementWithTooFewBranches
		return switch (config.type) {
			case RANDOM -> RandomRetrieveTask.build(config);
		};
	}

	protected void startRetrieving() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.RETRIEVING);
	}

	protected void stopRetrieving() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.COMPUTING);
	}
}
