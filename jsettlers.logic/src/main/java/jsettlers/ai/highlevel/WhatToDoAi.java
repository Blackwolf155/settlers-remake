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
package jsettlers.ai.highlevel;


import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jsettlers.ai.army.ArmyGeneral;
import jsettlers.ai.construction.ConstructionPositionFinder;
import jsettlers.ai.economy.EconomyMinister;
import jsettlers.ai.highlevel.pioneers.PioneerAi;
import jsettlers.ai.highlevel.pioneers.PioneerGroup;
import jsettlers.ai.highlevel.pioneers.target.SameBlockedPartitionLikePlayerFilter;
import jsettlers.ai.highlevel.pioneers.target.SurroundedByResourcesFilter;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.input.tasks.ConstructBuildingTask;
import jsettlers.input.tasks.ConvertGuiTask;
import jsettlers.input.tasks.SimpleBuildingGuiTask;
import jsettlers.input.tasks.EGuiAction;
import jsettlers.input.tasks.MoveToGuiTask;
import jsettlers.input.tasks.WorkAreaGuiTask;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.map.grid.MainGrid;
import jsettlers.logic.map.grid.movable.MovableGrid;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.network.client.interfaces.ITaskScheduler;

import static jsettlers.ai.highlevel.AiBuildingConstants.*;
import static jsettlers.common.buildings.EBuildingType.*;
import static jsettlers.common.material.EMaterialType.GEMS;
import static jsettlers.common.material.EMaterialType.GOLD;
import static jsettlers.common.material.EMaterialType.MEAD;
import jsettlers.common.action.EMoveToType;

/**
 * This WhatToDoAi is a high level KI. It delegates the decision which building is build next to its economy minister. However this WhatToDoAi takes care against lack of settlers and it builds a
 * toolsmith when needed but as late as possible. Furthermore it builds towers continously to spread the land. It destroys not needed living houses and stonecutters to get back building materials.
 * Which SOLDIERS to levy and to command the SOLDIERS is delegated to its armay general.
 *
 * @author codingberling
 */
class WhatToDoAi implements IWhatToDoAi {

	private static final int NUMBER_OF_SMALL_LIVING_HOUSE_BEDS = 10;
	private static final int NUMBER_OF_MEDIUM_LIVING_HOUSE_BEDS = 30;
	private static final int NUMBER_OF_BIG_LIVING_HOUSE_BEDS = 100;

	private static final int MINIMUM_NUMBER_OF_BEARERS = 10;
	private static final int MINIMUM_NUMBER_OF_JOBLESS_BEARERS = 10;
	private static final float MINIMUM_NUMBER_OF_JOBLESS_BEARERS_PER_BUILDING = 1.2f;

	private static final int NUMBER_OF_BEARERS_PER_HOUSE = 3;
	private static final int MAXIMUM_STONECUTTER_WORK_RADIUS_FACTOR = 2;
	private static final float WEAPON_SMITH_FACTOR = 7F;
	private static final int RESOURCE_PIONEER_GROUP_COUNT = 20;
	private static final int BROADEN_PIONEER_GROUP_COUNT = 40;

	private final MainGrid mainGrid;
	private final MovableGrid movableGrid;
	private final byte playerId;
	private final IInGamePlayer player;
	private final ITaskScheduler taskScheduler;
	private final AiStatistics aiStatistics;
	private final ArmyGeneral armyGeneral;
	private final ConstructionPositionFinder.Factory constructionPositionFinderFactory;
	private final EconomyMinister economyMinister;
	private final PioneerAi pioneerAi;
	private boolean isEndGame = false;
	private ArrayList<Object> failedConstructingBuildings;
	private PioneerGroup resourcePioneers;
	private PioneerGroup broadenerPioneers;
	private AiPositions.AiPositionFilter[] geologistFilters = new AiPositions.AiPositionFilter[EResourceType.values().length];

