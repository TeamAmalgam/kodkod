package kodkod.engine.bool;

import static kodkod.engine.bool.BooleanConstant.FALSE;
import static kodkod.engine.bool.BooleanConstant.TRUE;
import static kodkod.engine.bool.Operator.AND;
import static kodkod.engine.bool.Operator.OR;

import java.util.Iterator;

import kodkod.util.ints.HomogenousSequence;
import kodkod.util.ints.IndexedEntry;
import kodkod.util.ints.IntIterator;
import kodkod.util.ints.IntSet;
import kodkod.util.ints.RangeSequence;
import kodkod.util.ints.SparseSequence;
import kodkod.util.ints.TreeSequence;


/** 
 * <p>Represents an n-dimensional matrix of {@link kodkod.engine.bool.BooleanValue boolean values}.  
 * Boolean matrices are indexed using flat integer indeces.  For example,
 * let m be a the 2 x 3 matrix of boolean variables identifed by labels [0 4 1; 5 10 2].  
 * Then, m[0] = 0, m[3] = 5, m[5] = 2, etc. </p> 
 * 
 * <p>All values stored in the same matrix must be created by the same {@link kodkod.engine.bool.BooleanFactory circuit factory}.  
 * All methods that accept another BooleanMatrix as an input will throw an 
 * IllegalArgumentException if the values in the input matrix do not belong 
 * to the same factory as the values in the receiver matrix. </p>
 * 
 * <p>Some instances can store only constant values, or can only store
 * values at particular indices.  If this is the case, an attempt to call {@link #set(int, BooleanValue) }
 * with invalid parameters will cause an IllegalArgumentException. </p>
 * 
 * @specfield dimensions: Dimensions
 * @specfield factory: BooleanFactory
 * @specfield elements: [0..dimensions.capacity) -> one BooleanValue
 *
 * @author Emina Torlak  
 */
public final class BooleanMatrix implements Iterable<IndexedEntry<BooleanValue>>, Cloneable {
	/* these fields are intended to be used only be instances of BooleanMatrix */
	private final Dimensions dims;
	private final BooleanFactory factory;
	private final SparseSequence<BooleanValue> cells;
	

	/**  
	 * Constructs a new matrix with the given dimensions, factory, and entries.
	 * 
	 * @requires dimensions != null && factory != null && seq != null
	 * @requires seq.indices() in [0..dimensions.capacity)
	 * @effects this.dimensions' = dimensions && this.factory' = factory && 
	 *          this.elements' = [0..dimensions.capacity)->one FALSE 
	 */
	private BooleanMatrix(final Dimensions dimensions, final BooleanFactory factory, SparseSequence<BooleanValue> seq) {
		this.dims = dimensions;
		this.factory = factory;
		this.cells = seq;
	}
	
	/**  
	 * Constructs a new matrix with the given dimensions and factory.
	 * The constructed matrix can store any kind of BooleanValue.
	 * 
	 * @requires dimensions != null && factory != null 
	 * @effects this.dimensions' = dimensions && this.factory' = factory && 
	 *          this.elements' = [0..dimensions.capacity)->one FALSE 
	 */
	BooleanMatrix(Dimensions dims, BooleanFactory factory) {
		this.dims = dims;
		this.factory = factory;
		this.cells = new RangeSequence<BooleanValue>();
	}
	
	/**  
	 * Constructs a new matrix with the given dimensions and factory, 
	 * and initializes the indices in the given set to TRUE.
	 * The constructed matrix will be capable of storing only constants
	 * if the constant flag is on, otherwise, it will be able to store any kind of BooleanValue.
	 * 
	 * @requires constant => trueIndices is not modifiable using an external handle
	 * @requires dimensions != null && factory != null && trueIndices != null
	 * @requires dimensions.validate(trueIndices.min()) && dimensions.validate(trueIndices.max())
	 * @effects this.dimensions' = dimensions && this.factory' = factory && 
	 *          this.elements' = [0..dimensions.capacity)->one FALSE ++ trueIndices -> one TRUE
	 */
	BooleanMatrix(Dimensions dims, BooleanFactory factory, IntSet trueIndices, boolean constant) {
		this.dims = dims;
		this.factory = factory;
		if (constant)
			this.cells = new HomogenousSequence<BooleanValue>(TRUE, trueIndices);
		else if (trueIndices.isEmpty())
			this.cells = new TreeSequence<BooleanValue>();
		else {
			this.cells = new RangeSequence<BooleanValue>();
			for(IntIterator iter = trueIndices.iterator(); iter.hasNext(); ) {
				cells.put(iter.nextInt(), TRUE);
			}
		}
	}
	
