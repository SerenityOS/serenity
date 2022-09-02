/*
 * Copyright (c) 2021-2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "../LibUnicode/GeneratorUtil.h" // FIXME: Move this somewhere common.
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
#include <LibCore/Stream.h>

using StringIndexType = u32;
constexpr auto s_string_index_type = "u32"sv;

using DisplayPatternIndexType = u8;
constexpr auto s_display_pattern_index_type = "u8"sv;

using LanguageListIndexType = u8;
constexpr auto s_language_list_index_type = "u8"sv;

using TerritoryListIndexType = u8;
constexpr auto s_territory_list_index_type = "u8"sv;

using ScriptListIndexType = u8;
constexpr auto s_script_list_index_type = "u8"sv;

using CurrencyListIndexType = u16;
constexpr auto s_currency_list_index_type = "u16"sv;

using CalendarListIndexType = u8;
constexpr auto s_calendar_list_index_type = "u8"sv;

using DateFieldListIndexType = u8;
constexpr auto s_date_field_list_index_type = "u8"sv;

using KeywordListIndexType = u8;
constexpr auto s_keyword_list_index_type = "u8"sv;

using ListPatternIndexType = u16;
constexpr auto s_list_pattern_index_type = "u16"sv;

using ListPatternListIndexType = u8;
constexpr auto s_list_pattern_list_index_type = "u8"sv;

using TextLayoutIndexType = u8;
constexpr auto s_text_layout_index_type = "u8"sv;

static String format_identifier(StringView owner, String identifier)
{
    identifier = identifier.replace("-"sv, "_"sv, ReplaceMode::All);

    if (all_of(identifier, is_ascii_digit))
        return String::formatted("{}_{}", owner[0], identifier);
    if (is_ascii_lower_alpha(identifier[0]))
        return String::formatted("{:c}{}", to_ascii_uppercase(identifier[0]), identifier.substring_view(1));
    return identifier;
}

struct DisplayPattern {
    unsigned hash() const
    {
        return pair_int_hash(locale_pattern, locale_separator);
    }

    bool operator==(DisplayPattern const& other) const
    {
        return (locale_pattern == other.locale_pattern)
            && (locale_separator == other.locale_separator);
    }

    StringIndexType locale_pattern { 0 };
    StringIndexType locale_separator { 0 };
};

template<>
struct AK::Formatter<DisplayPattern> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, DisplayPattern const& patterns)
    {
        return Formatter<FormatString>::format(builder,
            "{{ {}, {} }}"sv,
            patterns.locale_pattern,
            patterns.locale_separator);
    }
};

template<>
struct AK::Traits<DisplayPattern> : public GenericTraits<DisplayPattern> {
    static unsigned hash(DisplayPattern const& p) { return p.hash(); }
};

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
            "{{ ListPatternType::{}, Style::{}, {}, {}, {}, {} }}"sv,
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

struct TextLayout {
    unsigned hash() const
    {
        return character_order.hash();
    }

    bool operator==(TextLayout const& other) const
    {
        return character_order == other.character_order;
    }

    StringView character_order;
};

template<>
struct AK::Formatter<TextLayout> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, TextLayout const& patterns)
    {
        return Formatter<FormatString>::format(builder,
            "{{ CharacterOrder::{} }}"sv,
            format_identifier({}, patterns.character_order));
    }
};

template<>
struct AK::Traits<TextLayout> : public GenericTraits<TextLayout> {
    static unsigned hash(TextLayout const& t) { return t.hash(); }
};

using LanguageList = Vector<StringIndexType>;
using TerritoryList = Vector<StringIndexType>;
using ScriptList = Vector<StringIndexType>;
using CurrencyList = Vector<StringIndexType>;
using CalendarList = Vector<StringIndexType>;
using DateFieldList = Vector<StringIndexType>;
using KeywordList = Vector<StringIndexType>;
using ListPatternList = Vector<ListPatternIndexType>;

struct LocaleData {
    String language;
    Optional<String> territory;
    Optional<String> variant;
    DisplayPatternIndexType display_patterns { 0 };
    LanguageListIndexType languages { 0 };
    TerritoryListIndexType territories { 0 };
    ScriptListIndexType scripts { 0 };
    CurrencyListIndexType long_currencies { 0 };
    CurrencyListIndexType short_currencies { 0 };
    CurrencyListIndexType narrow_currencies { 0 };
    CurrencyListIndexType numeric_currencies { 0 };
    CalendarListIndexType calendars { 0 };
    DateFieldListIndexType long_date_fields { 0 };
    DateFieldListIndexType short_date_fields { 0 };
    DateFieldListIndexType narrow_date_fields { 0 };
    KeywordListIndexType calendar_keywords { 0 };
    KeywordListIndexType collation_case_keywords { 0 };
    KeywordListIndexType collation_numeric_keywords { 0 };
    KeywordListIndexType number_system_keywords { 0 };
    ListPatternListIndexType list_patterns { 0 };
    TextLayoutIndexType text_layout { 0 };
};

struct LanguageMapping {
    CanonicalLanguageID<StringIndexType> key {};
    CanonicalLanguageID<StringIndexType> alias {};
};

struct CLDR {
    UniqueStringStorage<StringIndexType> unique_strings;
    UniqueStorage<DisplayPattern, DisplayPatternIndexType> unique_display_patterns;
    UniqueStorage<LanguageList, LanguageListIndexType> unique_language_lists;
    UniqueStorage<TerritoryList, TerritoryListIndexType> unique_territory_lists;
    UniqueStorage<ScriptList, ScriptListIndexType> unique_script_lists;
    UniqueStorage<CurrencyList, CurrencyListIndexType> unique_currency_lists;
    UniqueStorage<CalendarList, CalendarListIndexType> unique_calendar_lists;
    UniqueStorage<DateFieldList, DateFieldListIndexType> unique_date_field_lists;
    UniqueStorage<KeywordList, KeywordListIndexType> unique_keyword_lists;
    UniqueStorage<ListPatterns, ListPatternIndexType> unique_list_patterns;
    UniqueStorage<ListPatternList, ListPatternListIndexType> unique_list_pattern_lists;
    UniqueStorage<TextLayout, TextLayoutIndexType> unique_text_layouts;

    HashMap<String, LocaleData> locales;
    Vector<Alias> locale_aliases;

    Vector<String> languages;
    Vector<String> territories;
    Vector<String> scripts;
    Vector<String> variants;
    Vector<String> currencies;
    Vector<String> date_fields;
    Vector<Alias> date_field_aliases {
        // ECMA-402 and the CLDR refer to some date fields with different names. Defining these aliases
        // means we can remain agnostic about the naming differences elsewhere.
        { "dayperiod"sv, "dayPeriod"sv },
        { "week"sv, "weekOfYear"sv },
        { "zone"sv, "timeZoneName"sv },
    };

    HashMap<String, Vector<String>> keywords;
    HashMap<String, Vector<Alias>> keyword_aliases;
    HashMap<String, String> keyword_names;

    Vector<String> list_pattern_types;
    Vector<String> character_orders;
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

static ErrorOr<LanguageMapping> parse_language_mapping(CLDR& cldr, StringView key, StringView alias)
{
    auto parsed_key = TRY(CanonicalLanguageID<StringIndexType>::parse(cldr.unique_strings, key));
    auto parsed_alias = TRY(CanonicalLanguageID<StringIndexType>::parse(cldr.unique_strings, alias));
    return LanguageMapping { move(parsed_key), move(parsed_alias) };
}

static ErrorOr<void> parse_core_aliases(String core_supplemental_path, CLDR& cldr)
{
    LexicalPath core_aliases_path(move(core_supplemental_path));
    core_aliases_path = core_aliases_path.append("aliases.json"sv);

    auto core_aliases = TRY(read_json_file(core_aliases_path.string()));
    auto const& supplemental_object = core_aliases.as_object().get("supplemental"sv);
    auto const& metadata_object = supplemental_object.as_object().get("metadata"sv);
    auto const& alias_object = metadata_object.as_object().get("alias"sv);

    auto append_aliases = [&](auto& alias_object, auto& alias_map) {
        alias_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
            auto alias = value.as_object().get("_replacement"sv).as_string();

            if (key.contains('-')) {
                auto mapping = TRY_OR_DISCARD(parse_language_mapping(cldr, key, alias));
                cldr.max_variant_size = max(mapping.key.variants.size(), cldr.max_variant_size);
                cldr.max_variant_size = max(mapping.alias.variants.size(), cldr.max_variant_size);
                cldr.complex_mappings.append(move(mapping));
            } else {
                alias_map.set(key, cldr.unique_strings.ensure(alias));
            }
        });
    };

    append_aliases(alias_object.as_object().get("languageAlias"sv), cldr.language_aliases);
    append_aliases(alias_object.as_object().get("territoryAlias"sv), cldr.territory_aliases);
    append_aliases(alias_object.as_object().get("scriptAlias"sv), cldr.script_aliases);
    append_aliases(alias_object.as_object().get("variantAlias"sv), cldr.variant_aliases);
    append_aliases(alias_object.as_object().get("subdivisionAlias"sv), cldr.subdivision_aliases);

    return {};
}

static ErrorOr<void> parse_likely_subtags(String core_supplemental_path, CLDR& cldr)
{
    LexicalPath likely_subtags_path(move(core_supplemental_path));
    likely_subtags_path = likely_subtags_path.append("likelySubtags.json"sv);

    auto likely_subtags = TRY(read_json_file(likely_subtags_path.string()));
    auto const& supplemental_object = likely_subtags.as_object().get("supplemental"sv);
    auto const& likely_subtags_object = supplemental_object.as_object().get("likelySubtags"sv);

    likely_subtags_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto mapping = TRY_OR_DISCARD(parse_language_mapping(cldr, key, value.as_string()));
        cldr.max_variant_size = max(mapping.key.variants.size(), cldr.max_variant_size);
        cldr.max_variant_size = max(mapping.alias.variants.size(), cldr.max_variant_size);
        cldr.likely_subtags.append(move(mapping));
    });

    return {};
}

static ErrorOr<void> parse_identity(String locale_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath languages_path(move(locale_path)); // Note: Every JSON file defines identity data, so we can use any of them.
    languages_path = languages_path.append("languages.json"sv);

    auto languages = TRY(read_json_file(languages_path.string()));
    auto const& main_object = languages.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& identity_object = locale_object.as_object().get("identity"sv);
    auto const& language_string = identity_object.as_object().get("language"sv);
    auto const& territory_string = identity_object.as_object().get("territory"sv);
    auto const& script_string = identity_object.as_object().get("script"sv);
    auto const& variant_string = identity_object.as_object().get("variant"sv);

    locale.language = language_string.as_string();

    if (territory_string.is_string()) {
        locale.territory = territory_string.as_string();
        if (!cldr.territories.contains_slow(*locale.territory))
            cldr.territories.append(*locale.territory);
    }

    if (script_string.is_string()) {
        auto script = script_string.as_string();
        if (!cldr.scripts.contains_slow(script))
            cldr.scripts.append(script);
    }

    if (variant_string.is_string()) {
        locale.variant = variant_string.as_string();
        if (!cldr.variants.contains_slow(*locale.variant))
            cldr.variants.append(*locale.variant);
    }

    return {};
}

static ErrorOr<void> parse_locale_display_patterns(String locale_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath locale_display_names_path(move(locale_path));
    locale_display_names_path = locale_display_names_path.append("localeDisplayNames.json"sv);

    auto locale_display_names = TRY(read_json_file(locale_display_names_path.string()));
    auto const& main_object = locale_display_names.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(locale_display_names_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& locale_display_patterns_object = locale_display_names_object.as_object().get("localeDisplayPattern"sv);
    auto const& locale_pattern = locale_display_patterns_object.as_object().get("localePattern"sv);
    auto const& locale_separator = locale_display_patterns_object.as_object().get("localeSeparator"sv);

    DisplayPattern patterns {};
    patterns.locale_pattern = cldr.unique_strings.ensure(locale_pattern.as_string());
    patterns.locale_separator = cldr.unique_strings.ensure(locale_separator.as_string());

    locale.display_patterns = cldr.unique_display_patterns.ensure(move(patterns));
    return {};
}

static ErrorOr<void> preprocess_languages(String locale_path, CLDR& cldr)
{
    LexicalPath languages_path(move(locale_path));
    languages_path = languages_path.append("languages.json"sv);

    auto locale_languages = TRY(read_json_file(languages_path.string()));
    auto const& main_object = locale_languages.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& languages_object = locale_display_names_object.as_object().get("languages"sv);

    languages_object.as_object().for_each_member([&](auto const& key, auto const&) {
        if (!key.contains("-alt-"sv) && !cldr.languages.contains_slow(key))
            cldr.languages.append(key);
    });

    return {};
}

static ErrorOr<void> parse_unicode_extension_keywords(String bcp47_path, CLDR& cldr)
{
    constexpr auto desired_keywords = Array { "ca"sv, "co"sv, "hc"sv, "kf"sv, "kn"sv, "nu"sv };
    auto keywords = TRY(read_json_file(bcp47_path));

    auto const& keyword_object = keywords.as_object().get("keyword"sv);
    auto const& unicode_object = keyword_object.as_object().get("u"sv);
    if (unicode_object.is_null())
        return {};

    unicode_object.as_object().for_each_member([&](auto const& key, auto const& value) {
        if (!desired_keywords.span().contains_slow(key))
            return;

        auto const& name = value.as_object().get("_alias"sv);
        cldr.keyword_names.set(key, name.as_string());

        auto& keywords = cldr.keywords.ensure(key);

        // FIXME: ECMA-402 requires the list of supported collation types to include "default", but
        //        that type does not appear in collation.json.
        if (key == "co" && !keywords.contains_slow("default"sv))
            keywords.append("default"sv);

        value.as_object().for_each_member([&](auto const& keyword, auto const& properties) {
            if (!properties.is_object())
                return;

            // Filter out values not permitted by ECMA-402.
            // https://tc39.es/ecma402/#sec-intl-collator-internal-slots
            if (key == "co"sv && keyword.is_one_of("search"sv, "standard"sv))
                return;
            // https://tc39.es/ecma402/#sec-intl.numberformat-internal-slots
            if (key == "nu"sv && keyword.is_one_of("finance"sv, "native"sv, "traditio"sv))
                return;

            if (auto const& preferred = properties.as_object().get("_preferred"sv); preferred.is_string()) {
                cldr.keyword_aliases.ensure(key).append({ preferred.as_string(), keyword });
                return;
            }

            if (auto const& alias = properties.as_object().get("_alias"sv); alias.is_string())
                cldr.keyword_aliases.ensure(key).append({ keyword, alias.as_string() });

            keywords.append(keyword);
        });
    });

    return {};
}

static Optional<String> find_keyword_alias(StringView key, StringView calendar, CLDR& cldr)
{
    auto it = cldr.keyword_aliases.find(key);
    if (it == cldr.keyword_aliases.end())
        return {};

    auto alias = it->value.find_if([&](auto const& alias) { return calendar == alias.alias; });
    if (alias == it->value.end())
        return {};

    return alias->name;
}

static ErrorOr<void> parse_locale_languages(String locale_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath languages_path(move(locale_path));
    languages_path = languages_path.append("languages.json"sv);

    auto locale_languages = TRY(read_json_file(languages_path.string()));
    auto const& main_object = locale_languages.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(languages_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& languages_object = locale_display_names_object.as_object().get("languages"sv);

    LanguageList languages;
    languages.resize(cldr.languages.size());

    languages_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (key.contains("-alt-"sv))
            return;

        auto index = cldr.languages.find_first_index(key).value();
        languages[index] = cldr.unique_strings.ensure(value.as_string());
    });

    locale.languages = cldr.unique_language_lists.ensure(move(languages));
    return {};
}

static ErrorOr<void> parse_locale_territories(String locale_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath territories_path(move(locale_path));
    territories_path = territories_path.append("territories.json"sv);

    auto locale_territories = TRY(read_json_file(territories_path.string()));
    auto const& main_object = locale_territories.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(territories_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& territories_object = locale_display_names_object.as_object().get("territories"sv);

    TerritoryList territories;
    territories.resize(cldr.territories.size());

    territories_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (auto index = cldr.territories.find_first_index(key); index.has_value())
            territories[*index] = cldr.unique_strings.ensure(value.as_string());
    });

    locale.territories = cldr.unique_territory_lists.ensure(move(territories));
    return {};
}

static ErrorOr<void> parse_locale_scripts(String locale_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath scripts_path(move(locale_path));
    scripts_path = scripts_path.append("scripts.json"sv);

    auto locale_scripts = TRY(read_json_file(scripts_path.string()));
    auto const& main_object = locale_scripts.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(scripts_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& scripts_object = locale_display_names_object.as_object().get("scripts"sv);

    ScriptList scripts;
    scripts.resize(cldr.scripts.size());

    scripts_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (auto index = cldr.scripts.find_first_index(key); index.has_value())
            scripts[*index] = cldr.unique_strings.ensure(value.as_string());
    });

    locale.scripts = cldr.unique_script_lists.ensure(move(scripts));
    return {};
}

static ErrorOr<void> parse_locale_list_patterns(String misc_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath list_patterns_path(move(misc_path));
    list_patterns_path = list_patterns_path.append("listPatterns.json"sv);

    auto locale_list_patterns = TRY(read_json_file(list_patterns_path.string()));
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

        auto start = cldr.unique_strings.ensure(value.as_object().get("start"sv).as_string());
        auto middle = cldr.unique_strings.ensure(value.as_object().get("middle"sv).as_string());
        auto end = cldr.unique_strings.ensure(value.as_object().get("end"sv).as_string());
        auto pair = cldr.unique_strings.ensure(value.as_object().get("2"sv).as_string());

        if (!cldr.list_pattern_types.contains_slow(type))
            cldr.list_pattern_types.append(type);

        ListPatterns list_pattern { type, style, start, middle, end, pair };
        list_patterns.append(cldr.unique_list_patterns.ensure(move(list_pattern)));
    });

    locale.list_patterns = cldr.unique_list_pattern_lists.ensure(move(list_patterns));
    return {};
}

static ErrorOr<void> parse_locale_layout(String misc_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath layout_path(move(misc_path));
    layout_path = layout_path.append("layout.json"sv);

    auto locale_layout = TRY(read_json_file(layout_path.string()));
    auto const& main_object = locale_layout.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(layout_path.parent().basename());
    auto const& layout_object = locale_object.as_object().get("layout"sv);
    auto const& orientation_object = layout_object.as_object().get("orientation"sv);

    auto text_layout_character_order = [](StringView key) {
        if (key == "left-to-right"sv)
            return "ltr"sv;
        if (key == "right-to-left"sv)
            return "rtl"sv;
        VERIFY_NOT_REACHED();
    };

    auto const& character_order_string = orientation_object.as_object().get("characterOrder"sv);
    auto const& character_order = character_order_string.as_string();

    TextLayout layout {};
    layout.character_order = text_layout_character_order(character_order);

    if (!cldr.character_orders.contains_slow(layout.character_order))
        cldr.character_orders.append(layout.character_order);

    locale.text_layout = cldr.unique_text_layouts.ensure(move(layout));
    return {};
}

static ErrorOr<void> parse_locale_currencies(String numbers_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath currencies_path(move(numbers_path));
    currencies_path = currencies_path.append("currencies.json"sv);

    auto locale_currencies = TRY(read_json_file(currencies_path.string()));
    auto const& main_object = locale_currencies.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(currencies_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);
    auto const& currencies_object = locale_numbers_object.as_object().get("currencies"sv);

    currencies_object.as_object().for_each_member([&](auto const& key, JsonValue const&) {
        if (!cldr.currencies.contains_slow(key))
            cldr.currencies.append(key);
    });

    CurrencyList long_currencies {};
    long_currencies.resize(cldr.currencies.size());

    CurrencyList short_currencies {};
    short_currencies.resize(cldr.currencies.size());

    CurrencyList narrow_currencies {};
    narrow_currencies.resize(cldr.currencies.size());

    CurrencyList numeric_currencies {};
    numeric_currencies.resize(cldr.currencies.size());

    currencies_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        auto const& long_name = value.as_object().get("displayName"sv);
        auto const& short_name = value.as_object().get("symbol"sv);
        auto const& narrow_name = value.as_object().get("symbol-alt-narrow"sv);
        auto const& numeric_name = value.as_object().get("displayName-count-other"sv);

        auto index = cldr.currencies.find_first_index(key).value();
        long_currencies[index] = cldr.unique_strings.ensure(long_name.as_string());
        short_currencies[index] = cldr.unique_strings.ensure(short_name.as_string());
        narrow_currencies[index] = narrow_name.is_null() ? 0 : cldr.unique_strings.ensure(narrow_name.as_string());
        numeric_currencies[index] = cldr.unique_strings.ensure(numeric_name.is_null() ? long_name.as_string() : numeric_name.as_string());
    });

    locale.long_currencies = cldr.unique_currency_lists.ensure(move(long_currencies));
    locale.short_currencies = cldr.unique_currency_lists.ensure(move(short_currencies));
    locale.narrow_currencies = cldr.unique_currency_lists.ensure(move(narrow_currencies));
    locale.numeric_currencies = cldr.unique_currency_lists.ensure(move(numeric_currencies));
    return {};
}

static ErrorOr<void> parse_locale_calendars(String locale_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath locale_display_names_path(move(locale_path));
    locale_display_names_path = locale_display_names_path.append("localeDisplayNames.json"sv);

    auto locale_display_names = TRY(read_json_file(locale_display_names_path.string()));
    auto const& main_object = locale_display_names.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(locale_display_names_path.parent().basename());
    auto const& locale_display_names_object = locale_object.as_object().get("localeDisplayNames"sv);
    auto const& types_object = locale_display_names_object.as_object().get("types"sv);
    auto const& calendar_object = types_object.as_object().get("calendar"sv);

    auto const& supported_calendars = cldr.keywords.find("ca"sv)->value;

    CalendarList calendars;
    calendars.resize(supported_calendars.size());

    calendar_object.as_object().for_each_member([&](auto const& key, auto const& calendar) {
        auto index = supported_calendars.find_first_index(key);
        if (!index.has_value()) {
            auto alias = find_keyword_alias("ca"sv, key, cldr);
            index = supported_calendars.find_first_index(*alias);
        }

        calendars[*index] = cldr.unique_strings.ensure(calendar.as_string());
    });

    locale.calendars = cldr.unique_calendar_lists.ensure(move(calendars));
    return {};
}

static ErrorOr<void> parse_locale_date_fields(String dates_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath date_fields_path(move(dates_path));
    date_fields_path = date_fields_path.append("dateFields.json"sv);

    auto locale_date_fields = TRY(read_json_file(date_fields_path.string()));
    auto const& main_object = locale_date_fields.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(date_fields_path.parent().basename());
    auto const& dates_object = locale_object.as_object().get("dates"sv);
    auto const& fields_object = dates_object.as_object().get("fields"sv);

    auto is_sanctioned_field = [](StringView field) {
        // This is a copy of the units sanctioned for use within ECMA-402, with names adjusted for the names used by the CLDR.
        // https://tc39.es/ecma402/#table-validcodeforDateField
        return field.is_one_of("era"sv, "year"sv, "quarter"sv, "month"sv, "week"sv, "weekday"sv, "day"sv, "dayperiod"sv, "hour"sv, "minute"sv, "second"sv, "zone"sv);
    };

    fields_object.as_object().for_each_member([&](auto const& key, JsonValue const&) {
        if (!is_sanctioned_field(key))
            return;

        if (!cldr.date_fields.contains_slow(key))
            cldr.date_fields.append(key);
    });

    quick_sort(cldr.date_fields);

    DateFieldList long_date_fields {};
    long_date_fields.resize(cldr.date_fields.size());

    DateFieldList short_date_fields {};
    short_date_fields.resize(cldr.date_fields.size());

    DateFieldList narrow_date_fields {};
    narrow_date_fields.resize(cldr.date_fields.size());

    fields_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!is_sanctioned_field(key))
            return;

        auto const& long_name = value.as_object().get("displayName"sv);
        auto const& short_name = fields_object.as_object().get(String::formatted("{}-short", key)).as_object().get("displayName"sv);
        auto const& narrow_name = fields_object.as_object().get(String::formatted("{}-narrow", key)).as_object().get("displayName"sv);

        auto index = cldr.date_fields.find_first_index(key).value();
        long_date_fields[index] = cldr.unique_strings.ensure(long_name.as_string());
        short_date_fields[index] = cldr.unique_strings.ensure(short_name.as_string());
        narrow_date_fields[index] = cldr.unique_strings.ensure(narrow_name.as_string());
    });

    locale.long_date_fields = cldr.unique_date_field_lists.ensure(move(long_date_fields));
    locale.short_date_fields = cldr.unique_date_field_lists.ensure(move(short_date_fields));
    locale.narrow_date_fields = cldr.unique_date_field_lists.ensure(move(narrow_date_fields));
    return {};
}

static ErrorOr<void> parse_number_system_keywords(String locale_numbers_path, CLDR& cldr, LocaleData& locale)
{
    LexicalPath numbers_path(move(locale_numbers_path));
    numbers_path = numbers_path.append("numbers.json"sv);

    auto numbers = TRY(read_json_file(numbers_path.string()));
    auto const& main_object = numbers.as_object().get("main"sv);
    auto const& locale_object = main_object.as_object().get(numbers_path.parent().basename());
    auto const& locale_numbers_object = locale_object.as_object().get("numbers"sv);
    auto const& default_numbering_system_object = locale_numbers_object.as_object().get("defaultNumberingSystem"sv);
    auto const& other_numbering_systems_object = locale_numbers_object.as_object().get("otherNumberingSystems"sv);

    KeywordList keywords {};

    auto append_numbering_system = [&](String system_name) {
        if (auto system_alias = find_keyword_alias("nu"sv, system_name, cldr); system_alias.has_value())
            system_name = system_alias.release_value();

        auto index = cldr.unique_strings.ensure(move(system_name));
        if (!keywords.contains_slow(index))
            keywords.append(move(index));
    };

    append_numbering_system(default_numbering_system_object.as_string());

    other_numbering_systems_object.as_object().for_each_member([&](auto const&, JsonValue const& value) {
        append_numbering_system(value.as_string());
    });

    locale_numbers_object.as_object().for_each_member([&](auto const& key, JsonValue const& value) {
        if (!key.starts_with("defaultNumberingSystem-alt-"sv))
            return;
        append_numbering_system(value.as_string());
    });

    locale.number_system_keywords = cldr.unique_keyword_lists.ensure(move(keywords));
    return {};
}

static ErrorOr<void> parse_calendar_keywords(String locale_dates_path, CLDR& cldr, LocaleData& locale)
{
    auto calendars_iterator = TRY(path_to_dir_iterator(locale_dates_path, {}));
    KeywordList keywords {};

    while (calendars_iterator.has_next()) {
        auto locale_calendars_path = TRY(next_path_from_dir_iterator(calendars_iterator));

        LexicalPath calendars_path(move(locale_calendars_path));
        if (!calendars_path.basename().starts_with("ca-"sv))
            continue;

        auto calendars = TRY(read_json_file(calendars_path.string()));
        auto const& main_object = calendars.as_object().get("main"sv);
        auto const& locale_object = main_object.as_object().get(calendars_path.parent().basename());
        auto const& dates_object = locale_object.as_object().get("dates"sv);
        auto const& calendars_object = dates_object.as_object().get("calendars"sv);

        calendars_object.as_object().for_each_member([&](auto calendar_name, JsonValue const&) {
            // The generic calendar is not a supported Unicode calendar key, so skip it:
            // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Locale/calendar#unicode_calendar_keys
            if (calendar_name == "generic"sv)
                return;

            if (auto calendar_alias = find_keyword_alias("ca"sv, calendar_name, cldr); calendar_alias.has_value())
                calendar_name = calendar_alias.release_value();

            keywords.append(cldr.unique_strings.ensure(calendar_name));
        });
    }

    locale.calendar_keywords = cldr.unique_keyword_lists.ensure(move(keywords));
    return {};
}

static void fill_in_collation_keywords(CLDR& cldr, LocaleData& locale)
{
    // FIXME: If collation data becomes available in the CLDR, parse per-locale ordering from there.
    auto create_list_with_default_first = [&](auto key, auto default_value) {
        auto& values = cldr.keywords.find(key)->value;

        quick_sort(values, [&](auto const& lhs, auto const& rhs) {
            if (lhs == default_value)
                return true;
            if (rhs == default_value)
                return false;
            return lhs < rhs;
        });

        KeywordList keywords;
        keywords.ensure_capacity(values.size());

        for (auto const& value : values)
            keywords.append(cldr.unique_strings.ensure(value));

        return cldr.unique_keyword_lists.ensure(move(keywords));
    };

    static auto kf_index = create_list_with_default_first("kf"sv, "upper"sv);
    static auto kn_index = create_list_with_default_first("kn"sv, "true"sv);

    locale.collation_case_keywords = kf_index;
    locale.collation_numeric_keywords = kn_index;
}

static ErrorOr<void> parse_default_content_locales(String core_path, CLDR& cldr)
{
    LexicalPath default_content_path(move(core_path));
    default_content_path = default_content_path.append("defaultContent.json"sv);

    auto default_content = TRY(read_json_file(default_content_path.string()));
    auto const& default_content_array = default_content.as_object().get("defaultContent"sv);

    default_content_array.as_array().for_each([&](JsonValue const& value) {
        auto locale = value.as_string();
        StringView default_locale = locale;

        while (true) {
            if (cldr.locales.contains(default_locale))
                break;

            auto pos = default_locale.find_last('-');
            if (!pos.has_value())
                return;

            default_locale = default_locale.substring_view(0, *pos);
        }

        if (default_locale != locale)
            cldr.locale_aliases.append({ default_locale, move(locale) });
    });

    return {};
}

static ErrorOr<void> define_aliases_without_scripts(CLDR& cldr)
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
        return cldr.locale_aliases.find_if([&](auto const& alias) { return locale == alias.alias; });
    };

    auto append_alias_without_script = [&](auto const& locale) -> ErrorOr<void> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(cldr.unique_strings, locale));
        if ((parsed_locale.language == 0) || (parsed_locale.script == 0) || (parsed_locale.region == 0))
            return {};

        auto locale_without_script = String::formatted("{}-{}",
            cldr.unique_strings.get(parsed_locale.language),
            cldr.unique_strings.get(parsed_locale.region));

        if (cldr.locales.contains(locale_without_script))
            return {};
        if (find_alias(locale_without_script) != cldr.locale_aliases.end())
            return {};

        if (auto it = find_alias(locale); it != cldr.locale_aliases.end())
            cldr.locale_aliases.append({ it->name, locale_without_script });
        else
            cldr.locale_aliases.append({ locale, locale_without_script });

        return {};
    };

    for (auto const& locale : cldr.locales)
        TRY(append_alias_without_script(locale.key));
    for (auto const& locale : cldr.locale_aliases)
        TRY(append_alias_without_script(locale.alias));

    return {};
}

static ErrorOr<void> parse_all_locales(String bcp47_path, String core_path, String locale_names_path, String misc_path, String numbers_path, String dates_path, CLDR& cldr)
{
    auto bcp47_iterator = TRY(path_to_dir_iterator(move(bcp47_path), "bcp47"sv));
    auto identity_iterator = TRY(path_to_dir_iterator(locale_names_path));
    auto preprocess_iterator = TRY(path_to_dir_iterator(locale_names_path));
    auto locale_names_iterator = TRY(path_to_dir_iterator(move(locale_names_path)));
    auto misc_iterator = TRY(path_to_dir_iterator(move(misc_path)));
    auto numbers_iterator = TRY(path_to_dir_iterator(move(numbers_path)));
    auto dates_iterator = TRY(path_to_dir_iterator(move(dates_path)));

    LexicalPath core_supplemental_path(core_path);
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(Core::File::is_directory(core_supplemental_path.string()));

    TRY(parse_core_aliases(core_supplemental_path.string(), cldr));
    TRY(parse_likely_subtags(core_supplemental_path.string(), cldr));

    auto remove_variants_from_path = [&](String path) -> ErrorOr<String> {
        auto parsed_locale = TRY(CanonicalLanguageID<StringIndexType>::parse(cldr.unique_strings, LexicalPath::basename(path)));

        StringBuilder builder;
        builder.append(cldr.unique_strings.get(parsed_locale.language));
        if (auto script = cldr.unique_strings.get(parsed_locale.script); !script.is_empty())
            builder.appendff("-{}", script);
        if (auto region = cldr.unique_strings.get(parsed_locale.region); !region.is_empty())
            builder.appendff("-{}", region);

        return builder.build();
    };

    while (identity_iterator.has_next()) {
        auto locale_path = TRY(next_path_from_dir_iterator(identity_iterator));
        auto language = TRY(remove_variants_from_path(locale_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_identity(locale_path, cldr, locale));
    }

    while (preprocess_iterator.has_next()) {
        auto locale_path = TRY(next_path_from_dir_iterator(preprocess_iterator));
        TRY(preprocess_languages(locale_path, cldr));
    }

    quick_sort(cldr.languages);
    quick_sort(cldr.territories);
    quick_sort(cldr.scripts);

    while (bcp47_iterator.has_next()) {
        auto bcp47_path = TRY(next_path_from_dir_iterator(bcp47_iterator));
        TRY(parse_unicode_extension_keywords(move(bcp47_path), cldr));
    }

    while (locale_names_iterator.has_next()) {
        auto locale_path = TRY(next_path_from_dir_iterator(locale_names_iterator));
        auto language = TRY(remove_variants_from_path(locale_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_locale_display_patterns(locale_path, cldr, locale));
        TRY(parse_locale_languages(locale_path, cldr, locale));
        TRY(parse_locale_territories(locale_path, cldr, locale));
        TRY(parse_locale_scripts(locale_path, cldr, locale));
        TRY(parse_locale_calendars(locale_path, cldr, locale));
    }

    while (misc_iterator.has_next()) {
        auto misc_path = TRY(next_path_from_dir_iterator(misc_iterator));
        auto language = TRY(remove_variants_from_path(misc_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_locale_list_patterns(misc_path, cldr, locale));
        TRY(parse_locale_layout(misc_path, cldr, locale));
    }

    while (numbers_iterator.has_next()) {
        auto numbers_path = TRY(next_path_from_dir_iterator(numbers_iterator));
        auto language = TRY(remove_variants_from_path(numbers_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_locale_currencies(numbers_path, cldr, locale));
        TRY(parse_number_system_keywords(numbers_path, cldr, locale));
        fill_in_collation_keywords(cldr, locale);
    }

    while (dates_iterator.has_next()) {
        auto dates_path = TRY(next_path_from_dir_iterator(dates_iterator));
        auto language = TRY(remove_variants_from_path(dates_path));

        auto& locale = cldr.locales.ensure(language);
        TRY(parse_locale_date_fields(dates_path, cldr, locale));
        TRY(parse_calendar_keywords(dates_path, cldr, locale));
    }

    TRY(parse_default_content_locales(move(core_path), cldr));
    TRY(define_aliases_without_scripts(cldr));

    return {};
}

static ErrorOr<void> generate_unicode_locale_header(Core::Stream::BufferedFile& file, CLDR& cldr)
{
    StringBuilder builder;
    SourceGenerator generator { builder };

    generator.append(R"~~~(
#pragma once

#include <AK/Types.h>

namespace Locale {
)~~~");

    auto locales = cldr.locales.keys();
    auto keywords = cldr.keywords.keys();

    generate_enum(generator, format_identifier, "Locale"sv, "None"sv, locales, cldr.locale_aliases);
    generate_enum(generator, format_identifier, "Language"sv, {}, cldr.languages);
    generate_enum(generator, format_identifier, "Territory"sv, {}, cldr.territories);
    generate_enum(generator, format_identifier, "ScriptTag"sv, {}, cldr.scripts);
    generate_enum(generator, format_identifier, "Currency"sv, {}, cldr.currencies);
    generate_enum(generator, format_identifier, "DateField"sv, {}, cldr.date_fields, cldr.date_field_aliases);
    generate_enum(generator, format_identifier, "Variant"sv, {}, cldr.variants);
    generate_enum(generator, format_identifier, "ListPatternType"sv, {}, cldr.list_pattern_types);
    generate_enum(generator, format_identifier, "CharacterOrder"sv, {}, cldr.character_orders);
    generate_enum(generator, format_identifier, "Key"sv, {}, keywords);

    for (auto& keyword : cldr.keywords) {
        auto const& keyword_name = cldr.keyword_names.find(keyword.key)->value;
        auto enum_name = String::formatted("Keyword{}", format_identifier({}, keyword_name));

        if (auto aliases = cldr.keyword_aliases.find(keyword.key); aliases != cldr.keyword_aliases.end())
            generate_enum(generator, format_identifier, enum_name, {}, keyword.value, aliases->value);
        else
            generate_enum(generator, format_identifier, enum_name, {}, keyword.value);
    }

    generator.append(R"~~~(
}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

static ErrorOr<void> generate_unicode_locale_implementation(Core::Stream::BufferedFile& file, CLDR& cldr)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("string_index_type"sv, s_string_index_type);
    generator.set("locales_size"sv, String::number(cldr.locales.size()));
    generator.set("territories_size", String::number(cldr.territories.size()));
    generator.set("variants_size", String::number(cldr.max_variant_size));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/BinarySearch.h>
#include <AK/Optional.h>
#include <AK/Span.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibLocale/DateTimeFormat.h>
#include <LibLocale/Locale.h>
#include <LibLocale/LocaleData.h>
#include <LibUnicode/CurrencyCode.h>

namespace Locale {
)~~~");

    cldr.unique_strings.generate(generator);

    generator.append(R"~~~(
struct DisplayPatternImpl {
    DisplayPattern to_display_pattern() const
    {
        DisplayPattern display_patterns {};
        display_patterns.locale_pattern = decode_string(locale_pattern);
        display_patterns.locale_separator = decode_string(locale_separator);

        return display_patterns;
    }

    @string_index_type@ locale_pattern { 0 };
    @string_index_type@ locale_separator { 0 };
};

struct Patterns {
    ListPatternType type;
    Style style;
    @string_index_type@ start { 0 };
    @string_index_type@ middle { 0 };
    @string_index_type@ end { 0 };
    @string_index_type@ pair { 0 };
};

struct TextLayout {
    CharacterOrder character_order;
};
)~~~");

    generate_available_values(generator, "get_available_calendars"sv, cldr.keywords.find("ca"sv)->value, cldr.keyword_aliases.find("ca"sv)->value,
        [](auto calendar) {
            // FIXME: Remove this filter when we support all calendars.
            return calendar.is_one_of("gregory"sv, "iso8601"sv);
        });
    generate_available_values(generator, "get_available_collation_case_orderings"sv, cldr.keywords.find("kf"sv)->value, cldr.keyword_aliases.find("kf"sv)->value);
    generate_available_values(generator, "get_available_collation_numeric_orderings"sv, cldr.keywords.find("kn"sv)->value, cldr.keyword_aliases.find("kn"sv)->value);
    generate_available_values(generator, "get_available_collation_types"sv, cldr.keywords.find("co"sv)->value, cldr.keyword_aliases.find("co"sv)->value,
        [](auto collation) {
            // FIXME: Remove this filter when we support all collation types.
            return collation == "default"sv;
        });
    generate_available_values(generator, "get_available_hour_cycles"sv, cldr.keywords.find("hc"sv)->value);
    generate_available_values(generator, "get_available_number_systems"sv, cldr.keywords.find("nu"sv)->value);
    generate_available_values(generator, "get_available_currencies"sv, cldr.currencies);

    generator.append(R"~~~(
Span<StringView const> get_available_keyword_values(StringView key)
{
    auto key_value = key_from_string(key);
    if (!key_value.has_value())
        return {};

    switch (*key_value) {
    case Key::Ca:
        return get_available_calendars();
    case Key::Co:
        return get_available_collation_types();
    case Key::Hc:
        return get_available_hour_cycles();
    case Key::Kf:
        return get_available_collation_case_orderings();
    case Key::Kn:
        return get_available_collation_numeric_orderings();
    case Key::Nu:
        return get_available_number_systems();
    }

    VERIFY_NOT_REACHED();
}
)~~~");

    cldr.unique_display_patterns.generate(generator, "DisplayPatternImpl"sv, "s_display_patterns"sv, 30);
    cldr.unique_language_lists.generate(generator, s_string_index_type, "s_language_lists"sv);
    cldr.unique_territory_lists.generate(generator, s_string_index_type, "s_territory_lists"sv);
    cldr.unique_script_lists.generate(generator, s_string_index_type, "s_script_lists"sv);
    cldr.unique_currency_lists.generate(generator, s_string_index_type, "s_currency_lists"sv);
    cldr.unique_calendar_lists.generate(generator, s_string_index_type, "s_calendar_lists"sv);
    cldr.unique_date_field_lists.generate(generator, s_string_index_type, "s_date_field_lists"sv);
    cldr.unique_keyword_lists.generate(generator, s_string_index_type, "s_keyword_lists"sv);
    cldr.unique_list_patterns.generate(generator, "Patterns"sv, "s_list_patterns"sv, 10);
    cldr.unique_list_pattern_lists.generate(generator, s_list_pattern_index_type, "s_list_pattern_lists"sv);
    cldr.unique_text_layouts.generate(generator, "TextLayout"sv, "s_text_layouts"sv, 30);

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
            generator.append(first ? " "sv : ", "sv);
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

            generator.append(first ? " "sv : ", "sv);
            generator.append(String::number(mapping));
            first = false;
        }

        generator.append(" } };");
    };

    auto locales = cldr.locales.keys();
    quick_sort(locales);

    append_mapping(locales, cldr.locales, s_display_pattern_index_type, "s_locale_display_patterns"sv, [&](auto const& locale) { return locale.display_patterns; });
    append_mapping(locales, cldr.locales, s_language_list_index_type, "s_languages"sv, [&](auto const& locale) { return locale.languages; });
    append_mapping(locales, cldr.locales, s_territory_list_index_type, "s_territories"sv, [&](auto const& locale) { return locale.territories; });
    append_mapping(locales, cldr.locales, s_script_list_index_type, "s_scripts"sv, [&](auto const& locale) { return locale.scripts; });
    append_mapping(locales, cldr.locales, s_currency_list_index_type, "s_long_currencies"sv, [&](auto const& locale) { return locale.long_currencies; });
    append_mapping(locales, cldr.locales, s_currency_list_index_type, "s_short_currencies"sv, [&](auto const& locale) { return locale.short_currencies; });
    append_mapping(locales, cldr.locales, s_currency_list_index_type, "s_narrow_currencies"sv, [&](auto const& locale) { return locale.narrow_currencies; });
    append_mapping(locales, cldr.locales, s_currency_list_index_type, "s_numeric_currencies"sv, [&](auto const& locale) { return locale.numeric_currencies; });
    append_mapping(locales, cldr.locales, s_calendar_list_index_type, "s_calendars"sv, [&](auto const& locale) { return locale.calendars; });
    append_mapping(locales, cldr.locales, s_date_field_list_index_type, "s_long_date_fields"sv, [&](auto const& locale) { return locale.long_date_fields; });
    append_mapping(locales, cldr.locales, s_date_field_list_index_type, "s_short_date_fields"sv, [&](auto const& locale) { return locale.short_date_fields; });
    append_mapping(locales, cldr.locales, s_date_field_list_index_type, "s_narrow_date_fields"sv, [&](auto const& locale) { return locale.narrow_date_fields; });
    append_mapping(locales, cldr.locales, s_keyword_list_index_type, "s_calendar_keywords"sv, [&](auto const& locale) { return locale.calendar_keywords; });
    append_mapping(locales, cldr.locales, s_keyword_list_index_type, "s_collation_case_keywords"sv, [&](auto const& locale) { return locale.collation_case_keywords; });
    append_mapping(locales, cldr.locales, s_keyword_list_index_type, "s_collation_numeric_keywords"sv, [&](auto const& locale) { return locale.collation_numeric_keywords; });
    append_mapping(locales, cldr.locales, s_keyword_list_index_type, "s_number_system_keywords"sv, [&](auto const& locale) { return locale.number_system_keywords; });
    append_mapping(locales, cldr.locales, s_list_pattern_list_index_type, "s_locale_list_patterns"sv, [&](auto const& locale) { return locale.list_patterns; });
    append_mapping(locales, cldr.locales, s_text_layout_index_type, "s_locale_text_layouts"sv, [&](auto const& locale) { return locale.text_layout; });

    generator.append(R"~~~(

struct CanonicalLanguageID {
    LanguageID to_unicode_language_id() const
    {
        LanguageID language_id {};
        language_id.variants.ensure_capacity(variants_size);

        language_id.language = decode_string(language);
        if (script != 0)
            language_id.script = decode_string(script);
        if (region != 0)
            language_id.region = decode_string(region);
        for (size_t i = 0; i < variants_size; ++i)
            language_id.variants.append(decode_string(variants[i]));

        return language_id;
    }

    bool matches_variants(Vector<String> const& other_variants) const {
        if (variants_size == 0)
            return true;
        if (other_variants.size() != variants_size)
            return false;

        for (size_t i = 0; i < variants_size; ++i) {
            if (decode_string(variants[i]) != other_variants[i])
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
            auto const& lhs_language = cldr.unique_strings.get(lhs.key.language);
            auto const& rhs_language = cldr.unique_strings.get(rhs.key.language);

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

    append_complex_mapping("complex_alias"sv, cldr.complex_mappings);
    append_complex_mapping("likely_subtags"sv, cldr.likely_subtags);

    generator.append(R"~~~(
static LanguageMapping const* resolve_likely_subtag(LanguageID const& language_id)
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
        LanguageID search_key;

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
            auto const& key_language = decode_string(map.key.language);
            auto const& key_script = decode_string(map.key.script);
            auto const& key_region  = decode_string(map.key.region);

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
    auto @enum_snake@_mapping = decode_string(@enum_snake@_string_index);

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

        ValueFromStringOptions options {};
        options.return_type = "StringView"sv;
        options.return_format = "decode_string({})"sv;

        generate_value_from_string(generator, "resolve_{}_alias"sv, s_string_index_type, enum_snake, move(hashes), options);
    };

    append_from_string("Locale"sv, "locale"sv, cldr.locales.keys(), cldr.locale_aliases);

    append_from_string("Language"sv, "language"sv, cldr.languages);
    append_mapping_search("language"sv, "language"sv, "s_languages"sv, "s_language_lists"sv);
    append_alias_search("language"sv, cldr.language_aliases);

    append_from_string("Territory"sv, "territory"sv, cldr.territories);
    append_mapping_search("territory"sv, "territory"sv, "s_territories"sv, "s_territory_lists"sv);
    append_alias_search("territory"sv, cldr.territory_aliases);

    append_from_string("ScriptTag"sv, "script_tag"sv, cldr.scripts);
    append_mapping_search("script"sv, "script_tag"sv, "s_scripts"sv, "s_script_lists"sv);
    append_alias_search("script_tag"sv, cldr.script_aliases);

    append_from_string("Currency"sv, "currency"sv, cldr.currencies);
    append_mapping_search("long_currency"sv, "currency"sv, "s_long_currencies"sv, "s_currency_lists"sv);
    append_mapping_search("short_currency"sv, "currency"sv, "s_short_currencies"sv, "s_currency_lists"sv);
    append_mapping_search("narrow_currency"sv, "currency"sv, "s_narrow_currencies"sv, "s_currency_lists"sv);
    append_mapping_search("numeric_currency"sv, "currency"sv, "s_numeric_currencies"sv, "s_currency_lists"sv);

    append_from_string("DateField"sv, "date_field"sv, cldr.date_fields, cldr.date_field_aliases);
    append_mapping_search("long_date_field"sv, "date_field"sv, "s_long_date_fields"sv, "s_date_field_lists"sv);
    append_mapping_search("short_date_field"sv, "date_field"sv, "s_short_date_fields"sv, "s_date_field_lists"sv);
    append_mapping_search("narrow_date_field"sv, "date_field"sv, "s_narrow_date_fields"sv, "s_date_field_lists"sv);

    append_from_string("Key"sv, "key"sv, cldr.keywords.keys());

    for (auto const& keyword : cldr.keywords) {
        auto const& keyword_name = cldr.keyword_names.find(keyword.key)->value;
        auto enum_name = String::formatted("Keyword{}", format_identifier({}, keyword_name));
        auto enum_snake = String::formatted("keyword_{}", keyword.key);

        if (auto aliases = cldr.keyword_aliases.find(keyword.key); aliases != cldr.keyword_aliases.end())
            append_from_string(enum_name, enum_snake, keyword.value, aliases->value);
        else
            append_from_string(enum_name, enum_snake, keyword.value);
    }

    append_mapping_search("calendar"sv, "keyword_ca"sv, "s_calendars"sv, "s_calendar_lists"sv);

    append_alias_search("variant"sv, cldr.variant_aliases);
    append_alias_search("subdivision"sv, cldr.subdivision_aliases);

    append_from_string("ListPatternType"sv, "list_pattern_type"sv, cldr.list_pattern_types);

    append_from_string("CharacterOrder"sv, "character_order"sv, cldr.character_orders);
    generate_value_to_string(generator, "{}_to_string"sv, "CharacterOrder"sv, "character_order"sv, format_identifier, cldr.character_orders);

    generator.append(R"~~~(
static Span<@string_index_type@ const> find_keyword_indices(StringView locale, StringView key)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto key_value = key_from_string(key);
    if (!key_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    size_t keywords_index = 0;

    switch (*key_value) {
    case Key::Ca:
        keywords_index = s_calendar_keywords.at(locale_index);
        break;
    case Key::Kf:
        keywords_index = s_collation_case_keywords.at(locale_index);
        break;
    case Key::Kn:
        keywords_index = s_collation_numeric_keywords.at(locale_index);
        break;
    case Key::Nu:
        keywords_index = s_number_system_keywords.at(locale_index);
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return s_keyword_lists.at(keywords_index);
}

Optional<StringView> get_preferred_keyword_value_for_locale(StringView locale, StringView key)
{
    // Hour cycle keywords are region-based rather than locale-based, so they need to be handled specially.
    // FIXME: Calendar keywords are also region-based, and will need to be handled here when we support non-Gregorian calendars:
    //        https://github.com/unicode-org/cldr-json/blob/main/cldr-json/cldr-core/supplemental/calendarPreferenceData.json
    if (key == "hc"sv) {
        auto hour_cycles = get_locale_hour_cycles(locale);
        if (hour_cycles.is_empty())
            return {};

        return hour_cycle_to_string(hour_cycles[0]);
    }

    // FIXME: Generate locale-preferred collation data when available in the CLDR.
    if (key == "co"sv) {
        auto collations = get_available_collation_types();
        if (collations.is_empty())
            return {};

        return collations[0];
    }

    auto keyword_indices = find_keyword_indices(locale, key);
    if (keyword_indices.is_empty())
        return {};

    return decode_string(keyword_indices[0]);
}

Vector<StringView> get_keywords_for_locale(StringView locale, StringView key)
{
    // Hour cycle keywords are region-based rather than locale-based, so they need to be handled specially.
    // FIXME: Calendar keywords are also region-based, and will need to be handled here when we support non-Gregorian calendars:
    //        https://github.com/unicode-org/cldr-json/blob/main/cldr-json/cldr-core/supplemental/calendarPreferenceData.json
    if (key == "hc"sv) {
        auto hour_cycles = get_locale_hour_cycles(locale);

        Vector<StringView> values;
        values.ensure_capacity(hour_cycles.size());

        for (auto hour_cycle : hour_cycles)
            values.unchecked_append(hour_cycle_to_string(hour_cycle));

        return values;
    }

    // FIXME: Generate locale-preferred collation data when available in the CLDR.
    if (key == "co"sv)
        return Vector<StringView> { get_available_collation_types() };

    auto keyword_indices = find_keyword_indices(locale, key);

    Vector<StringView> keywords;
    keywords.ensure_capacity(keyword_indices.size());

    for (auto keyword : keyword_indices)
        keywords.unchecked_append(decode_string(keyword));

    return keywords;
}

Optional<DisplayPattern> get_locale_display_patterns(StringView locale)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.
    auto display_patterns_index = s_locale_display_patterns.at(locale_index);

    auto const& display_patterns = s_display_patterns.at(display_patterns_index);
    return display_patterns.to_display_pattern();
}

Optional<ListPatterns> get_locale_list_patterns(StringView locale, StringView list_pattern_type, Style list_pattern_style)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto type_value = list_pattern_type_from_string(list_pattern_type);
    if (!type_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.

    auto list_patterns_list_index = s_locale_list_patterns.at(locale_index);
    auto const& locale_list_patterns = s_list_pattern_lists.at(list_patterns_list_index);

    for (auto list_patterns_index : locale_list_patterns) {
        auto const& list_patterns = s_list_patterns.at(list_patterns_index);

        if ((list_patterns.type == type_value) && (list_patterns.style == list_pattern_style)) {
            auto const& start = decode_string(list_patterns.start);
            auto const& middle = decode_string(list_patterns.middle);
            auto const& end = decode_string(list_patterns.end);
            auto const& pair = decode_string(list_patterns.pair);

            return ListPatterns { start, middle, end, pair };
        }
    }

    return {};
}

static Optional<TextLayout> text_layout_for_locale(StringView locale)
{
    auto locale_value = locale_from_string(locale);
    if (!locale_value.has_value())
        return {};

    auto locale_index = to_underlying(*locale_value) - 1; // Subtract 1 because 0 == Locale::None.

    auto text_layouts_index = s_locale_text_layouts.at(locale_index);
    return s_text_layouts.at(text_layouts_index);
}

Optional<CharacterOrder> character_order_for_locale(StringView locale)
{
    if (auto text_layout = text_layout_for_locale(locale); text_layout.has_value())
        return text_layout->character_order;
    return {};
}

void resolve_complex_language_aliases(LanguageID& language_id)
{
    for (auto const& map : s_complex_alias) {
        auto const& key_language = decode_string(map.key.language);
        auto const& key_script = decode_string(map.key.script);
        auto const& key_region  = decode_string(map.key.region);

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

Optional<LanguageID> add_likely_subtags(LanguageID const& language_id)
{
    // https://www.unicode.org/reports/tr35/#Likely_Subtags
    auto const* likely_subtag = resolve_likely_subtag(language_id);
    if (likely_subtag == nullptr)
        return {};

    auto maximized = language_id;

    auto const& key_script = decode_string(likely_subtag->key.script);
    auto const& key_region = decode_string(likely_subtag->key.region);

    auto const& alias_language = decode_string(likely_subtag->alias.language);
    auto const& alias_script = decode_string(likely_subtag->alias.script);
    auto const& alias_region = decode_string(likely_subtag->alias.region);

    if (maximized.language == "und"sv)
        maximized.language = alias_language;
    if (!maximized.script.has_value() || (!key_script.is_empty() && !alias_script.is_empty()))
        maximized.script = alias_script;
    if (!maximized.region.has_value() || (!key_region.is_empty() && !alias_region.is_empty()))
        maximized.region = alias_region;

    return maximized;
}

Optional<String> resolve_most_likely_territory(LanguageID const& language_id)
{
    if (auto const* likely_subtag = resolve_likely_subtag(language_id); likely_subtag != nullptr)
        return decode_string(likely_subtag->alias.region);
    return {};
}

}
)~~~");

    TRY(file.write(generator.as_string_view().bytes()));
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView generated_header_path;
    StringView generated_implementation_path;
    StringView bcp47_path;
    StringView core_path;
    StringView locale_names_path;
    StringView misc_path;
    StringView numbers_path;
    StringView dates_path;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(bcp47_path, "Path to cldr-bcp47 directory", "bcp47-path", 'b', "bcp47-path");
    args_parser.add_option(core_path, "Path to cldr-core directory", "core-path", 'r', "core-path");
    args_parser.add_option(locale_names_path, "Path to cldr-localenames directory", "locale-names-path", 'l', "locale-names-path");
    args_parser.add_option(misc_path, "Path to cldr-misc directory", "misc-path", 'm', "misc-path");
    args_parser.add_option(numbers_path, "Path to cldr-numbers directory", "numbers-path", 'n', "numbers-path");
    args_parser.add_option(dates_path, "Path to cldr-dates directory", "dates-path", 'd', "dates-path");
    args_parser.parse(arguments);

    auto generated_header_file = TRY(open_file(generated_header_path, Core::Stream::OpenMode::Write));
    auto generated_implementation_file = TRY(open_file(generated_implementation_path, Core::Stream::OpenMode::Write));

    CLDR cldr;
    TRY(parse_all_locales(bcp47_path, core_path, locale_names_path, misc_path, numbers_path, dates_path, cldr));

    TRY(generate_unicode_locale_header(*generated_header_file, cldr));
    TRY(generate_unicode_locale_implementation(*generated_implementation_file, cldr));

    return 0;
}