	WhatToDoAi(IInGamePlayer player, AiStatistics aiStatistics, EconomyMinister economyMinister, ArmyGeneral armyGeneral, MainGrid mainGrid, ITaskScheduler taskScheduler) {
		this.player = player;
		this.playerId = player.getPlayerId();
		this.mainGrid = mainGrid;
		this.movableGrid = mainGrid.getMovableGrid();
		this.taskScheduler = taskScheduler;
		this.aiStatistics = aiStatistics;
		this.armyGeneral = armyGeneral;
		this.economyMinister = economyMinister;
		this.pioneerAi = new PioneerAi(aiStatistics, player);
		constructionPositionFinderFactory = new ConstructionPositionFinder.Factory(
				mainGrid.getPartitionsGrid().getPlayer(playerId).getCivilisation(),
				aiStatistics,
				mainGrid.getConstructionMarksGrid(),
				playerId);
		resourcePioneers = new PioneerGroup(RESOURCE_PIONEER_GROUP_COUNT);
		broadenerPioneers = new PioneerGroup(BROADEN_PIONEER_GROUP_COUNT);
		List<Integer> allPioneers = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.PIONEER).stream()
				.map(pos -> movableGrid.getMovableAt(pos.x, pos.y))
				.map(ILogicMovable::getID)
				.collect(Collectors.toList());

		int resourcePioneersNumber = Math.min(allPioneers.size(), RESOURCE_PIONEER_GROUP_COUNT);
		resourcePioneers.addAll(allPioneers.subList(0, resourcePioneersNumber));
		broadenerPioneers.addAll(allPioneers.subList(resourcePioneersNumber, allPioneers.size()));