	/**
	 * Returns a new empty sparse sequence which can most efficiently hold
	 * the elements storable in the sparse sequences s0 and s1.
	 * @return a new empty sparse sequence which can most efficiently hold
	 * the elements storable in the sparse sequences s0 and s1.
	 */
	private static SparseSequence<BooleanValue> make(SparseSequence<BooleanValue> s0, SparseSequence<BooleanValue> s1) {
		final Class<?> c0 = s0.getClass(), c1 = s1.getClass();
		if (c0!=c1 || c0==RangeSequence.class) 
			return new RangeSequence<BooleanValue>();
		else if (c0==HomogenousSequence.class) 
			return new HomogenousSequence<BooleanValue>(TRUE); 
		else 
			return new TreeSequence<BooleanValue>();	
		
	}
	
	/**
	 * Returns the dimensions of this matrix.
     * @return this.dimensions
     */
	public final Dimensions dimensions() { return dims; }
	
	/**
	 * Returns the factory used to construct all the non-constant
	 * entries in this matrix.
	 * @return this.factory
	 */
	public final BooleanFactory factory() { return factory; }
	
	/**
     * Returns the number of non-FALSE entries in this matrix.
     * @return #this.elements.(BooleanValue - FALSE)
     */
	public final int density() { return cells.size(); }
	
	/**
	 * Returns an IndexedEntry-based view of the non-zero entries in this matrix.  The returned
	 * iterator enumerates indexed entries that represent the non-FALSE entries in the matrix, in the ascending
	 * order of indeces.  For example, suppose that the elements of this are 0->FALSE, 1->(a & b), 2->FALSE, 3->(c | d).  Then,
	 * the Iterator will return two IndexedEntries, c1 then c2, such that c1.index=1 && c1.value = a & b and
	 * c2.index=3 && c.value = c | d.  Calling {@link Iterator#remove()} on the returned iterator has the same effect
	 * as setting the entry obtained through the last call to {@link Iterator#next()} to this.zero.
	 * @return an iterator over IndexedEntries representing the non-FALSE entries in this matrix.
	 */
	public final Iterator<IndexedEntry<BooleanValue>> iterator() {
		return cells.iterator();
	}
	
	/**
	 * Return FALSE if value is null; otherwise return value itself.
	 * @return FALSE if value is null; otherwise return value itself.
	 */
	private final BooleanValue maskNull(BooleanValue value) {
		return value == null ? FALSE : value;
	}
	
	/**
	 * Returns the value at the given index, without checking that the index is in bounds.
	 * @return this.elements[index]
	 */
	private final BooleanValue fastGet(final int index) {
		return maskNull(cells.get(index));
	}
	
	/**
     * Returns the element at the specified index.
     * @return this.elements[index]
     * @throws IndexOutOfBoundsException - index < 0 || index >= this.dimensions.capacity
     */
	public final BooleanValue get(final int index) {
		if (!dims.validate(index)) throw new IndexOutOfBoundsException(index + " is not a valid index.");
		return maskNull(cells.get(index));
	}
	
	
	/**
     * Returns a new matrix each of whose entries is a negation of the 
     * corresponding entry in this matrix.
     * 
     * @return { m: BooleanMatrix | m.dimensions=this.dimensions && m.factory = this.factory &&
     *                              all i: [0..m.dimensions.capacity) | m.elements[i] = !this.elements[i] }
     */
	public final BooleanMatrix not() {
		final BooleanMatrix negation = new BooleanMatrix(dims, factory, make(cells, cells));
		
		for (int i = 0, max = dims.capacity(); i < max; i++) {
			BooleanValue v = cells.get(i);
			if (v==null)
				negation.cells.put(i, TRUE);
			else if (v!=TRUE)
				negation.cells.put(i, v.negation());
		}
		
		return negation;
	}
	
