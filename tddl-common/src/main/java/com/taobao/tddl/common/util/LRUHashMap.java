/*(C) 2007-2012 Alibaba Group Holding Limited.	 *This program is free software; you can redistribute it and/or modify	*it under the terms of the GNU General Public License version 2 as	* published by the Free Software Foundation.	* Authors:	*   junyu <junyu@taobao.com> , shenxun <shenxun@taobao.com>,	*   linxuan <linxuan@taobao.com> ,qihao <qihao@taobao.com> 	*/	package com.taobao.tddl.common.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import com.taobao.tddl.common.exception.lru.LRUHashMapException;

/**
 * LRUHashMap ʵ�ֵĽӿڣ� get��put��putIfAbsent��size
 * ˵����size����ȡ�õ��ǵ�ǰ���������������������ֻ��ʵ��������ʵ���������������������ȣ�
 * 1����������ơ��ڲ��ֳ��˶��segement��ÿ��segement�Ƕ����������໥�����š�
 * 2��ÿ��segement�ڲ�ά��һ��˫������(�˻�����)��ÿ������/��ӣ��Ͱѽڵ��ƶ����˻�����β����
 * 3��ÿ��put������ͨ��hash��ɢ��ÿ��segement�У��ж�segment�������Ƿ񵽴���ֵ�� ���������ֵ����ɾ���˻���������ĩβ�Ľڵ㡣
 * 
 * @author xudanhui.pt Dec 31, 2010,1:19:32 PM
 */
