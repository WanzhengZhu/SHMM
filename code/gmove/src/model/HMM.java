package model;

import cluster.KMeans;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import com.sun.org.apache.regexp.internal.RE;
import data.Checkin;
import data.Sequence;
import data.SequenceDataset;
import data.WordDataset;
import distribution.Gaussian;
import distribution.Multinomial;
import distribution.VMF;
import myutils.ArrayUtils;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.io.*;
import java.util.*;

import static java.lang.Math.exp;
import static java.lang.Math.log;

/**
 * The hidden Markov Model. Created by chao on 4/14/15.
 */
public class HMM implements Serializable {

	public HMM() {}

	public HMM(DBObject o) {
		load(o);
	}

	// Fixed parameters.
	int maxIter;
	int R; // The number of sequences.
	int K; // The number of latent states.
	int M; // The number of Gaussian components in each state.
	int V; // The number of words.
	int N; // The length of the HMM.
	Boolean restart; // Re-training... (get different initialization parameters)
	int num_restart; // Number of Restart
	// weight of the sequences.
	double[] weight;
	double weightSum;
	// The latent variables
	double[][][] alpha; // alpha[r][n][k] is for the n-th position of sequence r at state k.
	double[][][] beta; // beta[r][n][k] is for the n-th position of sequence r at state k.
	double[][] con; // con[r][n] is ln p(x_n | x_1, x_2, ... x_n-1), this is used for normalization.
	double[][][] gamma; // gamma[r][n][k]: probability that the n-th position of sequence r is at state k.
	double[][][] xi; // xi[r][j][k] is the probability that the 1st position of sequence r is state j and the 2nd position is k.
	double[][][][] rho; // rho[r][n][k][l]: probability that the n-th position of sequence r is at state k and from Gaussian component l.
	double[][][][] rho_text; // rho[r][n][k][l]: probability that the n-th position of sequence r is at state k and from Gaussian component l.

	// The parameters that need to be inferred.
	double[] pi; // The prior distribution over the latent states.
	double[][] A; // The transition matrix for the K latent states.
	Multinomial[] textModel; // The K multinomial models for the text part.
	VMF[] textVecModel;
	Gaussian[][] textGaussianModel; // The K Gaussian mixtures for the latent states, each mixture has M components.
	Gaussian[][] textIndependentGaussianModel;
	Gaussian[][] geoModel; // The K Gaussian mixtures for the latent states, each mixture has M components.
	Gaussian[] temporalModel;

	double[][] c; // c[k][m] is the probability of choosing component m for state k;
	double[][] c_text; // c[k][m] is the probability of choosing component m for state k;
	// Log likelihood.
	double[][][] ll; // ll[r][n][k] is the log-likelihood p(x[r][n]|k).
	double[][] scalingFactor; // scalingFactor[r][n] is chosen from ll[r][n].
	double totalLL;
	// running time stat
	double elapsedTime;
	int embedding_dim;

	public HMM(int maxIter) {
		this.maxIter = maxIter;
	}

	public void train(SequenceDataset data, int K, int M, int info_option, String filename) throws IOException {
		if (info_option != 30 && info_option !=31) {
			long start = System.currentTimeMillis();
			init(data, K, M, info_option);
			iterate(data, info_option);
			evaluate(data, info_option);
			long end = System.currentTimeMillis();
			elapsedTime = (end - start) / 1000.0;
			WriteToFile(data, info_option, filename);
		}
		else{  // Synthetic Data...
			restart = true;
			num_restart=0;
			while (restart) {
				num_restart++;
				System.out.printf("\nNum of restart: " + num_restart);
				restart = false;
				init(data, K, M, info_option);
				iterate(data, info_option);
				if (restart==true)
					continue; // Needless to evaluate
				evaluate_synthetic(data, filename);
			}
		}
	}

	/**
	 * Step 1: initialize the geo and text models.
	 */

	protected void init(SequenceDataset data, int K, int M, int info_option) {
		init(data, K, M, null, info_option);
	}

	protected void init(SequenceDataset data, int K, int M, double[] weight, int info_option) {
		initFixedParameters(data, K, M, weight, info_option);
		initEStepParameters(info_option);
		initMStepParameters(data, info_option);
	}

	protected void initFixedParameters(SequenceDataset data, int K, int M, double[] weight, int info_option) {
		this.R = data.size();
		this.K = K;
		this.M = M;
		this.V = data.numWords();
		if (info_option == 30 || info_option ==31) {  // Synthetic Data...
			this.N = data.getHMMLength();
		} else {
			this.N = 2;
		}

		if ((info_option == 5) || (info_option == 21) || (info_option == 22) || info_option == 30 || info_option ==31)
			this.embedding_dim = data.getTextVecData().get(0).getDimension();
		setWeight(weight);
	}

	private void setWeight(double[] weight) {
		if (weight == null) {
			this.weight = new double[R];
			for (int i = 0; i < R; i++)
				this.weight[i] = 1.0;
		} else {
			this.weight = Arrays.copyOf(weight, weight.length);
		}
		weightSum = ArrayUtils.sum(this.weight);
	}

	protected void initEStepParameters(int info_option) {
		ll = new double[R][2][K];
		scalingFactor = new double[R][2];
		alpha = new double[R][2][K];
		beta = new double[R][2][K];
		con = new double[R][2];
		xi = new double[R][K][K];
		gamma = new double[R][2][K];
		rho = new double[R][2][K][M];
		if (info_option == 21)
			rho_text = new double[R][2][K][embedding_dim];
	}

	// Initialize the paramters that need to be inferred.
	protected void initMStepParameters(SequenceDataset data, int info_option) {
		/* ************** Original *************/
//		List<Integer>[] kMeansResults = runKMeans(data);
//		initPi(kMeansResults);
//		initA(kMeansResults);
//		//initTextModel(kMeansResults, data);
//		initTextVecModel(kMeansResults, data);
//		initGeoModel(kMeansResults, data);
//		initTemporalModel(kMeansResults, data);

		/* ************** Shuffle KMean List  *************/
//		List<Integer>[] kMeansResults = runKMeans(data);
////		List<Integer>[] kMeansResults = new List[]<Integer>();
//		List<Integer> list = new ArrayList<Integer>();
//		for (int i = 0; i < data.getTextVecData().size(); i++) list.add(i);
//		Collections.shuffle(list);
//		for (int k=0; k<K; k++) {
//			kMeansResults[k] = list.subList((int)(((double)(k)/K)*list.size()+1), (int)((double)(k+1)/K*list.size()));
//		}
//		initPi(kMeansResults);
//		initA(kMeansResults);
////		initTextModel(kMeansResults, data);
//		initTextVecModel(kMeansResults, data);
////		initGeoModel(kMeansResults, data);
////		initTemporalModel(kMeansResults, data);

		/* **************  Random initialization  *************/
		if (info_option == 5 || info_option == 30 || info_option ==31) {  // SHMM with only text or Synthetic data
			RandominitPi();
			RandominitA();
			RandominitTextVecModel();
		} else if ((info_option == 8) || (info_option == 18)) {  // With No information
			RandominitPi();
			RandominitA();
		} else {
		List<Integer>[] kMeansResults = runKMeans(data);
		initPi(kMeansResults);
		initA(kMeansResults);
		if ((info_option == 1) || (info_option == 2) || (info_option == 4) || (info_option == 6) || (info_option == 11) || (info_option == 12) || (info_option == 14) || (info_option == 16) || (info_option == 21) || (info_option == 22))
			initGeoModel(kMeansResults, data);
		if ((info_option == 1) || (info_option == 2) || (info_option == 3) || (info_option == 7) || (info_option == 11) || (info_option == 12) || (info_option == 13) || (info_option == 17) || (info_option == 21) || (info_option == 22))
			initTemporalModel(kMeansResults, data);
		if ((info_option == 11) || (info_option == 13) || (info_option == 14))
			initTextModel(kMeansResults, data);
		if ((info_option == 1) || (info_option == 3) || (info_option == 4))
			initTextVecModel(kMeansResults, data, info_option);
		if (info_option == 21)
			initTextGaussianModel(kMeansResults, data);
		if (info_option == 22)
			initTextIndependentGaussianModel(kMeansResults, data);
		}

		if ((info_option == 1) || (info_option == 3) || (info_option == 4) || (info_option == 5)) {
			System.out.print("\n\nInitial Kappa, Pi, A and Mean is: \n");
			for (int k = 0; k < K; k++) {
//				System.out.print(textVecModel[k].getMean());
				System.out.printf("%f   |   %.2f   | ", textVecModel[k].getKappa(), pi[k]);
				for (int l=0; l<K; l++) System.out.printf("  %.2f", A[k][l]);
				System.out.print("   |   ");
				for (int l=0; l<data.getTextVecData().get(0).getDimension(); l++) System.out.printf("%.2f    ", textVecModel[k].getMean().getEntry(l));
				System.out.print("\n");
			}
		} else {
			System.out.print("\n\nInitial Pi and A is: \n");
			for (int k = 0; k < K; k++) {
				System.out.printf("%.2f   | ", pi[k]);
				for (int l=0; l<K; l++) System.out.printf("  %.2f", A[k][l]);
				System.out.print("\n");
			}
		}
//		RandominitPi();
//		RandominitA();
//		RandominitTextVecModel(data.getTextVecData().get(0).getDimension());
	}

