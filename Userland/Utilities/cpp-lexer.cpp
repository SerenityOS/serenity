/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Try.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibCpp/Lexer.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    // FIXME: Remove this once we correctly define a proper set of pledge promises
    // (and if "exec" promise is not one of them).
    TRY(Core::System::prctl(PR_SET_NO_NEW_PRIVS, NO_NEW_PRIVS_MODE_ENFORCED, 0, 0));

    Core::ArgsParser args_parser;
    StringView path;
    args_parser.add_positional_argument(path, "Cpp File", "cpp-file", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    auto file = TRY(Core::Stream::File::open(path, Core::Stream::OpenMode::Read));
    auto content = TRY(file->read_until_eof());
    StringView content_view(content);

    Cpp::Lexer lexer(content);
    lexer.lex_iterable([](auto token) {
        outln("{}", token.to_deprecated_string());
    });

    return 0;
}
