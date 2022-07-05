package graphStructures;

public record neighborRecord(String source, String target, int rank) {

	@Override
	public boolean equals(Object obj) {		
		return source.equals(target);
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
