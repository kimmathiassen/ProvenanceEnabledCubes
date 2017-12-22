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
                   'query-vs-response-time', 'number-of-observations-vs-response-time',
                   'naive-vs-query-rewriting-response-time']

supportedCaches = ['cold', 'warm']

rI = {'dataset': 3, 'budget': 4, 'analytical-query': 1, 'provenance-query' : 2, 
      'runtime-analytical': 20, 'query-rewriting': 19, 'construct-time': 18, 'materialization-time': 17, 
      'cache-build-time' : 16, 'runtime-provenance': 21, 'from-clauses': 11, 'n-cached-fragments' : 12,
      'selection-strategy': 7, 'cache-strategy': 8, 'budget-jena': 5, 'materialized-data-size': 10}

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
                if budget == '0' and record[rI['budget-jena']] != 'default' :
                    budget = record[rI['budget-jena']]
                
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
                    materializationSize = int(record[rI['materialized-fragments-size']])
                                        
                    output[dataset][cache][selection][budget][aQuery][pQuery]['total-response-time'].append(totalResponseTime)
                    output[dataset][cache][selection][budget][aQuery][pQuery]['ratio-cached-fragments'].append(cachedFragmentsRatio)                    
                    output[dataset][cache][selection][budget][aQuery][pQuery]['materialized-fragments-size'].append(materializationSize)    
                else:
                    query = record[rI['analytical-query']] + record[rI['provenance-query']]
                    if query not in output[dataset][cache][selection][budget] :
                         output[dataset][cache][selection][budget][query] = {'total-response-time' : [], 'ratio-cached-fragments' : []}
                    
                    totalResponseTime = aggregateExecutionTimes(record)
                    cachedFragmentsRatio = computeCachedFragmentsRatio(record) 
                    materializationSize = int(record[rI['materialized-fragments-size']])
                                        
                    output[dataset][cache][selection][budget][query]['total-response-time'].append(totalResponseTime)
                    output[dataset][cache][selection][budget][query]['ratio-cached-fragments'].append(cachedFragmentsRatio)
                    output[dataset][cache][selection][budget][query]['materialized-fragments-size'].append(materializationSize)
    
    # Small fix to force comparison of the tepid strategy with the cold cache
    for dataset in output :
        if 'tepid' in output[dataset] and 'cold' in output[dataset] :
            if 'tdb' in output[dataset]['tepid'] :
                output[dataset]['cold']['tdb'] = output[dataset]['tepid']['tdb']
    
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

    # Construct an index based on query
    queryIndex = indexPerAQuery(data)
    return data, dataQueriesSep, queryIndex

def indexPerAQuery(data): 
    output = {}
    for dataset in data :
        output[dataset] = {}
        for cache in data[dataset] :
            for selection in data[dataset][cache] :
                for budget in data[dataset][cache][selection] :
                    for aQuery in data[dataset][cache][selection][budget] :
                        if aQuery not in output[dataset] :
                            output[dataset][aQuery] = {}
                        
                        if cache not in output[dataset][aQuery] :
                            output[dataset][aQuery][cache] = {}
                            
                        if selection not in output[dataset][aQuery][cache] :
                            output[dataset][aQuery][cache][selection] = {}
                            
                        if budget not in output[dataset][aQuery][cache][selection] :
                            output[dataset][aQuery][cache][selection][budget] = {}
                            
                        
                        output[dataset][aQuery][cache][selection][budget] = data[dataset][cache][selection][budget][aQuery]   
    
    return output

def getAverageForBudget(dataForBudget, metric):
    return getTotalAverageForBudget(dataForBudget, metric) / len(dataForBudget.keys())


def getTotalAverageForBudget(dataForBudget, metric) :
    total = 0.0
    for query in dataForBudget :
        #avg = float(sum(dataForBudget[query]['total-response-time'])) / float(len(dataForBudget[query]['total-response-time'])) 
        avg = stats.mean(dataForBudget[query][metric])
        total = total + avg
    
    return total #/ len(dataForBudget.keys())

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
    datasets = []
    for d in data :
        if cache not in data[d] :
            continue
        if selectionStrategy not in data[d][cache] :
            continue
        
        datasets.append(d)
    
    outputFigureHeaders(output)
    output.write('xlabel=Budget(\\%),ylabel={Evaluation Time [s]},scale only axis,xmin=0,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=north east]\n')

    # Now generate a plot per dataset
    colorIdx = 0
    for dataset in datasets :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = float(budgets[len(budgets) - 1])
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            if selectionStrategy != 'tdb' :
                normalizedBudget = (float(budget) / dbSize) * 100
            else :
                normalizedBudget = budget
            
            finalValue = getTotalAverageForBudget(recordsForDataset[str(budget)], 'total-response-time')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue / 1000.0)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + formatDataset(dataset) + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')    
    output.write('\\caption{Budget vs. response time (' + cache +  ' cache, ' + selectionStrategy +  ')}\n')
    output.write('\\end{figure}\n')


