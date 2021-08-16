/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "utilities/nonblockingQueue.inline.hpp"
#include "utilities/pair.hpp"
#include "threadHelper.inline.hpp"
#include "unittest.hpp"
#include <new>

class NonblockingQueueTestElement {
  typedef NonblockingQueueTestElement Element;

  Element* volatile _entry;
  Element* volatile _entry1;
  size_t _id;

  static Element* volatile* entry_ptr(Element& e) { return &e._entry; }
  static Element* volatile* entry1_ptr(Element& e) { return &e._entry1; }

public:
  using TestQueue = NonblockingQueue<Element, &entry_ptr>;
  using TestQueue1 = NonblockingQueue<Element, &entry1_ptr>;

  NonblockingQueueTestElement(size_t id = 0) : _entry(), _entry1(), _id(id) {}
  size_t id() const { return _id; }
  void set_id(size_t value) { _id = value; }
  Element* next() { return _entry; }
  Element* next1() { return _entry1; }
};

typedef NonblockingQueueTestElement Element;
typedef Element::TestQueue TestQueue;
typedef Element::TestQueue1 TestQueue1;

static void initialize(Element* elements, size_t size, TestQueue* queue) {
  for (size_t i = 0; i < size; ++i) {
    elements[i].set_id(i);
  }
  ASSERT_TRUE(queue->empty());
  ASSERT_EQ(0u, queue->length());
  ASSERT_TRUE(queue->is_end(queue->first()));
  ASSERT_TRUE(queue->pop() == NULL);

  for (size_t id = 0; id < size; ++id) {
    ASSERT_EQ(id, queue->length());
    Element* e = &elements[id];
    ASSERT_EQ(id, e->id());
    queue->push(*e);
    ASSERT_FALSE(queue->empty());
    // first() is always the oldest element.
    ASSERT_EQ(&elements[0], queue->first());
  }
}

class NonblockingQueueTestBasics : public ::testing::Test {
public:
  NonblockingQueueTestBasics();

  static const size_t nelements = 10;
  Element elements[nelements];
  TestQueue queue;
};

const size_t NonblockingQueueTestBasics::nelements;

NonblockingQueueTestBasics::NonblockingQueueTestBasics() : queue() {
  initialize(elements, nelements, &queue);
}

TEST_F(NonblockingQueueTestBasics, pop) {
  for (size_t i = 0; i < nelements; ++i) {
    ASSERT_FALSE(queue.empty());
    ASSERT_EQ(nelements - i, queue.length());
    Element* e = queue.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());
  }
  ASSERT_TRUE(queue.empty());
  ASSERT_EQ(0u, queue.length());
  ASSERT_TRUE(queue.pop() == NULL);
}

TEST_F(NonblockingQueueTestBasics, append) {
  TestQueue other_queue;
  ASSERT_TRUE(other_queue.empty());
  ASSERT_EQ(0u, other_queue.length());
  ASSERT_TRUE(other_queue.is_end(other_queue.first()));
  ASSERT_TRUE(other_queue.pop() == NULL);

  Pair<Element*, Element*> pair = queue.take_all();
  other_queue.append(*pair.first, *pair.second);
  ASSERT_EQ(nelements, other_queue.length());
  ASSERT_TRUE(queue.empty());
  ASSERT_EQ(0u, queue.length());
  ASSERT_TRUE(queue.is_end(queue.first()));
  ASSERT_TRUE(queue.pop() == NULL);

  for (size_t i = 0; i < nelements; ++i) {
    ASSERT_EQ(nelements - i, other_queue.length());
    Element* e = other_queue.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());
  }
  ASSERT_EQ(0u, other_queue.length());
  ASSERT_TRUE(other_queue.pop() == NULL);
}

