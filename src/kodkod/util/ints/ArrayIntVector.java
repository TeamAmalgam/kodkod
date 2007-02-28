/**
 * 
 */
package kodkod.util.ints;

/**
 * A mutable implementation of the <tt>IntVector</tt> interface.  Implements
 * all optional IntVector operations.  In addition to implementing the <tt>IntVector</tt> interface,
 * this class provides methods to manipulate the size of the array that is
 * used internally to store the elements of the IntVector. <p>
 *
 * The <tt>size</tt>, <tt>isEmpty</tt>, <tt>get</tt>, <tt>set</tt>,
 * and <tt>iterator</tt> operations run in constant
 * time.  The <tt>insert</tt> operations run in <i>amortized constant time</i>,
 * that is, adding n elements requires O(n) time.  All of the other operations
 * run in linear time (roughly speaking).  <p>
 *
 * Each <tt>MutableIntVector</tt> instance has a <i>capacity</i>.  The capacity is
 * the size of the array used to store the elements in the list.  It is always
 * at least as large as the list size.  As elements are added to a MutableIntVector,
 * its capacity grows automatically.  The details of the growth policy are not
 * specified beyond the fact that adding an element has constant amortized
 * time cost.<p> 
 *
 * An application can increase the capacity of an <tt>MutableIntVector</tt> instance
 * before adding a large number of elements using the <tt>ensureCapacity</tt>
 * operation.  This may reduce the amount of incremental reallocation.<p>
 * 
 * This impementation is not synchronized and its iterators are not fail-fast.
 * 
 * @author Emina Torlak
 */
public final class ArrayIntVector extends AbstractIntVector {

	private int[] elements;
	private int length;

	/**
	 * Constructs an empty <tt>MutableIntVector</tt> with the specified initial capacity.
	 *
	 * @exception IllegalArgumentException if the specified initial capacity
	 *            is negative
	 */
	public ArrayIntVector(int initialCapacity) {
		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal Capacity: "+
					initialCapacity);
		this.elements = new int[initialCapacity];
	}

	/**
	 * Constructs an empty <tt>MutableIntVector</tt> with an initial capacity of ten.
	 */
	public ArrayIntVector() {
		this(10);
	}

	/**
	 * Constructs a <tt>MutableIntVector</tt> containing the elements of the specified
	 * array, in proper sequence.  The <tt>MutableIntVector</tt> instance has an initial capacity of
	 * 110% the size of the specified collection.
	 *
	 * @throws NullPointerException if the specified array is null.
	 */
	public ArrayIntVector(int[] array) {
		length = array.length;
		// Allow 10% room for growth
		int capacity = (int) Math.min((length*110L)/100, Integer.MAX_VALUE);
		elements = new int[capacity];
		System.arraycopy(array, 0, elements, 0, length);
	}

	/**
	 * Trims the capacity of this <tt>MutableIntVector</tt> instance to be the
	 * list's current size.  An application can use this operation to minimize
	 * the storage of an <tt>MutableIntVector</tt> instance.
	 */
	public void trimToSize() {

		int oldCapacity = elements.length;
		if (length < oldCapacity) {
			int[] oldData = elements;
			elements = new int[length];
			System.arraycopy(oldData, 0, elements, 0, length);
		}
	}

	/**
	 * Increases the capacity of this <tt>MutableIntVector</tt> instance, if
	 * necessary, to ensure  that it can hold at least the number of elements
	 * specified by the minimum capacity argument. 
	 */
	public void ensureCapacity(int minCapacity) {

		int oldCapacity = elements.length;
		if (minCapacity > oldCapacity) {
			int[] oldData = elements;
			int newCapacity = (oldCapacity * 3)/2 + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			elements = new int[newCapacity];
			System.arraycopy(oldData, 0, elements, 0, length);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#copyInto(int[])
	 */
	public void copyInto(int[] array) {
		System.arraycopy(elements, 0, array, 0, length);
	}

	private void checkExcludeLength(int index) {
		if (index < 0 || index >= length)
			throw new IndexOutOfBoundsException();
	}
	
	private void checkIncludeLength(int index) {
		if (index < 0 || index > length)
			throw new IndexOutOfBoundsException();
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#get(int)
	 */
	public int get(int index) {
		checkExcludeLength(index);
		return elements[index];
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#insert(int, int)
	 */
	public void insert(int index, int element) {
		checkIncludeLength(index);

		ensureCapacity(length+1); 
		System.arraycopy(elements, index, elements, index + 1,length - index);
		elements[index] = element;
		length++;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#insert(int, kodkod.util.ints.IntVector)
	 */
	public void insert(int index, IntVector array) {
		checkIncludeLength(index);

		ensureCapacity(length+array.length()); 
		for(int i = 0, alength = array.length(); i < alength; i++) {
			elements[length+i] = array.get(i);
		}
		
		length+=array.length();
	}
	
	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#length()
	 */
	public int length() {
		return length;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#remove(int, int)
	 */
	public void remove(int fromIndex, int toIndex) {
		checkExcludeLength(fromIndex);
		if (toIndex < fromIndex || toIndex > length)
			throw new IndexOutOfBoundsException("toIndex: " + toIndex);
		final int numMoved = length - toIndex;
		if (numMoved > 0) {
			System.arraycopy(elements, toIndex, elements, fromIndex, numMoved);
		}
		length -= (toIndex - fromIndex);
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#set(int, int)
	 */
	public int set(int index, int element) {
		final int oldValue = get(index);
		elements[index] = element;
		return oldValue;
	}

	/**
	 * {@inheritDoc}
	 * @see kodkod.util.ints.IntVector#sort()
	 */
	public void sort() {
		java.util.Arrays.sort(elements, 0, length);
	}

}