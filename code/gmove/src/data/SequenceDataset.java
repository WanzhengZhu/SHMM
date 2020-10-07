package data;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import textAugmentation.Augmenter;
import org.apache.commons.math3.linear.RealVector;

import java.io.*;
import java.util.*;

/**
 * HMM training data. Created by chao on 4/29/15.
 */
public class SequenceDataset {

	// training data
	List<Sequence> trainseqs = new ArrayList<Sequence>();
	List<Map<Integer, Integer>> textData = new ArrayList<Map<Integer, Integer>>(); // The text data for the R trainseqs, the length is 2R
	List<RealVector> textVecData = new ArrayList<RealVector>();
	List<RealVector> geoData = new ArrayList<RealVector>(); // The geographical data for the R seqeunces, length 2R
	List<RealVector> temporalData = new ArrayList<RealVector>(); // The temporal data for the R seqeunces, length 2R
	List<RealVector> muData = new ArrayList<RealVector>();
	List<RealVector> kappaData = new ArrayList<RealVector>();
	List<RealVector> IniPiData = new ArrayList<RealVector>();
	List<RealVector> TransProbData = new ArrayList<RealVector>();

	int HMM_length;
	int NumofState;
	int numWords;
	// test data
	double testRatio;
	List<Sequence> testSeqs = new ArrayList<Sequence>();

//	public void load(String sequenceFile) throws IOException {
//		testRatio = 0;
//		load(sequenceFile, 0, false, 1);
//	}

	public void load(String sequenceFile, double testRatio, boolean filterTest, int info_option) throws IOException {
		this.testRatio = testRatio;
		List<Sequence> allSeqs = new ArrayList<Sequence>();
		BufferedReader br = new BufferedReader(new FileReader(sequenceFile));
		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			Sequence seq = parseSequence(line, info_option);
			allSeqs.add(seq);
		}
		br.close();

		Collections.shuffle(allSeqs, new Random(1));
		trainseqs = allSeqs.subList(0, (int) (allSeqs.size() * (1 - testRatio)));
		if (filterTest) {
			filterTestSeqs(allSeqs);
		} else {
//			testSeqs = trainseqs;
			testSeqs = allSeqs.subList((int) (allSeqs.size() * (1 - testRatio)), allSeqs.size());
		}

		// Geo, temporal and text data.
		geoData = new ArrayList<RealVector>();
		temporalData = new ArrayList<RealVector>();
		textData = new ArrayList<Map<Integer, Integer>>(); //This is for previous word augmenting
		textVecData = new ArrayList<RealVector>(); //This is for word2vec.

