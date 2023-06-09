/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.datasource.filter;

import elki.datasource.bundle.BundleMeta;
import elki.datasource.bundle.MultipleObjectsBundle;

/**
 * Dummy filter that doesn't do any filtering.
 * 
 * Useful for command line parameterization when you have multiple identically
 * named parameters, and want to set the second parameter only. Then you can
 * just use this dummy filter as first parameter.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class NoOpFilter extends AbstractStreamFilter {
  /**
   * Constructor.
   */
  public NoOpFilter() {
    super();
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    return objects;
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    return source.nextEvent();
  }
}