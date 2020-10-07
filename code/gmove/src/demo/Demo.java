package demo;

import data.PredictionDataset;
import data.SequenceDataset;
import data.WordDataset;
import model.EHMM;
import model.GeoHMM;
import model.HMM;
import predict.DistancePredictor;
import predict.EHMMPredictor;
import predict.HMMPredictor;
import textAugmentation.Augmenter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The main file for evaluating the models.
 * Created by chao on 4/16/15.
 */
public class Demo {

	static Map config;
//	static Mongo mongo;

	static WordDataset wd = new WordDataset();
	static SequenceDataset rawDb = new SequenceDataset(); // the database before augmentation
//	static SequenceDataset hmmdb = new SequenceDataset(); // the database after augmentation
	static PredictionDataset pd;

	static List<Integer> KList;
	static int maxIter;
	static boolean avgTest;
	static List<Integer> numStateList;  // the number of states for HMM.
	static int numComponent; // the number of GMM component for HMM
	static List<Integer> numClusterList; // the number of clusters for EHMM.
	static List<String> initMethodList;  // the list of initalization methods for EHMM.

	// parameters for augmentation.
	static List<Double> thresholdList;  // the list of similarity thresholds for augmenting text
	static List<Integer> augmentSizeList;  // the list of augmentation size
	static List<Integer> numAxisBinList;  // the list of number of bins per axis
	static boolean augmentTrain;
	static boolean augmentTest;
	static double augmentThreshold;
	static int augmentSize;
	static int numAxisBin;

	// parameters for prediction
	static double distThre;
	static double timeThre;
	static boolean filterTest;

	// parameters for evaluation
	static List<Integer> info_option_List;
	// SHMM (HMM+VMF): 1 is with all information; 2 is without text; 3 is without location; 4 is without time; 5 is with only text; 6 is with only location; 7 is with only time; 8 is with no information.
	// GMove (HMM+Bag-of-words): 11 is with all information; 12 is without text; 13 is without location; 14 is without time; 15 is with only text(Not supported); 16 is with only location; 17 is with only time; 18 is with no information.
	// Gaussian: 21&22 is with all information, but to use independent Gaussians to model word embeddings.
    // Synthetic: 30 is approximation; 31 is the exact solution by Newton's method.

	static String filename;
	/**
	 * ---------------------------------- Initialize ----------------------------------
	 **/
	static void init(String paraFile) throws Exception {
		config = new Config().load(paraFile);
		info_option_List = (List<Integer>) ((Map) config.get("predict")).get("info_option");
//		mongo = new Mongo(config); // init the connection to mongo db.

		// augment the text data by mining word spatiotemporal correlations.
		thresholdList = (List<Double>) ((Map) config.get("augment")).get("threshold");
		augmentSizeList = (List<Integer>) ((Map) config.get("augment")).get("augmentedSize");
		numAxisBinList = (List<Integer>) ((Map) config.get("augment")).get("numAxisBin");
		augmentTrain = (Boolean) ((Map) config.get("augment")).get("augmentTrain");
		augmentTest = (Boolean) ((Map) config.get("augment")).get("augmentTest");
		augmentSize = augmentSizeList.get(0);
		augmentThreshold = thresholdList.get(0);
		numAxisBin = numAxisBinList.get(0);
		distThre = (Double) ((Map) config.get("predict")).get("distThre");
		timeThre = (Double) ((Map) config.get("predict")).get("timeThre");
		filterTest = (Boolean) ((Map) config.get("predict")).get("filterTest");

		// the model parameters
		maxIter = (Integer) ((Map) config.get("hmm")).get("maxIter");
		KList = (List<Integer>) ((Map) config.get("predict")).get("K");
		avgTest = (Boolean) ((Map) config.get("predict")).get("avgTest");
		numStateList = (List<Integer>) ((Map) config.get("hmm")).get("numState");
		numComponent = (Integer) ((Map) config.get("hmm")).get("numComponent");
		numClusterList = (List<Integer>) ((Map) config.get("ehmm")).get("numCluster");
		initMethodList = (List<String>) ((Map) config.get("ehmm")).get("initMethod");
	}

