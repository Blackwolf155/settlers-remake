/*******************************************************************************
 * Copyright (c) 2018
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
package go.graphics.swing.contextcreator;

import java.awt.Component;

import go.graphics.swing.ContextContainer;

public abstract class ContextCreator<T extends Component> {

	public ContextCreator(ContextContainer ac, boolean debug) {
		parent = ac;
		this.debug = debug;
	}

	protected int width = 1, height = 1;
	protected boolean first_draw = true;
	protected int fpsLimit = 0;
	protected T canvas;
	protected ContextContainer parent;
	protected boolean debug;


	public abstract void stop();
	public abstract void initSpecific();

	public void repaint() {
		canvas.repaint();
	}

	public void requestFocus() {
		canvas.requestFocus();
	}


	protected void error(String message) throws ContextException {
		parent.fatal(message);
		throw new ContextException();
	}

	public void init() {
		initSpecific();

		parent.addCanvas(canvas);
	}

	public void updateFPSLimit(int fpsLimit) {
		this.fpsLimit = fpsLimit;
	}
}
