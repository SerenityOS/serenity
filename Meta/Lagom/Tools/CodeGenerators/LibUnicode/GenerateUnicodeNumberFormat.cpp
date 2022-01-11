/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GeneratorUtil.h"
#include <AK/AllOf.h>
#include <AK/Array.h>
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
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Traits.h>
#include <AK/Utf8View.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/NumberFormat.h>
#include <math.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

using NumberFormatIndexType = u16;
constexpr auto s_number_format_index_type = "u16"sv;

using NumberFormatListIndexType = u16;
constexpr auto s_number_format_list_index_type = "u16"sv;

using NumericSymbolListIndexType = u8;
constexpr auto s_numeric_symbol_list_index_type = "u8"sv;

using NumberSystemIndexType = u8;
constexpr auto s_number_system_index_type = "u8"sv;

using UnitIndexType = u16;
constexpr auto s_unit_index_type = "u16"sv;

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

    unsigned hash() const
    {
        auto hash = pair_int_hash(magnitude, exponent);
        hash = pair_int_hash(hash, static_cast<u8>(plurality));
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

    StringIndexType zero_format_index { 0 };
    StringIndexType positive_format_index { 0 };
    StringIndexType negative_format_index { 0 };
    Vector<StringIndexType> identifier_indices {};
};

template<>
struct AK::Formatter<NumberFormat> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, NumberFormat const& format)
    {
        StringBuilder identifier_indices;
        identifier_indices.join(", "sv, format.identifier_indices);

        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {}, {}, {{ {} }} }}",
            format.magnitude,
            format.exponent,
            static_cast<u8>(format.plurality),
            format.zero_format_index,
            format.positive_format_index,
            format.negative_format_index,
            identifier_indices.build());
    }
};

template<>
struct AK::Traits<NumberFormat> : public GenericTraits<NumberFormat> {
    static unsigned hash(NumberFormat const& f) { return f.hash(); }
};

using NumberFormatList = Vector<NumberFormatIndexType>;
using NumericSymbolList = Vector<StringIndexType>;

struct NumberSystem {
    unsigned hash() const
    {
        auto hash = pair_int_hash(system, symbols);
        hash = pair_int_hash(hash, primary_grouping_size);
        hash = pair_int_hash(hash, secondary_grouping_size);
        hash = pair_int_hash(hash, decimal_format);
        hash = pair_int_hash(hash, decimal_long_formats);
        hash = pair_int_hash(hash, decimal_short_formats);
        hash = pair_int_hash(hash, currency_format);
        hash = pair_int_hash(hash, accounting_format);
        hash = pair_int_hash(hash, currency_unit_formats);
        hash = pair_int_hash(hash, currency_short_formats);
        hash = pair_int_hash(hash, percent_format);
        hash = pair_int_hash(hash, scientific_format);
        return hash;
    }

    bool operator==(NumberSystem const& other) const
    {
        return (system == other.system)
            && (symbols == other.symbols)
            && (primary_grouping_size == other.primary_grouping_size)
            && (secondary_grouping_size == other.secondary_grouping_size)
            && (decimal_format == other.decimal_format)
            && (decimal_long_formats == other.decimal_long_formats)
            && (decimal_short_formats == other.decimal_short_formats)
            && (currency_format == other.currency_format)
            && (accounting_format == other.accounting_format)
            && (currency_unit_formats == other.currency_unit_formats)
            && (currency_short_formats == other.currency_short_formats)
            && (percent_format == other.percent_format)
            && (scientific_format == other.scientific_format);
    }

    StringIndexType system { 0 };
    NumericSymbolListIndexType symbols { 0 };

    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };

    NumberFormatIndexType decimal_format { 0 };
    NumberFormatListIndexType decimal_long_formats { 0 };
    NumberFormatListIndexType decimal_short_formats { 0 };

    NumberFormatIndexType currency_format { 0 };
    NumberFormatIndexType accounting_format { 0 };
    NumberFormatListIndexType currency_unit_formats { 0 };
    NumberFormatListIndexType currency_short_formats { 0 };

    NumberFormatIndexType percent_format { 0 };
    NumberFormatIndexType scientific_format { 0 };
};

