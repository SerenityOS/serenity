/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 * Copyright (c) 2020-2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

namespace Test {

enum class Result {
    Pass,
    Fail,
    Skip,
    Crashed,
};

struct Case {
    String name;
    Result result;
    String details;
};

struct Suite {
    String name;
    // A failed test takes precedence over a skipped test, which both have
    // precedence over a passed test
    Result most_severe_test_result { Result::Pass };
    Vector<Case> tests {};
};

struct Counts {
    // Not all of these might be used by a certain test runner, e.g. some
    // do not have a concept of suites, or might not load tests from files.
    unsigned tests_failed { 0 };
    unsigned tests_passed { 0 };
    unsigned tests_skipped { 0 };
    unsigned suites_failed { 0 };
    unsigned suites_passed { 0 };
    unsigned files_total { 0 };
};

}
