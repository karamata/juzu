/*
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
package juzu.plugin.amd;

import juzu.asset.AssetLocation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a JavaScript managed module.
 *
 * @author <a href="mailto:haithanh0809@gmail.com">Nguyen Thanh Hai</a>
 * @version $Id$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Module {

  /**
   * @return the asset id, used for referencing this asset, the value is optional.
   */
  String id() default "";

  /**
   * @return the value for resolving the asset
   */
  String value();

  /**
   * @return the asset dependencies, i.e the asset that are needed by this asset.
   */
  String[] depends() default {};

  /**
   * @return the asset location
   */
  AssetLocation location() default AssetLocation.APPLICATION;

  /**
   * An optional adapter.
   *
   * @return the module adapter
   */
  String adapter() default "";

  /**
   * An optional alias map for {@link #depends()} of the {@link #value()} member.
   *
   * @return the depends alias map
   */
  String[] aliases() default {};

  /**
   * Defines <code>max-age</code> cache control headers for this module asset.
   *
   * @return the max age
   */
  int maxAge() default -1;
}
