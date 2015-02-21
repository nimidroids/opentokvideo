//create publisherToken...						
Parse.Cloud.define("createPubToken", function(request, response) {
    var ActiveSessions = Parse.Object.extend("ActiveSessions");
    var query = new Parse.Query(ActiveSessions);
    query.equalTo("callerID", request.params.callerId);
    query.first({
      success: function(object) {
	    var sessionId = object.get("sessionID");
		var pubTok = object.get("publisherToken");
	   if(pubTok == undefined){
	    var publisherToken = opentok.generateToken(sessionId, { "role" : opentok.ROLE.PUBLISHER});
         object.set("publisherToken", publisherToken);
         object.save();
		 response.success("created publisherToken");
		 }else{
		  response.success("already exist publisherToken");
		 }
      },
      error: function(error) {
         response.error("Failed to create publisherToken");
      }
    });
  });
