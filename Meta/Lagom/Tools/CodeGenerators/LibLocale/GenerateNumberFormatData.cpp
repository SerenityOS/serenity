/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h" // FIXME: Move this somewhere common.
#include <AK/AllOf.h>
#include <AK/Array.h>
#include <AK/ByteString.h>
#include <AK/CharacterTypes.h>
#include <AK/Find.h>
#include <AK/Format.h>
#include <AK/HashFunctions.h>
#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/JsonParser.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/StringBuilder.h>
#include <AK/Traits.h>
#include <AK/Utf8View.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibFileSystem/FileSystem.h>
#include <LibJS/Runtime/Intl/SingleUnitIdentifiers.h>
#include <LibLocale/Locale.h>
#include <LibLocale/NumberFormat.h>
#include <LibLocale/PluralRules.h>
#include <math.h>

enum class NumberFormatType {
    Standard,
    Compact,
};

struct NumberFormat : public Locale::NumberFormat {
    using Base = Locale::NumberFormat;

    unsigned hash() const
    {
        auto hash = pair_int_hash(magnitude, exponent);
        hash = pair_int_hash(hash, to_underlying(plurality));
        hash = pair_int_hash(hash, zero_format_index);
        hash = pair_int_hash(hash, positive_format_index);
        hash = pair_int_hash(hash, negative_format_index);

        for (auto index : identifier_indices)
            hash = pair_int_hash(hash, index);

        return hash;
    }

    bool operator==(NumberFormat const& other) const
    {
        return (magnitude == other.magnitude)
            && (exponent == other.exponent)
            && (plurality == other.plurality)
            && (zero_format_index == other.zero_format_index)
            && (positive_format_index == other.positive_format_index)
            && (negative_format_index == other.negative_format_index)
            && (identifier_indices == other.identifier_indices);
    }

    size_t zero_format_index { 0 };
    size_t positive_format_index { 0 };
    size_t negative_format_index { 0 };
    Vector<size_t> identifier_indices {};
};

template<>
struct AK::Formatter<NumberFormat> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, NumberFormat const& format)
    {
        StringBuilder identifier_indices;
        identifier_indices.join(", "sv, format.identifier_indices);

        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {}, {}, {{ {} }} }}"sv,
            format.magnitude,
            format.exponent,
            to_underlying(format.plurality),
            format.zero_format_index,
            format.positive_format_index,
            format.negative_format_index,
            identifier_indices.to_byte_string());
    }
};

template<>
struct AK::Traits<NumberFormat> : public DefaultTraits<NumberFormat> {
    static unsigned hash(NumberFormat const& f) { return f.hash(); }
};

using NumberFormatList = Vector<size_t>;
using NumericSymbolList = Vector<size_t>;

struct NumberSystem {
    unsigned hash() const
    {
        auto hash = int_hash(symbols);
        hash = pair_int_hash(hash, primary_grouping_size);
        hash = pair_int_hash(hash, secondary_grouping_size);
        hash = pair_int_hash(hash, decimal_format);
        hash = pair_int_hash(hash, decimal_long_formats);
        hash = pair_int_hash(hash, decimal_short_formats);
        hash = pair_int_hash(hash, currency_format);
        hash = pair_int_hash(hash, accounting_format);
        hash = pair_int_hash(hash, currency_unit_formats);
        hash = pair_int_hash(hash, percent_format);
        hash = pair_int_hash(hash, scientific_format);
        return hash;
    }

    bool operator==(NumberSystem const& other) const
    {
        return (symbols == other.symbols)
            && (primary_grouping_size == other.primary_grouping_size)
            && (secondary_grouping_size == other.secondary_grouping_size)
            && (decimal_format == other.decimal_format)
            && (decimal_long_formats == other.decimal_long_formats)
            && (decimal_short_formats == other.decimal_short_formats)
            && (currency_format == other.currency_format)
            && (accounting_format == other.accounting_format)
            && (currency_unit_formats == other.currency_unit_formats)
            && (percent_format == other.percent_format)
            && (scientific_format == other.scientific_format);
    }

    size_t symbols { 0 };

    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };

    size_t decimal_format { 0 };
    size_t decimal_long_formats { 0 };
    size_t decimal_short_formats { 0 };

    size_t currency_format { 0 };
    size_t accounting_format { 0 };
    size_t currency_unit_formats { 0 };

    size_t percent_format { 0 };
    size_t scientific_format { 0 };
};