template<>
struct AK::Formatter<NumberSystem> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, NumberSystem const& system)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {} }}",
            system.system,
            system.symbols,
            system.primary_grouping_size,
            system.secondary_grouping_size,
            system.decimal_format,
            system.decimal_long_formats,
            system.decimal_short_formats,
            system.currency_format,
            system.accounting_format,
            system.currency_unit_formats,
            system.currency_short_formats,
            system.percent_format,
            system.scientific_format);
    }
};

template<>
struct AK::Traits<NumberSystem> : public GenericTraits<NumberSystem> {
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

    StringIndexType unit { 0 };
    NumberFormatListIndexType long_formats { 0 };
    NumberFormatListIndexType short_formats { 0 };
    NumberFormatListIndexType narrow_formats { 0 };
};

template<>
struct AK::Formatter<Unit> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Unit const& system)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {}, {}, {} }}",
            system.unit,
            system.long_formats,
            system.short_formats,
            system.narrow_formats);
    }
};

template<>
struct AK::Traits<Unit> : public GenericTraits<Unit> {
    static unsigned hash(Unit const& u) { return u.hash(); }
};

struct Locale {
    HashMap<String, NumberSystemIndexType> number_systems;
    HashMap<String, UnitIndexType> units {};
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;
    UniqueStorage<NumberFormat, NumberFormatIndexType> unique_formats;
    UniqueStorage<NumberFormatList, NumberFormatListIndexType> unique_format_lists;
    UniqueStorage<NumericSymbolList, NumericSymbolListIndexType> unique_symbols;
    UniqueStorage<NumberSystem, NumberSystemIndexType> unique_systems;
    UniqueStorage<Unit, UnitIndexType> unique_units;

    HashMap<String, Locale> locales;
    size_t max_identifier_count { 0 };
};

static String parse_identifiers(String pattern, StringView replacement, UnicodeLocaleData& locale_data, NumberFormat& format)
{
    static Utf8View whitespace { "\u0020\u00a0\u200f"sv };

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

        auto identifier = utf8_pattern.as_string().replace("'.'"sv, "."sv);
        auto identifier_index = locale_data.unique_strings.ensure(move(identifier));
        size_t replacement_index = 0;

        if (auto index = format.identifier_indices.find_first_index(identifier_index); index.has_value()) {
            replacement_index = *index;
        } else {
            replacement_index = format.identifier_indices.size();
            format.identifier_indices.append(identifier_index);

            locale_data.max_identifier_count = max(locale_data.max_identifier_count, format.identifier_indices.size());
        }

        pattern = String::formatted("{}{{{}:{}}}{}",
            *start_index > 0 ? pattern.substring_view(0, *start_index) : ""sv,
            replacement,
            replacement_index,
            pattern.substring_view(*start_index + utf8_pattern.byte_length()));
    }
}

static void parse_number_pattern(Vector<String> patterns, UnicodeLocaleData& locale_data, NumberFormatType type, NumberFormat& format, NumberSystem* number_system_for_groupings = nullptr)
{
    // https://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns
    // https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns
    VERIFY((patterns.size() == 1) || (patterns.size() == 2));

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

        if (type == NumberFormatType::Compact)
            return parse_identifiers(move(pattern), "compactIdentifier"sv, locale_data, format);

        return pattern;
    };

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

static void parse_number_pattern(Vector<String> patterns, UnicodeLocaleData& locale_data, NumberFormatType type, NumberFormatIndexType& format_index, NumberSystem* number_system_for_groupings = nullptr)
{
    NumberFormat format {};
    parse_number_pattern(move(patterns), locale_data, type, format, number_system_for_groupings);

    format_index = locale_data.unique_formats.ensure(move(format));
}

