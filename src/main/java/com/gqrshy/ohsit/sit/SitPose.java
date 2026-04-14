package com.gqrshy.ohsit.sit;

public enum SitPose {
    NORMAL("sitnormal"),
    RELAXED("sitchill"),
    CURLED("curledsit"),
    ON_KNEES("onknees"),
    LEGS_TO_SIDE("legstoside"),
    CRISSCROSS("cross");

    private final String animationName;

    SitPose(String animationName) {
        this.animationName = animationName;
    }

    public String getAnimationName() {
        return animationName;
    }
}
