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

using LanguageListIndexType = u8;
constexpr auto s_language_list_index_type = "u8"sv;

using TerritoryListIndexType = u8;
constexpr auto s_territory_list_index_type = "u8"sv;

using ScriptListIndexType = u8;
constexpr auto s_script_list_index_type = "u8"sv;

using CurrencyListIndexType = u16;
constexpr auto s_currency_list_index_type = "u16"sv;

using KeywordListIndexType = u8;
constexpr auto s_keyword_list_index_type = "u8"sv;

using ListPatternIndexType = u16;
constexpr auto s_list_pattern_index_type = "u16"sv;

using ListPatternListIndexType = u8;
constexpr auto s_list_pattern_list_index_type = "u8"sv;

static String format_identifier(StringView owner, String identifier)
{
    identifier = identifier.replace("-"sv, "_"sv, true);

    if (all_of(identifier, is_ascii_digit))
        return String::formatted("{}_{}", owner[0], identifier);
    if (is_ascii_lower_alpha(identifier[0]))
        return String::formatted("{:c}{}", to_ascii_uppercase(identifier[0]), identifier.substring_view(1));
    return identifier;
}

struct ListPatterns {
    unsigned hash() const
    {
        auto hash = pair_int_hash(type.hash(), style.hash());
        hash = pair_int_hash(hash, start);
        hash = pair_int_hash(hash, middle);
        hash = pair_int_hash(hash, end);
        hash = pair_int_hash(hash, pair);
        return hash;
    }

    bool operator==(ListPatterns const& other) const
    {
        return (type == other.type)
            && (style == other.style)
            && (start == other.start)
            && (middle == other.middle)
            && (end == other.end)
            && (pair == other.pair);
    }

    StringView type;
    StringView style;
    StringIndexType start { 0 };
    StringIndexType middle { 0 };
    StringIndexType end { 0 };
    StringIndexType pair { 0 };
};

template<>
struct AK::Formatter<ListPatterns> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, ListPatterns const& patterns)
    {
        return Formatter<FormatString>::format(builder,
            "{{ ListPatternType::{}, ListPatternStyle::{}, {}, {}, {}, {} }}",
            format_identifier({}, patterns.type),
            format_identifier({}, patterns.style),
            patterns.start,
            patterns.middle,
            patterns.end,
            patterns.pair);
    }
};

template<>
struct AK::Traits<ListPatterns> : public GenericTraits<ListPatterns> {
    static unsigned hash(ListPatterns const& p) { return p.hash(); }
};

using LanguageList = Vector<StringIndexType>;
using TerritoryList = Vector<StringIndexType>;
using ScriptList = Vector<StringIndexType>;
using CurrencyList = Vector<StringIndexType>;
using KeywordList = Vector<StringIndexType>;
using ListPatternList = Vector<ListPatternIndexType>;

struct Locale {
    String language;
    Optional<String> territory;
    Optional<String> variant;
    LanguageListIndexType languages { 0 };
    TerritoryListIndexType territories { 0 };
    ScriptListIndexType scripts { 0 };
    CurrencyListIndexType long_currencies { 0 };
    CurrencyListIndexType short_currencies { 0 };
    CurrencyListIndexType narrow_currencies { 0 };
    CurrencyListIndexType numeric_currencies { 0 };
    KeywordListIndexType keywords { 0 };
    ListPatternListIndexType list_patterns { 0 };
};

struct LanguageMapping {
    CanonicalLanguageID<StringIndexType> key {};
    CanonicalLanguageID<StringIndexType> alias {};
};

struct UnicodeLocaleData {
    UniqueStringStorage<StringIndexType> unique_strings;
    UniqueStorage<LanguageList, LanguageListIndexType> unique_language_lists;
    UniqueStorage<TerritoryList, TerritoryListIndexType> unique_territory_lists;
    UniqueStorage<ScriptList, ScriptListIndexType> unique_script_lists;
    UniqueStorage<CurrencyList, CurrencyListIndexType> unique_currency_lists;
    UniqueStorage<KeywordList, KeywordListIndexType> unique_keyword_lists;
    UniqueStorage<ListPatterns, ListPatternIndexType> unique_list_patterns;
    UniqueStorage<ListPatternList, ListPatternListIndexType> unique_list_pattern_lists;

