/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/resourceArea.hpp"
#include "utilities/growableArray.hpp"
#include "unittest.hpp"

struct WithEmbeddedArray {
  // Array embedded in another class
  GrowableArray<int> _a;

  // Resource allocated data array
  WithEmbeddedArray(int initial_max) : _a(initial_max) {}
  // Arena allocated data array
  WithEmbeddedArray(Arena* arena, int initial_max) : _a(arena, initial_max, 0, 0) {}
  // CHeap allocated data array
  WithEmbeddedArray(int initial_max, MEMFLAGS memflags) : _a(initial_max, memflags) {
    assert(memflags != mtNone, "test requirement");
  }
  WithEmbeddedArray(const GrowableArray<int>& other) : _a(other) {}
};

// Test fixture to work with TEST_VM_F
class GrowableArrayTest : public ::testing::Test {
protected:
  // friend -> private accessors
  template <typename E>
  static bool elements_on_C_heap(const GrowableArray<E>* array) {
    return array->on_C_heap();
  }
  template <typename E>
  static bool elements_on_stack(const GrowableArray<E>* array) {
    return array->on_stack();
  }
  template <typename E>
  static bool elements_on_arena(const GrowableArray<E>* array) {
    return array->on_arena();
  }

  template <typename ArrayClass>
  static void test_append(ArrayClass* a) {
    // Add elements
    for (int i = 0; i < 10; i++) {
      a->append(i);
    }

    // Check size
    ASSERT_EQ(a->length(), 10);

    // Check elements
    for (int i = 0; i < 10; i++) {
      EXPECT_EQ(a->at(i), i);
    }
  }

  template <typename ArrayClass>
  static void test_clear(ArrayClass* a) {
    // Add elements
    for (int i = 0; i < 10; i++) {
      a->append(i);
    }

    // Check size
    ASSERT_EQ(a->length(), 10);
    ASSERT_EQ(a->is_empty(), false);

    // Clear elements
    a->clear();

    // Check size
    ASSERT_EQ(a->length(), 0);
    ASSERT_EQ(a->is_empty(), true);

    // Add element
    a->append(11);

    // Check size
    ASSERT_EQ(a->length(), 1);
    ASSERT_EQ(a->is_empty(), false);

    // Clear elements
    a->clear();

    // Check size
    ASSERT_EQ(a->length(), 0);
    ASSERT_EQ(a->is_empty(), true);
  }

  template <typename ArrayClass>
  static void test_iterator(ArrayClass* a) {
    // Add elements
    for (int i = 0; i < 10; i++) {
      a->append(i);
    }

    // Iterate
    int counter = 0;
    for (GrowableArrayIterator<int> i = a->begin(); i != a->end(); ++i) {
      ASSERT_EQ(*i, counter++);
    }

    // Check count
    ASSERT_EQ(counter, 10);
  }

  template <typename ArrayClass>
  static void test_copy1(ArrayClass* a) {
    ASSERT_EQ(a->length(), 1);
    ASSERT_EQ(a->at(0), 1);

    // Only allowed to copy to stack and embedded ResourceObjs

    // Copy to stack
    {
      GrowableArray<int> c(*a);

      ASSERT_EQ(c.length(), 1);
      ASSERT_EQ(c.at(0), 1);
    }

    // Copy to embedded
    {
      WithEmbeddedArray c(*a);

      ASSERT_EQ(c._a.length(), 1);
      ASSERT_EQ(c._a.at(0), 1);
    }
  }

  template <typename ArrayClass>
  static void test_assignment1(ArrayClass* a) {
    ASSERT_EQ(a->length(), 1);
    ASSERT_EQ(a->at(0), 1);

    // Only allowed to assign to stack and embedded ResourceObjs

    // Copy to embedded/resource
    {
      ResourceMark rm;
      GrowableArray<int> c(1);
      c = *a;

      ASSERT_EQ(c.length(), 1);
      ASSERT_EQ(c.at(0), 1);
    }

    // Copy to embedded/arena
    {
      Arena arena(mtTest);
      GrowableArray<int> c(&arena, 1, 0, 0);
      c = *a;

      ASSERT_EQ(c.length(), 1);
      ASSERT_EQ(c.at(0), 1);
    }

    // Copy to embedded/resource
    {
      ResourceMark rm;
      WithEmbeddedArray c(1);
      c._a = *a;

      ASSERT_EQ(c._a.length(), 1);
      ASSERT_EQ(c._a.at(0), 1);
    }

    // Copy to embedded/arena
    {
      Arena arena(mtTest);
      WithEmbeddedArray c(&arena, 1);
      c._a = *a;

      ASSERT_EQ(c._a.length(), 1);
      ASSERT_EQ(c._a.at(0), 1);
    }
  }

  // Supported by all GrowableArrays
  enum TestEnum {
    Append,
    Clear,
    Iterator,
  };

