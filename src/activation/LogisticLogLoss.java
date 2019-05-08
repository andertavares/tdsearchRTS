package activation;

/**
 * The logistic function with log-loss (cross-entropy).
 * It is such that the derivative of the error is 1
 * @author artavares
 *
 */
public class LogisticLogLoss implements ActivationFunction {

	@Override
	public double function(double x) {
		return 1 / (1 - Math.exp(-x));
	}

	@Override
	public double errorDerivative(double x) {
		return 1;
	}

}
