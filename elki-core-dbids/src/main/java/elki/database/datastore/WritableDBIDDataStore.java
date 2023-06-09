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
package elki.database.datastore;

import elki.database.ids.DBID;
import elki.database.ids.DBIDRef;

/**
 * Data store specialized for doubles. Avoids boxing/unboxing.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
public interface WritableDBIDDataStore extends DBIDDataStore, WritableDataStore<DBID> {
  /**
   * Setter, but using materialized DBIDs.
   * 
   * @deprecated Use {@link #putDBID} instead, to avoid boxing/unboxing cost.
   */
  @Override
  @Deprecated
  DBID put(DBIDRef id, DBID value);

  /**
   * Associates the specified value with the specified id in this storage. If
   * the storage previously contained a value for the id, the previous value is
   * replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   */
  void putDBID(DBIDRef id, DBIDRef value);

  /**
   * Associates the specified value with the specified id in this storage. If
   * the storage previously contained a value for the id, the previous value is
   * replaced by the specified value.
   * 
   * @param id Database ID.
   * @param value Value to store.
   */
  void put(DBIDRef id, DBIDRef value);
}