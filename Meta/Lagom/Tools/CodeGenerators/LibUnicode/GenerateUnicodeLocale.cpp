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
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibUnicode/Locale.h>

using StringIndexType = u16;
constexpr auto s_string_index_type = "u16"sv;

struct ListPatterns {
    String type;
    String style;
    StringIndexType start { 0 };
    StringIndexType middle { 0 };
    StringIndexType end { 0 };
    StringIndexType pair { 0 };
};

struct Locale {
    String language;
    Optional<String> territory;
    Optional<String> variant;
    HashMap<String, StringIndexType> languages;
    HashMap<String, StringIndexType> territories;
    HashMap<String, StringIndexType> scripts;
    HashMap<String, StringIndexType> currencies;
    HashMap<String, StringIndexType> keywords;
    Vector<ListPatterns> list_patterns;
};

struct CanonicalLanguageID {
    StringIndexType language { 0 };
    StringIndexType script { 0 };
    StringIndexType region { 0 };
    Vector<StringIndexType> variants {};
};

struct LanguageMapping {
    CanonicalLanguageID key {};
    CanonicalLanguageID alias {};
};

struct UnicodeLocaleData {
    Vector<String> unique_strings;
    HashMap<StringView, StringIndexType> unique_string_indices;
    HashMap<String, Locale> locales;
    Vector<String> languages;
    Vector<String> territories;
    Vector<String> scripts;
    Vector<String> variants;
    Vector<String> currencies;
    Vector<String> keywords;
    Vector<String> list_pattern_types;
    Vector<String> list_pattern_styles;
    HashMap<String, StringIndexType> language_aliases;
    HashMap<String, StringIndexType> territory_aliases;
    HashMap<String, StringIndexType> script_aliases;
    HashMap<String, StringIndexType> variant_aliases;
    HashMap<String, StringIndexType> subdivision_aliases;
    Vector<LanguageMapping> complex_mappings;
    Vector<LanguageMapping> likely_subtags;
    size_t max_variant_size { 0 };
};

static StringIndexType ensure_unique_string(UnicodeLocaleData& locale_data, String string)
{
    // We maintain a set of unique strings in two structures: a vector which owns the unique string,
    // and a hash map which maps that string to its index in the vector. The vector is to ensure the
    // strings are generated in an easily known order, and the map is to allow quickly deciding if a
    // string is actually unique (otherwise, we'd have to linear-search the vector for each string).
    //
    // Also note that index 0 will be reserved for the empty string, so the index returned from this
    // method is actually the real index in the vector + 1.
    if (auto index = locale_data.unique_string_indices.get(string); index.has_value())
        return *index;

    locale_data.unique_strings.append(move(string));
    size_t index = locale_data.unique_strings.size();

    // There are currently on the order of 46K unique strings in UnicodeLocale.cpp.
    // If that number reaches 2^16, bump the StringIndexType alias to a u32.
    VERIFY(index < NumericLimits<StringIndexType>::max());

    auto string_index = static_cast<StringIndexType>(index);
    locale_data.unique_string_indices.set(locale_data.unique_strings.last(), string_index);

    return string_index;
}

static StringView get_unique_string(UnicodeLocaleData& locale_data, StringIndexType index)
{
    if (index == 0)
        return {};

    VERIFY(index <= locale_data.unique_strings.size());
    return locale_data.unique_strings.at(index - 1);
}

static Optional<CanonicalLanguageID> parse_language(UnicodeLocaleData& locale_data, StringView language)
{
    CanonicalLanguageID language_id {};

    auto segments = language.split_view('-');
    VERIFY(!segments.is_empty());
    size_t index = 0;

    if (Unicode::is_unicode_language_subtag(segments[index])) {
        language_id.language = ensure_unique_string(locale_data, segments[index]);
        if (segments.size() == ++index)
            return language_id;
    } else {
        return {};
    }

    if (Unicode::is_unicode_script_subtag(segments[index])) {
        language_id.script = ensure_unique_string(locale_data, segments[index]);
        if (segments.size() == ++index)
            return language_id;
    }

    if (Unicode::is_unicode_region_subtag(segments[index])) {
        language_id.region = ensure_unique_string(locale_data, segments[index]);
        if (segments.size() == ++index)
            return language_id;
    }

    while (index < segments.size()) {
        if (!Unicode::is_unicode_variant_subtag(segments[index]))
            return {};
        language_id.variants.append(ensure_unique_string(locale_data, segments[index++]));
    }

    return language_id;
}

static Optional<LanguageMapping> parse_language_mapping(UnicodeLocaleData& locale_data, StringView key, StringView alias)
{
    auto parsed_key = parse_language(locale_data, key);
    if (!parsed_key.has_value())
        return {};

    auto parsed_alias = parse_language(locale_data, alias);
    if (!parsed_alias.has_value())
        return {};

    return LanguageMapping { parsed_key.release_value(), parsed_alias.release_value() };
}

