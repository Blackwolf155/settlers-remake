package jsettlers.common.action.executable;

import jsettlers.common.action.Action;
import jsettlers.common.action.EActionType;

/**
 * Action for manipulating the game speed.
 * 
 * @author Blackwolf
 */
public abstract class TimeAction extends Action implements ExecutableAction {
	
	public TimeAction(EActionType actionType) {
		super(actionType);
	}
}
