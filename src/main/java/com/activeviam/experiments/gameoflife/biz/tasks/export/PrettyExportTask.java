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
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

public class PrettyExportTask extends AExportTask {

	private List<ATask<BoardChunk>> chunkTasks;
	private File file;

	private PrettyExportTask(List<ATask<BoardChunk>> chunkTasks, File file) {
		this.chunkTasks = chunkTasks;
		this.file = file;
	}

	private record Parameters(File file) {
	}

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
			for (Callable<BoardChunk> chunkTask : chunkTasks) {
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
		int width = chunks[0].width();
		int height = chunks[0].height();

		boolean[][] data = new boolean[width][];

		for (BoardChunk chunk : chunks) {
			if (chunk.getStripeWidth() >= 0) {
				System.arraycopy(chunk.data(), 0, data, chunk.beginWidth(), chunk.getStripeWidth());
			}
		}
		return new Board(width, height, data);
	}
}
