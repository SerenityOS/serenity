/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "memory/allocation.hpp"
#include "memory/resourceArea.hpp"
#include "unittest.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/resourceHash.hpp"

class CommonResourceHashtableTest : public ::testing::Test {
 protected:
  typedef void* K;
  typedef uintx V;
  const static MEMFLAGS MEM_TYPE = mtInternal;

  static unsigned identity_hash(const K& k) {
    return (unsigned) (uintptr_t) k;
  }

  static unsigned bad_hash(const K& k) {
    return 1;
  }

  static void* as_K(uintptr_t val) {
    return (void*) val;
  }

  class EqualityTestIter {
   public:

    bool do_entry(K const& k, V const& v) {
      if ((uintptr_t) k != (uintptr_t) v) {
        EXPECT_EQ((uintptr_t) k, (uintptr_t) v);
        return false;
      } else {
        return true; // continue iteration
      }
    }
  };

  class DeleterTestIter {
    int _val;
   public:
    DeleterTestIter(int i) : _val(i) {}

    bool do_entry(K const& k, V const& v) {
      if ((uintptr_t) k == (uintptr_t) _val) {
        // Delete me!
        return true;
      } else {
        return false; // continue iteration
      }
    }
  };

};

class SmallResourceHashtableTest : public CommonResourceHashtableTest {
 protected:

  template<
  unsigned (*HASH) (K const&) = primitive_hash<K>,
  bool (*EQUALS)(K const&, K const&) = primitive_equals<K>,
  unsigned SIZE = 256,
  ResourceObj::allocation_type ALLOC_TYPE = ResourceObj::RESOURCE_AREA
  >
  class Runner : public AllStatic {
   public:

    static void test(V step) {
      EqualityTestIter et;
      ResourceHashtable<K, V, SIZE, ALLOC_TYPE, MEM_TYPE, HASH, EQUALS> rh;

      ASSERT_FALSE(rh.contains(as_K(step)));

      ASSERT_TRUE(rh.put(as_K(step), step));
      ASSERT_TRUE(rh.contains(as_K(step)));

      ASSERT_FALSE(rh.put(as_K(step), step));

      ASSERT_TRUE(rh.put(as_K(2 * step), 2 * step));
      ASSERT_TRUE(rh.put(as_K(3 * step), 3 * step));
      ASSERT_TRUE(rh.put(as_K(4 * step), 4 * step));
      ASSERT_TRUE(rh.put(as_K(5 * step), 5 * step));

      ASSERT_FALSE(rh.remove(as_K(0x0)));

      rh.iterate(&et);
      if (::testing::Test::HasFailure()) {
        return;
      }

      ASSERT_TRUE(rh.remove(as_K(step)));
      ASSERT_FALSE(rh.contains(as_K(step)));
      rh.iterate(&et);


      // Test put_if_absent(key) (creating a default-created value)
      bool created = false;
      V* v = rh.put_if_absent(as_K(step), &created);
      ASSERT_TRUE(rh.contains(as_K(step)));
      ASSERT_TRUE(created);
      *v = (V)step;

      // Calling this function a second time should yield the same value pointer
      V* v2 = rh.put_if_absent(as_K(step), &created);
      ASSERT_EQ(v, v2);
      ASSERT_EQ(*v2, *v);
      ASSERT_FALSE(created);

      ASSERT_TRUE(rh.remove(as_K(step)));
      ASSERT_FALSE(rh.contains(as_K(step)));
      rh.iterate(&et);

      // Test put_if_absent(key, value)
      v = rh.put_if_absent(as_K(step), step, &created);
      ASSERT_EQ(*v, step);
      ASSERT_TRUE(rh.contains(as_K(step)));
      ASSERT_TRUE(created);

      v2 = rh.put_if_absent(as_K(step), step, &created);
      // Calling this function a second time should yield the same value pointer
      ASSERT_EQ(v, v2);
      ASSERT_EQ(*v2, (V)step);
      ASSERT_FALSE(created);

      ASSERT_TRUE(rh.remove(as_K(step)));
      ASSERT_FALSE(rh.contains(as_K(step)));
      rh.iterate(&et);


    }
  };
};

TEST_VM_F(SmallResourceHashtableTest, default) {
  ResourceMark rm;
  Runner<>::test(0x1);
}

TEST_VM_F(SmallResourceHashtableTest, default_shifted) {
  ResourceMark rm;
  Runner<>::test(0x10);
}

TEST_VM_F(SmallResourceHashtableTest, bad_hash) {
  ResourceMark rm;
  Runner<bad_hash>::test(0x1);
}

