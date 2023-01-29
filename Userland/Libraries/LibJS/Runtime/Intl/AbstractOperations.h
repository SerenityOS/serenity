/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Intl/SingleUnitIdentifiers.h>
#include <LibJS/Runtime/Temporal/AbstractOperations.h>
#include <LibJS/Runtime/Value.h>
#include <LibLocale/Forward.h>

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

using StringOrBoolean = Variant<StringView, bool>;

ThrowCompletionOr<Optional<::Locale::LocaleID>> is_structurally_valid_language_tag(VM&, StringView locale);
ThrowCompletionOr<String> canonicalize_unicode_locale_id(VM&, ::Locale::LocaleID& locale);
bool is_well_formed_currency_code(StringView currency);
bool is_well_formed_unit_identifier(StringView unit_identifier);
ThrowCompletionOr<Vector<String>> canonicalize_locale_list(VM&, Value locales);
Optional<StringView> best_available_locale(StringView locale);
ThrowCompletionOr<String> insert_unicode_extension_and_canonicalize(VM&, ::Locale::LocaleID locale_id, ::Locale::LocaleExtension extension);
ThrowCompletionOr<LocaleResult> resolve_locale(VM&, Vector<String> const& requested_locales, LocaleOptions const& options, Span<StringView const> relevant_extension_keys);
ThrowCompletionOr<Array*> supported_locales(VM&, Vector<String> const& requested_locales, Value options);
ThrowCompletionOr<Object*> coerce_options_to_object(VM&, Value options);
ThrowCompletionOr<StringOrBoolean> get_string_or_boolean_option(VM&, Object const& options, PropertyKey const& property, Span<StringView const> values, StringOrBoolean true_value, StringOrBoolean falsy_value, StringOrBoolean fallback);
ThrowCompletionOr<Optional<int>> default_number_option(VM&, Value value, int minimum, int maximum, Optional<int> fallback);
ThrowCompletionOr<Optional<int>> get_number_option(VM&, Object const& options, PropertyKey const& property, int minimum, int maximum, Optional<int> fallback);
ThrowCompletionOr<Vector<PatternPartition>> partition_pattern(VM&, StringView pattern);

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
