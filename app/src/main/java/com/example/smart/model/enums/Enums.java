package com.example.smart.model.enums;

public class Enums {

    public enum SHOPPING_HABITS_ENUM {
        SAVER,
        MODERATE,
        QUALITY;
    }

    public enum RECOMMENDATION_ENUM {
        MIN((5 * 60000)),
        HOUR((60 * 60000)),
        DAY((1440 * 60000));

        private long milliseconds;

        RECOMMENDATION_ENUM(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        public long getMilliseconds() {
            return this.milliseconds;
        }
    }

}
