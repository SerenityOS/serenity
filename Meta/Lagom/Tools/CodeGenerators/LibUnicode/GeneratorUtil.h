/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/HashFunctions.h>
#include <AK/HashMap.h>
#include <AK/JsonValue.h>
#include <AK/LexicalPath.h>
#include <AK/NumericLimits.h>
#include <AK/Optional.h>
#include <AK/QuickSort.h>
#include <AK/SourceGenerator.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Traits.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/Stream.h>
#include <LibUnicode/Locale.h>

template<class T>
inline constexpr bool StorageTypeIsList = false;

template<class T>
inline constexpr bool StorageTypeIsList<Vector<T>> = true;

template<typename T>
concept IntegralOrEnum = Integral<T> || Enum<T>;

template<IntegralOrEnum T>
struct AK::Traits<Vector<T>> : public GenericTraits<Vector<T>> {
    static unsigned hash(Vector<T> const& list)
    {
        auto hash = int_hash(static_cast<u32>(list.size()));

        for (auto value : list) {
            if constexpr (Enum<T>)
                hash = pair_int_hash(hash, to_underlying(value));
            else
                hash = pair_int_hash(hash, value);
        }

        return hash;
    }
};

template<typename StorageType, typename IndexType>
class UniqueStorage {
public:
    IndexType ensure(StorageType value)
    {
        // We maintain a set of unique values in two structures: a vector which stores the values in
        // the order they are added, and a hash map which maps that value to its index in the vector.
        // The vector is to ensure the values are generated in an easily known order, and the map is
        // to allow quickly deciding if a value is actually unique (otherwise, we'd have to linearly
        // search the vector for each value).
        //
        // Also note that index 0 will be reserved for the default-initialized value, so the index
        // returned from this method is actually the real index in the vector + 1.
        if (auto index = m_storage_indices.get(value); index.has_value())
            return *index;

        m_storage.append(move(value));
        size_t index = m_storage.size();

        VERIFY(index < NumericLimits<IndexType>::max());

        auto storage_index = static_cast<IndexType>(index);
        m_storage_indices.set(m_storage.last(), storage_index);

        return storage_index;
    }

    StorageType const& get(IndexType index) const
    {
        if (index == 0) {
            static StorageType empty {};
            return empty;
        }

        VERIFY(index <= m_storage.size());
        return m_storage.at(index - 1);
    }