public class LRUHashMap<K, V> extends AbstractMap<K, V> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9078805838094458404L;

	// Ĭ������
	static final int DEFAULT_INITIAL_CAPACITY = 16;

	// Ĭ��װ�����ӣ�����hash��
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	// Ĭ�ϲ�������
	static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	// �������
	static final int MAXIMUM_CAPACITY = 1 << 30;

	// ��󲢷�����--16����
	static final int MAX_SEGMENTS = 1 << 16;

	// ����֮ǰ���Դ�����
	static final int RETRIES_BEFORE_LOCK = 2;

	final int segmentMask;

	final int segmentShift;

	
	int size;

	final Segment<K, V>[] segments;

	private static int hash(int h) {
		// Spread bits to regularize both segment and index locations,
		// using variant of single-word Wang/Jenkins hash.
		h += (h << 15) ^ 0xffffcd7d;
		h ^= (h >>> 10);
		h += (h << 3);
		h ^= (h >>> 6);
		h += (h << 2) + (h << 14);
		return h ^ (h >>> 16);
	}

	final Segment<K, V> segmentFor(int hash) {
		return segments[(hash >>> segmentShift) & segmentMask];
	}

	/**
	 * Hash�ڵ�
	 * 
	 * @author xudanhui.pt Dec 31, 2010,1:24:11 PM
	 * @param <K>
	 * @param <V>
	 */
	static class HashEntry<K, V> {
		/**
		 * ��
		 */
		final K key;
		/**
		 * hashֵ
		 */
		final int hash;
		/**
		 * ֵ
		 */
		volatile V value;
		/**
		 * hash��ָ��
		 */
		final HashEntry<K, V> next;

		/**
		 * ˫���������һ���ڵ�
		 */
		HashEntry<K, V> linknext;
		/**
		 * ˫���������һ���ڵ�
		 */
		HashEntry<K, V> linkpref;

		/**
		 * �������
		 */
		AtomicBoolean dead;

		HashEntry(K key, int hash, HashEntry<K, V> next, V value,
				HashEntry<K, V> linknext, HashEntry<K, V> linkpref,
				AtomicBoolean dead) {
			this.key = key;
			this.hash = hash;
			this.next = next;
			this.value = value;
			this.linknext = linknext;
			this.linkpref = linkpref;
			this.dead = dead;
		}

		static final <K, V> HashEntry<K, V>[] newArray(int i) {
			return new HashEntry[i];
		}
	}

	/**
	 * �˻�����Ľڵ�,�̳���hash���ڵ�
	 * 
	 * @author xudanhui.pt Dec 31, 2010,1:57:58 PM
	 * @param <K>
	 * @param <V>
	 */

	static final class Segment<K, V> extends ReentrantLock implements
			Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * ��ֵ
		 */
		transient int threshold;

		/**
		 * ����
		 */
		transient volatile int count;


		/**
		 * hash��
		 */
		transient volatile HashEntry<K, V>[] table;

		/**
		 * ����ͷ�ڵ㣬һ��˫������ά��
		 */
		transient final HashEntry<K, V> header;// ͷ�ڵ�

		Segment(int initialCapacity, float lf) {
			// ����hash��
			table = HashEntry.<K, V> newArray(initialCapacity);
			// ������ֵ
			threshold = (int) (initialCapacity * lf);

			count = 0;
			// ����˫�������ͷ�ڵ�
			header = new HashEntry<K, V>(null, -1, null, null, null, null,
					new AtomicBoolean(false));
			header.linknext = header;
			header.linkpref = header;
		}

		@SuppressWarnings("unchecked")
		static final <K, V> Segment<K, V>[] newArray(int i) {
			return new Segment[i];
		}

		// ȡ��hash���е�һ��ֵ
		HashEntry<K, V> getFirst(int hash) {
			HashEntry<K, V>[] tab = table;
			return tab[hash & (tab.length - 1)];
		}

		// ������������¶�ֵ
		V readValueUnderLock(HashEntry<K, V> e) {
			lock();
			try {
				return e.value;
			} finally {
				unlock();
			}
		}

		// �ѽڵ��ƶ���˫�������ͷ��
		void moveNodeToHeader(HashEntry<K, V> e) {
			lock();
			try {
				if (!e.dead.get()) {
					// ���˻������жϿ�����
					e.linkpref.linknext = e.linknext;
					e.linknext.linkpref = e.linkpref;

					// ��ӵ��˻�����ͷ��
					header.linknext.linkpref = e;
					e.linknext = header.linknext;
					e.linkpref = header;
					header.linknext = e;
				}
			} finally {
				unlock();
			}

		}

		// get����
		V get(Object key, int hash) {
			HashEntry<K, V> e = getFirst(hash);
			// ��������
			while (e != null) {
				if (e.hash == hash && key.equals(e.key)) {
					V v = e.value;
					// �ѽڵ��ƶ���ͷ����
					moveNodeToHeader(e);
					if (v != null)
						return v;
					// ��������������ض��ܶ�����
					// tab[index] = new HashEntry<K,V>(key, hash, first, value)��
					// value��ֵ��tab[index]��ֵ���ܻ�����������������֮�󣬿��ܻ����ֵ
					// ������ֵ�Ļ�����������������ٶ�һ�飬һ���ܶ���
					return readValueUnderLock(e); // recheck
				}
				e = e.next;
			}
			return null;
		}

		// �������
		V put(K key, int hash, V value, boolean onlyIfAbsent)
				throws LRUHashMapException {
			lock();
			try {
				int c = count;
				// �����ǰ�ڵ������Ѿ����ڵ�����ֵ
				if (c >= threshold) {
					// ִ��ɾ��������
					HashEntry<K, V> node = header.linkpref;
					if (node == null) {
						throw new LRUHashMapException("segment�е��˻����������");
					}
					if (node == header) {
						throw new LRUHashMapException("segment���˻������Ѿ�Ϊ�գ�");
					}
					// �Ѿ����Ϊ��������������˵�������������������
					if (node.dead.get()) {
						throw new LRUHashMapException("node����������");
					}
					// �������
					node.dead.set(true);
					// ��β���ڵ��������ɾ����
					node.linkpref.linknext = node.linknext;
					node.linknext.linkpref = node.linkpref;

					// �ѽڵ��hash����ɾ��
					HashEntry<K, V>[] tab = table;
					int index = node.hash & (tab.length - 1);
					HashEntry<K, V> first = tab[index];
					HashEntry<K, V> e = first;

					while (e != null
							&& (e.hash != node.hash || !e.key.equals(node.key))) {
						e = e.next;
					}
					if (e == null) {
						throw new LRUHashMapException("hash���в�����Ҫɾ���Ľڵ㣡");
					} else {
						HashEntry<K, V> newFirst = e.next;
						// ѭ��ɾ��
						for (HashEntry<K, V> p = first; p != e; p = p.next) {
							HashEntry<K, V> newNode = new HashEntry<K, V>(
									p.key, p.hash, newFirst, p.value,
									p.linknext, p.linkpref, new AtomicBoolean(
											false));
							// ��������
							p.linknext.linkpref = newNode;
							p.linkpref.linknext = newNode;
							p.dead.set(true);// ����������������Ҫ��
							// ��ֵ
							newFirst = newNode;
						}
						tab[index] = newFirst;
						c--;// ������1��
						count = c;
					}
				}

				// ִ��put������
				HashEntry<K, V>[] tab = table;
				int index = hash & (tab.length - 1);
				HashEntry<K, V> first = tab[index];
				HashEntry<K, V> e = first;
				while (e != null && (e.hash != hash || !key.equals(e.key)))
					e = e.next;
				
				V oldValue;

				if (e != null) {
					oldValue = e.value;
					if (!onlyIfAbsent) {
						e.value = value;
						moveNodeToHeader(e);// �ƶ���ͷ��
					}
					
				} else {
					oldValue = null;
					HashEntry<K, V> newNode = new HashEntry<K, V>(key, hash,
							first, value, header.linknext, header,
							new AtomicBoolean(false));

					header.linknext.linkpref = newNode;
					header.linknext = newNode;
					tab[index] = newNode;
					c++;
					count = c;
				}
				return oldValue;
			} finally {
				unlock();
			}
		}

	}

	public LRUHashMap(int initialCapacity, float loadFactor,
			int concurrencyLevel) {

		if (!(loadFactor > 0) || initialCapacity < 0 || concurrencyLevel <= 0)
			throw new IllegalArgumentException();

		if (concurrencyLevel > MAX_SEGMENTS)
			concurrencyLevel = MAX_SEGMENTS;

		this.size=(int) (initialCapacity * loadFactor);
		// Find power-of-two sizes best matching arguments
		int sshift = 0;
		int ssize = 1;
		while (ssize < concurrencyLevel) {
			++sshift;
			ssize <<= 1;
		}
		segmentShift = 32 - sshift;
		segmentMask = ssize - 1;
		this.segments = Segment.newArray(ssize);

		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		int c = initialCapacity / ssize;
		if (c * ssize < initialCapacity)
			++c;
		int cap = 1;
		while (cap < c)
			cap <<= 1;
		for (int i = 0; i < this.segments.length; ++i)
			this.segments[i] = new Segment<K, V>(cap, loadFactor);
	}

	public V putIfAbsent(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		try {
			return segmentFor(hash).put(key, hash, value, true);
		} catch (LRUHashMapException e) {
			e.printStackTrace();
		}
		return value;
	}

	@Override
	public V put(K key, V value) {
		if (value == null)
			throw new NullPointerException();
		int hash = hash(key.hashCode());
		try {
			return segmentFor(hash).put(key, hash, value, false);
		} catch (LRUHashMapException e) {
			e.printStackTrace();
		}
		return value;
	}

	@Override
	public V get(Object key) {
		int hash = hash(key.hashCode());
		return segmentFor(hash).get(key, hash);
	}

	@Override
	public int size() {
		return size;
	
	}
	/**
	 * �������������!
	 */
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		return null;
	}
}