	// Run k-means for the geo data to initialize the params.
	protected List<Integer>[] runKMeans(SequenceDataset data) {
		List<Double> weights = new ArrayList<Double>();
		for (int i = 0; i < data.getGeoData().size(); i++)
			weights.add(weight[i / 2]);
		KMeans kMeans = new KMeans(500);
		return kMeans.cluster(data.getGeoData(), data.getTemporalData(), weights, K);
	}

	// numDataPoints is 2R.
	protected void initPi(List<Integer>[] kMeansResults) {
		pi = new double[K];
//		System.out.print("Initial Pi is: ");
		for (int i = 0; i < K; i++) {
			List<Integer> members = kMeansResults[i];
			double numerator = 0;
			for (Integer m : members) {
				numerator += weight[m / 2];
			}
			pi[i] = numerator / (2 * weightSum);
//			System.out.print(pi[i]+ "  ");
		}
//		System.out.print("\n");
	}

	protected void RandominitPi() {
		pi = new double[K];
		for (int i = 0; i < K; i++) {
			pi[i] = Math.random();
		}
		ArrayUtils.normalize(pi);
	}

	protected void initA(List<Integer>[] kMeansResults) {
		A = new double[K][K];
		int[] dataMembership = findMemebership(kMeansResults);
		for (int r = 0; r < R; r++) {
			int fromClusterId = dataMembership[2 * r];
			int toClusterId = dataMembership[2 * r + 1];
			A[fromClusterId][toClusterId] += weight[r];
		}
		for (int i = 0; i < K; i++) {
			double rowSum = ArrayUtils.sum(A[i]);
			if (rowSum == 0) {
				System.out.println("Transition matrix row is all zeros." + i);
			}
			for (int j = 0; j < K; j++) {
				A[i][j] /= rowSum;
			}
		}
	}

	protected void RandominitA() {
		A = new double[K][K];
		double[] temp;
		for (int i = 0; i < K; i++) {
			temp = new double[K];
			for (int j = 0; j < K; j++) {
				temp[j] = Math.random();
			}
			ArrayUtils.normalize(temp);
			A[i] = temp;
		}
	}

	// Find the kmeans membership for the 2*R places
	protected int[] findMemebership(List<Integer>[] kMeansResults) {
		int[] dataMembership = new int[2 * R];
		for (int clusterId = 0; clusterId < K; clusterId++) {
			List<Integer> clusterDataIds = kMeansResults[clusterId];
			for (int dataId : clusterDataIds)
				dataMembership[dataId] = clusterId;
		}
		return dataMembership;
	}

	protected void initTextModel(List<Integer>[] kMeansResults, SequenceDataset data) {
		this.textModel = new Multinomial[K];
		for (int i = 0; i < K; i++) {
			List<Integer> dataIds = kMeansResults[i];
			List<Map<Integer, Integer>> clusterData = new ArrayList<Map<Integer, Integer>>();
			List<Double> clusterWeights = new ArrayList<Double>();
			for (int dataId : dataIds) {
				clusterData.add(data.getTextData().get(dataId));
				clusterWeights.add(weight[dataId / 2]);
			}
			textModel[i] = new Multinomial();
			textModel[i].fit(V, clusterData, clusterWeights);
		}
	}

	protected void initTextVecModel(List<Integer>[] kMeansResults, SequenceDataset data, int info_option) {
		this.textVecModel = new VMF[K];
		for (int i = 0; i < K; i++) {
			List<Integer> dataIds = kMeansResults[i];
			List<RealVector> clusterData = new ArrayList<RealVector>();
			List<Double> clusterWeights = new ArrayList<Double>();
			for (int dataId : dataIds) {
				clusterData.add(data.getTextVecData().get(dataId));
				clusterWeights.add(weight[dataId / 2]);
			}
			textVecModel[i] = new VMF();
			textVecModel[i].fit(clusterData, clusterWeights, info_option);
		}
	}

	protected void RandominitTextVecModel() {
		this.textVecModel = new VMF[K];
		for (int i = 0; i < K; i++) {
			textVecModel[i] = new VMF();
			textVecModel[i].randominit(embedding_dim);
		}
	}

	protected void initTextGaussianModel(List<Integer>[] kMeansResults, SequenceDataset data) {
		this.textGaussianModel = new Gaussian[K][embedding_dim]; // It should be dimension of word embeddings
		this.c_text = new double[K][embedding_dim]; // It should be dimension of word embeddings
		for (int k = 0; k < K; k++) {
			List<RealVector> clusterData = new ArrayList<RealVector>();
			List<Double> clusterWeights = new ArrayList<Double>();
			List<Integer> dataIds = kMeansResults[k];
			for (int dataId : dataIds) {
				clusterData.add(data.getTextVecDatum(dataId));
				clusterWeights.add(weight[dataId / 2]);
			}
			KMeans kMeans = new KMeans(500);
			List<Integer>[] subKMeansResults = kMeans.cluster(clusterData, clusterWeights, embedding_dim);
			for (int m = 0; m < embedding_dim; m++) {
				List<Integer> subDataIds = subKMeansResults[m];
				List<RealVector> subClusterData = new ArrayList<RealVector>();
				List<Double> subClusterWeights = new ArrayList<Double>();
				for (int dataId : subDataIds) {
					subClusterData.add(clusterData.get(dataId));
					subClusterWeights.add(clusterWeights.get(dataId));
				}
				textGaussianModel[k][m] = new Gaussian();
				textGaussianModel[k][m].fit(subClusterData, subClusterWeights);
				c_text[k][m] = ArrayUtils.sum(subClusterWeights) / ArrayUtils.sum(clusterWeights);
			}
		}
	}