	static void loadData(int info_option) throws Exception {
		// load data
		String wordFile = null;
		String sequenceFile = null;
		String senvecFile = null;
		double testRatio = (Double) ((Map) config.get("predict")).get("testRatio");

        if (info_option == 30 || info_option ==31){  // Synthetic Data...
            String paraFile = (String) ((Map) ((Map) config.get("file")).get("input")).get("para");
            rawDb.loadpara(paraFile);
            sequenceFile = (String) ((Map) ((Map) config.get("file")).get("input")).get("synthetic_sequences");
            rawDb.load(sequenceFile, testRatio, filterTest, info_option);
        }
        else if (info_option > 10 && info_option < 20) { // (10-20) is Bag-of-words
			wordFile = (String) ((Map) ((Map) config.get("file")).get("input")).get("words");
			sequenceFile = (String) ((Map) ((Map) config.get("file")).get("input")).get("sequences");
			wd.load(wordFile);
			rawDb.load(sequenceFile, testRatio, filterTest, info_option);
			rawDb.setNumWords(wd.size());
			// generate the hmmd and prediction data
			if ((info_option == 11) || (info_option == 13) || (info_option == 14) || (info_option == 15))
				getAugmentedDataSet();
			System.out.println("Augmenting words finished.");
		} else {  // (0-9) is SHMM
			senvecFile = (String) ((Map) ((Map) config.get("file")).get("input")).get("sentencevec");
			rawDb.load(senvecFile, testRatio, filterTest, info_option);
		}

//		hmmdb = rawDb.getCopy(info_option);

		// extract test data
        if (info_option != 30 && info_option !=31){
            pd = rawDb.extractTestData();
            pd.genCandidates(distThre, timeThre);
            System.out.println("Extracting test data finished.");
        }
	}

	static void getAugmentedDataSet() throws Exception {
		Augmenter augmenter = new Augmenter(rawDb, wd, numAxisBin, numAxisBin, augmentThreshold);
		// augmented data for training
//		hmmdb = rawDb.getCopy();
		rawDb.augmentText(augmenter, augmentSize, augmentTrain, augmentTest);
	}

	/**
	 * ---------------------------------- Train and Predict ----------------------------------
	 **/
	static void run(int info_option) throws Exception {
		// run the predictors using default parameters
//		runDistance(info_option);
//		runGeoHMM(maxIter, numStateList.get(0), numComponent);
//		runHMM(maxIter, numStateList.get(0), numComponent);
        if (info_option == 30 || info_option ==31) {  // Synthetic Data...
            runHMM(maxIter, rawDb.getNumofState(), numComponent, info_option);
        } else {
            for (Integer numState : numStateList) {
                runHMM(maxIter, numState, numComponent, info_option);
            }
        }
//        runEHMM(maxIter, numClusterList.get(0), numStateList.get(0), numComponent, initMethodList.get(0), info_option);
//		for (Integer numCluster : numClusterList) {
//			runEHMM(maxIter, numCluster, numStateList.get(0), numComponent, initMethodList.get(0));
//		}
		// tune the parameters
//		evalNumStates();
//		evalNumCluster();
//		evalInitMethod();
//		evalAugmentation();
	}

