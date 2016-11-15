package dk.aau.cs.qweb.pec.lattice;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.DataFragment;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.fragment.MetadataFragment;
import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;


public class Lattice implements Iterable<Fragment>{
	
	/**
	 * Root fragment, i.e., ancestor of all fragments in the lattice.
	 * It represents the entire cube
	 */
	private Fragment root;
	
	/**
	 * Object containing the cube structure definition
	 */
	private RDFCubeStructure structure;
	
	/**
	 * Proxy to the actual cube data
	 */
	private RDFCubeDataSource data;
	
	/**
	 * Map from children to parents
	 */
	private MultiValuedMap<Fragment, Fragment> parentsGraph; 
	
	/**
	 * Map from parent fragments to children
	 */
	private MultiValuedMap<Fragment, Fragment> childrenGraph;
	
	/**
	 * Map from data fragments to metadata fragments
	 */
	private MultiValuedMap<Fragment, Fragment> metadataMap;
	
	/**
	 * Map from signature hash codes to partitions.
	 */
	private Map<Signature<String, String, String, String>, Fragment> partitionsFullSignatureMap;
	
	/**
	 * Map where the keys are data types and the values are all the fragments
	 * whose signature relation has that type as domain.
	 */
	private MultiValuedMap<String, Fragment> partitionsDomainOfSignatureMap;

	/**
	 * Map where the keys are data types and the values are all the fragments
	 * whose signature relation has that type as range.
	 */	
	private MultiValuedMap<String, Fragment> partitionsRangeOfSignatureMap;
	
