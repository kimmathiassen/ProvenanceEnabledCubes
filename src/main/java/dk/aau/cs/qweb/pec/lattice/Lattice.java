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
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.ext.com.google.common.collect.Sets;

import dk.aau.cs.qweb.pec.data.RDFCubeDataSource;
import dk.aau.cs.qweb.pec.data.RDFCubeStructure;
import dk.aau.cs.qweb.pec.fragment.Fragment;
import dk.aau.cs.qweb.pec.types.Quadruple;
import dk.aau.cs.qweb.pec.types.Signature;


public abstract class Lattice implements Iterable<Fragment>{
	
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
	protected RDFCubeDataSource data;
		
	/**
	 * Map from children to parents
	 */
	protected MultiValuedMap<Fragment, Fragment> parentsGraph; 
	
	/**
	 * Map from parent fragments to children
	 */
	protected MultiValuedMap<Fragment, Fragment> childrenGraph;
	
	
	/**
	 * Map from signature hash codes to fragments.
	 */
	private Map<Set<Signature>, Fragment> partitionsFullSignatureMap;
	
	/**
	 * Map from single signatures to fragments.
	 */
	private Map<Signature, Fragment> partitionsSingleSignatureMap;
	
	
	/**
	 * Map where the keys are predicate names and the values are all the fragments
	 * whose signature has this relation name.
	 */
	protected MultiValuedMap<String, Fragment> predicates2FragmentsMap;	
	
	
	/**
	 * Map with the keys are provenance identifiers and the values are all the 
	 * fragments whose signature contains that provenance identifier.
	 */
	protected MultiValuedMap<String, Fragment> provenanceId2FragmentMap;
	
	/**
	 * Map where the keys are pairs relation-provenance and the values
	 * are lists of fragments.
	 */
	private MultiValuedMap<Pair<String, String>, Fragment> predicatesAndProvid2FragmentsMap;
	
	/**
	 * Map where the keys are triples relation-object-provenance.
	 */
	private MultiValuedMap<Triple<String, String, String>, Fragment> predicatesAndObjectAndProvid2FragmentsMap;
	
	
	/**
	 * List of fragments with a signatures where only the provenance identifier part is bound.
	 */
	protected List<Fragment> onlyProvenanceIdFragments;
	
	/**
	 * Number of times the merge() routine has been called.
	 */
	protected int mergingSteps; 
	
	/**
	 * Initial number of fragments after having scanned the data source.
	 */
	protected int initialSize;
		
	/*** Construction methods ***/
	
	Lattice(Fragment root, RDFCubeStructure schema, RDFCubeDataSource data) {
		this.root = root;
		this.structure = schema;
		this.data = data;
		parentsGraph = new HashSetValuedHashMap<>();
		childrenGraph = new HashSetValuedHashMap<>();
		partitionsFullSignatureMap = new LinkedHashMap<>();
		partitionsSingleSignatureMap = new LinkedHashMap<>();
		predicates2FragmentsMap = new HashSetValuedHashMap<>();
		provenanceId2FragmentMap = new HashSetValuedHashMap<>();
		predicatesAndProvid2FragmentsMap = new HashSetValuedHashMap<>();
		predicatesAndObjectAndProvid2FragmentsMap = new HashSetValuedHashMap<>();
		onlyProvenanceIdFragments = new ArrayList<>();
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
		return fragment;
	}
	
