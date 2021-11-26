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

int main(int argc, char** argv)
{
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
        if (error.is_syscall())
            warnln("Runtime error: {}: {} (errno={})", error.string_literal(), strerror(error.code()), error.code());
        else if (error.is_errno())
            warnln("Runtime error: {} (errno={})", strerror(error.code()), error.code());
        else
            warnln("Runtime error: {}", error.string_literal());
        return 1;
    }
    return result.value();
}
