/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Error.h>
#include <AK/Slugify.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>
#include <LibUnicode/Normalize.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Vector<String> inputs;
    String output_type;
    char glue = '-';
    bool spa = false;

    Core::ArgsParser parser;
    parser.set_general_help("Slugify is a simple text to slug transform utility\n$ slugify 'Serenity is a cool ### PROject123.'");
    parser.add_option(output_type, "Output format to choose from 'md', 'html', 'plain'. (default: md)", "format", 'f', "FORMAT");
    parser.add_option(Core::ArgsParser::Option {
        .help_string = "Specify delimiter to join the parts. (default: -)",
        .long_name = "glue",
        .short_name = 'g',
        .value_name = "GLUE",
        .accept_value = [&glue](StringView s) -> ErrorOr<bool> {
            if (s.length() == 1 && is_ascii_printable(s[0])) {
                glue = s[0];
                return true;
            }
            return false;
        } });
    parser.add_option(spa, "Prepends hash/pound (#) to the slugified string when set, otherwise slash (/). Useful for markdowns like in GitHub (default: false)", "single-page", 's');
    parser.add_positional_argument(inputs, "Input strings to be slugified.", "inputs");
    if (!parser.parse(arguments)) {
        parser.print_usage(stderr, arguments.strings[0]);
        return 1;
    }

    if (is_ascii_space(glue)) {
        return Error::from_string_view("Glue cannot be a space character."sv);
    }

    auto prepend_char = spa ? '#' : '/';
    for (auto& input : inputs) {
        auto slugified = TRY(slugify(Unicode::normalize(input, Unicode::NormalizationForm::NFD), glue));

        if (output_type.is_empty() || output_type == "md") {
            outln("[{}]({}{})", input, prepend_char, slugified);
        } else if (output_type == "html") {
            outln("<a href='{}{}'>{}</a>", prepend_char, slugified, input);
        } else {
            outln("{}{}", prepend_char, input);
        }
    }
    return 0;
}
