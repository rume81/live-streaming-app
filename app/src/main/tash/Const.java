package webhawksit.com.livestreaming.utils;

public class Const {

	// red5 server status
	public static final String STREAMING_SERVER_REQUEST_PROTOCOL = "http://";
	public static final String QUERY_STRING_FOR_STREAMING_SERVER_PING = "/api/v1/server/ping?accessToken=";
	public static final String STREAMING_SERVER_ACCESS_TOKEN = "webhawksit123";

	//red5 server communication data
	public static  final  String STREAMING_AND_CHAT_SERVER_ADDRESS = "123.200.14.11"; //"153.126.152.115";  //"123.200.14.11";
	public static  final  int STREAMING_PORT_ADDRESS = 8554;
	public static final int STREAMING_SERVER_PORT = 5080;
	public static  final  String STREAMING_SERVER_APP_NAME = "live";
	public static  final  String RED5_SDK_LICENSE =  "7SUQ-TEXJ-H6KO-XJGZ"; // "4LWV-JZAZ-MWKA-ZF6H";   // Our new:: "7SUQ-TEXJ-H6KO-XJGZ";  // our previous:: 45KB-UA24-PF7Q-2AIU
	public static  final  float STREAMING_BUFFER_TIME = 1.0f;

	//openFire server communication data
	public static  final  int CHAT_SERVER_PORT = 5222;
	public static  final  String CHAT_SERVER_SERVICE_NAME = "webhawksit"; //"153.126.152.115";  // "webhawksit";
	public static  final  String CHAT_SERVER_RESOURCE_NAME = "Android";
	public static  final  String CHAT_ROOM_SERVICE_NAME = "conference.";
	public static  final  String ALTERNATE_CHAT_ROOM_REFERENCE = "demo";

	//not using now
	//stream status from red5 server
	public static final String JSON_URL_FOR_ALL_STREAMS =  "http://153.126.152.115:5080/api/v1/applications/live/streams?accessToken=";    //"http://123.200.14.11:5080/api/v1/applications/live/streams?accessToken=";
	public static final String JSON_URL_FOR_A_STREAMS =   "http://153.126.152.115:5080/api/v1/applications/live/streams/";   //"http://123.200.14.11:5080/api/v1/applications/live/streams/";
}