static void parse_core_aliases(String core_supplemental_path, UnicodeLocaleData& locale_data)
{
    LexicalPath core_aliases_path(move(core_supplemental_path));
    core_aliases_path = core_aliases_path.append("aliases.json"sv);
    VERIFY(Core::File::exists(core_aliases_path.string()));

    auto core_aliases_file_or_error = Core::File::open(core_aliases_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!core_aliases_file_or_error.is_error());

    auto core_aliases = JsonParser(core_aliases_file_or_error.value()->read_all()).parse();
    VERIFY(core_aliases.has_value());

    auto const& supplemental_object = core_aliases->as_object().get("supplemental"sv);
    auto const& metadata_object = supplemental_object.as_object().get("metadata"sv);
    auto const& alias_object = metadata_object.as_object().get("alias"sv);

    auto append_aliases = [&](auto& alias_object, auto& alias_map) {
        alias_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
            auto alias = value.as_object().get("_replacement"sv).as_string();

            if (key.contains('-')) {
                auto mapping = parse_language_mapping(locale_data, key, alias);
                if (!mapping.has_value())
                    return;

                locale_data.max_variant_size = max(mapping->key.variants.size(), locale_data.max_variant_size);
                locale_data.max_variant_size = max(mapping->alias.variants.size(), locale_data.max_variant_size);
                locale_data.complex_mappings.append(mapping.release_value());
            } else {
                alias_map.set(key, ensure_unique_string(locale_data, alias));
            }
        });
    };

    append_aliases(alias_object.as_object().get("languageAlias"sv), locale_data.language_aliases);
    append_aliases(alias_object.as_object().get("territoryAlias"sv), locale_data.territory_aliases);
    append_aliases(alias_object.as_object().get("scriptAlias"sv), locale_data.script_aliases);
    append_aliases(alias_object.as_object().get("variantAlias"sv), locale_data.variant_aliases);
    append_aliases(alias_object.as_object().get("subdivisionAlias"sv), locale_data.subdivision_aliases);
}

static void parse_likely_subtags(String core_supplemental_path, UnicodeLocaleData& locale_data)
{
    LexicalPath likely_subtags_path(move(core_supplemental_path));
    likely_subtags_path = likely_subtags_path.append("likelySubtags.json"sv);
    VERIFY(Core::File::exists(likely_subtags_path.string()));

    auto likely_subtags_file_or_error = Core::File::open(likely_subtags_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!likely_subtags_file_or_error.is_error());

    auto likely_subtags = JsonParser(likely_subtags_file_or_error.value()->read_all()).parse();
    VERIFY(likely_subtags.has_value());

    auto const& supplemental_object = likely_subtags->as_object().get("supplemental"sv);
    auto const& likely_subtags_object = supplemental_object.as_object().get("likelySubtags"sv);

    likely_subtags_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto mapping = parse_language_mapping(locale_data, key, value.as_string());
        if (!mapping.has_value())
            return;

        locale_data.max_variant_size = max(mapping->key.variants.size(), locale_data.max_variant_size);
        locale_data.max_variant_size = max(mapping->alias.variants.size(), locale_data.max_variant_size);
        locale_data.likely_subtags.append(mapping.release_value());
    });
}

static void parse_identity(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath languages_path(move(locale_path)); // Note: Every JSON file defines identity data, so we can use any of them.
    languages_path = languages_path.append("languages.json"sv);
    VERIFY(Core::File::exists(languages_path.string()));

    auto languages_file_or_error = Core::File::open(languages_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!languages_file_or_error.is_error());

    auto languages = JsonParser(languages_file_or_error.value()->read_all()).parse();
    VERIFY(languages.has_value());

    auto const& main_object = languages->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& identity_object = locale_object.as_object().get("identity"sv);
    auto const& language_string = identity_object.as_object().get("language"sv);
    auto const& territory_string = identity_object.as_object().get("territory"sv);
    auto const& variant_string = identity_object.as_object().get("variant"sv);

    locale.language = language_string.as_string();
    if (!locale_data.languages.contains_slow(locale.language))
        locale_data.languages.append(locale.language);

    if (territory_string.is_string()) {
        locale.territory = territory_string.as_string();
        if (!locale_data.territories.contains_slow(*locale.territory))
            locale_data.territories.append(*locale.territory);
    }

    if (variant_string.is_string()) {
        locale.variant = variant_string.as_string();
        if (!locale_data.variants.contains_slow(*locale.variant))
            locale_data.variants.append(*locale.variant);
    }
}

