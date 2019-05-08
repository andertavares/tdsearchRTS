package activation;

/**
 * Defines a default activation function, suitable for linear regression 
 * with mean squared error. Moreover, provides a dummy scaling for the reward  
 * 
 * @author artavares
 *
 */
public class DefaultActivationFunction {
	/**
	 * Returns x itself (a dummy activation function).
	 * Suitable for linear regression
	 * 
	 * @param x
	 * @return
	 */
	public double function(double x) {
		return x;
	}
	
	
	/**
	 * Returns 1 for the derivative of the error function. 
	 * Suitable for mean squared error, used in linear regression.
	 * 
	 * @param x
	 * @return
	 */
	public double errorDerivative(double x) {
		return 1;
	}
	
	/**
	 * Returns the rawReward itself (makes no scaling)
	 * @param rawReward
	 * @return
	 */
	public double scaleReward(int rawReward) {
		return rawReward;
	}
}
