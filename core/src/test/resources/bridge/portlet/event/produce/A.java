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

package bridge.portlet.event.produce;

import juzu.Action;
import juzu.EventQueue;
import juzu.Response;
import juzu.View;

import java.io.Serializable;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class A {

  @Action
  public Response.View action(EventQueue<Serializable> producer) {
    producer.send("the_event", 3);
    return A_.index();
  }

  @View
  public Response.Content index() {
    return Response.ok("<a id='trigger' href='" + A_.action() + "'>click</a>");
  }
}