	/**
	 * @throws IllegalArgumentException - f != this.factory
	 */
	private final void checkFactory(BooleanFactory f) {
		if (factory != f) throw new IllegalArgumentException();
	}
	
	/**
	 * @throws IllegalArgumentException - d != this.dimensions
	 */
	private final void checkDimensions(Dimensions d) {
		if (!dims.equals(d)) throw new IllegalArgumentException();
	}
	
	/**
	 * Returns a new matrix such that an entry in the returned matrix represents a 
	 * conjunction of the corresponding entries in this and other matrix.  The effect 
	 * of this method is the same as calling this.compose(Operator.Binary.AND, other).
	 * 
	 * @return { m: BooleanMatrix | m.dimensions = this.dimensions && m.factory = this.factory &&
	 *                              all i: [0..m.dimensions.capacity) | 
	 *                               m.elements[i] = this.elements[i] AND other.elements[i] }
	 * @throws NullPointerException - other = null
	 * @throws IllegalArgumentException - !other.dimensions.equals(this.dimensions) || this.factory != other.factory
	 */
	public final BooleanMatrix and(BooleanMatrix  other) {
		checkFactory(other.factory); checkDimensions(other.dims);
		final BooleanMatrix ret = new BooleanMatrix(dims, factory, make(cells, other.cells));
		final  SparseSequence<BooleanValue> small, big;
		if (cells.size()<=other.cells.size()) {
			small = cells; big = other.cells;
		} else {
			small = other.cells; big = cells;
		}
		for(IndexedEntry<BooleanValue> e0 : small) {
			BooleanValue v1 = big.get(e0.index());
			if (v1!=null)
				ret.fastSet(e0.index(), factory.fastCompose(AND, e0.value(), v1));
		}
		return ret;
	}
	
	/**
	 * Returns a new matrix such that an entry in the returned matrix represents a 
	 * combination of the corresponding entries in this and other matrix.  The effect 
	 * of this method is the same as calling this.compose(Operator.Binary.OR, other).
	 * 
	 * @return { m: BooleanMatrix | m.dimensions = this.dimensions && m.factory = this.factory &&
	 *                              all i: [0..m.dimensions.capacity) | 
	 *                               m.elements[i] = this.elements[i] OR other.elements[i] }
	 * @throws NullPointerException - other = null 
	 * @throws IllegalArgumentException - !other.dimensions.equals(this.dimensions) || this.factory != other.factory
	 */
	public final BooleanMatrix or(BooleanMatrix  other) {
		checkFactory(other.factory); checkDimensions(other.dims);
		final BooleanMatrix ret = new BooleanMatrix(dims, factory, make(cells, other.cells));
		final SparseSequence<BooleanValue> retSeq = ret.cells;
		for(IndexedEntry<BooleanValue> e0 : cells) {
			BooleanValue v1 = other.cells.get(e0.index());
			if (v1==null)
				retSeq.put(e0.index(), e0.value());
			else
				retSeq.put(e0.index(), factory.fastCompose(OR, e0.value(), v1));
		}
		for(IndexedEntry<BooleanValue> e1 : other.cells) {
			if (!cells.containsIndex(e1.index()))
				retSeq.put(e1.index(), e1.value());
		}

		return ret;
	}
	
	/**
     * Returns the cross product of this and other matrix, using conjunction instead of 
     * multiplication.
     * 
     * @return { m: BooleanMatrix | m = this x other }
     * @throws NullPointerException - other = null
     * @throws IllegalArgumentException - this.factory != other.factory
     */
	public final BooleanMatrix cross(final BooleanMatrix other) {
		checkFactory(other.factory);
		
		final BooleanMatrix ret =  new BooleanMatrix(dims.cross(other.dims), factory, make(cells, other.cells));
		if (cells.isEmpty() || other.cells.isEmpty()) return ret;
		
		final int ocap = other.dims.capacity();
		for(IndexedEntry<BooleanValue> e0 : cells) {
			int i = ocap * e0.index();
			for(IndexedEntry<BooleanValue> e1: other.cells) {
				BooleanValue conjunction = factory.fastCompose(AND, e0.value(), e1.value());
				if (conjunction != FALSE)
					ret.cells.put(i + e1.index(), conjunction);
			}
		}
		return ret;
	}
	
