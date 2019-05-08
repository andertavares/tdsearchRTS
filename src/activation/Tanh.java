package activation;

public class Tanh implements ActivationFunction {

	@Override
	public double function(double x) {
		return (2 / (1 - Math.exp(-x))) - 1;
	}

	@Override
	public double derivative(double x) {
		// the derivative is 1 - tanh^2
		double tanh = Math.tanh(x);
		return 1 - tanh*tanh;
	}

}