template<>
struct AK::Formatter<NumberSystem> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, NumberSystem const& system)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {} }}"sv,
            system.symbols,
            system.primary_grouping_size,
            system.secondary_grouping_size,
            system.decimal_format,
            system.decimal_long_formats,
            system.decimal_short_formats,
            system.currency_format,
            system.accounting_format,
            system.currency_unit_formats,
            system.percent_format,
            system.scientific_format);
    }
};

template<>
struct AK::Traits<NumberSystem> : public DefaultTraits<NumberSystem> {
    static unsigned hash(NumberSystem const& s) { return s.hash(); }
};

struct Unit {
    unsigned hash() const
    {
        auto hash = int_hash(unit);
        hash = pair_int_hash(hash, long_formats);
        hash = pair_int_hash(hash, short_formats);
        hash = pair_int_hash(hash, narrow_formats);
        return hash;
    }

    bool operator==(Unit const& other) const
    {
        return (unit == other.unit)
            && (long_formats == other.long_formats)
            && (short_formats == other.short_formats)
            && (narrow_formats == other.narrow_formats);
    }

    size_t unit { 0 };
    size_t long_formats { 0 };
    size_t short_formats { 0 };
    size_t narrow_formats { 0 };
};

template<>
struct AK::Formatter<Unit> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Unit const& system)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {} }}"sv,
            system.unit,
            system.long_formats,
            system.short_formats,
            system.narrow_formats);
    }
};

template<>
struct AK::Traits<Unit> : public DefaultTraits<Unit> {
    static unsigned hash(Unit const& u) { return u.hash(); }
};

struct LocaleData {
    Vector<size_t> number_systems;
    HashMap<ByteString, size_t> units {};
    u8 minimum_grouping_digits { 0 };
};

struct CLDR {
    UniqueStringStorage unique_strings;
    UniqueStorage<NumberFormat> unique_formats;
    UniqueStorage<NumberFormatList> unique_format_lists;
    UniqueStorage<NumericSymbolList> unique_symbols;
    UniqueStorage<NumberSystem> unique_systems;
    UniqueStorage<Unit> unique_units;

    HashMap<ByteString, Array<u32, 10>> number_system_digits;
    Vector<ByteString> number_systems;

    HashMap<ByteString, LocaleData> locales;
    size_t max_identifier_count { 0 };
};

static ErrorOr<void> parse_number_system_digits(ByteString core_supplemental_path, CLDR& cldr)
{
    LexicalPath number_systems_path(move(core_supplemental_path));
    number_systems_path = number_systems_path.append("numberingSystems.json"sv);

    auto number_systems = TRY(read_json_file(number_systems_path.string()));
    auto const& supplemental_object = number_systems.as_object().get_object("supplemental"sv).value();
    auto const& number_systems_object = supplemental_object.get_object("numberingSystems"sv).value();

    number_systems_object.for_each_member([&](auto const& number_system, auto const& digits_object) {
        auto type = digits_object.as_object().get_byte_string("_type"sv).value();
        if (type != "numeric"sv)
            return;

        auto digits = digits_object.as_object().get_byte_string("_digits"sv).value();

        Utf8View utf8_digits { digits };
        VERIFY(utf8_digits.length() == 10);

        auto& number_system_digits = cldr.number_system_digits.ensure(number_system);
        size_t index = 0;

        for (u32 digit : utf8_digits)
            number_system_digits[index++] = digit;

        if (!cldr.number_systems.contains_slow(number_system))
            cldr.number_systems.append(number_system);
    });

    return {};
}

static ByteString parse_identifiers(ByteString pattern, StringView replacement, CLDR& cldr, NumberFormat& format)
{
    static constexpr Utf8View whitespace { "\u0020\u00a0\u200f"sv };

    while (true) {
        Utf8View utf8_pattern { pattern };
        Optional<size_t> start_index;
        Optional<size_t> end_index;
        bool inside_replacement = false;

        for (auto it = utf8_pattern.begin(); it != utf8_pattern.end(); ++it) {
            if (*it == '{') {
                if (start_index.has_value()) {
                    end_index = utf8_pattern.byte_offset_of(it);
                    break;
                }

                inside_replacement = true;
            } else if (*it == '}') {
                inside_replacement = false;
            } else if (!inside_replacement && !start_index.has_value() && !whitespace.contains(*it)) {
                start_index = utf8_pattern.byte_offset_of(it);
            }
        }

        if (!start_index.has_value())
            return pattern;

        end_index = end_index.value_or(pattern.length());

        utf8_pattern = utf8_pattern.substring_view(*start_index, *end_index - *start_index);
        utf8_pattern = utf8_pattern.trim(whitespace);

        auto identifier = utf8_pattern.as_string().replace("'.'"sv, "."sv, ReplaceMode::FirstOnly);
        auto identifier_index = cldr.unique_strings.ensure(move(identifier));
        size_t replacement_index = 0;

        if (auto index = format.identifier_indices.find_first_index(identifier_index); index.has_value()) {
            replacement_index = *index;
        } else {
            replacement_index = format.identifier_indices.size();
            format.identifier_indices.append(identifier_index);

            cldr.max_identifier_count = max(cldr.max_identifier_count, format.identifier_indices.size());
        }

        pattern = ByteString::formatted("{}{{{}:{}}}{}",
            *start_index > 0 ? pattern.substring_view(0, *start_index) : ""sv,
            replacement,
            replacement_index,
            pattern.substring_view(*start_index + utf8_pattern.byte_length()));
    }
}

