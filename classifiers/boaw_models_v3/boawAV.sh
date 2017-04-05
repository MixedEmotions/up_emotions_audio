#!/bin/bash
# Arousal and valence prediction using bag-of-audio-words
# January 2017, Chair of Complex and Intelligent Systems, University of Passau

wavfile=../m2_W.wav

#Linux
./SMILExtract -C "mfcc_energy.conf" -logfile "smile.log" -I $wavfile -instname $wavfile -csvoutput "LLD.csv" -l 1
#Windows
#./SMILExtract.exe -C "mfcc_energy.conf" -logfile "smile.log" -I $wavfile -instname $wavfile -csvoutput "LLD.csv" -l 1

java -jar openXBOW.jar -i LLD.csv -attributes nt1[13] -o boaw.libsvm -norm 1 -b codebook &>/dev/null
./predict boaw.libsvm modelArousal.svr arousal.txt &>/dev/null
./predict boaw.libsvm modelValence.svr valence.txt &>/dev/null

cat arousal.txt
echo 
cat valence.txt

# Training parameters
# openXBOW: Codebook size 200, a=1
#./train -s 12 -B 1 -c 1e-4 trainArousal.libsvm modelArousal.svr
#./train -s 12 -B 1 -c 5e-4 trainValence.libsvm modelValence.svr
