package com.activeviam.experiments.gameoflife.biz.tasks.retrieve;

import static com.activeviam.experiments.gameoflife.biz.Utils.parseArg;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import java.util.Arrays;
import java.util.Random;

/**
 * This class represents a retrieve task that generates a random Game Of Life board.
 */
public class RandomRetrieveTask extends ARetrieveTask {

	private final int width;
	private final int height;
	private final long seed;

	/**
	 * Constructs a new RandomRetrieveTask instance.
	 *
	 * @param width  Board width
	 * @param height Board height
	 * @param seed   Random generator seed, see {@link Random#setSeed}.
	 */
	public RandomRetrieveTask(int width, int height, long seed) {
		this.width = width;
		this.height = height;
		this.seed = seed;
	}

	private record Parameters(int width, int height, long seed) {

	}


	/**
	 * Constructs a new {@link RandomRetrieveTask} instance.
	 *
	 * @param config Retrieve task configuration
	 * @return New task instance
	 */
	public static RandomRetrieveTask build(SourceConfig config) {
		Parameters params = tryParseParams(config);
		return new RandomRetrieveTask(params.width, params.height, params.seed);
	}

	private static Parameters tryParseParams(SourceConfig config) {
		if (config.args() == null || config.args().length != 3) {
			throw new IllegalArgumentException(
					"Bad argument count, expected [<width>, <height>, <seed>], got " + Arrays.toString(config.args()));
		}

		int width = parseArg(config.args(), 0, Integer.class);
		int height = parseArg(config.args(), 1, Integer.class);
		long seed = parseArg(config.args(), 2, Long.class);

		if (width <= 0) {
			throw new IllegalArgumentException("width must be positive");
		}
		if (height <= 0) {
			throw new IllegalArgumentException("height must be positive");
		}

		return new Parameters(width, height, seed);
	}

	@Override
	protected Board compute() {
		Random random = new Random();
		random.setSeed(seed);

		startRetrieving();
		boolean[][] cells = new boolean[width][height];
		for (int i = 0; i < width; ++i) {
			for (int j = 0; j < height; ++j) {
				cells[i][j] = random.nextBoolean();
			}
		}
		stopRetrieving();

		return new Board(width, height, cells);
	}
}
