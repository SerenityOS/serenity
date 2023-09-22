#pragma once

/* TestResult signals to the TestSuite how the TestCase execution went.
 */
enum class TestResult {
    NotRun,
    Passed,   // Test fn ran to completion without setting any of the below flags:
    Failed,   // Didn't get through EXPECT(...).
    Rejected, // Didn't get through the ASSUME(...) filter 15 times in a row
              // (in a randomized test).
              // Alternatively, user used REJECT(...).
    Overrun,  // Ran out of RandomRun data (in a randomized test, when shrinking).
              // This is fine, we'll just try some other shrink.
};
