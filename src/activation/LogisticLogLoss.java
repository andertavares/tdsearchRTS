package activation;

/**
 * The logistic function with log-loss (cross-entropy).
 * It is such that the derivative of the error is 1
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
	 * Scales the raw reward to the interval [0, 1]
	 */
	public double scaleReward(int rawReward) {
		if(rawReward < -1 || rawReward > 1) {
			throw new IllegalArgumentException("Raw reward must be in range [-1,1]");
		}
		
		return (rawReward + 1) / 2;
	}

}
