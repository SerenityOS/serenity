/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/AllOf.h>
#include <AK/CharacterTypes.h>
#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Utf8View.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibUnicode/Locale.h>
#include <math.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

enum class NumberFormatType {
    Standard,
    Compact,
};

struct NumberFormat : public Unicode::NumberFormat {
    using Base = Unicode::NumberFormat;

    static Base::Plurality plurality_from_string(StringView plurality)
    {
        if (plurality == "other"sv)
            return Base::Plurality::Other;
        if (plurality == "1"sv)
            return Base::Plurality::Single;
        if (plurality == "zero"sv)
            return Base::Plurality::Zero;
        if (plurality == "one"sv)
            return Base::Plurality::One;
        if (plurality == "two"sv)
            return Base::Plurality::Two;
        if (plurality == "few"sv)
            return Base::Plurality::Few;
        if (plurality == "many"sv)
            return Base::Plurality::Many;
        VERIFY_NOT_REACHED();
    }

    StringIndexType zero_format_index { 0 };
    StringIndexType positive_format_index { 0 };
    StringIndexType negative_format_index { 0 };
    StringIndexType compact_identifier_index { 0 };
};

struct NumberSystem {
    StringIndexType system { 0 };
    HashMap<String, StringIndexType> symbols {};

    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };

    NumberFormat decimal_format {};
    Vector<NumberFormat> decimal_long_formats {};
    Vector<NumberFormat> decimal_short_formats {};

    NumberFormat currency_format {};
    NumberFormat accounting_format {};
    Vector<NumberFormat> currency_unit_formats {};
    Vector<NumberFormat> currency_short_formats {};

    NumberFormat percent_format {};
    NumberFormat scientific_format {};
};

struct Locale {
    HashMap<String, NumberSystem> number_systems;
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;
    HashMap<String, Locale> locales;
    Vector<String> numeric_symbols;
};

static void parse_number_pattern(String pattern, UnicodeLocaleData& locale_data, NumberFormatType type, NumberFormat& format, NumberSystem* number_system_for_groupings = nullptr)
{
    // https://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns
    // https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns
    auto replace_patterns = [&](String pattern) {
        static HashMap<StringView, StringView> replacements = {
            { "{0}"sv, "{number}"sv },
            { "{1}"sv, "{currency}"sv },
            { "%"sv, "{percentSign}"sv },
            { "+"sv, "{plusSign}"sv },
            { "-"sv, "{minusSign}"sv },
            { "¤"sv, "{currency}"sv }, // U+00A4 Currency Sign
            { "E"sv, "{scientificSeparator}"sv },
        };

        for (auto const& replacement : replacements)
            pattern = pattern.replace(replacement.key, replacement.value, true);

        if (auto start_number_index = pattern.find_any_of("#0"sv, String::SearchDirection::Forward); start_number_index.has_value()) {
            auto end_number_index = *start_number_index + 1;

            for (; end_number_index < pattern.length(); ++end_number_index) {
                auto ch = pattern[end_number_index];
                if ((ch != '#') && (ch != '0') && (ch != ',') && (ch != '.'))
                    break;
            }

            if (number_system_for_groupings) {
                auto number_pattern = pattern.substring_view(*start_number_index, end_number_index - *start_number_index);

                auto group_separators = number_pattern.find_all(","sv);
                VERIFY((group_separators.size() == 1) || (group_separators.size() == 2));

                auto decimal = number_pattern.find('.');
                VERIFY(decimal.has_value());

                if (group_separators.size() == 1) {
                    number_system_for_groupings->primary_grouping_size = *decimal - group_separators[0] - 1;
                    number_system_for_groupings->secondary_grouping_size = number_system_for_groupings->primary_grouping_size;
                } else {
                    number_system_for_groupings->primary_grouping_size = *decimal - group_separators[1] - 1;
                    number_system_for_groupings->secondary_grouping_size = group_separators[1] - group_separators[0] - 1;
                }
            }

            pattern = String::formatted("{}{{number}}{}",
                *start_number_index > 0 ? pattern.substring_view(0, *start_number_index) : ""sv,
                pattern.substring_view(end_number_index));

            // This is specifically handled here rather than in the replacements HashMap above so
            // that we do not errantly replace zeroes in number patterns.
            if (pattern.contains(*replacements.get("E"sv)))
                pattern = pattern.replace("0"sv, "{scientificExponent}"sv);
        }

        if (type == NumberFormatType::Compact) {
            static Utf8View whitespace { "\u0020\u00a0"sv };

            Utf8View utf8_pattern { pattern };
            Optional<size_t> start_compact_index;
            Optional<size_t> end_compact_index;
            bool inside_replacement = false;

            for (auto it = utf8_pattern.begin(); it != utf8_pattern.end(); ++it) {
                if (*it == '{') {
                    if (start_compact_index.has_value()) {
                        end_compact_index = utf8_pattern.byte_offset_of(it);
                        break;
                    }

                    inside_replacement = true;
                } else if (*it == '}') {
                    inside_replacement = false;
                } else if (!inside_replacement && !start_compact_index.has_value() && !whitespace.contains(*it)) {
                    start_compact_index = utf8_pattern.byte_offset_of(it);
                }
            }

            if (!start_compact_index.has_value())
                return pattern;

            utf8_pattern = utf8_pattern.substring_view(*start_compact_index, end_compact_index.value_or(pattern.length()) - *start_compact_index);
            utf8_pattern = utf8_pattern.trim(whitespace);

            auto identifier = utf8_pattern.as_string().replace("'.'"sv, "."sv);
            format.compact_identifier_index = locale_data.unique_strings.ensure(move(identifier));

            pattern = pattern.replace(utf8_pattern.as_string(), "{compactIdentifier}");
        }

        return pattern;
    };

    auto patterns = pattern.split(';');
    VERIFY((patterns.size() == 1) || (patterns.size() == 2));

    if (format.magnitude != 0) {
        auto number_of_zeroes_in_pattern = patterns[0].count("0"sv);

        VERIFY(format.magnitude >= number_of_zeroes_in_pattern);
        format.compact_scale = format.magnitude - number_of_zeroes_in_pattern;
    }

    auto zero_format = replace_patterns(move(patterns[0]));
    format.positive_format_index = locale_data.unique_strings.ensure(String::formatted("{{plusSign}}{}", zero_format));

    if (patterns.size() == 2) {
        auto negative_format = replace_patterns(move(patterns[1]));
        format.negative_format_index = locale_data.unique_strings.ensure(move(negative_format));
    } else {
        format.negative_format_index = locale_data.unique_strings.ensure(String::formatted("{{minusSign}}{}", zero_format));
    }

    format.zero_format_index = locale_data.unique_strings.ensure(move(zero_format));
}

