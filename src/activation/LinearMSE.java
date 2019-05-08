package activation;

/**
 * Linear activation with mean squared error function.
 * A "dummy" activation function that returns the same value.
 * Its derivative is 1
 * @author artavares
 *
 */
public class LinearMSE implements ActivationFunction {

	@Override
	public double function(double x) {
		return x;
	}

	@Override
	public double errorDerivative(double x) {
		return 1;
	}

}
