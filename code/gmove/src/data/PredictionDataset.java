package data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by chao on 12/14/15.
 */
public class PredictionDataset {
    List<Sequence> sequences;
    List<Set<Checkin>> candidates;

    public PredictionDataset(List<Sequence> sequences) {
        this.sequences = sequences;
    }

    public void genCandidates(double distThre, double timeThre) {
        candidates = new ArrayList<Set<Checkin>>();
        for (Sequence seq : sequences) {
            candidates.add(getNeighbors(seq.getCheckin(1), distThre, timeThre));
        }
    }

    private Set<Checkin> getNeighbors(Checkin c, double distThre, double timeThre) {
        Set<Checkin> ret = new HashSet<Checkin>();
        for (Sequence seq : sequences) {
            for (Checkin cand : seq.getCheckins()) {
                    if (c.getreal_location().calcGeographicDist(cand.getreal_location()) <= distThre &&
//                            cand.getrealTimestamp() - c.getrealTimestamp() <= timeThre && cand.getrealTimestamp() - c.getrealTimestamp() >= 0)
                            Math.abs(cand.getrealTimestamp() - c.getrealTimestamp()) <= timeThre)
                        ret.add(cand);
            }
        }
        return ret;
    }

    public int size() {
        return sequences.size();
    }

    public Sequence getSeq(int i) {
        return sequences.get(i);
    }

    public Set<Checkin> getCands(int i) {
        return candidates.get(i);
    }

}
