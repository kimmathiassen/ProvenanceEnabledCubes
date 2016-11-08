package dk.aau.cs.qweb.pec.types;

/**
 * Template class for tuples of 4 elements.
 * @author galarraga
 *
 * @param <S>
 * @param <T>
 * @param <U>
 * @param <V>
 */
public class Quadruple<S, T, U, V> {
	
	private S first;
	
	private T second;
	
	private U third;
	
	private V fourth;

	public Quadruple(S first, T second, U third, V fourth) {
		this.setFirst(first);
		this.setSecond(second);
		this.setThird(third);
		this.setFourth(fourth);
	}

	public S getFirst() {
		return first;
	}

	public void setFirst(S first) {
		this.first = first;
	}

	public T getSecond() {
		return second;
	}

	public void setSecond(T second) {
		this.second = second;
	}

	public U getThird() {
		return third;
	}

	public void setThird(U third) {
		this.third = third;
	}

	public V getFourth() {
		return fourth;
	}

	public void setFourth(V fourth) {
		this.fourth = fourth;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((fourth == null) ? 0 : fourth.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		result = prime * result + ((third == null) ? 0 : third.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Quadruple other = (Quadruple) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (fourth == null) {
			if (other.fourth != null)
				return false;
		} else if (!fourth.equals(other.fourth))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		if (third == null) {
			if (other.third != null)
				return false;
		} else if (!third.equals(other.third))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[" + first + ", " + second + ", " + third + ", " + fourth + "]";
	}
}
