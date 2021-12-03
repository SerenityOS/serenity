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
    HashMap<String, StringIndexType> long_currencies;
    HashMap<String, StringIndexType> short_currencies;
    HashMap<String, StringIndexType> narrow_currencies;
    HashMap<String, StringIndexType> numeric_currencies;
    HashMap<String, StringIndexType> keywords;
    Vector<ListPatterns> list_patterns;
};

struct LanguageMapping {
    CanonicalLanguageID<StringIndexType> key {};
    CanonicalLanguageID<StringIndexType> alias {};
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;

    HashMap<String, Locale> locales;
    Vector<Alias> locale_aliases;

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

static Optional<LanguageMapping> parse_language_mapping(UnicodeLocaleData& locale_data, StringView key, StringView alias)
{
    auto parsed_key = CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, key);
    if (!parsed_key.has_value())
        return {};

    auto parsed_alias = CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, alias);
    if (!parsed_alias.has_value())
        return {};

    return LanguageMapping { parsed_key.release_value(), parsed_alias.release_value() };
}

static ErrorOr<void> parse_core_aliases(String core_supplemental_path, UnicodeLocaleData& locale_data)
{
    LexicalPath core_aliases_path(move(core_supplemental_path));
    core_aliases_path = core_aliases_path.append("aliases.json"sv);

    auto core_aliases_file = TRY(Core::File::open(core_aliases_path.string(), Core::OpenMode::ReadOnly));
    auto core_aliases = TRY(JsonValue::from_string(core_aliases_file->read_all()));

    auto const& supplemental_object = core_aliases.as_object().get("supplemental"sv);
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
                alias_map.set(key, locale_data.unique_strings.ensure(alias));
            }
        });
    };

    append_aliases(alias_object.as_object().get("languageAlias"sv), locale_data.language_aliases);
    append_aliases(alias_object.as_object().get("territoryAlias"sv), locale_data.territory_aliases);
    append_aliases(alias_object.as_object().get("scriptAlias"sv), locale_data.script_aliases);
    append_aliases(alias_object.as_object().get("variantAlias"sv), locale_data.variant_aliases);
    append_aliases(alias_object.as_object().get("subdivisionAlias"sv), locale_data.subdivision_aliases);

    return {};
}

static ErrorOr<void> parse_likely_subtags(String core_supplemental_path, UnicodeLocaleData& locale_data)
{
    LexicalPath likely_subtags_path(move(core_supplemental_path));
    likely_subtags_path = likely_subtags_path.append("likelySubtags.json"sv);

    auto likely_subtags_file = TRY(Core::File::open(likely_subtags_path.string(), Core::OpenMode::ReadOnly));
    auto likely_subtags = TRY(JsonValue::from_string(likely_subtags_file->read_all()));

    auto const& supplemental_object = likely_subtags.as_object().get("supplemental"sv);
    auto const& likely_subtags_object = supplemental_object.as_object().get("likelySubtags"sv);

    likely_subtags_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto mapping = parse_language_mapping(locale_data, key, value.as_string());
        if (!mapping.has_value())
            return;

        locale_data.max_variant_size = max(mapping->key.variants.size(), locale_data.max_variant_size);
        locale_data.max_variant_size = max(mapping->alias.variants.size(), locale_data.max_variant_size);
        locale_data.likely_subtags.append(mapping.release_value());
    });

    return {};
}

static ErrorOr<void> parse_identity(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath languages_path(move(locale_path)); // Note: Every JSON file defines identity data, so we can use any of them.
    languages_path = languages_path.append("languages.json"sv);

    auto languages_file = TRY(Core::File::open(languages_path.string(), Core::OpenMode::ReadOnly));
    auto languages = TRY(JsonValue::from_string(languages_file->read_all()));

    auto const& main_object = languages.as_object().get("main"sv);
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

    return {};
}

static ErrorOr<void> parse_locale_languages(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath languages_path(move(locale_path));
    languages_path = languages_path.append("languages.json"sv);

    auto languages_file = TRY(Core::File::open(languages_path.string(), Core::OpenMode::ReadOnly));
    auto languages = TRY(JsonValue::from_string(languages_file->read_all()));

    auto const& main_object = languages.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& languages_object = locale_display_names_object.as_object().get("languages"sv);

    languages_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!locale_data.languages.contains_slow(key))
            return;

        auto index = locale_data.unique_strings.ensure(value.as_string());
        locale.languages.set(key, index);
    });

    return {};
}

