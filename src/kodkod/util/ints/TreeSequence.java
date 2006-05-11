/**
 * 
 */
package kodkod.util.ints;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An implementation of a sparse sequence based on a
 * balanced binary search tree. 
 * 
 * @author Emina Torlak
 */
public final class TreeSequence<V> extends AbstractSparseSequence<V> 
 implements Cloneable {
	private final IntTree<Entry<V>> tree;
	private int size;
	/**
	 * Constructs an empty tree sequence.
	 * @effects no this.entries'
	 */
	public TreeSequence() {
		tree = new IntTree<Entry<V>>();
		size = 0;
	}

	/**
	 * Copy constructor.
	 * @effects creatres a deep copy of the original
	 */
	@SuppressWarnings("unchecked")
	private TreeSequence(TreeSequence<V> original) {
		this.size = original.size;
		try {
			this.tree = original.tree.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(); // unreachable code;
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.AbstractSparseSequence#iterator(int, int)
	 */
	@Override
	public Iterator<IndexedEntry<V>> iterator(int from, int to) {
		return from <= to ? new AscendingIterator(from, to) : new DescendingIterator(from, to);
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#size()
	 */
	public int size() {
		return size;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#clear()
	 */
	public void clear() {
		tree.clear();
		size = 0;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#put(int, V)
	 */
	public V put(int index, V value) {
		final Entry<V> e = tree.search(index);
		if (e==null) {
			size++;
			tree.insert(new Entry<V>(index, value));
			return null;
		} else {
			return e.setValue(value);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#get(int)
	 */
	public V get(int index) {
		final Entry<V> e = tree.search(index);
		return e==null ? null : e.value;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#remove(int)
	 */
	public V remove(int index) {
		final Entry<V> e = tree.search(index);
		if (e==null)
			return null;
		else {
			size--;
			tree.delete(e);
			return e.value;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#containsIndex(int)
	 */
	public boolean containsIndex(int index) {
		return tree.search(index) != null;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#first()
	 */
	public IndexedEntry<V> first() {
		return tree.min();
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#last()
	 */
	public IndexedEntry<V> last() {
		return tree.max();
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#ceil(int)
	 */
	public IndexedEntry<V> ceil(int index) {
		return tree.searchGTE(index);
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.SparseSequence#floor(int)
	 */
	public IndexedEntry<V> floor(int index) {
		return tree.searchLTE(index);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	public TreeSequence<V> clone() {
		// ok to use copy constructor to clone a final class
		return new TreeSequence<V>(this);
		
	}
	
//	public String toString() {
//		return tree.toString();
//	}

	private static final class Entry<V> extends IntTree.Node<Entry<V>> implements IndexedEntry<V>, Cloneable {
		V value;
		
		Entry(int key, V value) {
			super(key);
			this.value = value;
		}

		public int index() {
			return key;
		}

		public V value() {
			return value;
		}
		
		/**
		 * Sets this.value to the given value and returns the previous value.
		 * @effects this.value' = value
		 * @requires this.value
		 */
		V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}
		
		/**
		 * Compares the specified object with this entry for equality. Returns true if
		 * the given object is also an indexed entry and the two entries 
		 * have the same indeces and values. More formally, two entries e1 and e2 
		 * are equal if e1.index = e2.index && e1.value = e2.value.  This ensures 
		 * that the equals method works properly across different implementations of 
		 * the IndexedEntry interface.
		 * @return o in IndexedEntry && o.index = this.index && o.value = this.value
		 */
		public boolean equals(Object o) {
			if (o==this) return true;
			if (!(o instanceof IndexedEntry)) return false;
			return AbstractSparseSequence.equal(this, (IndexedEntry<?>)o);
		}
		
		/**
		 * Returns the hash code value for this indexed entry. The hash code of an 
		 * indexed entry e is defined to be:
		 * e.index ^ (e.value=null ? 0 : e.value.hashCode()).
		 * This ensures that e1.equals(e2) implies that e1.hashCode()==e2.hashCode() 
		 * for any two IndexedEntries e1 and e2, as required by the general contract of 
		 * Object.hashCode.
		 */
		public int hashCode() {
			return AbstractSparseSequence.hashCode(this);
		}
		
		public String toString() {
			return key + "=" + value;
		}		
		
		protected Entry<V> clone() throws CloneNotSupportedException {
			return (Entry<V>)super.clone();
		}
	}
	
	
	private abstract class EntryIterator implements Iterator<IndexedEntry<V>> {
		final int endIndex;
		Entry<V> lastReturned ;
		Entry<V> next;
		
		/**
		 * Constructs a tree iterator which traverses the tree starting at
		 * the given Entry in either descending or ascending order, depending 
		 * on whether start.index is greater than endIndex. 
		 */
		EntryIterator(Entry<V> start, int endIndex) {
			this.next = start;
			this.lastReturned = null;
			this.endIndex = endIndex;
		}
		
		/**
		 * Advances the next pointer to its successor,
		 * if this is an ascending iterator or to 
		 * its predecessor, if this is a descending iterator.
		 * @requires next != NIL
		 */
		abstract void advance();
		
		public abstract boolean hasNext();
		
		public IndexedEntry<V> next() {
			if (!hasNext())
				throw new NoSuchElementException();
			lastReturned = next;
			advance();
			return lastReturned;
		}
		
		public final void remove() {
			if (lastReturned == null)
				throw new IllegalStateException();
			final int nextIndex = next.key;
			TreeSequence.this.remove(lastReturned.key);
			// necessary since the structural modifications made by the delete
			// procedure may affect the next pointer
			next = tree.search(nextIndex);
			lastReturned = null;
		}
	}
		
	private final class AscendingIterator extends EntryIterator {
		/**
		 * Constructs an ascending iterator over the entries with
		 * indeces between from and to.
		 * @requires from <= to
		 */
		AscendingIterator(int from, int to) {
			super(tree.searchGTE(from),to);
		}
		/**
		 * Sets next to its successor.
		 */
		final void advance() {
			next = tree.successor(next);
		}
		
		/**
		 * Returns true if next != NIL and its index is less 
		 * than or equal to the ending index.
		 */
		public boolean hasNext() { 
			return next != null && next.key<=endIndex; 
		}
	}
	
	private final class DescendingIterator extends EntryIterator {
		/**
		 * Constructs a descending iterator over the entries with
		 * indeces between from and to.
		 * @requires from >= to
		 */
		DescendingIterator(int from, int to) {
			super(tree.searchLTE(from),to);
		}
		/**
		 * Sets next to its predecessor.
		 */
		final void advance() {
			next = tree.predecessor(next);
		}
		
		/**
		 * Returns true if next != NIL and its index is greater 
		 * than or equal to the ending index.
		 */
		public boolean hasNext() { 
			return next != null && next.key>=endIndex; 
		}
	}
	
}