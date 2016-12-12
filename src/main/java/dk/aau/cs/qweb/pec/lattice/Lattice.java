package dk.aau.cs.qweb.pec.lattice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
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
	 * Map from data fragments to closest metadata fragments
	 */
	private MultiValuedMap<Fragment, Fragment> metadataMap;
	
	/**
	 * Map from signature hash codes to fragments.
	 */
	private Map<Signature, Fragment> partitionsFullSignatureMap;
	
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
		
	
	/*** Construction methods ***/
	
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
	
	/**
	 * Uses the fragments' signatures and the schema to associate data fragments to metadata 
	 * fragments.
	 */
	void linkData2MetadataFragments() {
		for (Fragment metafragment : getLeaves()) {	
			// Do this only for data fragments.
			if (metafragment.containsMetadata()) {
				// Get all the fragments joining on the object
				Collection<Signature> signatures = metafragment.getSignatures();
				for (Signature signature : signatures) {
					String range = signature.getThird();
					// We will not take care of fragments of the form [null, null, null, provId]
					if (range == null) continue;
					
					// Check if there are object co-located fragments
					Set<Fragment> objectColocatedCandidates = (Set<Fragment>) partitionsDomainOfSignatureMap.get(range);					
					Set<Fragment> objectColocatedCandidatesFinal = null;
					if (objectColocatedCandidates != null) {
						objectColocatedCandidatesFinal = new LinkedHashSet<>(objectColocatedCandidates);
					}
					removeMetadataFragments(objectColocatedCandidatesFinal);

					if (!objectColocatedCandidates.isEmpty()) {
						link2Metadata(metafragment, objectColocatedCandidatesFinal);
					} else {
						// If not, then try to subject co-locate this fragment
						String domain = signature.getRange();
						if (domain == null) continue;
						Set<Fragment> subjectColocatedCandidates = 
								(Set<Fragment>) partitionsDomainOfSignatureMap.get(domain);
						if (subjectColocatedCandidates != null) {
							Set<Fragment> subjectColocatedCandidatesFinal = new 
									LinkedHashSet<>(subjectColocatedCandidates);
							removeMetadataFragments(subjectColocatedCandidatesFinal);
							link2Metadata(metafragment, subjectColocatedCandidatesFinal);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Removes metadata fragments from the provided set of fragments.
	 * @param objectColocatedCandidates
	 */
	private void removeMetadataFragments(Set<Fragment> objectColocatedCandidates) {
		List<Fragment> toRemove = new ArrayList<>();
		for (Fragment f : objectColocatedCandidates) {
			if (f.containsMetadata())
				toRemove.add(f);
		}
		objectColocatedCandidates.removeAll(toRemove);	
		
	}

	/**
	 * Links a metadata fragment to all its information triple fragments.
	 * @param metaFragment
	 * @param joiningFragments
	 */
	private void link2Metadata(Fragment metaFragment, Set<Fragment> joiningFragments) {
		for (Fragment informationFragment : joiningFragments) {
			metadataMap.put(informationFragment, metaFragment);
			Set<Fragment> infoFragmentAncestors = getAncestors(informationFragment);
			for (Fragment ancestor : infoFragmentAncestors) {
				metadataMap.put(ancestor, metaFragment);
			}
		}
	}


	/**
	 * Returns an RDF Data fragment which can be used as the root for a fragment lattice.
	 * @return
	 */
	static Fragment createFragment() {
		return new Fragment(++fragmentId);
	}
	
	
	/**
	 * It 
	 * @param provenanceId
	 * @param containsMetadata
	 * @return
	 */
	private static Fragment createFragment(String provenanceId, boolean containsMetadata) {
		Fragment fragment = new Fragment(new Signature(null, null, null, provenanceId), ++fragmentId);
		fragment.setContainsMetadata(containsMetadata);
		fragment.setContainsInfoTriples(!containsMetadata);
		return fragment;
	}
	
	/**
	 * @param relationSignature
	 * @return
	 */
	private Fragment createFragment(Signature relationSignature) {
		String relation = relationSignature.getPredicate();
		Fragment fragment = new Fragment(relationSignature, ++fragmentId);
		fragment.setContainsMetadata(structure.isMetadataRelation(relation));
		fragment.setContainsInfoTriples(structure.isFactualRelation(relation));
		return fragment;
	}
	
	
	// TODO: Keep separate maps for factual and metadata relations
	void registerTuple(Quadruple quad) {
		root.increaseSize();
		String provenanceIdentifier = quad.getGraphLabel();
		String relation = quad.getPredicate();
		
		// Register the triple in the fragment corresponding to the provenance identifier, i.e., [null, null, null, provId]
		Signature provSignature = new Signature(null, null, null, provenanceIdentifier);
		Fragment provFragment = partitionsFullSignatureMap.get(provSignature);
		if (provFragment == null) {
			provFragment = createFragment(provenanceIdentifier, structure.isMetadataRelation(relation));
			partitionsFullSignatureMap.put(provSignature, provFragment);
			addEdge(provFragment);
			indexSignature(provSignature, provFragment);
		}
		provFragment.increaseSize();
		
		// Register the triple in the fragment corresponding to the provenance identifier plus the relation [domain, relationName, range, provId]
		Pair<String, String> relationDomainAndRange = structure.getDomainAndRange(relation);		
		Signature relationSignature = new Signature(relationDomainAndRange.getLeft(), 
				relation, relationDomainAndRange.getRight(), provenanceIdentifier); 
		Fragment relationPlusProvFragment = partitionsFullSignatureMap.get(relationSignature);
		if (relationPlusProvFragment == null) {
			relationPlusProvFragment = createFragment(relationSignature);			
			partitionsFullSignatureMap.put(relationSignature, relationPlusProvFragment);
			relations2FragmentsMap.put(relation, relationPlusProvFragment);
			addEdge(relationPlusProvFragment, provFragment);
			indexSignature(relationSignature, relationPlusProvFragment);			
		}
		relationPlusProvFragment.increaseSize();
		
		// Handle rdf:type triples
		if (relation.equals(RDFCubeStructure.typeRelation)) {
			String object = quad.getObject();
			Signature subrelationSignature = new Signature(object, relation, 
					relationDomainAndRange.getRight(), provenanceIdentifier); 
			Fragment subrelationPlusProvFragment = partitionsFullSignatureMap.get(subrelationSignature);
			if (subrelationPlusProvFragment == null) {
				subrelationPlusProvFragment = createFragment(subrelationSignature);
				partitionsFullSignatureMap.put(subrelationSignature, subrelationPlusProvFragment);
				relations2FragmentsMap.put(relation, subrelationPlusProvFragment);
				addEdge(subrelationPlusProvFragment, relationPlusProvFragment);
				indexSignature(subrelationSignature, subrelationPlusProvFragment);
			}
			subrelationPlusProvFragment.increaseSize();
		}
	}

	private void indexSignature(Signature relationSignature, Fragment fragment) {
		if (relationSignature.getRange() != null) {
			partitionsDomainOfSignatureMap.put(relationSignature.getRange(), fragment);
		} else {
			partitionsDomainOfSignatureMap.put(nullString, fragment);
		}
		
		if (relationSignature.getThird() != null) {
			partitionsRangeOfSignatureMap.put(relationSignature.getThird(), fragment);
		} else {
			partitionsRangeOfSignatureMap.put(nullString, fragment);
		}
	}
	
	private boolean addEdge(Fragment child, Fragment parent) {			
		boolean result = parentsGraph.put(child, parent);
		childrenGraph.put(parent, child);
		return result;
	}
	
	private boolean addEdge(Fragment child) {
		return addEdge(child, root);
	}
	
	/**
	 * It creates and adds to the lattice a parent fragment whose signature
	 * subsumes the signatures of the fragments provided as arguments. The fragments must share a parent.
	 * The operation will create new parent which will become a child of the older parent.
	 * @param signature1
	 * @param signature
	 * @return boolean True if the operation succeeded, false otherwise. The operation will fail if one of
	 * the fragments does not belong to the lattice or they are not siblings, i.e., they do not share 
	 * any parent.
	 */
	public boolean createNewParent(Fragment f1, Fragment f2) {
		Fragment merged = f1.merge(f2, ++fragmentId);
		if (!parentsGraph.containsKey(f1) || 
				!parentsGraph.containsKey(f2))
			return false;
		
		Set<Fragment> parentsF1 = new LinkedHashSet<>(parentsGraph.get(f1));
		parentsF1.addAll(parentsGraph.get(f2));
		
		for (Fragment commonParent : parentsF1) {
			parentsGraph.get(f1).remove(commonParent);
			parentsGraph.get(f2).remove(commonParent);
			childrenGraph.get(commonParent).remove(f1);
			childrenGraph.get(commonParent).remove(f2);
			parentsGraph.put(merged, commonParent);
			parentsGraph.put(f1, merged);			
			parentsGraph.put(f2, merged);
			childrenGraph.put(merged, f1);
			childrenGraph.put(merged, f2);
			childrenGraph.put(commonParent, merged);
			if (f1.containsInfoTriples() && f2.containsInfoTriples()) {
				for (Fragment f : getMetadataFragments(f1))
					metadataMap.put(commonParent, f);	
				
				for (Fragment f : getMetadataFragments(f2))
					metadataMap.put(commonParent, f);	
			}
		}
		
		return true;
	}

	/*** Access methods ***/
	
	/**
	 * Get a fragment by signature.
	 * @param signature
	 * @return
	 */
	public Fragment getFragmentBySignature(Signature signature) {
		return partitionsFullSignatureMap.get(signature);
		
	}

	
	/**
	 * It returns all possible ancestors paths for the given fragment.
	 * @param fragment
	 * @return
	 */
	public List<List<Fragment>> getAncestorPaths(Fragment fragment) {
		List<List<Fragment>> result = new ArrayList<>();
		if (fragment.equals(root)) {
			List<Fragment> singleton = new ArrayList<>();
			singleton.add(fragment);
			result.add(singleton);
		} else {
			// I have parents
			for (Fragment parent : parentsGraph.get(fragment)) {
				List<List<Fragment>> parentPaths = getAncestorPaths(parent);
				for (List<Fragment> parentPath : parentPaths) {
					// I add myself to the paths provided by my parents
					parentPath.add(fragment);
				}
				Collections.reverse(parentPaths);
				result.addAll(parentPaths);
			}
		}
		
		return result;
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
		
		LinkedHashSet<Fragment> queue;
		Iterator<Fragment> backupIterator;
		
		LatticeIterator() {
			queue = new LinkedHashSet<>();
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
		return parentsGraph.keySet().size() + 1;
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
		relation = relation.replaceAll("<", "");
		relation = relation.replaceAll(">", "");
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
	

	/**
	 * It returns true if the given fragment is the exact same object 
	 * registered as root in the lattice (object identity is applied)
	 * @param fragment
	 * @return
	 */
	public boolean isRoot(Fragment fragment) {
		return root == fragment;
	}
	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		for (Fragment partition : this) {
			strBuilder.append(partition + "----> " + (parentsGraph.containsKey(partition) ? parentsGraph.get(partition) : "null") + "\n");	
		}
		strBuilder.append("Metadata\n");
		for (Fragment fragment : metadataMap.keySet())
			strBuilder.append(fragment + ": " + metadataMap.get(fragment) + "\n");
		return strBuilder.toString();
		
	}


	public Set<Signature> getSignaturesByPredicate(String predicate) {
		Set<Signature> signatures = new HashSet<Signature>();
		if (relations2FragmentsMap.containsKey(predicate)) {
			Collection<Fragment> fragmentSet = relations2FragmentsMap.get(predicate);
			for (Fragment fragment : fragmentSet) {
				for (Signature signature : fragment.getSignatures()) {
					signatures.add(signature);
				}
			}
		}
		return signatures;
	}

}
