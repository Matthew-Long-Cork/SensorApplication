As of 30/08/2019
===========================================================================================
code location:  	https://gitlab.com/NimbusDev/stem_iot/umi-android-application

Branches to use: 	myDev - this has all the functionality of the blew branch but the BT is commented out ... my be bit buggy? commecnted the code out in a short time
			blew  - this is the most up to date branch!! NOTE: BT is incomplete in this branch.

Andriod version used:	Android studio 3.4.1 (ref Andriod_studio_version.PNG in Nimbus_sensor_app_documentation folder)
JRE: 			1.8.0_152-release-1343-b01 amd64
===========================================================================================

Run the code with a phone connected via USB as the Andriod emulator is super heavy on the ram and processor so can be very slow.
All Nimbus mods are commected but the Goolge open sources code is 'lightly' commented

Issue locations:

(read main_documentation.doc)
Inside 'Whistlepunk_libary' is where all mods where made:

	Whistlepunk_libary
			 Java
	   		    com.google.android.apps.forscience
	                                                      whistlepunk
									
									...then the 'sensors' folder has the phone sensors
									... 'project' folder has the experiment
									... 'blew' folder is our bluetooth connect work, and the attempt at reading snesors
									      	==> this is where the sensortag issues is!


1. Work to recreate connection to Thingsboard.tec_gateway.com. 
	recover the location from Gary
	or 
	work with him to make a new one


2. Bluetooth. Issues with external SensorTag. Code is located in the following files:
	All of the 'blew' folder
	and any possible links/referneces to other files

	VISUAL IS IN: 	1.ManageDevicesRecyclerFragment.java
			2.fragment_manage_devices.xml:
						2.1.Just remove all the 'COMMENTED OUT BECAUSE BLUETOOTH IS INCOMPLETE' comments
						2.2. in 'android.support.v7.widget.RecyclerView' (in fragment_manage_devices.xml) CHANGE  'android:layout_height="600dp"' TO --> 600dp
			3.License_Activity.java


3. Issue with creating a title for a new experiment. Code is  located in the following files:
												project
	      												experiment
	

	