package com.activeviam.experiments.gameoflife.biz.tasks.export;

import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext.ExecutionStage;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.List;

public abstract class AExportTask extends ATask<Void> {

	public enum SinkType {
		PRETTY
	}

	public record SinkConfig(SinkType type, Object[] args) {
	}

	public static AExportTask build(SinkConfig sinkConfig, List<ATask<BoardChunk>> lastGeneration) {
		//noinspection SwitchStatementWithTooFewBranches
		return switch (sinkConfig.type) {
			case PRETTY -> PrettyExportTask.build(sinkConfig, lastGeneration);
		};
	}

	protected void startExporting() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.EXPORTING);
	}

	protected void stopExporting() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.DONE);
	}
}