		for (Sequence sequence : trainseqs) {
			if (sequence.size() != 2) {
				System.out.println("Warning! The sequence's length is not 2.");
			}
			List<Checkin> checkins = sequence.getCheckins();
			for (Checkin c : checkins) {
				geoData.add(c.getLocation().toRealVector());
				textData.add(c.getMessage());
//				RealVector temp = null;
//				temp = c.getVector();
//				temp.mapDivideToSelf(temp.getNorm());
//				textVecData.add(temp);
				textVecData.add(c.getVector());
				temporalData.add(new ArrayRealVector(new double[] { c.getTimestamp() % 1440 })); // get the minutes of the timestamp.
			}
		}
		System.out.println("Loading geo, temporal, and textual data finished.");
	}


	public void loadpara(String sequenceFile) throws IOException {
		List<Sequence> allSeqs = new ArrayList<Sequence>();
		BufferedReader br = new BufferedReader(new FileReader(sequenceFile));
		// mu, kappa data.
		muData = new ArrayList<RealVector>();
		kappaData = new ArrayList<RealVector>();
		IniPiData = new ArrayList<RealVector>();
		TransProbData = new ArrayList<RealVector>();

		int linenum = 0;

		while (true) {
			String line = br.readLine();
			if (line == null)
				break;
			linenum++;
		}
		br.close();
//		System.out.print(linenum);

		BufferedReader br2 = new BufferedReader(new FileReader(sequenceFile));

		for (int i=0; i<linenum; i++){
			String line = br2.readLine();
			RealVector messagevector = parseMessageVector(line);
			if (i==0)
				NumofState = (int) messagevector.getEntry(i);
			else if (i<(linenum-1)/4+1)
				muData.add(messagevector);
			else if (i<(linenum-1)/2+1)
				kappaData.add(messagevector);
			else if (i<(linenum-1)*3/4+1)
				IniPiData.add(messagevector);
			else
				TransProbData.add(messagevector);
		}
		System.out.print(muData + "  |  " + kappaData + "  |  " + IniPiData + "  |  " + TransProbData);
	}

	public void augmentText(Augmenter augmenter, int augmentedSize, boolean augmentTrain, boolean augmentTest) {
		if (augmentTrain) {
			textData.clear();
			for (Sequence sequence : trainseqs) {
				List<Checkin> checkins = sequence.getCheckins();
				for (Checkin c : checkins) {
					c.setMessage(augmenter.getAugmentedText(c.getMessage(), augmentedSize));
					textData.add(c.getMessage());
				}
			}
		}
		if (augmentTest) {
			for (Sequence sequence : testSeqs) {
				List<Checkin> checkins = sequence.getCheckins();
				for (Checkin c : checkins) {
					c.setMessage(augmenter.getAugmentedText(c.getMessage(), augmentedSize));
				}
			}
		}
	}

	private void filterTestSeqs(List<Sequence> allSeqs) {
		HashMap<Long, HashSet<Integer>> user2seqs = new HashMap<Long, HashSet<Integer>>();
		for (int i = 0; i < trainseqs.size(); i++) {
			Sequence seq = trainseqs.get(i);
			long user = seq.getUserId();
			if (!user2seqs.containsKey(user)) {
				user2seqs.put(user, new HashSet<Integer>());
			}
			user2seqs.get(user).add(i);
		}
		testSeqs = new ArrayList<Sequence>();
		for (int i = (int) (allSeqs.size() * (1 - testRatio)); i < allSeqs.size(); ++i) {
			Sequence seq = allSeqs.get(i);
			long user = seq.getUserId();
			if (user2seqs.containsKey(user)) {
				testSeqs.add(seq);
			}
		}
		System.out.println("filtered testSeqs size: " + testSeqs.size());
	}

	// add training seq
	public void addSequence(Sequence s) {
		this.trainseqs.add(s);
	}

	// add test seq
	public void addTestSequence(Sequence s) {
		this.testSeqs.add(s);
	}

	public void addTextDatum(Map<Integer, Integer> message) {
		this.textData.add(message);
	}

	public void addTextDatumVec(RealVector messagevector) {
		this.textVecData.add(messagevector);
	}

	public void addGeoDatum(RealVector rv) {
		this.geoData.add(rv);
	}

	public void addTemporalDatum(RealVector rv) {
		this.temporalData.add(rv);
	}

	public void addmuDatum(RealVector rv) {
		this.muData.add(rv);
	}

	public void addkappaDatum(RealVector rv) {
		this.kappaData.add(rv);
	}

	public void addIniPiDatum(RealVector rv) {
		this.IniPiData.add(rv);
	}

	public void addTransProbDatum(RealVector rv) {
		this.TransProbData.add(rv);
	}

	public void setNumWords(int numWords) {
		this.numWords = numWords;
	}

	public void setTestRatio(double testRatio) {
		this.testRatio = testRatio;
	}

	// Each line contains: checkin Id, userId, placeid, timestamp, message
	private Sequence parseSequence(String line, int info_option) {
		String[] items = line.split(",");

		if (info_option == 30 || info_option ==31) {  // Synthetic Data...
			HMM_length = items.length / 6;
			Checkin start = toCheckin(Arrays.copyOfRange(items, 0, items.length / HMM_length), info_option);
			long userId = start.getUserId();
			Sequence seq = new Sequence(userId);
			seq.addCheckin(start);
			for (int n = 1; n < HMM_length; n++) {
				Checkin next = toCheckin(Arrays.copyOfRange(items, items.length / HMM_length*n, items.length/HMM_length*(n+1)), info_option);
				seq.addCheckin(next);
			}
			return seq;
		}
		else {
			Checkin start = toCheckin(Arrays.copyOfRange(items, 0, items.length / 2), info_option);
			Checkin end = toCheckin(Arrays.copyOfRange(items, items.length / 2, items.length), info_option);
			long userId = start.getUserId();
			Sequence seq = new Sequence(userId);
			seq.addCheckin(start);
			seq.addCheckin(end);
			return seq;
		}


	}



	private Checkin toCheckin(String[] items, int info_option) {
		if (items.length < 6) {
			System.out.println("Error when parsing checkins.");
			return null;
		}
		int checkinId = Integer.parseInt(items[0]);
		int timestamp = Integer.parseInt(items[1]);
		long userId = Long.parseLong(items[2]);
		double lat = Double.parseDouble(items[3]);
		double lng = Double.parseDouble(items[4]);
		Map<Integer, Integer> message = null;
		RealVector messagevector = null;
		if ((info_option == 11) || (info_option == 13) || (info_option == 14) || (info_option == 15)) { // (10-20) is Bag-of-words
			message = parseMessage(items[5]);
		}
		else if ((info_option == 1) || (info_option == 3) || (info_option == 4) || (info_option == 5) || (info_option == 21) || (info_option == 22) || info_option == 30 || info_option ==31) { // 2 is without text; 6 is with only location; 7 is with only time; 8 is with no information
			messagevector = parseMessageVector(items[5]);
		}
//		messagevector = new ArrayRealVector(new double[]{});
//		messagevector = messagevector.append(0);
		return new Checkin(checkinId, timestamp, userId, lat, lng, message, messagevector, info_option);
	}

	private Map<Integer, Integer> parseMessage(String s) {
		Map<Integer, Integer> message = new HashMap<Integer, Integer>();
		String[] items = s.split("\\s");
		if (items.length == 0) {
			System.out.println("Warning! Checkin has no message.");
		}
		for (int i = 0; i < items.length; i++) {
			int wordId = Integer.parseInt(items[i]);
			int oldCnt = message.containsKey(wordId) ? message.get(wordId) : 0;
			message.put(wordId, oldCnt + 1);
		}
		return message;
	}

	private RealVector parseMessageVector(String s) {
		RealVector messagevector = new ArrayRealVector(new double[]{});
		String[] items = s.split("\\s");
		if (items.length == 0) {
			System.out.println("Warning! Checkin has no message.");
		}
		for (int i=0; i < items.length; i++){
			messagevector = messagevector.append(Double.parseDouble(items[i]));
		}
		return messagevector;
	}

	public List<Sequence> getSequences() {
		return trainseqs;
	}

	public List<RealVector> getGeoData() {
		return geoData;
	}

	public List<RealVector> getTemporalData() {
		return temporalData;
	}

	public List<Map<Integer, Integer>> getTextData() {
		return textData;
	}

	public List<RealVector> getTextVecData() {
		return textVecData;
	}

	public RealVector getGeoDatum(int index) {
		return geoData.get(index);
	}

	public RealVector getTemporalDatum(int index) {
		return temporalData.get(index);
	}

	public Map<Integer, Integer> getTextDatum(int index) {
		return textData.get(index);
	}

	public RealVector getTextVecDatum(int index) {
		return textVecData.get(index);
	}

	public RealVector getmuDatum(int index) {
		return muData.get(index);
	}

	public RealVector getkappaDatum(int index) {
		return kappaData.get(index);
	}

	public RealVector getIniPiDatum(int index) {
		return IniPiData.get(index);
	}

	public RealVector getTransProbDatum(int index) {
		return TransProbData.get(index);
	}

	public Sequence getSequence(int i) {
		return trainseqs.get(i);
	}

	public int getHMMLength() {
		return HMM_length;
	}

	public int getNumofState() {
		return NumofState;
	}

	public int size() {
		return trainseqs.size();
	}

	public int numWords() {
		return numWords;
	}

	public PredictionDataset extractTestData() throws Exception {
		return new PredictionDataset(testSeqs);
	}

