package pl.shockah;

import java.util.*;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class WeightedRandom<T> {
	protected LinkedList<ImmutablePair<T,Double>> list = new LinkedList<ImmutablePair<T,Double>>();
	protected final Random rnd;
	
	public WeightedRandom() {
		rnd = new Random();
	}
	public WeightedRandom(long seed) {
		this();
		rnd.setSeed(seed);
	}
	
	public void add(T element, double weight) {
		list.add(new ImmutablePair<T,Double>(element,weight));
	}
	public void remove(T element) {
		Iterator<ImmutablePair<T,Double>> iterator = list.iterator();
		while (iterator.hasNext()) if (iterator.next().getLeft().equals(element)) iterator.remove();
	}
	
	public T get() {
		double r = rnd.nextDouble(), t = 0d;
		for (ImmutablePair<T,Double> pair : list) t += pair.getRight();
		r *= t;
		
		for (ImmutablePair<T,Double> pair : list) {
			if (r > pair.getRight()) r -= pair.getRight();
			else return pair.getLeft();
		}
		
		return null;
	}
}