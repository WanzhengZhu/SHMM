package predict;

import data.Checkin;
import data.Sequence;
import myutils.ScoreCell;

/**
 * Created by chao on 5/3/15.
 */
public class DistancePredictor extends Predictor {

    public ScoreCell calcScore(Sequence m, Checkin p, int info_option) {
        int placeId = p.getId();
        Checkin startPlace = m.getCheckin(0);
        double score = p.getLocation().calcEuclideanDist(startPlace.getLocation());
        return new ScoreCell(placeId, score);
    }

    public void printAccuracy() {
        System.out.println("Distance-based predictor accuracy:" + accuracy);
    }
}