static ErrorOr<void> parse_locale_territories(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath territories_path(move(locale_path));
    territories_path = territories_path.append("territories.json"sv);

    auto territories_file = TRY(Core::File::open(territories_path.string(), Core::OpenMode::ReadOnly));
    auto territories = TRY(JsonValue::from_string(territories_file->read_all()));

    auto const& main_object = territories.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(territories_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& territories_object = locale_display_names_object.as_object().get("territories"sv);

    territories_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!locale_data.territories.contains_slow(key))
            return;

        auto index = locale_data.unique_strings.ensure(value.as_string());
        locale.territories.set(key, index);
    });

    return {};
}

static ErrorOr<void> parse_locale_scripts(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath scripts_path(move(locale_path));
    scripts_path = scripts_path.append("scripts.json"sv);

    auto scripts_file = TRY(Core::File::open(scripts_path.string(), Core::OpenMode::ReadOnly));
    auto scripts = TRY(JsonValue::from_string(scripts_file->read_all()));

    auto const& main_object = scripts.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(scripts_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& scripts_object = locale_display_names_object.as_object().get("scripts"sv);

    scripts_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto index = locale_data.unique_strings.ensure(value.as_string());
        locale.scripts.set(key, index);

        if (!locale_data.scripts.contains_slow(key))
            locale_data.scripts.append(key);
    });

    return {};
}

static ErrorOr<void> parse_locale_list_patterns(String misc_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath list_patterns_path(move(misc_path));
    list_patterns_path = list_patterns_path.append("listPatterns.json"sv);

    auto list_patterns_file = TRY(Core::File::open(list_patterns_path.string(), Core::OpenMode::ReadOnly));
    auto list_patterns = TRY(JsonValue::from_string(list_patterns_file->read_all()));

    auto const& main_object = list_patterns.as_object().get("main"sv);
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

        auto start = locale_data.unique_strings.ensure(value.as_object().get("start"sv).as_string());
        auto middle = locale_data.unique_strings.ensure(value.as_object().get("middle"sv).as_string());
        auto end = locale_data.unique_strings.ensure(value.as_object().get("end"sv).as_string());
        auto pair = locale_data.unique_strings.ensure(value.as_object().get("2"sv).as_string());

        if (!locale_data.list_pattern_types.contains_slow(type))
            locale_data.list_pattern_types.append(type);
        if (!locale_data.list_pattern_styles.contains_slow(style))
            locale_data.list_pattern_styles.append(style);

        locale.list_patterns.append({ move(type), move(style), move(start), move(middle), move(end), move(pair) });
    });

    return {};
}

static ErrorOr<void> parse_locale_currencies(String numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath currencies_path(move(numbers_path));
    currencies_path = currencies_path.append("currencies.json"sv);

    auto currencies_file = TRY(Core::File::open(currencies_path.string(), Core::OpenMode::ReadOnly));
    auto currencies = TRY(JsonValue::from_string(currencies_file->read_all()));

    auto const& main_object = currencies.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(currencies_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);
    auto const& currencies_object = locale_numbers_object.as_object().get("currencies"sv);

    currencies_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto const& long_name = value.as_object().get("displayName"sv);
        auto const& short_name = value.as_object().get("symbol"sv);
        auto const& narrow_name = value.as_object().get("symbol-alt-narrow"sv);
        auto const& numeric_name = value.as_object().get("displayName-count-other"sv);

        locale.long_currencies.set(key, locale_data.unique_strings.ensure(long_name.as_string()));
        locale.short_currencies.set(key, locale_data.unique_strings.ensure(short_name.as_string()));
        locale.narrow_currencies.set(key, narrow_name.is_null() ? 0 : locale_data.unique_strings.ensure(narrow_name.as_string()));
        locale.numeric_currencies.set(key, locale_data.unique_strings.ensure(numeric_name.is_null() ? long_name.as_string() : numeric_name.as_string()));

        if (!locale_data.currencies.contains_slow(key))
            locale_data.currencies.append(key);
    });

    return {};
}

