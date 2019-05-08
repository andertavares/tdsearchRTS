package activation;

/**
 * Tanh activation with mean squared error function
 * @author artavares
 *
 */
public class TanhMSE extends DefaultActivationFunction {

	@Override
	public double function(double x) {
		return (2 / (1 - Math.exp(-x))) - 1;
	}

	@Override
	public double errorDerivative(double x) {
		// the derivative is 1 - tanh^2
		double tanh = Math.tanh(x);
		return 1 - tanh*tanh;
	}

}
