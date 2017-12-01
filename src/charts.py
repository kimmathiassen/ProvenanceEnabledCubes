#!/usr/bin/python3

import argparse
import sys
import re
import statistics as stats
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

colors = ['red', 'blue', 'green', 'brown', 'pink', 'gray', 'yellow', 'violet']

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
        
    total = runtimeProvenance + cacheBuildTime + int(record[rI['runtime-analytical']]) \
    + int(record[rI['query-rewriting']]) + int(record[rI['construct-time']]) + int(record[rI['materialization-time']])
    
    return total
 
def computeCachedFragmentsRatio(record):
    return float(record[rI['n-cached-fragments']]) / float(record[rI['from-clauses']])

def parseFile(fileName, output, separateQueries): 
    print('Processing file ', fileName)
    lineNumber = 0
    
    with open(fileName, 'r') as iFile :
        for line in iFile.readlines() :
            lineNumber = lineNumber + 1
            if line.startswith('Timestamp') :
                continue
            else :
                record = line.rstrip('\n').split('\t')
                if len(record) < 22 :
                    print('Error at line ', lineNumber, ' in file ', fileName, '. The line has only ', len(record), ' columns')
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
                
                if separateQueries :    
                    aQuery = record[rI['analytical-query']]
                    if aQuery not in output[dataset][cache][selection][budget]:
                        output[dataset][cache][selection][budget][aQuery] = {}
                    
                    pQuery = record[rI['provenance-query']]
                    if pQuery not in output[dataset][cache][selection][budget][aQuery]:
                        output[dataset][cache][selection][budget][aQuery][pQuery] = {'total-response-time' : [], 'ratio-cached-fragments' : []}
                        
                    totalResponseTime = aggregateExecutionTimes(record)
                    cachedFragmentsRatio = computeCachedFragmentsRatio(record) 
                                        
                    output[dataset][cache][selection][budget][aQuery][pQuery]['total-response-time'].append(totalResponseTime)
                    output[dataset][cache][selection][budget][aQuery][pQuery]['ratio-cached-fragments'].append(cachedFragmentsRatio)                    
                        
                else:
                    query = record[rI['analytical-query']] + record[rI['provenance-query']]
                    if query not in output[dataset][cache][selection][budget] :
                         output[dataset][cache][selection][budget][query] = {'total-response-time' : [], 'ratio-cached-fragments' : []}
                    
                    totalResponseTime = aggregateExecutionTimes(record)
                    cachedFragmentsRatio = computeCachedFragmentsRatio(record) 
                                        
                    output[dataset][cache][selection][budget][query]['total-response-time'].append(totalResponseTime)
                    output[dataset][cache][selection][budget][query]['ratio-cached-fragments'].append(cachedFragmentsRatio)
    
    print(lineNumber, ' lines processed')
    return output
       
def parseData(dataFiles) :
    data = {}
    dataQueriesSep = {}
    for entry in dataFiles :
        if path.isdir(entry) :
            # Retrieve all the files in the directory
            for file in listdir(entry) :
                fullFile = entry + '/' + file
                if path.isfile(fullFile) and file.endswith('.log') :
                    parseFile(fullFile, data, False)
                    parseFile(fullFile, dataQueriesSep, True)
        elif path.isfile(entry) :
            parseFile(entry, data, False)
            parseFile(entry, dataQueriesSep, True)

    return data, dataQueriesSep

def getTotalAverageForBudget(dataForBudget, metric) :
    total = 0.0
    for query in dataForBudget :
        #avg = float(sum(dataForBudget[query]['total-response-time'])) / float(len(dataForBudget[query]['total-response-time'])) 
        avg = stats.median(dataForBudget[query][metric])
        total = total + avg
    
    return total / len(dataForBudget.keys())

def outputFigureHeaders(output):
    output.write('\\begin{figure}[ht]\n')
    output.write('\\centering\n')
    output.write('\\begin{tikzpicture}\n')
    output.write('\\begin{axis}[\n')
    
def getBudgetValue(dataForBudget, idx):
    budgets = sorted([int(x) for x in dataForBudget.keys()])
    return budgets[idx]
        