	/**
	 * Run models with default paramters
	 */
	static void runDistance(int info_option) throws Exception {
		DistancePredictor dp = new DistancePredictor();
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
		bw.write("\nDistance Based Results: ");
		double prediction_results = 0;
		for (Integer K : KList) {
			dp.predict(pd, K, info_option);
			System.out.println("Distance based prediction accuracy:" + dp.getAccuracy());
			bw.write(dp.getAccuracy() + " ");
			prediction_results += dp.getAccuracy();
//			mongo.writePrediction(dp, K);
		}
		bw.write(Double.toString(prediction_results/5));
		bw.close();
	}

//	static void runGeoHMM(int maxIter, int numStates, int numComponent) throws Exception {
//		GeoHMM geoHMM;
//		try {
//			geoHMM =  mongo.loadGeoHMM(numStates);
////			throw new IOException();
//		} catch (Exception e) {
//			System.out.println("Cannot load GeoHMM from the Mongo DB. Start GeoHMM training.");
//			geoHMM = new GeoHMM(maxIter);
//			geoHMM.train(hmmdb, numStates, numComponent);
////			mongo.writeGeoHMM(geoHMM);
//			HMMPredictor hp = new HMMPredictor(geoHMM, avgTest);
//			for (Integer K : KList) {
//				hp.predict(pd, K);
//				System.out.println("GeoHMM based prediction accuracy:" + hp.getAccuracy());
////				mongo.writePredicton(geoHMM, hp, K);
//			}
//		}
//		// predict
//	}

	static void runHMM(int maxIter, int numStates, int numComponent, int info_option) throws Exception {
		HMM h;
		h = new HMM(maxIter);
		h.train(rawDb, numStates, numComponent, info_option, filename);

        if (info_option != 30 && info_option !=31) {
            HMMPredictor hp = new HMMPredictor(h, avgTest);
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
            bw.write("\nNumber of latent states is: "+ numStates);
            bw.write("\nHMM Results: ");
            double prediction_results = 0;
            for (Integer K : KList) {  // Top K Accuracy
                hp.predict(pd, K, info_option);
                System.out.println("HMM based prediction accuracy:" + hp.getAccuracy());
                bw.write(hp.getAccuracy() + " ");
                prediction_results += hp.getAccuracy();
            }
            bw.write(Double.toString(prediction_results/5));
            bw.close();
        }
	}

	static void runEHMM(int maxIter, int numCluster, int numStates, int numComponent, String initMethod, int info_option) throws Exception {
		EHMM ehmm;
		ehmm = new EHMM(maxIter, numStates, numStates, numComponent, numCluster, initMethod);
		ehmm.train(rawDb, info_option);
		EHMMPredictor ep = new EHMMPredictor(ehmm, avgTest);
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
		bw.write("\nEHMM Results: ");
		double prediction_results = 0;
		for (Integer K : KList) {
			ep.predict(pd, K, info_option);
			System.out.println("EHMM based prediction accuracy:" + ep.getAccuracy());
			bw.write(ep.getAccuracy() + " ");
			prediction_results += ep.getAccuracy();
		}
		bw.write(Double.toString(prediction_results/5));
		bw.close();
		// The following block are the previous Chao Zhang's code.
//		try {
//            ehmm =  mongo.loadEHMM(numStates, numCluster, initMethod, hmmdb,
//                        augmentTest, augmentThreshold, augmentSize, numAxisBin);
////			throw new IOException();
//		} catch (Exception e) {
//			System.out.println("Cannot load EHMM from the Mongo DB. Start EHMM training.");
//			ehmm = new EHMM(maxIter, numStates, numStates, numComponent, numCluster, initMethod);
//			ehmm.train(hmmdb);
////			mongo.writeEHMM(ehmm, augmentTest, augmentThreshold, augmentSize, numAxisBin);
//			EHMMPredictor ep = new EHMMPredictor(ehmm, avgTest);
//			for (Integer K : KList) {
//				ep.predict(pd, K);
//				System.out.println("EHMM based prediction accuracy:" + ep.getAccuracy());
////				mongo.writePredicton(ehmm, ep, augmentTest, augmentThreshold, augmentSize, numAxisBin, K);
//			}
//		}
	}