    HashMap<String, Locale> locales;
    Vector<Alias> locale_aliases;

    Vector<String> languages;
    Vector<String> territories;
    Vector<String> scripts;
    Vector<String> variants;
    Vector<String> currencies;
    Vector<String> keywords { "ca"sv, "nu"sv }; // FIXME: These should be parsed from BCP47. https://unicode-org.atlassian.net/browse/CLDR-15158
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

// Some parsing is expected to fail. For example, the CLDR contains language mappings
// with locales such as "en-GB-oed" that are canonically invalid locale IDs.
#define TRY_OR_DISCARD(expression)             \
    ({                                         \
        auto _temporary_result = (expression); \
        if (_temporary_result.is_error())      \
            return;                            \
        _temporary_result.release_value();     \
    })

static ErrorOr<LanguageMapping> parse_language_mapping(UnicodeLocaleData& locale_data, StringView key, StringView alias)
{
    auto parsed_key = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, key));
    auto parsed_alias = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, alias));
    return LanguageMapping { move(parsed_key), move(parsed_alias) };
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
                auto mapping = TRY_OR_DISCARD(parse_language_mapping(locale_data, key, alias));
                locale_data.max_variant_size = max(mapping.key.variants.size(), locale_data.max_variant_size);
                locale_data.max_variant_size = max(mapping.alias.variants.size(), locale_data.max_variant_size);
                locale_data.complex_mappings.append(move(mapping));
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
        auto mapping = TRY_OR_DISCARD(parse_language_mapping(locale_data, key, value.as_string()));
        locale_data.max_variant_size = max(mapping.key.variants.size(), locale_data.max_variant_size);
        locale_data.max_variant_size = max(mapping.alias.variants.size(), locale_data.max_variant_size);
        locale_data.likely_subtags.append(move(mapping));
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
    auto const& script_string = identity_object.as_object().get("script"sv);
    auto const& variant_string = identity_object.as_object().get("variant"sv);

    locale.language = language_string.as_string();
    if (!locale_data.languages.contains_slow(locale.language))
        locale_data.languages.append(locale.language);

    if (territory_string.is_string()) {
        locale.territory = territory_string.as_string();
        if (!locale_data.territories.contains_slow(*locale.territory))
            locale_data.territories.append(*locale.territory);
    }

    if (script_string.is_string()) {
        auto script = script_string.as_string();
        if (!locale_data.scripts.contains_slow(script))
            locale_data.scripts.append(script);
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
    auto locale_languages = TRY(JsonValue::from_string(languages_file->read_all()));

    auto const& main_object = locale_languages.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& languages_object = locale_display_names_object.as_object().get("languages"sv);

    LanguageList languages;
    languages.resize(locale_data.languages.size());

    languages_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (auto index = locale_data.languages.find_first_index(key); index.has_value())
            languages[*index] = locale_data.unique_strings.ensure(value.as_string());
    });

    locale.languages = locale_data.unique_language_lists.ensure(move(languages));
    return {};
}

static ErrorOr<void> parse_locale_territories(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath territories_path(move(locale_path));
    territories_path = territories_path.append("territories.json"sv);

    auto territories_file = TRY(Core::File::open(territories_path.string(), Core::OpenMode::ReadOnly));
    auto locale_territories = TRY(JsonValue::from_string(territories_file->read_all()));

    auto const& main_object = locale_territories.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(territories_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& territories_object = locale_display_names_object.as_object().get("territories"sv);

    TerritoryList territories;
    territories.resize(locale_data.territories.size());

    territories_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (auto index = locale_data.territories.find_first_index(key); index.has_value())
            territories[*index] = locale_data.unique_strings.ensure(value.as_string());
    });

    locale.territories = locale_data.unique_territory_lists.ensure(move(territories));
    return {};
}

