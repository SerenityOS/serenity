/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <unistd.h>

static char backslash_escaped_char(char c)
{
    switch (c) {
    case '\\':
        return c;
    // `\"` produces `"` with printf(1), but `\"` with echo(1)
    case 'a':
        return '\a';
    case 'b':
        return '\b';
    case 'e':
        return '\e';
    case 'f':
        return '\f';
    case 'n':
        return '\n';
    case 'r':
        return '\r';
    case 't':
        return '\t';
    case 'v':
        return '\v';
    default:
        return c;
    }
}

static String interpret_backslash_escapes(String s)
{
    StringBuilder builder;

    for (size_t i = 0; i < s.length();) {
        if (char c = s[i++]; c != '\\') {
            builder.append(c);
            continue;
        }
        if (i == s.length()) {
            // Last character of string is '\' -- output it verbatim.
            builder.append('\\');
        }

        char c = s[i++];
        if (c == 'c') // `\c` suppresses further output.
            break;
        // FIXME: \0ooo, \xHH, \uHHHH, \UHHHHHHHH should produce characters if followed by
        // enough digits.
        builder.append(backslash_escaped_char(c));
    }

    return builder.build();
}

int main(int argc, char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> values;
    bool no_trailing_newline = false;
    bool should_interpret_backslash_escapes = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(no_trailing_newline, "Do not output a trailing newline", nullptr, 'n');
    args_parser.add_option(should_interpret_backslash_escapes, "Interpret backslash escapes", nullptr, 'e');
    args_parser.add_positional_argument(values, "Values to print out", "string", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    String output = String::join(' ', values);
    if (should_interpret_backslash_escapes)
        output = interpret_backslash_escapes(move(output));
    out("{}", output);
    if (!no_trailing_newline)
        outln();
    return 0;
}