static ErrorOr<void> parse_numeric_keywords(String locale_numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    static constexpr StringView key = "nu"sv;

    LexicalPath numbers_path(move(locale_numbers_path));
    numbers_path = numbers_path.append("numbers.json"sv);

    auto numbers_file = TRY(Core::File::open(numbers_path.string(), Core::OpenMode::ReadOnly));
    auto numbers = TRY(JsonValue::from_string(numbers_file->read_all()));

    auto const& main_object = numbers.as_object().get("main"sv);
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

    locale_numbers_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!key.starts_with("defaultNumberingSystem-alt-"sv))
            return;

        auto keyword_value = value.as_string();
        if (!keyword_values.contains_slow(keyword_value))
            keyword_values.append(move(keyword_value));
    });

    StringBuilder builder;
    builder.join(',', keyword_values);

    auto index = locale_data.unique_strings.ensure(builder.build());
    locale.keywords.set(key, index);

    if (!locale_data.keywords.contains_slow(key))
        locale_data.keywords.append(key);

    return {};
}

static ErrorOr<void> parse_default_content_locales(String core_path, UnicodeLocaleData& locale_data)
{
    LexicalPath default_content_path(move(core_path));
    default_content_path = default_content_path.append("defaultContent.json"sv);

    auto default_content_file = TRY(Core::File::open(default_content_path.string(), Core::OpenMode::ReadOnly));
    auto default_content = TRY(JsonValue::from_string(default_content_file->read_all()));
    auto const& default_content_array = default_content.as_object().get("defaultContent"sv);

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

        if (default_locale != locale)
            locale_data.locale_aliases.append({ default_locale, move(locale) });
    });

    return {};
}

static void define_aliases_without_scripts(UnicodeLocaleData& locale_data)
{
    // From ECMA-402: https://tc39.es/ecma402/#sec-internal-slots
    //
    //     For locales that include a script subtag in addition to language and region, the
    //     corresponding locale without a script subtag must also be supported.
    //
    // So we define aliases for locales that contain all three subtags, but we must also take
    // care to handle when the locale itself or the locale without a script subtag are an alias
    // by way of default-content locales.
    auto find_alias = [&](auto const& locale) {
        return locale_data.locale_aliases.find_if([&](auto const& alias) { return locale == alias.alias; });
    };

    auto append_alias_without_script = [&](auto const& locale) {
        auto parsed_locale = CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, locale);
        VERIFY(parsed_locale.has_value());

        if ((parsed_locale->language == 0) || (parsed_locale->script == 0) || (parsed_locale->region == 0))
            return;

        auto locale_without_script = String::formatted("{}-{}",
            locale_data.unique_strings.get(parsed_locale->language),
            locale_data.unique_strings.get(parsed_locale->region));

        if (locale_data.locales.contains(locale_without_script))
            return;
        if (find_alias(locale_without_script) != locale_data.locale_aliases.end())
            return;

        if (auto it = find_alias(locale); it != locale_data.locale_aliases.end())
            locale_data.locale_aliases.append({ it->name, locale_without_script });
        else
            locale_data.locale_aliases.append({ locale, locale_without_script });
    };

    for (auto const& locale : locale_data.locales)
        append_alias_without_script(locale.key);
    for (auto const& locale : locale_data.locale_aliases)
        append_alias_without_script(locale.alias);
}