	protected void initTextIndependentGaussianModel(List<Integer>[] kMeansResults, SequenceDataset data) {
		this.textIndependentGaussianModel = new Gaussian[K][embedding_dim]; // K states, each having M components
		for (int k = 0; k < K; k++) {
			for (int i = 0; i < embedding_dim; i++){
				List<RealVector> clusterData = new ArrayList<RealVector>();
				List<Double> clusterWeights = new ArrayList<Double>();
				List<Integer> dataIds = kMeansResults[k];
				for (int dataId : dataIds) {
					clusterData.add(data.getTextVecDatum(dataId).getSubVector(i, 1));
					clusterWeights.add(weight[dataId / 2]);
				}
				textIndependentGaussianModel[k][i] = new Gaussian();
				textIndependentGaussianModel[k][i].fit(clusterData, clusterWeights);
			}
		}
	}

	// Initialize the geo model and c
	protected void initGeoModel(List<Integer>[] kMeansResults, SequenceDataset data) {
		this.geoModel = new Gaussian[K][M]; // K states, each having M components
		this.c = new double[K][M];
		for (int k = 0; k < K; k++) {
			List<RealVector> clusterData = new ArrayList<RealVector>();
			List<Double> clusterWeights = new ArrayList<Double>();
			List<Integer> dataIds = kMeansResults[k];
			for (int dataId : dataIds) {
				clusterData.add(data.getGeoDatum(dataId));
				clusterWeights.add(weight[dataId / 2]);
			}
			KMeans kMeans = new KMeans(500);
			List<Integer>[] subKMeansResults = kMeans.cluster(clusterData, clusterWeights, M);
			for (int m = 0; m < M; m++) {
				List<Integer> subDataIds = subKMeansResults[m];
				List<RealVector> subClusterData = new ArrayList<RealVector>();
				List<Double> subClusterWeights = new ArrayList<Double>();
				for (int dataId : subDataIds) {
					subClusterData.add(clusterData.get(dataId));
					subClusterWeights.add(clusterWeights.get(dataId));
				}
				geoModel[k][m] = new Gaussian();
				geoModel[k][m].fit(subClusterData, subClusterWeights);
				c[k][m] = ArrayUtils.sum(subClusterWeights) / ArrayUtils.sum(clusterWeights);
			}
		}
	}

	// Initialize the temporal model and c
	protected void initTemporalModel(List<Integer>[] kMeansResults, SequenceDataset data) {
		this.temporalModel = new Gaussian[K]; // K states, each having M components
		for (int k = 0; k < K; k++) {
			List<RealVector> clusterData = new ArrayList<RealVector>();
			List<Double> clusterWeights = new ArrayList<Double>();
			List<Integer> dataIds = kMeansResults[k];
			for (int dataId : dataIds) {
				clusterData.add(data.getTemporalDatum(dataId));
				clusterWeights.add(weight[dataId / 2]);
			}
			temporalModel[k] = new Gaussian();
			temporalModel[k].fit(clusterData, clusterWeights);
		}
	}

	/**
	 * Step 2: iterate over the e-step and m-step.
	 */
	protected void iterate(SequenceDataset data, int info_option) {
		double prevLL = totalLL;
		for (int iter = 0; iter < maxIter; iter++) {
			eStep(data, info_option);
			mStep(data, info_option);
			calcTotalLL();

			if (info_option != 30 && info_option !=31) {
				System.out.println("HMM finished iteration " + iter + ". Log-likelihood:" + totalLL);
			} else {  // Synthetic Data...
//				System.out.println("HMM finished iteration " + iter + ". Log-likelihood:" + totalLL);
				if (Double.isNaN(totalLL)){
					restart = true;
					break;
				}
				if (iter == maxIter)
					restart = true;
			}
			if (Math.abs(totalLL - prevLL) <= 0.01)
				break;
			prevLL = totalLL;
		}
	}

	/**
	 * Step 2.1: learning the parameters using EM: E-Step.
	 */
	protected void eStep(SequenceDataset data, int info_option) {
		calcLL(data, info_option);
		scaleLL();
		calcAlpha();
		calcBeta();
		calcGamma();
		calcXi();
		if ((info_option == 1) || (info_option == 2) || (info_option == 4) || (info_option == 6) || (info_option == 11) || (info_option == 12) || (info_option == 14) || (info_option == 16) || (info_option == 21) || (info_option == 22)) // Only for Geo information
			calcRho(data);
		if (info_option == 21)
			calcRho_text(data);
	}

	// Compute the log likelihood.
	protected void calcLL(SequenceDataset data, int info_option) {
		for (int r = 0; r < R; r++)
			for (int n = 0; n < 2; n++)
				for (int k = 0; k < K; k++)
					if (info_option > 10 && info_option <= 20) {
						ll[r][n][k] = calcLLState(data.getGeoDatum(2 * r + n), data.getTemporalDatum(2 * r + n), data.getTextDatum(2 * r + n), k, info_option);
					} else {
						ll[r][n][k] = calcLLVecState(data.getGeoDatum(2 * r + n), data.getTemporalDatum(2 * r + n), data.getTextVecDatum(2 * r + n), k, info_option);
					}
	}

	protected void scaleLL() {
		// Find the scaling factors.
		for (int r = 0; r < R; r++)
			for (int n = 0; n < 2; n++)
				scalingFactor[r][n] = ArrayUtils.max(ll[r][n]);
		// Scale the log-likelihood.
		for (int r = 0; r < R; r++)
			for (int n = 0; n < 2; n++)
				for (int k = 0; k < K; k++)
					ll[r][n][k] -= scalingFactor[r][n];
	}

	protected void calcAlpha() {
		// Compute alpha[r][0][k], in the log domain!
		for (int r = 0; r < R; r++)
			for (int k = 0; k < K; k++)
				alpha[r][0][k] = log(pi[k]) + ll[r][0][k];
		// Compute con[r][0], namely ln p(x_0)
		for (int r = 0; r < R; r++)
			con[r][0] = ArrayUtils.sumExpLog(alpha[r][0]);
		// Normalize alpha[r][0][k]
		for (int r = 0; r < R; r++)
			ArrayUtils.logNormalize(alpha[r][0]);

		// Compute alpha[r][n][k], again in the log domain.
		for (int n = 1; n < N; n++) {
			for (int r = 0; r < R; r++) {
				for (int k = 0; k < K; k++) {
					alpha[r][n][k] = ll[r][n][k];
					double sum = 1e-200;
					for (int j = 0; j < K; j++) {
						sum += alpha[r][n-1][j] * A[j][k];
					}
					alpha[r][n][k] += log(sum);
				}
			}
			// Compute con[r][1], namely ln p(x_1 | x_0)
			for (int r = 0; r < R; r++)
				con[r][n] = ArrayUtils.sumExpLog(alpha[r][n]);
			// Normalize alpha[r][1][k]
			for (int r = 0; r < R; r++)
				ArrayUtils.logNormalize(alpha[r][n]);
		}

//		// Compute alpha[r][0][k], in the log domain!
//		for (int r = 0; r < R; r++)
//			for (int k = 0; k < K; k++)
//				alpha[r][0][k] = log(pi[k]) + ll[r][0][k];
//		// Compute con[r][0], namely ln p(x_0)
//		for (int r = 0; r < R; r++)
//			con[r][0] = ArrayUtils.sumExpLog(alpha[r][0]);
//		// Normalize alpha[r][0][k]
//		for (int r = 0; r < R; r++)
//			ArrayUtils.logNormalize(alpha[r][0]);
//		// Compute alpha[r][1][k], again in the log domain.
//		for (int r = 0; r < R; r++) {
//			for (int k = 0; k < K; k++) {
//				alpha[r][1][k] = ll[r][1][k];
//				double sum = 1e-200;
//				for (int j = 0; j < K; j++) {
//					sum += alpha[r][0][j] * A[j][k];
//				}
//				alpha[r][1][k] += log(sum);
//			}
//		}
//		// Compute con[r][1], namely ln p(x_1 | x_0)
//		for (int r = 0; r < R; r++)
//			con[r][1] = ArrayUtils.sumExpLog(alpha[r][1]);
//		// Normalize alpha[r][1][k]
//		for (int r = 0; r < R; r++)
//			ArrayUtils.logNormalize(alpha[r][1]);
	}

