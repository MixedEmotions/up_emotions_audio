var onFail = function(e) {
	//alert("I am Failed");
	console.log('Rejected!', e);
};

var onSuccess = function(s) {
	//alert("I am Successed");
	var context = new webkitAudioContext();
	var mediaStreamSource = context.createMediaStreamSource(s);
	recorder = new Recorder(mediaStreamSource);
	recorder.record();

	// audio loopback
	// mediaStreamSource.connect(context.destination);
};

function stopRecording() {
	recorder.stop();
	alert("SSSS");
	recorder.exportWAV(function(s) {
		
		audio.src = window.URL.createObjectURL(s);
		alert(audio.src);
	});
}

window.URL = window.URL || window.webkitURL;
navigator.getUserMedia  = navigator.getUserMedia || navigator.webkitGetUserMedia || navigator.mozGetUserMedia || navigator.msGetUserMedia;

var recorder;
var audio = document.querySelector('audio');

function toggleRecording( e ) {
	alert("instop");
    if (e.classList.contains("recording")) {
    	alert("instop");
    	stopRecording();
        e.classList.remove("recording");
        // stop recording
        //audioRecorder.stop();
        //a/udioRecorder.getBuffers( gotBuffers );
        //saveAudio();
    	
    } else {
        // start recording
    	alert("instart");
    	startRecording();
        e.classList.add("recording");
    	/*window.addEventListener('load', initAudio );

    	alert("I am here");
        if (!audioRecorder)
            return;
        audioRecorder.clear();
        audioRecorder.record();*/
    }
}

function startRecording() {
	if (navigator.getUserMedia) {
		navigator.getUserMedia({audio: true}, onSuccess, onFail);
	} else {
		console.log('navigator.getUserMedia not present');
	}
}

