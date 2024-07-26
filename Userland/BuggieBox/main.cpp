/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Tim Schumacher <timschumi@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibMain/Main.h>
#include <LibShell/Shell.h>

#define ENUMERATE_UTILITIES(E, ALIAS) \
    ALIAS(b2sum, checksum)            \
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
    E(init)                           \
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
#define TOOL_NAME(name) #name##sv,
#define ALIAS_NAME(alias, name) #alias##sv,
    auto const tool_names = {
        ENUMERATE_UTILITIES(TOOL_NAME, ALIAS_NAME)
    };
#undef TOOL_NAME
#undef ALIAS_NAME

    outln(stderr);
    outln(stderr, "Usage:");
    outln(stderr, "* Specify a utility as an argument:");
    outln(stderr, "  $ BuggieBox UTILITY");
    outln(stderr, "* Create a symbolic link with the target being this binary,");
    outln(stderr, "  and ensure the basename is one of the supported utilities' name.");

    outln(stderr);
    outln(stderr, "The following utilities are supported:");
    int count = 0;
    for (auto const& tool_name : tool_names) {
        if (++count % 5 == 1)
            out(stderr, "\n\t");
        out(stderr, "{:12}", tool_name);
    }
    outln(stderr);
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
        outln(stderr, "Detected directly running BuggieBox without specifying a utility.");
        fail();
        return 1;
    }
    bool found_runner = false;
    LexicalPath runbase { arguments.strings[0] };
    auto result = TRY(run_program(arguments, runbase, found_runner));
    if (!found_runner) {
        outln(stderr, "'{}' is not supported by BuggieBox.", runbase);
        fail();
    }
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