	protected void calcBeta() {
		// Compute beta[r][1][k]
		for (int r = 0; r < R; r++)
			for (int k = 0; k < K; k++)
				beta[r][1][k] = 1.0;
		// Compute beta[r][0][k]
		for (int r = 0; r < R; r++) {
			for (int k = 0; k < K; k++) {
				double sum = 0;
				for (int j = 0; j < K; j++) {
					if (A[k][j] == 0)
						sum += 0;
					else if (ll[r][1][j] - con[r][1] >= 500)
						sum += A[k][j] * 1e200;
					else
						sum += exp(ll[r][1][j] - con[r][1]) * A[k][j];
				}
				beta[r][0][k] = sum;
			}
		}
	}

	protected void calcGamma() {
		for (int r = 0; r < R; r++)
			for (int n = 0; n < 2; n++)
				for (int k = 0; k < K; k++)
					gamma[r][n][k] = alpha[r][n][k] * beta[r][n][k];
	}

	protected void calcXi() {
		for (int r = 0; r < R; r++)
			for (int   j = 0; j < K; j++)
				for (int k = 0; k < K; k++)
					xi[r][j][k] = alpha[r][0][j] * exp(ll[r][1][k] - con[r][1]) * A[j][k] * beta[r][1][k];
	}

	protected void calcRho(SequenceDataset data) {
		for (int r = 0; r < R; r++) {
			for (int n = 0; n < 2; n++) {
				RealVector v = data.getGeoDatum(2 * r + n);
				for (int k = 0; k < K; k++) {
					for (int m = 0; m < M; m++)
						rho[r][n][k][m] = calcGeoLLComponent(v, k, m); // Log domain.
					ArrayUtils.logNormalize(rho[r][n][k]); // Transform to normal domain.
					for (int m = 0; m < M; m++)
						rho[r][n][k][m] = gamma[r][n][k] * rho[r][n][k][m];
				}
			}
		}
	}

	protected void calcRho_text(SequenceDataset data) {
		for (int r = 0; r < R; r++) {
			for (int n = 0; n < 2; n++) {
				RealVector v = data.getTextVecDatum(2 * r + n);
				for (int k = 0; k < K; k++) {
					for (int m = 0; m < embedding_dim; m++)
						rho_text[r][n][k][m] = calcTextGaussianLLComponent(v, k, m); // Log domain.
					ArrayUtils.logNormalize(rho_text[r][n][k]); // Transform to normal domain.
					for (int m = 0; m < embedding_dim; m++)
						rho_text[r][n][k][m] = gamma[r][n][k] * rho_text[r][n][k][m];
				}
			}
		}
	}

	/**
	 * Step 2.2: learning the parameters using EM: M-Step.
	 */
	protected void mStep(SequenceDataset data, int info_option) {
		updatePi();
		updateA();
		//1 is with all information; 2 is without text; 3 is without location; 4 is without time; 5 is with only text; 6 is with only location; 7 is with only time.
		if ((info_option == 1) || (info_option == 3) || (info_option == 4) || (info_option == 5) || info_option == 30 || info_option ==31)
			updateTextVecModel(data, info_option);
		if ((info_option == 11) || (info_option == 13) || (info_option == 14) || (info_option == 15))
			updateTextModel(data);
		if (info_option == 21)
			updateTextGaussianModel(data);
		if (info_option == 22)
			updateTextIndependentGaussianModel(data);
		if ((info_option == 1) || (info_option == 2) || (info_option == 4) || (info_option == 6) || (info_option == 11) || (info_option == 12) || (info_option == 14) || (info_option == 16) || (info_option == 21) || (info_option == 22))
			updateGeoModel(data);
		if ((info_option == 1) || (info_option == 2) || (info_option == 3) || (info_option == 7) || (info_option == 11) || (info_option == 12) || (info_option == 13) || (info_option == 17) || (info_option == 21) || (info_option == 22))
			updateTemporalModel(data);
	}

	protected void updatePi() {
//		System.out.printf("Updated Pi is: ");
		for (int k = 0; k < K; k++) {
			double numerator = 0;
			for (int r = 0; r < R; r++) {
				numerator += gamma[r][0][k] * weight[r];
			}
			pi[k] = numerator / weightSum;
//			System.out.printf("%.2f   ", pi[k]);
		}
	}

	protected void updateA() {
		for (int j = 0; j < K; j++) {
			double denominator = 0;
			for (int r = 0; r < R; r++)
				for (int k = 0; k < K; k++)
					denominator += weight[r] * xi[r][j][k];
			for (int k = 0; k < K; k++) {
				double numerator = 0;
				for (int r = 0; r < R; r++) {
					numerator += weight[r] * xi[r][j][k];
				}
				if (denominator == 0)
					denominator = 1e-200; // Otherwise the results will become NaN
				A[j][k] = numerator / denominator;
			}
		}
	}

	protected void updateTextModel(SequenceDataset data) {
		for (int k = 0; k < K; k++) {
			List<Double> textWeights = new ArrayList<Double>();
			for (int r = 0; r < R; r++)
				for (int n = 0; n < 2; n++)
					textWeights.add(weight[r] * gamma[r][n][k]);
			textModel[k].fit(V, data.getTextData(), textWeights);
		}
	}

	protected void updateTextVecModel(SequenceDataset data, int info_option) {
		System.out.printf("Updated Kappa is: ");
		for (int k = 0; k < K; k++) {
			List<Double> textWeights = new ArrayList<Double>();
			for (int r = 0; r < R; r++)
				for (int n = 0; n < N; n++)
					textWeights.add(weight[r] * gamma[r][n][k]);
//			double sum = 0;
//			for(double i : textWeights) sum += i;
//			if (sum>5)
			textVecModel[k].fit(data.getTextVecData(), textWeights, info_option);
			System.out.printf("%.2f   ", textVecModel[k].getKappa());
		}
	}

	protected void updateTextGaussianModel(SequenceDataset data) {
		updateC_text();
		for (int k = 0; k < K; k++) {
			for (int m = 0; m < embedding_dim; m++) {
				List<Double> weights = new ArrayList<Double>();
				for (int r = 0; r < R; r++)
					for (int n = 0; n < 2; n++)
						weights.add(weight[r] * rho_text[r][n][k][m]);
				textGaussianModel[k][m].fit(data.getTextVecData(), weights);
			}
		}
	}

	protected void updateC_text() {
		for (int k = 0; k < K; k++) {
			double denominator = 0;
			for (int r = 0; r < R; r++)
				for (int n = 0; n < 2; n++)
					denominator += weight[r] * gamma[r][n][k];
			for (int m = 0; m < embedding_dim; m++) {
				double numerator = 0;
				for (int r = 0; r < R; r++)
					for (int n = 0; n < 2; n++)
						numerator += weight[r] * rho_text[r][n][k][m];
				c_text[k][m] = numerator / denominator;
			}
		}
	}