static void parse_number_pattern(Vector<ByteString> patterns, CLDR& cldr, NumberFormatType type, NumberFormat& format, NumberSystem* number_system_for_groupings = nullptr)
{
    // https://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns
    // https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns
    VERIFY((patterns.size() == 1) || (patterns.size() == 2));

    auto replace_patterns = [&](ByteString pattern) {
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
            pattern = pattern.replace(replacement.key, replacement.value, ReplaceMode::All);

        if (auto start_number_index = pattern.find_any_of("#0"sv, ByteString::SearchDirection::Forward); start_number_index.has_value()) {
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

            pattern = ByteString::formatted("{}{{number}}{}",
                *start_number_index > 0 ? pattern.substring_view(0, *start_number_index) : ""sv,
                pattern.substring_view(end_number_index));

            // This is specifically handled here rather than in the replacements HashMap above so
            // that we do not errantly replace zeroes in number patterns.
            if (pattern.contains(*replacements.get("E"sv)))
                pattern = pattern.replace("0"sv, "{scientificExponent}"sv, ReplaceMode::FirstOnly);
        }

        if (type == NumberFormatType::Compact)
            return parse_identifiers(move(pattern), "compactIdentifier"sv, cldr, format);

        return pattern;
    };

    auto zero_format = replace_patterns(move(patterns[0]));
    format.positive_format_index = cldr.unique_strings.ensure(ByteString::formatted("{{plusSign}}{}", zero_format));

    if (patterns.size() == 2) {
        auto negative_format = replace_patterns(move(patterns[1]));
        format.negative_format_index = cldr.unique_strings.ensure(move(negative_format));
    } else {
        format.negative_format_index = cldr.unique_strings.ensure(ByteString::formatted("{{minusSign}}{}", zero_format));
    }

    format.zero_format_index = cldr.unique_strings.ensure(move(zero_format));
}

static void parse_number_pattern(Vector<ByteString> patterns, CLDR& cldr, NumberFormatType type, size_t& format_index, NumberSystem* number_system_for_groupings = nullptr)
{
    NumberFormat format {};
    parse_number_pattern(move(patterns), cldr, type, format, number_system_for_groupings);

    format_index = cldr.unique_formats.ensure(move(format));
}

