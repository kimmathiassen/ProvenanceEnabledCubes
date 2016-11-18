package dk.aau.cs.qweb.pec.lattice;

import java.util.Collection;
import java.util.Collections;
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
	
	private static int fragmentId = 0;
	
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
	 * Map from signature hash codes to fragments.
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
	
	/**
	 * Map where the keys are relation names and the values are all the fragments
	 * whose signature has this relation name.
	 */
	private MultiValuedMap<String, Fragment> relations2FragmentsMap;
	
	
	
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
		relations2FragmentsMap = new HashSetValuedHashMap<>();
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

	/**
	 * Returns all the ancestors of a fragment in the lattice
	 * @param fragment
	 * @return
	 */
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
	 * It returns all the parents of a fragment in the lattice.
	 * @param fragment
	 */
	public Set<Fragment> getChildren(Fragment fragment) {
		Collection<Fragment> parents = childrenGraph.get(fragment);		
		Set<Fragment> result = null;
		if (parents != null) {		
			result = new LinkedHashSet<>(parents);
		} else {
			result = Collections.emptySet();
		}
		
		return result;
	}
	
	public Set<Fragment> getMeasureFragments() {
		Set<Fragment> result = new LinkedHashSet<>();
		for (Fragment fragment : this) {
			if (structure.containsMeasureTriples(fragment.getSignatures())) {
				result.add(fragment);
			}
		}
		
		return result;
	}
	
	public Set<Fragment> ssjoinCandidates(Fragment fragment) {
		Set<Fragment> candidates = new LinkedHashSet<>();
		for (Fragment candidate : this) {
			if (fragment == candidate)
				continue;
			
			if (fragment.canSignatureJoinSubject2Subject(candidate)) {
				candidates.add(fragment);
			}
		}
		
		return candidates;
	}


	/**
	 * Returns an RDF Data fragment which can be used as the root for a fragment lattice.
	 * @return
	 */
	static Fragment createFragment() {
		return new DataFragment(++fragmentId);
	}
	
	private static Fragment createFragment(Signature<String, String, String, String> signature, boolean isMetadata) {
		if (isMetadata)
			return new MetadataFragment(signature, ++fragmentId);
		else			
			return new DataFragment(signature, ++fragmentId);
	}
	

	private Fragment createFragment(Signature<String, String, String, String> relationSignature) {
		String relation = relationSignature.getSecond();
		if (structure.isMetadataRelation(relation)) {
			return new MetadataFragment(relationSignature, ++fragmentId);
		} else {
			return new DataFragment(relationSignature, ++fragmentId);
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
		Fragment provFragment = partitionsFullSignatureMap.get(provSignature);
		if (provFragment == null) {
			provFragment = createFragment(provSignature, structure.isMetadataRelation(relation));
			partitionsFullSignatureMap.put(provSignature, provFragment);
			addEdge(provFragment);
		}
		provFragment.increaseSize();
		
		// Register the triple in the fragment corresponding to the provenance identifier
		Pair<String, String> relationDomainAndRange = structure.getDomainAndRange(relation);		
		Signature<String, String, String, String> relationSignature = new Signature<>(relationDomainAndRange.getLeft(), 
				relation, relationDomainAndRange.getRight(), provenanceIdentifier); 
		Fragment relationPlusProvFragment = partitionsFullSignatureMap.get(relationSignature);
		if (relationPlusProvFragment == null) {
			relationPlusProvFragment = createFragment(relationSignature);			
			partitionsFullSignatureMap.put(relationSignature, provFragment);
			relations2FragmentsMap.put(relation, relationPlusProvFragment);
			addEdge(relationPlusProvFragment, provFragment);
			if (relationSignature.getFirst() != null) {
				partitionsDomainOfSignatureMap.put(relationSignature.getFirst(), relationPlusProvFragment);
			} else {
				partitionsDomainOfSignatureMap.put(nullString, relationPlusProvFragment);
			}
			
			if (relationSignature.getThird() != null) {
				partitionsRangeOfSignatureMap.put(relationSignature.getThird(), relationPlusProvFragment);
			} else {
				partitionsRangeOfSignatureMap.put(nullString, relationPlusProvFragment);
			}
		}
		relationPlusProvFragment.increaseSize();
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
	
	/**
	 * It returns all the fragments whose relation name is equal to the relation sent as argument.
	 * @param relation
	 * @return
	 */
	public Set<Fragment> getFragmentsForRelation(String relation) {
		if (relations2FragmentsMap.containsKey(relation)) {
			return new LinkedHashSet<>(relations2FragmentsMap.get(relation));
		} else {
			return Collections.emptySet();
		}
	}

	
	public RDFCubeDataSource getData() {
		return data;
	}


	public RDFCubeStructure getStructure() {
		return structure;
	}


	public Fragment getRoot() {
		return root;
	}
	

}
