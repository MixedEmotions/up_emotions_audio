#!/bin/bash
curl --user testuser:testuser  -X GET "http://X.X.X.X:X/technologies/stt?path=$1"

#output form: {"result":{"info":{"id":"ID","state":"waiting"}}}
#OR once processing is finished:
#	{"result":{"one_best_result":{"segmentation":[
#	{"start":1600000,"end":4200000,"word":"<s>"},
#	{"start":4200000,"end":11100000,"word":"I"},
#	{“start":11100000,"end":14000000,"word":"AM"},
#	{"start":14000000,"end":19100000,"word":"FINE"},
#	{"start":19100000,"end":22800000,"word":"."}}]}}}
#	Where <s> denotes ‘silence’, and start/end are 0.0000001s → 1600000 = 0.16s
