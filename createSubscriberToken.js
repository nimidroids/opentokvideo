/create subscriberToken Method Using in app
//create subscriberToken...                      
Parse.Cloud.define("createSubToken", function(request, response) {
    var ActiveSessions = Parse.Object.extend("ActiveSessions");
    var query = new Parse.Query(ActiveSessions);
    query.equalTo("callerID", request.params.callerId);
	query.equalTo("receiverID", request.params.calleeId);
    query.first({
      success: function(object) {
        var sessionId = object.get("sessionID");
        var subTok = object.get("subscriberToken");
       if(subTok == undefined){
        var subscriberToken = opentok.generateToken(sessionId, { "role" : opentok.ROLE.PUBLISHER});
         object.set("subscriberToken", subscriberToken);
         object.save();
         response.success("created subscriberToken.");
         }else{
          response.success("already exist subscriberToken.");
         }
      },
      error: function(error) {
         response.error("Failed to create subscriberToken.");
      }
    });
  });
  