static ErrorOr<void> parse_number_systems(String locale_numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath numbers_path(move(locale_numbers_path));
    numbers_path = numbers_path.append("numbers.json"sv);

    auto numbers_file = TRY(Core::File::open(numbers_path.string(), Core::OpenMode::ReadOnly));
    auto numbers = TRY(JsonValue::from_string(numbers_file->read_all()));

    auto const& main_object = numbers.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(numbers_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);

    HashMap<String, NumberSystem> number_systems;

    auto ensure_number_system = [&](auto const& system) -> NumberSystem& {
        return number_systems.ensure(system, [&]() {
            auto system_index = locale_data.unique_strings.ensure(system);
            return NumberSystem { .system = system_index };
        });
    };

    auto parse_number_format = [&](auto const& format_object) {
        Vector<NumberFormatIndexType> result;
        result.ensure_capacity(format_object.size());

        format_object.for_each_member([&](auto const& key, JsonValue const& value) {
            auto split_key = key.split_view('-');
            if (split_key.size() != 3)
                return;

            auto patterns = value.as_string().split(';');
            NumberFormat format {};

            if (auto type = split_key[0].template to_uint<u64>(); type.has_value()) {
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

            format.plurality = NumberFormat::plurality_from_string(split_key[2]);
            parse_number_pattern(move(patterns), locale_data, NumberFormatType::Compact, format);

            auto format_index = locale_data.unique_formats.ensure(move(format));
            result.append(format_index);
        });

        return locale_data.unique_format_lists.ensure(move(result));
    };

    auto numeric_symbol_from_string = [&](StringView numeric_symbol) -> Optional<Unicode::NumericSymbol> {
        if (numeric_symbol == "decimal"sv)
            return Unicode::NumericSymbol::Decimal;
        if (numeric_symbol == "exponential"sv)
            return Unicode::NumericSymbol::Exponential;
        if (numeric_symbol == "group"sv)
            return Unicode::NumericSymbol::Group;
        if (numeric_symbol == "infinity"sv)
            return Unicode::NumericSymbol::Infinity;
        if (numeric_symbol == "minusSign"sv)
            return Unicode::NumericSymbol::MinusSign;
        if (numeric_symbol == "nan"sv)
            return Unicode::NumericSymbol::NaN;
        if (numeric_symbol == "percentSign"sv)
            return Unicode::NumericSymbol::PercentSign;
        if (numeric_symbol == "plusSign"sv)
            return Unicode::NumericSymbol::PlusSign;
        return {};
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

            NumericSymbolList symbols;

            value.as_object().for_each_member([&](auto const& symbol, JsonValue const& localization) {
                auto numeric_symbol = numeric_symbol_from_string(symbol);
                if (!numeric_symbol.has_value())
                    return;

                if (to_underlying(*numeric_symbol) >= symbols.size())
                    symbols.resize(to_underlying(*numeric_symbol) + 1);

                auto symbol_index = locale_data.unique_strings.ensure(localization.as_string());
                symbols[to_underlying(*numeric_symbol)] = symbol_index;
            });

            number_system.symbols = locale_data.unique_symbols.ensure(move(symbols));
        } else if (key.starts_with(decimal_formats_prefix)) {
            auto system = key.substring(decimal_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string().split(';'), locale_data, NumberFormatType::Standard, number_system.decimal_format, &number_system);

            auto const& long_format = value.as_object().get("long"sv).as_object().get("decimalFormat"sv);
            number_system.decimal_long_formats = parse_number_format(long_format.as_object());

            auto const& short_format = value.as_object().get("short"sv).as_object().get("decimalFormat"sv);
            number_system.decimal_short_formats = parse_number_format(short_format.as_object());
        } else if (key.starts_with(currency_formats_prefix)) {
            auto system = key.substring(currency_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string().split(';'), locale_data, NumberFormatType::Standard, number_system.currency_format);

            format_object = value.as_object().get("accounting"sv);
            parse_number_pattern(format_object.as_string().split(';'), locale_data, NumberFormatType::Standard, number_system.accounting_format);

            number_system.currency_unit_formats = parse_number_format(value.as_object());

            if (value.as_object().has("short"sv)) {
                auto const& short_format = value.as_object().get("short"sv).as_object().get("standard"sv);
                number_system.currency_short_formats = parse_number_format(short_format.as_object());
            }
        } else if (key.starts_with(percent_formats_prefix)) {
            auto system = key.substring(percent_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string().split(';'), locale_data, NumberFormatType::Standard, number_system.percent_format);
        } else if (key.starts_with(scientific_formats_prefix)) {
            auto system = key.substring(scientific_formats_prefix.length());
            auto& number_system = ensure_number_system(system);

            auto format_object = value.as_object().get("standard"sv);
            parse_number_pattern(format_object.as_string().split(';'), locale_data, NumberFormatType::Standard, number_system.scientific_format);
        }
    });

    for (auto& number_system : number_systems) {
        auto system_index = locale_data.unique_systems.ensure(move(number_system.value));
        locale.number_systems.set(number_system.key, system_index);
    }

    return {};
}

