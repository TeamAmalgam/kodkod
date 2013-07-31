package kodkod.intelision;

public class TemporaryTuple {
	Object left;
	int right;
	
	public TemporaryTuple(Object left, int right)
	{
		this.left = left;
		this.right = right;
	}
	
	public Object left()
	{
		return left;
	}
	
	public int right()
	{
		return right;
	}

	public String toString()
	{
		return "[" + left + "," + right + "]";
	}
	
	
	public void setRight(int i)
	{
		right = i;
	}
	//Since the arithmetic is using 32 java ints, we need to put the solution
	//back into the correct range in case of overflow (spurious counterexamples...)
	//Example: bitwidth = 4, weight = x+y+z, x=y=z=6 => weight = 18 => weight = 18%16 = 2
	public void rebound(int bitwidth)
	{
		int nums = (int)Math.pow(2, bitwidth);
		int halfNums = (int)Math.pow(2, bitwidth-1);
		int mod = right % nums;
		if(mod >= 0)
		{
			if(mod >= halfNums)
				right = mod - nums;
			else
				right = mod;
		}
		else 
		{
			if(mod <= -halfNums)
				right = mod + nums;
			else
				right = mod;
		}
	}
}
