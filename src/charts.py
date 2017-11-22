#!/usr/bin/python

import argparse

supportedCharts = ['budget-vs-time', 'budget-vs-cached-fragments', 
                   'query-vs-response-time', 'number-of-observations-vs-response-time']

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



parser = argparse.ArgumentParser()
parser.add_argument("-c", "--config", help="Path to the configuration file", action="help", default=argparse.SUPPRESS)
parser.add_argument("-h", "--help", help="Shows this help message and exits")

args = parser.parse_args()
if args.config is not None :
    configFile = args.config
    args = parseConfigFile(args.config)


