/*
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
 */
package jsettlers.main.android.core.resources;

import java.io.File;

import android.content.res.AssetManager;

import jsettlers.logic.map.loading.list.DirectoryMapLister;
import jsettlers.logic.map.loading.list.MapList.DefaultMapListFactory;

public class AndroidMapListFactory extends DefaultMapListFactory {

	public AndroidMapListFactory(AssetManager manager, File originalMapsDirectory, File writeableDir) {
		super();
		if(originalMapsDirectory != null) {
			directories.add(new DirectoryMapLister(originalMapsDirectory, false));
		}
		directories.add(new AndroidAssetsMapLister(manager, ""));
		addResourcesDirectory(writeableDir);
	}
}