static void parse_locale_languages(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath languages_path(move(locale_path));
    languages_path = languages_path.append("languages.json"sv);
    VERIFY(Core::File::exists(languages_path.string()));

    auto languages_file_or_error = Core::File::open(languages_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!languages_file_or_error.is_error());

    auto languages = JsonParser(languages_file_or_error.value()->read_all()).parse();
    VERIFY(languages.has_value());

    auto const& main_object = languages->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& languages_object = locale_display_names_object.as_object().get("languages"sv);

    languages_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!locale_data.languages.contains_slow(key))
            return;

        auto index = ensure_unique_string(locale_data, value.as_string());
        locale.languages.set(key, index);
    });
}

static void parse_locale_territories(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath territories_path(move(locale_path));
    territories_path = territories_path.append("territories.json"sv);
    VERIFY(Core::File::exists(territories_path.string()));

    auto territories_file_or_error = Core::File::open(territories_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!territories_file_or_error.is_error());

    auto territories = JsonParser(territories_file_or_error.value()->read_all()).parse();
    VERIFY(territories.has_value());

    auto const& main_object = territories->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(territories_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& territories_object = locale_display_names_object.as_object().get("territories"sv);

    territories_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!locale_data.territories.contains_slow(key))
            return;

        auto index = ensure_unique_string(locale_data, value.as_string());
        locale.territories.set(key, index);
    });
}

static void parse_locale_scripts(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath scripts_path(move(locale_path));
    scripts_path = scripts_path.append("scripts.json"sv);
    VERIFY(Core::File::exists(scripts_path.string()));

    auto scripts_file_or_error = Core::File::open(scripts_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!scripts_file_or_error.is_error());

    auto scripts = JsonParser(scripts_file_or_error.value()->read_all()).parse();
    VERIFY(scripts.has_value());

    auto const& main_object = scripts->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(scripts_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& scripts_object = locale_display_names_object.as_object().get("scripts"sv);

    scripts_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto index = ensure_unique_string(locale_data, value.as_string());
        locale.scripts.set(key, index);

        if (!locale_data.scripts.contains_slow(key))
            locale_data.scripts.append(key);
    });
}

static void parse_locale_list_patterns(String misc_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath list_patterns_path(move(misc_path));
    list_patterns_path = list_patterns_path.append("listPatterns.json"sv);
    VERIFY(Core::File::exists(list_patterns_path.string()));

    auto list_patterns_file_or_error = Core::File::open(list_patterns_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!list_patterns_file_or_error.is_error());

    auto list_patterns = JsonParser(list_patterns_file_or_error.value()->read_all()).parse();
    VERIFY(list_patterns.has_value());

    auto const& main_object = list_patterns->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(list_patterns_path.parent().basename());
    auto const& list_patterns_object = locale_object.as_object().get("listPatterns"sv);

    auto list_pattern_type = [](StringView key) {
        if (key.contains("type-standard"sv))
            return "conjunction"sv;
        if (key.contains("type-or"sv))
            return "disjunction"sv;
        if (key.contains("type-unit"sv))
            return "unit"sv;
        VERIFY_NOT_REACHED();
    };

    auto list_pattern_style = [](StringView key) {
        if (key.contains("short"sv))
            return "short"sv;
        if (key.contains("narrow"sv))
            return "narrow"sv;
        return "long"sv;
    };

    list_patterns_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto type = list_pattern_type(key);
        auto style = list_pattern_style(key);

        auto start = ensure_unique_string(locale_data, value.as_object().get("start"sv).as_string());
        auto middle = ensure_unique_string(locale_data, value.as_object().get("middle"sv).as_string());
        auto end = ensure_unique_string(locale_data, value.as_object().get("end"sv).as_string());
        auto pair = ensure_unique_string(locale_data, value.as_object().get("2"sv).as_string());

        if (!locale_data.list_pattern_types.contains_slow(type))
            locale_data.list_pattern_types.append(type);
        if (!locale_data.list_pattern_styles.contains_slow(style))
            locale_data.list_pattern_styles.append(style);

        locale.list_patterns.append({ move(type), move(style), move(start), move(middle), move(end), move(pair) });
    });
}

static void parse_locale_currencies(String numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath currencies_path(move(numbers_path));
    currencies_path = currencies_path.append("currencies.json"sv);
    VERIFY(Core::File::exists(currencies_path.string()));

    auto currencies_file_or_error = Core::File::open(currencies_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!currencies_file_or_error.is_error());

    auto currencies = JsonParser(currencies_file_or_error.value()->read_all()).parse();
    VERIFY(currencies.has_value());

    auto const& main_object = currencies->as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(currencies_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);
    auto const& currencies_object = locale_numbers_object.as_object().get("currencies"sv);

    currencies_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto const& display_name = value.as_object().get("displayName"sv);

        auto index = ensure_unique_string(locale_data, display_name.as_string());
        locale.currencies.set(key, index);

        if (!locale_data.currencies.contains_slow(key))
            locale_data.currencies.append(key);
    });
}