	/**
     * Sets the value at the specified index to the given value;
     * returns the value previously at the specified position.  
     * It performs no index or null checking.
     * 
     * @effects this.elements'[index] = formula
     */
	private final void fastSet(final int index, final BooleanValue formula) {
		if (formula==FALSE) cells.remove(index);
		else cells.put(index,formula);
	}
	
	/**
     * Returns the dot product of this and other matrix, using conjunction instead of 
     * multiplication and disjunction instead of addition.
     * 
     * @return { m: BooleanMatrix | m = this*other }
     * @throws NullPointerException - other = null
     * @throws IllegalArgumentException - this.factory != other.factory
     * @throws IllegalArgumentException - dimensions incompatible for multiplication
     */
	public final BooleanMatrix dot(final BooleanMatrix other) {  
		checkFactory(other.factory);
		
		final BooleanMatrix ret =  new BooleanMatrix(dims.dot(other.dims), factory, make(cells, other.cells));
		if (cells.isEmpty() || other.cells.isEmpty()) return ret;
		
		final SparseSequence<BooleanValue> retCells = ret.cells;
		final int b = other.dims.dimension(0); 
		final int c = other.dims.capacity() / b; 
		
		for(IndexedEntry<BooleanValue> e0 : cells) {
			int i = e0.index();
			BooleanValue iVal = e0.value();
			int rowHead = (i % b)*c, rowTail = rowHead + c - 1;
			for(Iterator<IndexedEntry<BooleanValue>> iter1 = other.cells.iterator(rowHead, rowTail); iter1.hasNext();) {
				IndexedEntry<BooleanValue> e1 = iter1.next();
				BooleanValue retVal = factory.fastCompose(AND, iVal, e1.value());
				if (retVal != FALSE) {
					int k = (i / b)*c + e1.index()%c;
					if (retVal==TRUE) retCells.put(k, TRUE);
					else {
						BooleanValue kVal = retCells.get(k);
						if (kVal != TRUE) {
							if (kVal==null) {
								kVal = BooleanAccumulator.treeGate(OR);
								retCells.put(k, kVal);
							} 
							((BooleanAccumulator) kVal).add(retVal);
						}
					}
				}
			}		
		}
		
		// make mutable gates immutable
		for(IndexedEntry<BooleanValue> e : ret.cells) {
			if (e.value()!=TRUE) {
				ret.cells.put(e.index(), factory.fastAdopt((BooleanAccumulator) e.value()));
			}
		}
		return ret;
	}
	
	/**
	 * Returns a formula stating that the entries in this matrix are a subset of 
	 * the entries in the given matrix; i.e. the value of every entry in this matrix
	 * implies the value of the corresponding entry in the given matrix.
	 * @return { f: BooleanValue | f <=> (this.elements[0]=>other.elements[0]) AND ... 
	 *            AND (this.elements[this.dimensions.capacity-1]=>other.elements[this.dimensions.capacity-1]))
	 * @throws NullPointerException -  other = null 
	 * @throws IllegalArgumentException - !other.dimensions.equals(this.dimensions) || this.factory != other.factory           
	 */
	public final BooleanValue subset(BooleanMatrix other) {
		checkFactory(other.factory); checkDimensions(other.dims);
		final BooleanAccumulator a = BooleanAccumulator.treeGate(AND);
		for(IndexedEntry<BooleanValue> e0: cells) {
			if (a.add(factory.fastCompose(OR, e0.value().negation(), other.fastGet(e0.index())))==FALSE)
				return FALSE;
		}
		return factory.fastAdopt(a);
	}
	
