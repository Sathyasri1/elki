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
package elki.data.model;

import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;
import elki.utilities.io.FormatUtil;

/**
 * Cluster model that stores a prototype for each cluster.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class SimplePrototypeModel<V> implements PrototypeModel<V>, TextWriteable {
  /**
   * Cluster prototype
   */
  protected V prototype;

  /**
   * Constructor with prototype
   * 
   * @param prototype Cluster prototype
   */
  public SimplePrototypeModel(V prototype) {
    super();
    this.prototype = prototype;
  }

  @Override
  public V getPrototype() {
    return prototype;
  }

  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Model class: " + getClass().getName());
    out.commentPrintLn("Cluster " + getPrototypeType() + ": " + prototypeToString());
  }

  /**
   * Format the prototype as string.
   *
   * @return Prototype
   */
  protected String prototypeToString() {
    return (prototype instanceof double[]) ? FormatUtil.format((double[]) prototype) : prototype.toString();
  }

  @Override
  public String getPrototypeType() {
    return "Prototype";
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + prototypeToString() + "]";
  }
}
