package jsettlers.graphics.image.reader.custom.graphics;

import jsettlers.common.images.AnimationSequence;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.DatFileReader;
import jsettlers.graphics.map.draw.ImageProvider;

public class CustomDatFile13 extends GenericCustomDatFile {

	CustomDatFile13(DatFileReader fallback, ImageProvider imageProvider) {
		super(fallback, imageProvider);
	}

	@Override
	protected SingleImage getGuiImage(int index) {
		return null;
	}

	@Override
	protected SingleImage getLandscapeImage(int index) {
		return null;
	}

	@Override
	protected AnimationSequence getSettlerSequence(int index) {
		if(index == 0) {
			return new AnimationSequence("dat13_0", 0, 2);
		}
		return null;
	}
}