	/**
	 * Returns a formula stating that the entries in this matrix are equivalent to 
	 * the entries in the given matrix; i.e. the value of every entry in this matrix
	 * is true if and only if the value of the corresponding entry in the given matrix is true.
	 * The same formula can be obtained by calling factory.and(this.subset(other), other.subset(this)),
	 * but this method performs the operation more efficiently.
	 * @return { f: BooleanValue | f <=> (this.elements[0]<=>other.elements[0]) AND ... 
	 *            AND (this.elements[this.dimensions.capacity-1]<=>other.elements[this.dimensions.capacity-1]))
	 * @throws NullPointerException -  other = null 
	 * @throws IllegalArgumentException - !other.dimensions.equals(this.dimensions) || this.factory != other.factory           
	 */
	public final BooleanValue equivalent(BooleanMatrix other) {
		/* the following encoding usually generates smaller cnfs that are harder to solve than
		 * simply conjoining the mutual subset constraint. */
//		checkFactory(other.factory); checkDimensions(other.dims);
//		final BooleanAccumulator a = BooleanAccumulator.treeGate(AND);
//		for(IndexedEntry<BooleanValue> e0: cells) {
//			BooleanValue v1 = other.fastGet(e0.index());
//			if (a.add(factory.fastITE(e0.value(), v1, v1.negation()))==FALSE)
//				return FALSE;
//		}
//		for(IndexedEntry<BooleanValue> e1: cells) {
//			if (!cells.containsIndex(e1.index()))
//				if (a.add(e1.value().negation())==FALSE)
//					return FALSE;
//		}
//		return factory.fastAdopt(a);
		return factory.fastCompose(AND, this.subset(other), other.subset(this));
	}
	
	/**
	 * Returns a matrix representing the asymmetric difference between
	 * the entries in this and the given matrix.  The same matrix can 
	 * be obtained by calling this.and(other.not()), but this method
	 * performs the operation more efficiently (intermediate
	 * values are not explicity created).
	 * @return { m: BooleanMatrix | m.dimensions = this.dimensions && m.factory = this.factory &&
	 *                              all i: [0..m.dimensions.capacity) | 
	 *                               m.elements[i] = this.elements[i] AND !other.elements[i] }
	 * @throws NullPointerException - other = null
	 * @throws IllegalArgumentException - !other.dimensions.equals(this.dimensions) || this.factory != other.factory
	 */
	public final BooleanMatrix difference(BooleanMatrix other) {
		checkFactory(other.factory); checkDimensions(other.dims);
		final BooleanMatrix ret = new BooleanMatrix(dims, factory, make(cells, other.cells));
		for(IndexedEntry<BooleanValue> e0 : cells) {
			ret.fastSet(e0.index(), factory.fastCompose(AND, e0.value(), other.fastGet(e0.index()).negation()));
		}
		return ret;
	}
	
	/**
     * Returns the transitive closure of this matrix.
     * 
     * @return { m: BooleanMatrix | m = ^this }
     * @throws UnsupportedOperationException - #this.diensions != 2 || !this.dimensions.square()
     */
	public final BooleanMatrix closure() {
		if (dims.numDimensions() != 2 || !dims.isSquare()) {
			throw new UnsupportedOperationException("#this.diensions != 2 || !this.dimensions.square()");
		}
		if (cells.isEmpty())
			return clone();
		
		BooleanMatrix ret = this;
	
		// compute the number of rows in the matrix
		int rowNum = 0;
		final int rowFactor = dims.dimension(1);
		for(IndexedEntry<BooleanValue> rowLead = cells.first(); 
		    rowLead != null; rowLead = cells.ceil(((rowLead.index()/rowFactor) + 1) * rowFactor)) {
			rowNum++; 
		}	
		
		// compute closure using iterative squaring
		for(int i = 1; i < rowNum; i*=2) {
			ret = ret.or(ret.dot(ret));
		}
		
		return ret==this ? clone() : ret;
	}
	
