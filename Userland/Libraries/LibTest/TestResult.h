/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Test {

// TestResult signals to the TestSuite how the TestCase execution went.
enum class TestResult {
    NotRun,

    // Test fn ran to completion without setting any of the below flags
    Passed,

    // Didn't get through EXPECT(...).
    Failed,

    // Ran out of RandomRun data (in a randomized test, when shrinking).
    // This is fine, we'll just try some other shrink.
    Overrun,
};

// Used eg. to signal we've ran out of prerecorded random bits.
// Defined in TestSuite.cpp
void set_current_test_result(TestResult);

} // namespace Test
