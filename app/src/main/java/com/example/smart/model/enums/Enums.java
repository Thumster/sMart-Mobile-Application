package com.example.smart.model.enums;

public class Enums {

    public enum SHOPPING_HABITS_ENUM {
        SAVER,
        MODERATE,
        QUALITY;
    }

    public enum RECOMMENDATION_ENUM {
        ALL(0),
        MIN((5 * 60000)),
        HOUR((60 * 60000)),
        DAY((1440 * 60000)),
        OFF(Long.MAX_VALUE);

        private long milliseconds;

        RECOMMENDATION_ENUM(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        public long getMilliseconds() {
            return this.milliseconds;
        }
    }

}