	/**
     * Returns the transpose of this matrix.
     * 
     * @return { m: BooleanMatrix | m = ~this }
     * @throws UnsupportedOperationException - #this.dimensions != 2
     */
	public final BooleanMatrix transpose() {
		final BooleanMatrix ret = new BooleanMatrix(dims.transpose(), factory, make(cells, cells));
		final int rows = dims.dimension(0), cols = dims.dimension(1);
		for (IndexedEntry<BooleanValue> e0 : cells) {
			ret.cells.put((e0.index()%cols)*rows + (e0.index()/cols), e0.value());
		}
		return ret;
	}
		
	/**
	 * Returns a boolean matrix m such that m = this if the given condition evaluates
	 * to TRUE and m = other otherwise.
	 * 
	 * @return { m: BooleanMatrix | m.dimensions = this.dimensions &&
     *                              all i: [0..m.dimensions.capacity) | 
     *                                 m.elements[i] = condition => this.elements[i], other.elements[i] }
     * @throws NullPointerException - other = null || condition = null
     * @throws IllegalArgumentException - !other.dimensions.equals(this.dimensions) || this.factory != other.factory
	 */
	public final BooleanMatrix choice(BooleanValue condition, BooleanMatrix other) {
		checkFactory(other.factory); checkDimensions(other.dims);
		if (condition==TRUE) return this.clone();
		else if (condition==FALSE) return other.clone();
	
		final BooleanMatrix ret =  new BooleanMatrix(dims, factory);
		final SparseSequence<BooleanValue> retSeq = ret.cells, otherCells = other.cells;
		for(IndexedEntry<BooleanValue> e0 : cells) {
			BooleanValue v1 = otherCells.get(e0.index());
			if (v1==null)
				retSeq.put(e0.index(), factory.fastCompose(AND, condition, e0.value()));
			else
				retSeq.put(e0.index(), factory.fastITE(condition, e0.value(), v1));
		}
		for(IndexedEntry<BooleanValue> e1 : other.cells) {
			if (!cells.containsIndex(e1.index()))
				retSeq.put(e1.index(), factory.fastCompose(AND, condition.negation(), e1.value()));
		}
		return ret;
	}
	
	/**
	 * Returns a conjunction of the negated values between 
	 * start, inclusive, and end, exclusive.
	 * @requires 0 <= start < end <= this.dimensions.capacity() 
	 * @return !this.elements[start] && !this.elements[start+1] && ... && !this.elements[end-1]
	 */
	private final BooleanValue nand(int start, int end) {
		final BooleanAccumulator g = BooleanAccumulator.treeGate(AND);
		for(Iterator<IndexedEntry<BooleanValue>> iter = cells.iterator(start, end-1); iter.hasNext(); ) {
			if (g.add(iter.next().value().negation())==FALSE) 
				return FALSE;
		}
		return factory.fastAdopt(g);
	}
	
	/**
	 * Overrides the values in this matrix with those in <code>other</code>.
	 * Specifically, for each index i of the returned matrix m,
	 * m.elements[i] is true iff other.elements[i] is true or
	 * this.elements[i] is true and all elements of <code>other</code> 
	 * that are in the same row as i are false.
	 * @return {m: BooleanMatrix | m.dimensions = this.dimensions &&
	 *                             all i: [0..m.capacity()) | m.elements[i] = 
	 *                               other.elements[i] || 
	 *                               this.elements[i] && !OR(other.elements[row(i)]) }
	 * where other.elements[row(i)] selects all elements of <code>other</code>
	 * that are in the same row as i.  
	 * @throws NullPointerException - other = null
	 * @throws IllegalArgumentException - other.dimensions != this.dimensions
	 */
	public final BooleanMatrix override(BooleanMatrix other) {
		checkFactory(other.factory); checkDimensions(other.dims);
		if (other.cells.isEmpty()) return this.clone();
		final BooleanMatrix ret = new BooleanMatrix(dims, factory, make(cells, other.cells));
		ret.cells.putAll(other.cells);
		final int rowLength = dims.capacity() / dims.dimension(0);
		int row = -1;
		BooleanValue rowVal = BooleanConstant.TRUE;
		for(IndexedEntry<BooleanValue> e0 : cells) {
			int e0row = e0.index() / rowLength;
			if (row != e0row) {
				row = e0row;
				rowVal = other.nand(row*rowLength, (row+1)*rowLength);
			}
			ret.fastSet(e0.index(), factory.fastCompose(OR, ret.fastGet(e0.index()), 
					   factory.fastCompose(AND, e0.value(), rowVal)));
		}
		return ret;
	}
	
