package distribution;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.math3.linear.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * The gaussian distribution.
 * Created by chao on 4/14/15.
 */
public class Gaussian implements Serializable {

    RealVector mean = null;
    RealMatrix var = null;
    double weightSum = 0;

    int dimension;
    private double CONSTANT;
    double varDeterminant; // The inverse of the variance.
    RealMatrix varInverse; // The inverse of the variance.

    public Gaussian() {}

    public Gaussian(RealVector mean, RealMatrix var) {
        this.dimension = mean.getDimension();
        this.mean = mean;
        this.var = var;
        calcVarInverse();
    }

    public Gaussian(DBObject o) {
        load(o);
    }

    public void fit(List<RealVector> data, List<Double> weights) {
        if (data.size() != weights.size()) {
            System.out.println("Error when fitting Gaussian. Database size:" + data.size() + "Weight size:" + weights.size());
            System.exit(1);
        }
        init(data.get(0).getDimension());
        calcWeightSum(weights);
        calcMean(data, weights);
        calcVar(data, weights);
        calcVarInverse();
    }

    public RealVector getMean() {
        return mean;
    }

    public RealMatrix getVar() {
        return var;
    }

    private void init(int dimension) {
        this.dimension = dimension;
        mean = new ArrayRealVector(dimension);
        var = new Array2DRowRealMatrix(dimension, dimension);
    }

    private void calcWeightSum(List<Double> weights) {
        weightSum = 0;
        for (Double w : weights)
            weightSum += w;
    }

    private void calcMean(List<RealVector> data, List<Double> weights) {
        for(int index = 0; index < data.size(); index ++) {
            RealVector x = data.get(index);
            double weight = weights.get(index);
            mean = mean.add(x.mapMultiply(weight));
        }
        mean.mapDivideToSelf(weightSum);
    }

    private void calcVar(List<RealVector> data, List<Double> weights) {
        for(int index = 0; index < data.size(); index ++) {
            RealVector x = data.get(index).subtract(mean);
            RealMatrix m = x.outerProduct(x);
            double weight = weights.get(index);
            var = var.add(m.scalarMultiply(weight));
        }
        var = var.scalarMultiply( 1.0 / weightSum );
    }

    private void calcVarInverse() {
        if(dimension == 1) {
            if(Math.abs(var.getEntry(0, 0)) <= 1e-6)
                var.setEntry(0, 0, 1e-5);
            varInverse = new LUDecomposition(var).getSolver().getInverse();
            varDeterminant = new LUDecomposition(var).getDeterminant();
        } else if (dimension == 2) {
            boolean inverseFinished = false;
            int counter = 0;
            while (inverseFinished == false) {
                if (Math.abs(var.getEntry(0, 0)) <= 1e-6)
                    var.setEntry(0, 0, 1e-5);
                if (Math.abs(var.getEntry(1, 1)) <= 1e-6)
                    var.setEntry(1, 1, 1e-5);
                if (counter >= 5) {
                    var.setEntry(0, 1, 0);
                    var.setEntry(1, 0, 0);
                    varInverse = new LUDecomposition(var).getSolver().getInverse();
                    break;
                }
                try {
                    varInverse = new LUDecomposition(var).getSolver().getInverse();
                    inverseFinished = true;
                } catch (Exception e) {
                    System.out.println("Error when computing the inverse of" + var);
                    var.setEntry(0, 0, var.getEntry(0, 0) * 1.01);
                    var.setEntry(1, 1, var.getEntry(1, 1) * 1.01);
                    System.out.println("The matrix is changed to" + var);
                }
                counter++;
            }
            varDeterminant = new LUDecomposition(var).getDeterminant();
        } else { // For embedding Gaussian varInverse update
            boolean inverseFinished = false;
            int counter = 0;
            while (inverseFinished == false) {
                for (int i = 0; i < dimension; i++){
                    if (Math.abs(var.getEntry(i, i)) <= 1e-6)
                        var.setEntry(i, i, 1e-5);
                }
//                if (Math.abs(var.getEntry(0, 0)) <= 1e-6)
//                    var.setEntry(0, 0, 1e-5);
//                if (Math.abs(var.getEntry(1, 1)) <= 1e-6)
//                    var.setEntry(1, 1, 1e-5);
                if (counter >= 5) {
                    for (int i = 0; i < dimension; i++)
                        var.setEntry(i, dimension-i-1, 0);
                    varInverse = new LUDecomposition(var).getSolver().getInverse();
                    break;
                }
                try {
                    varInverse = new LUDecomposition(var).getSolver().getInverse();
                    inverseFinished = true;
                } catch (Exception e) {
//                    System.out.println("Error when computing the inverse of" + var);
                    for (int i = 0; i < dimension; i++)
                        var.setEntry(i, i, var.getEntry(i, i) * 1.01);
//                    System.out.println("The matrix is changed to" + var);
                }
                counter++;
            }
            varDeterminant = new LUDecomposition(var).getDeterminant();
//            System.out.println("Fitting Gaussian Warning: dimension larger than 2!");
        }
        CONSTANT = - dimension / 2.0 * Math.log(2*Math.PI) - 0.5 * Math.log( varDeterminant );
    }

    public double calcLL(RealVector sample) {
        RealVector vector = sample.subtract(mean);
        double result =  - 0.5 * vector.dotProduct(varInverse.operate(vector));
        return result + CONSTANT;
    }


    @Override
    public String toString() {
        return "Mean:" + mean + "\n" + "Var:" + var + "\n";
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
        ret.put("dim", dimension);
        ret.put("mean", mean.toArray());
        ret.put("var", var.getData());
        ret.put("varInverse", varInverse.getData());
        ret.put("varDeterminant", varDeterminant);
        ret.put("CONSTANT", CONSTANT);
        ret.put("weightedSum", weightSum);
        return ret;
    }

    protected void load(DBObject o) {
        this.dimension = (Integer) o.get("dim");
        this.varDeterminant = (Double) o.get("varDeterminant");
        this.CONSTANT = (Double) o.get("CONSTANT");
        this.weightSum = (Double) o.get("weightedSum");

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
        this.var = new Array2DRowRealMatrix(varValues);

        Object[] varValueInverseList = ((BasicDBList)o.get("varInverse")).toArray();
        double[][] varInverseValues = new double[varValueInverseList.length][((BasicDBList)varValueInverseList[0]).size()];
        for(int i=0; i<varValueInverseList.length; i++) {
            BasicDBList list = (BasicDBList) varValueInverseList[i];
            for (int j = 0; j < list.size(); j++)
                varInverseValues[i][j] = (Double) list.get(j);
        }
        this.varInverse = new Array2DRowRealMatrix(varInverseValues);
    }

}
