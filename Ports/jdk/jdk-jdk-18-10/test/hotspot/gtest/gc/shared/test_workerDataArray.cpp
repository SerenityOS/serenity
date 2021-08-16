/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/workerDataArray.inline.hpp"
#include "memory/resourceArea.hpp"
#include "unittest.hpp"
#include "utilities/ostream.hpp"

static const double epsilon = 0.0001;

template<typename T>
class WorkerDataArrayTest : public ::testing::Test {
 protected:
  WorkerDataArrayTest() :
    title("Test array"),
    array(NULL, title, 3),
    sub_item_title("Sub item array") {

    array.create_thread_work_items(sub_item_title);
  }

  const char* print_summary() {
    stringStream out;
    array.print_summary_on(&out);
    return out.as_string();
  }

  const char* print_details() {
    stringStream out;
    array.print_details_on(&out);
    return out.as_string();
  }

  const char* print_expected_summary() {
    return prepend_with(title, expected_summary());
  }

  const char* print_expected_details() {
    return prepend_with("", expected_details());
  }

  // returns expected summary for array without uninitialized elements
  // used it because string representation of double depends on locale
  static const char* format_summary(
    T min, double avg, T max, T diff, T sum, size_t workers);

  const char* title;
  WorkerDataArray<T> array;

  const char* sub_item_title;

 private:
  virtual const char* expected_summary() = 0;
  virtual const char* expected_details() = 0;

  static const char* prepend_with(const char* str, const char* orig) {
    stringStream out;
    out.print("%-30s", str);
    out.print("%s", orig);
    return out.as_string();
  }

  ResourceMark rm;
};

template<>
const char* WorkerDataArrayTest<size_t>::format_summary(
  size_t min, double avg, size_t max, size_t diff, size_t sum, size_t workers) {

  stringStream out;
  out.print(" Min: " SIZE_FORMAT
            ", Avg: %4.1lf, Max: " SIZE_FORMAT
            ", Diff: " SIZE_FORMAT ", Sum: " SIZE_FORMAT
            ", Workers: " SIZE_FORMAT "\n",
            min, avg, max, diff, sum, workers);
  return out.as_string();
}

template<>
const char* WorkerDataArrayTest<double>::format_summary(
  double min, double avg, double max, double diff, double sum, size_t workers) {

  stringStream out;
  out.print(" Min: %4.1lf"
            ", Avg: %4.1lf, Max: %4.1lf"
            ", Diff: %4.1lf, Sum: %4.1lf"
            ", Workers: " SIZE_FORMAT "\n",
            min, avg, max, diff, sum, workers);
  return out.as_string();
}

class BasicWorkerDataArrayTest : public WorkerDataArrayTest<size_t> {
 protected:
  BasicWorkerDataArrayTest() {
    array.set(0, 5);
    array.set(1, 3);
    array.set(2, 7);

    array.set_thread_work_item(0, 1);
    array.set_thread_work_item(1, 2);
    array.set_thread_work_item(2, 3);
  }

 private:
  virtual const char* expected_summary() {
    return format_summary(3, 5.0, 7, 4, 15, 3);
  }

  virtual const char* expected_details() {
    return "  5  3  7\n";
  }
};

TEST_VM_F(BasicWorkerDataArrayTest, sum_test) {
  ASSERT_EQ(15u, array.sum());
  ASSERT_EQ(6u, array.thread_work_items(0)->sum());
}

TEST_VM_F(BasicWorkerDataArrayTest, average_test) {
  ASSERT_NEAR(5.0, array.average(), epsilon);
  ASSERT_NEAR(2.0, array.thread_work_items(0)->average(), epsilon);
}

TEST_VM_F(BasicWorkerDataArrayTest, print_summary_on_test) {
  ASSERT_STREQ(print_expected_summary(), print_summary());
}

TEST_VM_F(BasicWorkerDataArrayTest, print_details_on_test) {
  ASSERT_STREQ(print_expected_details(), print_details());
}

class AddWorkerDataArrayTest : public WorkerDataArrayTest<size_t> {
 protected:
  AddWorkerDataArrayTest() {
    array.set(0, 5);
    array.set(1, 3);
    array.set(2, 7);

    for (uint i = 0; i < 3; i++) {
      array.add(i, 1);
    }

    WorkerDataArray<size_t>* sub_items = array.thread_work_items(0);

    sub_items->set(0, 1);
    sub_items->set(1, 2);
    sub_items->set(2, 3);

    for (uint i = 0; i < 3; i++) {
      array.add_thread_work_item(i, 1);
    }
  }