TEST_VM_F(SmallResourceHashtableTest, bad_hash_shifted) {
  ResourceMark rm;
  Runner<bad_hash>::test(0x10);
}

TEST_VM_F(SmallResourceHashtableTest, identity_hash) {
  ResourceMark rm;
  Runner<identity_hash>::test(0x1);
}

TEST_VM_F(SmallResourceHashtableTest, identity_hash_shifted) {
  ResourceMark rm;
  Runner<identity_hash>::test(0x10);
}

TEST_VM_F(SmallResourceHashtableTest, primitive_hash_no_rm) {
  Runner<primitive_hash<K>, primitive_equals<K>, 512, ResourceObj::C_HEAP>::test(0x1);
}

TEST_VM_F(SmallResourceHashtableTest, primitive_hash_no_rm_shifted) {
  Runner<primitive_hash<K>, primitive_equals<K>, 512, ResourceObj::C_HEAP>::test(0x10);
}

TEST_VM_F(SmallResourceHashtableTest, bad_hash_no_rm) {
  Runner<bad_hash, primitive_equals<K>, 512, ResourceObj::C_HEAP>::test(0x1);
}

TEST_VM_F(SmallResourceHashtableTest, bad_hash_no_rm_shifted) {
  Runner<bad_hash, primitive_equals<K>, 512, ResourceObj::C_HEAP>::test(0x10);
}

TEST_VM_F(SmallResourceHashtableTest, identity_hash_no_rm) {
  Runner<identity_hash, primitive_equals<K>, 1, ResourceObj::C_HEAP>::test(0x1);
}

TEST_VM_F(SmallResourceHashtableTest, identity_hash_no_rm_shifted) {
  Runner<identity_hash, primitive_equals<K>, 1, ResourceObj::C_HEAP>::test(0x10);
}

class GenericResourceHashtableTest : public CommonResourceHashtableTest {
 protected:

  template<
  unsigned (*HASH) (K const&) = primitive_hash<K>,
  bool (*EQUALS)(K const&, K const&) = primitive_equals<K>,
  unsigned SIZE = 256,
  ResourceObj::allocation_type ALLOC_TYPE = ResourceObj::RESOURCE_AREA
  >
  class Runner : public AllStatic {
   public:

    static void test(unsigned num_elements = SIZE) {
      EqualityTestIter et;
      ResourceHashtable<K, V, SIZE, ALLOC_TYPE, MEM_TYPE, HASH, EQUALS> rh;

      for (uintptr_t i = 0; i < num_elements; ++i) {
        ASSERT_TRUE(rh.put(as_K(i), i));
      }

      rh.iterate(&et);
      if (::testing::Test::HasFailure()) {
        return;
      }

      for (uintptr_t i = num_elements; i > 0; --i) {
        uintptr_t index = i - 1;
        ASSERT_TRUE((rh.remove(as_K(index))));
      }

      rh.iterate(&et);
      if (::testing::Test::HasFailure()) {
        return;
      }
      for (uintptr_t i = num_elements; i > 0; --i) {
        uintptr_t index = i - 1;
        ASSERT_FALSE(rh.remove(as_K(index)));
      }
      rh.iterate(&et);

      // Add more entries in and then delete one.
      for (uintptr_t i = 10; i > 0; --i) {
        uintptr_t index = i - 1;
        ASSERT_TRUE(rh.put(as_K(index), index));
      }
      DeleterTestIter dt(5);
      rh.unlink(&dt);
      ASSERT_FALSE(rh.get(as_K(5)));
    }
  };
};

TEST_VM_F(GenericResourceHashtableTest, default) {
  ResourceMark rm;
  Runner<>::test();
}

TEST_VM_F(GenericResourceHashtableTest, bad_hash) {
  ResourceMark rm;
  Runner<bad_hash>::test();
}

TEST_VM_F(GenericResourceHashtableTest, identity_hash) {
  ResourceMark rm;
  Runner<identity_hash>::test();
}

TEST_VM_F(GenericResourceHashtableTest, primitive_hash_no_rm) {
  Runner<primitive_hash<K>, primitive_equals<K>, 512, ResourceObj::C_HEAP>::test();
}

TEST_VM_F(GenericResourceHashtableTest, bad_hash_no_rm) {
  Runner<bad_hash, primitive_equals<K>, 512, ResourceObj::C_HEAP>::test();
}

TEST_VM_F(GenericResourceHashtableTest, identity_hash_no_rm) {
  Runner<identity_hash, primitive_equals<K>, 1, ResourceObj::C_HEAP>::test(512);
}