static ErrorOr<void> parse_locale_scripts(String locale_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath scripts_path(move(locale_path));
    scripts_path = scripts_path.append("scripts.json"sv);

    auto scripts_file = TRY(Core::File::open(scripts_path.string(), Core::OpenMode::ReadOnly));
    auto locale_scripts = TRY(JsonValue::from_string(scripts_file->read_all()));

    auto const& main_object = locale_scripts.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(scripts_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& scripts_object = locale_display_names_object.as_object().get("scripts"sv);

    ScriptList scripts;
    scripts.resize(locale_data.scripts.size());

    scripts_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (auto index = locale_data.scripts.find_first_index(key); index.has_value())
            scripts[*index] = locale_data.unique_strings.ensure(value.as_string());
    });

    locale.scripts = locale_data.unique_script_lists.ensure(move(scripts));
    return {};
}

static ErrorOr<void> parse_locale_list_patterns(String misc_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath list_patterns_path(move(misc_path));
    list_patterns_path = list_patterns_path.append("listPatterns.json"sv);

    auto list_patterns_file = TRY(Core::File::open(list_patterns_path.string(), Core::OpenMode::ReadOnly));
    auto locale_list_patterns = TRY(JsonValue::from_string(list_patterns_file->read_all()));

    auto const& main_object = locale_list_patterns.as_object().get("main"sv);
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

    ListPatternList list_patterns;
    list_patterns.ensure_capacity(list_patterns_object.as_object().size());

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

        ListPatterns list_pattern { type, style, start, middle, end, pair };
        list_patterns.append(locale_data.unique_list_patterns.ensure(move(list_pattern)));
    });

    locale.list_patterns = locale_data.unique_list_pattern_lists.ensure(move(list_patterns));
    return {};
}

static ErrorOr<void> parse_locale_currencies(String numbers_path, UnicodeLocaleData& locale_data, Locale& locale)
{
    LexicalPath currencies_path(move(numbers_path));
    currencies_path = currencies_path.append("currencies.json"sv);

    auto currencies_file = TRY(Core::File::open(currencies_path.string(), Core::OpenMode::ReadOnly));
    auto locale_currencies = TRY(JsonValue::from_string(currencies_file->read_all()));

    auto const& main_object = locale_currencies.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(currencies_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);
    auto const& currencies_object = locale_numbers_object.as_object().get("currencies"sv);

    currencies_object.as_object().for_each_member([&](auto const& key, JsonValue const&) {
        if (!locale_data.currencies.contains_slow(key))
            locale_data.currencies.append(key);
    });

    CurrencyList long_currencies {};
    long_currencies.resize(locale_data.currencies.size());

    CurrencyList short_currencies {};
    short_currencies.resize(locale_data.currencies.size());

    CurrencyList narrow_currencies {};
    narrow_currencies.resize(locale_data.currencies.size());

    CurrencyList numeric_currencies {};
    numeric_currencies.resize(locale_data.currencies.size());

    currencies_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto const& long_name = value.as_object().get("displayName"sv);
        auto const& short_name = value.as_object().get("symbol"sv);
        auto const& narrow_name = value.as_object().get("symbol-alt-narrow"sv);
        auto const& numeric_name = value.as_object().get("displayName-count-other"sv);

        auto index = locale_data.currencies.find_first_index(key).value();
        long_currencies[index] = locale_data.unique_strings.ensure(long_name.as_string());
        short_currencies[index] = locale_data.unique_strings.ensure(short_name.as_string());
        narrow_currencies[index] = narrow_name.is_null() ? 0 : locale_data.unique_strings.ensure(narrow_name.as_string());
        numeric_currencies[index] = locale_data.unique_strings.ensure(numeric_name.is_null() ? long_name.as_string() : numeric_name.as_string());
    });

    locale.long_currencies = locale_data.unique_currency_lists.ensure(move(long_currencies));
    locale.short_currencies = locale_data.unique_currency_lists.ensure(move(short_currencies));
    locale.narrow_currencies = locale_data.unique_currency_lists.ensure(move(narrow_currencies));
    locale.numeric_currencies = locale_data.unique_currency_lists.ensure(move(numeric_currencies));
    return {};
}

static ErrorOr<void> parse_numeric_keywords(String locale_numbers_path, UnicodeLocaleData& locale_data, KeywordList& keywords)
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

    auto index = locale_data.keywords.find_first_index(key).value();
    keywords[index] = locale_data.unique_strings.ensure(builder.build());

    return {};
}