	/**
	 * Evaluate different parameters.
	 */
//	static void evalNumStates() throws Exception {
//		if ((Boolean) ((Map)config.get("hmm")).get("evalNumState") == false)	return;
//		for (Integer numState : numStateList) {
//			runHMM(maxIter, numState, numComponent);
//			runGeoHMM(maxIter, numState, numComponent);
//			runEHMM(maxIter, numClusterList.get(0), numState, numComponent, initMethodList.get(0));
//		}
//	}
//
//	static void evalNumCluster() throws Exception {
//		if ((Boolean) ((Map)config.get("ehmm")).get("evalNumCluster") == false)	return;
//		for (Integer numCluster : numClusterList) {
//			runEHMM(maxIter, numCluster, numStateList.get(0), numComponent, initMethodList.get(0));
//		}
//	}
//
//	static void evalInitMethod() throws Exception {
//		if ((Boolean) ((Map)config.get("ehmm")).get("evalInitMethod") == false)	return;
//		for (String initMethod : initMethodList) {
//			runEHMM(maxIter, numClusterList.get(0), numStateList.get(0), numComponent, initMethod);
//		}
//	}

//	static void evalAugmentation() throws Exception {
//		evalAugmentationThresh();
//		evalAugmentationSize();
//		evalNumAxisBin();
//	}
//
//
//	static void evalAugmentationThresh() throws Exception {
//		if ((Boolean) ((Map)config.get("augment")).get("evalThresh") == false)	return;
//		for (Double threshold : thresholdList) {
//			augmentThreshold = threshold;
//			getAugmentedDataSet();
//			runHMM(maxIter, numStateList.get(0), numComponent);
//			runEHMM(maxIter, numClusterList.get(0), numStateList.get(0), numComponent, initMethodList.get(0));
//		}
//		augmentThreshold = thresholdList.get(0);  // restore the default value
//	}
//
//	static void evalAugmentationSize() throws Exception {
//		if ((Boolean) ((Map)config.get("augment")).get("evalSize") == false)	return;
//		for (Integer asize : augmentSizeList) {
//			augmentSize = asize;
//			getAugmentedDataSet();
//			runHMM(maxIter, numStateList.get(0), numComponent);
//			runEHMM(maxIter, numClusterList.get(0), numStateList.get(0), numComponent, initMethodList.get(0));
//		}
//		augmentSize = augmentSizeList.get(0);
//	}
//
//	static void evalNumAxisBin() throws Exception {
//		if ((Boolean) ((Map)config.get("augment")).get("evalNumBin") == false)	return;
//		for (Integer numBin : numAxisBinList) {
//			numAxisBin = numBin;
//			getAugmentedDataSet();
//			runHMM(maxIter, numStateList.get(0), numComponent);
//			runEHMM(maxIter, numClusterList.get(0), numStateList.get(0), numComponent, initMethodList.get(0));
//		}
//		numAxisBin = numAxisBinList.get(0);
//	}

		/** ---------------------------------- Main ---------------------------------- **/
	public static void main(String [] args) throws Exception {
		// Initialization...
		String paraFile = args.length > 0 ? args[0] : "../run/tf-la.yaml";
		if (paraFile.equals("../run/tf-la.yaml"))
			filename = "../../Results/Twitter-LA/Results_LA.txt";
		if (paraFile.equals("../run/tf-ny.yaml"))
			filename = "../../Results/Twitter-NY/Results_NY.txt";
		if (paraFile.equals("../run/synthetic.yaml"))
			filename = "../../Results/Synthetic/Results_Synthetic.txt";
		init(paraFile);

		// Write the initial parameters to file...
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
		bw.write("\ntimeThre: " + timeThre);
		bw.write("   distThre: " + distThre);
		bw.close();

		// Run the Main Program...
		for (int info_option : info_option_List) {
			bw = new BufferedWriter(new FileWriter(filename, true));
			bw.write("\ninfo_option: " + info_option);
			bw.close();
			loadData(info_option);
			run(info_option);
		}
    }
}