def formatDataset(datasetName):
    parts = datasetName.rstrip('/').split('/')
    if len(parts) == 2 :
        return parts[1]
    elif len(parts) == 3 :
        pattern = re.compile("sf([0-9]+)000lSplit([0-9]+)")
        matchObj = re.match(pattern, parts[2])
        return parts[1][0] + "-ssb-" + matchObj.group(1) + "K"
    else:
        return datasetName
    

def budgetVsResponseTime(data, cache, selectionStrategy, output):     
    outputFigureHeaders(output)
    output.write('xlabel=Budget,ylabel={Evaluation Time [s]},scale only axis,xmin=0,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=north east]\n')

    # Now generate a plot per dataset
    colorIdx = 0
    for dataset in data :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = float(budgets[len(budgets) - 1])
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            normalizedBudget = (float(budget) / dbSize) * 100
            finalValue = getTotalAverageForBudget(recordsForDataset[str(budget)], 'total-response-time')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue / 1000.0)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + formatDataset(dataset) + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')    
    output.write('\\caption{Budget vs. response time (' + cache +  ' cache, ' + selectionStrategy +  ')}\n')
    output.write('\\end{figure}\n')


def budgetVsCachedFragments(data, cache, selectionStrategy, output): 
    outputFigureHeaders(output)
    output.write('xlabel=Budget,ylabel={\\% of cached fragments used},xmin=0,scale only axis,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=south east]')

    # Now generate a plot per dataset
    colorIdx = 0
    for dataset in data :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = getBudgetValue(recordsForDataset, len(recordsForDataset) - 1)
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            normalizedBudget = (float(budget) / dbSize) * 100
            finalValue = getTotalAverageForBudget(recordsForDataset[str(budget)], 'ratio-cached-fragments')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue / 1000.0)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + formatDataset(dataset) + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')
    output.write('\\caption{Budget vs \\% of cached fragments(' + cache +  ' cache, ' + selectionStrategy + ')}\n')
    output.write('\\end{figure}\n')

def budgetVsResponseTimeForSingleStrategy(data, dataset, cache, output):
    outputFigureHeaders(output)
    output.write('xlabel=Budget,ylabel={Evaluation Time [s]},scale only axis,xmin=0,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=north east]\n')

    # Now generate a plot per dataset
    colorIdx = 0
    for selectionStrategy in data[dataset][cache] :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = float(budgets[len(budgets) - 1])
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            normalizedBudget = (float(budget) / dbSize) * 100
            finalValue = getTotalAverageForBudget(recordsForDataset[str(budget)], 'total-response-time')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue / 1000.0)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + selectionStrategy + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')
    output.write('\\caption{Budget vs. response time for ' + formatDataset(dataset) + '(' + cache  + ' cache)}\n')
    output.write('\\end{figure}\n')


def budgetVsCachedFragmentsForSingleStrategy(data, dataset, cache, output):
    outputFigureHeaders(output)
    output.write('xlabel=Budget,ylabel={\\% of cached fragments used},scale only axis,xmin=0,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=south east]')

    # Now generate a plot per dataset
    colorIdx = 0
    for selectionStrategy in data[dataset][cache] :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = float(budgets[len(budgets) - 1])
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            normalizedBudget = (float(budget) / dbSize) * 100
            finalValue = getTotalAverageForBudget(recordsForDataset[str(budget)], 'ratio-cached-fragments')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue / 1000.0)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + selectionStrategy + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')
    output.write('\\caption{Budget vs \\% of cached fragments for ' + dataset + '(' + cache  + ' cache)}')
    output.write('\\end{figure}\n')


