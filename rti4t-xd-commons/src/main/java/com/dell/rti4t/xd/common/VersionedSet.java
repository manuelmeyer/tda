package com.dell.rti4t.xd.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class VersionedSet<T> implements Set<T> {
	
	private static final Logger LOG = LoggerFactory.getLogger(VersionedSet.class);
	
	private ConcurrentHashMap<T, Integer> versionedMap = new ConcurrentHashMap<>(50_000);	
	private Integer version = 0;
	
	public void incrementVersion() {
		version++;
		LOG.info("Data version in set is {}", version);
	}
	
	public int prune() {
		int totalRemoved = 0;
		Iterator<Entry<T, Integer>> iterator = versionedMap.entrySet().iterator();
		while (iterator.hasNext()) {
			if (!iterator.next().getValue().equals(version)) {
				totalRemoved++;
				iterator.remove();
			}
		}
		return totalRemoved;
	}

	@Override
	public int size() {
		return versionedMap.size();
	}

	@Override
	public boolean isEmpty() {
		return versionedMap.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return versionedMap.get(o) != null;
	}
	
	class SetIterator<T> implements Iterator<T> {
		
		private Iterator<Entry<T, Integer>> iterator;
		
		public SetIterator(Iterator<Entry<T, Integer>> iterator) {
			this.iterator = iterator;
		}

		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}

		@Override
		public T next() {
			return iterator.next().getKey();
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new SetIterator<T>(versionedMap.entrySet().iterator());
	}

	@Override
	public Object[] toArray() {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public boolean add(T e) {
		return (versionedMap.put(e, version) != null);
	}

	@Override
	public boolean remove(Object o) {
		return versionedMap.remove(o) != null;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public void clear() {
		versionedMap.clear();
	}
}
