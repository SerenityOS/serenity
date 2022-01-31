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
#include <LibJS/Runtime/Value.h>
#include <LibUnicode/Forward.h>

namespace JS::Intl {

using Fallback = Variant<Empty, bool, StringView>;

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

constexpr auto sanctioned_simple_unit_identifiers()
{
    return AK::Array { "acre"sv, "bit"sv, "byte"sv, "celsius"sv, "centimeter"sv, "day"sv, "degree"sv, "fahrenheit"sv, "fluid-ounce"sv, "foot"sv, "gallon"sv, "gigabit"sv, "gigabyte"sv, "gram"sv, "hectare"sv, "hour"sv, "inch"sv, "kilobit"sv, "kilobyte"sv, "kilogram"sv, "kilometer"sv, "liter"sv, "megabit"sv, "megabyte"sv, "meter"sv, "mile"sv, "mile-scandinavian"sv, "milliliter"sv, "millimeter"sv, "millisecond"sv, "minute"sv, "month"sv, "ounce"sv, "percent"sv, "petabyte"sv, "pound"sv, "second"sv, "stone"sv, "terabit"sv, "terabyte"sv, "week"sv, "yard"sv, "year"sv };
}

Optional<Unicode::LocaleID> is_structurally_valid_language_tag(StringView locale);
String canonicalize_unicode_locale_id(Unicode::LocaleID& locale);
bool is_well_formed_currency_code(StringView currency);
bool is_well_formed_unit_identifier(StringView unit_identifier);
ThrowCompletionOr<Vector<String>> canonicalize_locale_list(GlobalObject&, Value locales);
Optional<String> best_available_locale(StringView locale);
String insert_unicode_extension_and_canonicalize(Unicode::LocaleID locale_id, Unicode::LocaleExtension extension);
LocaleResult resolve_locale(Vector<String> const& requested_locales, LocaleOptions const& options, Span<StringView const> relevant_extension_keys);
Vector<String> lookup_supported_locales(Vector<String> const& requested_locales);
Vector<String> best_fit_supported_locales(Vector<String> const& requested_locales);
ThrowCompletionOr<Array*> supported_locales(GlobalObject&, Vector<String> const& requested_locales, Value options);
ThrowCompletionOr<Object*> coerce_options_to_object(GlobalObject& global_object, Value options);
ThrowCompletionOr<Value> get_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, Value::Type type, Span<StringView const> values, Fallback fallback);
ThrowCompletionOr<Optional<int>> default_number_option(GlobalObject& global_object, Value value, int minimum, int maximum, Optional<int> fallback);
ThrowCompletionOr<Optional<int>> get_number_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, int minimum, int maximum, Optional<int> fallback);
Vector<PatternPartition> partition_pattern(StringView pattern);

template<size_t Size>
ThrowCompletionOr<Value> get_option(GlobalObject& global_object, Object const& options, PropertyKey const& property, Value::Type type, StringView const (&values)[Size], Fallback fallback)
{
    return get_option(global_object, options, property, type, Span<StringView const> { values }, fallback);
}

}
