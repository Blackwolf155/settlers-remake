/*******************************************************************************
 * Copyright (c) 2015
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
package jsettlers.main;

import jsettlers.common.ai.EPlayerType;
import jsettlers.common.menu.IMapDefinition;
import jsettlers.common.menu.IMultiplayerConnector;
import jsettlers.common.menu.IStartScreen;
import jsettlers.common.menu.IStartingGame;
import jsettlers.common.menu.Player;
import jsettlers.common.utils.collections.ChangingList;
import jsettlers.logic.map.loading.MapLoader;
import jsettlers.logic.map.loading.list.MapList;
import jsettlers.logic.player.PlayerSetting;

/**
 * This class implements the {@link IStartScreen} interface and acts as connector between the start screen and the game logic.
 *
 * @author Andreas Eberle
 */
public class StartScreenConnector implements IStartScreen {

	private final MapList mapList;

	public StartScreenConnector() {
		this.mapList = MapList.getDefaultList();
	}

	@Override
	public ChangingList<? extends IMapDefinition> getSingleplayerMaps() {
		return mapList.getFreshMaps();
	}

	@Override
	public ChangingList<? extends IMapDefinition> getStoredSingleplayerGames() {
		return mapList.getSavedMaps();
	}

	@Override
	public ChangingList<? extends IMapDefinition> getMultiplayerMaps() {
		return getSingleplayerMaps();
	}

	@Override
	public ChangingList<? extends IMapDefinition> getRestorableMultiplayerGames() {
		return getStoredSingleplayerGames();
	}

	@Override
	public IStartingGame startSingleplayerGame(IMapDefinition map) {
		MapLoader mapLoader = mapList.getMapById(map.getMapId());

		byte playerId = (byte) 0;
		PlayerSetting[] playerSettings = PlayerSetting.createDefaultSettings(playerId, (byte) mapLoader.getMaxPlayers());

		JSettlersGame game = new JSettlersGame(mapLoader, 4711L, playerId, playerSettings);
		return game.start();
	}

	@Override
	public IStartingGame loadSingleplayerGame(IMapDefinition map) {
		MapLoader mapLoader = mapList.getMapById(map.getMapId());

		PlayerSetting[] playerSettings = mapLoader.getFileHeader().getPlayerSettings();

		byte playerId = 0; // find playerId of HUMAN player
		for (byte i = 0; i < playerSettings.length; i++) {
			if (playerSettings[i].getPlayerType() == EPlayerType.HUMAN) {
				playerId = i;
				break;
			}
		}

		JSettlersGame game = new JSettlersGame(mapLoader, 4711L, playerId, playerSettings);
		return game.start();
	}

	@Override
	public IMultiplayerConnector getMultiplayerConnector(String serverAddr, Player player) {
		return new MultiplayerConnector(serverAddr, player.getId(), player.getName());
	}

}
