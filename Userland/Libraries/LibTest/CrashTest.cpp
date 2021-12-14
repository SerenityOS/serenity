/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <LibTest/CrashTest.h>
#include <sys/wait.h>
#include <unistd.h>

#ifndef AK_OS_MACOS
#    include <sys/prctl.h>
#endif

namespace Test {

Crash::Crash(String test_type, Function<Crash::Failure()> crash_function,
    SuccessCondition success_condition)
    : m_type(move(test_type))
    , m_crash_function(move(crash_function))
    , m_success_condition(success_condition)
{
}

bool Crash::run(RunType run_type)
{
    outln("\x1B[33mTesting\x1B[0m: \"{}\"", m_type);

    if (run_type == RunType::UsingCurrentProcess) {
        return print_report(m_crash_function());
    } else {
        // Run the test in a child process so that we do not crash the crash program :^)
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork");
            VERIFY_NOT_REACHED();
        } else if (pid == 0) {
#ifndef AK_OS_MACOS
            if (prctl(PR_SET_DUMPABLE, 0, 0) < 0)
                perror("prctl(PR_SET_DUMPABLE)");
#endif
            exit(m_crash_function());
        }

        int status;
        waitpid(pid, &status, 0);
        if (WIFEXITED(status)) {
            return do_report(Failure(WEXITSTATUS(status)));
        }
        if (WIFSIGNALED(status)) {
            return do_report(WTERMSIG(status));
        }
        VERIFY_NOT_REACHED();
    }
}

bool Crash::do_report(report_type report)
{
    bool pass = report.has<int>();
    if (m_success_condition == SuccessCondition::DidCrash) {
        pass = !pass;
    }

    if (pass)
        out("\x1B[32mPASS\x1B[0m: ");
    else
        out("\x1B[31mFAIL\x1B[0m: ");

    report.visit(
        [&](const Failure& failure) {
            switch (failure) {
            case Failure::DidNotCrash:
                outln("Did not crash!");
                break;
            case Failure::UnexpectedError:
                outln("Unexpected error!");
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        },
        [&](const int& signal) {
            outln("Terminated with signal {}", signal);
        });

    return pass;
}

}
