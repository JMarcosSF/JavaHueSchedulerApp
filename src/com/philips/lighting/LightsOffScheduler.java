package com.philips.lighting;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

public class LightsOffScheduler extends Thread implements Scheduler{

	SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
	Date now = null;

	private Date startTimedLightOffTime;
	private Date endTimedLightOffTime;
	private boolean nowUpdated;
	
	PHLight light;
	PHLightState lightState; 
	PHBridge bridge;
	
	public LightsOffScheduler(PHLight light, PHLightState lightState, PHBridge bridge) {
		this.light = light;
		this.lightState = lightState;
		this.bridge = bridge;
		
		try {
			Calendar cal = Calendar.getInstance();
			now = formatter.parse(formatter.format(new Date()));
			cal.setTime(new Date());
			
			
			setLightOffTimerSchedule("23:00", "04:00");
			System.out.println("Now: " + now);
			System.out.println("start time: " + startTimedLightOffTime);
			System.out.println("end time: " + endTimedLightOffTime + "\n");
//			this.start();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public LightsOffScheduler() {
		
		try {
			Calendar cal = Calendar.getInstance();
			now = formatter.parse(formatter.format(new Date()));
			cal.setTime(new Date());
			
			
			setLightOffTimerSchedule("23:00", "04:00");
			System.out.println("Now: " + now);
			if(isCarryOverEndTimeOneDay()) {
				
			}
			System.out.println("start time: " + startTimedLightOffTime);
			System.out.println("end time: " + endTimedLightOffTime);
//			this.start();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean isCarryOverEndTimeOneDay() {
		if(startTimedLightOffTime.after(endTimedLightOffTime)) {
			return true;
		}
		return false;
	}
	
	@Override
	public void setLightOffTimerSchedule(String startTimeFormatted, String endTimeFormatted) {
		try {
			setStartTimedLightOffTimeByFormattedString(startTimeFormatted);
			setEndTimedLightOffTimeByFormattedString(endTimeFormatted);
			Calendar cal = Calendar.getInstance();
			if(isCarryOverEndTimeOneDay()) {
				cal.setTime(endTimedLightOffTime);
				cal.add(Calendar.DATE, 1);
				this.endTimedLightOffTime = cal.getTime();
			}
			Calendar midnightCal = new GregorianCalendar();
			// reset hour, minutes, seconds and millis
			midnightCal.set(Calendar.HOUR_OF_DAY, 0);
			midnightCal.set(Calendar.MINUTE, 0);
			midnightCal.set(Calendar.SECOND, 0);
			midnightCal.set(Calendar.MILLISECOND, 0);
			Date midnight;
			midnight = midnightCal.getTime();
			// If current date and time is after midnight of today, add a day to 
			// representation of now.
			if(new Date().after(midnight) && !nowUpdated) {
				cal.setTime(now);
				cal.add(Calendar.DATE, 1);
				this.now = cal.getTime();
				System.out.println("HERE!!!");
				nowUpdated = true;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void setStartTimedLightOffTimeByFormattedString(String formattedTime)throws ParseException {
		try {
			this.startTimedLightOffTime = this.formatter.parse(formattedTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	public void setEndTimedLightOffTimeByFormattedString(String formattedTime)throws ParseException {
		try {
			this.endTimedLightOffTime = this.formatter.parse(formattedTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		handleTimedLightDimAndOff(light, lightState, bridge);
	}

	@Override
	public boolean isWithinSchedule() {
//		if(now.after(startTimedLightOffTime) && now.before(endTimedLightOffTime)) {
		if(now.after(startTimedLightOffTime) && now.before(endTimedLightOffTime)) {
			System.out.println("\nWithin scheduled time for lights off!!!\n");
			return true;
		}
		return false;
	}

	public void handleTimedLightDimAndOff(PHLight light, PHLightState lightState, PHBridge bridge) {
		try {
			lightState.setBrightness(125);
			bridge.updateLightState(light, lightState);
//			Thread.sleep(1200000);	// delay between dim and light off event.
			Thread.sleep(20000);
			lightState.setOn(false);
			bridge.updateLightState(light, lightState);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
