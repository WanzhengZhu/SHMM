package textAugmentation;

import java.util.*;
import myutils.*;
import data.Checkin;
import data.Sequence;
import data.SequenceDataset;

//compute and maintain word idf
public class WordIdf {
	HashMap<Integer, Double> word2idf = new HashMap<Integer, Double>();
	int N = 0;

	public WordIdf(SequenceDataset sequenceDataset) {
		for (Sequence sequence : sequenceDataset.getSequences()) {
			for (Checkin checkin : sequence.getCheckins()) {
				++N;
				for (Integer word : checkin.getMessage().keySet()) {
					if (!word2idf.containsKey(word)) {
						word2idf.put(word, (double) 1); //put 1 instead of 0 for smoothing
					}
					word2idf.put(word, word2idf.get(word) + 1);
				}
			}
		}
		for (int word : word2idf.keySet()) {
			word2idf.put(word, Math.log(N / word2idf.get(word)));
		}
	}

	public Double getIdf(int word) {
		if (word2idf.containsKey(word)) {
			return word2idf.get(word);
		} else {
			return Math.log(N / 1.0);
		}
	}

	public boolean containsKey(Integer word) {
		return word2idf.containsKey(word);
	}
}
