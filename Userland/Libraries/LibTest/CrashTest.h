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
    enum class RunType {
        UsingChildProcess,
        UsingCurrentProcess,
    };

    enum class Failure {
        DidNotCrash,
        UnexpectedError,
    };

    static constexpr int ANY_SIGNAL = -1;

    Crash(String test_type, Function<Crash::Failure()> crash_function, int crash_signal = ANY_SIGNAL);

    bool run(RunType run_type = RunType::UsingChildProcess);

private:
    using Report = Variant<Failure, int>;
    bool do_report(Report report);

    String m_type;
    Function<Crash::Failure()> m_crash_function;
    int m_crash_signal;
};

}