static void parse_number_systems(String locale_numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath numbers_path(move(locale_numbers_path));
    numbers_path = numbers_path.append("numbers.json"sv);
    VERIFY(Core::File::exists(numbers_path.string()));

    auto numbers_file_or_error = Core::File::open(numbers_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!numbers_file_or_error.is_error());

    auto numbers = JsonParser(numbers_file_or_error.value()->read_all()).parse();
    VERIFY(numbers.has_value());

    auto const& main_object = numbers->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(numbers_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);

    auto ensure_number_system = [&](auto const& system) -> NumberSystem& {
        return locale.number_systems.ensure(system, [&]() {
            auto system_index = locale_data.unique_strings.ensure(system);
            return NumberSystem { .system = system_index };
        });
    };

    auto parse_number_format = [&](auto const& format_object) {
        Vector<NumberFormat> result;
        result.ensure_capacity(format_object.size());

        format_object.for_each_member([&](auto const& key, JsonValue const& value) {
            auto split_key = key.split_view('-');
            if (split_key.size() != 3)
                return;

            NumberFormat format {};

            if (auto type = split_key[0].template to_uint<u64>(); type.has_value()) {
                VERIFY(*type % 10 == 0);
                format.magnitude = static_cast<u8>(log10(*type));
            } else {
                VERIFY(split_key[0] == "unitPattern"sv);
            }

            format.plurality = NumberFormat::plurality_from_string(split_key[2]);
            parse_number_pattern(value.as_string(), locale_data, NumberFormatType::Compact, format);

            result.append(move(format));
        });

        return result;
    };

    locale_numbers_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        constexpr auto symbols_prefix = "symbols-numberSystem-"sv;
        constexpr auto decimal_formats_prefix = "decimalFormats-numberSystem-"sv;
        constexpr auto currency_formats_prefix = "currencyFormats-numberSystem-"sv;
        constexpr auto percent_formats_prefix = "percentFormats-numberSystem-"sv;
        constexpr auto scientific_formats_prefix = "scientificFormats-numberSystem-"sv;

        if (key.starts_with(symbols_prefix)) {
            auto system = key.substring(symbols_prefix.length());
            auto& number_system = ensure_number_system(system);

            value.as_object().for_each_member([&](auto const& symbol, JsonValue const& localization) {
                auto symbol_index = locale_data.unique_strings.ensure(localization.as_string());
                number_system.symbols.set(symbol, symbol_index);

                if (!locale_data.numeric_symbols.contains_slow(symbol))
                    locale_data.numeric_symbols.append(symbol);
            });
        } else if (key.starts_with(decimal_formats_prefix)) {
            auto system = key.substring(decimal_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string(), locale_data, NumberFormatType::Standard, number_system.decimal_format, &number_system);

            auto const& long_format = value.as_object().get("long"sv).as_object().get("decimalFormat"sv);
            number_system.decimal_long_formats = parse_number_format(long_format.as_object());

            auto const& short_format = value.as_object().get("short"sv).as_object().get("decimalFormat"sv);
            number_system.decimal_short_formats = parse_number_format(short_format.as_object());
        } else if (key.starts_with(currency_formats_prefix)) {
            auto system = key.substring(currency_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string(), locale_data, NumberFormatType::Standard, number_system.currency_format);

            format_object = value.as_object().get("accounting"sv);
            parse_number_pattern(format_object.as_string(), locale_data, NumberFormatType::Standard, number_system.accounting_format);

            number_system.currency_unit_formats = parse_number_format(value.as_object());

            if (value.as_object().has("short"sv)) {
                auto const& short_format = value.as_object().get("short"sv).as_object().get("standard"sv);
                number_system.currency_short_formats = parse_number_format(short_format.as_object());
            }
        } else if (key.starts_with(percent_formats_prefix)) {
            auto system = key.substring(percent_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string(), locale_data, NumberFormatType::Standard, number_system.percent_format);
        } else if (key.starts_with(scientific_formats_prefix)) {
            auto system = key.substring(scientific_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string(), locale_data, NumberFormatType::Standard, number_system.scientific_format);
        }
    });
}

