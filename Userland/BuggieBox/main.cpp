/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibMain/Main.h>
#include <Userland/Shell/Shell.h>

#define ENUMERATE_UTILITIES(E, ALIAS) \
    E(cat)                            \
    E(checksum)                       \
    E(chmod)                          \
    E(chown)                          \
    E(cp)                             \
    E(df)                             \
    E(env)                            \
    E(file)                           \
    E(find)                           \
    E(id)                             \
    E(less)                           \
    E(ln)                             \
    E(ls)                             \
    E(lsblk)                          \
    ALIAS(md5sum, checksum)           \
    E(mkdir)                          \
    E(mknod)                          \
    E(mount)                          \
    E(mv)                             \
    E(ps)                             \
    E(rm)                             \
    E(rmdir)                          \
    E(sh)                             \
    ALIAS(sha1sum, checksum)          \
    ALIAS(sha256sum, checksum)        \
    ALIAS(sha512sum, checksum)        \
    ALIAS(Shell, sh)                  \
    E(tail)                           \
    E(tree)                           \
    E(umount)                         \
    E(uname)                          \
    E(uniq)

// Declare the entrypoints of all the tools that we delegate to.
// Some tools have additional aliases that we skip in the declarations.
// Relying on `decltype(serenity_main)` ensures that we always stay consistent with the `serenity_main` signature.
#define DECLARE_ENTRYPOINT(name) decltype(serenity_main) name##_main;
#define SKIP(alias, name)
ENUMERATE_UTILITIES(DECLARE_ENTRYPOINT, SKIP)
#undef DECLARE_ENTRYPOINT
#undef SKIP

static void fail()
{
    out(stderr, "Direct running of BuggieBox was detected without finding a proper requested utility.\n");
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
#define ALIAS_RUNNER_ENTRY(alias, name) { #alias##sv, name##_main },
    ENUMERATE_UTILITIES(RUNNER_ENTRY, ALIAS_RUNNER_ENTRY)
#undef RUNNER_ENTRY
#undef ALIAS_RUNNER_ENTRY
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
    if (!found_runner)
        fail();
    return result;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
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
