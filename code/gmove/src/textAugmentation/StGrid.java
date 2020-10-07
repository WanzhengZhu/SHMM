package textAugmentation;

import java.io.Serializable;

// "StGrid" stands for "spatial-temporal grid"
public class StGrid implements Serializable{
	int lngGrid;
	int latGrid;
	int timeGrid;

	public StGrid(int lngGrid, int latGrid, int timeGrid) {
		this.lngGrid = lngGrid;
		this.latGrid = latGrid;
		this.timeGrid = timeGrid;
	}

	@Override
	public String toString() {
		return lngGrid + "\t" + latGrid + "\t" + timeGrid + "\n";
	}
}
