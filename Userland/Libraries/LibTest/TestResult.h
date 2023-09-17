#pragma once

enum class TestResult {
    NotRun,
    Passed,   // test function ran to completion without setting any of the below flags:
    Failed,   // didn't get through EXPECT(...)
    Rejected, // didn't get through the ASSUME(...) filter 15 times in a row
    Overrun,  // ran out of RandomRun data
    HitLimit, // hit RandomRun length limit
};
