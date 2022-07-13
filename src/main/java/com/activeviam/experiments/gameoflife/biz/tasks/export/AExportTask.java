package com.activeviam.experiments.gameoflife.biz.tasks.export;

import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.concurrent.Callable;

public abstract class AExportTask extends ATask<Void> {

	public enum SinkType {
		PRETTY
	}

	public record SinkConfig(SinkType type, Object[] args) {
	}

	public static Callable<Void> build(SinkConfig sinkConfig, Callable<BoardChunk>[] lastGeneration) {
		//noinspection SwitchStatementWithTooFewBranches
		return switch (sinkConfig.type) {
			case PRETTY -> PrettyExportTask.build(sinkConfig, lastGeneration);
		};
	}
}