    void generate(SourceGenerator& generator, StringView type, StringView name, size_t max_values_per_row) requires(!StorageTypeIsList<StorageType>)
    {
        generator.set("type"sv, type);
        generator.set("name"sv, name);
        generator.set("size"sv, String::number(m_storage.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@ + 1> @name@ { {
    {})~~~");

        size_t values_in_current_row = 1;

        for (auto const& value : m_storage) {
            if (values_in_current_row++ > 0)
                generator.append(", ");

            if constexpr (IsSame<StorageType, String>)
                generator.append(String::formatted("\"{}\"sv", value));
            else
                generator.append(String::formatted("{}", value));

            if (values_in_current_row == max_values_per_row) {
                values_in_current_row = 0;
                generator.append(",\n    ");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    }

    void generate(SourceGenerator& generator, StringView type, StringView name) requires(StorageTypeIsList<StorageType>)
    {
        generator.set("type"sv, type);
        generator.set("name"sv, name);

        for (size_t i = 0; i < m_storage.size(); ++i) {
            auto const& list = m_storage[i];

            generator.set("index"sv, String::number(i));
            generator.set("size"sv, String::number(list.size()));

            generator.append(R"~~~(
static constexpr Array<@type@, @size@> @name@@index@ { {)~~~");

            bool first = true;
            for (auto const& value : list) {
                generator.append(first ? " "sv : ", "sv);
                generator.append(String::formatted("{}", value));
                first = false;
            }

            generator.append(" } };");
        }

        generator.set("size"sv, String::number(m_storage.size()));

        generator.append(R"~~~(

static constexpr Array<Span<@type@ const>, @size@ + 1> @name@ { {
    {})~~~");

        constexpr size_t max_values_per_row = 10;
        size_t values_in_current_row = 1;

        for (size_t i = 0; i < m_storage.size(); ++i) {
            if (values_in_current_row++ > 0)
                generator.append(", ");

            generator.set("index"sv, String::number(i));
            generator.append("@name@@index@.span()");

            if (values_in_current_row == max_values_per_row) {
                values_in_current_row = 0;
                generator.append(",\n    ");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    }

    // clang-format off
    // clang-format gets confused by the requires() clauses above, and formats this section very weirdly.
protected:
    Vector<StorageType> m_storage;
    HashMap<StorageType, IndexType> m_storage_indices;
    // clang-format on
};

template<typename StringIndexType>
class UniqueStringStorage : public UniqueStorage<String, StringIndexType> {
    using Base = UniqueStorage<String, StringIndexType>;

public:
    // The goal of the string table generator is to ensure the table is located within the read-only
    // section of the shared library. If StringViews are generated directly, the table will be located
    // in the initialized data section. So instead, we generate run-length encoded (RLE) arrays to
    // represent the strings.
    void generate(SourceGenerator& generator) const
    {
        constexpr size_t max_values_per_row = 300;
        size_t values_in_current_row = 0;

        auto append_hex_value = [&](auto value) {
            if (values_in_current_row++ > 0)
                generator.append(", ");

            generator.append(String::formatted("{:#x}", value));

            if (values_in_current_row == max_values_per_row) {
                values_in_current_row = 0;
                generator.append(",\n    ");
            }
        };

        Vector<u32> string_indices;
        string_indices.ensure_capacity(Base::m_storage.size());
        u32 next_index { 0 };

        for (auto const& string : Base::m_storage) {
            // Ensure the string length may be encoded as two u8s.
            VERIFY(string.length() <= NumericLimits<u16>::max());

            string_indices.unchecked_append(next_index);
            next_index += string.length() + 2;
        }

        generator.set("size", String::number(next_index));
        generator.append(R"~~~(
static constexpr Array<u8, @size@> s_encoded_strings { {
    )~~~");

        for (auto const& string : Base::m_storage) {
            auto length = string.length();
            append_hex_value((length & 0xff00) >> 8);
            append_hex_value(length & 0x00ff);

            for (auto ch : string)
                append_hex_value(static_cast<u8>(ch));
        }

        generator.append(R"~~~(
} };
)~~~");

        generator.set("size", String::number(string_indices.size()));
        generator.append(R"~~~(
static constexpr Array<u32, @size@> s_encoded_string_indices { {
    )~~~");

        values_in_current_row = 0;
        for (auto index : string_indices)
            append_hex_value(index);

        generator.append(R"~~~(
} };

static constexpr StringView decode_string(size_t index)
{
    if (index == 0)
        return {};

    index = s_encoded_string_indices[index - 1];

    auto length_high = s_encoded_strings[index];
    auto length_low = s_encoded_strings[index + 1];

    size_t length = (length_high << 8) | length_low;
    if (length == 0)
        return {};

    auto const* start = &s_encoded_strings[index + 2];
    return { reinterpret_cast<char const*>(start), length };
}
)~~~");
    }
};

struct Alias {
    String name;
    String alias;
};

template<typename StringIndexType>
struct CanonicalLanguageID {
    static ErrorOr<CanonicalLanguageID> parse(UniqueStringStorage<StringIndexType>& unique_strings, StringView language)
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
            return Error::from_string_literal("Expected language subtag");
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
                return Error::from_string_literal("Expected variant subtag");
            language_id.variants.append(unique_strings.ensure(segments[index++]));
        }

        return language_id;
    }

    StringIndexType language { 0 };
    StringIndexType script { 0 };
    StringIndexType region { 0 };
    Vector<StringIndexType> variants {};
};

inline ErrorOr<NonnullOwnPtr<Core::Stream::BufferedFile>> open_file(StringView path, Core::Stream::OpenMode mode)
{
    if (path.is_empty())
        return Error::from_string_literal("Provided path is empty, please provide all command line options");

    auto file = TRY(Core::Stream::File::open(path, mode));
    return Core::Stream::BufferedFile::create(move(file));
}

inline ErrorOr<JsonValue> read_json_file(StringView path)
{
    auto file = TRY(open_file(path, Core::Stream::OpenMode::Read));

    StringBuilder builder;
    Array<u8, 4096> buffer;

    // FIXME: When Core::Stream supports reading an entire file, use that.
    while (TRY(file->can_read_line())) {
        auto bytes_read = TRY(file->read(buffer));
        TRY(builder.try_append(StringView { bytes_read }));
    }

    return JsonValue::from_string(builder.build());
}

inline ErrorOr<Core::DirIterator> path_to_dir_iterator(String path, StringView subpath = "main"sv)
{
    LexicalPath lexical_path(move(path));
    if (!subpath.is_empty())
        lexical_path = lexical_path.append(subpath);

    Core::DirIterator iterator(lexical_path.string(), Core::DirIterator::SkipParentAndBaseDir);
    if (iterator.has_error()) {
        // FIXME: Make Core::DirIterator return a StringView for its error
        //        string.
        auto const* error_string_ptr = iterator.error_string();
        return Error::from_string_view({ error_string_ptr, strlen(error_string_ptr) });
    }

    return iterator;
}

inline ErrorOr<String> next_path_from_dir_iterator(Core::DirIterator& iterator)
{
    auto next_path = iterator.next_full_path();
    if (iterator.has_error()) {
        // FIXME: Make Core::DirIterator return a StringView for its error
        //        string.
        auto const* error_string_ptr = iterator.error_string();
        return Error::from_string_view({ error_string_ptr, strlen(error_string_ptr) });
    }

    return next_path;
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

struct ValueFromStringOptions {
    Optional<StringView> return_type {};
    StringView return_format { "{}"sv };
    CaseSensitivity sensitivity { CaseSensitivity::CaseSensitive };
};

template<typename ValueType>
void generate_value_from_string(SourceGenerator& generator, StringView method_name_format, StringView value_type, StringView value_name, HashValueMap<ValueType> hashes, ValueFromStringOptions options = {})
{
    ensure_from_string_types_are_generated(generator);

    generator.set("method_name", String::formatted(method_name_format, value_name));
    generator.set("value_type", value_type);
    generator.set("value_name", value_name);
    generator.set("return_type", options.return_type.has_value() ? *options.return_type : value_type);
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

    generator.set("return_statement", String::formatted(options.return_format, "value->value"sv));
    generator.append(R"~~~(
    } };
)~~~");

    if (options.sensitivity == CaseSensitivity::CaseSensitive) {
        generator.append(R"~~~(
    auto hash = key.hash();
)~~~");
    } else {
        generator.append(R"~~~(
    auto hash = CaseInsensitiveStringViewTraits::hash(key);
)~~~");
    }

    generator.append(R"~~~(
    if (auto const* value = binary_search(hash_pairs, hash, nullptr, HashValueComparator<@value_type@> {}))
        return @return_statement@;
    return {};
}
)~~~");
}

template<typename IdentifierFormatter>
void generate_value_to_string(SourceGenerator& generator, StringView method_name_format, StringView value_type, StringView value_name, IdentifierFormatter&& format_identifier, Span<String const> values)
{
    generator.set("method_name", String::formatted(method_name_format, value_name));
    generator.set("value_type", value_type);
    generator.set("value_name", value_name);

    generator.append(R"~~~(
StringView @method_name@(@value_type@ @value_name@)
{
    using enum @value_type@;

    switch (@value_name@) {)~~~");

    for (auto const& value : values) {
        generator.set("enum_value", format_identifier(value_type, value));
        generator.set("string_value", value);
        generator.append(R"~~~(
    case @enum_value@:
        return "@string_value@"sv;)~~~");
    }

    generator.append(R"~~~(
    }

    VERIFY_NOT_REACHED();
}
)~~~");
}

template<typename IdentifierFormatter>
void generate_enum(SourceGenerator& generator, IdentifierFormatter&& format_identifier, StringView name, StringView default_, Vector<String>& values, Vector<Alias> aliases = {})
{
    quick_sort(values, [](auto const& value1, auto const& value2) { return value1.to_lowercase() < value2.to_lowercase(); });
    quick_sort(aliases, [](auto const& alias1, auto const& alias2) { return alias1.alias.to_lowercase() < alias2.alias.to_lowercase(); });

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

    for (auto const& alias : aliases) {
        generator.set("alias", format_identifier(name, alias.alias));
        generator.set("value", format_identifier(name, alias.name));
        generator.append(R"~~~(
    @alias@ = @value@,)~~~");
    }

    generator.append(R"~~~(
};
)~~~");
}

template<typename LocalesType, typename IdentifierFormatter, typename ListFormatter>
void generate_mapping(SourceGenerator& generator, LocalesType const& locales, StringView type, StringView name, StringView format, IdentifierFormatter&& format_identifier, ListFormatter&& format_list)
{
    auto format_mapping_name = [&](StringView format, StringView name) {
        String mapping_name;

        if constexpr (IsNullPointer<IdentifierFormatter>)
            mapping_name = name.replace("-"sv, "_"sv, ReplaceMode::All);
        else
            mapping_name = format_identifier(type, name);

        return String::formatted(format, mapping_name.to_lowercase());
    };

    Vector<String> mapping_names;

    for (auto const& locale : locales) {
        String mapping_name;

        if constexpr (requires { locale.key; }) {
            mapping_name = format_mapping_name(format, locale.key);
            format_list(mapping_name, locale.value);
        } else {
            mapping_name = format_mapping_name(format, locale);
            format_list(mapping_name, locale);
        }

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

template<typename T>
void generate_available_values(SourceGenerator& generator, StringView name, Vector<T> const& values, Vector<Alias> const& aliases = {}, Function<bool(StringView)> value_filter = {})
{
    generator.set("name", name);

    generator.append(R"~~~(
Span<StringView const> @name@()
{
    static constexpr auto values = Array {)~~~");

    bool first = true;
    for (auto const& value : values) {
        if (value_filter && !value_filter(value))
            continue;

        generator.append(first ? " "sv : ", "sv);
        first = false;

        if (auto it = aliases.find_if([&](auto const& alias) { return alias.alias == value; }); it != aliases.end())
            generator.append(String::formatted("\"{}\"sv", it->name));
        else
            generator.append(String::formatted("\"{}\"sv", value));
    }

    generator.append(R"~~~( };
    return values.span();
}
)~~~");
}