	protected void updateTextIndependentGaussianModel(SequenceDataset data) {
		for (int k = 0; k < K; k++) {
			List<Double> weights = new ArrayList<Double>();
			for (int r = 0; r < R; r++)
				for (int n = 0; n < 2; n++)
					weights.add(weight[r] * gamma[r][n][k]);
			for (int i = 0; i < embedding_dim; i++){
				List<RealVector> clusterData = new ArrayList<RealVector>();
				for (int dataId = 0; dataId < data.getTextVecData().size(); dataId++)
					clusterData.add(data.getTextVecDatum(dataId).getSubVector(i, 1));
				textIndependentGaussianModel[k][i].fit(clusterData, weights);
			}
		}
	}

	protected void updateGeoModel(SequenceDataset data) {
		updateC();
		for (int k = 0; k < K; k++) {
			for (int m = 0; m < M; m++) {
				List<Double> weights = new ArrayList<Double>();
				for (int r = 0; r < R; r++)
					for (int n = 0; n < 2; n++)
						weights.add(weight[r] * rho[r][n][k][m]);
				geoModel[k][m].fit(data.getGeoData(), weights);
			}
		}
	}

	protected void updateC() {
		for (int k = 0; k < K; k++) {
			double denominator = 0;
			for (int r = 0; r < R; r++)
				for (int n = 0; n < 2; n++)
					denominator += weight[r] * gamma[r][n][k];
			for (int m = 0; m < M; m++) {
				double numerator = 0;
				for (int r = 0; r < R; r++)
					for (int n = 0; n < 2; n++)
						numerator += weight[r] * rho[r][n][k][m];
				c[k][m] = numerator / denominator;
			}
		}
	}

	protected void updateTemporalModel(SequenceDataset data) {
		for (int k = 0; k < K; k++) {
			List<Double> weights = new ArrayList<Double>();
			for (int r = 0; r < R; r++)
				for (int n = 0; n < 2; n++)
					weights.add(weight[r] * gamma[r][n][k]);
			temporalModel[k].fit(data.getTemporalData(), weights);
		}
	}

	/**
	 * Step 3: Evaluate Synthetic data.
	 */
	protected void evaluate(SequenceDataset data, int info_option) {
		if ((info_option == 1) || (info_option == 3) || (info_option == 4) || (info_option == 5)) {
			System.out.print("\n\nEstimated Kappa, Pi, A and Mean is: \n");
			for (int k = 0; k < K; k++) {
//			System.out.print(textVecModel[k].getMean());
				System.out.printf("%d: %f   |   %.2f   | ", k+1, textVecModel[k].getKappa(), pi[k]);
				for (int l=0; l<K; l++) System.out.printf("  %.2f", A[k][l]);
				System.out.printf("   |  ");
				for (int l=0; l<data.getTextVecData().get(0).getDimension(); l++) System.out.printf("%.2f    ", textVecModel[k].getMean().getEntry(l));
				System.out.print("\n");
			}
		} else {
			System.out.print("\n\nEstimated Pi and A is: \n");
			for (int k = 0; k < K; k++) {
				System.out.printf("%d: %.2f   | ", k+1, pi[k]);
				for (int l=0; l<K; l++) System.out.printf("  %.2f", A[k][l]);
				System.out.print("\n");
			}
		}
//		System.out.print("The ground truth Mean, Kappa, Pi and A is: \n");
//		for (int k = 0; k < K; k++) {
//			System.out.print(data.getmuDatum(k) + "   |  " + data.getkappaDatum(k) + "  |  " + data.getIniPiDatum(k) + "   |  " + data.getTransProbDatum(k) + "\n");
//		}
//
//		int[] Idx = GetMatchingIndex(data);
//		EvaluateMeanMse(data,Idx);
//		EvaluateKappaMse(data,Idx);
//		EvaluateIniPiMse(data,Idx);
//		EvaluateAMse(data,Idx);
	}

	protected void evaluate_synthetic(SequenceDataset data, String filename) throws IOException{
		int[] Idx = GetMatchingIndex(data);
		if (!restart){ // Matching index is good.
			double MeanMse = EvaluateMeanMse(data,Idx);
			double KappaMse = EvaluateKappaMse(data,Idx);
			double IniPiMse = EvaluateIniPiMse(data,Idx);
			double AMse = EvaluateAMse(data,Idx);

			System.out.print("\n\nEstimated Kappa, Pi, A and Mean is: \n");
			for (int k = 0; k < K; k++) {
//			System.out.print(textVecModel[k].getMean());
				System.out.printf("%f   |   %.2f   | ", textVecModel[k].getKappa(), pi[k]);
				for (int l=0; l<K; l++) System.out.printf("  %.2f", A[k][l]);
				System.out.print("   |   ");
				for (int l=0; l<data.getTextVecData().get(0).getDimension(); l++) System.out.printf("%.2f    ", textVecModel[k].getMean().getEntry(l));
				System.out.print("\n");
			}

			System.out.print("The ground truth Kappa, Pi, A and Mean is: \n");
			for (int k = 0; k < K; k++) {
				System.out.print(data.getkappaDatum(k) + "  |  " + data.getIniPiDatum(k) + "   |  " + data.getTransProbDatum(k)+ "   |  " + data.getmuDatum(k)  + "\n");
			}

			System.out.printf("\nSum of Mean abs error is:        %f", MeanMse);
			System.out.printf("\nSum of Kappa abs error is:       %f", KappaMse);
			System.out.printf("\nSum of Initial Pi abs error is:  %f", IniPiMse);
			System.out.printf("\nSum of Trans Prob abs error is:  %f", AMse);

			WriteToFile(MeanMse, KappaMse, IniPiMse, AMse, data, Idx, filename);
		}
	}

	protected int[] GetMatchingIndex(SequenceDataset data){
		double MeanMse[] = new double[K*K];
		int[] index = new int[K];
		int temp_index;
		for (int l = 0; l < K; l++)
			for (int k = 0; k < K; k++)
				MeanMse[l*K+k] = (data.getmuDatum(l).subtract(textVecModel[k].getMean())).getNorm();

		for (int k = 0; k < K; k++) {
			temp_index = minIndex(MeanMse);
			index[temp_index / K] = temp_index - K * (temp_index / K);
			for (int l = 0; l < K; l++)
				MeanMse[K * (temp_index / K)+l] = 1000;
		}

		CheckDuplicate(index);
		if (!restart) {
			System.out.print("\nMatching Index is: ");
			for (int k = 0; k < K; k++)
				System.out.print(index[k] + " ");
		}
		return index;

	}

	protected void CheckDuplicate(int[] Idx){
		Boolean duplicates=false;
		for (int j=0;j<Idx.length;j++)
			for (int k=j+1;k<Idx.length;k++)
				if (k!=j && Idx[k] == Idx[j])
					duplicates=true;
		if (duplicates)
			restart=true;
	}

	protected int minIndex(double... ds) {
		int idx = -1;
		double d= Double.POSITIVE_INFINITY;
		for(int i = 0; i < ds.length; i++)
			if(ds[i] < d) {
				d = ds[i];
				idx = i;
			}
		return idx;
	}

	protected double EvaluateMeanMse(SequenceDataset data, int[] Idx){
		double MeanMse = 0;
		for (int l = 0; l < K; l++) {
//			MeanMse += (data.getmuDatum(l).subtract(textVecModel[Idx[l]].getMean())).getNorm();
			MeanMse += (data.getmuDatum(l).subtract(textVecModel[Idx[l]].getMean())).getL1Norm();
		}
		return MeanMse;
	}

