/*
 * Copyright (c) 2023, Gurkirat Singh <tbhaxor@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/DeprecatedString.h>
#include <AK/StringBuilder.h>

#include <ctype.h>

#include <LibCore/ArgsParser.h>

#include <LibMain/Main.h>

AK::ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    AK::DeprecatedString text {};
    AK::DeprecatedString format {};
    AK::DeprecatedString glue {};
    bool single_page {};

    Core::ArgsParser parser;
    parser.set_general_help("Slugify is a simple text to slug transform utility\n\t$ slugify 'Serenity is a cool ### PROject123.'");
    parser.add_positional_argument(text, "Source text to slugify", "text");
    parser.add_option(single_page, "Prepend hash (#) to the link for navigating on same page like GitHub READMEs (default: false)", "single-page", 's');
    parser.add_option(glue, "Specifies joining string (default: -)", "glue", 'g', "GLUE");
    parser.add_option(format, "Output format to use, choices: html, href, md, rst (default: href)", "format", 'f', "FORMAT");
    parser.parse(arguments);

    // Populate default values
    if (glue.is_empty()) {
        glue = "-";
    }
    if (format.is_empty()) {
        format = "href";
    }

    // Clean unwanted filters
    AK::StringBuilder clean_builder {};
    bool just_processed_space = false;
    for (size_t i = 0; i < text.length(); i++) {
        if (isalnum(text[i]) || text[i] == '_' || text[i] == '-') {
            clean_builder.append_as_lowercase(text[i]);
            just_processed_space = false;
        } else if (text[i] == ' ' && !just_processed_space) {
            clean_builder.append(glue);
            just_processed_space = true;
        }
    }

    // Removes trailing hyphen (-) symbol
    AK::DeprecatedString cleaned_text = clean_builder.to_deprecated_string();
    while (cleaned_text.ends_with('-')) {
        cleaned_text = cleaned_text.substring(0, cleaned_text.length() - 1);
    }

    // Handle single page or relative page link prefixes
    DeprecatedString link_prepend = single_page ? "#" : "/";
    if (format.matches(AK::DeprecatedString("rst"))) {
        AK::outln("\\`{} <{}{}>\\`_", text, link_prepend, cleaned_text);
    } else if (format.matches(AK::DeprecatedString("href"))) {
        AK::outln("{}{}", link_prepend, cleaned_text);
    } else if (format.matches(AK::DeprecatedString("md"))) {
        AK::outln("[{}]({}{})", text, link_prepend, cleaned_text);
    } else if (format.matches(AK::DeprecatedString("html"))) {
        AK::outln("<a href=\"{}{}\">{}</a>", link_prepend, cleaned_text, text);
    } else {
        AK::warnln("Format type not supported, found {}", format);
        return ENOTSUP;
    }

    return 0;
}