static ErrorOr<void> parse_number_systems(ByteString locale_numbers_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath numbers_path(move(locale_numbers_path));
    numbers_path = numbers_path.append("numbers.json"sv);

    auto numbers = TRY(read_json_file(numbers_path.string()));
    auto const& main_object = numbers.as_object().get_object("main"sv).value();
    auto const& locale_object = main_object.get_object(numbers_path.parent().basename()).value();
    auto const& locale_numbers_object = locale_object.get_object("numbers"sv).value();
    auto const& minimum_grouping_digits = locale_numbers_object.get_byte_string("minimumGroupingDigits"sv).value();

    Vector<Optional<NumberSystem>> number_systems;
    number_systems.resize(cldr.number_systems.size());

    auto ensure_number_system = [&](auto const& system) -> NumberSystem& {
        auto system_index = cldr.number_systems.find_first_index(system).value();
        VERIFY(system_index < number_systems.size());

        auto& number_system = number_systems.at(system_index);
        if (!number_system.has_value())
            number_system = NumberSystem {};

        return number_system.value();
    };

    auto parse_number_format = [&](auto const& format_object) {
        Vector<size_t> result;
        result.ensure_capacity(format_object.size());

        format_object.for_each_member([&](auto const& key, JsonValue const& value) {
            auto split_key = key.split_view('-');
            if (split_key.size() != 3)
                return;

            auto patterns = value.as_string().split(';');
            NumberFormat format {};

            if (auto type = split_key[0].template to_number<u64>(); type.has_value()) {
                VERIFY(*type % 10 == 0);
                format.magnitude = static_cast<u8>(log10(*type));

                if (patterns[0] != "0"sv) {
                    auto number_of_zeroes_in_pattern = patterns[0].count("0"sv);
                    VERIFY(format.magnitude >= number_of_zeroes_in_pattern);

                    format.exponent = format.magnitude + 1 - number_of_zeroes_in_pattern;
                }
            } else {
                VERIFY(split_key[0] == "unitPattern"sv);
            }

            format.plurality = Locale::plural_category_from_string(split_key[2]);
            parse_number_pattern(move(patterns), cldr, NumberFormatType::Compact, format);

            auto format_index = cldr.unique_formats.ensure(move(format));
            result.append(format_index);
        });

        return cldr.unique_format_lists.ensure(move(result));
    };

    auto numeric_symbol_from_string = [&](StringView numeric_symbol) -> Optional<Locale::NumericSymbol> {
        if (numeric_symbol == "approximatelySign"sv)
            return Locale::NumericSymbol::ApproximatelySign;
        if (numeric_symbol == "decimal"sv)
            return Locale::NumericSymbol::Decimal;
        if (numeric_symbol == "exponential"sv)
            return Locale::NumericSymbol::Exponential;
        if (numeric_symbol == "group"sv)
            return Locale::NumericSymbol::Group;
        if (numeric_symbol == "infinity"sv)
            return Locale::NumericSymbol::Infinity;
        if (numeric_symbol == "minusSign"sv)
            return Locale::NumericSymbol::MinusSign;
        if (numeric_symbol == "nan"sv)
            return Locale::NumericSymbol::NaN;
        if (numeric_symbol == "percentSign"sv)
            return Locale::NumericSymbol::PercentSign;
        if (numeric_symbol == "plusSign"sv)
            return Locale::NumericSymbol::PlusSign;
        if (numeric_symbol == "timeSeparator"sv)
            return Locale::NumericSymbol::TimeSeparator;
        return {};
    };

    locale_numbers_object.for_each_member([&](auto const& key, JsonValue const& value) {
        constexpr auto symbols_prefix = "symbols-numberSystem-"sv;
        constexpr auto decimal_formats_prefix = "decimalFormats-numberSystem-"sv;
        constexpr auto currency_formats_prefix = "currencyFormats-numberSystem-"sv;
        constexpr auto percent_formats_prefix = "percentFormats-numberSystem-"sv;
        constexpr auto scientific_formats_prefix = "scientificFormats-numberSystem-"sv;
        constexpr auto misc_patterns_prefix = "miscPatterns-numberSystem-"sv;

        if (key.starts_with(symbols_prefix)) {
            auto system = key.substring(symbols_prefix.length());
            auto& number_system = ensure_number_system(system);

            NumericSymbolList symbols;

            value.as_object().for_each_member([&](auto const& symbol, JsonValue const& localization) {
                auto numeric_symbol = numeric_symbol_from_string(symbol);
                if (!numeric_symbol.has_value())
                    return;

                if (to_underlying(*numeric_symbol) >= symbols.size())
                    symbols.resize(to_underlying(*numeric_symbol) + 1);

                auto symbol_index = cldr.unique_strings.ensure(localization.as_string());
                symbols[to_underlying(*numeric_symbol)] = symbol_index;
            });

            // The range separator does not appear in the symbols list, we have to extract it from
            // the range pattern.
            auto misc_patterns_key = ByteString::formatted("{}{}", misc_patterns_prefix, system);
            auto misc_patterns = locale_numbers_object.get_object(misc_patterns_key).value();
            auto range_separator = misc_patterns.get_byte_string("range"sv).value();

            auto begin_index = range_separator.find("{0}"sv).value() + "{0}"sv.length();
            auto end_index = range_separator.find("{1}"sv).value();
            range_separator = range_separator.substring(begin_index, end_index - begin_index);

            if (to_underlying(Locale::NumericSymbol::RangeSeparator) >= symbols.size())
                symbols.resize(to_underlying(Locale::NumericSymbol::RangeSeparator) + 1);

            auto symbol_index = cldr.unique_strings.ensure(move(range_separator));
            symbols[to_underlying(Locale::NumericSymbol::RangeSeparator)] = symbol_index;

            number_system.symbols = cldr.unique_symbols.ensure(move(symbols));
        } else if (key.starts_with(decimal_formats_prefix)) {
            auto system = key.substring(decimal_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get_byte_string("standard"sv).value();
            parse_number_pattern(format_object.split(';'), cldr, NumberFormatType::Standard, number_system.decimal_format, &number_system);

            auto const& long_format = value.as_object().get_object("long"sv)->get_object("decimalFormat"sv).value();
            number_system.decimal_long_formats = parse_number_format(long_format);

            auto const& short_format = value.as_object().get_object("short"sv)->get_object("decimalFormat"sv).value();
            number_system.decimal_short_formats = parse_number_format(short_format);
        } else if (key.starts_with(currency_formats_prefix)) {
            auto system = key.substring(currency_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get_byte_string("standard"sv).value();
            parse_number_pattern(format_object.split(';'), cldr, NumberFormatType::Standard, number_system.currency_format);

            format_object = value.as_object().get_byte_string("accounting"sv).value();
            parse_number_pattern(format_object.split(';'), cldr, NumberFormatType::Standard, number_system.accounting_format);

            number_system.currency_unit_formats = parse_number_format(value.as_object());
        } else if (key.starts_with(percent_formats_prefix)) {
            auto system = key.substring(percent_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get_byte_string("standard"sv).value();
            parse_number_pattern(format_object.split(';'), cldr, NumberFormatType::Standard, number_system.percent_format);
        } else if (key.starts_with(scientific_formats_prefix)) {
            auto system = key.substring(scientific_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get_byte_string("standard"sv).value();
            parse_number_pattern(format_object.split(';'), cldr, NumberFormatType::Standard, number_system.scientific_format);
        }
    });

    locale.number_systems.ensure_capacity(number_systems.size());

    for (auto& number_system : number_systems) {
        size_t system_index = 0;
        if (number_system.has_value())
            system_index = cldr.unique_systems.ensure(number_system.release_value());

        locale.number_systems.append(system_index);
    }

    locale.minimum_grouping_digits = minimum_grouping_digits.template to_number<u8>().value();
    return {};
}

static ErrorOr<void> parse_units(ByteString locale_units_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath units_path(move(locale_units_path));
    units_path = units_path.append("units.json"sv);

    auto locale_units = TRY(read_json_file(units_path.string()));
    auto const& main_object = locale_units.as_object().get_object("main"sv).value();
    auto const& locale_object = main_object.get_object(units_path.parent().basename()).value();
    auto const& locale_units_object = locale_object.get_object("units"sv).value();
    auto const& long_object = locale_units_object.get_object("long"sv).value();
    auto const& short_object = locale_units_object.get_object("short"sv).value();
    auto const& narrow_object = locale_units_object.get_object("narrow"sv).value();

    HashMap<ByteString, Unit> units;

    auto ensure_unit = [&](auto const& unit) -> Unit& {
        return units.ensure(unit, [&]() {
            auto unit_index = cldr.unique_strings.ensure(unit);
            return Unit { .unit = unit_index };
        });
    };

    auto is_sanctioned_unit = [](StringView unit_name) {
        // LibUnicode generally tries to avoid being directly dependent on ECMA-402, but this rather significantly reduces the amount
        // of data generated here, and ECMA-402 is currently the only consumer of this data.
        constexpr auto sanctioned_units = JS::Intl::sanctioned_single_unit_identifiers();
        return find(sanctioned_units.begin(), sanctioned_units.end(), unit_name) != sanctioned_units.end();
    };

    auto parse_units_object = [&](auto const& units_object, Locale::Style style) {
        constexpr auto unit_pattern_prefix = "unitPattern-count-"sv;
        constexpr auto combined_unit_separator = "-per-"sv;

        units_object.for_each_member([&](auto const& key, JsonValue const& value) {
            auto end_of_category = key.find('-');
            if (!end_of_category.has_value())
                return;

            auto unit_name = key.substring(*end_of_category + 1);

            if (!is_sanctioned_unit(unit_name)) {
                auto indices = unit_name.find_all(combined_unit_separator);
                if (indices.size() != 1)
                    return;

                auto numerator = unit_name.substring_view(0, indices[0]);
                auto denominator = unit_name.substring_view(indices[0] + combined_unit_separator.length());
                if (!is_sanctioned_unit(numerator) || !is_sanctioned_unit(denominator))
                    return;
            }

            auto& unit = ensure_unit(unit_name);
            NumberFormatList formats;

            value.as_object().for_each_member([&](auto const& unit_key, JsonValue const& pattern_value) {
                if (!unit_key.starts_with(unit_pattern_prefix))
                    return;

                NumberFormat format {};

                auto plurality = unit_key.substring_view(unit_pattern_prefix.length());
                format.plurality = Locale::plural_category_from_string(plurality);

                auto zero_format = pattern_value.as_string().replace("{0}"sv, "{number}"sv, ReplaceMode::FirstOnly);
                zero_format = parse_identifiers(zero_format, "unitIdentifier"sv, cldr, format);

                format.positive_format_index = cldr.unique_strings.ensure(zero_format.replace("{number}"sv, "{plusSign}{number}"sv, ReplaceMode::FirstOnly));
                format.negative_format_index = cldr.unique_strings.ensure(zero_format.replace("{number}"sv, "{minusSign}{number}"sv, ReplaceMode::FirstOnly));
                format.zero_format_index = cldr.unique_strings.ensure(move(zero_format));

                formats.append(cldr.unique_formats.ensure(move(format)));
            });

            auto number_format_list_index = cldr.unique_format_lists.ensure(move(formats));

            switch (style) {
            case Locale::Style::Long:
                unit.long_formats = number_format_list_index;
                break;
            case Locale::Style::Short:
                unit.short_formats = number_format_list_index;
                break;
            case Locale::Style::Narrow:
                unit.narrow_formats = number_format_list_index;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        });
    };

    parse_units_object(long_object, Locale::Style::Long);
    parse_units_object(short_object, Locale::Style::Short);
    parse_units_object(narrow_object, Locale::Style::Narrow);

    for (auto& unit : units) {
        auto unit_index = cldr.unique_units.ensure(move(unit.value));
        locale.units.set(unit.key, unit_index);
    }

    return {};
}

static ErrorOr<void> parse_all_locales(ByteString core_path, ByteString numbers_path, ByteString units_path, CLDR& cldr)
{
    LexicalPath core_supplemental_path(move(core_path));
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(FileSystem::is_directory(core_supplemental_path.string()));

    TRY(parse_number_system_digits(core_supplemental_path.string(), cldr));

    auto remove_variants_from_path = [&](ByteString path) -> ErrorOr<ByteString> {
        auto parsed_locale = TRY(CanonicalLanguageID::parse(cldr.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(cldr.unique_strings.get(parsed_locale.language));
        if (auto script = cldr.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = cldr.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.to_byte_string();
    };

    TRY(Core::Directory::for_each_entry(TRY(String::formatted("{}/main", numbers_path)), Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto& directory) -> ErrorOr<IterationDecision> {
        auto numbers_path = LexicalPath::join(directory.path().string(), entry.name).string();
        auto language = TRY(remove_variants_from_path(numbers_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_number_systems(numbers_path, cldr, locale));
        return IterationDecision::Continue;
    }));

    TRY(Core::Directory::for_each_entry(TRY(String::formatted("{}/main", units_path)), Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto& directory) -> ErrorOr<IterationDecision> {
        auto units_path = LexicalPath::join(directory.path().string(), entry.name).string();
        auto language = TRY(remove_variants_from_path(units_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_units(units_path, cldr, locale));
        return IterationDecision::Continue;
    }));

    return {};
}

static ByteString format_identifier(StringView, ByteString identifier)
{
    return identifier.to_titlecase();
}

static ErrorOr<void> generate_unicode_locale_header(Core::InputBufferedFile& file, CLDR& cldr)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Types.h>

namespace Locale {
)~~~");

    generate_enum(generator, format_identifier, "NumberSystem"sv, {}, cldr.number_systems);

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_unicode_locale_implementation(Core::InputBufferedFile& file, CLDR& cldr)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, cldr.unique_strings.type_that_fits());
    generator.set("number_format_index_type"sv, cldr.unique_formats.type_that_fits());
    generator.set("number_format_list_index_type"sv, cldr.unique_format_lists.type_that_fits());
    generator.set("numeric_symbol_list_index_type"sv, cldr.unique_symbols.type_that_fits());
    generator.set("identifier_count", ByteString::number(cldr.max_identifier_count));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibLocale/Locale.h>
#include <LibLocale/LocaleData.h>
#include <LibLocale/NumberFormat.h>
#include <LibLocale/NumberFormatData.h>
#include <LibLocale/PluralRules.h>

namespace Locale {
)~~~");

    cldr.unique_strings.generate(generator);

    generator.append(R"~~~(
struct NumberFormatImpl {
    NumberFormat to_unicode_number_format() const {
        NumberFormat number_format {};

        number_format.magnitude = magnitude;
        number_format.exponent = exponent;
        number_format.plurality = static_cast<PluralCategory>(plurality);
        number_format.zero_format = decode_string(zero_format);
        number_format.positive_format = decode_string(positive_format);
        number_format.negative_format = decode_string(negative_format);

        number_format.identifiers.ensure_capacity(identifiers.size());
        for (@string_index_type@ identifier : identifiers)
            number_format.identifiers.unchecked_append(decode_string(identifier));

        return number_format;
    }

    u8 magnitude { 0 };
    u8 exponent { 0 };
    u8 plurality { 0 };
    @string_index_type@ zero_format { 0 };
    @string_index_type@ positive_format { 0 };
    @string_index_type@ negative_format { 0 };
    Array<@string_index_type@, @identifier_count@> identifiers {};
};

struct NumberSystemData {
    @numeric_symbol_list_index_type@ symbols { 0 };

    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };

    @number_format_index_type@ decimal_format { 0 };
    @number_format_list_index_type@ decimal_long_formats { 0 };
    @number_format_list_index_type@ decimal_short_formats { 0 };

    @number_format_index_type@ currency_format { 0 };
    @number_format_index_type@ accounting_format { 0 };
    @number_format_list_index_type@ currency_unit_formats { 0 };

    @number_format_index_type@ percent_format { 0 };
    @number_format_index_type@ scientific_format { 0 };
};

struct Unit {
    @string_index_type@ unit { 0 };
    @number_format_list_index_type@ long_formats { 0 };
    @number_format_list_index_type@ short_formats { 0 };
    @number_format_list_index_type@ narrow_formats { 0 };
};
)~~~");

    cldr.unique_formats.generate(generator, "NumberFormatImpl"sv, "s_number_formats"sv, 10);
    cldr.unique_format_lists.generate(generator, cldr.unique_formats.type_that_fits(), "s_number_format_lists"sv);
    cldr.unique_symbols.generate(generator, cldr.unique_strings.type_that_fits(), "s_numeric_symbol_lists"sv);
    cldr.unique_systems.generate(generator, "NumberSystemData"sv, "s_number_systems"sv, 10);
    cldr.unique_units.generate(generator, "Unit"sv, "s_units"sv, 10);

    auto locales = cldr.locales.keys();
    quick_sort(locales);

    generator.set("size", ByteString::number(locales.size()));
    generator.append(R"~~~(
static constexpr Array<u8, @size@> s_minimum_grouping_digits { { )~~~");

    bool first = true;
    for (auto const& locale : locales) {
        generator.append(first ? " "sv : ", "sv);
        generator.append(ByteString::number(cldr.locales.find(locale)->value.minimum_grouping_digits));
        first = false;
    }
    generator.append(" } };\n");

    auto append_map = [&](ByteString name, auto type, auto const& map) {
        generator.set("name", name);
        generator.set("type", type);
        generator.set("size", ByteString::number(map.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@> @name@ { {)~~~");

        bool first = true;
        for (auto const& item : map) {
            generator.append(first ? " "sv : ", "sv);
            if constexpr (requires { item.value; })
                generator.append(ByteString::number(item.value));
            else
                generator.append(ByteString::number(item));
            first = false;
        }

        generator.append(" } };");
    };

    generate_mapping(generator, cldr.number_system_digits, "u32"sv, "s_number_systems_digits"sv, "s_number_systems_digits_{}"sv, nullptr, [&](auto const& name, auto const& value) { append_map(name, "u32"sv, value); });
    generate_mapping(generator, cldr.locales, cldr.unique_systems.type_that_fits(), "s_locale_number_systems"sv, "s_number_systems_{}"sv, nullptr, [&](auto const& name, auto const& value) { append_map(name, cldr.unique_systems.type_that_fits(), value.number_systems); });
    generate_mapping(generator, cldr.locales, cldr.unique_units.type_that_fits(), "s_locale_units"sv, "s_units_{}"sv, nullptr, [&](auto const& name, auto const& value) { append_map(name, cldr.unique_units.type_that_fits(), value.units); });

    generator.append(R"~~~(
static Optional<NumberSystem> keyword_to_number_system(KeywordNumbers keyword)
{
    switch (keyword) {)~~~");

    for (auto const& number_system : cldr.number_systems) {
        generator.set("name"sv, format_identifier({}, number_system));
        generator.append(R"~~~(
    case KeywordNumbers::@name@:
        return NumberSystem::@name@;)~~~");
    }

    generator.append(R"~~~(
    default:
        return {};
    }
}

Optional<ReadonlySpan<u32>> get_digits_for_number_system(StringView system)
{
    auto number_system_keyword = keyword_nu_from_string(system);
    if (!number_system_keyword.has_value())
        return {};

    auto number_system_value = keyword_to_number_system(*number_system_keyword);
    if (!number_system_value.has_value())
        return {};

    auto number_system_index = to_underlying(*number_system_value);
    return s_number_systems_digits[number_system_index];
}

static NumberSystemData const* find_number_system(StringView locale, StringView system)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return nullptr;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto const& number_systems = s_locale_number_systems.at(locale_index);

    auto lookup_number_system = [&](auto number_system) -> NumberSystemData const* {
        auto number_system_keyword = keyword_nu_from_string(number_system);
        if (!number_system_keyword.has_value())
            return nullptr;

        auto number_system_value = keyword_to_number_system(*number_system_keyword);
        if (!number_system_value.has_value())
            return nullptr;

        auto number_system_index = to_underlying(*number_system_value);
        number_system_index = number_systems.at(number_system_index);

        if (number_system_index == 0)
            return nullptr;

        return &s_number_systems.at(number_system_index);
    };

    if (auto const* number_system = lookup_number_system(system))
        return number_system;

    auto default_number_system = get_preferred_keyword_value_for_locale(locale, "nu"sv);
    if (!default_number_system.has_value())
        return nullptr;

    return lookup_number_system(*default_number_system);
}

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, NumericSymbol symbol)
{
    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        auto symbols = s_numeric_symbol_lists.at(number_system->symbols);

        auto symbol_index = to_underlying(symbol);
        if (symbol_index >= symbols.size())
            return {};

        return decode_string(symbols[symbol_index]);
    }

    return {};
}

Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    u8 minimum_grouping_digits = s_minimum_grouping_digits[to_underlying(*locale_value) - 1];

    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr)
        return NumberGroupings { minimum_grouping_digits, number_system->primary_grouping_size, number_system->secondary_grouping_size };
    return {};
}

Optional<NumberFormat> get_standard_number_system_format(StringView locale, StringView system, StandardNumberFormatType type)
{
    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        @number_format_index_type@ format_index = 0;

        switch (type) {
        case StandardNumberFormatType::Decimal:
            format_index = number_system->decimal_format;
            break;
        case StandardNumberFormatType::Currency:
            format_index = number_system->currency_format;
            break;
        case StandardNumberFormatType::Accounting:
            format_index = number_system->accounting_format;
            break;
        case StandardNumberFormatType::Percent:
            format_index = number_system->percent_format;
            break;
        case StandardNumberFormatType::Scientific:
            format_index = number_system->scientific_format;
            break;
        }

        return s_number_formats[format_index].to_unicode_number_format();
    }

    return {};
}

Vector<NumberFormat> get_compact_number_system_formats(StringView locale, StringView system, CompactNumberFormatType type)
{
    Vector<NumberFormat> formats;

    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        @number_format_list_index_type@ number_format_list_index { 0 };

        switch (type) {
        case CompactNumberFormatType::DecimalLong:
            number_format_list_index = number_system->decimal_long_formats;
            break;
        case CompactNumberFormatType::DecimalShort:
            number_format_list_index = number_system->decimal_short_formats;
            break;
        case CompactNumberFormatType::CurrencyUnit:
            number_format_list_index = number_system->currency_unit_formats;
            break;
        }

        auto number_formats = s_number_format_lists.at(number_format_list_index);
        formats.ensure_capacity(number_formats.size());

        for (auto number_format : number_formats)
            formats.unchecked_append(s_number_formats[number_format].to_unicode_number_format());
    }

    return formats;
}

static Unit const* find_units(StringView locale, StringView unit)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return nullptr;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto const& locale_units = s_locale_units.at(locale_index);

    for (auto unit_index : locale_units) {
        auto const& units = s_units.at(unit_index);

        if (unit == decode_string(units.unit))
            return &units;
    };

    return nullptr;
}

Vector<NumberFormat> get_unit_formats(StringView locale, StringView unit, Style style)
{
    Vector<NumberFormat> formats;

    if (auto const* units = find_units(locale, unit); units != nullptr) {
        @number_format_list_index_type@ number_format_list_index { 0 };

        switch (style) {
        case Style::Long:
            number_format_list_index = units->long_formats;
            break;
        case Style::Short:
            number_format_list_index = units->short_formats;
            break;
        case Style::Narrow:
            number_format_list_index = units->narrow_formats;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        auto number_formats = s_number_format_lists.at(number_format_list_index);
        formats.ensure_capacity(number_formats.size());

        for (auto number_format : number_formats)
            formats.unchecked_append(s_number_formats[number_format].to_unicode_number_format());
    }

    return formats;
}

}
)~~~");

    TRY(file.write_until_depleted(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView core_path;
    StringView numbers_path;
    StringView units_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(numbers_path, "Path to cldr-numbers directory", "numbers-path", 'n', "numbers-path");
    args_parser.add_option(units_path, "Path to cldr-units directory", "units-path", 'u', "units-path");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::File::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::File::OpenMode::Write));

    CLDR cldr;
    TRY(parse_all_locales(core_path, numbers_path, units_path, cldr));

    TRY(generate_unicode_locale_header(*generated_header_file, cldr));
    TRY(generate_unicode_locale_implementation(*generated_implementation_file, cldr));

    return 0;
}