	protected double EvaluateKappaMse(SequenceDataset data, int[] Idx){
		double KappaMse = 0;
		for (int l = 0; l < K; l++)
			KappaMse += Math.abs(data.getkappaDatum(l).getEntry(0) - textVecModel[Idx[l]].getKappa());
		return KappaMse;
	}

	protected double EvaluateIniPiMse(SequenceDataset data, int[] Idx){
		double IniPiMse = 0;
		for (int l = 0; l < K; l++)
			IniPiMse += Math.abs(data.getIniPiDatum(l).getEntry(0) - pi[Idx[l]]);
		return IniPiMse;
	}

	protected double EvaluateAMse(SequenceDataset data, int[] Idx){
		double AMse = 0;
		for (int l = 0; l < K; l++)
			for (int k = 0; k < K; k++)
				AMse += Math.abs(data.getTransProbDatum(l).getEntry(k)-A[Idx[l]][Idx[k]]);
		return AMse;
	}

	protected void WriteToFile(SequenceDataset data, int info_option, String filename) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
		bw.write("\nTraining time is: " + elapsedTime);
		bw.write("\nGeo information is: [");
		for (int k = 0; k < K; k++)
			bw.write("(" + String.format("%.6f", geoModel[k][0].getMean().getEntry(1)) + ", " + String.format("%.6f", geoModel[k][0].getMean().getEntry(0)) + "), ");
		bw.write("]\n");

		if ((info_option == 1) || (info_option == 3) || (info_option == 4) || (info_option == 5))
			WriteEstimatedPara(bw, data.getTextVecData().get(0).getDimension());

//		WriteGroundTruthPara(bw, data);
//		bw.write("Sum of Mean abs error is:        " + MeanMse);
//		bw.write("\nSum of Kappa abs error is:       " + KappaMse);
//		bw.write("\nSum of Initial Pi abs error is:  "+ IniPiMse);
//		bw.write("\nSum of Trans Prob abs error is:  "+ AMse);
//		bw.write("\nMatching Index is:  ");
//		for (int k=0; k < Idx.length; k++) bw.write(Idx[k] + " ");
//		bw.write("\nNumber of restart is: "+ num_restart);
//		bw.write("\nLength of the HMM is: "+ N);
//		bw.write("\nNumber of latent states is: "+ K);
//		bw.write("\nDimension of VMF is: "+ data.getTextVecData().get(0).getDimension());
//		bw.write("\nNumber of sequences is: "+ R);
		bw.close();
	}

	protected void WriteToFile(double MeanMse, double KappaMse, double IniPiMse, double AMse, SequenceDataset data, int[] Idx, String filename) throws IOException{
		BufferedWriter bw = new BufferedWriter(new FileWriter(filename, true));
//		WriteEstimatedPara(bw, data.getTextVecData().get(0).getDimension());
		WriteGroundTruthPara(bw, data);
//
		bw.write("\nSum of Mean abs error is:        " + MeanMse);
		bw.write("\nSum of Kappa abs error is:       " + KappaMse);
		bw.write("\nSum of Initial Pi abs error is:  "+ IniPiMse);
		bw.write("\nSum of Trans Prob abs error is:  "+ AMse);
//		bw.write("\nMatching Index is:  ");
//		for (int k=0; k < Idx.length; k++) bw.write(Idx[k] + " ");
//		bw.write("\nNumber of restart is: "+ num_restart);
		bw.write("\nLength of the HMM is: "+ N);
		bw.write("\nNumber of latent states is: "+ K);
		bw.write("\nDimension of VMF is: "+ data.getTextVecData().get(0).getDimension());
		bw.write("\nNumber of sequences is: "+ R);
		bw.close();
	}

	protected void WriteEstimatedPara(BufferedWriter bw, int dimension) throws IOException{
		bw.write("Estimated Kappa and Mean is: \n");
		for (int k = 0; k < K; k++)
			bw.write(textVecModel[k].getKappa() + " ");
		bw.write("\n");
		for (int k = 0; k < K; k++)
			bw.write(pi[k] + " ");
		bw.write("\n");
		for (int k = 0; k < K; k++){
			for (int l=0; l<K; l++)
				bw.write(A[k][l] + " ");
			bw.write("\n");
		}
		for (int k = 0; k < K; k++) {
			for (int l = 0; l < dimension; l++)
				bw.write(textVecModel[k].getMean().getEntry(l) + " ");
			bw.write("\n");
		}
	}

	protected void WriteGroundTruthPara(BufferedWriter bw, SequenceDataset data) throws IOException{
		bw.write("\nThe ground truth Kappa, Pi, A and Mean is: \n");
		for (int k = 0; k < K; k++)
			bw.write(data.getkappaDatum(k).getEntry(0) + " ");
		bw.write("\n");
		for (int k = 0; k < K; k++)
			bw.write(data.getIniPiDatum(k).getEntry(0) + " ");
		bw.write("\n");
		for (int k = 0; k < K; k++){
			for (int l=0; l<K; l++)
				bw.write(data.getTransProbDatum(k).getEntry(l) + " ");
			bw.write("\n");
		}
		for (int k = 0; k < K; k++){
			for (int l = 0; l < data.getTextVecData().get(0).getDimension(); l++)
				bw.write(data.getmuDatum(k).getEntry(l) + " ");
			bw.write("\n");
		}
	}

	/**
	 * Functions for computing probabilities
	 */
	// Calc the likelihood that the given data is generated from state k
	protected double calcLLState(RealVector geoDatum, RealVector temporalDatum, Map<Integer, Integer> textDatum,
			int k, int info_option) {
		return calcLLState(geoDatum, temporalDatum, textDatum, k, false, info_option);
	}

	protected double calcLLVecState(RealVector geoDatum, RealVector temporalDatum, RealVector textVecDatum,
								 int k, int info_option) {
		return calcLLVecState(geoDatum, temporalDatum, textVecDatum, k, false, info_option);
	}

	protected double calcLLState(RealVector geoDatum, RealVector temporalDatum, Map<Integer, Integer> textDatum, int k,
								 boolean isTest, int info_option) {
		double textProb = 0;
		double geoProb = 0;
		double temporalProb = 0;
		// 1 is with all information; 2 is without text; 3 is without location; 4 is without time; 5 is with only text; 6 is with only location; 7 is with only time.
		if ((info_option == 11) || (info_option == 13) || (info_option == 14) || (info_option == 15))
			textProb = textModel[k].calcLL(textDatum, isTest);
		if ((info_option == 11) || (info_option == 12) || (info_option == 14) || (info_option == 16))
			geoProb = calcGeoLLState(geoDatum, k);
		if ((info_option == 11) || (info_option == 12) || (info_option == 13) || (info_option == 17))
			temporalProb = temporalModel[k].calcLL(temporalDatum);
		return geoProb + temporalProb + textProb;
	}