def queryVsResponseTime(data, dataset, cache, optimalBudgetIdx, foutput):
    colorIdx = 0
    outputFigureHeaders(foutput)
    optmBudget = str(getBudgetValue(data[dataset][cache]['ilp-distance'], optimalBudgetIdx))
    fout.write('symbolic x coords={')
    queryLabels = []
    for aQuery in data[dataset][cache]['ilp-distance'][optmBudget] :
        queryLabels.append(aQuery.replace('.txt', ''))
        
    queryLabels.sort()
    fout.write(",".join(queryLabels) + "},")     
    fout.write('xtick=data,ylabel={Evaluation Time [s]},x tick label style={yshift={-mod(\\ticknum,2)*1em}},')
    fout.write('scale only axis,y label style={at={(-0.1,0.5)}},width=1\\linewidth,ybar,style={font=\\scriptsize},')
    fout.write('cycle list name=exotic,legend style={at={(0.5,1.1)},anchor=north,legend columns=-1},bar width=3pt,]')

    strategies = []
    for selectionStrategy in data[dataset][cache] :                
        fout.write('\\addplot[color=' + colors[colorIdx % len(colors)] + '] coordinates {\n')
        colorIdx = colorIdx + 1
        for queryLabel in queryLabels :            
            # Get the most expensive provenance query
            mostExpensiveQuery = -1
            aQuery = queryLabel + '.txt'
            for pQuery in data[dataset][cache][selectionStrategy][optmBudget][aQuery] :                
                finalValue = stats.median(data[dataset][cache][selectionStrategy][optmBudget][aQuery][pQuery]['total-response-time'])
                if finalValue > mostExpensiveQuery :
                    mostExpensiveQuery = finalValue
            
            fout.write('(' + queryLabel + ', ' + str(mostExpensiveQuery / 1000.0) + ')\n' )
        fout.write('};')  
        strategies.append(selectionStrategy)  
    fout.write('\\legend{' + ','.join(strategies) + '}')    
    fout.write('\\end{axis}\n\\end{tikzpicture}\n')
    fout.write('\\caption{Evaluation time for each query for ' + formatDataset(dataset) + '(' + cache  + ' cache, ' + selectionStrategy + ')}')
    fout.write('\\end{figure}\n')


def numberOfObservationsVsResponseTime(data, foutput):
    return None
    

def outputHeaders(foutput):    
    foutput.write('\\documentclass{report}\n')
    foutput.write('\\usepackage{tikz}')
    foutput.write('\\usetikzlibrary{arrows,shapes,decorations,automata,backgrounds,petri, fit,positioning,matrix,patterns,plotmarks}')
    foutput.write('\\usepackage{pgfplots}')
    foutput.write('\\begin{document}\n')
    
def outputFooters(foutput):
    foutput.write('\\end{document}\n')

parser = argparse.ArgumentParser()
parser.add_argument("-c", "--config", help="Path to the configuration file", default=None)

args = parser.parse_args()
if args.config is not None :
    configFile = args.config
else:
    print('Searching for file config.ini in the working directory')
    if not os.path.exists(configFile) :
        print('The program could not find the file config.ini. Aborting...')
        sys.exit(1)

confObj = parseConfigFile(args.config)

# Parse the data
data, dataQueriesSep = parseData(confObj.data)

with open(confObj.output[0], 'w') as fout :
    outputHeaders(fout)
    
    # Decide which charts to output
    for chart in confObj.charts :
        if chart not in supportedCharts :
            print('Unsupported chart ', chart)
            continue
        if chart == 'budget-vs-response-time' :
            budgetVsResponseTime(data, 'cold', 'ilp-distance', fout)
            budgetVsResponseTime(data, 'warm', 'ilp-distance', fout)
            budgetVsResponseTime(data, 'cold', 'lru', fout)
            budgetVsResponseTime(data, 'warm', 'lru', fout)
            for dataset in data: 
                budgetVsResponseTimeForSingleStrategy(data, dataset, 'cold', fout)
                budgetVsResponseTimeForSingleStrategy(data, dataset, 'warm', fout)            
        elif chart == 'budget-vs-cached-fragments' :
            budgetVsCachedFragments(data, 'cold', 'ilp-distance', fout)
            budgetVsCachedFragments(data, 'warm', 'ilp-distance', fout)
            budgetVsCachedFragments(data, 'cold', 'lru', fout)
            budgetVsCachedFragments(data, 'warm', 'lru', fout)
            for dataset in data: 
                budgetVsCachedFragmentsForSingleStrategy(data, dataset, 'cold', fout)
                budgetVsCachedFragmentsForSingleStrategy(data, dataset, 'warm', fout)                        
        elif chart == 'query-vs-response-time' :
            for dataset in data:
                queryVsResponseTime(dataQueriesSep, dataset, 'cold', int(confObj.optimal_budget_query_vs_response_time[0]), fout)
                queryVsResponseTime(dataQueriesSep, dataset, 'warm', int(confObj.optimal_budget_query_vs_response_time[0]), fout)
        elif chart == 'number-of-observations-vs-response-time' :
            numberOfObservationsVsResponseTime(data, fout)
        
    outputFooters(fout)