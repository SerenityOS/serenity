/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/CrashTest.h>
#include <sys/wait.h>
#include <unistd.h>

namespace Test {

Crash::Crash(String test_type, Function<Crash::Failure()> crash_function)
    : m_type(test_type)
    , m_crash_function(move(crash_function))
{
}

void Crash::run(RunType run_type = RunType::UsingChildProcess)
{
    printf("\x1B[33mTesting\x1B[0m: \"%s\"\n", m_type.characters());

    auto run_crash_and_print_if_error = [this]() {
        auto failure = m_crash_function();

        // If we got here something went wrong
        printf("\x1B[31mFAIL\x1B[0m: ");
        switch (failure) {
        case Failure::DidNotCrash:
            printf("Did not crash!\n");
            break;
        case Failure::UnexpectedError:
            printf("Unexpected error!\n");
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    };

    if (run_type == RunType::UsingCurrentProcess) {
        run_crash_and_print_if_error();
    } else {

        // Run the test in a child process so that we do not crash the crash program :^)
        pid_t pid = fork();
        if (pid < 0) {
            perror("fork");
            VERIFY_NOT_REACHED();
        } else if (pid == 0) {
            run_crash_and_print_if_error();
            exit(0);
        }

        int status;
        waitpid(pid, &status, 0);
        if (WIFSIGNALED(status))
            printf("\x1B[32mPASS\x1B[0m: Terminated with signal %d\n", WTERMSIG(status));
    }
}

}
