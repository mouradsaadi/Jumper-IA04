
$(document).ready(function() {
	var sender = new Sender();
	
	
	$("#buttonGet").on("click",function(){
		data = encodeURIComponent("id_get");
		sender.send(GET_REQUEST,data,"localhost","8182","hello");
	});
	
	$("#buttonPost").on("click",function(){
		data = {url:'lequipe.fr',tag : 'sport'};
		sender.send(POST_REQUEST,data,"localhost","8182","hello");
	});
});
