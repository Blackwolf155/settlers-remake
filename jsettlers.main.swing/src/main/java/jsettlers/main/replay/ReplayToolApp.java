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
package jsettlers.main.replay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import jsettlers.common.CommonConstants;
import jsettlers.common.resources.ResourceManager;
import jsettlers.common.resources.SwingResourceProvider;
import jsettlers.logic.map.loading.MapLoadException;
import jsettlers.main.swing.SwingResourceLoader;
import jsettlers.main.swing.settings.SettingsManager;

/**
 * @author Andreas Eberle
 */
public class ReplayToolApp {

	public static void main(String[] args) throws IOException, InterruptedException, MapLoadException, SwingResourceLoader.ResourceSetupException {
		CommonConstants.ENABLE_CONSOLE_LOGGING = true;

		ResourceManager.setProvider(new SwingResourceProvider());
		SettingsManager.setup(args);
		SwingResourceLoader.setup();

		int targetGameTimeMinutes = SettingsManager.getInstance().getTargetTimeMinutes().orElseThrow(() -> new IllegalArgumentException("Please specify target time"));
		String replayFileString = SettingsManager.getInstance().getReplayFile().orElseThrow(() -> new IllegalArgumentException("Replay file needs to be specified with --replayFile=<FILE>"));

		File replayFile = new File(replayFileString);
		if (!replayFile.exists()) {
			throw new FileNotFoundException("Found replayFile parameter, but file can not be found: " + replayFile);
		}

		ReplayUtils.replayAndCreateSavegame(new ReplayUtils.ReplayFile(replayFile), targetGameTimeMinutes, "replayForSavegame.log");

		Thread.sleep(2000L);
		System.exit(0);
	}
}
