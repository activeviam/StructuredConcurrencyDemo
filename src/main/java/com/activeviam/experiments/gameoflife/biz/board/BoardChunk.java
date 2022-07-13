package com.activeviam.experiments.gameoflife.biz.board;

public record BoardChunk(int width, int height, int beginWidth, int endWidth, boolean[][] data) {

	public int getStripeWidth() {
		return endWidth - beginWidth;
	}

	public boolean getAt(int i, int j) {
		if (i < 0 || i >= getStripeWidth()) {
			return false;
		}
		if (j < 0 || j >= height) {
			return false;
		}

		return data[i][j];
	}
}
