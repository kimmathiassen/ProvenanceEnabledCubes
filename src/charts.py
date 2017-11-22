#!/usr/bin/python

import argparse
from os import path
from os import listdir
from numpy.core import records
from pyatspi import selection

supportedCharts = ['budget-vs-response-time', 'budget-vs-cached-fragments', 
                   'query-vs-response-time', 'number-of-observations-vs-response-time']

rI = {'dataset': 3, 'budget': 4, 'analytical-query': 1, 'provenance-query' : 2, 
      'runtime-analytical': 20, 'query-rewriting': 19, 'construct-time': 18, 'materialization-time': 17, 
      'cache-build-time' : 16, 'runtime-provenance': 21, 'from-clauses': 11, 'n-cached-fragments' : 12,
      'selection-strategy': 7, 'cache-strategy': 8}

configFile = 'config.ini'

class Conf(object):
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


def aggregateExecutionTimes(record): 
    runtimeProvenance = (0 if record[rI['runtime-provenance']] == '-1' else int(record[rI['runtime-provenance']]))
    cacheBuildTime = (0 if record[rI['cache-build-time']] == '-1' else int(record[rI['cache-build-time']])) 
    
    return runtimeProvenance +  cacheBuildTime + int(record[rI['runtime-analytical']]) 
    + int(record[rI['query-rewriting']]) + record[rI['construct-time']] + record[rI['materialization-time']]

 
def computeCachedFragmentsRatio(record):
    return float(record[rI['n-cached-fragments']]) / float(record[rI['from-clauses']])

def parseFile(fileName, output): 
    print 'Processing file ', fileName
    lineNumber = 0
    
    with open(fileName, 'r') as iFile :
        for line in iFile.readlines() :
            #print line
            lineNumber = lineNumber + 1
            if line.startswith('Timestamp') :
                continue
            else :
                record = line.rstrip('\n').split('\t')
                if len(record) < 22 :
                    print 'Error at line ', lineNumber, ' in file ', fileName, '. The line has only ', len(record), ' columns'
                dataset = record[rI['dataset']]
                if dataset not in output :
                    output[dataset] = {}
                    
                cache = record[rI['cache-strategy']]
                if cache not in output[dataset] :
                    output[dataset][cache] = {}

                selection = record[rI['selection-strategy']]
                if selection not in output[dataset][cache] :
                    output[dataset][cache][selection] = {}                    
                
                budget = record[rI['budget']]
                if budget not in output[dataset][cache][selection] :                    
                    output[dataset][cache][selection][budget] = {}                                    
                    
                query = record[rI['analytical-query']] + record[rI['provenance-query']]
                if query not in output[dataset][cache][selection][budget] :
                     output[dataset][cache][selection][budget][query] = {'total-response-time' : [], 'ratio-cached-fragments' : []}
                
                totalResponseTime = aggregateExecutionTimes(record)
                cachedFragmentsRatio = computeCachedFragmentsRatio(record) 
                                    
                output[dataset][cache][selection][budget][query]['total-response-time'].append(totalResponseTime)
                output[dataset][cache][selection][budget][query]['ratio-cached-fragments'].append(cachedFragmentsRatio)
    
    print lineNumber, ' lines processed'
    return output
       
def parseData(dataFiles) :
    data = {}
    for entry in dataFiles :
        if path.isdir(entry) :
            # Retrieve all the files in the directory
            for file in listdir(entry) :
                fullFile = entry + '/' + file
                if path.isfile(fullFile) and file.endswith('.log') :
                    parseFile(fullFile, data)
        elif path.isfile(entry) :
            parseFile(entry, data)

    return data

def getTotalAverageResponseTimeForBudget(dataForBudget) :
    total = 0.0
    nRecords = 0
    for query in dataForBudget :
        avg = float(sum(dataForBudget[query]['total-response-time'])) / float(len(dataForBudget[query]['total-response-time'])) 
        total = total + avg
        nRecords = nRecords + 1
    
    return avg / nRecords

def budgetVsResponseTime(data, cache, selectionStrategy, output):     
    output.write('\\begin{figure}[ht]')
    #output.write('\\begin{minipage}[b]{0.40\linewidth}')
    output.write('\\centering')
    output.write('\\begin{tikzpicture}')
    output.write('\\begin{axis}[')
    output.write('xlabel=Budget,ylabel={Evaluation Time [s]},scale only axis,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=north east]')

    # Now generate a plot per dataset
    for dataset in data :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted(recordsForDataset.keys());
        dbSize = float(budgets[len(budgets) - 1])
        output.write('\\addplot[color=red,mark=x] coordinates {')
        for budget in budgets :
            normalizedBudget = (float(budget) / dbSize) * 100
            finalValue = getTotalAverageResponseTimeForBudget(recordsForDataset[budget])
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue)  + ')' )
        output.write('};')

    output.write('\\end{axis}\\end{tikzpicture}\\end{figure}')

def budgetVsCachedFragments(data, foutput): 
    return None

def queryVsResponseTime(data, foutput):
    return None

def numberOfObservationsVsResponseTime(data, foutput):
    return None
    

def outputHeaders(foutput):    
    foutput.write('\\documentclass{report}')
    foutput.write('\\begin{document}')
    
def outputFooters(foutput):
    foutput.write('\\end{document}')

parser = argparse.ArgumentParser()
parser.add_argument("-c", "--config", help="Path to the configuration file", default=None)

args = parser.parse_args()
if args.config is not None :
    configFile = args.config
else:
    print 'Searching for file config.ini in the working directory'
    if not os.path.exists(configFile) :
        print 'The program could not find the file config.ini. Aborting...'
        sys.exit(1)

confObj = parseConfigFile(args.config)

# Parse the data
data = parseData(confObj.data)

with open(confObj.output[0], 'w') as fout :
    outputHeaders(fout)
    
    # Decide which charts to output
    for chart in confObj.charts :
        if chart not in supportedCharts :
            print 'Unsupported chart ', chart
            continue
        if chart == 'budget-vs-response-time' :
            budgetVsResponseTime(data, 'cold', 'ilp-distance', fout)
#            budgetVsResponseTime(data, 'warm', 'ilp-distance', fout)
#            budgetVsResponseTime(data, 'cold', 'lru', fout)
#            budgetVsResponseTime(data, 'warm', 'lru', fout)            
        elif chart == 'budget-vs-cached-fragments' :
            budgetVsCachedFragments(data, fout)
        elif chart == 'query-vs-response-time' :
            queryVsResponseTime(data, fout)    
        elif chart == 'number-of-observations-vs-response-time' :
            numberOfObservationsVsResponseTime(data, fout)
        
    outputFooters(fout)