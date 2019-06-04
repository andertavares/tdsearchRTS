package activation;

/**
 * Defines a default activation: the identity function, suitable for linear regression 
 * with mean squared error. Moreover, provides a dummy scaling for the 
 * target (actual rather than predicted) value  
 * 
 * @author artavares
 *
 */
public class IdentityActivation {
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
	 * Returns the raw value itself (makes no scaling)
	 * @param rawValue
	 * @return
	 */
	public double scaleTargetValue(double rawValue) {
		return rawValue;
	}
}
