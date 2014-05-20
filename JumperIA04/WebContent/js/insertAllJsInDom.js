/**
 * Load JS library file needed by OMNI player
 * @function
 * @global
 */

var jsList = new Array("init.js","constants.js","Sender.js");

var libs=["jquery-min_1-10-2.js","less.js"];
		
for (var i = 0, len = libs.length; i < len; i++) {
	document.write("<script type='text/javascript' src='lib/" + libs[i] + "'></script>");
}
// Build dos command for compile all js file in omni.js (without debug and info logs)
// and omni-min.js (minimize/obfuscate)
var cmdSuppDebugDeb = 'findstr /v "Log.debug Log.info console.log" "';
var cmdSuppDebugFin = '" >> ..\\lib\\omni.js';
var tmpSuppDebug = 'del /f ..\\lib\\omni.tmp\n';

var cmdCompilDeb = 'java -jar zz_DEV_zz_compiler.jar';
var cmdCompilFin = ' --js_output_file=..\\lib\\omni-min.tmp --compilation_level=SIMPLE_OPTIMIZATIONS'; //ADVANCED_OPTIMIZATIONS

for (var i = 0, len = jsList.length; i < len; i++) {
	if (jsList[i] != "utils/Log.js") {
		tmpSuppDebug +=  "type " + jsList[i].replace("/","\\") + " >> ..\\lib\\omni.tmp\n";
	}
	document.write("<script type='text/javascript' src='js/" + jsList[i] + "'></script>");
}