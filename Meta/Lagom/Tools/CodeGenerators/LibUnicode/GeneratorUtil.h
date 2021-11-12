/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/HashMap.h>
#include <AK/LexicalPath.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibUnicode/Locale.h>

template<typename StringIndexType>
class UniqueStringStorage {
public:
    StringIndexType ensure(String string)
    {
        // We maintain a set of unique strings in two structures: a vector which owns the unique string,
        // and a hash map which maps that string to its index in the vector. The vector is to ensure the
        // strings are generated in an easily known order, and the map is to allow quickly deciding if a
        // string is actually unique (otherwise, we'd have to linear-search the vector for each string).
        //
        // Also note that index 0 will be reserved for the empty string, so the index returned from this
        // method is actually the real index in the vector + 1.
        if (auto index = m_unique_string_indices.get(string); index.has_value())
            return *index;

        m_unique_strings.append(move(string));
        size_t index = m_unique_strings.size();

        VERIFY(index < NumericLimits<StringIndexType>::max());

        auto string_index = static_cast<StringIndexType>(index);
        m_unique_string_indices.set(m_unique_strings.last(), string_index);

        return string_index;
    }

    StringView get(StringIndexType index) const
    {
        if (index == 0)
            return {};

        VERIFY(index <= m_unique_strings.size());
        return m_unique_strings.at(index - 1);
    }

    void generate(SourceGenerator& generator)
    {
        generator.set("size"sv, String::number(m_unique_strings.size()));

        generator.append(R"~~~(
static constexpr Array<StringView, @size@ + 1> s_string_list { {
    {})~~~");

        constexpr size_t max_strings_per_row = 40;
        size_t strings_in_current_row = 1;

        for (auto const& string : m_unique_strings) {
            if (strings_in_current_row++ > 0)
                generator.append(", ");

            generator.append(String::formatted("\"{}\"sv", string));

            if (strings_in_current_row == max_strings_per_row) {
                strings_in_current_row = 0;
                generator.append(",\n    ");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    }

private:
    Vector<String> m_unique_strings;
    HashMap<StringView, StringIndexType> m_unique_string_indices;
};

template<typename StringIndexType>
struct CanonicalLanguageID {
    static Optional<CanonicalLanguageID> parse(UniqueStringStorage<StringIndexType>& unique_strings, StringView language)
    {
        CanonicalLanguageID language_id {};

        auto segments = language.split_view('-');
        VERIFY(!segments.is_empty());
        size_t index = 0;

        if (Unicode::is_unicode_language_subtag(segments[index])) {
            language_id.language = unique_strings.ensure(segments[index]);
            if (segments.size() == ++index)
                return language_id;
        } else {
            return {};
        }

        if (Unicode::is_unicode_script_subtag(segments[index])) {
            language_id.script = unique_strings.ensure(segments[index]);
            if (segments.size() == ++index)
                return language_id;
        }

        if (Unicode::is_unicode_region_subtag(segments[index])) {
            language_id.region = unique_strings.ensure(segments[index]);
            if (segments.size() == ++index)
                return language_id;
        }

        while (index < segments.size()) {
            if (!Unicode::is_unicode_variant_subtag(segments[index]))
                return {};
            language_id.variants.append(unique_strings.ensure(segments[index++]));
        }

        return language_id;
    }

    StringIndexType language { 0 };
    StringIndexType script { 0 };
    StringIndexType region { 0 };
    Vector<StringIndexType> variants {};
};

inline Core::DirIterator path_to_dir_iterator(String path)
{
    LexicalPath lexical_path(move(path));
    lexical_path = lexical_path.append("main"sv);
    VERIFY(Core::File::is_directory(lexical_path.string()));

    Core::DirIterator iterator(lexical_path.string(), Core::DirIterator::SkipParentAndBaseDir);
    if (iterator.has_error()) {
        warnln("{}: {}", lexical_path.string(), iterator.error_string());
        VERIFY_NOT_REACHED();
    }

    return iterator;
}

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

template<typename IdentifierFormatter>
void generate_enum(SourceGenerator& generator, IdentifierFormatter&& format_identifier, StringView name, StringView default_, Vector<String>& values)
{
    quick_sort(values);

    generator.set("name", name);
    generator.set("underlying", ((values.size() + !default_.is_empty()) < 256) ? "u8"sv : "u16"sv);

    generator.append(R"~~~(
enum class @name@ : @underlying@ {)~~~");

    if (!default_.is_empty()) {
        generator.set("default", default_);
        generator.append(R"~~~(
    @default@,)~~~");
    }

    for (auto const& value : values) {
        generator.set("value", format_identifier(name, value));
        generator.append(R"~~~(
    @value@,)~~~");
    }

    generator.append(R"~~~(
};
)~~~");
}

template<typename LocalesType, typename ListFormatter>
void generate_mapping(SourceGenerator& generator, LocalesType const& locales, StringView type, StringView name, StringView format, ListFormatter&& format_list)
{
    auto format_mapping_name = [](StringView format, StringView name) {
        auto mapping_name = name.to_lowercase_string().replace("-"sv, "_"sv, true);
        return String::formatted(format, mapping_name);
    };

    Vector<String> mapping_names;

    for (auto const& locale : locales) {
        auto mapping_name = format_mapping_name(format, locale.key);
        format_list(mapping_name, locale.value);
        mapping_names.append(move(mapping_name));
    }

    quick_sort(mapping_names);

    generator.set("type", type);
    generator.set("name", name);
    generator.set("size", String::number(locales.size()));
    generator.append(R"~~~(
static constexpr Array<Span<@type@ const>, @size@> @name@ { {
    )~~~");

    constexpr size_t max_values_per_row = 10;
    size_t values_in_current_row = 0;

    for (auto& mapping_name : mapping_names) {
        if (values_in_current_row++ > 0)
            generator.append(" ");

        generator.set("name", move(mapping_name));
        generator.append("@name@.span(),");

        if (values_in_current_row == max_values_per_row) {
            values_in_current_row = 0;
            generator.append("\n    ");
        }
    }

    generator.append(R"~~~(
} };
)~~~");
}
