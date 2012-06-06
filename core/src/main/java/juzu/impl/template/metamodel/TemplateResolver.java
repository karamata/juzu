/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.impl.template.metamodel;

import juzu.impl.application.ApplicationContext;
import juzu.impl.application.metamodel.ApplicationMetaModel;
import juzu.impl.compiler.BaseProcessor;
import juzu.impl.compiler.CompilationException;
import juzu.impl.compiler.ElementHandle;
import juzu.impl.compiler.ProcessingContext;
import juzu.impl.controller.metamodel.ControllerMetaModel;
import juzu.impl.controller.metamodel.ControllerMethodMetaModel;
import juzu.impl.controller.metamodel.ParameterMetaModel;
import juzu.impl.inject.Export;
import juzu.impl.spi.template.EmitContext;
import juzu.impl.spi.template.TemplateProvider;
import juzu.impl.spi.template.Template;
import juzu.impl.template.metadata.TemplateDescriptor;
import juzu.impl.utils.Content;
import juzu.impl.utils.FQN;
import juzu.impl.utils.Logger;
import juzu.impl.utils.MethodInvocation;
import juzu.impl.utils.Path;
import juzu.impl.utils.Tools;

import javax.annotation.Generated;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * The template repository.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
public class TemplateResolver implements Serializable {

  /** . */
  private static final Logger log = BaseProcessor.getLogger(TemplateResolver.class);

  /** . */
  private final ApplicationMetaModel application;

  /** . */
  private Map<Path, Template<?>> templates;

  /** . */
  private Map<Path, FileObject> resourceCache;

  /** . */
  private Map<FQN, FileObject> stubCache;

  /** . */
  private Map<FQN, FileObject> classCache;

  public TemplateResolver(ApplicationMetaModel application) {
    if (application == null) {
      throw new NullPointerException();
    }

    //
    this.application = application;
    this.templates = new HashMap<Path, Template<?>>();
    this.resourceCache = new HashMap<Path, FileObject>();
    this.stubCache = new HashMap<FQN, FileObject>();
    this.classCache = new HashMap<FQN, FileObject>();
  }

  public Collection<Template<?>> getTemplates() {
    return templates.values();
  }

  public void removeTemplate(Path path) {
    // Shall we do something else ?
    templates.remove(path);
  }

  public void prePassivate() {
    log.log("Evicting cache " + resourceCache.keySet());
    resourceCache.clear();
    stubCache.clear();
    classCache.clear();
  }

  public void process(TemplateMetaModelPlugin plugin, ProcessingContext context) throws CompilationException {
    // Evict templates that are out of date
    log.log("Synchronizing existing templates " + templates.keySet());
    for (Iterator<Template<?>> i = templates.values().iterator();i.hasNext();) {
      Template<?> template = i.next();
      Path.Absolute absolute = application.getTemplates().resolve(template.getPath());
      Content content = context.resolveResource(application.getHandle(), absolute);
      if (content == null) {
        // That will generate a template not found error
        i.remove();
        log.log("Detected template removal " + template.getPath());
      }
      else if (content.getLastModified() > template.getLastModified()) {
        // That will force the regeneration of the template
        i.remove();
        log.log("Detected stale template " + template.getPath());
      }
      else {
        log.log("Template " + template.getPath() + " is valid");
      }
    }

    // Build missing templates
    log.log("Building missing templates");
    Map<Path, Template<?>> copy = new HashMap<Path, Template<?>>(templates);
    for (TemplateMetaModel templateMeta : application.getTemplates()) {
      Template<?> template = copy.get(templateMeta.getPath());
      if (template == null) {
        log.log("Compiling template " + templateMeta.getPath());
        ModelTemplateProcessContext compiler = new ModelTemplateProcessContext(templateMeta, new HashMap<Path, Template<?>>(copy), context);
        Collection<Template<?>> resolved = compiler.resolve(templateMeta);
        for (Template<?> added : resolved) {
          copy.put(added.getPath(), added);
        }
      }
    }
    templates = copy;

    // Generate missing files from template
    for (Template<?> template : templates.values()) {
      //
      Path originPath = template.getOriginPath();
      TemplateMetaModel templateMeta = application.getTemplates().get(originPath);

      //
      // We compute the class elements from the field elements (as eclipse will make the relationship)
      Set<FQN> types = new LinkedHashSet<FQN>();
      for (TemplateRefMetaModel ref : templateMeta.getRefs()) {
        ElementHandle.Field handle = ref.getHandle();
        types.add(handle.getFQN());
      }
      final Element[] elements = new Element[types.size()];
      int index = 0;
      for (FQN type : types) {
        elements[index++] = context.getTypeElement(type.getName());
      }

      // Resolve the stub
      resolveStub(template, plugin, context, elements);

      // Resolve the qualified class
      resolvedQualified(template, context, elements);

      //
      resolveScript(template, plugin, context, elements);
    }
  }