static void parse_numeric_keywords(String locale_numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    static constexpr StringView key = "nu"sv;

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
    auto const& default_numbering_system_object = locale_numbers_object.as_object().get("defaultNumberingSystem"sv);
    auto const& other_numbering_systems_object = locale_numbers_object.as_object().get("otherNumberingSystems"sv);

    Vector<String> keyword_values {};
    keyword_values.append(default_numbering_system_object.as_string());

    other_numbering_systems_object.as_object().for_each_member([&](auto const&, JsonValue const& value) {
        auto keyword_value = value.as_string();
        if (!keyword_values.contains_slow(keyword_value))
            keyword_values.append(move(keyword_value));
    });

    StringBuilder builder;
    builder.join(',', keyword_values);

    auto index = ensure_unique_string(locale_data, builder.build());
    locale.keywords.set(key, index);

    if (!locale_data.keywords.contains_slow(key))
        locale_data.keywords.append(key);
}

static void parse_default_content_locales(String core_path, UnicodeLocaleData& locale_data)
{
    LexicalPath default_content_path(move(core_path));
    default_content_path = default_content_path.append("defaultContent.json"sv);
    VERIFY(Core::File::exists(default_content_path.string()));

    auto default_content_file_or_error = Core::File::open(default_content_path.string(), Core::OpenMode::ReadOnly);
    VERIFY(!default_content_file_or_error.is_error());

    auto default_content = JsonParser(default_content_file_or_error.value()->read_all()).parse();
    VERIFY(default_content.has_value());

    auto const& default_content_array = default_content->as_object().get("defaultContent"sv);

    default_content_array.as_array().for_each([&](JsonValue const& value) {
        auto locale = value.as_string();
        StringView default_locale = locale;

        while (true) {
            if (locale_data.locales.contains(default_locale))
                break;

            auto pos = default_locale.find_last('-');
            if (!pos.has_value())
                return;

            default_locale = default_locale.substring_view(0, *pos);
        }

        locale_data.locales.set(locale, locale_data.locales.get(default_locale).value());
    });
}

static Core::DirIterator path_to_dir_iterator(String path)
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

