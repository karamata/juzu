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

package plugin.asset.inheritance;

import juzu.Response;
import juzu.View;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class A {
  @View
  public Response.Content index() {
    String content = "" +
      "<script>\n" +
      "$(function() {\n" +
      "  $('#trigger').click(function() {\n" +
      "    alert(\"OK MEN\");\n" +
      "  });\n" +
      "});\n" +
      "</script>\n" +
      "<a id='trigger' href='#'>click</a>";
    return Response.ok(content).withAssets("main.css").withAssets("jquery.js");
  }
}
