package com.sheepdog.mashmesh.util;

import org.apache.velocity.app.VelocityEngine;

public class VelocityConfiguration {
    private static VelocityEngine engine = null;

    public static VelocityEngine getInstance() {
        if (engine == null) {
            VelocityEngine newEngine = new VelocityEngine();
            engine = newEngine;
        }

        return engine;
    }
}
