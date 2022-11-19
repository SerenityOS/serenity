/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <Userland/BuggieBox/Globals.h>
#include <Userland/Libraries/LibC/unistd.h>
#include <Userland/Shell/Shell.h>

bool g_follow_symlinks = false;
bool g_there_was_an_error = false;
bool g_have_seen_action_command = false;

#define ENUMERATE_UTILITIES(E) \
    E(cat)                     \
    E(checksum)                \
    E(chmod)                   \
    E(chown)                   \
    E(cp)                      \
    E(df)                      \
    E(env)                     \
    E(file)                    \
    E(find)                    \
    E(id)                      \
    E(less)                    \
    E(ln)                      \
    E(ls)                      \
    E(lsblk)                   \
    E(mkdir)                   \
    E(mknod)                   \
    E(mount)                   \
    E(mv)                      \
    E(ps)                      \
    E(rm)                      \
    E(sh)                      \
    E(rmdir)                   \
    E(tail)                    \
    E(test)                    \
    E(tree)                    \
    E(umount)                  \
    E(uname)                   \
    E(uniq)

// Declare the entrypoints of all the tools that we delegate to.
// Relying on `decltype(serenity_main)` ensures that we always stay consistent with the `serenity_main` signature.
#define DECLARE_ENTRYPOINT(name) decltype(serenity_main) name##_main;
ENUMERATE_UTILITIES(DECLARE_ENTRYPOINT)
#undef DECLARE_ENTRYPOINT

static void fail()
{
    out(stderr, "Direct running of BuggieBox was detected without finding a proper requested utility.\n");
    out(stderr, "The following programs are supported: uname, env, lsblk, file, df, mount, umount, mkdir, ");
    out(stderr, "rmdir, rm, chown, chmod, cp, ln, ls, mv, cat, md5sum, sha1sum, sha256sum, sha512sum, sh, uniq, id, tail, ");
    out(stderr, "find, less, mknod, ps\n");
    out(stderr, "To use one of these included utilities, create a symbolic link with the target being this binary, and ensure the basename");
    out(stderr, "is included within.\n");
}

static void fail_with_unknown_utility(StringView requested_utility_name)
{
    out(stderr, "Running of BuggieBox was detected without finding the requested utility \"{}\".\n", requested_utility_name);
    out(stderr, "The following programs are supported: uname, env, lsblk, file, df, mount, umount, mkdir, ");
    out(stderr, "rmdir, rm, chown, chmod, cp, ln, ls, mv, cat, md5sum, sha1sum, sha256sum, sha512sum, sh, uniq, id, tail, ");
    out(stderr, "find, less, mknod, ps\n");
    out(stderr, "To use one of these included utilities, create a symbolic link with the target being this binary, and ensure the basename");
    out(stderr, "is included within.\n");
}

struct Runner {
    StringView name;
    ErrorOr<int> (*func)(Main::Arguments arguments) = nullptr;
};

static constexpr Runner s_runners[] = {
#define RUNNER_ENTRY(name) { #name##sv, name##_main },
    ENUMERATE_UTILITIES(RUNNER_ENTRY)
#undef RUNNER_ENTRY

    // Some tools have additional aliases.
    { "md5sum"sv, checksum_main },
    { "sha1sum"sv, checksum_main },
    { "sha256sum"sv, checksum_main },
    { "sha512sum"sv, checksum_main },
    { "Shell"sv, sh_main },
    { "["sv, test_main },
};

static ErrorOr<int> run_program(Main::Arguments arguments, LexicalPath const& runbase, bool& found_runner)
{
    for (auto& runner : s_runners) {
        if (runbase.basename() == runner.name) {
            found_runner = true;
            return runner.func(arguments);
        }
    }
    return 0;
}

static ErrorOr<int> buggiebox_main(Main::Arguments arguments)
{
    if (arguments.argc == 0) {
        fail();
        return 1;
    }
    bool found_runner = false;
    LexicalPath runbase { arguments.strings[0] };
    auto result = TRY(run_program(arguments, runbase, found_runner));
    if (!found_runner) {
        fail_with_unknown_utility(arguments.strings[0]);
        return 1;
    }
    return result;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    if (arguments.strings[0] == "emergency_shell"sv) {
        int stdin_new_fd = TRY(Core::System::open("/dev/tty0"sv, O_RDWR));
        int stdout_new_fd = TRY(Core::System::open("/dev/tty0"sv, O_RDWR));
        int stderr_new_fd = TRY(Core::System::open("/dev/tty0"sv, O_RDWR));

        close(0);
        close(1);
        close(2);

        TRY(Core::System::dup2(stdin_new_fd, 0));
        TRY(Core::System::dup2(stdout_new_fd, 1));
        TRY(Core::System::dup2(stderr_new_fd, 2));
        warnln("===========================================");
        warnln("ERROR: Failed to continue booting.");
        warnln("You have been dropped to the emergency shell.");
        warnln("Run /bin/BuggieBox to view the available utilities.");
        warnln("To continue booting from a specified device, run /bin/BuggieBox init [DEVICE_FILE]");
        warnln("===========================================");
        return sh_main(arguments);
    }

    LexicalPath runbase { arguments.strings[0] };
    if (runbase.basename() == "BuggieBox"sv) {
        Main::Arguments utility_arguments = arguments;
        utility_arguments.argc--;
        utility_arguments.argv++;
        utility_arguments.strings = arguments.strings.slice(1);
        return buggiebox_main(utility_arguments);
    }
    return buggiebox_main(arguments);
}
