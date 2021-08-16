/*
 * Copyright (c) 2017, Red Hat, Inc. and/or its affiliates.
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

#include "precompiled.hpp"
#include "memory/universe.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "unittest.hpp"
#include "utilities/globalDefinitions.hpp"

TEST_VM(typeArrayOopDesc, bool_at_put) {
  char mem[100];
  memset(mem, 0, ARRAY_SIZE(mem));

  char* addr = align_up(mem, 16);

  typeArrayOop o = (typeArrayOop) cast_to_oop(addr);
  o->set_klass(Universe::boolArrayKlassObj());
  o->set_length(10);


  ASSERT_EQ((jboolean)0, o->bool_at(0));
  ASSERT_EQ((jboolean)0, o->bool_at(1));
  ASSERT_EQ((jboolean)0, o->bool_at(2));
  ASSERT_EQ((jboolean)0, o->bool_at(3));
  ASSERT_EQ((jboolean)0, o->bool_at(4));
  ASSERT_EQ((jboolean)0, o->bool_at(5));
  ASSERT_EQ((jboolean)0, o->bool_at(6));
  ASSERT_EQ((jboolean)0, o->bool_at(7));

  o->bool_at_put(3, 255); // Check for masking store.
  o->bool_at_put(2, 1);
  o->bool_at_put(1, 1);
  o->bool_at_put(0, 1);

  ASSERT_EQ((jboolean)1, o->bool_at(0));
  ASSERT_EQ((jboolean)1, o->bool_at(1));
  ASSERT_EQ((jboolean)1, o->bool_at(2));
  ASSERT_EQ((jboolean)1, o->bool_at(3));
  ASSERT_EQ((jboolean)0, o->bool_at(4));
  ASSERT_EQ((jboolean)0, o->bool_at(5));
  ASSERT_EQ((jboolean)0, o->bool_at(6));
  ASSERT_EQ((jboolean)0, o->bool_at(7));
}
