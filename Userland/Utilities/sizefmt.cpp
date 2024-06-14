/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/CharacterTypes.h>
#include <AK/Checked.h>
#include <AK/NumberFormat.h>
#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static ErrorOr<u64> use_integer_with_char_suffix(StringView argument, size_t suffix_length)
{
    if (suffix_length >= argument.length())
        return Error::from_string_literal("Invalid value was specified");
    argument = argument.substring_view(0, argument.length() - suffix_length);

    Optional<u64> numeric_optional = argument.to_number<u64>();
    if (numeric_optional.has_value())
        return numeric_optional.release_value();
    return Error::from_string_literal("Invalid value was specified");
}

static ErrorOr<u64> handle_char_suffix(char suffix, AK::HumanReadableBasedOn human_readable_based_on)
{
    u64 suffix_multiplier = 1;
    switch (suffix) {
    case 'k':
    case 'K':
        suffix_multiplier = (human_readable_based_on == AK::HumanReadableBasedOn::Base2 ? KiB : KB);
        break;
    case 'M':
        suffix_multiplier = (human_readable_based_on == AK::HumanReadableBasedOn::Base2 ? MiB : MB);
        break;
    case 'G':
        suffix_multiplier = (human_readable_based_on == AK::HumanReadableBasedOn::Base2 ? GiB : GB);
        break;
    case 'T':
        suffix_multiplier = (human_readable_based_on == AK::HumanReadableBasedOn::Base2 ? TiB : TB);
        break;
    default:
        return Error::from_string_literal("Unknown size suffix");
    }
    return suffix_multiplier;
}

static ErrorOr<u64> multiply_number_with_suffix(u64 numeric_value_without_suffix, char suffix, AK::HumanReadableBasedOn human_readable_based_on)
{
    auto suffix_multiplier = TRY(handle_char_suffix(suffix, human_readable_based_on));
    if (Checked<u64>::multiplication_would_overflow(numeric_value_without_suffix, suffix_multiplier))
        return Error::from_string_literal("Numeric value multiplication would overflow");
    return numeric_value_without_suffix * suffix_multiplier;
}

static ErrorOr<u64> handle_number(StringView argument)
{
    if (auto number_with_no_suffix = use_integer_with_char_suffix(argument, 0); !number_with_no_suffix.is_error())
        return number_with_no_suffix.release_value();

    if (argument.ends_with("iB"sv)) {
        auto numeric_value_without_suffix = TRY(use_integer_with_char_suffix(argument, 3));
        auto suffix = argument[argument.length() - 3];
        return multiply_number_with_suffix(numeric_value_without_suffix, suffix, AK::HumanReadableBasedOn::Base2);
    }

    if (argument.ends_with("B"sv)) {
        auto numeric_value_without_suffix = TRY(use_integer_with_char_suffix(argument, 2));
        auto suffix = argument[argument.length() - 2];
        return multiply_number_with_suffix(numeric_value_without_suffix, suffix, AK::HumanReadableBasedOn::Base10);
    }

    return Error::from_string_literal("Invalid value was specified");
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    StringView argument;

    Core::ArgsParser args_parser;
    args_parser.set_general_help(
        "Show the 'real' size of a number with a suffix (possibly).");
    args_parser.add_positional_argument(argument, "Number with possibly a suffix", "number");
    args_parser.parse(arguments);

    if (argument.is_null() || argument.is_empty())
        return Error::from_string_literal("Invalid value");

    outln("{}", TRY(handle_number(argument)));
    return 0;
}
