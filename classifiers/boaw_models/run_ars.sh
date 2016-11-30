#!/bin/bash
# Arousal and valence prediction using bag-of-audio-words

wavfile=$1 #'/media/sag/DATA/workPassau/Projects/MixedEmotions/Databases/Youtube_data_package/data/Audio/video1(00h00m27s-00h01m01s).wav'
interval=0.04     # hop size

#./SMILExtract -C "mfcc_energy.conf" -logfile "smile.log" -I $wavfile -instname $wavfile -csvoutput "LLD.csv" -l 1
#./SMILExtract -C "mfcc_energy.conf" -logfile "smile.log" -I $wavfile -instname $wavfile -csvoutput "LLDval.csv" -l 1

#java -jar openWord.jar -i LLDval.csv -o boaw.libsvm -a 20 -size 1000 -t 10.0 $interval -b bookValence &>/dev/null
#./predict boaw.libsvm modelValence.svr valence.txt &>/dev/null
NEW_UUID='LLD'$(date | md5sum | awk '{ print substr( $0, 1, 10 ) }')'a'
./SMILExtract -C "mfcc_energy.conf" -logfile "smile.log" -I $wavfile -instname $wavfile -csvoutput ${NEW_UUID}.csv -l 1

java -jar openXBOW.jar -i ${NEW_UUID}.csv -attributes nt1111111111111 -o boaw.libsvm -a 10 -norm 1 -b book &>/dev/null
./predict boaw.libsvm modelArousal.svr ${NEW_UUID}arousal.txt &>/dev/null
#cat valence.txt | awk '{ sum += $1; sum2+=$1*$1; n++ } END { if (n > 0) print sum/n , 1-((sum2/n)-(sum/n*sum/n))^.5; }' > Vres.txt

input=${NEW_UUID}arousal.txt

i=1
while read mean 
do
#  if [[ $std == *"nan"* ]]
#  then
#    std=1;
#  fi
a=`awk "BEGIN{printf \"%.3f\",$mean}"`
printf '​{ "PROCESSOR":"OpenSMILE","ORIGIN":"boaw","TYPE":"regression","COMPONENT":"maxboaw2","VIDX":'$i',"VALUE":'$a',"PROB":[{"CONFIDENCE":1}]}​\n'
done < "$input"

rm ${NEW_UUID}arousal.txt ${NEW_UUID}.csv