static ErrorOr<void> parse_calendar_keywords(String locale_dates_path, UnicodeLocaleData& locale_data, KeywordList& keywords)
{
    static constexpr StringView key = "ca"sv;

    auto calendars_iterator = TRY(path_to_dir_iterator(locale_dates_path, {}));
    Vector<String> keyword_values {};

    while (calendars_iterator.has_next()) {
        auto locale_calendars_path = TRY(next_path_from_dir_iterator(calendars_iterator));

        LexicalPath calendars_path(move(locale_calendars_path));
        if (!calendars_path.basename().starts_with("ca-"sv))
            continue;

        auto calendars_file = TRY(Core::File::open(calendars_path.string(), Core::OpenMode::ReadOnly));
        auto calendars = TRY(JsonValue::from_string(calendars_file->read_all()));

        auto const& main_object = calendars.as_object().get("main"sv);
        auto const& locale_object = main_object.as_object().get(calendars_path.parent().basename());
        auto const& dates_object = locale_object.as_object().get("dates"sv);
        auto const& calendars_object = dates_object.as_object().get("calendars"sv);

        calendars_object.as_object().for_each_member([&](auto const& calendar_name, JsonValue const&) {
            // The generic calendar is not a supported Unicode calendar key, so skip it:
            // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/calendar#unicode_calendar_keys
            if (calendar_name == "generic"sv)
                return;

            // FIXME: Similar to the calendar aliases defined in GenerateUnicodeDateTimeFormat, this
            //        should be parsed from BCP47. https://unicode-org.atlassian.net/browse/CLDR-15158
            if (calendar_name == "gregorian"sv)
                keyword_values.append("gregory"sv);
            else
                keyword_values.append(calendar_name);
        });
    }

    StringBuilder builder;
    builder.join(',', keyword_values);

    auto index = locale_data.keywords.find_first_index(key).value();
    keywords[index] = locale_data.unique_strings.ensure(builder.build());

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

static ErrorOr<void> define_aliases_without_scripts(UnicodeLocaleData& locale_data)
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

    auto append_alias_without_script = [&](auto const& locale) -> ErrorOr<void> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(locale_data.unique_strings, locale));
        if ((parsed_locale.language == 0) || (parsed_locale.script == 0) || (parsed_locale.region == 0))
            return {};

        auto locale_without_script = String::formatted("{}-{}",
            locale_data.unique_strings.get(parsed_locale.language),
            locale_data.unique_strings.get(parsed_locale.region));

        if (locale_data.locales.contains(locale_without_script))
            return {};
        if (find_alias(locale_without_script) != locale_data.locale_aliases.end())
            return {};

        if (auto it = find_alias(locale); it != locale_data.locale_aliases.end())
            locale_data.locale_aliases.append({ it->name, locale_without_script });
        else
            locale_data.locale_aliases.append({ locale, locale_without_script });

        return {};
    };

    for (auto const& locale : locale_data.locales)
        TRY(append_alias_without_script(locale.key));
    for (auto const& locale : locale_data.locale_aliases)
        TRY(append_alias_without_script(locale.alias));

    return {};
}

