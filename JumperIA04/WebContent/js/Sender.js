// --------------------------------------------------------------
// --------------- Sender CLASS ----------------
// --------------------------------------------------------------

Sender = function() {
};
Sender.prototype = new Object();
Sender.prototype.constructor = Sender;

Sender.prototype.send = function(RequestType,RequestData,ServerAdress,ServerPort,ServerRessource) {
	$.ajax({
    type: RequestType,
    dataType: "json",
    contentType: "application/json",
    data : { foo : 'bar', bar : 'foo' },
    url: "http://"+ServerAdress+":"+ServerPort+"/"+ServerRessource,
	}).always(function(data) {
		console.log("Réponse de requete "+RequestType+" : "+data.responseText);
	})
	
};

