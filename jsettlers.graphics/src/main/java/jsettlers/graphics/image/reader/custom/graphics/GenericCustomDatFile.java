package jsettlers.graphics.image.reader.custom.graphics;

import java.util.function.Function;
import jsettlers.common.images.AnimationSequence;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.reader.DatFileReader;
import jsettlers.graphics.image.reader.EmptyDatFile;
import jsettlers.graphics.image.reader.WrappedAnimation;
import jsettlers.graphics.image.sequence.ArraySequence;
import jsettlers.graphics.image.sequence.Sequence;
import jsettlers.graphics.image.sequence.SequenceList;
import jsettlers.graphics.map.draw.ImageProvider;

public abstract class GenericCustomDatFile extends EmptyDatFile {

	private final DatFileReader fallback;
	private final ImageProvider imageProvider;

	GenericCustomDatFile(DatFileReader fallback, ImageProvider imageProvider) {
		this.fallback = fallback;
		this.imageProvider = imageProvider;
	}

	@Override
	public Sequence<SingleImage> getGuis() {
		return getSequenceWithFallback(fallback.getGuis(), this::getGuiImage);
	}

	@Override
	public Sequence<SingleImage> getLandscapes() {
		return getSequenceWithFallback(fallback.getLandscapes(), this::getLandscapeImage);
	}

	public Sequence<SingleImage> getSequenceWithFallback(Sequence<SingleImage> fallback, Function<Integer, SingleImage> realFunc) {
		SingleImage[] images = new SingleImage[fallback.length()];
		for(int i = 0; i < images.length; i++) {
			SingleImage our = realFunc.apply(i);
			if(our == null) {
				our = fallback.getImage(i, () -> "unknown name");
			}

			images[i] = our;
		}
		return new ArraySequence<>(images);
	}

	protected abstract SingleImage getGuiImage(int index);

	protected abstract SingleImage getLandscapeImage(int index);

	protected abstract AnimationSequence getSettlerSequence(int index);

	@Override
	public SequenceList<Image> getSettlers() {
		return new SequenceList<>() {
			private final SequenceList<Image> fallbackSequence = fallback.getSettlers();

			@Override
			public Sequence<Image> get(int index) {
				AnimationSequence seq = getSettlerSequence(index);
				if (seq != null) {
					return new WrappedAnimation(imageProvider, seq);
				} else {
					return fallbackSequence.get(index);
				}
			}

			@Override
			public int size() {
				return fallbackSequence.size();
			}
		};
	}
}