TEST_F(NonblockingQueueTestBasics, two_queues) {
  TestQueue1 queue1;
  ASSERT_TRUE(queue1.pop() == NULL);

  for (size_t id = 0; id < nelements; ++id) {
    queue1.push(elements[id]);
  }
  ASSERT_EQ(nelements, queue1.length());
  Element* e0 = queue.first();
  Element* e1 = queue1.first();
  ASSERT_TRUE(e0 != NULL);
  ASSERT_TRUE(e1 != NULL);
  ASSERT_FALSE(queue.is_end(e0));
  ASSERT_FALSE(queue1.is_end(e1));
  while (!queue.is_end(e0) && !queue1.is_end(e1)) {
    ASSERT_EQ(e0, e1);
    e0 = e0->next();
    e1 = e1->next1();
  }
  ASSERT_TRUE(queue.is_end(e0));
  ASSERT_TRUE(queue1.is_end(e1));

  for (size_t i = 0; i < nelements; ++i) {
    ASSERT_EQ(nelements - i, queue.length());
    ASSERT_EQ(nelements - i, queue1.length());

    Element* e = queue.pop();
    ASSERT_TRUE(e != NULL);
    ASSERT_EQ(&elements[i], e);
    ASSERT_EQ(i, e->id());

    Element* e1 = queue1.pop();
    ASSERT_TRUE(e1 != NULL);
    ASSERT_EQ(&elements[i], e1);
    ASSERT_EQ(i, e1->id());

    ASSERT_EQ(e, e1);
  }
  ASSERT_EQ(0u, queue.length());
  ASSERT_EQ(0u, queue1.length());
  ASSERT_TRUE(queue.pop() == NULL);
  ASSERT_TRUE(queue1.pop() == NULL);
}

class NonblockingQueueTestThread : public JavaTestThread {
  uint _id;
  TestQueue* _from;
  TestQueue* _to;
  volatile size_t* _processed;
  size_t _process_limit;
  size_t _local_processed;
  volatile bool _ready;

public:
  NonblockingQueueTestThread(Semaphore* post,
                             uint id,
                             TestQueue* from,
                             TestQueue* to,
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

TEST_VM(NonblockingQueueTest, stress) {
  Semaphore post;
  TestQueue initial_queue;
  TestQueue start_queue;
  TestQueue middle_queue;
  TestQueue final_queue;
  volatile size_t stage1_processed = 0;
  volatile size_t stage2_processed = 0;

  const size_t nelements = 10000;
  Element* elements = NEW_C_HEAP_ARRAY(Element, nelements, mtOther);
  for (size_t id = 0; id < nelements; ++id) {
    ::new (&elements[id]) Element(id);
    initial_queue.push(elements[id]);
  }
  ASSERT_EQ(nelements, initial_queue.length());

  // - stage1 threads pop from start_queue and push to middle_queue.
  // - stage2 threads pop from middle_queue and push to final_queue.
  // - all threads in a stage count the number of elements processed in
  //   their corresponding stageN_processed counter.

  const uint stage1_threads = 2;
  const uint stage2_threads = 2;
  const uint nthreads = stage1_threads + stage2_threads;
  NonblockingQueueTestThread* threads[nthreads] = {};

  for (uint i = 0; i < ARRAY_SIZE(threads); ++i) {
    TestQueue* from = &start_queue;
    TestQueue* to = &middle_queue;
    volatile size_t* processed = &stage1_processed;
    if (i >= stage1_threads) {
      from = &middle_queue;
      to = &final_queue;
      processed = &stage2_processed;
    }
    threads[i] =
      new NonblockingQueueTestThread(&post, i, from, to, processed, nelements);
    threads[i]->doit();
    while (!threads[i]->ready()) {} // Wait until ready to start test.
  }

  // Transfer elements to start_queue to start test.
  Pair<Element*, Element*> pair = initial_queue.take_all();
  start_queue.append(*pair.first, *pair.second);

  // Wait for all threads to complete.
  for (uint i = 0; i < nthreads; ++i) {
    post.wait();
  }

  // Verify expected state.
  ASSERT_EQ(nelements, stage1_processed);
  ASSERT_EQ(nelements, stage2_processed);
  ASSERT_EQ(0u, initial_queue.length());
  ASSERT_EQ(0u, start_queue.length());
  ASSERT_EQ(0u, middle_queue.length());
  ASSERT_EQ(nelements, final_queue.length());
  while (final_queue.pop() != NULL) {}

  FREE_C_HEAP_ARRAY(Element, elements);
}
