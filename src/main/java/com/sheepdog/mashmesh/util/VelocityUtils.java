package com.sheepdog.mashmesh.util;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.ToolManager;

import java.io.StringWriter;
import java.util.Properties;

public class VelocityUtils {
    private static VelocityEngine engine = null;
    private static ToolManager toolManager = null;

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

    public static VelocityContext getContext() {
        if (toolManager == null) {
            ToolManager newToolManager = new ToolManager();
            newToolManager.configure("WEB-INF/velocity-tools.xml");
            toolManager = newToolManager;
        }

        return new VelocityContext(toolManager.createContext());
    }

    public static String renderTemplateToString(String templatePath, Context context) {
        Template template = getInstance().getTemplate(templatePath);
        StringWriter writer = new StringWriter();
        template.merge(context, writer);
        return writer.toString();
    }
}
