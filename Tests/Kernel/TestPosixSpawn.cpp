/*
 * Copyright (c) 2025, Tomás Simões <tomasprsimoes@tecnico.ulisboa.pt>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/System.h>
#include <LibTest/TestCase.h>

TEST_CASE(test_posix_spawn_bin_true_success)
{
    // Arguments for the spawned process. argv[0] is the program name.
    char* argv[] = { const_cast<char*>("/bin/true"), nullptr };
    int status;

    // Attempt to spawn /bin/true with no fileactions or spawnattr
    constexpr StringView path_argument = "/bin/true"sv;

    auto pid = TRY_OR_FAIL(Core::System::posix_spawn(path_argument, nullptr, nullptr, argv, environ));

    // Wait for the child process to terminate.
    pid_t waited_pid = waitpid(pid, &status, 0);
    EXPECT_EQ(waited_pid, pid);

    EXPECT_EQ(WEXITSTATUS(status), 0);
}
