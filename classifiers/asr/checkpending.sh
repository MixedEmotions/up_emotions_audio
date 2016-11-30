#!/bin/bash
curl --user testuser:testuser -X GET "http://X.X.X.X:X/pending/$1"

#output form: {"result":{"info":{"id":"ID","state":"waiting"}}}
#OR
#{"result":{"info":{"id":"ID","state":"finished"}}}


