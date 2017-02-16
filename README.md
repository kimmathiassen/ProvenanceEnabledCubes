# ProvenanceEnabledCubes

The purpose of the lattice is to select and materialize fragments such that we can answer provenance-enabled analytical queries.
The lattice is build by traversing the data and group data based on common shared features, currently it groups by predicates.
Using our selection algorithm fragments are selected for materialization (cached in memory).
When fragments have been materialized they can be used to answer an provenance-enabled analytical query.
This is done by first executing the provenance query and then analyzing the basic graph pattern of the analytical queries.
This gives us a set of provenance identifiers and a set of triple patterns.
Using the set of provenance identifiers and the set of triples pattens we can determine if any of the materialized fragments can be used to answer part of the query.
All materialized fragments that exist in the intersection of these two sets can be used to answer the analytical query.
