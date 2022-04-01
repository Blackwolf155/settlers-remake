package jsettlers.common.action.executable;

import jsettlers.common.action.EActionType;
import jsettlers.common.menu.IStartedGame;

/**
 * Action that increases the game speed by a certain factor.
 * 
 * @author Blackwolf
 */
public class IncreaseSpeedAction extends TimeAction {

	private final IStartedGame iStartedGame;
	private final int increaseFactor;
	
	public IncreaseSpeedAction(IStartedGame iStartedGame) {
		super(EActionType.SPEED_FASTER);
		this.iStartedGame = iStartedGame;
		this.increaseFactor = 1;
	}
	
	public IncreaseSpeedAction(IStartedGame iStartedGame, int increaseFactor) {
		super(EActionType.SPEED_FASTER);
		this.iStartedGame = iStartedGame;
		this.increaseFactor = increaseFactor;
	}

	@Override
	public void executeAction() {
		iStartedGame.getGameTimeProvider().getGameClock().increaseGameSpeed(increaseFactor);
	}
}
