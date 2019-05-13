package activation;

/**
 * The logistic function with log-loss (cross-entropy).
 * It is such that the derivative of the error is 1.
 * 
 * It provides a proper scaling for the target value for logistic regression.
 * @author artavares
 *
 */
public class LogisticLogLoss extends DefaultActivationFunction {

	@Override
	public double function(double x) {
		return 1 / (1 + Math.exp(-x));
	}

	@Override
	public double errorDerivative(double x) {
		return 1;
	}
	
	@Override
	/**
	 * Assumes the target value is in [-1, 1] and scales it to [0, 1] (minmax scaling)
	 */
	public double scaleTargetValue(double rawValue) {
		if(rawValue < -1 || rawValue > 1) {
			throw new IllegalArgumentException("Raw value must be in range [-1,1]");
		}
		
		return (rawValue + 1) / 2;
	}

}
