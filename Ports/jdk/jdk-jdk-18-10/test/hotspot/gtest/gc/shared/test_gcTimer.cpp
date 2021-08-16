/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/shared/gcTimer.hpp"
#include "utilities/ticks.hpp"
#include "unittest.hpp"

class GCTimerTest {
 public:
  static void register_gc_start(GCTimer* const timer, jlong ticks) {
    timer->register_gc_start(Ticks(ticks));
  }
  static void register_gc_end(GCTimer* const timer, jlong ticks) {
    timer->register_gc_end(Ticks(ticks));
  }
  static void register_gc_pause_start(GCTimer* const timer, jlong ticks) {
    timer->register_gc_pause_start("pause", Ticks(ticks));
  }
  static void register_gc_pause_end(GCTimer* const timer, jlong ticks) {
    timer->register_gc_pause_end(Ticks(ticks));
  }
  static void register_gc_concurrent_start(ConcurrentGCTimer* const timer, jlong ticks) {
    timer->register_gc_concurrent_start("concurrent", Ticks(ticks));
  }
  static void register_gc_concurrent_end(ConcurrentGCTimer* const timer, jlong ticks) {
    timer->register_gc_concurrent_end(Ticks(ticks));
  }

  static Tickspan duration(jlong ticks) { return Ticks(ticks) - Ticks(0); }
};

static Tickspan duration(jlong ticks) { return GCTimerTest::duration(ticks); }

TEST(GCTimer, start) {
  GCTimer gc_timer;
  GCTimerTest::register_gc_start(&gc_timer, 1);

  EXPECT_EQ(1, gc_timer.gc_start().value());
}

TEST(GCTimer, end) {
  GCTimer gc_timer;

  GCTimerTest::register_gc_start(&gc_timer, 1);
  GCTimerTest::register_gc_end(&gc_timer, 2);

  EXPECT_EQ(2, gc_timer.gc_end().value());
}

TEST(GCTimer, pause) {
  GCTimer gc_timer;

  GCTimerTest::register_gc_start(&gc_timer, 1);
  GCTimerTest::register_gc_pause_start(&gc_timer, 2);
  GCTimerTest::register_gc_pause_end(&gc_timer, 4);
  GCTimerTest::register_gc_end(&gc_timer, 5);

  TimePartitions* partitions = gc_timer.time_partitions();
  EXPECT_EQ(1, partitions->num_phases());
  EXPECT_EQ(duration(2), partitions->sum_of_pauses());

  EXPECT_EQ(5, gc_timer.gc_end().value());
}

TEST(ConcurrentGCTimer, pause) {
  ConcurrentGCTimer gc_timer;

  GCTimerTest::register_gc_start(&gc_timer, 1);
  GCTimerTest::register_gc_pause_start(&gc_timer, 2);
  GCTimerTest::register_gc_pause_end(&gc_timer, 4);
  GCTimerTest::register_gc_end(&gc_timer, 7);

  TimePartitions* partitions = gc_timer.time_partitions();
  EXPECT_EQ(1, partitions->num_phases());
  EXPECT_EQ(duration(2), partitions->sum_of_pauses());

  EXPECT_EQ(7, gc_timer.gc_end().value());
}

TEST(ConcurrentGCTimer, concurrent) {
  ConcurrentGCTimer gc_timer;

  GCTimerTest::register_gc_start(&gc_timer, 1);
  GCTimerTest::register_gc_concurrent_start(&gc_timer, 2);
  GCTimerTest::register_gc_concurrent_end(&gc_timer, 4);
  GCTimerTest::register_gc_end(&gc_timer, 5);

  TimePartitions* partitions = gc_timer.time_partitions();
  EXPECT_EQ(1, partitions->num_phases());
  EXPECT_EQ(duration(0), partitions->sum_of_pauses());

  EXPECT_EQ(5, gc_timer.gc_end().value());
}

class TimePartitionsTest {
 public:

  static void validate_gc_phase(GCPhase* phase, int level, const char* name, const jlong& start, const jlong& end) {
    EXPECT_EQ(level, phase->level());
    EXPECT_STREQ(name, phase->name());
    EXPECT_EQ(start, phase->start().value());
    EXPECT_EQ(end, phase->end().value());
  }

