package dk.aau.cs.qweb.pec.lattice;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.commons.lang3.tuple.Pair;

import dk.aau.cs.qweb.pec.data.InMemoryRDFCubeDataSource;
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
				Signature<String, String, String, String> signature = fragment.getFirstSignature();
				String domain = signature.getFirst();
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
		strBuilder.append(root);
		strBuilder.append("\n");
		for (Fragment partition : (Set<Fragment>)parentsGraph.keySet()) {
			strBuilder.append(partition + "---->" + parentsGraph.get(partition) + "\n");	
		}
		strBuilder.append(metadataMap + "\n");
		return strBuilder.toString();
		
	}
	
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
			}
			if (relationSignature.getThird() != null) {
				partitionsRangeOfSignatureMap.put(relationSignature.getThird(), relationPlusProvPartition);
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

	@Override
	public Iterator<Fragment> iterator() {
		// TODO Auto-generated method stub
		return new Iterator<Fragment>() {
			@SuppressWarnings("unchecked")
			Iterator<Fragment> it = parentsGraph.keySet().iterator();
			
			boolean rootVisited = false;
			
			@Override
			public boolean hasNext() {
				if (rootVisited) {
					return it.hasNext();
				} else {
					return true;
				}
			}

			@Override
			public Fragment next() {
				if (rootVisited) {
					return it.next();
				} else {
					rootVisited = true;
					return root;
				}
			}	
		};
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
