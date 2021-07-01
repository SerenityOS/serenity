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

Crash::Crash(String test_type, Function<Crash::Failure()> crash_function)
    : m_type(move(test_type))
    , m_crash_function(move(crash_function))
{
}

bool Crash::run(RunType run_type)
{
    outln("\x1B[33mTesting\x1B[0m: \"{}\"", m_type);

    auto run_crash_and_print_if_error = [this]() -> bool {
        auto failure = m_crash_function();

        // If we got here something went wrong
        out("\x1B[31mFAIL\x1B[0m: ");
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
        return false;
    };

    if (run_type == RunType::UsingCurrentProcess) {
        return run_crash_and_print_if_error();
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
            run_crash_and_print_if_error();
            exit(0);
        }

        int status;
        waitpid(pid, &status, 0);
        if (WIFSIGNALED(status)) {
            outln("\x1B[32mPASS\x1B[0m: Terminated with signal {}", WTERMSIG(status));
            return true;
        }
        return false;
    }
}

}
