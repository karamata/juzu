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

package inject.lifecycle.scoped;

import juzu.SessionScoped;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@SessionScoped
public class Bean {

  /** . */
  public static int construct;

  /** . */
  public static int destroy;

  @PostConstruct
  public void construct() {
    construct++;
  }

  @PreDestroy
  public void destroy() {
    destroy++;
  }

  public void m() {
    // Here just to force a creation since we can have a proxy
  }
}
