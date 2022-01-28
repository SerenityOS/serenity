/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibMain/Main.h>
#include <string.h>
#include <time.h>

int main(int argc, char** argv)
{
    tzset();

    Vector<StringView> arguments;
    arguments.ensure_capacity(argc);
    for (int i = 0; i < argc; ++i)
        arguments.unchecked_append(argv[i]);

    auto result = serenity_main({
        .argc = argc,
        .argv = argv,
        .strings = arguments.span(),
    });
    if (result.is_error()) {
        auto error = result.release_error();
        warnln("\033[31;1mRuntime error\033[0m: {}", error);
#ifdef __serenity__
        dbgln("\033[31;1mExiting with runtime error\033[0m: {}", error);
#endif
        return 1;
    }
    return result.value();
}
