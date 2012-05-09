package pl.shockah;

import java.util.*;

public class WeightedRandom<T> {
	protected LinkedList<Pair<T,Double>> list = new LinkedList<Pair<T,Double>>();
	protected final Random rnd;
	
	public WeightedRandom() {
		rnd = new Random();
	}
	public WeightedRandom(long seed) {
		this();
		rnd.setSeed(seed);
	}
	
	public void add(T element, double weight) {
		list.add(new Pair<T,Double>(element,weight));
	}
	public void remove(T element) {
		Iterator<Pair<T,Double>> iterator = list.iterator();
		while (iterator.hasNext()) if (iterator.next().get1().equals(element)) iterator.remove();
	}
	
	public T get() {
		double r = rnd.nextDouble(), t = 0d;
		for (Pair<T,Double> pair : list) t += pair.get2();
		r *= t;
		
		for (Pair<T,Double> pair : list) {
			if (r > pair.get2()) r -= pair.get2();
			else return pair.get1();
		}
		
		return null;
	}
}