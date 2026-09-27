package com.smms.assistance.infrastructure.client;

import java.time.LocalTime;

public class EffectiveScheduleOutput {
    public boolean workDay;
    public String expectedStartTime;
    public String expectedEndTime;
    public Integer graceMinutes;
    public String absenceCutoffTime;
    public String timezone;
    public String shiftType;

    public EffectiveScheduleOutput() {}
}