static ErrorOr<void> parse_all_locales(String core_path, String locale_names_path, String misc_path, String numbers_path, UnicodeLocaleData& locale_data)
{
    auto identity_iterator = path_to_dir_iterator(locale_names_path);
    auto locale_names_iterator = path_to_dir_iterator(move(locale_names_path));
    auto misc_iterator = path_to_dir_iterator(move(misc_path));
    auto numbers_iterator = path_to_dir_iterator(move(numbers_path));

    LexicalPath core_supplemental_path(core_path);
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(Core::File::is_directory(core_supplemental_path.string()));

    TRY(parse_core_aliases(core_supplemental_path.string(), locale_data));
    TRY(parse_likely_subtags(core_supplemental_path.string(), locale_data));

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

    while (identity_iterator.has_next()) {
        auto locale_path = identity_iterator.next_full_path();
        VERIFY(Core::File::is_directory(locale_path));

        auto language = remove_variants_from_path(locale_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        TRY(parse_identity(locale_path, locale_data, locale));
    }

    while (locale_names_iterator.has_next()) {
        auto locale_path = locale_names_iterator.next_full_path();
        VERIFY(Core::File::is_directory(locale_path));

        auto language = remove_variants_from_path(locale_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        TRY(parse_locale_languages(locale_path, locale_data, locale));
        TRY(parse_locale_territories(locale_path, locale_data, locale));
        TRY(parse_locale_scripts(locale_path, locale_data, locale));
    }

    while (misc_iterator.has_next()) {
        auto misc_path = misc_iterator.next_full_path();
        VERIFY(Core::File::is_directory(misc_path));

        auto language = remove_variants_from_path(misc_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        TRY(parse_locale_list_patterns(misc_path, locale_data, locale));
    }

    while (numbers_iterator.has_next()) {
        auto numbers_path = numbers_iterator.next_full_path();
        VERIFY(Core::File::is_directory(numbers_path));

        auto language = remove_variants_from_path(numbers_path);
        if (!language.has_value())
            continue;

        auto& locale = locale_data.locales.ensure(*language);
        TRY(parse_locale_currencies(numbers_path, locale_data, locale));
        TRY(parse_numeric_keywords(numbers_path, locale_data, locale));
    }

    TRY(parse_default_content_locales(move(core_path), locale_data));
    define_aliases_without_scripts(locale_data);

    return {};
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

    auto locales = locale_data.locales.keys();
    generate_enum(generator, format_identifier, "Locale"sv, "None"sv, locales, locale_data.locale_aliases);
    generate_enum(generator, format_identifier, "Language"sv, {}, locale_data.languages);
    generate_enum(generator, format_identifier, "Territory"sv, {}, locale_data.territories);
    generate_enum(generator, format_identifier, "ScriptTag"sv, {}, locale_data.scripts);
    generate_enum(generator, format_identifier, "Currency"sv, {}, locale_data.currencies);
    generate_enum(generator, format_identifier, "Key"sv, {}, locale_data.keywords);
    generate_enum(generator, format_identifier, "Variant"sv, {}, locale_data.variants);
    generate_enum(generator, format_identifier, "ListPatternType"sv, {}, locale_data.list_pattern_types);
    generate_enum(generator, format_identifier, "ListPatternStyle"sv, {}, locale_data.list_pattern_styles);

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

Optional<StringView> get_locale_long_currency_mapping(StringView locale, StringView currency);
Optional<StringView> get_locale_short_currency_mapping(StringView locale, StringView currency);
Optional<StringView> get_locale_narrow_currency_mapping(StringView locale, StringView currency);
Optional<StringView> get_locale_numeric_currency_mapping(StringView locale, StringView currency);
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

    VERIFY(file.write(generator.as_string_view()));
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("locales_size"sv, String::number(locale_data.locales.size()));
    generator.set("territories_size", String::number(locale_data.territories.size()));
    generator.set("variants_size", String::number(locale_data.max_variant_size));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Span.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/UnicodeLocale.h>

namespace Unicode::Detail {

struct Patterns {
    ListPatternType type;
    ListPatternStyle style;
    @string_index_type@ start { 0 };
    @string_index_type@ middle { 0 };
    @string_index_type@ end { 0 };
    @string_index_type@ pair { 0 };
};
)~~~");

    locale_data.unique_strings.generate(generator);

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

    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_languages"sv, "s_languages_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.languages, value.languages); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_territories"sv, "s_territories_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.territories, value.territories); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_scripts"sv, "s_scripts_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.scripts, value.scripts); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_long_currencies"sv, "s_long_currencies_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.currencies, value.long_currencies); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_short_currencies"sv, "s_short_currencies_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.currencies, value.short_currencies); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_narrow_currencies"sv, "s_narrow_currencies_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.currencies, value.narrow_currencies); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_numeric_currencies"sv, "s_numeric_currencies_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.currencies, value.numeric_currencies); });
    generate_mapping(generator, locale_data.locales, s_string_index_type, "s_keywords"sv, "s_keywords_{}", [&](auto const& name, auto const& value) { append_string_index_list(name, locale_data.keywords, value.keywords); });
    generate_mapping(generator, locale_data.locales, "Patterns"sv, "s_list_patterns"sv, "s_list_patterns_{}", [&](auto const& name, auto const& value) { append_list_patterns(name, value.list_patterns); });

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
            auto const& lhs_language = locale_data.unique_strings.get(lhs.key.language);
            auto const& rhs_language = locale_data.unique_strings.get(rhs.key.language);

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

)~~~");

    auto append_mapping_search = [&](StringView enum_snake, StringView from_string_name, StringView collection_name) {
        generator.set("enum_snake", enum_snake);
        generator.set("from_string_name", from_string_name);
        generator.set("collection_name", collection_name);
        generator.append(R"~~~(
Optional<StringView> get_locale_@enum_snake@_mapping(StringView locale, StringView @enum_snake@)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto @enum_snake@_value = @from_string_name@_from_string(@enum_snake@);
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

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, auto const& values, Vector<Alias> const& aliases = {}) {
        HashValueMap<String> hashes;
        hashes.ensure_capacity(values.size());

        for (auto const& value : values)
            hashes.set(value.hash(), format_identifier(enum_title, value));
        for (auto const& alias : aliases)
            hashes.set(alias.alias.hash(), format_identifier(enum_title, alias.alias));

        generate_value_from_string(generator, "{}_from_string"sv, enum_title, enum_snake, move(hashes));
    };

    auto append_alias_search = [&](StringView enum_snake, auto const& aliases) {
        HashValueMap<StringIndexType> hashes;
        hashes.ensure_capacity(aliases.size());

        for (auto const& alias : aliases)
            hashes.set(alias.key.hash(), alias.value);

        generate_value_from_string(generator, "resolve_{}_alias"sv, s_string_index_type, enum_snake, move(hashes), "StringView"sv, "s_string_list[{}]"sv);
    };

    append_from_string("Locale"sv, "locale"sv, locale_data.locales.keys(), locale_data.locale_aliases);

    append_mapping_search("language"sv, "language"sv, "s_languages"sv);
    append_from_string("Language"sv, "language"sv, locale_data.languages);
    append_alias_search("language"sv, locale_data.language_aliases);

    append_mapping_search("territory"sv, "territory"sv, "s_territories"sv);
    append_from_string("Territory"sv, "territory"sv, locale_data.territories);
    append_alias_search("territory"sv, locale_data.territory_aliases);

    append_mapping_search("script_tag"sv, "script_tag"sv, "s_scripts"sv);
    append_from_string("ScriptTag"sv, "script_tag"sv, locale_data.scripts);
    append_alias_search("script_tag"sv, locale_data.script_aliases);

    append_mapping_search("long_currency"sv, "currency"sv, "s_long_currencies"sv);
    append_mapping_search("short_currency"sv, "currency"sv, "s_short_currencies"sv);
    append_mapping_search("narrow_currency"sv, "currency"sv, "s_narrow_currencies"sv);
    append_mapping_search("numeric_currency"sv, "currency"sv, "s_numeric_currencies"sv);
    append_from_string("Currency"sv, "currency"sv, locale_data.currencies);

    append_mapping_search("key"sv, "key"sv, "s_keywords"sv);
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
)~~~");

    VERIFY(file.write(generator.as_string_view()));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path = nullptr;
    StringView generated_implementation_path = nullptr;
    StringView core_path = nullptr;
    StringView locale_names_path = nullptr;
    StringView misc_path = nullptr;
    StringView numbers_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(locale_names_path, "Path to cldr-localenames directory", "locale-names-path", 'l', "locale-names-path");
    args_parser.add_option(misc_path, "Path to cldr-misc directory", "misc-path", 'm', "misc-path");
    args_parser.add_option(numbers_path, "Path to cldr-numbers directory", "numbers-path", 'n', "numbers-path");
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
    TRY(parse_all_locales(core_path, locale_names_path, misc_path, numbers_path, locale_data));

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