static ErrorOr<void> parse_units(String locale_units_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath units_path(move(locale_units_path));
    units_path = units_path.append("units.json"sv);

    auto units_file = TRY(Core::File::open(units_path.string(), Core::OpenMode::ReadOnly));
    auto locale_units = TRY(JsonValue::from_string(units_file->read_all()));

    auto const& main_object = locale_units.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(units_path.parent().basename());
    auto const& locale_units_object = locale_object.as_object().get("units"sv);
    auto const& long_object = locale_units_object.as_object().get("long"sv);
    auto const& short_object = locale_units_object.as_object().get("short"sv);
    auto const& narrow_object = locale_units_object.as_object().get("narrow"sv);

    HashMap<String, Unit> units;

    auto ensure_unit = [&](auto const& unit) -> Unit& {
        return units.ensure(unit, [&]() {
            auto unit_index = locale_data.unique_strings.ensure(unit);
            return Unit { .unit = unit_index };
        });
    };

    auto is_sanctioned_unit = [](StringView unit_name) {
        // This is a copy of the units sanctioned for use within ECMA-402. LibUnicode generally tries to
        // avoid being directly dependent on ECMA-402, but this rather significantly reduces the amount
        // of data generated here, and ECMA-402 is currently the only consumer of this data.
        // https://tc39.es/ecma402/#table-sanctioned-simple-unit-identifiers
        constexpr auto sanctioned_units = AK::Array { "acre"sv, "bit"sv, "byte"sv, "celsius"sv, "centimeter"sv, "day"sv, "degree"sv, "fahrenheit"sv, "fluid-ounce"sv, "foot"sv, "gallon"sv, "gigabit"sv, "gigabyte"sv, "gram"sv, "hectare"sv, "hour"sv, "inch"sv, "kilobit"sv, "kilobyte"sv, "kilogram"sv, "kilometer"sv, "liter"sv, "megabit"sv, "megabyte"sv, "meter"sv, "mile"sv, "mile-scandinavian"sv, "milliliter"sv, "millimeter"sv, "millisecond"sv, "minute"sv, "month"sv, "ounce"sv, "percent"sv, "petabyte"sv, "pound"sv, "second"sv, "stone"sv, "terabit"sv, "terabyte"sv, "week"sv, "yard"sv, "year"sv };
        return find(sanctioned_units.begin(), sanctioned_units.end(), unit_name) != sanctioned_units.end();
    };

    auto parse_units_object = [&](auto const& units_object, Unicode::Style style) {
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
                format.plurality = NumberFormat::plurality_from_string(plurality);

                auto zero_format = pattern_value.as_string().replace("{0}"sv, "{number}"sv);
                zero_format = parse_identifiers(zero_format, "unitIdentifier"sv, locale_data, format);

                format.positive_format_index = locale_data.unique_strings.ensure(zero_format.replace("{number}"sv, "{plusSign}{number}"sv));
                format.negative_format_index = locale_data.unique_strings.ensure(zero_format.replace("{number}"sv, "{minusSign}{number}"sv));
                format.zero_format_index = locale_data.unique_strings.ensure(move(zero_format));

                formats.append(locale_data.unique_formats.ensure(move(format)));
            });

            auto number_format_list_index = locale_data.unique_format_lists.ensure(move(formats));

            switch (style) {
            case Unicode::Style::Long:
                unit.long_formats = number_format_list_index;
                break;
            case Unicode::Style::Short:
                unit.short_formats = number_format_list_index;
                break;
            case Unicode::Style::Narrow:
                unit.narrow_formats = number_format_list_index;
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        });
    };

    parse_units_object(long_object.as_object(), Unicode::Style::Long);
    parse_units_object(short_object.as_object(), Unicode::Style::Short);
    parse_units_object(narrow_object.as_object(), Unicode::Style::Narrow);

    for (auto& unit : units) {
        auto unit_index = locale_data.unique_units.ensure(move(unit.value));
        locale.units.set(unit.key, unit_index);
    }

    return {};
}

