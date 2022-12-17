/*
 * Copyright (c) 2022, Eli Youngs <eli.m.youngs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/GenericLexer.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibRegex/RegexMatcher.h>
#include <LibRegex/RegexOptions.h>

struct SubstitutionCommand {
    Regex<PosixExtended> regex;
    StringView replacement;
    PosixOptions options;
};

static ErrorOr<SubstitutionCommand> parse_command(StringView command)
{
    auto generic_error_message = "Incomplete substitution command"sv;

    auto lexer = GenericLexer(command);

    auto address = lexer.consume_until('s');
    if (!address.is_empty())
        warnln("sed: Addresses are currently ignored");

    if (!lexer.consume_specific('s'))
        return Error::from_string_view(generic_error_message);

    if (lexer.is_eof())
        return Error::from_string_view(generic_error_message);

    auto delimiter = lexer.consume();
    if (delimiter == '\n' || delimiter == '\\')
        return Error::from_string_literal("\\n and \\ cannot be used as delimiters.");

    auto pattern = lexer.consume_until(delimiter);
    if (pattern.is_empty())
        return Error::from_string_literal("Substitution patterns cannot be empty.");

    if (!lexer.consume_specific(delimiter))
        return Error::from_string_view(generic_error_message);

    auto replacement = lexer.consume_until(delimiter);

    // According to Posix, "s/x/y" is an invalid substitution command.
    // It must have a closing delimiter: "s/x/y/"
    if (!lexer.consume_specific(delimiter))
        return Error::from_string_literal("The substitution command was not properly terminated.");

    PosixOptions const options = PosixOptions(PosixFlags::Global | PosixFlags::SingleMatch);

    auto flags = lexer.consume_all();
    if (!flags.is_empty())
        warnln("sed: Flags are currently ignored");

    return SubstitutionCommand { Regex<PosixExtended> { pattern }, replacement, options };
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser args_parser;

    StringView command_input;
    Vector<StringView> filepaths;

    args_parser.add_positional_argument(command_input, "Command", "command_input", Core::ArgsParser::Required::Yes);
    args_parser.add_positional_argument(filepaths, "File", "file", Core::ArgsParser::Required::No);

    args_parser.parse(args);

    auto command = TRY(parse_command(command_input));

    if (filepaths.is_empty())
        filepaths = { "-"sv };

    Array<u8, PAGE_SIZE> buffer {};
    for (auto const& filepath : filepaths) {
        auto file_unbuffered = TRY(Core::Stream::File::open_file_or_standard_stream(filepath, Core::Stream::OpenMode::Read));
        auto file = TRY(Core::Stream::BufferedFile::create(move(file_unbuffered)));

        while (!file->is_eof()) {
            auto line = TRY(file->read_line(buffer));

            // Substitutions can apply to blank lines in the middle of a file,
            // but not to the trailing newline that marks the end of a file.
            if (line.is_empty() && file->is_eof())
                break;

            auto result = command.regex.replace(line, command.replacement, command.options);
            outln(result);
        }
    }

    return 0;
}
