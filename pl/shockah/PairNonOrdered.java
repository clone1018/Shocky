package pl.shockah;

public class PairNonOrdered<A> extends Pair<A,A> {
	public PairNonOrdered(A first, A second) {
		super(first,second);
	}
	
	public boolean equals(Object instance) {
		if (instance instanceof PairNonOrdered<?>) {
			PairNonOrdered<?> other = (PairNonOrdered<?>)instance;
			return super.equals(other) || super.equals(other.reverse());
		}
		return false;
	}
	
	public PairNonOrdered<A> reverse() {
		return new PairNonOrdered<A>(get2(),get1());
	}
}