package activation;

public interface ActivationFunction {

	/**
	 * Returns the value of the activation function for the given input.
	 * @param x
	 * @return
	 */
	public double activate(double x);
	
	
	/**
	 * Returns the derivative of the error function at the given input. 
	 * 
	 * @param x
	 * @return
	 */
	public double errorDerivative(double x);
	
	/**
	 * Returns a proper scaling of the raw value to fit the 
	 * range of the activation function.
	 * @param rawValue
	 * @return
	 */
	public double scaleTargetValue(double rawValue);
}