  private <A extends Serializable> void resolveScript(final Template<A> template, final TemplateMetaModelPlugin plugin, final ProcessingContext context, final Element[] elements) {
    context.executeWithin(elements[0], new Callable<Void>() {
      public Void call() throws Exception {

        // If CCE that would mean there is an internal bug
        TemplateProvider<A> provider = (TemplateProvider<A>)plugin.providers.get(template.getPath().getExt());

        // If it's the cache we do nothing
        if (!resourceCache.containsKey(template.getPath())) {
          //
          Writer writer = null;
          try {
            A ast = template.getAST();
            EmitContext emitCtx = new EmitContext() {
              @Override
              public MethodInvocation resolveMethodInvocation(String typeName, String methodName, Map<String, String> parameterMap) throws CompilationException {
                ControllerMethodMetaModel method = application.getControllers().resolve(typeName, methodName, parameterMap.keySet());

                //
                if (method == null) {
                  throw ControllerMetaModel.CONTROLLER_METHOD_NOT_RESOLVED.failure(methodName + "(" + parameterMap + ")");
                }

                //
                List<String> args = new ArrayList<String>();
                for (ParameterMetaModel param : method.getParameters()) {
                  String value = parameterMap.get(param.getName());
                  args.add(value);
                }
                return new MethodInvocation(method.getController().getHandle().getFQN().getName() + "_", method.getName() + "URL", args);
              }
            };

            //
            CharSequence res = provider.emit(emitCtx, ast);

            //
            Path.Absolute absolute = application.getTemplates().resolve(template.getPath());
            FileObject scriptFile = context.createResource(StandardLocation.CLASS_OUTPUT, absolute.as(provider.getTargetExtension()), elements);
            writer = scriptFile.openWriter();
            writer.write(res.toString());

            // Put it in cache
            resourceCache.put(template.getPath(), scriptFile);

            //
            log.log("Generated template script " + template.getPath() + " as " + scriptFile.toUri() +
              " with originating elements " + Arrays.asList(elements));
          }
          catch (IOException e) {
            throw TemplateMetaModel.CANNOT_WRITE_TEMPLATE_SCRIPT.failure(e, template.getPath());
          }
          finally {
            Tools.safeClose(writer);
          }
        }
        else {
          log.log("Template " + template.getPath() + " was found in cache");
        }

        //
        return null;
      }
    });
  }