static void parse_all_locales(String core_path, String locale_names_path, String misc_path, String numbers_path, UnicodeLocaleData& locale_data)
{
    auto identity_iterator = path_to_dir_iterator(locale_names_path);
    auto locale_names_iterator = path_to_dir_iterator(move(locale_names_path));
    auto misc_iterator = path_to_dir_iterator(move(misc_path));
    auto numbers_iterator = path_to_dir_iterator(move(numbers_path));

    LexicalPath core_supplemental_path(core_path);
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(Core::File::is_directory(core_supplemental_path.string()));

    parse_core_aliases(core_supplemental_path.string(), locale_data);
    parse_likely_subtags(core_supplemental_path.string(), locale_data);

    auto remove_variants_from_path = [&](String path) -> Optional<String> {
        auto parsed_locale = parse_language(locale_data, LexicalPath::basename(path));
        if (!parsed_locale.has_value())
            return {};

        StringBuilder builder;
        builder.append(get_unique_string(locale_data, parsed_locale->language));
        if (auto script = get_unique_string(locale_data, parsed_locale->script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = get_unique_string(locale_data, parsed_locale->region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (identity_iterator.has_next()) {
        auto locale_path = identity_iterator.next_full_path();
        VERIFY(Core::File::is_directory(locale_path));

        auto language = remove_variants_from_path(locale_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        parse_identity(locale_path, locale_data, locale);
    }

    while (locale_names_iterator.has_next()) {
        auto locale_path = locale_names_iterator.next_full_path();
        VERIFY(Core::File::is_directory(locale_path));

        auto language = remove_variants_from_path(locale_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        parse_locale_languages(locale_path, locale_data, locale);
        parse_locale_territories(locale_path, locale_data, locale);
        parse_locale_scripts(locale_path, locale_data, locale);
    }

    while (misc_iterator.has_next()) {
        auto misc_path = misc_iterator.next_full_path();
        VERIFY(Core::File::is_directory(misc_path));

        auto language = remove_variants_from_path(misc_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        parse_locale_list_patterns(misc_path, locale_data, locale);
    }

    while (numbers_iterator.has_next()) {
        auto numbers_path = numbers_iterator.next_full_path();
        VERIFY(Core::File::is_directory(numbers_path));

        auto language = remove_variants_from_path(numbers_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        parse_locale_currencies(numbers_path, locale_data, locale);
        parse_numeric_keywords(numbers_path, locale_data, locale);
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

    auto generate_enum = [&](StringView name, StringView default_, Vector<String>& values) {
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
    };

    generator.append(R"~~~(
#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#include <LibUnicode/Forward.h>

namespace Unicode {
)~~~");

    auto locales = locale_data.locales.keys();
    generate_enum("Locale"sv, "None"sv, locales);
    generate_enum("Language"sv, {}, locale_data.languages);
    generate_enum("Territory"sv, {}, locale_data.territories);
    generate_enum("ScriptTag"sv, {}, locale_data.scripts);
    generate_enum("Currency"sv, {}, locale_data.currencies);
    generate_enum("Key"sv, {}, locale_data.keywords);
    generate_enum("Variant"sv, {}, locale_data.variants);
    generate_enum("ListPatternType"sv, {}, locale_data.list_pattern_types);
    generate_enum("ListPatternStyle"sv, {}, locale_data.list_pattern_styles);

    generator.append(R"~~~(
namespace Detail {

Optional<Locale> locale_from_string(StringView locale);

Optional<StringView> get_locale_language_mapping(StringView locale, StringView language);
Optional<Language> language_from_string(StringView language);
Optional<StringView> resolve_language_alias(StringView language);

Optional<StringView> get_locale_territory_mapping(StringView locale, StringView territory);
Optional<Territory> territory_from_string(StringView territory);
Optional<StringView> resolve_territory_alias(StringView territory);

Optional<StringView> get_locale_script_tag_mapping(StringView locale, StringView script_tag);
Optional<ScriptTag> script_tag_from_string(StringView script_tag);
Optional<StringView> resolve_script_tag_alias(StringView script_tag);

Optional<StringView> get_locale_currency_mapping(StringView locale, StringView currency);
Optional<Currency> currency_from_string(StringView currency);

Optional<StringView> get_locale_key_mapping(StringView locale, StringView key);
Optional<Key> key_from_string(StringView key);

Optional<ListPatterns> get_locale_list_pattern_mapping(StringView locale, StringView list_pattern_type, StringView list_pattern_style);
Optional<ListPatternType> list_pattern_type_from_string(StringView list_pattern_type);
Optional<ListPatternStyle> list_pattern_style_from_string(StringView list_pattern_style);

Optional<StringView> resolve_variant_alias(StringView variant);
Optional<StringView> resolve_subdivision_alias(StringView subdivision);

void resolve_complex_language_aliases(Unicode::LanguageID& language_id);

Optional<Unicode::LanguageID> add_likely_subtags(Unicode::LanguageID const& language_id);
Optional<String> resolve_most_likely_territory(Unicode::LanguageID const& language_id);

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
    generator.set("strings_size"sv, String::number(locale_data.unique_strings.size()));
    generator.set("locales_size"sv, String::number(locale_data.locales.size()));
    generator.set("territories_size", String::number(locale_data.territories.size()));
    generator.set("variants_size", String::number(locale_data.max_variant_size));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Span.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/UnicodeLocale.h>

namespace Unicode {

struct Patterns {
    ListPatternType type;
    ListPatternStyle style;
    @string_index_type@ start { 0 };
    @string_index_type@ middle { 0 };
    @string_index_type@ end { 0 };
    @string_index_type@ pair { 0 };
};
)~~~");

    generator.append(R"~~~(
static constexpr Array<StringView, @strings_size@ + 1> s_string_list { {
    {})~~~");

    constexpr size_t max_strings_per_row = 30;
    size_t strings_in_current_row = 1;

    for (auto const& string : locale_data.unique_strings) {
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

    auto format_mapping_name = [](StringView format, StringView name) {
        auto mapping_name = name.to_lowercase_string().replace("-"sv, "_"sv, true);
        return String::formatted(format, mapping_name);
    };

    auto append_index = [&](auto index) {
        generator.append(String::formatted(", {}", index));
    };

    auto append_list_and_size = [&](auto const& list) {
        if (list.is_empty()) {
            generator.append(", {}, 0");
            return;
        }

        bool first = true;
        generator.append(", {");
        for (auto const& item : list) {
            generator.append(first ? " " : ", ");
            generator.append(String::number(item));
            first = false;
        }
        generator.append(String::formatted(" }}, {}", list.size()));
    };

    auto append_string_index_list = [&](String name, auto const& keys, auto const& mappings) {
        generator.set("name", name);
        generator.set("size", String::number(keys.size()));

        generator.append(R"~~~(
static constexpr Array<@string_index_type@, @size@> @name@ { {
    )~~~");

        constexpr size_t max_values_per_row = 30;
        size_t values_in_current_row = 0;

        for (auto const& key : keys) {
            if (values_in_current_row++ > 0)
                generator.append(" ");

            if (auto it = mappings.find(key); it != mappings.end())
                generator.set("mapping"sv, String::number(it->value));
            else
                generator.set("mapping"sv, "0"sv);
            generator.append("@mapping@,");

            if (values_in_current_row == max_values_per_row) {
                values_in_current_row = 0;
                generator.append("\n    ");
            }
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    auto append_list_patterns = [&](StringView name, Vector<ListPatterns> const& list_patterns) {
        generator.set("name", name);
        generator.set("size", String::number(list_patterns.size()));

        generator.append(R"~~~(
static constexpr Array<Patterns, @size@> @name@ { {)~~~");

        for (auto const& list_pattern : list_patterns) {
            generator.set("type"sv, String::formatted("ListPatternType::{}", format_identifier({}, list_pattern.type)));
            generator.set("style"sv, String::formatted("ListPatternStyle::{}", format_identifier({}, list_pattern.style)));
            generator.set("start"sv, String::number(list_pattern.start));
            generator.set("middle"sv, String::number(list_pattern.middle));
            generator.set("end"sv, String::number(list_pattern.end));
            generator.set("pair"sv, String::number(list_pattern.pair));

            generator.append(R"~~~(
    { @type@, @style@, @start@, @middle@, @end@, @pair@ },)~~~");
        }

        generator.append(R"~~~(
} };
)~~~");
    };

    auto append_mapping = [&](StringView type, StringView name, StringView format, auto format_list_callback) {
        Vector<String> mapping_names;

        for (auto const& locale : locale_data.locales) {
            auto mapping_name = format_mapping_name(format, locale.key);
            format_list_callback(mapping_name, locale.value);
            mapping_names.append(move(mapping_name));
        }

        quick_sort(mapping_names);

        generator.set("type", type);
        generator.set("name", name);
        generator.set("size", String::number(locale_data.locales.size()));
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
    };

    append_mapping(s_string_index_type, "s_languages"sv, "s_languages_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.languages, value.languages); });
    append_mapping(s_string_index_type, "s_territories"sv, "s_territories_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.territories, value.territories); });
    append_mapping(s_string_index_type, "s_scripts"sv, "s_scripts_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.scripts, value.scripts); });
    append_mapping(s_string_index_type, "s_currencies"sv, "s_currencies_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.currencies, value.currencies); });
    append_mapping(s_string_index_type, "s_keywords"sv, "s_keywords_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.keywords, value.keywords); });
    append_mapping("Patterns"sv, "s_list_patterns"sv, "s_list_patterns_{}", [&](auto const& name, auto const& value) { append_list_patterns(name, value.list_patterns); });

    generator.append(R"~~~(
struct CanonicalLanguageID {
    Unicode::LanguageID to_unicode_language_id() const
    {
        Unicode::LanguageID language_id {};
        language_id.variants.ensure_capacity(variants_size);

        language_id.language = s_string_list[language];
        if (script != 0)
            language_id.script = s_string_list[script];
        if (region != 0)
            language_id.region = s_string_list[region];
        for (size_t i = 0; i < variants_size; ++i)
            language_id.variants.append(s_string_list[variants[i]]);

        return language_id;
    }

    bool matches_variants(Vector<String> const& other_variants) const {
        if (variants_size == 0)
            return true;
        if (other_variants.size() != variants_size)
            return false;

        for (size_t i = 0; i < variants_size; ++i) {
            if (s_string_list[variants[i]] != other_variants[i])
                return false;
        }

        return true;
    };

    @string_index_type@ language { 0 };
    @string_index_type@ script { 0 };
    @string_index_type@ region { 0 };
    Array<@string_index_type@, @variants_size@> variants {};
    size_t variants_size { 0 };

};

struct LanguageMapping {
    CanonicalLanguageID key;
    CanonicalLanguageID alias;
};
)~~~");

    auto append_complex_mapping = [&](StringView name, auto& mappings) {
        generator.set("size", String::number(mappings.size()));
        generator.set("name"sv, name);

        generator.append(R"~~~(
static constexpr Array<LanguageMapping, @size@> s_@name@ { {
)~~~");

        quick_sort(mappings, [&](auto const& lhs, auto const& rhs) {
            auto const& lhs_language = get_unique_string(locale_data, lhs.key.language);
            auto const& rhs_language = get_unique_string(locale_data, rhs.key.language);

            // Sort the keys such that "und" language tags are at the end, as those are less specific.
            if (lhs_language.starts_with("und"sv) && !rhs_language.starts_with("und"sv))
                return false;
            if (!lhs_language.starts_with("und"sv) && rhs_language.starts_with("und"sv))
                return true;
            return lhs_language < rhs_language;
        });

        for (auto const& mapping : mappings) {
            generator.set("language"sv, String::number(mapping.key.language));
            generator.append("    { { @language@");

            append_index(mapping.key.script);
            append_index(mapping.key.region);
            append_list_and_size(mapping.key.variants);

            generator.set("language"sv, String::number(mapping.alias.language));
            generator.append(" }, { @language@");

            append_index(mapping.alias.script);
            append_index(mapping.alias.region);
            append_list_and_size(mapping.alias.variants);

            generator.append(" } },\n");
        }

        generator.append("} };\n");
    };

    append_complex_mapping("complex_alias"sv, locale_data.complex_mappings);
    append_complex_mapping("likely_subtags"sv, locale_data.likely_subtags);

    generator.append(R"~~~(
static LanguageMapping const* resolve_likely_subtag(Unicode::LanguageID const& language_id)
{
    // https://unicode.org/reports/tr35/#Likely_Subtags
    enum class State {
        LanguageScriptRegion,
        LanguageRegion,
        LanguageScript,
        Language,
        UndScript,
        Done,
    };

    auto state = State::LanguageScriptRegion;

    while (state != State::Done) {
        Unicode::LanguageID search_key;

        switch (state) {
        case State::LanguageScriptRegion:
            state = State::LanguageRegion;
            if (!language_id.script.has_value() || !language_id.region.has_value())
                continue;

            search_key.language = *language_id.language;
            search_key.script = *language_id.script;
            search_key.region = *language_id.region;
            break;

        case State::LanguageRegion:
            state = State::LanguageScript;
            if (!language_id.region.has_value())
                continue;

            search_key.language = *language_id.language;
            search_key.region = *language_id.region;
            break;

        case State::LanguageScript:
            state = State::Language;
            if (!language_id.script.has_value())
                continue;

            search_key.language = *language_id.language;
            search_key.script = *language_id.script;
            break;

        case State::Language:
            state = State::UndScript;
            search_key.language = *language_id.language;
            break;

        case State::UndScript:
            state = State::Done;
            if (!language_id.script.has_value())
                continue;

            search_key.language = "und"sv;
            search_key.script = *language_id.script;
            break;

        default:
            VERIFY_NOT_REACHED();
        }

        for (auto const& map : s_likely_subtags) {
            auto const& key_language = s_string_list[map.key.language];
            auto const& key_script = s_string_list[map.key.script];
            auto const& key_region  = s_string_list[map.key.region];

            if (key_language != search_key.language)
                continue;
            if (!key_script.is_empty() || search_key.script.has_value()) {
                if (key_script != search_key.script)
                    continue;
            }
            if (!key_region.is_empty() || search_key.region.has_value()) {
                if (key_region != search_key.region)
                    continue;
            }

            return &map;
        }
    }

    return nullptr;
}

namespace Detail {
)~~~");

    auto append_mapping_search = [&](StringView enum_title, StringView enum_snake, StringView collection_name) {
        generator.set("enum_title", enum_title);
        generator.set("enum_snake", enum_snake);
        generator.set("collection_name", collection_name);
        generator.append(R"~~~(
Optional<StringView> get_locale_@enum_snake@_mapping(StringView locale, StringView @enum_snake@)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto @enum_snake@_value = @enum_snake@_from_string(@enum_snake@);
    if (!@enum_snake@_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto @enum_snake@_index = to_underlying(*@enum_snake@_value);

    auto const& mappings = @collection_name@.at(locale_index);
    auto @enum_snake@_string_index = mappings.at(@enum_snake@_index);
    auto @enum_snake@_mapping = s_string_list.at(@enum_snake@_string_index);

    if (@enum_snake@_mapping.is_empty())
        return {};
    return @enum_snake@_mapping;
}
)~~~");
    };

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, auto const& values) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        for (auto const& value : values)
            hashes.set(value.hash(), format_identifier(enum_title, value));

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes));
    };

    auto append_alias_search = [&](StringView enum_snake, auto const& aliases) {
        HashValueMap<StringIndexType> hashes;
        hashes.ensure_capacity(aliases.size());

        for (auto const& alias : aliases)
            hashes.set(alias.key.hash(), alias.value);

        generate_value_from_string(generator, "resolve_{}_alias"sv, s_string_index_type, enum_snake, move(hashes), "StringView"sv, "s_string_list[{}]"sv);
    };

    append_from_string("Locale"sv, "locale"sv, locale_data.locales.keys());

    append_mapping_search("Language"sv, "language"sv, "s_languages"sv);
    append_from_string("Language"sv, "language"sv, locale_data.languages);
    append_alias_search("language"sv, locale_data.language_aliases);

    append_mapping_search("Territory"sv, "territory"sv, "s_territories"sv);
    append_from_string("Territory"sv, "territory"sv, locale_data.territories);
    append_alias_search("territory"sv, locale_data.territory_aliases);

    append_mapping_search("ScriptTag"sv, "script_tag"sv, "s_scripts"sv);
    append_from_string("ScriptTag"sv, "script_tag"sv, locale_data.scripts);
    append_alias_search("script_tag"sv, locale_data.script_aliases);

    append_mapping_search("Currency"sv, "currency"sv, "s_currencies"sv);
    append_from_string("Currency"sv, "currency"sv, locale_data.currencies);

    append_mapping_search("Key"sv, "key"sv, "s_keywords"sv);
    append_from_string("Key"sv, "key"sv, locale_data.keywords);

    append_alias_search("variant"sv, locale_data.variant_aliases);
    append_alias_search("subdivision"sv, locale_data.subdivision_aliases);

    append_from_string("ListPatternType"sv, "list_pattern_type"sv, locale_data.list_pattern_types);
    append_from_string("ListPatternStyle"sv, "list_pattern_style"sv, locale_data.list_pattern_styles);

    generator.append(R"~~~(
Optional<ListPatterns> get_locale_list_pattern_mapping(StringView locale, StringView list_pattern_type, StringView list_pattern_style)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto type_value = list_pattern_type_from_string(list_pattern_type);
    if (!type_value.has_value())
        return {};

    auto style_value = list_pattern_style_from_string(list_pattern_style);
    if (!style_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto const& locale_list_patterns = s_list_patterns.at(locale_index);

    for (auto const& list_patterns : locale_list_patterns) {
        if ((list_patterns.type == type_value) && (list_patterns.style == style_value)) {
            auto const& start = s_string_list[list_patterns.start];
            auto const& middle = s_string_list[list_patterns.middle];
            auto const& end = s_string_list[list_patterns.end];
            auto const& pair = s_string_list[list_patterns.pair];

            return ListPatterns { start, middle, end, pair };
        }
    }

    return {};
}

void resolve_complex_language_aliases(Unicode::LanguageID& language_id)
{
    for (auto const& map : s_complex_alias) {
        auto const& key_language = s_string_list[map.key.language];
        auto const& key_script = s_string_list[map.key.script];
        auto const& key_region  = s_string_list[map.key.region];

        if ((key_language != language_id.language) && (key_language != "und"sv))
            continue;
        if (!key_script.is_empty() && (key_script != language_id.script))
            continue;
        if (!key_region.is_empty() && (key_region != language_id.region))
            continue;
        if (!map.key.matches_variants(language_id.variants))
            continue;

        auto alias = map.alias.to_unicode_language_id();

        if (alias.language == "und"sv)
            alias.language = move(language_id.language);
        if (key_script.is_empty() && !alias.script.has_value())
            alias.script = move(language_id.script);
        if (key_region.is_empty() && !alias.region.has_value())
            alias.region = move(language_id.region);
        if (map.key.variants_size == 0 && alias.variants.is_empty())
            alias.variants = move(language_id.variants);

        language_id = move(alias);
        break;
    }
}

Optional<Unicode::LanguageID> add_likely_subtags(Unicode::LanguageID const& language_id)
{
    // https://www.unicode.org/reports/tr35/#Likely_Subtags
    auto const* likely_subtag = resolve_likely_subtag(language_id);
    if (likely_subtag == nullptr)
        return {};

    auto maximized = language_id;

    auto const& key_script = s_string_list[likely_subtag->key.script];
    auto const& key_region = s_string_list[likely_subtag->key.region];

    auto const& alias_language = s_string_list[likely_subtag->alias.language];
    auto const& alias_script = s_string_list[likely_subtag->alias.script];
    auto const& alias_region = s_string_list[likely_subtag->alias.region];

    if (maximized.language == "und"sv)
        maximized.language = alias_language;
    if (!maximized.script.has_value() || (!key_script.is_empty() && !alias_script.is_empty()))
        maximized.script = alias_script;
    if (!maximized.region.has_value() || (!key_region.is_empty() && !alias_region.is_empty()))
        maximized.region = alias_region;

    return maximized;
}

Optional<String> resolve_most_likely_territory(Unicode::LanguageID const& language_id)
{
    if (auto const* likely_subtag = resolve_likely_subtag(language_id); likely_subtag != nullptr)
        return s_string_list[likely_subtag->alias.region];
    return {};
}

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
    char const* locale_names_path = nullptr;
    char const* misc_path = nullptr;
    char const* numbers_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(locale_names_path, "Path to cldr-localenames directory", "locale-names-path", 'l', "locale-names-path");
    args_parser.add_option(misc_path, "Path to cldr-misc directory", "misc-path", 'm', "misc-path");
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
    parse_all_locales(core_path, locale_names_path, misc_path, numbers_path, locale_data);

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
