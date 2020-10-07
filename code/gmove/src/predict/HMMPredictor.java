package predict;

import data.Checkin;
import data.Sequence;
import model.HMM;
import myutils.ScoreCell;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by chao on 5/2/15.
 */
public class HMMPredictor extends Predictor {

	HMM model;
	boolean avgTest;

	public HMMPredictor(HMM model, boolean avgTest) {
		this.model = model;
		this.avgTest = avgTest;
	}

	public ScoreCell calcScore(Sequence m, Checkin p, int info_option) {
		Checkin startPlace = m.getCheckin(0);
		List<RealVector> geo = new ArrayList<RealVector>();
		geo.add(startPlace.getLocation().toRealVector());
		geo.add(p.getLocation().toRealVector());
		List<RealVector> temporal = new ArrayList<RealVector>();
		temporal.add(new ArrayRealVector(new double[] { startPlace.getTimestamp() % 1440 }));
		temporal.add(new ArrayRealVector(new double[] { p.getTimestamp() % 1440 }));
		double score;
		if (info_option > 10) {
			List<Map<Integer, Integer>> text = new ArrayList<Map<Integer, Integer>>();
			text.add(startPlace.getMessage());
			text.add(p.getMessage());
			score = model.calcLL(geo, temporal, text, avgTest, info_option);
		} else {
			List<RealVector> textVec = new ArrayList<RealVector>();
			textVec.add(startPlace.getVector());
			textVec.add(p.getVector());
			score = model.calcVecLL(geo, temporal, textVec, avgTest, info_option);
		}
		int checkinId = p.getId();
//		System.out.println(checkinId + "," + score);
		return new ScoreCell(checkinId, score);
	}

	public void printAccuracy() {
		System.out.println("HMM-based predictor accuracy:" + accuracy);
	}

}
