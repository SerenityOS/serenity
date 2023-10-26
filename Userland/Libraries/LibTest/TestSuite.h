/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Macros.h> // intentionally first -- we redefine VERIFY and friends in here

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/Vector.h>
#include <LibTest/TestCase.h>
#include <LibTest/TestResult.h>

namespace Test {

class TestSuite {
public:
    static TestSuite& the()
    {
        if (s_global == nullptr)
            s_global = new TestSuite();
        return *s_global;
    }

    static void release()
    {
        if (s_global)
            delete s_global;
        s_global = nullptr;
    }

    int run(Vector<NonnullRefPtr<TestCase>> const&);
    int main(DeprecatedString const& suite_name, Span<StringView> arguments);
    Vector<NonnullRefPtr<TestCase>> find_cases(DeprecatedString const& search, bool find_tests, bool find_benchmarks);
    void add_case(NonnullRefPtr<TestCase> const& test_case)
    {
        m_cases.append(test_case);
    }

    TestResult current_test_result() const { return m_current_test_result; }
    void set_current_test_result(TestResult result) { m_current_test_result = result; }

    void set_suite_setup(Function<void()> setup) { m_setup = move(setup); }

private:
    static TestSuite* s_global;
    Vector<NonnullRefPtr<TestCase>> m_cases;
    u64 m_testtime = 0;
    u64 m_benchtime = 0;
    DeprecatedString m_suite_name;
    u64 m_benchmark_repetitions = 1;
    Function<void()> m_setup;
    TestResult m_current_test_result = TestResult::NotRun;
};

}
