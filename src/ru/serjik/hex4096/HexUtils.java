package ru.serjik.hex4096;

public class HexUtils
{
	public static final int distance(int q1, int r1, int q2, int r2)
	{
		return (Math.abs(q1 - q2) + Math.abs(r1 - r2) + Math.abs(q1 + r1 - q2 - r2)) / 2;
	}
	
	

}
