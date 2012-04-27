package pl.shockah;

public class Pair<A,B> {
	private A first;
	private B second;

	public Pair(A first, B second) {
		super();
		this.first = first;
		this.second = second;
	}

	public boolean equals(Object instance) {
		if (instance instanceof Pair<?,?>) {
			Pair<?,?> other = (Pair<?,?>)instance;
			boolean check1 = false, check2 = false;
			
			if (first != null) check1 = first.equals(other.first);
			if (!check1 && other.first != null) check1 = other.first.equals(first);
			if (second != null) check2 = second.equals(other.second);
			if (!check2 && other.second != null) check2 = other.second.equals(second);
			
			return check1 && check2;
		}
		return false;
	}
	public int hashCode() {
		return first.hashCode()*second.hashCode();
	}
	public String toString() { return "("+first+"|"+second+")";}

	public A get1() {return first;}
	public void set1(A first) {this.first = first;}

	public B get2() {return second;}
	public void set2(B second) {this.second = second;}
}