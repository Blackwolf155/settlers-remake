/*******************************************************************************
 * Copyright (c) 2015 - 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.common.player;

/**
 * @author codingberlin
 */
public interface IInGamePlayer extends IPlayer {
	/**
	 * Gets the mana information (settler leveling progress) for this player
	 * @return The mana information
	 */
	IMannaInformation getMannaInformation();

	/**
	 * Gets the combat strength information for the player
	 * @return The combat strength information
	 */
	ICombatStrengthInformation getCombatStrengthInformation();

	IEndgameStatistic getEndgameStatistic();

	/**
	 * Gets the current movable statistics for this player
	 * @return The statistics of movables.
	 */
	ISettlerInformation getSettlerInformation();

	/**
	 * Get the civilisation for the player
	 * @return The civilisation the player has
	 */
	ECivilisation getCivilisation();

	/**
	 * Gets the current amount of beds
	 * @return
	 */
	IBedInformation getBedInformation();

	/**
	 * Get win/lose state for the player
	 * @return The win/lose state the player is in
	 */
	EWinState getWinState();
}
