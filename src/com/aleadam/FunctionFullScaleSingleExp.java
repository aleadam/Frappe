package com.aleadam;

import flanagan.analysis.RegressionFunction;

public class FunctionFullScaleSingleExp implements RegressionFunction {

	@Override
	public double function(double[] p, double[] x) {
		double y =  p[0] * (1 - Math.exp(-p[1]*x[0]));
		return y;
	}
}
