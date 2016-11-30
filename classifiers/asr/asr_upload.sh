#!/bin/bash
curl -X POST --data-binary @$1 --user testuser:testuser  http://X.X.X.X:X/audiofile?path=$1

#output form:{"result":{"info":{"name":"FILENAME.wav"}}}

