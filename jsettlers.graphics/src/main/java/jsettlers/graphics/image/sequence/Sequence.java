/*
 * Copyright (c) 2015 - 2018
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
package jsettlers.graphics.image.sequence;

import java.util.function.Supplier;
import jsettlers.graphics.image.Image;

/**
 * This is a sequence of images, describing e.g. an animation.
 * 
 * @author Michael Zangl
 *
 * @param <T>
 *            The image type.
 */
public interface Sequence<T extends Image> {

	/**
	 * Gets the length of the sequence.
	 * 
	 * @return The number of images in the seuqence.
	 */
	int length();

	/**
	 * Gets a image at a given position.
	 * 
	 * @param index
	 *            The image index in the sequence.
	 * @return The image.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of the ounds of the base array.
	 */
	T getImage(int index, Supplier<String> custom_name);

	/**
	 * Gets a image at a given position.
	 * 
	 * @param index
	 *            The image index in the sequence.
	 * @return The image, or a null image if the index is out of bounds
	 */
	Image getImageSafe(int index, Supplier<String> custom_name);

}