  template <typename ArrayClass>
  static void do_test(ArrayClass* a, TestEnum test) {
    switch (test) {
      case Append:
        test_append(a);
        break;

      case Clear:
        test_clear(a);
        break;

      case Iterator:
        test_iterator(a);
        break;

      default:
        fatal("Missing dispatch");
        break;
    }
  }

  // Only supported by GrowableArrays without CHeap data arrays
  enum TestNoCHeapEnum {
    Copy1,
    Assignment1,
  };

  template <typename ArrayClass>
  static void do_test(ArrayClass* a, TestNoCHeapEnum test) {
    switch (test) {
      case Copy1:
        test_copy1(a);
        break;

      case Assignment1:
        test_assignment1(a);
        break;

      default:
        fatal("Missing dispatch");
        break;
    }
  }

  enum ModifyEnum {
    Append1,
    Append1Clear,
    Append1ClearAndDeallocate,
    NoModify
  };

  template <typename ArrayClass>
  static void do_modify(ArrayClass* a, ModifyEnum modify) {
    switch (modify) {
      case Append1:
        a->append(1);
        break;

      case Append1Clear:
        a->append(1);
        a->clear();
        break;

      case Append1ClearAndDeallocate:
        a->append(1);
        a->clear_and_deallocate();
        break;

      case NoModify:
        // Nothing to do
        break;

      default:
        fatal("Missing dispatch");
        break;
    }
  }

  static const int Max0 = 0;
  static const int Max1 = 1;

  template <typename ArrayClass, typename T>
  static void modify_and_test(ArrayClass* array, ModifyEnum modify, T test) {
    do_modify(array, modify);
    do_test(array, test);
  }

  template <typename T>
  static void with_no_cheap_array(int max, ModifyEnum modify, T test) {
    // Resource/Resource allocated
    {
      ResourceMark rm;
      GrowableArray<int>* a = new GrowableArray<int>(max);
      modify_and_test(a, modify, test);
    }

    // Resource/Arena allocated
    //  Combination not supported

    // CHeap/Resource allocated
    //  Combination not supported

    // CHeap/Arena allocated
    //  Combination not supported

    // Stack/Resource allocated
    {
      ResourceMark rm;
      GrowableArray<int> a(max);
      modify_and_test(&a, modify, test);
    }

    // Stack/Arena allocated
    {
      Arena arena(mtTest);
      GrowableArray<int> a(&arena, max, 0, 0);
      modify_and_test(&a, modify, test);
    }

    // Embedded/Resource allocated
    {
      ResourceMark rm;
      WithEmbeddedArray w(max);
      modify_and_test(&w._a, modify, test);
    }

    // Embedded/Arena allocated
    {
      Arena arena(mtTest);
      WithEmbeddedArray w(&arena, max);
      modify_and_test(&w._a, modify, test);
    }
  }

  static void with_cheap_array(int max, ModifyEnum modify, TestEnum test) {
    // Resource/CHeap allocated
    //  Combination not supported

    // CHeap/CHeap allocated
    {
      GrowableArray<int>* a = new (ResourceObj::C_HEAP, mtTest) GrowableArray<int>(max, mtTest);
      modify_and_test(a, modify, test);
      delete a;
    }

    // Stack/CHeap allocated
    {
      GrowableArray<int> a(max, mtTest);
      modify_and_test(&a, modify, test);
    }

    // Embedded/CHeap allocated
    {
      WithEmbeddedArray w(max, mtTest);
      modify_and_test(&w._a, modify, test);
    }
  }

  static void with_all_types(int max, ModifyEnum modify, TestEnum test) {
    with_no_cheap_array(max, modify, test);
    with_cheap_array(max, modify, test);
  }

  static void with_all_types_empty(TestEnum test) {
    with_all_types(Max0, NoModify, test);
  }

  static void with_all_types_max_set(TestEnum test) {
    with_all_types(Max1, NoModify, test);
  }

  static void with_all_types_cleared(TestEnum test) {
    with_all_types(Max1, Append1Clear, test);
  }

  static void with_all_types_clear_and_deallocated(TestEnum test) {
    with_all_types(Max1, Append1ClearAndDeallocate, test);
  }

  static void with_all_types_all_0(TestEnum test) {
    with_all_types_empty(test);
    with_all_types_max_set(test);
    with_all_types_cleared(test);
    with_all_types_clear_and_deallocated(test);
  }

  static void with_no_cheap_array_append1(TestNoCHeapEnum test) {
    with_no_cheap_array(Max0, Append1, test);
  }
};

TEST_VM_F(GrowableArrayTest, append) {
  with_all_types_all_0(Append);
}

TEST_VM_F(GrowableArrayTest, clear) {
  with_all_types_all_0(Clear);
}

TEST_VM_F(GrowableArrayTest, iterator) {
  with_all_types_all_0(Iterator);
}

TEST_VM_F(GrowableArrayTest, copy) {
  with_no_cheap_array_append1(Copy1);
}

