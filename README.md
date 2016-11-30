# up_emotions_audio
This module aims to extract emotions from audio.  The input argument is either an uploaded audio/video file to the server or a URL. The output is the predicted emotion in terms of Arousal and Valence within the JSON-LD format.

- change the content of the 'rest_vars' pointing to 'classifiers' directory and an empty 'download' directory.

- define the path to the 'rest_vars' in the er/src/com/opensmile/maven/path.java as 'var_file' value.

- change the directory of 'weka' in the 'classifiers/RF_models/run_*.sh'

- if using your own asr, change the bash commands in 'classifiers/asr/*.sh' file to your own asr service.
