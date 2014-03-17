package kodkod.intelision;

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public final class PowerSet {

	private PowerSet() {
		throw new UnsupportedOperationException();
	}
	
	/** Given an integer m, this method returns a set of BitSet objects. For each number between 0
	 * and (2^m)-1, a BitSet is returned, representing the binary encoding of the numbers.
	 * For instance, for m = 2, the method returns [{}, {0}, {1}, {0, 1}], where {} represents 0,
	 * and {0,1} represents 2^0 + 2^1 = 3. 
	 */
	public static List<BitSet> buildPowerSet(final int m) {	
		final List<BitSet> result = new LinkedList<BitSet>();
		//m is the number of metrics, thus we want BitSets for 2^m 
		final int number = (int)Math.pow(2, m);
		
		// loop for integer 0 to (2^m)-1
		for(int i = 0; i < number; i++){
			//derive BitSet for variable value
			int value = i;
			final BitSet bs = new BitSet();
			int bitIndex=0;

			while(value>0)
			{
			final int remainder=value%2;
			if(remainder == 0)
				bs.set(bitIndex, false);
			else
				bs.set(bitIndex, true);
			value/=2;
			bitIndex++;
			}
			result.add(bs);
		}
		// return result set of BitSets
		return Collections.unmodifiableList(result);
	}

	public static BitSet next(final BitSet current) {
		final BitSet next = (BitSet) current.clone();
		
		// regular case
		int firstFalseIndex = -1;
		// work up to the first false bit and flip it
		for (int i = 0; i < next.size(); i++) {
			if (!current.get(i)) {
				firstFalseIndex = i;
				break;
			}
		}
		if (firstFalseIndex == -1) {
			// didn't find a zero: prepend a one
			firstFalseIndex = current.cardinality();
		}

		// now work down from the first false bit, flipping all the way
		for (int i = firstFalseIndex; i >= 0; i--) {
			next.flip(i);
		}

		return next;
	}

	public static Iterator<BitSet> iteratePowerSet(final int m) {
		return new Iterator<BitSet>() {
			private BitSet current = new BitSet();
			private boolean done = false;
			
			@Override
			public boolean hasNext() {
				if (done) {
					return false;
				} else if (current.cardinality() < m) {
					return true;
				} else if (current.cardinality() == m) {
					done = true;
					return true;
				} else {
					throw new IllegalStateException("current.cardinality() == " + current.cardinality());
				}
			}

			@Override
			public BitSet next() {
				final BitSet result = (BitSet) current.clone();
				current = PowerSet.next(current);
				return result;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}};
	}
	
	
	public static <T> Iterator<Set<T>> iteratePowerSet(final Set<T> original) {
		@SuppressWarnings("unchecked")
		final T[] a = (T[]) original.toArray();
		
		return new Iterator<Set<T>>() {
			private Iterator<BitSet> bitSetIterator = iteratePowerSet(original.size());

			@Override
			public boolean hasNext() {
				return bitSetIterator.hasNext();
			}

			@Override
			public Set<T> next() {
				return new AbstractSet<T> () {
					private final BitSet bits = bitSetIterator.next();

					@Override
					public int size() {
						return bits.cardinality();
					}

					@Override
					public Iterator<T> iterator() {
						return new Iterator<T>() {
							int index = 0;
							int numberReturned = 0;
							
							@Override
							public boolean hasNext() {
								return numberReturned < bits.cardinality();
							}

							@Override
							public T next() {
								numberReturned++;
								while (!bits.get(index)) {
									index++;
								}
								return a[index++];
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}};
					}


				};
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	
}