TEST_VM_F(GrowableArrayTest, assignment) {
  with_no_cheap_array_append1(Assignment1);
}

#ifdef ASSERT
TEST_VM_F(GrowableArrayTest, where) {
  WithEmbeddedArray s(1, mtTest);
  ASSERT_FALSE(s._a.allocated_on_C_heap());
  ASSERT_TRUE(elements_on_C_heap(&s._a));

  // Resource/Resource allocated
  {
    ResourceMark rm;
    GrowableArray<int>* a = new GrowableArray<int>();
    ASSERT_TRUE(a->allocated_on_res_area());
    ASSERT_TRUE(elements_on_stack(a));
  }

  // Resource/CHeap allocated
  //  Combination not supported

  // Resource/Arena allocated
  //  Combination not supported

  // CHeap/Resource allocated
  //  Combination not supported

  // CHeap/CHeap allocated
  {
    GrowableArray<int>* a = new (ResourceObj::C_HEAP, mtTest) GrowableArray<int>(0, mtTest);
    ASSERT_TRUE(a->allocated_on_C_heap());
    ASSERT_TRUE(elements_on_C_heap(a));
    delete a;
  }

  // CHeap/Arena allocated
  //  Combination not supported

  // Stack/Resource allocated
  {
    ResourceMark rm;
    GrowableArray<int> a(0);
    ASSERT_TRUE(a.allocated_on_stack());
    ASSERT_TRUE(elements_on_stack(&a));
  }

  // Stack/CHeap allocated
  {
    GrowableArray<int> a(0, mtTest);
    ASSERT_TRUE(a.allocated_on_stack());
    ASSERT_TRUE(elements_on_C_heap(&a));
  }

  // Stack/Arena allocated
  {
    Arena arena(mtTest);
    GrowableArray<int> a(&arena, 0, 0, 0);
    ASSERT_TRUE(a.allocated_on_stack());
    ASSERT_TRUE(elements_on_arena(&a));
  }

  // Embedded/Resource allocated
  {
    ResourceMark rm;
    WithEmbeddedArray w(0);
    ASSERT_TRUE(w._a.allocated_on_stack());
    ASSERT_TRUE(elements_on_stack(&w._a));
  }

  // Embedded/CHeap allocated
  {
    WithEmbeddedArray w(0, mtTest);
    ASSERT_TRUE(w._a.allocated_on_stack());
    ASSERT_TRUE(elements_on_C_heap(&w._a));
  }

  // Embedded/Arena allocated
  {
    Arena arena(mtTest);
    WithEmbeddedArray w(&arena, 0);
    ASSERT_TRUE(w._a.allocated_on_stack());
    ASSERT_TRUE(elements_on_arena(&w._a));
  }
}

TEST_VM_ASSERT_MSG(GrowableArrayAssertingTest, copy_with_embedded_cheap,
    "assert.!on_C_heap... failed: Copying of CHeap arrays not supported") {
  WithEmbeddedArray s(1, mtTest);
  // Intentionally asserts that copy of CHeap arrays are not allowed
  WithEmbeddedArray c(s);
}

TEST_VM_ASSERT_MSG(GrowableArrayAssertingTest, assignment_with_embedded_cheap,
    "assert.!on_C_heap... failed: Assignment of CHeap arrays not supported") {
  WithEmbeddedArray s(1, mtTest);
  WithEmbeddedArray c(1, mtTest);

  // Intentionally asserts that assignment of CHeap arrays are not allowed
  c = s;
}

#endif

TEST(GrowableArrayCHeap, sanity) {
  // Stack/CHeap
  {
    GrowableArrayCHeap<int, mtTest> a(0);
#ifdef ASSERT
    ASSERT_TRUE(a.allocated_on_stack());
#endif
    ASSERT_TRUE(a.is_empty());

    a.append(1);
    ASSERT_FALSE(a.is_empty());
    ASSERT_EQ(a.at(0), 1);
  }

  // CHeap/CHeap
  {
    GrowableArrayCHeap<int, mtTest>* a = new GrowableArrayCHeap<int, mtTest>(0);
#ifdef ASSERT
    ASSERT_TRUE(a->allocated_on_C_heap());
#endif
    ASSERT_TRUE(a->is_empty());

    a->append(1);
    ASSERT_FALSE(a->is_empty());
    ASSERT_EQ(a->at(0), 1);
    delete a;
  }

  // CHeap/CHeap - nothrow new operator
  {
    GrowableArrayCHeap<int, mtTest>* a = new (std::nothrow) GrowableArrayCHeap<int, mtTest>(0);
#ifdef ASSERT
    ASSERT_TRUE(a->allocated_on_C_heap());
#endif
    ASSERT_TRUE(a->is_empty());

    a->append(1);
    ASSERT_FALSE(a->is_empty());
    ASSERT_EQ(a->at(0), 1);
    delete a;
  }
}