static ErrorOr<void> parse_all_locales(String numbers_path, String units_path, UnicodeLocaleData& locale_data)
{
    auto numbers_iterator = TRY(path_to_dir_iterator(move(numbers_path)));
    auto units_iterator = TRY(path_to_dir_iterator(move(units_path)));

    auto remove_variants_from_path = [&](String path) -> ErrorOr<String> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(locale_data.unique_strings.get(parsed_locale.language));
        if (auto script = locale_data.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = locale_data.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (numbers_iterator.has_next()) {
        auto numbers_path = TRY(next_path_from_dir_iterator(numbers_iterator));
        auto language = TRY(remove_variants_from_path(numbers_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_number_systems(numbers_path, locale_data, locale));
    }

    while (units_iterator.has_next()) {
        auto units_path = TRY(next_path_from_dir_iterator(units_iterator));
        auto language = TRY(remove_variants_from_path(units_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_units(units_path, locale_data, locale));
    }

    return {};
}

static void generate_unicode_locale_header(Core::File& file, UnicodeLocaleData&)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    // FIXME: Update unicode_data.cmake to not require a header.
    generator.append(R"~~~(
#pragma once
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("number_format_index_type"sv, s_number_format_index_type);
    generator.set("number_format_list_index_type"sv, s_number_format_list_index_type);
    generator.set("numeric_symbol_list_index_type"sv, s_numeric_symbol_list_index_type);
    generator.set("identifier_count", String::number(locale_data.max_identifier_count));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/NumberFormat.h>
#include <LibUnicode/UnicodeNumberFormat.h>

namespace Unicode {
)~~~");

    locale_data.unique_strings.generate(generator);

    generator.append(R"~~~(
struct NumberFormatImpl {
    NumberFormat to_unicode_number_format() const {
        NumberFormat number_format {};

        number_format.magnitude = magnitude;
        number_format.exponent = exponent;
        number_format.plurality = static_cast<NumberFormat::Plurality>(plurality);
        number_format.zero_format = s_string_list[zero_format];
        number_format.positive_format = s_string_list[positive_format];
        number_format.negative_format = s_string_list[negative_format];

        number_format.identifiers.ensure_capacity(identifiers.size());
        for (@string_index_type@ identifier : identifiers)
            number_format.identifiers.append(s_string_list[identifier]);

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

struct NumberSystem {
    @string_index_type@ system { 0 };
    @numeric_symbol_list_index_type@ symbols { 0 };

    u8 primary_grouping_size { 0 };
    u8 secondary_grouping_size { 0 };

    @number_format_index_type@ decimal_format { 0 };
    @number_format_list_index_type@ decimal_long_formats { 0 };
    @number_format_list_index_type@ decimal_short_formats { 0 };

    @number_format_index_type@ currency_format { 0 };
    @number_format_index_type@ accounting_format { 0 };
    @number_format_list_index_type@ currency_unit_formats { 0 };
    @number_format_list_index_type@ currency_short_formats { 0 };

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

    locale_data.unique_formats.generate(generator, "NumberFormatImpl"sv, "s_number_formats"sv, 10);
    locale_data.unique_format_lists.generate(generator, s_number_format_index_type, "s_number_format_lists"sv);
    locale_data.unique_symbols.generate(generator, s_string_index_type, "s_numeric_symbol_lists"sv);
    locale_data.unique_systems.generate(generator, "NumberSystem"sv, "s_number_systems"sv, 10);
    locale_data.unique_units.generate(generator, "Unit"sv, "s_units"sv, 10);

    auto append_map = [&](String name, auto type, auto const& map) {
        generator.set("name", name);
        generator.set("type", type);
        generator.set("size", String::number(map.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@> @name@ { {)~~~");

        bool first = true;
        for (auto const& item : map) {
            generator.append(first ? " " : ", ");
            generator.append(String::number(item.value));
            first = false;
        }

        generator.append(" } };");
    };

    generate_mapping(generator, locale_data.locales, s_number_system_index_type, "s_locale_number_systems"sv, "s_number_systems_{}", nullptr, [&](auto const& name, auto const& value) { append_map(name, s_number_system_index_type, value.number_systems); });
    generate_mapping(generator, locale_data.locales, s_unit_index_type, "s_locale_units"sv, "s_units_{}", nullptr, [&](auto const& name, auto const& value) { append_map(name, s_unit_index_type, value.units); });

    generator.append(R"~~~(
static NumberSystem const* find_number_system(StringView locale, StringView system)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return nullptr;

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto const& number_systems = s_locale_number_systems.at(locale_index);

    for (auto system_index : number_systems) {
        auto const& number_system = s_number_systems.at(system_index);

        if (system == s_string_list[number_system.system])
            return &number_system;
    };

    return nullptr;
}

Optional<StringView> get_number_system_symbol(StringView locale, StringView system, NumericSymbol symbol)
{
    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr) {
        auto symbols = s_numeric_symbol_lists.at(number_system->symbols);

        auto symbol_index = to_underlying(symbol);
        if (symbol_index >= symbols.size())
            return {};

        return s_string_list[symbols[symbol_index]];
    }

    return {};
}

Optional<NumberGroupings> get_number_system_groupings(StringView locale, StringView system)
{
    if (auto const* number_system = find_number_system(locale, system); number_system != nullptr)
        return NumberGroupings { number_system->primary_grouping_size, number_system->secondary_grouping_size };
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
        case CompactNumberFormatType::CurrencyShort:
            number_format_list_index = number_system->currency_short_formats;
            break;
        }

        auto number_formats = s_number_format_lists.at(number_format_list_index);
        formats.ensure_capacity(number_formats.size());

        for (auto number_format : number_formats)
            formats.append(s_number_formats[number_format].to_unicode_number_format());
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

        if (unit == s_string_list[units.unit])
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
            formats.append(s_number_formats[number_format].to_unicode_number_format());
    }

    return formats;
}

}
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path = nullptr;
    StringView generated_implementation_path = nullptr;
    StringView numbers_path = nullptr;
    StringView units_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(numbers_path, "Path to cldr-numbers directory", "numbers-path", 'n', "numbers-path");
    args_parser.add_option(units_path, "Path to cldr-units directory", "units-path", 'u', "units-path");
    args_parser.parse(arguments);

    auto open_file = [&](StringView path) -> ErrorOr<NonnullRefPtr<Core::File>> {
        if (path.is_empty()) {
            args_parser.print_usage(stderr, arguments.argv[0]);
            return Error::from_string_literal("Must provide all command line options"sv);
        }

        return Core::File::open(path, Core::OpenMode::ReadWrite);
    };

    auto generated_header_file = TRY(open_file(generated_header_path));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path));

    UnicodeLocaleData locale_data;
    TRY(parse_all_locales(numbers_path, units_path, locale_data));

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