//	protected double calcLLVecState(RealVector geoDatum, RealVector temporalDatum, RealVector textVecDatum, int k,
//									boolean isTest) {
//		double textVecProb = textVecModel[k].calcLL(textVecDatum);
//		double geoProb = calcGeoLLState(geoDatum, k);
//		double temporalProb = temporalModel[k].calcLL(temporalDatum);
//		return geoProb + temporalProb + textVecProb;
//	}

	protected double calcLLVecState(RealVector geoDatum, RealVector temporalDatum, RealVector textVecDatum, int k,
									boolean isTest, int info_option) {
		double textVecProb = 0;
		double geoProb = 0;
		double temporalProb = 0;
		// 1 is with all information; 2 is without text; 3 is without location; 4 is without time; 5 is with only text; 6 is with only location; 7 is with only time.
		if ((info_option == 1) || (info_option == 3) || (info_option == 4) || (info_option == 5) || (info_option == 30) || (info_option == 31))
			textVecProb = textVecModel[k].calcLL(textVecDatum);
		if ((info_option == 1) || (info_option == 2) || (info_option == 4) || (info_option == 6) || (info_option == 21) || (info_option == 22))
			geoProb = calcGeoLLState(geoDatum, k);
		if ((info_option == 1) || (info_option == 2) || (info_option == 3) || (info_option == 7) || (info_option == 21) || (info_option == 22))
			temporalProb = temporalModel[k].calcLL(temporalDatum);
		if (info_option == 21)
			textVecProb = calcTextGaussianLLState(textVecDatum, k);
		if (info_option == 22){
			for (int i = 0; i < embedding_dim; i++)
				textVecProb += textIndependentGaussianModel[k][i].calcLL(textVecDatum.getSubVector(i, 1));
		}

		return geoProb + temporalProb + textVecProb;
	}

	protected double calcTextGaussianLLState(RealVector v, int k) {
		double[] lnProb = new double[embedding_dim];
		for (int m = 0; m < embedding_dim; m++)
			lnProb[m] = calcTextGaussianLLComponent(v, k, m);
		double maxLnProb = ArrayUtils.max(lnProb);
		for (int m = 0; m < embedding_dim; m++)
			lnProb[m] -= maxLnProb;
		double sum = 0;
		for (int m = 0; m < embedding_dim; m++)
			sum += exp(lnProb[m]);
		return maxLnProb + log(sum);
	}

	protected double calcTextGaussianLLComponent(RealVector v, int k, int m) {
		double prior = c_text[k][m];
		double logGeoProb = textGaussianModel[k][m].calcLL(v);
		return log(prior) + logGeoProb;
	}

	// Calc the probability that v is generated from the gmm of state k.
	protected double calcGeoLLState(RealVector v, int k) {
		double[] lnProb = new double[M];
		for (int m = 0; m < M; m++)
			lnProb[m] = calcGeoLLComponent(v, k, m);
		double maxLnProb = ArrayUtils.max(lnProb);
		for (int m = 0; m < M; m++)
			lnProb[m] -= maxLnProb;
		double sum = 0;
		for (int m = 0; m < M; m++)
			sum += exp(lnProb[m]);
		return maxLnProb + log(sum);
	}

	// Compute the prob that v is generated from the m-th component of state k.
	protected double calcGeoLLComponent(RealVector v, int k, int m) {
		double prior = c[k][m];
		double logGeoProb = geoModel[k][m].calcLL(v);
		return log(prior) + logGeoProb;
	}

	protected void calcTotalLL() {
		totalLL = 0;
		for (int r = 0; r < R; r++) {
			double hmmLL = 0;
			for (int n = 0; n < 2; n++)
				hmmLL += con[r][n] + scalingFactor[r][n];
			totalLL += weight[r] * hmmLL;
		}
	}

	/**
	 * Functions for output.
	 */
	public String toString(WordDataset wd) {

		// Write K M.
		String s = "# K M\n";
		s += K + " " + M + "\n";

		// Write Pi.
		s += "# Pi\n";
		for (int i = 0; i < K; i++)
			s += pi[i] + " ";
		s += "\n";

		// Write A.
		s += "# Transition\n";
		for (int j = 0; j < K; j++) {
			for (int k = 0; k < K; k++) {
				s += A[j][k] + " ";
			}
			s += "\n";
		}

		// Write geo model.
		s += "# geo\n";
		for (int k = 0; k < K; k++) {
			for (int m = 0; m < M; m++) {
				s += k + "," + m + "," + pi[k] + "," + c[k][m] + ",";
				RealVector mean = geoModel[k][m].getMean();
				s += mean.getEntry(0) + "," + mean.getEntry(1) + ",";
				RealMatrix var = geoModel[k][m].getVar();
				s += var.getEntry(0, 0) + "," + var.getEntry(0, 1) + "," + var.getEntry(1, 0) + "," + var.getEntry(1, 1)
						+ "\n";
			}
		}

		// write temporal model.
		s += "# temporal\n";
		for (int i = 0; i < K; i++) {
			RealVector mean = temporalModel[i].getMean();
			RealMatrix var = temporalModel[i].getVar();
			s += mean.getEntry(0) + " ";
			s += var.getEntry(0, 0) + "\n";
		}

		// Write text model.
		s += "# text\n";
		for (int i = 0; i < K; i++) {
			s += "------------------------------ State " + i + "------------------------------\n";
			s += textModel[i].getWordDistribution(wd, 20) + "\n"; // Output the top 20 words.
		}
		return s;
	}

	// Load from a model file.
	public static HMM load(String inputFile) throws Exception {
		ObjectInputStream objectinputstream = new ObjectInputStream(new FileInputStream(inputFile));
		HMM m = (HMM) objectinputstream.readObject();
		objectinputstream.close();
		return m;
	}

	// Serialize
	public void serialize(String serializeFile) throws Exception {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializeFile));
		oos.writeObject(this);
		oos.close();
	}

	public void write(WordDataset wd, String outputFile) throws Exception {
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false));
		bw.append(this.toString(wd));
		bw.close();
	}

	/**
	 * Compute the ll of a test sequence.
	 */
//	public double calcLL(List<RealVector> geo, List<RealVector> temporal, List<Map<Integer, Integer>> text) {
//		return calcLL(geo, temporal, text, false);
//	}