def budgetVsCachedFragments(data, cache, selectionStrategy, output): 
    if cache == 'tepid' or selectionStrategy == 'tdb' :
        return
    
    datasets = []
    for d in data :
        if cache not in data[d] :
            continue
        if selectionStrategy not in data[d][cache] :
            continue
        
        datasets.append(d)

    
    outputFigureHeaders(output)
    output.write('xlabel=Budget(\\%),ylabel={cache hit-rate},xmin=0,scale only axis,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=south east]')

    # Now generate a plot per dataset
    colorIdx = 0
    for dataset in datasets :
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = getBudgetValue(recordsForDataset, len(recordsForDataset) - 1)
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            normalizedBudget = (float(budget) / dbSize) * 100
            finalValue = getAverageForBudget(recordsForDataset[str(budget)], 'ratio-cached-fragments')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + formatDataset(dataset) + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')
    output.write('\\caption{Budget vs \\% of cached fragments(' + cache +  ' cache, ' + selectionStrategy + ')}\n')
    output.write('\\end{figure}\n')

def budgetVsResponseTimeForSingleStrategy(data, dataset, cache, output):
    if dataset in data:
        if cache not in data[dataset] :
            return
    else:
        return
    
    
    outputFigureHeaders(output)
    output.write('xlabel=Budget,ylabel={Evaluation Time [s]},scale only axis,xmin=0,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=north east]\n')

    # Now generate a plot per dataset
    colorIdx = 0
    for selectionStrategy in data[dataset][cache] :
        if selectionStrategy == 'mockup' :
            continue
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
    if cache == 'tepid' :
        return

    if dataset in data:
        if cache not in data[dataset] :
            return
    else:
        return
        
    outputFigureHeaders(output)
    output.write('xlabel=Budget,ylabel={cache hit-rate},scale only axis,xmin=0,y label style={at={(-0.1,0.5)}},width=1\\linewidth,legend pos=south east]')

    # Now generate a plot per dataset
    colorIdx = 0
    for selectionStrategy in data[dataset][cache] :
        if selectionStrategy == 'mockup' or selectionStrategy == 'tdb':
            continue
        recordsForDataset = data[dataset][cache][selectionStrategy]
        # Gather all budgets and normalize them
        budgets = sorted([int(x) for x in recordsForDataset.keys()])
        dbSize = float(budgets[len(budgets) - 1])
        output.write('\\addplot[color=' + colors[colorIdx % len(colors)] + ',mark=x] coordinates {\n')
        for budget in budgets :
            if selectionStrategy != 'tdb' :
                normalizedBudget = (float(budget) / dbSize) * 100
            else :
                normalizedBudget = budget
            finalValue = getAverageForBudget(recordsForDataset[str(budget)], 'ratio-cached-fragments')
            output.write('(' + str(normalizedBudget) + ', ' + str(finalValue)  + ')\n' )
        output.write('};\n')
        output.write('\\addlegendentry{' + selectionStrategy + '}\n')
        colorIdx = colorIdx + 1

    output.write('\\end{axis}\n\\end{tikzpicture}\n')
    output.write('\\caption{Budget vs \\% of cached fragments for ' + dataset + '(' + cache  + ' cache)}\n')
    output.write('\\end{figure}\n')


def queryVsResponseTime(data, dataset, cache, optimalBudgetIdx, foutput):
    if cache not in data[dataset] :
        return
        
    metric = 'ilp-distance'
    if metric not in data[dataset][cache] :        
        if 'ilp-distance-improved' in data[dataset][cache] :
            metric = 'ilp-distance-improved'
        else :
            return
    
    if optimalBudgetIdx >= len(data[dataset][cache][metric]) :
        return 
    
    colorIdx = 0
    outputFigureHeaders(foutput)

    
    optmBudget = str(getBudgetValue(data[dataset][cache][metric], optimalBudgetIdx))
    fout.write('symbolic x coords={')
    queryLabels = []
    for aQuery in data[dataset][cache][metric][optmBudget] :
        queryLabels.append(aQuery.replace('.txt', ''))
        
    queryLabels.sort()
    fout.write(",".join(queryLabels) + "},")     
    fout.write('xtick=data,ylabel={Evaluation Time [s]},x tick label style={yshift={-mod(\\ticknum,2)*1em}},')
    fout.write('scale only axis,y label style={at={(-0.1,0.5)}},width=1\\linewidth,ybar,style={font=\\scriptsize},')
    fout.write('cycle list name=exotic,legend style={at={(0.5,1.1)},anchor=north,legend columns=-1},bar width=3pt,]')

    strategies = []
    for selectionStrategy in data[dataset][cache] :   
        if selectionStrategy == 'mockup' :
            continue             
        
        if selectionStrategy == 'tdb' :
            bs = [int(x) for x in data[dataset][cache][selectionStrategy]]
            bs.sort()
            optmBudgetKey = str(bs[optimalBudgetIdx])
        else :
            optmBudgetKey = optmBudget  
        
        fout.write('\\addplot[color=' + colors[colorIdx % len(colors)] + '] coordinates {\n')
        colorIdx = colorIdx + 1
        for queryLabel in queryLabels :            
            # Get the most expensive provenance query
            mostExpensiveQuery = -1
            aQuery = queryLabel + '.txt'
            for pQuery in data[dataset][cache][selectionStrategy][optmBudgetKey][aQuery] :                
                finalValue = stats.median(data[dataset][cache][selectionStrategy][optmBudgetKey][aQuery][pQuery]['total-response-time'])
                if finalValue > mostExpensiveQuery :
                    mostExpensiveQuery = finalValue
            
            fout.write('(' + queryLabel + ', ' + str(mostExpensiveQuery / 1000.0) + ')\n' )
        fout.write('};')  
        strategies.append(selectionStrategy)  
    fout.write('\\legend{' + ','.join(strategies) + '}')    
    fout.write('\\end{axis}\n\\end{tikzpicture}\n')
    fout.write('\\caption{Evaluation time for each query for ' + formatDataset(dataset) + '(' + cache  + ' cache)}')
    fout.write('\\end{figure}\n')


