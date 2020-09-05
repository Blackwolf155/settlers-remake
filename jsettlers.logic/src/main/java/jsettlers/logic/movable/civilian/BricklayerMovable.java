package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableBricklayer;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IConstructableBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BricklayerMovable extends Movable implements IManageableBricklayer {
	private static final float BRICKLAYER_ACTION_DURATION = 1f;

	private IConstructableBuilding constructionSite = null;
	private boolean registered = false;
	private ShortPoint2D targetPosition;
	private EDirection lookDirection;

	private boolean fleeing;

	public BricklayerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.BRICKLAYER, position, player, movable, tree);
	}

	private static final Root<BricklayerMovable> tree = new Root<>(createBricklayerBehaviour());

	private static Node<BricklayerMovable> createBricklayerBehaviour() {
		return guardSelector(
				guard(mov -> mov.fleeing,
					sequence(
						BehaviorTreeHelper.action(BricklayerMovable::abortJob),
						alwaysSucceed() // TODO
					)
				),
				guard(mov -> mov.constructionSite != null && mov.constructionSite.isBricklayerRequestActive(),
					sequence(
						selector(
							goToPos(mov -> mov.targetPosition, mov -> mov.constructionSite != null && mov.constructionSite.isBricklayerRequestActive()), // TODO
							sequence(
								BehaviorTreeHelper.action(BricklayerMovable::abortJob),
								alwaysFail()
							)
						),
						BehaviorTreeHelper.action(mov -> {
							mov.lookInDirection(mov.lookDirection);
						}),
						repeat(mov -> true,
							sequence(
								condition(mov -> mov.constructionSite.tryToTakeMaterial()),
								playAction(EMovableAction.ACTION1, mov -> (short)(BRICKLAYER_ACTION_DURATION*1000))
							)
						)
					)
				),
				guard(mov -> !mov.registered,
					BehaviorTreeHelper.action(mov -> {
						mov.constructionSite = null;
						mov.registered = true;
						mov.grid.addJobless(mov);
					})
				)
		);
	}

	@Override
	public boolean setBricklayerJob(IConstructableBuilding constructionSite, ShortPoint2D bricklayerTargetPos, EDirection direction) {
		if(this.constructionSite == null) {
			this.constructionSite = constructionSite;
			this.targetPosition = bricklayerTargetPos;
			this.lookDirection = direction;
			registered = false;
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if(constructionSite != null) {
			abortJob();
		} else {
			grid.removeJobless(this);
		}
	}

	private void abortJob() {
		constructionSite.bricklayerRequestFailed(targetPosition, lookDirection);
	}
}
