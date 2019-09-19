package utils;

public class MathHelper {
	/**
	 * Dot product between two vectors (i.e. sum(v1[i] * v2[i]) for i = 0, ..., length of the vectors
	 * @param vector1
	 * @param vector2
	 * @return
	 */
	public static double dotProduct(double[] vector1, double[] vector2) {
		assert vector1.length == vector2.length;
		
		double value = 0;
		for(int i = 0; i < vector1.length; i++) {
			value += vector1[i] * vector2[i];
		}
		return value;
	}
}
