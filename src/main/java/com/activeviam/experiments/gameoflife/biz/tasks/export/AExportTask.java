package com.activeviam.experiments.gameoflife.biz.tasks.export;

import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.biz.GameOfLifeContext.ExecutionStage;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.List;

/**
 * This class represents an abstract Game Of Life board export task.
 */
public abstract class AExportTask extends ATask<Void> {

	/**
	 * This enum is used to select the appropriate export task implementation.
	 */
	public enum SinkType {
		/**
		 * Export the board in the human-readable text format. see {@link PrettyExportTask}.
		 */
		PRETTY
	}

	/**
	 * A class that represents export task configuration
	 *
	 * @param type Sink type
	 * @param args Arguments to be passed to the task constructor
	 */
	public record SinkConfig(SinkType type, Object[] args) {

	}

	/**
	 * Build an export task.
	 *
	 * @param sinkConfig     Configuration of the export task
	 * @param lastGeneration Tasks that return chunks of the last generation
	 * @return An export task
	 */
	public static AExportTask build(SinkConfig sinkConfig, List<ATask<BoardChunk>> lastGeneration) {
		//noinspection SwitchStatementWithTooFewBranches
		return switch (sinkConfig.type) {
			case PRETTY -> PrettyExportTask.build(sinkConfig, lastGeneration);
		};
	}

	/**
	 * Notify the context that the exporting stage has begun. See {@link ExecutionStage}.
	 */
	protected void startExporting() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.EXPORTING);
	}

	/**
	 * Notify the context that the exporting stage has been done. See {@link ExecutionStage}.
	 */
	protected void stopExporting() {
		GameOfLifeContext.getContext().addTimestamp(ExecutionStage.DONE);
	}
}