 private:
  virtual const char* expected_summary() {
    return format_summary(4, 6.0, 8, 4, 18, 3);
  }

  virtual const char* expected_details() {
    return "  6  4  8\n";
  }
};

TEST_VM_F(AddWorkerDataArrayTest, sum_test) {
  ASSERT_EQ(18u, array.sum());
  ASSERT_EQ(9u, array.thread_work_items(0)->sum());
}

TEST_VM_F(AddWorkerDataArrayTest, average_test) {
  ASSERT_NEAR(6.0, array.average(), epsilon);
  ASSERT_NEAR(3.0, array.thread_work_items(0)->average(), epsilon);
}

TEST_VM_F(AddWorkerDataArrayTest, print_summary_on_test) {
  ASSERT_STREQ(print_expected_summary(), print_summary());
}

TEST_VM_F(AddWorkerDataArrayTest, print_details_on_test) {
  ASSERT_STREQ(print_expected_details(), print_details());
}

class UninitializedElementWorkerDataArrayTest : public WorkerDataArrayTest<size_t> {
 protected:
  UninitializedElementWorkerDataArrayTest() {
    array.set(0, 5);
    array.set(1, WorkerDataArray<size_t>::uninitialized());
    array.set(2, 7);
  }

 private:
  virtual const char* expected_summary() {
    return format_summary(5, 6.0, 7, 2, 12, 2);
  }

  virtual const char* expected_details() {
    return "  5 -  7\n";
  }
};

TEST_VM_F(UninitializedElementWorkerDataArrayTest, sum_test) {
  ASSERT_EQ(12u, array.sum());
}

TEST_VM_F(UninitializedElementWorkerDataArrayTest, average_test) {
  ASSERT_NEAR(6.0, array.average(), epsilon);
}

TEST_VM_F(UninitializedElementWorkerDataArrayTest, print_summary_on_test) {
  ASSERT_STREQ(print_expected_summary(), print_summary());
}

TEST_VM_F(UninitializedElementWorkerDataArrayTest, print_details_on_test) {
  ASSERT_STREQ(print_expected_details(), print_details());
}

class UninitializedWorkerDataArrayTest : public WorkerDataArrayTest<size_t> {
 protected:
  UninitializedWorkerDataArrayTest() {
    array.set(0, WorkerDataArray<size_t>::uninitialized());
    array.set(1, WorkerDataArray<size_t>::uninitialized());
    array.set(2, WorkerDataArray<size_t>::uninitialized());
  }

 private:
  virtual const char* expected_summary() {
    return " skipped\n";
  }

  virtual const char* expected_details() {
    return " - - -\n";
  }
};

TEST_VM_F(UninitializedWorkerDataArrayTest, sum_test) {
  ASSERT_EQ(0u, array.sum());
}

TEST_VM_F(UninitializedWorkerDataArrayTest, average_test) {
  ASSERT_NEAR(0.0, array.average(), epsilon);
}

TEST_VM_F(UninitializedWorkerDataArrayTest, print_summary_on_test) {
  ASSERT_STREQ(print_expected_summary(), print_summary());
}

TEST_VM_F(UninitializedWorkerDataArrayTest, print_details_on_test) {
  ASSERT_STREQ(print_expected_details(), print_details());
}

class UninitializedDoubleElementWorkerDataArrayTest : public WorkerDataArrayTest<double> {
 protected:
  UninitializedDoubleElementWorkerDataArrayTest() {
    array.set(0, 5.1 / MILLIUNITS);
    array.set(1, WorkerDataArray<double>::uninitialized());
    array.set(2, 7.2 / MILLIUNITS);
  }

 private:
  virtual const char* expected_summary() {
    return format_summary(5.1, 6.1, 7.2, 2.1, 12.3, 2);
  }

  virtual const char* expected_details() {
    stringStream out;
    out.print(" %4.1lf - %4.1lf\n", 5.1, 7.2);
    return out.as_string();
  }
};

TEST_VM_F(UninitializedDoubleElementWorkerDataArrayTest, sum_test) {
  ASSERT_NEAR(12.3 / MILLIUNITS, array.sum(), epsilon);
}

TEST_VM_F(UninitializedDoubleElementWorkerDataArrayTest, average_test) {
  ASSERT_NEAR(6.15 / MILLIUNITS, array.average(), epsilon);
}

TEST_VM_F(UninitializedDoubleElementWorkerDataArrayTest, print_summary_on_test) {
  ASSERT_STREQ(print_expected_summary(), print_summary());
}

TEST_VM_F(UninitializedDoubleElementWorkerDataArrayTest, print_details_on_test) {
  ASSERT_STREQ(print_expected_details(), print_details());
}