	/**
	 * Returns a BooleanValue that constrains at least one value in this.elements to be true.  The
	 * effect of this method is the same as calling this.orFold().
	 * @return { f: BooleanValue | f <=> this.elements[0] || ... || this.elements[this.dimensions.capacity-1] }
	 */
	public final BooleanValue some() {
		final BooleanAccumulator g = BooleanAccumulator.treeGate(OR);
		for(IndexedEntry<BooleanValue> e : cells) {
			if (g.add(e.value())==TRUE) 
				return TRUE;
		}
		return factory.fastAdopt(g);
	}
	
	/**
	 * Returns a BooleanValue that constrains at most one value in this.elements to be true.
	 * The effect of this method is the same as calling this.factory.or(this.one(), this.none()).
	 * @return { f: BooleanValue | f <=> this.one() || this.none() }
	 */
	public final BooleanValue lone() {
		return factory.fastCompose(OR, one(), none());
	}
	
	/**
	 * Returns a BooleanValue that constraints exactly one value in this.elements to be true.
	 * @return { f: BooleanValue | f <=> #this.elements[int] = 1 }
	 */
	public final BooleanValue one() {
		if (cells.isEmpty())
			return FALSE; 
		else {
			final int density = cells.size();
			final BooleanAccumulator g = BooleanAccumulator.treeGate(OR);
			UPDATEg : for (int i = 0; i < density; i++) { // g contains the outermost disjunction
				Iterator<IndexedEntry<BooleanValue>> entries = cells.iterator();
				BooleanAccumulator v = BooleanAccumulator.treeGate(AND); // v contains the individual conjunctions
				for(int j = 0; j < i; j++) {
					if (v.add(entries.next().value().negation())==FALSE) {
						continue UPDATEg;
					}
				}
				if (v.add(entries.next().value())==FALSE) continue;
				for(int j = i+1; j < density; j++) {
					if (v.add(entries.next().value().negation())==FALSE) {
						continue UPDATEg;
					}
				}
				if (g.add(factory.fastAdopt(v))==TRUE) return TRUE;
			}
			return factory.fastAdopt(g);
		}
	}
	
	/**
	 * Returns a BooleanValue that constraints all values in this.elements to be false.
	 * The effect of this method is the same as calling this.factory.not(this.orFold()).
	 * @return { f: BooleanValue | f <=> !(this.elements[0] || ... || !this.elements[this.dimensions.capacity-1]) }
	 */
	public final BooleanValue none() {
		return some().negation();
	}
	
	/**
     * Sets the value at the specified index to the given value;
     * returns the value previously at the specified position.  
     * 
     * @return this.elements[index]
     * @effects this.elements'[index] = formula
     * @throws NullPointerException - formula = null
     * @throws IllegalArgumentException - formula !in this.factory.components
     * @throws IndexOutOfBoundsException - index < 0 || index >= this.dimensions.capacity
     */
	public final BooleanValue set(final int index, final BooleanValue formula) {
		if (!dims.validate(index)) throw new IndexOutOfBoundsException("index < 0 || index >= this.dimensions.capacity");
		if (formula==null) throw new NullPointerException("formula=null");
		if (formula==FALSE) return maskNull(cells.remove(index));
		else return maskNull(cells.put(index,formula));
	}
	
	/**
	 * Returns a copy of this boolean matrix.
	 * @return {m: BooleanMatrix - this | m.dimensions = this.dimensions &&
	 *                                    m.elements = copy of this.elements } 
	 */
	public BooleanMatrix clone()  {
		try {
			return new BooleanMatrix(dims, factory, cells.clone());
		} catch (CloneNotSupportedException e) {
			throw new InternalError(); // unreachable code.
		}
	}
	
	public String toString() {
		final StringBuffer buff = new StringBuffer("dimensions: ");
		buff.append(dims);
		buff.append(", elements: ");
		buff.append(cells);
		return buff.toString();
	}	
}
