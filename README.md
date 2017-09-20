Live Streaming App:
===================

 Core Features:
 ======================
 1) **User Login and Logout:** 
 New User will able to login with credential and also able to logout from the app. 
 2) **Publish Stream:** 
 User will able to go publish stream screen by tapping on the fab button from home page. And then by tapping on the go live button the live streaming will be started and could be stopped by stop live button.
 

 Project Install with Android Studio:
 ====================================
 
If you are using Android studio update version then no need to install git, because this process is included with update android studio version. Then follow the following steps:
1) Just go to the folder in your computer where you want to clone the project.
2) Open Git Bash.
3) Type git clone, and then paste the URL of the project from github 
   git clone https://github.com/webhawksit/live-streaming.git 
4) Press Enter. Your local clone will be created.

**Building using Android Studio:**
1. Open Android Studio and launch the Android SDK manager from it (Tools | Android | SDK Manager)
2. Check that these two components are installed and updated to the latest version. Install or upgrade them if necessary. 
	1.	Android SDK Platform Tools
	2.	Android Support Library
	3.	Google Play Services
	4.	Google Repository
	3.	Return to Android Studio and select Open … from File menu
	4.	Select the LiveStreaming (the cloned project) directory
	5.	 After successful build gradle then run the project with real device.


  Red5 Server configuration:
  ==========================
### Software Dependencies
	Need to download and install Java SE Runtime Envrionment (JRE) - recommended version 8 or higher - to run Red5 Pro server.After installing java, add a new system variable for JAVA_HOME. Find the installation folder for your Java installation, then go to System Properties, Advanced System Settings, Environment Variables. Under System variables, click on New....

    Variable name: JAVA_HOME
    Variable path: (will be something like) C:\Program Files (x86)\Java\jre1.8.0_66
	
### Red5 Pro With WebRTC
	
We recommend running WebRTC on linux, due to CPU and memory requirements. Please see Installing Red5 Pro on an Ubuntu Linux Server. If you want to run Red5 Pro WebRTC on your windows desktop for development purposes, you will need to install Microsoft Visual Studio redistributables if you don't have Visual Studio on your machine.
	
### Red5 Pro Installation
To install the Red5 Pro Server:

1. Download the server .zip distribution to your local machine. Make sure to login with your account on http://account.red5pro.com and download the server from https://account.red5pro.com/download.
2. Copy the server .zip distribution into the directory you wish to run Red5 Pro from
3. Unzip the Red5 Pro distribution in the directory
4. For ease of use, rename the Red5 Pro distribution directory to red5pro.
5. Keep unchanged the \red5pro-server-3.2.0.b154-release\conf\red5.properties
6. Change on \red5pro-server-3.2.0.b154-release\webapps\api\WEB-INF\red5-web.properties set security.accessToken=xxxxx, your api access token.
7. Change on \red5pro-server-3.2.0.b154-release\webapps\api\WEB-INF\security\hosts.txt set
		localhost
		*
		~
	
	If you have a Professional License Key

    Note: as of release 3.0.0 your LICENSE.KEY file will be included in your server download.

	If you have purchased a Red5 Pro Professional license from http://account.red5pro.com, 
	then you need to add it to the root of the Red5 Pro Server installation. 
	Create a new text file named LICENSE.KEY, and type or copy/paste your 16-digit license code into that file before starting Red5 Pro.

### Add execute permissions to the following files:
		
   red5.bat
   red5-shutdown.bat

### Security
	
The following Inbound ports need to be open on your server/firewall for Red5 Pro features to work:
	
Port 	Description
22 		SSH
5080 	default web access of Red5
1935 	default Red5 RTMP port
8554 	default RTSP port
6262 	websockets (for second screen and HLS)
8088 	second screen client registry
3939 	Windows Remote Desktop (required for cloud hosted management)

Ports required for WebRTC server using SSL:
	
Port 	Description
22 		SSH
80 		modified web access of Red5
443 	standard HTTPS port
1935 	default Red5 RTMP port
8554 	default RTSP port
6262 	websockets for HLS
8081 	websockets for WebRTC
8083 	secure websockets for WebRTC
