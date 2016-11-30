#!/bin/bash
#echo "h$1"
#python rnn_sentiment.py --mode=sentence --sentence="$1"
str="$1"
os=$(echo $str| sed 's/_/ /g')
#echo ${os}
##strrep=${str//_/ }
v=$(python rnn_sentiment.py --mode=sentence --sentence="$os" | sed -n '1!p')
##echo $(python rnn_sentiment.py --mode=sentence --sentence=\"$1\"
##v=$(python rnn_sentiment.py --mode=sentence --sentence=\"$1\" | sed -n '1!p')
a=`awk "BEGIN{printf \"%.3f\",$v}"`
printf '​{ "PROCESSOR":"python","ORIGIN":"theano","TYPE":"regression","COMPONENT":"sentiment","VIDX":0,"VALUE":'$a',"PROB":[{"CONFIDENCE":1}]}​\n'
