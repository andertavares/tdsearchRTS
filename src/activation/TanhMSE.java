package activation;

/**
 * Hyperbolic tangent activation with mean squared error function
 * @author artavares
 *
 */
public class TanhMSE implements ActivationFunction {

	@Override
	/**
	 * Returns the value of the hyperbolic tangent at point x
	 */
	public double activate(double x) {
		return Math.tanh(x);
	}

	@Override
	/**
	 * The MSE derivative for Tanh activation is 1 - tanh(x)^2
	 */
	public double errorDerivative(double x) {
		// the derivative is 1 - tanh^2
		double tanh = Math.tanh(x);
		return 1 - tanh*tanh;
	}

	@Override
	/**
	 * Assumes the raw value is in [-1, 1], so return it as is.
	 */
	public double scaleTargetValue(double rawValue) {
		if(rawValue < -1 || rawValue > 1) {
			throw new IllegalArgumentException("Raw value must be in range [-1,1]");
		}
		return rawValue;
	}

}