static void parse_all_locales(String core_path, String numbers_path, UnicodeLocaleData& locale_data)
{
    auto numbers_iterator = path_to_dir_iterator(move(numbers_path));

    auto remove_variants_from_path = [&](String path) -> Optional<String> {
        auto parsed_locale = CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, LexicalPath::basename(path));
        if (!parsed_locale.has_value())
            return {};

        StringBuilder builder;
        builder.append(locale_data.unique_strings.get(parsed_locale->language));
        if (auto script = locale_data.unique_strings.get(parsed_locale->script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = locale_data.unique_strings.get(parsed_locale->region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (numbers_iterator.has_next()) {
        auto numbers_path = numbers_iterator.next_full_path();
        VERIFY(Core::File::is_directory(numbers_path));

        auto language = remove_variants_from_path(numbers_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        parse_number_systems(numbers_path, locale_data, locale);
    }

    parse_default_content_locales(move(core_path), locale_data);
}

static String format_identifier(StringView owner, String identifier)
{
    identifier = identifier.replace("-"sv, "_"sv, true);

    if (all_of(identifier, is_ascii_digit))
        return String::formatted("{}_{}", owner[0], identifier);
    if (is_ascii_lower_alpha(identifier[0]))
        return String::formatted("{:c}{}", to_ascii_uppercase(identifier[0]), identifier.substring_view(1));
    return identifier;
}

static void generate_unicode_locale_header(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode {
)~~~");

    generate_enum(generator, format_identifier, "NumericSymbol"sv, {}, locale_data.numeric_symbols);

    generator.append(R"~~~(
namespace Detail {

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, StringView numeric_symbol);
Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system);
Optional<NumberFormat> get_standard_number_system_format(StringView locale, StringView system, StandardNumberFormatType type);
Vector<NumberFormat> get_compact_number_system_formats(StringView locale, StringView system, CompactNumberFormatType type);
Optional<NumericSymbol> numeric_symbol_from_string(StringView numeric_symbol);

}

}
)~~~");

    file.write(generator.as_string_view());
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("numeric_symbols_size", String::number(locale_data.numeric_symbols.size()));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Span.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/UnicodeNumberFormat.h>

namespace Unicode::Detail {
)~~~");

    locale_data.unique_strings.generate(generator);

    generator.append(R"~~~(
struct NumberFormat {
    Unicode::NumberFormat to_unicode_number_format() const {
        Unicode::NumberFormat number_format {};

        number_format.magnitude = magnitude;
        number_format.compact_scale = compact_scale;
        number_format.plurality = static_cast<Unicode::NumberFormat::Plurality>(plurality);
        number_format.zero_format = s_string_list[zero_format];
        number_format.positive_format = s_string_list[positive_format];
        number_format.negative_format = s_string_list[negative_format];
        number_format.compact_identifier = s_string_list[compact_identifier];

        return number_format;
    }

    u8 magnitude { 0 };
    u8 compact_scale { 0 };
    u8 plurality { 0 };
    @string_index_type@ zero_format { 0 };
    @string_index_type@ positive_format { 0 };
    @string_index_type@ negative_format { 0 };
    @string_index_type@ compact_identifier { 0 };
};

struct NumberSystem {
    @string_index_type@ system { 0 };
    Array<@string_index_type@, @numeric_symbols_size@> symbols {};

    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };

    NumberFormat decimal_format {};
    Span<NumberFormat const> decimal_long_formats {};
    Span<NumberFormat const> decimal_short_formats {};

    NumberFormat currency_format {};
    NumberFormat accounting_format {};
    Span<NumberFormat const> currency_unit_formats {};
    Span<NumberFormat const> currency_short_formats {};

    NumberFormat percent_format {};
    NumberFormat scientific_format {};
};
)~~~");

    auto append_number_format = [&](auto const& number_format) {
        generator.set("magnitude"sv, String::number(number_format.magnitude));
        generator.set("compact_scale"sv, String::number(number_format.compact_scale));
        generator.set("plurality"sv, String::number(static_cast<u8>(number_format.plurality)));
        generator.set("zero_format"sv, String::number(number_format.zero_format_index));
        generator.set("positive_format"sv, String::number(number_format.positive_format_index));
        generator.set("negative_format"sv, String::number(number_format.negative_format_index));
        generator.set("compact_identifier"sv, String::number(number_format.compact_identifier_index));
        generator.append("{ @magnitude@, @compact_scale@, @plurality@, @zero_format@, @positive_format@, @negative_format@, @compact_identifier@ },");
    };

    auto append_number_formats = [&](String name, auto const& number_formats) {
        generator.set("name"sv, move(name));
        generator.set("size"sv, String::number(number_formats.size()));

        generator.append(R"~~~(
static constexpr Array<NumberFormat, @size@> @name@ { {
    )~~~");

        constexpr size_t max_values_per_row = 10;
        size_t values_in_current_row = 0;

        for (auto const& number_format : number_formats) {
            if (values_in_current_row++ > 0)
                generator.append(" ");

            append_number_format(number_format);

            if (values_in_current_row == max_values_per_row) {
                values_in_current_row = 0;
                generator.append("\n    ");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    auto append_number_systems = [&](String name, auto const& number_systems) {
        auto format_name = [&](StringView system, StringView format) {
            return String::formatted("{}_{}_{}", name, system, format);
        };

        for (auto const& number_system : number_systems) {
            append_number_formats(format_name(number_system.key, "dl"sv), number_system.value.decimal_long_formats);
            append_number_formats(format_name(number_system.key, "ds"sv), number_system.value.decimal_short_formats);
            append_number_formats(format_name(number_system.key, "cu"sv), number_system.value.currency_unit_formats);
            append_number_formats(format_name(number_system.key, "cs"sv), number_system.value.currency_short_formats);
        }

        generator.set("name", name);
        generator.set("size", String::number(number_systems.size()));

        generator.append(R"~~~(
static constexpr Array<NumberSystem, @size@> @name@ { {)~~~");

        for (auto const& number_system : number_systems) {
            generator.set("system"sv, String::number(number_system.value.system));
            generator.set("primary_grouping_size"sv, String::number(number_system.value.primary_grouping_size));
            generator.set("secondary_grouping_size"sv, String::number(number_system.value.secondary_grouping_size));
            generator.set("decimal_long_formats"sv, format_name(number_system.key, "dl"sv));
            generator.set("decimal_short_formats"sv, format_name(number_system.key, "ds"sv));
            generator.set("currency_unit_formats"sv, format_name(number_system.key, "cu"sv));
            generator.set("currency_short_formats"sv, format_name(number_system.key, "cs"sv));
            generator.append(R"~~~(
    { @system@, {)~~~");

            for (auto const& symbol : locale_data.numeric_symbols) {
                auto index = number_system.value.symbols.get(symbol).value_or(0);
                generator.set("index", String::number(index));
                generator.append(" @index@,");
            }

            generator.append(" }, @primary_grouping_size@, @secondary_grouping_size@, ");
            append_number_format(number_system.value.decimal_format);
            generator.append(" @decimal_long_formats@.span(), @decimal_short_formats@.span(), ");
            append_number_format(number_system.value.currency_format);
            generator.append(" ");
            append_number_format(number_system.value.accounting_format);
            generator.append(" @currency_unit_formats@.span(), @currency_short_formats@.span(), ");
            append_number_format(number_system.value.percent_format);
            generator.append(" ");
            append_number_format(number_system.value.scientific_format);
            generator.append(" },");
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    generate_mapping(generator, locale_data.locales, "NumberSystem"sv, "s_number_systems"sv, "s_number_systems_{}", [&](auto const& name, auto const& value) { append_number_systems(name, value.number_systems); });

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, auto const& values) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        for (auto const& value : values)
            hashes.set(value.hash(), format_identifier(enum_title, value));

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes));
    };

    append_from_string("NumericSymbol"sv, "numeric_symbol"sv, locale_data.numeric_symbols);

    generator.append(R"~~~(
static NumberSystem const* find_number_system(StringView locale, StringView system)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return nullptr;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto const& number_systems = s_number_systems.at(locale_index);

    for (auto const& number_system : number_systems) {
        if (system == s_string_list[number_system.system])
            return &number_system;
    };

    return nullptr;
}

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, StringView symbol)
{
    auto symbol_value = numeric_symbol_from_string(symbol);
    if (!symbol_value.has_value())
        return {};

    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        auto symbol_index = to_underlying(*symbol_value);
        return s_string_list[number_system->symbols[symbol_index]];
    }

    return {};
}

Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system)
{
    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr)
        return NumberGroupings { number_system->primary_grouping_size, number_system->secondary_grouping_size };
    return {};
}

Optional<Unicode::NumberFormat> get_standard_number_system_format(StringView locale, StringView system, StandardNumberFormatType type)
{
    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        switch (type) {
        case StandardNumberFormatType::Decimal:
            return number_system->decimal_format.to_unicode_number_format();
        case StandardNumberFormatType::Currency:
            return number_system->currency_format.to_unicode_number_format();
        case StandardNumberFormatType::Accounting:
            return number_system->accounting_format.to_unicode_number_format();
        case StandardNumberFormatType::Percent:
            return number_system->percent_format.to_unicode_number_format();
        case StandardNumberFormatType::Scientific:
            return number_system->scientific_format.to_unicode_number_format();
        }
    }

    return {};
}

Vector<Unicode::NumberFormat> get_compact_number_system_formats(StringView locale, StringView system, CompactNumberFormatType type)
{
    Vector<Unicode::NumberFormat> formats;

    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        Span<NumberFormat const> number_formats;

        switch (type) {
        case CompactNumberFormatType::DecimalLong:
            number_formats = number_system->decimal_long_formats;
            break;
        case CompactNumberFormatType::DecimalShort:
            number_formats = number_system->decimal_short_formats;
            break;
        case CompactNumberFormatType::CurrencyUnit:
            number_formats = number_system->currency_unit_formats;
            break;
        case CompactNumberFormatType::CurrencyShort:
            number_formats = number_system->currency_short_formats;
            break;
        }

        formats.ensure_capacity(number_formats.size());

        for (auto const& number_format : number_formats)
            formats.append(number_format.to_unicode_number_format());
    }

    return formats;
}

}
)~~~");

    file.write(generator.as_string_view());
}

int main(int argc, char** argv)
{
    char const* generated_header_path = nullptr;
    char const* generated_implementation_path = nullptr;
    char const* core_path = nullptr;
    char const* numbers_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(numbers_path, "Path to cldr-numbers directory", "numbers-path", 'n', "numbers-path");
    args_parser.parse(argc, argv);

    auto open_file = [&](StringView path, StringView flags, Core::OpenMode mode = Core::OpenMode::ReadOnly) {
        if (path.is_empty()) {
            warnln("{} is required", flags);
            args_parser.print_usage(stderr, argv[0]);
            exit(1);
        }

        auto file_or_error = Core::File::open(path, mode);
        if (file_or_error.is_error()) {
            warnln("Failed to open {}: {}", path, file_or_error.release_error());
            exit(1);
        }

        return file_or_error.release_value();
    };

    auto generated_header_file = open_file(generated_header_path, "-h/--generated-header-path", Core::OpenMode::ReadWrite);
    auto generated_implementation_file = open_file(generated_implementation_path, "-c/--generated-implementation-path", Core::OpenMode::ReadWrite);

    UnicodeLocaleData locale_data;
    parse_all_locales(core_path, numbers_path, locale_data);

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
