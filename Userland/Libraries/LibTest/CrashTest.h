/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * Copyright (c) 2021, Brian Gianforaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/String.h>
#include <AK/Variant.h>

namespace Test {

class Crash {
public:
    enum class SuccessCondition {
        DidCrash,
        DidNotCrash,
    };

    enum class RunType {
        UsingChildProcess,
        UsingCurrentProcess,
    };

    enum class Failure {
        DidNotCrash,
        UnexpectedError,
    };

    Crash(String test_type, Function<Crash::Failure()> crash_function,
        SuccessCondition success_condition = SuccessCondition::DidCrash);

    bool run(RunType run_type = RunType::UsingChildProcess);

private:
    using report_type = Variant<Failure, int>;
    bool do_report(report_type report);

    String m_type;
    Function<Crash::Failure()> m_crash_function;
    SuccessCondition m_success_condition;
};

}
