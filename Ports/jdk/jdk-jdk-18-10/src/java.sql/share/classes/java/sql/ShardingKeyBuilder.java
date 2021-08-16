/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package java.sql;

/**
 * A builder created from a {@code DataSource}  or {@code XADataSource} object,
 * used to create a {@link ShardingKey} with sub-keys of supported data types.
 * Implementations must
 * support JDBCType.VARCHAR and  may also support additional data types.
 * <p>
 * The following example illustrates the use of {@code ShardingKeyBuilder} to
 * create a {@link ShardingKey}:
 * <pre>
 * {@code
 *
 *     DataSource ds = new MyDataSource();
 *     ShardingKey shardingKey = ds.createShardingKeyBuilder()
 *                           .subkey("abc", JDBCType.VARCHAR)
 *                           .subkey(94002, JDBCType.INTEGER)
 *                           .build();
 * }
 * </pre>
 *
 * @since 9
 */
public interface ShardingKeyBuilder {

    /**
     * This method will be called to add a subkey into a Sharding Key object being
     * built. The order in which subkey method is called is important as it
     * indicates the order of placement of the subkey within the Sharding Key.
     *
     * @param subkey contains the object that needs to be part of shard sub key
     * @param subkeyType sub-key data type of type java.sql.SQLType
     * @return this builder object
     */
    ShardingKeyBuilder subkey(Object subkey, SQLType subkeyType);

    /**
     * Returns an instance of the object defined by this builder.
     *
     * @return The built object
     * @throws java.sql.SQLException If an error occurs building the object
     */
    ShardingKey build() throws SQLException;
}
