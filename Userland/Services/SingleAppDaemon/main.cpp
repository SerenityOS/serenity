/*
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2021, Nick Vella <nick@nxk.io>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <serenity.h>

bool run_as_command(String const& run_input);

static AK::String determine_single_app_mode()
{

    auto f = Core::File::construct("/proc/single_app");
    if (!f->open(Core::OpenMode::ReadOnly)) {
        dbgln("Failed to read single_app: {}", f->error_string());
        return "no";
    }
    const String single_app = String::copy(f->read_all(), Chomp);
    if (f->error()) {
        dbgln("Failed to read single_app: {}", f->error_string());
        return "no";
    }

    dbgln("Read single_app: {}", single_app);

    return single_app;
}

// The following function was taken from Userland/Applications/Run
bool run_as_command(String const& run_input)
{
    pid_t child_pid;
    char const* shell_executable = "/bin/Shell"; // TODO query and use the user's preferred shell.
    char const* argv[] = { shell_executable, "-c", run_input.characters(), nullptr };

    if ((errno = posix_spawn(&child_pid, shell_executable, nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        return false;
    }

    // Command spawned in child shell. Hide and wait for exit code.
    int status;
    if (waitpid(child_pid, &status, 0) < 0)
        return false;

    int child_error = WEXITSTATUS(status);
    dbgln("Child shell exited with code {}", child_error);

    // 127 is typically the shell indicating command not found. 126 for all other errors.
    if (child_error == 126 || child_error == 127) {
        return false;
    }

    return true;
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath proc exec"));
    AK::String app = determine_single_app_mode();
    bool result = run_as_command(app);
    if (result) {
    } else {
        dbgln("An error occured launching the specified application (does the path exist?). Please check your kernel cmdline and dmesg.");
    }
    run_as_command("/bin/shutdown --now");
    VERIFY_NOT_REACHED();
}