	/**
	 * @param relationSignature
	 * @return
	 */
	private Fragment createFragment(Signature relationSignature) {
		Fragment fragment = new Fragment(relationSignature, ++fragmentId);
		return fragment;
	}
	
	
	/**
	 * It reads a tuple and updates the lattice, i.e., creates new fragments if necessary or
	 * updates the size of existing fragments.
	 * @param quad
	 */
	void registerTuple(Quadruple quad) {
		root.increaseSize();
		String provenanceIdentifier = quad.getGraphLabel();
		String relation = quad.getPredicate();
		boolean isMeasureTriple = structure.isMeasureProperty(relation);
		
		// Register the triple in the fragment corresponding to the provenance identifier, i.e., [null, null, null, provId]
		Signature provSignature = new Signature(null, null, null, provenanceIdentifier);
		Fragment provFragment = partitionsFullSignatureMap.get(Sets.newHashSet(provSignature));
		if (provFragment == null) {
			provFragment = createFragment(provenanceIdentifier, structure.isMetadataProperty(relation));
			partitionsFullSignatureMap.put(Sets.newHashSet(provSignature), provFragment);
			partitionsSingleSignatureMap.put(provSignature, provFragment);
			predicates2FragmentsMap.put(relation, provFragment);
			provenanceId2FragmentMap.put(provenanceIdentifier, provFragment);
			onlyProvenanceIdFragments.add(provFragment);
			addEdge(provFragment);
		}
		provFragment.increaseSize();
		if (isMeasureTriple) {
			provFragment.increaseMeasureTriplesCount();
		}
		
		// Register the triple in the fragment corresponding to the provenance identifier plus the relation [null, relationName, null, provId]
		Signature relationSignature = new Signature(null, relation, null, provenanceIdentifier); 
		Fragment relationPlusProvFragment = partitionsFullSignatureMap.get(Sets.newHashSet(relationSignature));
		if (relationPlusProvFragment == null) {
			relationPlusProvFragment = createFragment(relationSignature);			
			partitionsFullSignatureMap.put(Sets.newHashSet(relationSignature), relationPlusProvFragment);
			partitionsSingleSignatureMap.put(relationSignature, relationPlusProvFragment);
			predicates2FragmentsMap.put(relation, relationPlusProvFragment);
			predicatesAndProvid2FragmentsMap.put(new MutablePair<>(relation, provenanceIdentifier), relationPlusProvFragment);
			provenanceId2FragmentMap.put(provenanceIdentifier, relationPlusProvFragment);
			addEdge(relationPlusProvFragment, provFragment);			
		}
		relationPlusProvFragment.increaseSize();
		if (isMeasureTriple) relationPlusProvFragment.increaseMeasureTriplesCount();
		
		// Handle rdf:type triples
		if (relation.equals(RDFCubeStructure.typeRelation)) {
			String object = quad.getObject();
			Signature subrelationSignature = new Signature(null, relation, object, provenanceIdentifier); 
			Fragment subrelationPlusProvFragment = partitionsFullSignatureMap.get(Sets.newHashSet(subrelationSignature));
			if (subrelationPlusProvFragment == null) {
				subrelationPlusProvFragment = createFragment(subrelationSignature);				
				partitionsFullSignatureMap.put(Sets.newHashSet(subrelationSignature), subrelationPlusProvFragment);
				partitionsSingleSignatureMap.put(subrelationSignature, subrelationPlusProvFragment);
				predicates2FragmentsMap.put(relation, subrelationPlusProvFragment);
				predicatesAndProvid2FragmentsMap.put(new MutablePair<>(relation, provenanceIdentifier), subrelationPlusProvFragment);
				predicatesAndObjectAndProvid2FragmentsMap.put(new MutableTriple<>(relation, provenanceIdentifier, object), subrelationPlusProvFragment);
				provenanceId2FragmentMap.put(provenanceIdentifier, subrelationPlusProvFragment);
				addEdge(subrelationPlusProvFragment, relationPlusProvFragment);
			}
			subrelationPlusProvFragment.increaseSize();
			if (isMeasureTriple) subrelationPlusProvFragment.increaseMeasureTriplesCount();
		}
		
		//Merging is started here
		if (isMergeStartConditionFulfilled()) {
			initialSize = size();
			while (!isMergeEndConditionFulfilled()) {
				if (merge()) {
					++mergingSteps;					
				} else {
					break;
				}
			}
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
	 * subsumes the signatures of the fragments provided as arguments. The fragments must share at least one parent.
	 * The operation will create new parent which will become a child of the older parent.
	 * @param signature1
	 * @param signature
	 * @return boolean the new parent if the operation does not fail, null otherwise. The operation fails if one of
	 * the fragments does not belong to the lattice or they are not siblings, i.e., they do not share 
	 * any parent.
	 */
	protected Fragment createNewParent(Fragment f1, Fragment f2) {
		if (!parentsGraph.containsKey(f1) || 
				!parentsGraph.containsKey(f2))
			return null;
		
		Fragment merged = f1.merge(f2, ++fragmentId);
		
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
		}
		
		return merged;
	}
	
	private void addMergedFragment(Fragment mergedFragment, Fragment f1, Fragment f2) {
		Set<Fragment> parents = new LinkedHashSet<>(parentsGraph.get(f1));
		parents.addAll(parentsGraph.get(f2));
		
		Set<Fragment> children = new LinkedHashSet<>(childrenGraph.get(f1));
		children.addAll(childrenGraph.get(f2));
		
		removeFragment(f1);
		removeFragment(f2);
		// Parent and children data structures
		for (Fragment pf : parents) {
			parentsGraph.put(mergedFragment, pf);
			childrenGraph.put(pf, mergedFragment);
		}
		
		for (Fragment cf : children) {
			parentsGraph.put(cf, mergedFragment);
			childrenGraph.put(mergedFragment, cf);
		}

		indexFragment(mergedFragment);	

	}
	
	/**
	 * Assuming the arguments are contained in the lattice and they both have the same
	 * provenance identifiers, this method merges the fragments by creating a
	 * new fragment with the same provenance identifiers and the union of the properties. The original fragments
	 * are removed from the lattice.
	 * 
	 * @param f1
	 * @param f2
	 * @return The newly created fragment or null if the fragments cannot be merged for any reason, e.g.,
	 * one of the them is not in the lattice, or they do not have identical provenance identifiers.
	 */
	public Fragment mergeByRelation(Fragment f1, Fragment f2) {
		if (!parentsGraph.containsKey(f1)
				|| !parentsGraph.containsKey(f2)) {
			return null;
		}
		
		Set<String> provIdentifiers1 = f1.getProvenanceIdentifiers();
		Set<String> provIdentifiers2 = f2.getProvenanceIdentifiers();
		if (!provIdentifiers1.equals(provIdentifiers2)) {
			return null;
		}
				
		Fragment mergedFragment = f1.merge(f2, ++fragmentId);
		addMergedFragment(mergedFragment, f1, f2);
		
		return mergedFragment;
	}
	
	/**
	 * 
	 * @param f1
	 * @param f2
	 * @return
	 */
	public Fragment mergeByProvenanceId(Fragment f1, Fragment f2) {
		if (!parentsGraph.containsKey(f1)
				|| !parentsGraph.containsKey(f2)) {
			return null;
		}
		
		if (!f1.getPredicates().isEmpty() 
				|| !f2.getPredicates().isEmpty()) {
			return null; 
		}

		Fragment mergedFragment = f1.merge(f2, ++fragmentId);		
		addMergedFragment(mergedFragment, f1, f2);
		
		return mergedFragment;
	}

	/**
	 * It adds the fragment to all the lattice's indexes (except the
	 * parent and child indexes)
	 * @param fragment
	 */
	protected void indexFragment(Fragment fragment) {	
		Set<String> predicates = fragment.getPredicates();
		for (String property : predicates) {
			predicates2FragmentsMap.put(property, fragment);			
		}
		
		if (predicates.size() > 1) {
			predicates2FragmentsMap.put(fragment.getPredicatesConcat(), fragment);
		}
		
		for (String pid : fragment.getProvenanceIdentifiers()) {
			provenanceId2FragmentMap.put(pid, fragment);
		}
		
		for (Pair<String, String> pair : fragment.getPairsPredicateProvenanceId()) {
			predicatesAndProvid2FragmentsMap.put(pair, fragment);
		}
		
		for (Triple<String, String, String> triple : fragment.getTriplesPredicateObjectAndProvenanceId()) {
			predicatesAndObjectAndProvid2FragmentsMap.put(triple, fragment);
		}
		
		partitionsFullSignatureMap.put(fragment.getSignatures(), fragment);		
		for (Signature s : fragment.getSignatures()) {
			partitionsSingleSignatureMap.put(s, fragment);
		}
	}


	/**
	 * It removes a fragment from the lattice
	 * @param f1
	 */
	private void removeFragment(Fragment f) {
		Collection<Fragment> fragmentsChildren = childrenGraph.remove(f);
		Collection<Fragment> fragmentsParent = parentsGraph.remove(f);
		Collection<String> provIds = f.getProvenanceIdentifiers();
		Collection<String> predicates = f.getPredicates();
		Collection<Pair<String, String>> pairs = f.getPairsPredicateProvenanceId();
		Collection<Triple<String, String, String>> triples = f.getTriplesPredicateObjectAndProvenanceId();
		
		for (Fragment fragment : fragmentsChildren) {
			parentsGraph.get(fragment).remove(f);
		}
		
		for (Fragment fragment : fragmentsParent) {
			childrenGraph.get(fragment).remove(f);
		}
				
		for (String property : predicates) {
			predicates2FragmentsMap.get(property).remove(f);
		}
		
		for (String pid : provIds) {
			provenanceId2FragmentMap.get(pid).remove(f);
		}
		
		for (Pair<String, String> pair : pairs) {
			predicatesAndProvid2FragmentsMap.get(pair).remove(f);
		}
		
		for (Triple<String, String, String> triple : triples) {
			predicatesAndObjectAndProvid2FragmentsMap.get(triple).remove(f);
		}
		
		partitionsFullSignatureMap.remove(f.getSignatures());
		for (Signature s : f.getSignatures()) {
			partitionsSingleSignatureMap.remove(s);
		}
	}


	/*** Access methods ***/
	
	/**
	 * Get a fragment by signature.
	 * @param signature
	 * @return
	 */
	public Fragment getFragmentByFullSignature(Set<Signature> signature) {
		return partitionsFullSignatureMap.get(signature);
		
	}
	
	/**
	 * Get a fragment by a single signature element (a quadruple)
	 * @param signatureElement
	 * @return
	 */
	public Fragment getFragmentBySingleSignature(Signature signatureElement) {
		return partitionsSingleSignatureMap.get(signatureElement);
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
	
	public Set<Fragment> getMeasureFragments(boolean hasRelationInSignature) {
		Set<Fragment> result = new LinkedHashSet<>();
		for (Fragment fragment : this) {
			if (fragment.containsMeasureTriples()) {
				if (hasRelationInSignature) {
					if (!fragment.getPredicates().isEmpty()) {
						result.add(fragment);
					}
				} else {
					result.add(fragment);
				}
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
	 * It returns all the fragments whose relation name is equal to the relation sent as argument.
	 * @param relation
	 * @param b 
	 * @return
	 */
	@Deprecated
	public Set<Fragment> getFragmentsForRelation(String relation, boolean hasRelationInSignature) {
		Set<Fragment> result = new LinkedHashSet<>();
		if (predicates2FragmentsMap.containsKey(relation)) {
			for (Fragment f : predicates2FragmentsMap.get(relation)) {
				if (hasRelationInSignature) {
					if (f.containsSignatureWithRelation(relation)) {
						result.add(f);
					}
				} else {
					result.add(f);
				}
			}
			return result;
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
	

	/**
	 * Checks whether this fragment is in the lattice
	 * @param left
	 * @return
	 */
	public boolean contains(Fragment fragment) {
		return parentsGraph.containsKey(fragment) 
				|| childrenGraph.containsKey(fragment);
	}

	
	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		for (Fragment partition : this) {
			strBuilder.append(partition + "----> " + (parentsGraph.containsKey(partition) ? parentsGraph.get(partition) : "null") + "\n");	
		}
		return strBuilder.toString();
		
	}


	public Set<Signature> getSignaturesByPredicate(String predicate) {
		Set<Signature> signatures = new HashSet<Signature>();
		if (predicates2FragmentsMap.containsKey(predicate)) {
			Collection<Fragment> fragmentSet = predicates2FragmentsMap.get(predicate);
			for (Fragment fragment : fragmentSet) {
				for (Signature signature : fragment.getSignatures()) {
					signatures.add(signature);
				}
			}
		}
		return signatures;
	}

	/**
	 * If a fragment has only one child, the fragment is marked as redundant (we favor
	 * fragments with more specific signatures).
	 */
	public void markRedundantFragments() {
		for (Fragment f : childrenGraph.keySet()) {
			if (isRoot(f)) continue;
				
			Set<Fragment> children = (Set<Fragment>) childrenGraph.get(f);
			
			if (children.size() == 1) {
				children.iterator().next().markAsRedundant(true);				
			}
			
		}
		
	}


	public Set<Fragment> getFragmentsForPartialSignatureWithProvenanceIdentifiers(Signature partialTriplePatternSignature, Set<String> provenanceIdentifiers) {
		Set<Fragment> result = new LinkedHashSet<>();
		String relation = partialTriplePatternSignature.getPredicate();
		MutablePair<String, String> pair = new MutablePair<>();
		pair.setLeft(relation);
		for (String provId : provenanceIdentifiers) {
			pair.setRight(provId);
			Collection<Fragment> frags = predicatesAndProvid2FragmentsMap.get(pair);
			if (frags != null) {
				result.addAll(frags);
			}
		}
		
		return result;
	}
	
	public int getMergingSteps() {
		return mergingSteps;
	}
	
	public abstract boolean isMergeStartConditionFulfilled();
	
	public abstract boolean isMergeEndConditionFulfilled();
	
	public abstract boolean merge();


	public int getInitialSize() {
		return initialSize;
	}

	
	public Set<Fragment> getMostSpecificFragmentsForPartialSignatureWithProvenanceIdentifiers(
			Signature partialTriplePatternSignature, Set<String> provenanceIdentifiers) {
		Set<Fragment> result = new LinkedHashSet<>();
		Signature newSignature = partialTriplePatternSignature.copy();
		for (String provenanceIdentifier : provenanceIdentifiers) {
			newSignature.setProvenanceIdentifier(provenanceIdentifier);			
			Fragment f = getFragmentBySingleSignature(newSignature);
			if (f != null) {
				result.add(f);
			}
		}
		
		return result;
	}


	public Set<Fragment> getLeastSpecificFragmentsForPartialSignatures(Set<String> provenanceIdentifiers) {
		Set<Fragment> result = new LinkedHashSet<>();
		for (String provenanceIdentifier : provenanceIdentifiers) {
			Signature signature = new Signature(null, null, null, provenanceIdentifier);
			Fragment f = getFragmentBySingleSignature(signature);
			if (f != null) {
				result.add(f);
			}
		}
		return result;
	}


	public Set<String> getAllPredicates(Fragment fragment) {
		Set<String> result = new LinkedHashSet<>();
		for (Signature s : fragment.getSignatures()) {
			if (s.getPredicate() != null) {
				result.add(s.getPredicate());
			} else {
				result.addAll(getRelationsForProvenanceIdentifier(s.getGraphLabel()));
			}
		}
		
		return result;

	}


	private List<String> getRelationsForProvenanceIdentifier(String graphLabel) {
		Collection<Fragment> fragments = provenanceId2FragmentMap.get(graphLabel);
		List<String> result = new ArrayList<>();
		for (Fragment f : fragments) {
			result.addAll(f.getPredicates());
		}
		
		return result;
	}

}
