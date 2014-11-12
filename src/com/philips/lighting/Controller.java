package com.philips.lighting;

import java.util.List;
import java.util.Random;

import com.philips.lighting.data.HueProperties;
import com.philips.lighting.gui.AccessPointList;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResourcesCache;
import com.philips.lighting.model.PHHueParsingError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class Controller {

	private PHHueSDK phHueSDK;

	private static final int MAX_HUE=65535;
	private Controller instance;

	private boolean killSwitchOn = true;

	private LightsOffScheduler los;
	
	boolean lightOffSchedulerIsActive = HueProperties.getLightOffSchedulerIsActive();

	public Controller() {
		this.phHueSDK = PHHueSDK.getInstance();
		this.instance = this;

		System.out.println(connectToLastKnownAccessPoint());
		HueProperties.loadProperties();
    	boolean x = HueProperties.getLightOffSchedulerIsActive();
    	System.out.println(x + "!!!!!!!!!");
	}

	public void findBridges() {
		phHueSDK = PHHueSDK.getInstance();
		PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
		sm.search(true, true);
	}

	private PHSDKListener listener = new PHSDKListener() {

		@Override
		public void onAccessPointsFound(List<PHAccessPoint> accessPointsList) {
			//			desktopView.getFindingBridgeProgressBar().setVisible(false);
			AccessPointList accessPointList = new AccessPointList(accessPointsList, instance);
			accessPointList.setVisible(true);
			accessPointList.setLocationRelativeTo(null);  // Centre the AccessPointList Frame
		}

		@Override
		public void onAuthenticationRequired(PHAccessPoint accessPoint) {
			// Start the Pushlink Authentication.
			phHueSDK.startPushlinkAuthentication(accessPoint);

		}

		@Override
		public void onBridgeConnected(PHBridge bridge) {
			phHueSDK.setSelectedBridge(bridge);
//			phHueSDK.enableHeartbeat(bridge, PHHueSDK.HB_INTERVAL);
			phHueSDK.enableHeartbeat(bridge, 7000);
			String username = HueProperties.getUsername();
			String lastIpAddress =  bridge.getResourceCache().getBridgeConfiguration().getIpAddress();   
			System.out.println("On connected: IP " + lastIpAddress);
			HueProperties.storeUsername(username);
			HueProperties.storeLastIPAddress(lastIpAddress);
			HueProperties.storeLightOffSchedulerIsActive(false);
			HueProperties.saveProperties();

			// Update the GUI.
			//			desktopView.getLastConnectedIP().setText(lastIpAddress);
			//			desktopView.getLastUserName().setText(username);

			// Close the PushLink dialog (if it is showing).
			//			if (pushLinkDialog!=null && pushLinkDialog.isShowing()) {
			//				pushLinkDialog.setVisible(false);
			//			}

			// Enable the Buttons/Controls to change the hue bulbs.s
			//			desktopView.getRandomLightsButton().setEnabled(true);
			//			desktopView.getSetLightsButton().setEnabled(true);

		}

		@Override
		public void onCacheUpdated(List<Integer> arg0, PHBridge arg1) {
			System.out.println("Cache Updated");
		}

		@Override
		public void onConnectionLost(PHAccessPoint arg0) {
			System.out.println("Connection Lost!!!");
		}

		@Override
		public void onConnectionResumed(PHBridge arg0) {
			PHBridge bridge = phHueSDK.getSelectedBridge();
			PHBridgeResourcesCache cache = bridge.getResourceCache();
			
			List<PHLight> allLights = cache.getAllLights();
			
			for (PHLight light : allLights) {
				PHLightState lightState = new PHLightState();
				los = new LightsOffScheduler(light, lightState, bridge);
				//				System.out.println("Light Name: " + light.getName());
				System.out.println(light.getLastKnownLightState().getBrightness());
				if(light.getName().equalsIgnoreCase("front_flood") &&
						light.getLastKnownLightState().isReachable()) {
					HueProperties.loadProperties();
					if(los.isWithinSchedule()) {				
						// problem is in this conditional!!!!
						System.out.println("BEFORE!!!!!!!!!______lightOffSchedulerIsActiive: " + HueProperties.getLightOffSchedulerIsActive());
						if(killSwitchOn && !HueProperties.getLightOffSchedulerIsActive() 
								&& light.getLastKnownLightState().isOn()) {
							System.out.println("\nStarting new Thread from los.start()...\n");
							los.start();
							HueProperties.storeLightOffSchedulerIsActive(true);
							System.out.println("SET ACTIVE TO TRUE!!!!!!!!!!!");
							HueProperties.saveProperties();
							System.out.println("lightOffSchedulerIsActiive: " + HueProperties.getLightOffSchedulerIsActive());
						}
					}
				}
				System.out.println("!!!!!" + light.getLastKnownLightState().getBrightness());
				// !!!! 125 is the brightness setting!!! ACCOUNT FOR THIS AND CHANGE IT TO A UNIVERSAL VAR
				if(!light.getLastKnownLightState().isOn() || (lightOffSchedulerIsActive && light.getLastKnownLightState().getBrightness() > 125)) {
					// Physical state of light was turned off and unreachable.
					System.out.println("\nPhysical state of light was turned off and unreachable.\n");
					HueProperties.storeLightOffSchedulerIsActive(false);
					HueProperties.saveProperties();
				}
			}
		}

		@Override
		public void onError(int code, final String message) {

		}

		@Override
		public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {  
			for (PHHueParsingError parsingError: parsingErrorsList) {
				System.out.println("ParsingError : " + parsingError.getMessage());
			}
		} 
	};

	public PHSDKListener getListener() {
		return listener;
	}

	public void setListener(PHSDKListener listener) {
		this.listener = listener;
	}

	public void randomLights() {
		PHBridge bridge = phHueSDK.getSelectedBridge();
		PHBridgeResourcesCache cache = bridge.getResourceCache();

		List<PHLight> allLights = cache.getAllLights();
		Random rand = new Random();

		for (PHLight light : allLights) {
			PHLightState lightState = new PHLightState();
			lightState.setHue(rand.nextInt(MAX_HUE));
			bridge.updateLightState(light, lightState); // If no bridge response is required then use this simpler form.
		}
	}

	/**
	 * Connect to the last known access point.
	 * This method is triggered by the Connect to Bridge button but it can equally be used to automatically connect to a bridge.
	 * 
	 */
	public boolean connectToLastKnownAccessPoint() {

		HueProperties.storeUsername("BQzC0fmj0HZGZsRj");
		HueProperties.storeLastIPAddress("192.168.1.25");
		String username = HueProperties.getUsername();
		String lastIpAddress =  HueProperties.getLastConnectedIP();     
		PHAccessPoint accessPoint = new PHAccessPoint();
		accessPoint.setIpAddress(lastIpAddress);
		accessPoint.setUsername(username);
		phHueSDK.connect(accessPoint);
		return true;
	}

}
