/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Process.h>
#include <LibTest/TestCase.h>

TEST_CASE(crash_on_api_misuse)
{
    {
        auto process = TRY_OR_FAIL(Core::Process::spawn({ .executable = "/bin/true",
            .keep_as_child = Core::KeepAsChild::No }));

        EXPECT_CRASH("calling wait_for_termination() on disowned child", [&] {
            EXPECT(!process.wait_for_termination().is_error());
            return Test::Crash::Failure::DidNotCrash;
        });

        EXPECT_CRASH("calling take_pid() on disowned child", [&] {
            process.take_pid();
            return Test::Crash::Failure::DidNotCrash;
        });
    }

    {
        auto process = TRY_OR_FAIL(Core::Process::spawn({ .executable = "/bin/true",
            .keep_as_child = Core::KeepAsChild::Yes }));

        EXPECT_CRASH("calling take_pid() after wait_for_termination()", [&] {
            EXPECT(!process.wait_for_termination().is_error());
            process.take_pid();
            return Test::Crash::Failure::DidNotCrash;
        });

        EXPECT_CRASH("calling wait_for_termination() after take_pid()", [&] {
            EXPECT(!process.wait_for_termination().is_error());
            process.take_pid();
            return Test::Crash::Failure::DidNotCrash;
        });
        // This creates a zombie process.
        process.take_pid();
    }

    EXPECT_CRASH("Require explicit call to wait_for_termination() of take_pid()", [&] {
        {
            auto maybe_process = Core::Process::spawn(
                { .executable = "/bin/true",
                    .keep_as_child = Core::KeepAsChild::Yes });
            EXPECT(!maybe_process.is_error());
        }
        return Test::Crash::Failure::DidNotCrash;
    });
}

TEST_CASE(no_crash)
{
    {
        auto process = TRY_OR_FAIL(Core::Process::spawn({ .executable = "/bin/true",
            .keep_as_child = Core::KeepAsChild::No }));
    }
    {
        auto process = TRY_OR_FAIL(Core::Process::spawn({ .executable = "/bin/true",
            .keep_as_child = Core::KeepAsChild::Yes }));
        TRY_OR_FAIL(process.wait_for_termination());
    }
    {
        auto process = TRY_OR_FAIL(Core::Process::spawn({ .executable = "/bin/true",
            .keep_as_child = Core::KeepAsChild::Yes }));
        // This creates a zombie process.
        process.take_pid();
    }
}
