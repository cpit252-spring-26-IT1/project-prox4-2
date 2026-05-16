package sa.edu.kau.fcit.cpit252.project;

import java.time.LocalTime;

public class TimeAccessChecker {

    private static final int BUSINESS_HOURS_START = 7;  
    private static final int BUSINESS_HOURS_END = 23;   

    public boolean isAccessAllowed() {
        int currentHour = LocalTime.now().getHour();
        return currentHour >= BUSINESS_HOURS_START && currentHour < BUSINESS_HOURS_END;
    }

    public String getAccessWindow() {
        return BUSINESS_HOURS_START + ":00 - " + BUSINESS_HOURS_END + ":00";
    }
}