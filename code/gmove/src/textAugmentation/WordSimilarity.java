package textAugmentation;

import java.io.*;
import java.util.*;
import myutils.*;
import data.Checkin;
import data.Sequence;
import data.SequenceDataset;
import data.WordDataset;
//import demo.Test;

//compute and maintain word feature vector (every dimension is a stGrid) and word similarities
public class WordSimilarity implements Serializable {
	int lngGridNum = 10;
	int latGridNum = 10;
	double similarityThresh = 0.1;

	WordDataset wd = null;
	HashMap<Integer, HashMap<StGrid, Double>> word2stGrids = new HashMap<Integer, HashMap<StGrid, Double>>();
	HashMap<Integer, HashMap<Integer, Double>> similarities = new HashMap<Integer, HashMap<Integer, Double>>();

	public WordSimilarity(SequenceDataset sequenceDataset, WordDataset wd, int lngGridNum, int latGridNum,
			double similarityThresh) {
		this.wd = wd;
		this.lngGridNum = lngGridNum;
		this.latGridNum = latGridNum;
		this.similarityThresh = similarityThresh;
		double lngMax = Double.MIN_VALUE;
		double latMax = Double.MIN_VALUE;
		double lngMin = Double.MAX_VALUE;
		double latMin = Double.MAX_VALUE;
		for (Sequence sequence : sequenceDataset.getSequences()) {
			for (Checkin checkin : sequence.getCheckins()) {
				double lng = checkin.getLocation().getLng();
				double lat = checkin.getLocation().getLat();
				if (lng > lngMax) {
					lngMax = lng;
				}
				if (lat > latMax) {
					latMax = lat;
				}
				if (lng < lngMin) {
					lngMin = lng;
				}
				if (lat < latMin) {
					latMin = lat;
				}
			}
		}
		for (Sequence sequence : sequenceDataset.getSequences()) {
			for (Checkin checkin : sequence.getCheckins()) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(checkin.getTimestamp() * 1000);
				int timeGrid = calendar.get(Calendar.HOUR_OF_DAY);
				double lng = checkin.getLocation().getLng();
				double lat = checkin.getLocation().getLat();
				int lngGrid = (int) Math.ceil((lng - lngMin) * lngGridNum / (lngMax - lngMin));
				int latGrid = (int) Math.ceil((lat - latMin) * latGridNum / (latMax - latMin));
				StGrid stGrid = new StGrid(lngGrid, latGrid, timeGrid);
				//				System.out.print(stGrid);
				for (Integer word : checkin.getMessage().keySet()) {
					if (!word2stGrids.containsKey(word)) {
						word2stGrids.put(word, new HashMap<StGrid, Double>());
					}
					HashMap<StGrid, Double> stGrids = word2stGrids.get(word);
					if (!stGrids.containsKey(stGrid)) {
						stGrids.put(stGrid, (double) 0);
					}
					stGrids.put(stGrid, stGrids.get(stGrid) + 1);
				}
			}
		}
		int count = 0;
		for (Integer word1 : word2stGrids.keySet()) {
			for (Integer word2 : word2stGrids.keySet()) {
				double similarityScore = computeSimilarity(word1, word2);
				if (similarityScore > similarityThresh) {
					++count;
					if (!similarities.containsKey(word1)) {
						similarities.put(word1, new HashMap<Integer, Double>());
					}
					similarities.get(word1).put(word2, similarityScore);
				}
			}
		}
		System.out.println("wd size: " + wd.size());
		System.out.println("similar pairs: " + count);
	}

	public Integer getStGridNum(int word) {
		return word2stGrids.get(word).size();
	}

	public HashMap<Integer, HashMap<Integer, Double>> getSimilarities() {
		return similarities;
	}

	public double computeSimilarity(int word1, int word2) {
		HashMap<StGrid, Double> stGrids1 = word2stGrids.get(word1);
		HashMap<StGrid, Double> stGrids2 = word2stGrids.get(word2);
		return new MapVectorUtils<StGrid>().cosine(stGrids1, stGrids2);
	}

	public void printHighlySimilarPairs() throws Exception {
		List<RankedObject> rankedWordPairs = new ArrayList<RankedObject>();
		for (Integer word1 : word2stGrids.keySet()) {
			for (Integer word2 : word2stGrids.keySet()) {
				if (similarities.containsKey(word1) && similarities.get(word1).containsKey(word2)) {
					double similarityScore = similarities.get(word1).get(word2);
					if (similarityScore > similarityThresh) {
						String gridNum1 = getStGridNum(word1).toString();
						String gridNum2 = getStGridNum(word2).toString();
						String ps = String.join("\t", wd.getWord(word1), wd.getWord(word2), gridNum1, gridNum2);
						rankedWordPairs.add(new RankedObject(ps, similarityScore));
					}
				}
			}
		}
		Collections.sort(rankedWordPairs);
		//		new CollectionFile<>(Test.WorkPath + "results/wordSimilarity.txt").writeFrom(rankedWordPairs);
	}


}
