package com.aleadam;

import flanagan.analysis.RegressionFunction;

public class FunctionFullScaleDoubleExp implements RegressionFunction {

	@Override
	public double function(double[] p, double[] x) {
		double y =  p[0] + p[1] - p[0]*Math.exp(-p[2]*x[0]) - p[1]*Math.exp(-p[3]*x[0]);
		return y;
	}
}
