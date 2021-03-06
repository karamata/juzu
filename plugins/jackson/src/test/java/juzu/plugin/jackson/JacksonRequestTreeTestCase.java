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

package juzu.plugin.jackson;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.util.Iterator;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class JacksonRequestTreeTestCase extends AbstractJacksonRequestTestCase {

  @Deployment(testable = false)
  public static WebArchive createDeployment() {
    return createServletDeployment(true, "plugin.jackson.request.node");
  }

  @Override
  public void testRequest() throws Exception {
    super.testRequest();
    ObjectNode node = assertInstanceOf(ObjectNode.class, payload);
    Iterator<String> fields = node.fieldNames();
    assertEquals("foo", fields.next());
    assertFalse(fields.hasNext());
    TextNode text = assertInstanceOf(TextNode.class, node.get("foo"));
    assertEquals("bar", text.textValue());
  }
}