	private static String nullString = "null";
		
	
	Lattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		this.root = root;
		this.structure = schema;
		this.data = data;
		parentsGraph = new HashSetValuedHashMap<>();
		childrenGraph = new HashSetValuedHashMap<>();
		metadataMap = new HashSetValuedHashMap<>();
		partitionsFullSignatureMap = new LinkedHashMap<>();
		partitionsDomainOfSignatureMap = new HashSetValuedHashMap<>();
		partitionsRangeOfSignatureMap = new HashSetValuedHashMap<>();
	}
	
	
	void linkData2MetadataFragments() {
		for (Fragment fragment : parentsGraph.keySet()) {
			Set<Fragment> ancestors = getAncestors(fragment);
			if (!fragment.isMetadata()) {
				// Get all the fragments joining on the object
				Collection<Signature<String, String, String, String>> signatures = fragment.getSignatures();
				for (Signature<String, String, String, String> signature : signatures) {
					String domain = signature.getFirst();
					if (domain == null) {
						domain = nullString;
					}
					// This fragment can contain anything on the subject (most likely an rdf:type fragment)
					Set<Fragment> candidateMetadataFragments = (Set<Fragment>) partitionsRangeOfSignatureMap.get(domain);
					if (candidateMetadataFragments != null) {
						for (Fragment candidateFragment : candidateMetadataFragments) {
							if (candidateFragment.isMetadata()) {
								metadataMap.put(fragment, candidateFragment);
								for (Fragment ancestor : ancestors) {
									metadataMap.put(ancestor, candidateFragment);
								}
							}
						}
					}
				}
			}
		}
		
	}

	public Set<Fragment> getAncestors(Fragment fragment) {
		Set<Fragment> parents = (Set<Fragment>) parentsGraph.get(fragment);
		Set<Fragment> result = new LinkedHashSet<>();
		if (parents != null) {
			for (Fragment parent : parents) {
				result.add(parent);
				result.addAll(getAncestors(parent));
			}
		}
		
		return result;
	}

	/**
	 * Returns an RDF Data fragment which can be used as the root for a fragment lattice.
	 * @return
	 */
	static Fragment createFragment() {
		return new DataFragment();
	}
	
	private static Fragment createFragment(Signature<String, String, String, String> signature, boolean isMetadata) {
		if (isMetadata)
			return new MetadataFragment(signature);
		else			
			return new DataFragment(signature);
	}
	

	private Fragment createFragment(Signature<String, String, String, String> relationSignature) {
		String relation = relationSignature.getSecond();
		if (structure.isMetadataRelation(relation)) {
			return new MetadataFragment(relationSignature);
		} else {
			return new DataFragment(relationSignature);
		}
	}
	

	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		for (Fragment partition : this) {
			strBuilder.append(partition + "----> " + (parentsGraph.containsKey(partition) ? parentsGraph.get(partition) : "null") + "\n");	
		}
		strBuilder.append(metadataMap + "\n");
		return strBuilder.toString();
		
	}
	
	// TODO: Keep separate maps for factual and metadata relations
	void registerTuple(Quadruple<String, String, String, String> quad) {
		root.increaseSize();
		String provenanceIdentifier = quad.getFourth();
		String relation = quad.getSecond();
		
		// Register the triple in the fragment corresponding to the provenance identifier
		Signature<String, String, String, String> provSignature = new Signature<>(null, null, null, provenanceIdentifier);
		Fragment provPartition = partitionsFullSignatureMap.get(provSignature);
		if (provPartition == null) {
			provPartition = createFragment(provSignature, structure.isMetadataRelation(relation));
			partitionsFullSignatureMap.put(provSignature, provPartition);
			addEdge(provPartition);
		}
		provPartition.increaseSize();
		
		// Register the triple in the fragment corresponding to the provenance identifier
		Pair<String, String> relationDomainAndRange = structure.getDomainAndRange(relation);		
		Signature<String, String, String, String> relationSignature = new Signature<>(relationDomainAndRange.getLeft(), 
				relation, relationDomainAndRange.getRight(), provenanceIdentifier); 
		Fragment relationPlusProvPartition = partitionsFullSignatureMap.get(relationSignature);
		if (relationPlusProvPartition == null) {
			relationPlusProvPartition = createFragment(relationSignature);
			partitionsFullSignatureMap.put(relationSignature, provPartition);
			addEdge(relationPlusProvPartition, provPartition);
			if (relationSignature.getFirst() != null) {
				partitionsDomainOfSignatureMap.put(relationSignature.getFirst(), relationPlusProvPartition);
			} else {
				partitionsDomainOfSignatureMap.put(nullString, relationPlusProvPartition);
			}
			
			if (relationSignature.getThird() != null) {
				partitionsRangeOfSignatureMap.put(relationSignature.getThird(), relationPlusProvPartition);
			} else {
				partitionsRangeOfSignatureMap.put(nullString, relationPlusProvPartition);
			}
		}
		relationPlusProvPartition.increaseSize();
	}

	private boolean addEdge(Fragment child, Fragment parent) {			
		boolean result = parentsGraph.put(child, parent);
		childrenGraph.put(parent, child);
		return result;
	}
	
	private boolean addEdge(Fragment child) {
		return addEdge(child, root);
	}
	
	private Set<Fragment> getLeaves() {
		Set<Fragment> result = new LinkedHashSet<>(parentsGraph.keySet());
		result.removeAll(childrenGraph.keySet());
		return result;
	}

	@Override
	public Iterator<Fragment> iterator() {
		return new LatticeIterator();
	}

	
	class LatticeIterator implements Iterator<Fragment> {
		
		Queue<Fragment> queue;
		Iterator<Fragment> backupIterator;
		
		LatticeIterator() {
			queue = new LinkedList<>();
			traverse(getLeaves());
			backupIterator = queue.iterator();
		}

		private void traverse(Set<Fragment> currentLevel) {
			if (currentLevel.isEmpty()) return;
			
			Set<Fragment> nextLevel = new LinkedHashSet<>();
			queue.addAll(currentLevel);
			
			for (Fragment f : currentLevel){
				nextLevel.addAll(parentsGraph.get(f));
			}
			
			traverse(nextLevel);
		}

		@Override
		public boolean hasNext() {
			return backupIterator.hasNext();
		}

		@Override
		public Fragment next() {
			return backupIterator.next();
		}
		
	}
	

	public int size() {
		return parentsGraph.size() + 1;
	}


	/**
	 * It returns a set with all the metadata fragments that should be materialized with the
	 * given data fragment in order to guarantee a cube in the materialization.
	 * @param bestFragment
	 * @return
	 */
	public Set<Fragment> getMetadataFragments(Fragment dataFragment) {
		// First verify if it has metadata directly attached
		Set<Fragment> result = new LinkedHashSet<>();
		Set<Fragment> metaFragments = (Set<Fragment>) metadataMap.get(dataFragment);
		if (metaFragments != null) {
			result.addAll(metaFragments);
		} else {
			// Look at the children
			Set<Fragment> children = (Set<Fragment>) childrenGraph.get(dataFragment);
			if (children != null) {
				for (Fragment child : children) {
					result.addAll(getMetadataFragments(child));
				}
			}
		}
		
		return result;
	}
	
	public RDFCubeDataSource getData() {
		return data;
	}


	public RDFCubeStructure getStructure() {
		return structure;
}
}
