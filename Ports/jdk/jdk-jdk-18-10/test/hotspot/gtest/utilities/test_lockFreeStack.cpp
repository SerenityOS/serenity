/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/lockFreeStack.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"
#include <new>

class LockFreeStackTestElement {
  typedef LockFreeStackTestElement Element;

  Element* volatile _entry;
  Element* volatile _entry1;
  size_t _id;

  static Element* volatile* entry_ptr(Element& e) { return &e._entry; }
  static Element* volatile* entry1_ptr(Element& e) { return &e._entry1; }

public:
  LockFreeStackTestElement(size_t id = 0) : _entry(), _entry1(), _id(id) {}
  size_t id() const { return _id; }
  void set_id(size_t value) { _id = value; }

  typedef LockFreeStack<Element, &entry_ptr> TestStack;
  typedef LockFreeStack<Element, &entry1_ptr> TestStack1;
};

typedef LockFreeStackTestElement Element;
typedef Element::TestStack TestStack;
typedef Element::TestStack1 TestStack1;

static void initialize_ids(Element* elements, size_t size) {
  for (size_t i = 0; i < size; ++i) {
    elements[i].set_id(i);
  }
}

class LockFreeStackTestBasics : public ::testing::Test {
public:
  LockFreeStackTestBasics();

  static const size_t nelements = 10;
  Element elements[nelements];
  TestStack stack;

private:
  void initialize();
};

const size_t LockFreeStackTestBasics::nelements;

LockFreeStackTestBasics::LockFreeStackTestBasics() : stack() {
  initialize_ids(elements, nelements);
  initialize();
}

void LockFreeStackTestBasics::initialize() {
  ASSERT_TRUE(stack.empty());
  ASSERT_EQ(0u, stack.length());
  ASSERT_TRUE(stack.pop() == NULL);
  ASSERT_TRUE(stack.top() == NULL);

  for (size_t id = 0; id < nelements; ++id) {
    ASSERT_EQ(id, stack.length());
    Element* e = &elements[id];
    ASSERT_EQ(id, e->id());
    stack.push(*e);
    ASSERT_FALSE(stack.empty());
    ASSERT_EQ(e, stack.top());
  }
}

TEST_F(LockFreeStackTestBasics, push_pop) {
  for (size_t i = nelements; i > 0; ) {
    ASSERT_FALSE(stack.empty());
    ASSERT_EQ(i, stack.length());
    --i;
    Element* e = stack.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());
  }
  ASSERT_TRUE(stack.empty());
  ASSERT_EQ(0u, stack.length());
  ASSERT_TRUE(stack.pop() == NULL);
}

TEST_F(LockFreeStackTestBasics, prepend_one) {
  TestStack other_stack;
  ASSERT_TRUE(other_stack.empty());
  ASSERT_TRUE(other_stack.pop() == NULL);
  ASSERT_EQ(0u, other_stack.length());
  ASSERT_TRUE(other_stack.top() == NULL);
  ASSERT_TRUE(other_stack.pop() == NULL);

  other_stack.prepend(*stack.pop_all());
  ASSERT_EQ(nelements, other_stack.length());
  ASSERT_TRUE(stack.empty());
  ASSERT_EQ(0u, stack.length());
  ASSERT_TRUE(stack.pop() == NULL);
  ASSERT_TRUE(stack.top() == NULL);

  for (size_t i = nelements; i > 0; ) {
    ASSERT_EQ(i, other_stack.length());
    --i;
    Element* e = other_stack.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());
  }
  ASSERT_EQ(0u, other_stack.length());
  ASSERT_TRUE(other_stack.pop() == NULL);
}

TEST_F(LockFreeStackTestBasics, prepend_two) {
  TestStack other_stack;
  ASSERT_TRUE(other_stack.empty());
  ASSERT_EQ(0u, other_stack.length());
  ASSERT_TRUE(other_stack.top() == NULL);
  ASSERT_TRUE(other_stack.pop() == NULL);

  Element* top = stack.pop_all();
  ASSERT_EQ(top, &elements[nelements - 1]);
  other_stack.prepend(*top, elements[0]);

  for (size_t i = nelements; i > 0; ) {
    ASSERT_EQ(i, other_stack.length());
    --i;
    Element* e = other_stack.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());
  }
  ASSERT_EQ(0u, other_stack.length());
  ASSERT_TRUE(other_stack.pop() == NULL);
}

