package com.philips.lighting;

public interface Scheduler {
	
	public boolean isCarryOverEndTimeOneDay();
	
	public void setLightOffTimerSchedule(String startTimeFormatted, String endTimeFormatted);
	
	public boolean isWithinSchedule();
	
}
