/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

#ifndef SHARE_JFR_METADATA_JFRSERIALIZER_HPP
#define SHARE_JFR_METADATA_JFRSERIALIZER_HPP

#include "memory/allocation.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "jfrfiles/jfrTypes.hpp"

/*
 * A "type" in Jfr is a binary relation defined by enumerating a set of <key, value> ordered pairs:
 *
 * { <1, myvalue>, <2, mysecondvalue>, ... }
 *
 * The key should be a type relative unique id. A value is an instance of the type.
 *
 * By defining and registering a type, keys can be written to event fields and the
 * framework will maintain the mapping to the corresponding value (if you register as below).
 *
 * Inherit JfrSerializer, create a CHeapObj instance and then use JfrSerializer::register_serializer(...) to register.
 * Once registered, the ownership of the serializer instance is transferred to Jfr.
 *
 * How to register:
 *
 * bool register_serializer(JfrTypeId id, bool require_safepoint, bool permit_cache, JfrSerializer* serializer)
 *
 * The type identifiers are machine generated into an enum located in jfrfiles/jfrTypes.hpp (included).
 *
 *  enum JfrTypeId {
 *    ...
 *    TYPE_THREADGROUP,
 *    TYPE_CLASSLOADER,
 *    TYPE_METHOD,
 *    TYPE_SYMBOL,
 *    TYPE_THREADSTATE,
 *    TYPE_INFLATECAUSE,
 *    ...
 *
 * id                 this is the id of the type your are defining (see the enum above).
 * require_safepoint  indicate if your type need to be evaluated and serialized under a safepoint.
 * permit_cache       indicate if your type constants are stable to be cached.
 *                    (implies the callback is invoked only once and the contents will be cached. Set this to true for static information).
 * serializer         the serializer instance.
 *
 * See below for guidance about how to implement serialize().
 *
 */
class JfrSerializer : public CHeapObj<mtTracing> {
 public:
  virtual ~JfrSerializer() {}
  virtual void on_rotation() {}
  static bool register_serializer(JfrTypeId id, bool permit_cache, JfrSerializer* serializer);
  virtual void serialize(JfrCheckpointWriter& writer) = 0;
};

/*
 * Defining serialize(JfrCheckpointWriter& writer):
 *
 *  Invoke writer.write_count(N) for the number of ordered pairs (cardinality) to be defined.
 *
 *  You then write each individual ordered pair, <key, value> ...
 *
 *  Here is a simple example, describing a type defining string constants:
 *
 *  void MyType::serialize(JfrCheckpointWriter& writer) {
 *    const int nof_causes = ObjectSynchronizer::inflate_cause_nof;
 *    writer.write_count(nof_causes);                           // write number of ordered pairs (mappings) to follow
 *    for (int i = 0; i < nof_causes; i++) {
 *      writer.write_key(i);                                    // write key
 *      writer.write(ObjectSynchronizer::inflate_cause_name((ObjectSynchronizer::InflateCause)i)); // write value
 *    }
 *  }
 *
 * Note that values can be complex, and can also referer to other types.
 *
 * Please see jfr/recorder/checkpoint/types/jfrType.cpp for reference.
 */

#endif // SHARE_JFR_METADATA_JFRSERIALIZER_HPP
