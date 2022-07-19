package com.activeviam.experiments.gameoflife.biz.tasks.export;

import static com.activeviam.experiments.gameoflife.biz.Utils.parseArg;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * An implementation of export task. Exports the Game Of Life board into a file in the human-readable format, like
 * this:
 * <pre>
 * _*_
 * __*
 * ***
 * </pre>
 */
public class PrettyExportTask extends AExportTask {

	private List<ATask<BoardChunk>> chunkTasks;
	private File file;

	/**
	 * Constructs the task.
	 *
	 * @param chunkTasks List of tasks that produce the chunks of the last generation of the board
	 * @param file       The file where the result should be exported
	 */
	public PrettyExportTask(List<ATask<BoardChunk>> chunkTasks, File file) {
		this.chunkTasks = chunkTasks;
		this.file = file;
	}

	private record Parameters(File file) {

	}

	/**
	 * Builds a new {@link PrettyExportTask} instance.
	 *
	 * @param config     Export task configuration
	 * @param chunkTasks The tasks that produce the chunks of the last generation of the board
	 * @return A new instance
	 */
	public static PrettyExportTask build(SinkConfig config, List<ATask<BoardChunk>> chunkTasks) {
		Parameters params = tryParseParams(config);
		return new PrettyExportTask(chunkTasks, params.file);
	}

	private static Parameters tryParseParams(SinkConfig config) {
		if (config.args() == null || config.args().length != 1) {
			throw new IllegalArgumentException(
					"Bad argument count, expected [<file>] got " + Arrays.toString(config.args()));
		}

		File file = parseArg(config.args(), 0, File.class);

		return new Parameters(file);
	}

	@Override
	protected List<ATask<?>> getDependencies() {
		return new ArrayList<>(chunkTasks);
	}

	@Override
	protected Void compute() throws Exception {
		BoardChunk[] chunks = new BoardChunk[chunkTasks.size()];

		try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
			List<Future<BoardChunk>> futures = new ArrayList<>(chunkTasks.size());
			for (ATask<BoardChunk> chunkTask : chunkTasks) {
				futures.add(scope.fork(chunkTask));
			}

			scope.join().throwIfFailed();

			for (int i = 0; i < chunks.length; ++i) {
				chunks[i] = futures.get(i).resultNow();
			}
		}

		startExporting();
		Board board = merge(chunks);
		try (var stream = new PrintStream(new FileOutputStream(file))) {
			for (int y = 0; y < board.height(); ++y) {
				for (int x = 0; x < board.width(); ++x) {
					stream.print(board.getAt(x, y) ? '*' : '_');
				}
				stream.println();
			}
		}
		stopExporting();

		return null;
	}

	@Override
	protected void dispose() {
		chunkTasks = null;
		file = null;
	}

	private Board merge(BoardChunk[] chunks) {
		int width = chunks[0].getWidth();
		int height = chunks[0].getHeight();

		boolean[][] data = new boolean[width][];

		for (BoardChunk chunk : chunks) {
			if (chunk.getStripeWidth() >= 0) {
				System.arraycopy(chunk.getData(), 0, data, chunk.getBeginWidth(), chunk.getStripeWidth());
			}
		}
		return new Board(width, height, data);
	}
}
