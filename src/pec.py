#!/usr/bin/python

import argparse
import os
import subprocess
import sys
import uuid

offlineParams = ['load-instance-data', 'budget-percentage', 'lattice-merge-strategy', 'budget', 'add-cache'] 

budgetZeroRun = []

class Conf(object) :
	pass

def parseConfigFile(configFile) :
	config = Conf()
	with open(configFile, 'r') as f :
		for line in f :
			if line == '\n' or line.startswith('%') :
				continue
			line = line.rstrip()	
			parts = line.split(':')
			if len(parts) < 2 :
				continue
			
			param = parts[0].strip()
			value = parts[1].strip()
			param = param.replace('-', '_')			
			if not hasattr(config, param) :				
				setattr(config, param, [])
			getattr(config, param).append(value)
			
	return config	


def parseBudget(inFile, budget):
	if budget == 0 or budget == 0.0 :
		combination = dataset + cachingStrat
		if combination not in budgetZeroRun :
			writeBudget(inFile, budget)
			budgetZeroRun.append(combination)
	else :
		writeBudget(inFile, budget)


def writeBudget(ifile, budget):		
	if type(budget) == int:
		ifile.write('budget: ' + str(budget) + '\n')
	else :
		ifile.write('budget-percentage: ' + str("%.0f" % budget) + '\n')

def dumpParameters(ifile, args, dataset, cachingStrat, budget, mergingStrat) :
	ifile.write('load-instance-data: ' + dataset + '\n')
	
	if type(budget) in (tuple, list) :		
		for b in budget :
			parseBudget(ifile, b)	
	else :
		parseBudget(ifile, budget)
	
	ifile.write('lattice-merge-strategy: ' + mergingStrat + '\n')
	ifile.write('add-cache: ' + cachingStrat + '\n')
	
	for attr in vars(args) :
		cleanAttr = attr.replace('_', '-')
		if hasattr(args, attr) :
			values = getattr(args, attr)
			for value in values :
				if cleanAttr in offlineParams :
					continue
				if not type(value) == str :
					ifile.write(cleanAttr + ': ' + str(value) + '\n')
				else :	
					ifile.write(cleanAttr + ': ' + value + '\n')

				
parser = argparse.ArgumentParser()
parser.add_argument("-c", "--config", help="Path to the configuration file", default=None)
parser.add_argument("-lid", "--load_instance_data", help="Path to the data")
parser.add_argument("-lcs", "--load_cube_structure", help="Path to the cube structure (schema)")
parser.add_argument("-dt", "--database_type", help="Database type. Supported types: tdb, inmemory")
parser.add_argument("-pq", "--provenance_queries", help="Path to a single file or directory with the provenance queries")
parser.add_argument("-aq", "--analytical_queries", help="Path to a single file or directory with the analytical queries")
parser.add_argument("-ill", "--ilp_log_location", help="Path to the log file for the ILP fragments selector")
parser.add_argument("-gll", "--greedy_log_location", help="Path to the log file for the greedy fragments selector")
parser.add_argument("-nll", "--naive_log_location", help="Path to the log file for the naive fragments selector")
parser.add_argument("-bp", "--budget_percentage", help="Memory budget expressed in terms of the percentage of triples in the input dataset", type=float)
parser.add_argument("-b", "--budget", help="Memory budget expressed in absolute number of triples", type=int)
parser.add_argument("-rll", "--result_log_location", help="Log file for the results of the execution of queries")
parser.add_argument("-ell", "--experimental_log_location", help="Log file for the experimental results")
parser.add_argument("-fs", "--fragment_selector", help="Fragment selector for materialization of fragments. Supported values: ilp, greedy, naive")
parser.add_argument("-ac", "--add_cache", help="Add caching strategy. Supported values: cold, warm")
parser.add_argument("-to", "--timeout", help="Timeout for the execution of queries", type=int)
parser.add_argument("-es", "--evaluation_strategy", help="Query evaluation strategy. Supported values: basic, fullMaterialization")
parser.add_argument("-lms", "--lattice_merge_strategy", help="Lattice merging strategy. Supported values: noMerge, naive, property, provenance")
parser.add_argument("-rr", "--reduce_ratio", help="Lattice merging reduce ratio (value between 0 and 1). If the merging strategy is naive, property or provenance, it reduces the lattice")
parser.add_argument("-oits", "--output_ilp_to_stdout", action="store_true")


args = parser.parse_args()
if args.config is not None :
	configFile = args.config
	args = parseConfigFile(args.config)

# Parse the offline parameters

datasets = None
if type(args.load_instance_data) == list :
	datasets = args.load_instance_data
elif type(args.load_instance_data) == str :
	datasets = [args.load_instance_data]
	
budgets = []	
if hasattr(args, 'budget') :
	if type(args.budget) == list :
		budgets.extend([int(b) for b in args.budget])
	elif type(args.budget) == int :	
		budgets.append(args.budget)

# Give preference to budgets provided as ratios
if hasattr(args, 'budget_percentage') :
	if type(args.budget_percentage) == list :
		budgets.extend([float(b) for b in args.budget_percentage])
	elif type(args.budget_percentage) == float :	
		budgets.append(args.budget_percentage)

mergingStrategies = None
if type(args.lattice_merge_strategy) == list :
	mergingStrategies = args.lattice_merge_strategy
elif type(args.lattice_merge_strategy) == str :
	mergingStrategies = [args.lattice_merge_strategy]	

cachingStrategies = None
if type(args.add_cache) == list :
	cachingStrategies = args.add_cache
elif type(args.add_cache) == str :
	cachingStrategies = [args.add_cache]

assert datasets is not None
assert budgets is not None and len(budgets) > 0
assert mergingStrategies is not None
assert cachingStrategies is not None and len(cachingStrategies) > 0
print datasets, cachingStrategies, budgets, mergingStrategies
## Create a temporary config file per combination of offline arguments
for dataset in datasets :
	for cachingStrat in cachingStrategies :
		if cachingStrat == 'cold' :
			for budget in budgets :
				for mergingStrat in mergingStrategies :
					filename = str(uuid.uuid4())
					try : 
						fileObj = open(filename, 'w')
						dumpParameters(fileObj, args, dataset, cachingStrat, budget, mergingStrat)
						fileObj.close()
						print 'Offline arguments: ' + dataset + ', ' + cachingStrat + ', ' + str(budget) + ', ' + mergingStrat
						cmdLine = 'java -jar pec.jar -c ' + filename + ' >> pec-stdout 2>&1'				
						print 'Running', cmdLine			
						retVal = os.system(cmdLine)
						print 'Command returned value ' + str(os.WEXITSTATUS(retVal))
					finally:
						os.unlink(filename)
		elif cachingStrat == 'warm' :
			for mergingStrat in mergingStrategies :
				filename = str(uuid.uuid4())
				try : 
					fileObj = open(filename, 'w')
					dumpParameters(fileObj, args, dataset, cachingStrat, budgets, mergingStrat)
					fileObj.close()
					print 'Offline arguments: ' + dataset + ', ' + cachingStrat + ', ' + str(budgets) + ', ' + mergingStrat
					cmdLine = 'java -jar pec.jar -c ' + filename + ' >> pec-stdout 2>&1'				
					print 'Running', cmdLine			
					retVal = os.system(cmdLine)
					print 'Command returned value ' + str(os.WEXITSTATUS(retVal))
				finally:
					os.unlink(filename)
						