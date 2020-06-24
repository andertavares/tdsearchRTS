package utils;

/**
 * Records match data
 * TODO include match duration, begin, end and trace
 * @author anderson
 *
 */
public class MatchData {
	
	public static final int MATCH_ERROR = 2;
	public static final int DRAW = -1;
	public static final int P1_WINS = 0;
	public static final int P2_WINS = 1;
	
	/**
	 * 0 or 1: victory of the respective player; -1: draw
	 */
	public int winner;
	
	/**
	 * How many frames the match took.
	 */
	public int frames;

	
	/**
	 * Records this match data
	 * @param winner who won the match (0 or 1: victory of the respective player; -1: draw)
	 * @param frames how many frames the match took.
	 */
	public MatchData(int winner, int frames) {
		this.winner = winner;
		this.frames = frames;
	}
	
	
}
