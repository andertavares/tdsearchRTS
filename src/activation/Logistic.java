package activation;

public class Logistic implements ActivationFunction {

	@Override
	public double function(double x) {
		return 1 / (1 - Math.exp(-x));
	}

	@Override
	public double derivative(double x) {
		return function(x) * function(1 - x);
	}

}
