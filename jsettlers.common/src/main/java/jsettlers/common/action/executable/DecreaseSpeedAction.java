package jsettlers.common.action.executable;

import jsettlers.common.action.EActionType;
import jsettlers.common.menu.IStartedGame;

/**
 * Action that decreases the game speed by a certain factor.
 *
 * @author Blackwolf
 */
public class DecreaseSpeedAction extends TimeAction {

	private final IStartedGame iStartedGame;
	private final int decreaseFactor;
	
	public DecreaseSpeedAction(IStartedGame iStartedGame) {
		super(EActionType.SPEED_SLOWER);
		this.iStartedGame = iStartedGame;
		this.decreaseFactor = 1;
	}
	public DecreaseSpeedAction(IStartedGame iStartedGame, int decreaseFactor) {
		super(EActionType.SPEED_SLOWER);
		this.iStartedGame = iStartedGame;
		this.decreaseFactor = decreaseFactor;
	}

	@Override
	public void executeAction() {
		iStartedGame.getGameTimeProvider().getGameClock().decreaseGameSpeed(decreaseFactor);
	}
}