  static void validate_pauses(const TimePartitions& time_partitions, const Tickspan& expected_sum_of_pauses, const Tickspan& expected_longest_pause) {
    EXPECT_EQ(expected_sum_of_pauses, time_partitions.sum_of_pauses());
    EXPECT_EQ(expected_longest_pause, time_partitions.longest_pause());
  }
  static void validate_pauses(const TimePartitions& time_partitions, const Tickspan& expected_pause) {
    validate_pauses(time_partitions, expected_pause, expected_pause);
  }
  static void validate_pauses(const TimePartitions& time_partitions, jlong end, jlong start) {
    validate_pauses(time_partitions, Ticks(end) - Ticks(start));
  }
  static void validate_pauses(const TimePartitions& time_partitions, jlong all_end, jlong all_start, jlong longest_end, jlong longest_start) {
    validate_pauses(time_partitions, Ticks(all_end) - Ticks(all_start), Ticks(longest_end) - Ticks(longest_start));
  }

  static void report_gc_phase_start(TimePartitions* const partitions, const char* name, jlong ticks, GCPhase::PhaseType type=GCPhase::PausePhaseType) {
    partitions->report_gc_phase_start(name, Ticks(ticks), type);
  }

  static void report_gc_phase_end(TimePartitions* const partitions, jlong ticks) {
    partitions->report_gc_phase_end(Ticks(ticks));
  }
};

TEST(TimePartitionPhasesIterator, one_pause) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase", 2);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 8);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase", 2, 8));

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, 8, 2));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}

TEST(TimePartitionPhasesIterator, two_pauses) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase1", 2);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 3);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase2", 4);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 6);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase1", 2, 3));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase2", 4, 6));

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, 3, 0, 2, 0));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}

TEST(TimePartitionPhasesIterator, one_sub_pause_phase) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase", 2);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase", 3);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 4);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 5);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase", 2, 5));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase", 3, 4));

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, 3, 0));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}

TEST(TimePartitionPhasesIterator, max_nested_pause_phases) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase", 2);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase1", 3);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase2", 4);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase3", 5);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 6);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 7);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 8);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 9);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase", 2, 9));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase1", 3, 8));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 2, "SubPhase2", 4, 7));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 3, "SubPhase3", 5, 6));

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, 7, 0));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}

TEST(TimePartitionPhasesIterator, many_sub_pause_phases) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase", 2);

  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase1", 3);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 4);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase2", 5);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 6);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase3", 7);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 8);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase4", 9);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 10);

  TimePartitionsTest::report_gc_phase_end(&time_partitions, 11);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase", 2, 11));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase1", 3, 4));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase2", 5, 6));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase3", 7, 8));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase4", 9, 10));

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, 9, 0));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}

TEST(TimePartitionPhasesIterator, many_sub_pause_phases2) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "PausePhase", 2);

  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase1", 3);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase11", 4);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 5);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase12", 6);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 7);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 8);

  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase2", 9);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase21", 10);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 11);
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase22", 12);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 13);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 14);

  TimePartitionsTest::report_gc_phase_start(&time_partitions, "SubPhase3", 15);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 16);

  TimePartitionsTest::report_gc_phase_end(&time_partitions, 17);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "PausePhase", 2, 17));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase1", 3, 8));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 2, "SubPhase11", 4, 5));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 2, "SubPhase12", 6, 7));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase2", 9, 14));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 2, "SubPhase21", 10, 11));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 2, "SubPhase22", 12, 13));
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 1, "SubPhase3", 15, 16));

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, 15, 0));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}

TEST(TimePartitionPhasesIterator, one_concurrent) {
  TimePartitions time_partitions;
  TimePartitionsTest::report_gc_phase_start(&time_partitions, "ConcurrentPhase", 2, GCPhase::ConcurrentPhaseType);
  TimePartitionsTest::report_gc_phase_end(&time_partitions, 8);

  TimePartitionPhasesIterator iter(&time_partitions);

  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_gc_phase(iter.next(), 0, "ConcurrentPhase", 2, 8));
  // ConcurrentPhaseType should not affect to both 'sum_of_pauses()' and 'longest_pause()'.
  EXPECT_NO_FATAL_FAILURE(TimePartitionsTest::validate_pauses(time_partitions, Tickspan()));

  EXPECT_FALSE(iter.has_next()) << "Too many elements";
}