static ErrorOr<void> parse_all_locales(String core_path, String locale_names_path, String misc_path, String numbers_path, String dates_path, UnicodeLocaleData& locale_data)
{
    auto identity_iterator = TRY(path_to_dir_iterator(locale_names_path));
    auto locale_names_iterator = TRY(path_to_dir_iterator(move(locale_names_path)));
    auto misc_iterator = TRY(path_to_dir_iterator(move(misc_path)));
    auto numbers_iterator = TRY(path_to_dir_iterator(move(numbers_path)));
    auto dates_iterator = TRY(path_to_dir_iterator(move(dates_path)));

    LexicalPath core_supplemental_path(core_path);
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(Core::File::is_directory(core_supplemental_path.string()));

    TRY(parse_core_aliases(core_supplemental_path.string(), locale_data));
    TRY(parse_likely_subtags(core_supplemental_path.string(), locale_data));

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

    while (identity_iterator.has_next()) {
        auto locale_path = TRY(next_path_from_dir_iterator(identity_iterator));
        auto language = TRY(remove_variants_from_path(locale_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_identity(locale_path, locale_data, locale));
    }

    quick_sort(locale_data.languages);
    quick_sort(locale_data.territories);
    quick_sort(locale_data.scripts);

    HashMap<String, KeywordList> keywords;
    auto ensure_keyword_list = [&](auto const& language) -> KeywordList& {
        return keywords.ensure(language, [&]() {
            KeywordList keywords;
            keywords.resize(locale_data.keywords.size());
            return keywords;
        });
    };

    while (locale_names_iterator.has_next()) {
        auto locale_path = TRY(next_path_from_dir_iterator(locale_names_iterator));
        auto language = TRY(remove_variants_from_path(locale_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_locale_languages(locale_path, locale_data, locale));
        TRY(parse_locale_territories(locale_path, locale_data, locale));
        TRY(parse_locale_scripts(locale_path, locale_data, locale));
    }

    while (misc_iterator.has_next()) {
        auto misc_path = TRY(next_path_from_dir_iterator(misc_iterator));
        auto language = TRY(remove_variants_from_path(misc_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_locale_list_patterns(misc_path, locale_data, locale));
    }

    while (numbers_iterator.has_next()) {
        auto numbers_path = TRY(next_path_from_dir_iterator(numbers_iterator));
        auto language = TRY(remove_variants_from_path(numbers_path));

        auto& locale = locale_data.locales.ensure(language);
        TRY(parse_locale_currencies(numbers_path, locale_data, locale));

        auto& keywords = ensure_keyword_list(language);
        TRY(parse_numeric_keywords(numbers_path, locale_data, keywords));
    }

    while (dates_iterator.has_next()) {
        auto dates_path = TRY(next_path_from_dir_iterator(dates_iterator));
        auto language = TRY(remove_variants_from_path(dates_path));

        auto& keywords = ensure_keyword_list(language);
        TRY(parse_calendar_keywords(dates_path, locale_data, keywords));
    }

    TRY(parse_default_content_locales(move(core_path), locale_data));
    TRY(define_aliases_without_scripts(locale_data));

    for (auto& list : keywords) {
        auto& locale = locale_data.locales.find(list.key)->value;
        locale.keywords = locale_data.unique_keyword_lists.ensure(move(list.value));
    }

    return {};
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
    locale_data.unique_language_lists.generate(generator, s_string_index_type, "s_language_lists"sv);
    locale_data.unique_territory_lists.generate(generator, s_string_index_type, "s_territory_lists"sv);
    locale_data.unique_script_lists.generate(generator, s_string_index_type, "s_script_lists"sv);
    locale_data.unique_currency_lists.generate(generator, s_string_index_type, "s_currency_lists"sv);
    locale_data.unique_keyword_lists.generate(generator, s_string_index_type, "s_keyword_lists"sv);
    locale_data.unique_list_patterns.generate(generator, "Patterns"sv, "s_list_patterns"sv, 10);
    locale_data.unique_list_pattern_lists.generate(generator, s_list_pattern_index_type, "s_list_pattern_lists"sv);

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

    auto append_mapping = [&](auto const& keys, auto const& map, auto type, auto name, auto mapping_getter) {
        generator.set("type", type);
        generator.set("name", name);
        generator.set("size", String::number(keys.size()));

        generator.append(R"~~~(
static constexpr Array<@type@, @size@> @name@ { {)~~~");

        bool first = true;
        for (auto const& key : keys) {
            auto const& value = map.find(key)->value;
            auto mapping = mapping_getter(value);

            generator.append(first ? " " : ", ");
            generator.append(String::number(mapping));
            first = false;
        }

        generator.append(" } };");
    };

    auto locales = locale_data.locales.keys();
    quick_sort(locales);

    append_mapping(locales, locale_data.locales, s_language_list_index_type, "s_languages"sv, [&](auto const& locale) { return locale.languages; });
    append_mapping(locales, locale_data.locales, s_territory_list_index_type, "s_territories"sv, [&](auto const& locale) { return locale.territories; });
    append_mapping(locales, locale_data.locales, s_script_list_index_type, "s_scripts"sv, [&](auto const& locale) { return locale.scripts; });
    append_mapping(locales, locale_data.locales, s_currency_list_index_type, "s_long_currencies"sv, [&](auto const& locale) { return locale.long_currencies; });
    append_mapping(locales, locale_data.locales, s_currency_list_index_type, "s_short_currencies"sv, [&](auto const& locale) { return locale.short_currencies; });
    append_mapping(locales, locale_data.locales, s_currency_list_index_type, "s_narrow_currencies"sv, [&](auto const& locale) { return locale.narrow_currencies; });
    append_mapping(locales, locale_data.locales, s_currency_list_index_type, "s_numeric_currencies"sv, [&](auto const& locale) { return locale.numeric_currencies; });
    append_mapping(locales, locale_data.locales, s_keyword_list_index_type, "s_keywords"sv, [&](auto const& locale) { return locale.keywords; });
    append_mapping(locales, locale_data.locales, s_list_pattern_list_index_type, "s_locale_list_patterns"sv, [&](auto const& locale) { return locale.list_patterns; });

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

    auto append_mapping_search = [&](StringView enum_snake, StringView from_string_name, StringView collection_name, StringView unique_list) {
        generator.set("enum_snake", enum_snake);
        generator.set("from_string_name", from_string_name);
        generator.set("collection_name", collection_name);
        generator.set("unique_list", unique_list);

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

    auto mapping_index = @collection_name@.at(locale_index);
    auto const& mappings = @unique_list@.at(mapping_index);

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

    append_mapping_search("language"sv, "language"sv, "s_languages"sv, "s_language_lists"sv);
    append_from_string("Language"sv, "language"sv, locale_data.languages);
    append_alias_search("language"sv, locale_data.language_aliases);

    append_mapping_search("territory"sv, "territory"sv, "s_territories"sv, "s_territory_lists"sv);
    append_from_string("Territory"sv, "territory"sv, locale_data.territories);
    append_alias_search("territory"sv, locale_data.territory_aliases);

    append_mapping_search("script_tag"sv, "script_tag"sv, "s_scripts"sv, "s_script_lists"sv);
    append_from_string("ScriptTag"sv, "script_tag"sv, locale_data.scripts);
    append_alias_search("script_tag"sv, locale_data.script_aliases);

    append_mapping_search("long_currency"sv, "currency"sv, "s_long_currencies"sv, "s_currency_lists"sv);
    append_mapping_search("short_currency"sv, "currency"sv, "s_short_currencies"sv, "s_currency_lists"sv);
    append_mapping_search("narrow_currency"sv, "currency"sv, "s_narrow_currencies"sv, "s_currency_lists"sv);
    append_mapping_search("numeric_currency"sv, "currency"sv, "s_numeric_currencies"sv, "s_currency_lists"sv);
    append_from_string("Currency"sv, "currency"sv, locale_data.currencies);

    append_mapping_search("key"sv, "key"sv, "s_keywords"sv, "s_keyword_lists"sv);
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

    auto list_patterns_list_index = s_locale_list_patterns.at(locale_index);
    auto const& locale_list_patterns = s_list_pattern_lists.at(list_patterns_list_index);

    for (auto list_patterns_index : locale_list_patterns) {
        auto const& list_patterns = s_list_patterns.at(list_patterns_index);

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
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView core_path;
    StringView locale_names_path;
    StringView misc_path;
    StringView numbers_path;
    StringView dates_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(locale_names_path, "Path to cldr-localenames directory", "locale-names-path", 'l', "locale-names-path");
    args_parser.add_option(misc_path, "Path to cldr-misc directory", "misc-path", 'm', "misc-path");
    args_parser.add_option(numbers_path, "Path to cldr-numbers directory", "numbers-path", 'n', "numbers-path");
    args_parser.add_option(dates_path, "Path to cldr-dates directory", "dates-path", 'd', "dates-path");
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
    TRY(parse_all_locales(core_path, locale_names_path, misc_path, numbers_path, dates_path, locale_data));

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
