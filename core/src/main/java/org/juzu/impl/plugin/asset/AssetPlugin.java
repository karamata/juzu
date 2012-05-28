package org.juzu.impl.plugin.asset;

import org.juzu.asset.AssetLocation;
import org.juzu.impl.asset.AssetMetaData;
import org.juzu.impl.metamodel.MetaModelPlugin;
import org.juzu.impl.plugin.Plugin;
import org.juzu.impl.request.RequestLifeCycle;
import org.juzu.impl.utils.JSON;

import javax.annotation.processing.SupportedAnnotationTypes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@SupportedAnnotationTypes("org.juzu.plugin.asset.Assets")
public class AssetPlugin extends Plugin
{

   public AssetPlugin()
   {
      super("asset");
   }

   @Override
   public MetaModelPlugin newMetaModelPlugin()
   {
      return new AssetMetaModelPlugin();
   }

   @Override
   public AssetDescriptor loadDescriptor(ClassLoader loader, JSON config) throws Exception
   {
      String packageName = config.getString("package");
      List<AssetMetaData> scripts = load(packageName, config.getList("scripts", JSON.class));
      List<AssetMetaData> stylesheets = load(packageName, config.getList("stylesheets", JSON.class));
      return new AssetDescriptor(packageName, scripts, stylesheets);
   }

   private List<AssetMetaData> load(String packageName, List<? extends JSON> scripts)
   {
      List<AssetMetaData> abc = Collections.emptyList();
      if (scripts != null && scripts.size() > 0)
      {
         abc = new ArrayList<AssetMetaData>();
         for (JSON script : scripts)
         {
            String id = script.getString("id");
            AssetLocation location = AssetLocation.safeValueOf(script.getString("location"));

            //
            String value = script.getString("src");
            if (!value.startsWith("/") && location == AssetLocation.CLASSPATH)
            {
               value = "/" + packageName.replace('.', '/') + "/" + value;
            }

            //
            AssetMetaData descriptor = new AssetMetaData(
               id,
               location,
               value,
               script.getArray("depends", String.class)
            );
            abc.add(descriptor);
         }
      }
      return abc;
   }

   @Override
   public Class<? extends RequestLifeCycle> getLifeCycleClass()
   {
      return AssetLifeCycle.class;
   }
}