package distribution;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.math3.linear.*;
import jdistlib.math.Bessel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The VMF distribution.
 * Created by Wanzheng Zhu on 1/May/17.
 */
public class VMF implements Serializable {

	RealVector mean = null;
	double kappa = 0;
	double weightSum = 0;


	public VMF() {}

	public VMF(RealVector mean, RealMatrix var) {
//		this.dimension = mean.getDimension();
		this.mean = mean;
	}

	public VMF(DBObject o) {
		load(o);
	}

	public void fit(List<RealVector> data, List<Double> weights, int info_option) {
		if (data.size() != weights.size()) {
			System.out.println("Error when fitting Gaussian. Database size:" + data.size() + "Weight size:" + weights.size());
			System.exit(1);
		}
		init(data.get(0).getDimension());
		calcWeightSum(weights);
		calcMean(data, weights);

		if (info_option == 30)
			calcConcentration(data, weights);
		else
			calcExactConcentration(data, weights);

		if (info_option == 30 || info_option ==31)  // Synthetic Data...
			mean.mapDivideToSelf(mean.getNorm());
		else
			mean.mapDivideToSelf(mean.getNorm() + 1e-200);

//        System.out.println(mean);
	}

	public RealVector getMean() {
		return mean;
	}

	public double getKappa() {
		return kappa;
	}

	private void init(int dimension) {
		mean = new ArrayRealVector(dimension);
	}

	public void randominit(int dimension) {
		mean = new ArrayRealVector(dimension);
		for (int i = 0; i < dimension; i++) {
			mean.addToEntry(i,2*Math.random()-1);
		}
		mean.mapDivideToSelf(mean.getNorm());
		kappa = Math.random()*10;
//		this.dimension = dimension;
	}

	private void calcWeightSum(List<Double> weights) {
		weightSum = 1e-200;
		for (Double w : weights)
			weightSum += w;
	}

	private void calcMean(List<RealVector> data, List<Double> weights) {
		for(int index = 0; index < data.size(); index ++) {
			RealVector x = data.get(index);
			double weight = weights.get(index);
			mean = mean.add(x.mapMultiply(weight));
		}
	}


    private void calcConcentration(List<RealVector> data, List<Double> weights) {
	    // To Calculate the k of the VMF distribution
		double R_head = mean.getNorm() / weightSum;
		kappa = R_head * (data.get(0).getDimension() - R_head*R_head) / (1-R_head*R_head);
		if ((kappa>500) || (kappa<=0) || (Double.isNaN(kappa))) // Otherwise Bessel.i can't work for large kappa
			kappa = 500;
    }

	private void calcExactConcentration(List<RealVector> data, List<Double> weights) {
		// To Calculate the k of the VMF distribution
		double R_head = mean.getNorm() / weightSum;
		double dim = data.get(0).getDimension();
		kappa = R_head * (dim - R_head*R_head) / (1-R_head*R_head);
//		kappa = Math.random()*100;

		//Newton's Method: x_{n+1} = x_n - f(x_n)/f'(x_n)
		double diff = 1.0;
//		int number_of_iteration = 0;
		while (Math.abs(diff) > 1e-10){
			double A_d = (Bessel.i(kappa, dim/2, false)) / (Bessel.i(kappa, dim/2-1, false));
			diff = (A_d - R_head) / (1- A_d*A_d - (dim-1)/kappa * A_d);
			kappa = kappa - diff;
//			number_of_iteration++;
		}
//		System.out.printf("Number of iteration is " + number_of_iteration);

		if ((kappa>500) || (kappa<=0) || (Double.isNaN(kappa))) // Otherwise Bessel.i can't work for large kappa
			kappa = 500;
	}

	public double calcLL(RealVector sample) {
	    // Return the log likelihood of the VMF distribution f(X | \mu, k)
		double result = Bessel.i(kappa, (double)(mean.getDimension())/2-1, false );
		double numerator = (mean.getDimension()/2-1) * Math.log(kappa);
		double denominator = mean.getDimension()/2 * Math.log(2*Math.PI) + Math.log(result);
		result = numerator - denominator;
		result = result + kappa * mean.dotProduct(sample);
        if (Double.isInfinite(result)) // To control the likelihood ll
				result = 1000;
		return result;
	}

	public static void main(String [] args) {
		double [] v1 = new double [] {-1, 1};
		double [] v2 = new double [] {1, 1};
		double [] v3 = new double [] {1, -1};
		double [] v4 = new double [] {-1, -1};
		RealVector rv1 = new ArrayRealVector(v1);
		RealVector rv2 = new ArrayRealVector(v2);
		RealVector rv3 = new ArrayRealVector(v3);
		RealVector rv4 = new ArrayRealVector(v4);
		List<RealVector> data = new ArrayList<RealVector>();
		data.add(rv1);
		data.add(rv2);
		data.add(rv3);
		data.add(rv4);
		Double [] w = new Double [] {1.0, 1.0, 1.0, 1.0};
		List<Double> weights = Arrays.asList(w);
		System.out.println(weights);
		Gaussian gaussian = new Gaussian();
		gaussian.fit(data, weights);
		System.out.println(gaussian.mean);
		System.out.println(gaussian.var);
		System.out.println(gaussian.calcLL(new ArrayRealVector(new Double[]{0.0, 0.0})));
	}

	public DBObject toBSon() {
		BasicDBObject ret = new BasicDBObject();
		return ret;
	}

	protected void load(DBObject o) {

		Object[] meanValueList = ((BasicDBList)o.get("mean")).toArray();
		double[] meanValues = new double[meanValueList.length];
		for(int i= 0; i<meanValueList.length; i++)
			meanValues[i] = (Double) meanValueList[i];
		this.mean = new ArrayRealVector(meanValues);

		Object[] varValueList = ((BasicDBList)o.get("var")).toArray();
		double[][] varValues = new double[varValueList.length][((BasicDBList)varValueList[0]).size()];
		for(int i=0; i<varValueList.length; i++) {
			BasicDBList list = (BasicDBList) varValueList[i];
			for (int j = 0; j < list.size(); j++)
				varValues[i][j] = (Double) list.get(j);
		}

		Object[] varValueInverseList = ((BasicDBList)o.get("varInverse")).toArray();
		double[][] varInverseValues = new double[varValueInverseList.length][((BasicDBList)varValueInverseList[0]).size()];
		for(int i=0; i<varValueInverseList.length; i++) {
			BasicDBList list = (BasicDBList) varValueInverseList[i];
			for (int j = 0; j < list.size(); j++)
				varInverseValues[i][j] = (Double) list.get(j);
		}
	}
}
