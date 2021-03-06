/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package examples.plugin.bundlegen.impl;

import juzu.impl.request.Request;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Julien Viet
 */
public class BundleResolver {

  public static String resolve(Class<?> bundleClass, String key) {
    Request request = Request.getCurrent();
    Locale locale = request.getUserContext().getLocale();
    ResourceBundle bundle = ResourceBundle.getBundle(bundleClass.getName(), locale, request.getApplication().getClassLoader());
    return bundle != null ? bundle.getString(key) : null;
  }
}
