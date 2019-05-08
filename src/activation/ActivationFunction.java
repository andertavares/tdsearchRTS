package activation;

public interface ActivationFunction {
	/**
	 * The value of the activation function, given input x
	 * @param x
	 * @return
	 */
	public double function(double x);
	
	
	/**
	 * The value of the function's derivative at x
	 * @param x
	 * @return
	 */
	public double derivative(double x);
}