//	public double calcVecLL(List<RealVector> geo, List<RealVector> temporal, List<RealVector> textVec) {
//		return calcVecLL(geo, temporal, textVec, false);
//	}

	public double calcLL(List<RealVector> geo, List<RealVector> temporal, List<Map<Integer, Integer>> text,
			boolean isTest, int info_option) {
		double[][] ll = new double[2][K]; // ll[n][k] is the log-likelihood p(x[n]|k).
		double[] scalingFactor = new double[2]; // scalingFactor[n] is chosen from ll[n].
		double[][] alpha = new double[2][K]; // alpha[n][k] is for the n-th position of sequence r at state k.
		double[] con = new double[2]; // con[n] is ln p(x_n | x_1, x_2, ... x_n-1), this is used for normalization.
		// calc LL
		for (int n = 0; n < 2; n++)
			for (int k = 0; k < K; k++)
				ll[n][k] = calcLLState(geo.get(n), temporal.get(n), text.get(n), k, isTest, info_option);
		// Find the scaling factors.
		for (int n = 0; n < 2; n++)
			scalingFactor[n] = ArrayUtils.max(ll[n]);
		// Scale the log-likelihood.
		for (int n = 0; n < 2; n++)
			for (int k = 0; k < K; k++)
				ll[n][k] -= scalingFactor[n];
		// Compute alpha[0][k], in the log domain!
		for (int k = 0; k < K; k++)
			alpha[0][k] = log(pi[k]) + ll[0][k];
		// Compute con[0], namely ln p(x_0)
		con[0] = ArrayUtils.sumExpLog(alpha[0]);
		// Normalize alpha[0][k]
		ArrayUtils.logNormalize(alpha[0]);
		// Compute alpha[1][k], again in the log domain.
		for (int k = 0; k < K; k++) {
			alpha[1][k] = ll[1][k];
			double sum = 1e-200;
			for (int j = 0; j < K; j++) {
				sum += alpha[0][j] * A[j][k];
			}
			alpha[1][k] += log(sum);
		}
		// Compute con[1], namely ln p(x_1 | x_0)
		con[1] = ArrayUtils.sumExpLog(alpha[1]);
		// the result ll.
		double hmmLL = 0;
		for (int n = 0; n < 2; n++)
			hmmLL += con[n] + scalingFactor[n];
		return hmmLL;
	}

	public double calcVecLL(List<RealVector> geo, List<RealVector> temporal, List<RealVector> textVec,
						 boolean isTest, int info_option) {
		double[][] ll = new double[2][K]; // ll[n][k] is the log-likelihood p(x[n]|k).
		double[] scalingFactor = new double[2]; // scalingFactor[n] is chosen from ll[n].
		double[][] alpha = new double[2][K]; // alpha[n][k] is for the n-th position of sequence r at state k.
		double[] con = new double[2]; // con[n] is ln p(x_n | x_1, x_2, ... x_n-1), this is used for normalization.
		// calc LL
		for (int n = 0; n < 2; n++)
			for (int k = 0; k < K; k++)
				ll[n][k] = calcLLVecState(geo.get(n), temporal.get(n), textVec.get(n), k, isTest, info_option);
		// Find the scaling factors.
		for (int n = 0; n < 2; n++)
			scalingFactor[n] = ArrayUtils.max(ll[n]);
		// Scale the log-likelihood.
		for (int n = 0; n < 2; n++)
			for (int k = 0; k < K; k++)
				ll[n][k] -= scalingFactor[n];
		// Compute alpha[0][k], in the log domain!
		for (int k = 0; k < K; k++)
			alpha[0][k] = log(pi[k]) + ll[0][k];
		// Compute con[0], namely ln p(x_0)
		con[0] = ArrayUtils.sumExpLog(alpha[0]);
		// Normalize alpha[0][k]
		ArrayUtils.logNormalize(alpha[0]);
		// Compute alpha[1][k], again in the log domain.
		for (int k = 0; k < K; k++) {
			alpha[1][k] = ll[1][k];
			double sum = 1e-200;
			for (int j = 0; j < K; j++) {
				sum += alpha[0][j] * A[j][k];
			}
			alpha[1][k] += log(sum);
		}
		// Compute con[1], namely ln p(x_1 | x_0)
		con[1] = ArrayUtils.sumExpLog(alpha[1]);
		// the result ll.
		double hmmLL = 0;
		for (int n = 0; n < 2; n++)
			hmmLL += con[n] + scalingFactor[n];
		return hmmLL;
	}

	public double calcSeqScore(Sequence seq, int info_option) {
		Checkin startPlace = seq.getCheckin(0);
		Checkin endPlace = seq.getCheckin(1);
		List<RealVector> geo = new ArrayList<RealVector>();
		List<RealVector> temporal = new ArrayList<RealVector>();
//		List<Map<Integer, Integer>> text = new ArrayList<Map<Integer, Integer>>();
		List<RealVector> textvec = new ArrayList<RealVector>();
		geo.add(startPlace.getLocation().toRealVector());
		temporal.add(new ArrayRealVector(new double[] { startPlace.getTimestamp() % 1440 }));
//		text.add(startPlace.getMessage());
		textvec.add(startPlace.getVector());
		geo.add(endPlace.getLocation().toRealVector());
		temporal.add(new ArrayRealVector(new double[] { endPlace.getTimestamp() % 1440 }));
//		text.add(endPlace.getMessage());
		textvec.add(endPlace.getVector());
//		return calcLL(geo, temporal, text);
		return calcVecLL(geo, temporal, textvec, Boolean.FALSE, info_option);

	}

	public DBObject toBson() {
		DBObject o = new BasicDBObject();
		o.put("R", R);
		o.put("K", K);
		o.put("M", M);
		o.put("V", V);
		o.put("pi", pi);
		o.put("A", A);
		o.put("c", c);
		List<DBObject> text = new ArrayList<DBObject>();
		for (Multinomial m : textModel)
			text.add(m.toBSon());
		o.put("textModel", text);
		List<List<DBObject>> geo = new ArrayList<List<DBObject>>();
		for (int i = 0; i < geoModel.length; i++) {
			List<DBObject> gmmdata = new ArrayList<DBObject>();
			Gaussian[] gmm = geoModel[i];
			for (int j = 0; j < gmm.length; j++) {
				gmmdata.add(gmm[j].toBSon());
			}
			geo.add(gmmdata);
		}
		o.put("geoModel", geo);
		List<DBObject> temporal = new ArrayList<DBObject>();
		for (Gaussian t : temporalModel)
			temporal.add(t.toBSon());
		o.put("temporalModel", temporal);
		return o;
	}


	public DBObject statsToBson() {
		DBObject o = new BasicDBObject();
		o.put("maxIter", maxIter);
		o.put("R", R);
		o.put("K", K);
		o.put("M", M);
		o.put("V", V);
		o.put("time", elapsedTime);
		return o;
	}

	public void load(DBObject o) {
		this.R = (Integer) o.get("R");
		this.K = (Integer) o.get("K");
		this.M = (Integer) o.get("M");
		this.V = (Integer) o.get("V");

		BasicDBList piList = (BasicDBList) o.get("pi");
		this.pi = new double[piList.size()];
		for (int i = 0; i < piList.size(); i++) {
			this.pi[i] = (Double) piList.get(i);
		}

		Object[] aList = ((BasicDBList) o.get("A")).toArray();
		this.A = new double[aList.length][((BasicDBList) aList[0]).size()];
		for (int i = 0; i < aList.length; i++) {
			BasicDBList list = (BasicDBList) aList[i];
			for (int j = 0; j < list.size(); j++)
				A[i][j] = (Double) list.get(j);
		}

		Object[] cList = ((BasicDBList) o.get("c")).toArray();
		this.c = new double[cList.length][((BasicDBList) cList[0]).size()];
		for (int i = 0; i < cList.length; i++) {
			BasicDBList list = (BasicDBList) cList[i];
			for (int j = 0; j < list.size(); j++)
				c[i][j] = (Double) list.get(j);
		}

		List<DBObject> text = (List<DBObject>) o.get("textModel");
		this.textModel = new Multinomial[text.size()];
		for (int i = 0; i < text.size(); i++)
			this.textModel[i] = new Multinomial(text.get(i));

		List<List<DBObject>> geo = (List<List<DBObject>>) o.get("geoModel");
		int row = geo.size(), column = geo.get(0).size();
		geoModel = new Gaussian[row][column];
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < column; j++) {
				DBObject d = geo.get(i).get(j);
				Gaussian g = new Gaussian(d);
				geoModel[i][j] = g;
			}
		}

		List<DBObject> temporal = (List<DBObject>) o.get("temporalModel");
		this.temporalModel = new Gaussian[text.size()];
		for (int i = 0; i < temporal.size(); i++)
			this.temporalModel[i] = new Gaussian(temporal.get(i));
	}

	/**
	 * Methods for the ensemble of HMM.
	 */
	// don't need to return LL, since I noticed the LL is stored in "totalLL" and can be accessed any time
	public void train(SequenceDataset data, int K, int M, double[] seqsFracCount, int info_option) {
		init(data, K, M, seqsFracCount, info_option);
		iterate(data, info_option);
	}

	// don't need to return LL, since I noticed the LL is stored in "totalLL" and can be accessed any time
	public void update(SequenceDataset data, double[] seqsFracCount, int info_option) {
		setWeight(seqsFracCount);
		iterate(data, info_option);
	}

	public double getTotalLL() {
		return totalLL;
	}

}