TEST_F(LockFreeStackTestBasics, two_stacks) {
  TestStack1 stack1;
  ASSERT_TRUE(stack1.pop() == NULL);

  for (size_t id = 0; id < nelements; ++id) {
    stack1.push(elements[id]);
  }
  ASSERT_EQ(nelements, stack1.length());
  Element* e0 = stack.top();
  Element* e1 = stack1.top();
  while (true) {
    ASSERT_EQ(e0, e1);
    if (e0 == NULL) break;
    e0 = stack.next(*e0);
    e1 = stack1.next(*e1);
  }

  for (size_t i = nelements; i > 0; ) {
    ASSERT_EQ(i, stack.length());
    ASSERT_EQ(i, stack1.length());
    --i;
    Element* e = stack.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());

    Element* e1 = stack1.pop();
    ASSERT_TRUE(e1 != NULL);
    ASSERT_EQ(&elements[i], e1);
    ASSERT_EQ(i, e1->id());

    ASSERT_EQ(e, e1);
  }
  ASSERT_EQ(0u, stack.length());
  ASSERT_EQ(0u, stack1.length());
  ASSERT_TRUE(stack.pop() == NULL);
  ASSERT_TRUE(stack1.pop() == NULL);
}

class LockFreeStackTestThread : public JavaTestThread {
  uint _id;
  TestStack* _from;
  TestStack* _to;
  volatile size_t* _processed;
  size_t _process_limit;
  size_t _local_processed;
  volatile bool _ready;

public:
  LockFreeStackTestThread(Semaphore* post,
                          uint id,
                          TestStack* from,
                          TestStack* to,
                          volatile size_t* processed,
                          size_t process_limit) :
    JavaTestThread(post),
    _id(id),
    _from(from),
    _to(to),
    _processed(processed),
    _process_limit(process_limit),
    _local_processed(0),
    _ready(false)
  {}

  virtual void main_run() {
    Atomic::release_store_fence(&_ready, true);
    while (true) {
      Element* e = _from->pop();
      if (e != NULL) {
        _to->push(*e);
        Atomic::inc(_processed);
        ++_local_processed;
      } else if (Atomic::load_acquire(_processed) == _process_limit) {
        tty->print_cr("thread %u processed " SIZE_FORMAT, _id, _local_processed);
        return;
      }
    }
  }

  bool ready() const { return Atomic::load_acquire(&_ready); }
};

TEST_VM(LockFreeStackTest, stress) {
  Semaphore post;
  TestStack initial_stack;
  TestStack start_stack;
  TestStack middle_stack;
  TestStack final_stack;
  volatile size_t stage1_processed = 0;
  volatile size_t stage2_processed = 0;

  const size_t nelements = 10000;
  Element* elements = NEW_C_HEAP_ARRAY(Element, nelements, mtOther);
  for (size_t id = 0; id < nelements; ++id) {
    ::new (&elements[id]) Element(id);
    initial_stack.push(elements[id]);
  }
  ASSERT_EQ(nelements, initial_stack.length());

  // - stage1 threads pop from start_stack and push to middle_stack.
  // - stage2 threads pop from middle_stack and push to final_stack.
  // - all threads in a stage count the number of elements processed in
  //   their corresponding stageN_processed counter.

  const uint stage1_threads = 2;
  const uint stage2_threads = 2;
  const uint nthreads = stage1_threads + stage2_threads;
  LockFreeStackTestThread* threads[nthreads] = {};

  for (uint i = 0; i < ARRAY_SIZE(threads); ++i) {
    TestStack* from = &start_stack;
    TestStack* to = &middle_stack;
    volatile size_t* processed = &stage1_processed;
    if (i >= stage1_threads) {
      from = &middle_stack;
      to = &final_stack;
      processed = &stage2_processed;
    }
    threads[i] =
      new LockFreeStackTestThread(&post, i, from, to, processed, nelements);
    threads[i]->doit();
    while (!threads[i]->ready()) {} // Wait until ready to start test.
  }

  // Transfer elements to start_stack to start test.
  start_stack.prepend(*initial_stack.pop_all());

  // Wait for all threads to complete.
  for (uint i = 0; i < nthreads; ++i) {
    post.wait();
  }

  // Verify expected state.
  ASSERT_EQ(nelements, stage1_processed);
  ASSERT_EQ(nelements, stage2_processed);
  ASSERT_EQ(0u, initial_stack.length());
  ASSERT_EQ(0u, start_stack.length());
  ASSERT_EQ(0u, middle_stack.length());
  ASSERT_EQ(nelements, final_stack.length());
  while (final_stack.pop() != NULL) {}

  FREE_C_HEAP_ARRAY(Element, elements);
}
