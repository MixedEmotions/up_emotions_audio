#!/bin/bash
export CLASSPATH=$CLASSPATH:/path/to/weka-3-8-0/weka.jar
NEW_UUID=$(date | md5sum | awk '{ print substr( $0, 1, 10 ) }')'g.arff'
#NEW_UUID=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 12 | head -n 1)'.arff'
./SMILExtract -C IS09_emotion_gender.conf -I $1 -l 0 -classlabel Female -O $NEW_UUID
res=$(java weka.classifiers.trees.RandomForest -T  $NEW_UUID -l gender_rf.model -classifications weka.classifiers.evaluation.output.prediction.PlainText | grep ':' |tr -s ' '|sed -r 's/^ //g'|cut -d' ' -f3|cut -d':' -f2)
#| sed -r 's/^/class=/g'
male='Male'
if [ "$res" = "$male" ];
then
printf '{"PROCESSOR":"OpenSMILE","ORIGIN":"libsvm","TYPE":"classification","COMPONENT":"mysvmsink","VIDX":1,"VALUE":"Male","PROB":[{"CLASS_IDX":0,"CLASS_NAME":"Male","CLASS_PROB":1.0},{"CLASS_IDX":1,"CLASS_NAME":"Female","CLASS_PROB":0.0},],}​\n'
fi 
female='Female'
if [ "$res" = "$female" ];
then
printf '{"PROCESSOR":"OpenSMILE","ORIGIN":"libsvm","TYPE":"classification","COMPONENT":"mysvmsink","VIDX":1,"VALUE":"Female","PROB":[{"CLASS_IDX":0,"CLASS_NAME":"Male","CLASS_PROB":0.0},{"CLASS_IDX":1,"CLASS_NAME":"Female","CLASS_PROB":1.0},],}​\n'
fi
rm $NEW_UUID

