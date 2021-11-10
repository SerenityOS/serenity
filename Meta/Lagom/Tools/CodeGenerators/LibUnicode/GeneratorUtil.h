/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>

inline void ensure_from_string_types_are_generated(SourceGenerator& generator)
{
    static bool generated_from_string_types = false;
    if (generated_from_string_types)
        return;

    generator.append(R"~~~(
template <typename ValueType>
struct HashValuePair {
    unsigned hash { 0 };
    ValueType value {};
};

template <typename ValueType>
struct HashValueComparator
{
    constexpr int operator()(unsigned hash, HashValuePair<ValueType> const& pair)
    {
        if (hash > pair.hash)
            return 1;
        if (hash < pair.hash)
            return -1;
        return 0;
    }
};
)~~~");

    generated_from_string_types = true;
}

template<typename ValueType>
using HashValueMap = HashMap<unsigned, ValueType>;

template<typename ValueType>
void generate_value_from_string(SourceGenerator& generator, StringView method_name_format, StringView value_type, StringView value_name, HashValueMap<ValueType> hashes, Optional<StringView> return_type = {}, StringView return_format = "{}"sv)
{
    ensure_from_string_types_are_generated(generator);

    generator.set("method_name", String::formatted(method_name_format, value_name));
    generator.set("value_type", value_type);
    generator.set("value_name", value_name);
    generator.set("return_type", return_type.has_value() ? *return_type : value_type);
    generator.set("size", String::number(hashes.size()));

    generator.append(R"~~~(
Optional<@return_type@> @method_name@(StringView key)
{
    constexpr Array<HashValuePair<@value_type@>, @size@> hash_pairs { {
        )~~~");

    auto hash_keys = hashes.keys();
    quick_sort(hash_keys);

    constexpr size_t max_values_per_row = 10;
    size_t values_in_current_row = 0;

    for (auto hash_key : hash_keys) {
        if (values_in_current_row++ > 0)
            generator.append(" ");

        if constexpr (IsIntegral<ValueType>)
            generator.set("value"sv, String::number(hashes.get(hash_key).value()));
        else
            generator.set("value"sv, String::formatted("{}::{}", value_type, hashes.get(hash_key).value()));

        generator.set("hash"sv, String::number(hash_key));
        generator.append("{ @hash@U, @value@ },"sv);

        if (values_in_current_row == max_values_per_row) {
            generator.append("\n        ");
            values_in_current_row = 0;
        }
    }

    generator.set("return_statement", String::formatted(return_format, "value->value"sv));
    generator.append(R"~~~(
    } };

    if (auto const* value = binary_search(hash_pairs, key.hash(), nullptr, HashValueComparator<@value_type@> {}))
        return @return_statement@;
    return {};
}
)~~~");
}