//	public SequenceDataset getCopy(int info_option) {
//		SequenceDataset copiedDataSet = new SequenceDataset();
//		for (Sequence s : trainseqs) {
//			copiedDataSet.addSequence(s.copy(info_option));
//		}
//		for (Sequence s : testSeqs) {
//			copiedDataSet.addTestSequence(s.copy(info_option));
//		}
//		for (Map<Integer, Integer> m : textData) {
//			copiedDataSet.addTextDatum(new HashMap(m));
//		}
//		for (RealVector m : textVecData) {
//			copiedDataSet.addTextDatumVec(m);
//		}
//		for (RealVector rv : geoData ) {
//			copiedDataSet.addGeoDatum(rv);
//		}
//		for (RealVector rv : temporalData) {
//			copiedDataSet.addTemporalDatum(rv);
//		}
//		for (RealVector rv : muData) {
//			copiedDataSet.addmuDatum(rv);
//		}
//		for (RealVector rv : kappaData) {
//			copiedDataSet.addkappaDatum(rv);
//		}
//		for (RealVector rv : IniPiData) {
//			copiedDataSet.addIniPiDatum(rv);
//		}
//		for (RealVector rv : TransProbData) {
//			copiedDataSet.addTransProbDatum(rv);
//		}
//		copiedDataSet.setNumWords(this.numWords);
//		copiedDataSet.setTestRatio(this.testRatio);
//		return copiedDataSet;
//	}

}
