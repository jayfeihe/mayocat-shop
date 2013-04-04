package org.mayocat.rest.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mayocat.theme.ThemeManager;
import org.mayocat.theme.TemplateNotFoundException;
import org.mayocat.views.Template;
import org.mayocat.views.TemplateEngine;
import org.mayocat.views.TemplateEngineException;
import org.xwiki.component.annotation.Component;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @version $Id$
 */
@Component("frontViewMessageBodyWriter")
public class FrontViewMessageBodyWriter implements MessageBodyWriter<FrontView>, org.mayocat.rest.Provider
{
    @Inject
    private Provider<TemplateEngine> engine;

    @Inject
    private ThemeManager themeManager;

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return FrontView.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(FrontView frontView, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType)
    {
        return -1;
    }

    @Override
    public void writeTo(FrontView frontView, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException
    {
        try {

            Template template = themeManager.resolveIndexTemplate(frontView.getBreakpoint());
            Template layout = null;

            if (frontView.getModel().isPresent()) {
                Optional<String> path = themeManager.resolveModelPath(frontView.getModel().get());
                if (path.isPresent()) {
                    try {
                        layout = themeManager.resolveTemplate(path.get(), frontView.getBreakpoint());
                    } catch (TemplateNotFoundException e) {
                        // Keep going
                    }
                }
                // else just fallback on the default layout
            }

            if (layout == null) {
                layout = themeManager.resolveTemplate(frontView.getLayout() + ".html", frontView.getBreakpoint());
            }

            frontView.getBindings().put("layout", layout.getId());

            ObjectMapper mapper = new ObjectMapper();
            String jsonContext = mapper.writeValueAsString(frontView.getBindings());

            try {
                engine.get().register(layout);
                engine.get().register(template);
                String rendered = engine.get().render(template.getId(), jsonContext);
                entityStream.write(rendered.getBytes());
            } catch (TemplateEngineException e) {
                // Re-serialize the bindings as json with indentation for better debugging
                mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
                Map<String, Object> context = frontView.getBindings();
                jsonContext = mapper.writeValueAsString(context);

                Template error = themeManager.resolveTemplate("error.html", frontView.getBreakpoint());
                Map<String, Object> errorContext = Maps.newHashMap();
                errorContext.put("error", e.getMessage());
                errorContext.put("stackTrace", ExceptionUtils.getStackTrace(e));
                errorContext.put("context", StringEscapeUtils.escapeXml(jsonContext).trim());

                try {
                    engine.get().register(error);
                    String rendered = engine.get().render(error.getId(), mapper.writeValueAsString(errorContext));
                    entityStream.write(rendered.getBytes());
                } catch (TemplateEngineException e1) {
                    throw new RuntimeException(e);
                }
            }
        } catch (TemplateNotFoundException e) {
            throw new WebApplicationException(e);
        }
    }

}