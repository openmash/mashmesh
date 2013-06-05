package com.sheepdog.mashmesh.util;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;

import java.io.StringWriter;
import java.util.Properties;

public class VelocityUtils {
    private static VelocityEngine engine = null;

    public static VelocityEngine getInstance() {
        if (engine == null) {
            Properties properties = new Properties();
            properties.put("resource.loader", "file");
            properties.put("file.resource.loader.path", "WEB-INF/templates");
            properties.put("file.resource.loader.cache", true);

            VelocityEngine newEngine = new VelocityEngine(properties);
            engine = newEngine;
        }

        return engine;
    }

    public static String renderTemplateToString(String templatePath, Context context) {
        Template template = getInstance().getTemplate(templatePath);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
