#pragma once

/* TestResult signals to the TestSuite how the TestCase execution went.
 */
enum class TestResult {
    NotRun,
    Passed,   // test function ran to completion without setting any of the below flags:
    Failed,   // didn't get through EXPECT(...)
    Rejected, // didn't get through the ASSUME(...) filter 15 times in a row (in a randomized test)
              // alternatively, user used REJECT(...)
    Overrun,  // ran out of RandomRun data (in a randomized test, when shrinking)
    HitLimit, // hit RandomRun length limit (in a randomized test, when generating)
};