  private <A extends Serializable> void resolvedQualified(Template<A> template, ProcessingContext context, Element[] elements) {
    Path path = template.getPath();
    Path.Absolute absolute = application.getTemplates().resolve(path);
    if (classCache.containsKey(path.getFQN())) {
      log.log("Template class " + path + " was found in cache");
      return;
    }

    //
    Writer writer = null;
    try {
      // Template qualified class
      FileObject classFile = context.createSourceFile(absolute.getFQN(), elements);
      writer = classFile.openWriter();
      writer.append("package ").append(absolute.getQN()).append(";\n");
      writer.append("import ").append(Tools.getImport(juzu.Path.class)).append(";\n");
      writer.append("import ").append(Tools.getImport(Export.class)).append(";\n");
      writer.append("import ").append(Tools.getImport(Generated.class)).append(";\n");
      writer.append("import ").append(Tools.getImport(TemplateDescriptor.class)).append(";\n");
      writer.append("import javax.inject.Inject;\n");
      writer.append("import ").append(Tools.getImport(ApplicationContext.class)).append(";\n");
      writer.append("@Generated({})\n");
      writer.append("@Export\n");
      writer.append("@Path(\"").append(path.getValue()).append("\")\n");
      writer.append("public class ").append(path.getRawName()).append(" extends ").append(juzu.template.Template.class.getName()).append("\n");
      writer.append("{\n");
      writer.append("@Inject\n");
      writer.append("public ").append(path.getRawName()).append("(").
        append(ApplicationContext.class.getSimpleName()).append(" applicationContext").
        append(")\n");
      writer.append("{\n");
      writer.append("super(applicationContext, \"").append(path.getValue()).append("\");\n");
      writer.append("}\n");

      //
      writer.append("public static final TemplateDescriptor DESCRIPTOR = new TemplateDescriptor(").append(absolute.getFQN().getName()).append(".class);\n");

      //
      String baseBuilderName = Tools.getImport(juzu.template.Template.Builder.class);
      if (template.getParameters() != null) {
        // Implement abstract method with this class Builder covariant return type
        writer.append("public Builder with() {\n");
        writer.append("return new Builder();\n");
        writer.append("}\n");

        // Setters on builders
        writer.append("public class Builder extends ").append(baseBuilderName).append("\n");
        writer.append("{\n");
        for (String paramName : template.getParameters()) {
          writer.append("public Builder ").append(paramName).append("(Object ").append(paramName).append(") {\n");
          writer.append("set(\"").append(paramName).append("\",").append(paramName).append(");\n");
          writer.append("return this;\n");
          writer.append(("}\n"));
        }
        writer.append("}\n");
      }
      else {
        // Implement abstract method
        writer.append("public ").append(baseBuilderName).append(" with() {\n");
        writer.append("return new ").append(baseBuilderName).append("();\n");
        writer.append("}\n");
      }

      // Close class
      writer.append("}\n");

      //
      classCache.put(path.getFQN(), classFile);

      //
      log.log("Generated template class " + path + " as " + classFile.toUri() +
        " with originating elements " + Arrays.asList(elements));
    }
    catch (IOException e) {
      throw TemplateMetaModel.CANNOT_WRITE_TEMPLATE_CLASS.failure(e, elements[0], path);
    }
    finally {
      Tools.safeClose(writer);
    }
  }

  private void resolveStub(Template<?> template, TemplateMetaModelPlugin plugin, ProcessingContext context, Element[] elements) {
    if (stubCache.containsKey(template.getPath().getFQN())) {
      log.log("Template strub " + template.getPath().getFQN() + " was found in cache");
      return;
    }

    //
    Path absolute = application.getTemplates().resolve(template.getPath());

    //
    FQN stubFQN = new FQN(absolute.getFQN().getName() + "_");
    TemplateProvider provider = plugin.providers.get(template.getPath().getExt());
    Writer writer = null;
    try {
      // Template stub
      JavaFileObject stubFile = context.createSourceFile(stubFQN, elements);
      writer = stubFile.openWriter();
      writer.append("package ").append(stubFQN.getPackageName()).append(";\n");
      writer.append("import ").append(Tools.getImport(Generated.class)).append(";\n");
      writer.append("@Generated({\"").append(stubFQN.getName()).append("\"})\n");
      writer.append("public class ").append(stubFQN.getSimpleName()).append(" extends ").append(provider.getTemplateStubType().getName()).append(" {\n");
      writer.append("}");

      //
      stubCache.put(template.getPath().getFQN(), stubFile);

      //
      log.log("Generating template stub " + stubFQN.getName() + " as " + stubFile.toUri() +
        " with originating elements " + Arrays.asList(elements));
    }
    catch (IOException e) {
      throw TemplateMetaModel.CANNOT_WRITE_TEMPLATE_STUB.failure(e, elements[0], template.getPath());
    }
    finally {
      Tools.safeClose(writer);
    }
  }
}