def naiveVsQueryRewritingResponseTime(data, foutput) :
    # output[dataset][cache][selection][budget][aQuery][pQuery]['total-response-time']
    colorIdx = 0
    for cache in supportedCaches :        
        datasets = []
        
        for d in data :
            if cache in data[d] :
                if 'mockup' in data[d][cache] and \
                ('ilp-distance-improved' in data[d][cache] or 'ilp-distance' in data[d][cache]) :
                    oponent = 'ilp-distance'
                    if not oponent in data[d][cache] :
                        oponent = 'ilp-distance-improved'
                        
                    if '0' in data[d][cache]['mockup'] and '0' in data[d][cache][oponent] :
                        datasets.append(d)
        
        if len(datasets) == 0 :
            continue
        
        outputFigureHeaders(foutput)
        foutput.write('symbolic x coords={')
        foutput.write(",".join([formatDataset(x) for x in datasets]) + "},")     
        foutput.write('xtick=data,ylabel={Evaluation Time [s]},x tick label style={yshift={-mod(\\ticknum,2)*1em}},')
        foutput.write('scale only axis,y label style={at={(-0.1,0.5)}},width=1\\linewidth,ybar,style={font=\\scriptsize},')
        foutput.write('cycle list name=exotic,legend style={at={(0.5,1.1)},anchor=north,legend columns=-1},bar width=3pt,]\n')

        for method in ['mockup', 'optm'] :
            foutput.write('\\addplot[color=' + colors[colorIdx % len(colors)] + '] coordinates {\n')
            colorIdx = colorIdx + 1
            oponent = method
            if method == 'optm' :
                oponent = 'ilp-distance' if 'ilp-distance' in data[dataset][cache] else 'ilp-distance-improved'
            for dataset in datasets :                                
                value = getTotalAverageForBudget(data[dataset][cache][oponent]['0'], 'total-response-time')
                foutput.write('(' + formatDataset(dataset) + ', ' + str(value / 1000.0) + ')\n' )
                    
            foutput.write('};\n') 
                
        foutput.write('\\legend{' + ','.join(['no-optm', 'optm']) + '}\n')    
        foutput.write('\\end{axis}\n\\end{tikzpicture}\n')
        foutput.write('\\caption{Total evaluation time per dataset (' + cache  + ' cache, budget=0, no-optimization vs. our query rewriting)}')
        foutput.write('\\end{figure}\n')
                    

def numberOfObservationsVsResponseTime(data, foutput):
    return None
    

def outputHeaders(foutput):    
    foutput.write('\\documentclass{report}\n')
    foutput.write('\\usepackage{tikz}\n')
    foutput.write('\\usetikzlibrary{arrows,shapes,decorations,automata,backgrounds,petri, fit,positioning,matrix,patterns,plotmarks}\n')
    foutput.write('\\usepackage{pgfplots}\n')
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
data, dataQueriesSep, queryData = parseData(confObj.data)

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
            budgetVsResponseTime(data, 'cold', 'ilp-distance-improved', fout)
            budgetVsResponseTime(data, 'warm', 'ilp-distance-improved', fout)
            budgetVsResponseTime(data, 'cold', 'lru', fout)
            budgetVsResponseTime(data, 'warm', 'lru', fout)
            budgetVsResponseTime(data, 'tepid', 'tdb', fout)
            for dataset in data: 
                budgetVsResponseTimeForSingleStrategy(data, dataset, 'cold', fout)
                budgetVsResponseTimeForSingleStrategy(data, dataset, 'warm', fout)            
        elif chart == 'budget-vs-cached-fragments' :
            budgetVsCachedFragments(data, 'cold', 'ilp-distance', fout)
            budgetVsCachedFragments(data, 'warm', 'ilp-distance', fout)
            budgetVsCachedFragments(data, 'cold', 'ilp-distance-improved', fout)
            budgetVsCachedFragments(data, 'warm', 'ilp-distance-improved', fout)
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
        elif chart == 'naive-vs-query-rewriting-response-time' :
            naiveVsQueryRewritingResponseTime(data, fout)
        # elif chart == 'query-vs-budget' :
        #    for dataset in queryData :
        #        queryVsBudget(dataset, )
        
    outputFooters(fout)
