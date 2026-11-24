package mezz.jei.common.search;

import mezz.jei.api.search.ISearchStorage;
import mezz.jei.api.search.ISearchStorageBuilder;
import net.mezzdev.bakedsubstring.BakedSubstringIndex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class BakedSubstringIndexBuilder<T> implements ISearchStorageBuilder<T> {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final boolean BAKED_INDEX_AVAILABLE = checkBakedIndexAvailable();

	private final ISearchStorageBuilder<T> delegate;

	public BakedSubstringIndexBuilder() {
		this.delegate = BAKED_INDEX_AVAILABLE ? new BakedDelegate<>() : new FallbackDelegate<>();
	}

	private static boolean checkBakedIndexAvailable() {
		try {
			Class.forName("net.mezzdev.bakedsubstring.BakedSubstringIndex", false, BakedSubstringIndexBuilder.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			LOGGER.warn("BakedSubstringIndex library not found; falling back to in-memory substring search storage. Ensure the shaded JEI JAR is installed.", e);
			return false;
		}
	}

	@Override
	public void put(String key, T value) {
		delegate.put(key, value);
	}

	@Override
	public ISearchStorage<T> build() {
		return delegate.build();
	}

	private static final class BakedDelegate<T> implements ISearchStorageBuilder<T> {
		private final BakedSubstringIndex.Builder<T> builder = BakedSubstringIndex.builder();

		@Override
		public void put(String key, T value) {
			builder.put(key, value);
		}

		@Override
		public ISearchStorage<T> build() {
			BakedSubstringIndex<T> bakedStorage = builder.build();
			return new BakedSubstringIndexSearchStorage<>(bakedStorage);
		}
	}

	private static final class FallbackDelegate<T> implements ISearchStorageBuilder<T> {
		private record Entry<T>(String key, T value) {}
		private final List<Entry<T>> entries = new ArrayList<>();

		@Override
		public void put(String key, T value) {
			if (key != null && !key.isEmpty() && value != null) {
				entries.add(new Entry<>(key, value));
			}
		}

		@Override
		public ISearchStorage<T> build() {
			List<Entry<T>> snapshot = java.util.Collections.synchronizedList(new ArrayList<>(entries));
			return new ISearchStorage<>() {
				@Override
				public void put(String key, T value) {
					if (key != null && !key.isEmpty() && value != null) {
						snapshot.add(new Entry<>(key, value));
					}
				}

				@Override
				public void getSearchResults(String token, Consumer<Collection<T>> resultsConsumer) {
					if (token == null || token.isEmpty()) {
						return;
					}
					Set<T> matched = new LinkedHashSet<>();
					for (Entry<T> entry : snapshot) {
						if (entry.key().contains(token)) {
							matched.add(entry.value());
						}
					}
					resultsConsumer.accept(matched);
				}

				@Override
				public void getAllElements(Consumer<Collection<T>> resultsConsumer) {
					Set<T> all = new LinkedHashSet<>(snapshot.size());
					for (Entry<T> entry : snapshot) {
						all.add(entry.value());
					}
					resultsConsumer.accept(all);
				}

				@Override
				public String statistics() {
					return "FallbackSubstringSearchStorage[entries=" + snapshot.size() + "]";
				}
			};
		}
	}
}