		for (EResourceType resourceType : EResourceType.VALUES) {
			geologistFilters[resourceType.ordinal] = new AiPositions.CombinedAiPositionFilter(
					new SurroundedByResourcesFilter(mainGrid, mainGrid.getLandscapeGrid(), resourceType),
					new SameBlockedPartitionLikePlayerFilter(aiStatistics, playerId));
		}
	}

	@Override
	public void applyLightRules() {
		armyGeneral.applyLightRules(new HashSet<>());
	}

	@Override
	public void applyHeavyRules() {
		if (aiStatistics.isAlive(playerId)) {
			economyMinister.update();
			isEndGame = economyMinister.isEndGame();
			failedConstructingBuildings = new ArrayList<>();
			destroyBuildings();
			commandPioneers();
			buildBuildings();
			Set<Integer> soldiersWithOrders = new HashSet<>();
			armyGeneral.applyHeavyRules(soldiersWithOrders);
			sendGeologists();
		}
	}

	private List<EResourceType> getNeededResources() {
		List<EResourceType> neededResources = new ArrayList<>();
		neededResources.add(EResourceType.COAL);
		neededResources.add(EResourceType.IRONORE);
		neededResources.add(EResourceType.GOLDORE);

		if(player.getCivilisation() == ECivilisation.EGYPTIAN) {
			neededResources.add(EResourceType.GEMSTONE);
		}

		return neededResources;
	}

	private void sendGeologists() {
		int geologistsCount = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.GEOLOGIST).size();
		List<ShortPoint2D> bearersPositions = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.BEARER);
		int bearersCount = bearersPositions.size();
		int stoneCutterCount = aiStatistics.getNumberOfBuildingTypeForPlayer(STONECUTTER, playerId);

		List<EResourceType> neededResources = getNeededResources();

		if (geologistsCount == 0 && stoneCutterCount >= 1 && bearersCount - neededResources.size() > MINIMUM_NUMBER_OF_BEARERS) {
			Map<EResourceType, ILogicMovable> targetGeologists = new EnumMap<>(EResourceType.class);
			List<Integer> convertBearers = new ArrayList<>();

			int bearerIndex = 0;
			for(EResourceType neededResource : neededResources) {
				ILogicMovable targetBearer = getBearerAt(bearersPositions.get(bearerIndex));
				targetGeologists.put(neededResource, targetBearer);

				convertBearers.add(targetBearer.getID());
				bearerIndex++;
			}

			taskScheduler.scheduleTask(new ConvertGuiTask(playerId, convertBearers, EMovableType.GEOLOGIST));

			for(Map.Entry<EResourceType, ILogicMovable> targetGeologist : targetGeologists.entrySet()) {
				sendGeologistToNearest(targetGeologist.getValue(), targetGeologist.getKey());
			}

		}
	}

	private void sendGeologistToNearest(ILogicMovable geologist, EResourceType resourceType) {
		ShortPoint2D resourcePoint = aiStatistics.getNearestResourcePointForPlayer(aiStatistics.getPositionOfPartition(playerId), resourceType, playerId, Integer.MAX_VALUE,
				geologistFilters[resourceType.ordinal]);
		if (resourcePoint == null) {
			resourcePoint = aiStatistics.getNearestResourcePointInDefaultPartitionFor(
					aiStatistics.getPositionOfPartition(playerId), resourceType, Integer.MAX_VALUE, geologistFilters[resourceType.ordinal]);
		}
		if (resourcePoint != null) {
			sendMovableTo(geologist, resourcePoint, EMoveToType.DEFAULT);
		}
	}

	private ILogicMovable getBearerAt(ShortPoint2D point) {
		return mainGrid.getMovableGrid().getMovableAt(point.x, point.y);
	}

	private void sendMovableTo(ILogicMovable movable, ShortPoint2D target, EMoveToType moveToType) {
		if (movable != null) {
			taskScheduler.scheduleTask(new MoveToGuiTask(playerId, target, Collections.singletonList(movable.getID()), moveToType));
		}
	}

	private void destroyBuildings() {
		int stonecutterWorkRadius = STONECUTTER.getVariant(player.getCivilisation()).getWorkRadius();

		// destroy stonecutters or set their work areas
		for (ShortPoint2D stoneCutterPosition : aiStatistics.getBuildingPositionsOfTypeForPlayer(STONECUTTER, playerId)) {
			if (aiStatistics.getBuildingAt(stoneCutterPosition).cannotWork()) {
				int numberOfStoneCutters = aiStatistics.getNumberOfBuildingTypeForPlayer(STONECUTTER, playerId);

				ShortPoint2D nearestStone = aiStatistics.getStonesForPlayer(playerId)
						.getNearestPoint(stoneCutterPosition, stonecutterWorkRadius * MAXIMUM_STONECUTTER_WORK_RADIUS_FACTOR, null);
				if (nearestStone != null && numberOfStoneCutters < economyMinister.getMidGameNumberOfStoneCutters()) {
					taskScheduler.scheduleTask(new WorkAreaGuiTask(EGuiAction.SET_WORK_AREA, playerId, nearestStone, stoneCutterPosition));
				} else {
					taskScheduler.scheduleTask(new SimpleBuildingGuiTask(EGuiAction.DESTROY_BUILDING, playerId, stoneCutterPosition));
					break; // destroy only one stone cutter
				}
			}
		}

		// TODO set work area of rice farm

		// destroy livinghouses
		if (economyMinister.automaticLivingHousesEnabled()) {
			int numberOfFreeBeds = player.getBedInformation().getTotalBedAmount()
					- aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.BEARER).size();
			if (numberOfFreeBeds >= NUMBER_OF_SMALL_LIVING_HOUSE_BEDS + 1 && !destroyLivingHouse(SMALL_LIVINGHOUSE)) {
				if (numberOfFreeBeds >= NUMBER_OF_MEDIUM_LIVING_HOUSE_BEDS + 1 && !destroyLivingHouse(MEDIUM_LIVINGHOUSE)) {
					if (numberOfFreeBeds >= NUMBER_OF_BIG_LIVING_HOUSE_BEDS + 1) {
						destroyLivingHouse(BIG_LIVINGHOUSE);
					}
				}
			}
		}

		// destroy not necessary buildings to get enough space for livinghouses in end-game
		if (isEndGame && isWoodJam()) {
			List<ShortPoint2D> foresters = aiStatistics.getBuildingPositionsOfTypeForPlayer(FORESTER, playerId);
			if (foresters.size() > 1) {
				for (int i = 1; i < foresters.size(); i++) {
					taskScheduler.scheduleTask(new SimpleBuildingGuiTask(EGuiAction.DESTROY_BUILDING, playerId, foresters.get(i)));
				}
			}

			aiStatistics.getBuildingPositionsOfTypeForPlayer(LUMBERJACK, playerId).stream()
					.filter(lumberJackPosition -> aiStatistics.getBuildingAt(lumberJackPosition).cannotWork())
					.forEach(lumberJackPosition -> taskScheduler.scheduleTask(new SimpleBuildingGuiTask(EGuiAction.DESTROY_BUILDING, playerId, lumberJackPosition)));

			if ((aiStatistics.getNumberOfBuildingTypeForPlayer(SAWMILL, playerId) * 3 - 2) > aiStatistics.getNumberOfBuildingTypeForPlayer(LUMBERJACK, playerId)) {
				taskScheduler.scheduleTask(new SimpleBuildingGuiTask(EGuiAction.DESTROY_BUILDING, playerId, aiStatistics.getBuildingPositionsOfTypeForPlayer(SAWMILL, playerId).get(0)));
			}

			for (ShortPoint2D bigTemple : aiStatistics.getBuildingPositionsOfTypeForPlayer(BIG_TEMPLE, playerId)) {
				taskScheduler.scheduleTask(new SimpleBuildingGuiTask(EGuiAction.DESTROY_BUILDING, playerId, bigTemple));
			}
		}
	}

	private boolean destroyLivingHouse(EBuildingType livingHouseType) {
		for (ShortPoint2D livingHousePosition : aiStatistics.getBuildingPositionsOfTypeForPlayer(livingHouseType, playerId)) {
			if (aiStatistics.getBuildingAt(livingHousePosition).cannotWork()) {
				taskScheduler.scheduleTask(new SimpleBuildingGuiTask(EGuiAction.DESTROY_BUILDING, playerId, livingHousePosition));
				return true;
			}
		}
		return false;
	}

	private boolean isWoodJam() {
		return aiStatistics.getNumberOfMaterialTypeForPlayer(EMaterialType.TRUNK, playerId) > aiStatistics.getNumberOfBuildingTypeForPlayer(LUMBERJACK, playerId) * 2;
	}

	private void buildBuildings() {
		if (aiStatistics.getNumberOfNotFinishedBuildingsForPlayer(playerId) < economyMinister.getNumberOfParallelConstructionSites()) {
			if (economyMinister.automaticLivingHousesEnabled() && buildLivingHouse())
				return;
			if (isEndGame) {
				return;
			}
			if (isLackOfSettlers()) {
				return;
			}
			if (buildTower()) {
				return;
			}
			if (buildStock())
				return;
			buildEconomy();
		}
	}

	private boolean buildTower() {
		for (ShortPoint2D towerPosition : aiStatistics.getBuildingPositionsOfTypeForPlayer(TOWER, playerId)) {
			Building tower = aiStatistics.getBuildingAt(towerPosition);
			if (!tower.isConstructionFinished() || !tower.isOccupied()) {
				return false;
			}
		}

		List<ShortPoint2D> threatenedBorder = aiStatistics.threatenedBorderOf(playerId);
		if (threatenedBorder.size() == 0) {
			return false;
		}

		ShortPoint2D position = constructionPositionFinderFactory
				.getBorderDefenceConstructionPosition(threatenedBorder)
				.findBestConstructionPosition();
		if (position != null) {
			taskScheduler.scheduleTask(new ConstructBuildingTask(EGuiAction.BUILD, playerId, position, TOWER));
			sendSwordsmenToTower(position);
			return true;
		}
		return false;
	}

	private void sendSwordsmenToTower(ShortPoint2D position) {
		ILogicMovable soldier = aiStatistics.getNearestSwordsmanOf(position, playerId);
		if (soldier != null) {
			sendMovableTo(soldier, position, EMoveToType.DEFAULT);
		}
	}

	private boolean buildStock() {
		int goldProducers = 0;
		goldProducers += aiStatistics.getTotalNumberOfBuildingTypeForPlayer(GOLDMELT, playerId);
		goldProducers += aiStatistics.getTotalNumberOfBuildingTypeForPlayer(GEMSMINE, playerId);
		if (goldProducers < 1) {
			return false;
		}

		int stockCount = aiStatistics.getTotalNumberOfBuildingTypeForPlayer(STOCK, playerId);
		int goldCount = aiStatistics.getNumberOfMaterialTypeForPlayer(GOLD, playerId);
		int gemsCount = aiStatistics.getNumberOfMaterialTypeForPlayer(GEMS, playerId);
		int totalStore = goldCount;
		if(player.getCivilisation() == ECivilisation.EGYPTIAN) {
			totalStore += gemsCount;
		}

		return stockCount * 6 * 8 - 32 < totalStore && construct(STOCK);
	}

	private void buildEconomy() {
		Map<EBuildingType, Integer> playerBuildingPlan = new HashMap<>();
		for (EBuildingType currentBuildingType : economyMinister.getBuildingsToBuild()) {
			addBuildingCountToBuildingPlan(currentBuildingType, playerBuildingPlan);
			if (buildingNeedsToBeBuild(playerBuildingPlan, currentBuildingType)
					&& buildingDependenciesAreFulfilled(currentBuildingType)
					&& construct(currentBuildingType)) {
				return;
			}
		}
	}

	private boolean buildingDependenciesAreFulfilled(EBuildingType targetBuilding) {
		switch (targetBuilding) {
		case IRONMINE:
			return ratioFits(COALMINE, COAL_MINE_TO_IRON_MINE_RATIO, IRONMINE);
		case WEAPONSMITH:
			return ratioFits(IRONMELT, IRONMELT_TO_WEAPON_SMITH_RATIO, WEAPONSMITH);
		case IRONMELT:
			return ratioFits(COALMINE, COAL_MINE_TO_SMITH_RATIO, IRONMELT)
					&& ratioFits(IRONMINE, IRON_MINE_TO_IRONMELT_RATIO, IRONMELT);
		case BARRACK:
			return ratioFits(WEAPONSMITH, WEAPON_SMITH_TO_BARRACKS_RATIO, BARRACK);
		case MILL:
			return ratioFits(FARM, FARM_TO_MILL_RATIO, MILL);
		case BAKER:
			return ratioFits(FARM, FARM_TO_BAKER_RATIO, BAKER);
		case WATERWORKS:
			return ratioFits(FARM, FARM_TO_WATERWORKS_RATIO, WATERWORKS) || ratioFits(MEAD_BREWERY, MEAD_BREWERY_TO_WATERWORKS_RATIO,WATERWORKS);
		case SLAUGHTERHOUSE:
			return ratioFits(FARM, FARM_TO_SLAUGHTER_RATIO, SLAUGHTERHOUSE);
		case PIG_FARM:
			return ratioFits(FARM, FARM_TO_PIG_FARM_RATIO, PIG_FARM);
		case TEMPLE:
			return ratioFits(player.getCivilisation().getMannaBuilding(), MANNA_BUILDING_TO_TEMPLE_RATIO, TEMPLE);
		case BREWERY:
			return aiStatistics.getNumberOfBuildingTypeForPlayer(EBuildingType.FARM, playerId) > 0;
		case SAWMILL:
			return ratioFits(LUMBERJACK, LUMBERJACK_TO_SAWMILL_RATIO, SAWMILL);
		case FORESTER:
			return ratioFits(LUMBERJACK, LUMBERJACK_TO_FORESTER_RATIO, FORESTER);
		case DISTILLERY:
			return ratioFits(RICE_FARM, RICE_FARM_TO_DISTILLERY_RATIO, DISTILLERY);
		case MEAD_BREWERY:
			return ratioFits(BEEKEEPING, BEEKEEPING_TO_MEAD_BREWERY_RATIO, MEAD_BREWERY);
		default:
			return true;
		}
	}

	private boolean ratioFits(EBuildingType leftBuilding, double leftToRightBuildingRatio, EBuildingType rightBuilding) {
		return aiStatistics.getTotalNumberOfBuildingTypeForPlayer(leftBuilding,
				playerId) >= (double) aiStatistics.getTotalNumberOfBuildingTypeForPlayer(rightBuilding, playerId) * leftToRightBuildingRatio;
	}

	private boolean buildingNeedsToBeBuild(Map<EBuildingType, Integer> playerBuildingPlan, EBuildingType currentBuildingType) {
		int currentNumberOfBuildings = aiStatistics.getTotalNumberOfBuildingTypeForPlayer(currentBuildingType, playerId);
		int targetNumberOfBuildings = playerBuildingPlan.get(currentBuildingType);
		return currentNumberOfBuildings < targetNumberOfBuildings;
	}

	private void addBuildingCountToBuildingPlan(EBuildingType buildingType, Map<EBuildingType, Integer> playerBuildingPlan) {
		if (!playerBuildingPlan.containsKey(buildingType)) {
			playerBuildingPlan.put(buildingType, 0);
		}
		playerBuildingPlan.put(buildingType, playerBuildingPlan.get(buildingType) + 1);
	}

	private boolean isLackOfSettlers() {
		return aiStatistics.getPositionsOfJoblessBearersForPlayer(playerId).size() == 0;
	}

	private void commandPioneers() {
		if (isLackOfSettlers()) {
			releasePioneers(10);
		} else if (aiStatistics.getBorderIngestibleByPioneersOf(playerId).isEmpty() || !aiStatistics.getEnemiesInTownOf(playerId).isEmpty()) {
			releasePioneers(Integer.MAX_VALUE);
		} else if (aiStatistics.getNumberOfTotalBuildingsForPlayer(playerId) >= 4) {
			sendOutPioneers();
		}
	}

	private void releasePioneers(int numberOfPioneers) {
		broadenerPioneers.clear();
		resourcePioneers.clear();
		List<ShortPoint2D> pioneers = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.PIONEER);
		if (!pioneers.isEmpty()) {
			List<Integer> pioneerIds = pioneers.stream()
					.limit(numberOfPioneers)
					.map(pioneerPosition -> mainGrid.getMovableGrid().getMovableAt(pioneerPosition.x, pioneerPosition.y).getID())
					.collect(Collectors.toList());
			taskScheduler.scheduleTask(new ConvertGuiTask(playerId, pioneerIds, EMovableType.BEARER));
			if (numberOfPioneers == Integer.MAX_VALUE) {
				// pioneers which can not be converted shall walk into player's land to be converted the next tic
				taskScheduler.scheduleTask(new MoveToGuiTask(playerId, aiStatistics.getPositionOfPartition(playerId), pioneerIds, EMoveToType.FORCED));
			}
		}
	}

	private void sendOutPioneers() {
		resourcePioneers.removeDeadPioneers();
		broadenerPioneers.removeDeadPioneers();

		if (!resourcePioneers.isFull()) {
			fill(resourcePioneers);
		} else if (!broadenerPioneers.isFull()) {
			fill(broadenerPioneers);
		}
		setNewTargetForResourcePioneers();
		setNewTargetForBroadenerPioneers();
	}

	private void setNewTargetForBroadenerPioneers() {
		if (broadenerPioneers.isNotEmpty()) {
			PioneerGroup pioneersWithNoAction = broadenerPioneers.getPioneersWithNoAction();
			ShortPoint2D broadenTarget = pioneerAi.findBroadenTarget();
			if (broadenTarget != null) {
				taskScheduler.scheduleTask(new MoveToGuiTask(playerId, broadenTarget, pioneersWithNoAction.getPioneerIds(), EMoveToType.DEFAULT));
			}
		}
	}

	private void setNewTargetForResourcePioneers() {
		if (resourcePioneers.isNotEmpty()) {
			ShortPoint2D resourceTarget = pioneerAi.findResourceTarget();
			if (resourceTarget != null) {
				taskScheduler.scheduleTask(new MoveToGuiTask(playerId, resourceTarget, resourcePioneers.getPioneerIds(), EMoveToType.DEFAULT));
			}
		}
	}

	private void fill(PioneerGroup pioneerGroup) {
		int numberOfBearers = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.BEARER).size();
		int numberOfJoblessBearers = aiStatistics.getPositionsOfJoblessBearersForPlayer(playerId).size();

		int minRequiredJoblessBearers = Math.max(MINIMUM_NUMBER_OF_JOBLESS_BEARERS, (int) (MINIMUM_NUMBER_OF_JOBLESS_BEARERS_PER_BUILDING * aiStatistics.getNumberOfTotalBuildingsForPlayer(playerId)));
		int maxNewPioneersCount = Math.min(numberOfBearers - MINIMUM_NUMBER_OF_BEARERS, numberOfJoblessBearers - minRequiredJoblessBearers);

		if (maxNewPioneersCount > 0) {
			pioneerGroup.fill(taskScheduler, aiStatistics, playerId, maxNewPioneersCount);
		}
	}

	private boolean buildLivingHouse() {
		int futureNumberOfBearers = aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, EMovableType.BEARER).size()
				+ aiStatistics.getNumberOfNotFinishedBuildingTypesForPlayer(BIG_LIVINGHOUSE, playerId) * NUMBER_OF_BIG_LIVING_HOUSE_BEDS
				+ aiStatistics.getNumberOfNotFinishedBuildingTypesForPlayer(SMALL_LIVINGHOUSE, playerId) * NUMBER_OF_SMALL_LIVING_HOUSE_BEDS
				+ aiStatistics.getNumberOfNotFinishedBuildingTypesForPlayer(MEDIUM_LIVINGHOUSE, playerId) * NUMBER_OF_MEDIUM_LIVING_HOUSE_BEDS;
		if (futureNumberOfBearers < MINIMUM_NUMBER_OF_BEARERS
				|| (aiStatistics.getNumberOfTotalBuildingsForPlayer(playerId) + aiStatistics.getNumberOfBuildingTypeForPlayer(WEAPONSMITH, playerId) * WEAPON_SMITH_FACTOR)
						* NUMBER_OF_BEARERS_PER_HOUSE > futureNumberOfBearers) {
			if (aiStatistics.getTotalNumberOfBuildingTypeForPlayer(STONECUTTER, playerId) < 1
					|| aiStatistics.getTotalNumberOfBuildingTypeForPlayer(LUMBERJACK, playerId) < 1) {
				return construct(SMALL_LIVINGHOUSE);
			} else if (aiStatistics.getTotalNumberOfBuildingTypeForPlayer(WEAPONSMITH, playerId) < 2) {
				return construct(MEDIUM_LIVINGHOUSE);
			} else {
				return construct(BIG_LIVINGHOUSE);
			}
		}
		return false;
	}

	private boolean construct(EBuildingType type) {
		if (failedConstructingBuildings.size() > 1 && failedConstructingBuildings.contains(type)) {
			return false;
		}
		ShortPoint2D position = constructionPositionFinderFactory
				.getBestConstructionPositionFinderFor(type)
				.findBestConstructionPosition();
		if (position != null) {
			taskScheduler.scheduleTask(new ConstructBuildingTask(EGuiAction.BUILD, playerId, position, type));
			if (type.isMilitaryBuilding()) {
				sendSwordsmenToTower(position);
			}
			return true;
		}
		failedConstructingBuildings.add(type);
		return false;
	}

	@Override
	public String toString() {
		return "Player " + playerId + " with " + economyMinister.toString() + " and " + armyGeneral.toString();
	}
}
