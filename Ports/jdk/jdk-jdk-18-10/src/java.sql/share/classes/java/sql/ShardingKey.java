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
 * Interface used to indicate that this object represents a Sharding Key. A
 * {@code ShardingKey} instance is only guaranteed to be compatible with the
 * data source instance that it was derived from. A {@code ShardingKey} is
 * created using {@link ShardingKeyBuilder}.
 * <p>
 * The following example illustrates the use of {@link ShardingKeyBuilder} to
 * create a {@code ShardingKey}:
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
 * <p>
 *
 * A {@code ShardingKey} may also be used for specifying a
 * {@code superShardingKey}. Databases that support composite Sharding may use a
 * {@code superShardingKey} to specify a additional level of partitioning within
 * the Shard.
 * <p>
 * The following example illustrates the use of {@link ShardingKeyBuilder} to
 * create a {@code superShardingKey} for an eastern region with a
 * {@code ShardingKey} specified for the Pittsburgh branch office:
 * <pre>
 * {@code
 *
 *     DataSource ds = new MyDataSource();
 *     ShardingKey superShardingKey = ds.createShardingKeyBuilder()
 *                           .subkey("EASTERN_REGION", JDBCType.VARCHAR)
 *                           .build();
 *     ShardingKey shardingKey = ds.createShardingKeyBuilder()
 *                           .subkey("PITTSBURGH_BRANCH", JDBCType.VARCHAR)
 *                           .build();
 *     Connection con = ds.createConnectionBuilder()
 *                           .superShardingKey(superShardingKey)
 *                           .shardingKey(shardingKey)
 *                           .build();
 * }
 * </pre>
 *
 * @since 9
 */
public interface ShardingKey {

}
