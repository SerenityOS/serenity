/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Value.h>
#include <LibUnicode/Forward.h>

namespace JS::Intl {

struct LocaleOptions {
    Value locale_matcher;
    Optional<String> ca; // [[Calendar]]
    Optional<String> co; // [[Collation]]
    Optional<String> hc; // [[HourCycle]]
    Optional<String> kf; // [[CaseFirst]]
    Optional<String> kn; // [[Numeric]]
    Optional<String> nu; // [[NumberingSystem]]
};

struct LocaleResult {
    String locale;
    String data_locale;
    Optional<String> ca; // [[Calendar]]
    Optional<String> co; // [[Collation]]
    Optional<String> hc; // [[HourCycle]]
    Optional<String> kf; // [[CaseFirst]]
    Optional<String> kn; // [[Numeric]]
    Optional<String> nu; // [[NumberingSystem]]
};

struct PatternPartition {
    PatternPartition() = default;

    PatternPartition(StringView type_string, String value_string)
        : type(type_string)
        , value(move(value_string))
    {
    }

    StringView type;
    String value;
};

struct PatternPartitionWithSource : public PatternPartition {
    static Vector<PatternPartitionWithSource> create_from_parent_list(Vector<PatternPartition> partitions)
    {
        Vector<PatternPartitionWithSource> result;
        result.ensure_capacity(partitions.size());

        for (auto& partition : partitions) {
            PatternPartitionWithSource partition_with_source {};
            partition_with_source.type = partition.type;
            partition_with_source.value = move(partition.value);
            result.append(move(partition_with_source));
        }

        return result;
    }

    bool operator==(PatternPartitionWithSource const& other) const
    {
        return (type == other.type) && (value == other.value) && (source == other.source);
    }

    StringView source;
};

// Table 2: Single units sanctioned for use in ECMAScript, https://tc39.es/ecma402/#table-sanctioned-single-unit-identifiers
constexpr auto sanctioned_single_unit_identifiers()
{
    return AK::Array { "acre"sv, "bit"sv, "byte"sv, "celsius"sv, "centimeter"sv, "day"sv, "degree"sv, "fahrenheit"sv, "fluid-ounce"sv, "foot"sv, "gallon"sv, "gigabit"sv, "gigabyte"sv, "gram"sv, "hectare"sv, "hour"sv, "inch"sv, "kilobit"sv, "kilobyte"sv, "kilogram"sv, "kilometer"sv, "liter"sv, "megabit"sv, "megabyte"sv, "meter"sv, "mile"sv, "mile-scandinavian"sv, "milliliter"sv, "millimeter"sv, "millisecond"sv, "minute"sv, "month"sv, "ounce"sv, "percent"sv, "petabyte"sv, "pound"sv, "second"sv, "stone"sv, "terabit"sv, "terabyte"sv, "week"sv, "yard"sv, "year"sv };
}

// Additional single units used in ECMAScript required by the Intl.DurationFormat proposal
constexpr auto extra_sanctioned_single_unit_identifiers()
{
    return AK::Array { "microsecond"sv, "nanosecond"sv };
}

using StringOrBoolean = Variant<StringView, bool>;

Optional<Unicode::LocaleID> is_structurally_valid_language_tag(StringView locale);
String canonicalize_unicode_locale_id(Unicode::LocaleID& locale);
bool is_well_formed_currency_code(StringView currency);
bool is_well_formed_unit_identifier(StringView unit_identifier);
ThrowCompletionOr<Vector<String>> canonicalize_locale_list(VM&, Value locales);
Optional<String> best_available_locale(StringView locale);
String insert_unicode_extension_and_canonicalize(Unicode::LocaleID locale_id, Unicode::LocaleExtension extension);
LocaleResult resolve_locale(Vector<String> const& requested_locales, LocaleOptions const& options, Span<StringView const> relevant_extension_keys);
Vector<String> lookup_supported_locales(Vector<String> const& requested_locales);
Vector<String> best_fit_supported_locales(Vector<String> const& requested_locales);
ThrowCompletionOr<Array*> supported_locales(VM&, Vector<String> const& requested_locales, Value options);
ThrowCompletionOr<Object*> coerce_options_to_object(VM&, Value options);
ThrowCompletionOr<StringOrBoolean> get_string_or_boolean_option(VM&, Object const& options, PropertyKey const& property, Span<StringView const> values, StringOrBoolean true_value, StringOrBoolean falsy_value, StringOrBoolean fallback);
ThrowCompletionOr<Optional<int>> default_number_option(VM&, Value value, int minimum, int maximum, Optional<int> fallback);
ThrowCompletionOr<Optional<int>> get_number_option(VM&, Object const& options, PropertyKey const& property, int minimum, int maximum, Optional<int> fallback);
Vector<PatternPartition> partition_pattern(StringView pattern);

template<size_t Size>
ThrowCompletionOr<StringOrBoolean> get_string_or_boolean_option(VM& vm, Object const& options, PropertyKey const& property, StringView const (&values)[Size], StringOrBoolean true_value, StringOrBoolean falsy_value, StringOrBoolean fallback)
{
    return get_string_or_boolean_option(vm, options, property, Span<StringView const> { values }, move(true_value), move(falsy_value), move(fallback));
}

// NOTE: ECMA-402's GetOption is being removed in favor of a shared ECMA-262 GetOption in the Temporal proposal.
// Until Temporal is merged into ECMA-262, our implementation lives in the Temporal-specific AO file & namespace.
using Temporal::get_option;
using Temporal::OptionType;

}
