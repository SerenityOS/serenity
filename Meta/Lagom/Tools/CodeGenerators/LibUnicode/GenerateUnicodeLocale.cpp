/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

struct Locale {
    String language;
    Optional<String> territory;
    Optional<String> variant;
    HashMap<String, String> languages;
    HashMap<String, String> territories;
    HashMap<String, String> scripts;
    HashMap<String, String> currencies;
};

struct CanonicalLanguageID {
    String language {};
    String script {};
    String region {};
    Vector<String> variants {};
};

struct LanguageMapping {
    CanonicalLanguageID key {};
    CanonicalLanguageID alias {};
};

struct UnicodeLocaleData {
    HashMap<String, Locale> locales;
    Vector<String> languages;
    Vector<String> territories;
    Vector<String> scripts;
    Vector<String> variants;
    Vector<String> currencies;
    HashMap<String, String> language_aliases;
    HashMap<String, String> territory_aliases;
    HashMap<String, String> script_aliases;
    HashMap<String, String> variant_aliases;
    HashMap<String, String> subdivision_aliases;
    Vector<LanguageMapping> complex_mappings;
    Vector<LanguageMapping> likely_subtags;
    size_t max_variant_size { 0 };
};

static void write_to_file_if_different(Core::File& file, StringView contents)
{
    auto const current_contents = file.read_all();

    if (StringView { current_contents.bytes() } == contents)
        return;

    VERIFY(file.seek(0));
    VERIFY(file.truncate(0));
    VERIFY(file.write(contents));
}

static Optional<CanonicalLanguageID> parse_language(StringView language)
{
    CanonicalLanguageID language_id {};

    auto segments = language.split_view('-');
    VERIFY(!segments.is_empty());
    size_t index = 0;

    if (Unicode::is_unicode_language_subtag(segments[index])) {
        language_id.language = segments[index];
        if (segments.size() == ++index)
            return language_id;
    } else {
        return {};
    }

    if (Unicode::is_unicode_script_subtag(segments[index])) {
        language_id.script = segments[index];
        if (segments.size() == ++index)
            return language_id;
    }

    if (Unicode::is_unicode_region_subtag(segments[index])) {
        language_id.region = segments[index];
        if (segments.size() == ++index)
            return language_id;
    }

    while (index < segments.size()) {
        if (!Unicode::is_unicode_variant_subtag(segments[index]))
            return {};
        language_id.variants.append(segments[index++]);
    }

    return language_id;
}

static Optional<LanguageMapping> parse_language_mapping(StringView key, StringView alias)
{
    auto parsed_key = parse_language(key);
    if (!parsed_key.has_value())
        return {};

    auto parsed_alias = parse_language(alias);
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
                auto mapping = parse_language_mapping(key, alias);
                if (!mapping.has_value())
                    return;

                locale_data.max_variant_size = max(mapping->key.variants.size(), locale_data.max_variant_size);
                locale_data.max_variant_size = max(mapping->alias.variants.size(), locale_data.max_variant_size);
                locale_data.complex_mappings.append(mapping.release_value());
            } else {
                alias_map.set(key, move(alias));
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
        auto mapping = parse_language_mapping(key, value.as_string());
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

static void parse_locale_languages(String locale_path, Locale& locale)
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
        locale.languages.set(key, value.as_string());
    });
}

static void parse_locale_territories(String locale_path, Locale& locale)
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
        locale.territories.set(key, value.as_string());
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
        locale.scripts.set(key, value.as_string());
        if (!locale_data.scripts.contains_slow(key))
            locale_data.scripts.append(key);
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
        locale.currencies.set(key, display_name.as_string());
        if (!locale_data.currencies.contains_slow(key))
            locale_data.currencies.append(key);
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

static void parse_all_locales(String core_path, String locale_names_path, String numbers_path, UnicodeLocaleData& locale_data)
{
    auto locale_names_iterator = path_to_dir_iterator(move(locale_names_path));
    auto numbers_iterator = path_to_dir_iterator(move(numbers_path));

    LexicalPath core_supplemental_path(move(core_path));
    core_supplemental_path = core_supplemental_path.append("supplemental"sv);
    VERIFY(Core::File::is_directory(core_supplemental_path.string()));

    parse_core_aliases(core_supplemental_path.string(), locale_data);
    parse_likely_subtags(core_supplemental_path.string(), locale_data);

    while (locale_names_iterator.has_next()) {
        auto locale_path = locale_names_iterator.next_full_path();
        VERIFY(Core::File::is_directory(locale_path));

        auto& locale = locale_data.locales.ensure(LexicalPath::basename(locale_path));
        parse_identity(locale_path, locale_data, locale);
        parse_locale_languages(locale_path, locale);
        parse_locale_territories(locale_path, locale);
        parse_locale_scripts(locale_path, locale_data, locale);
    }

    while (numbers_iterator.has_next()) {
        auto numbers_path = numbers_iterator.next_full_path();
        VERIFY(Core::File::is_directory(numbers_path));

        auto& locale = locale_data.locales.ensure(LexicalPath::basename(numbers_path));
        parse_locale_currencies(numbers_path, locale_data, locale);
    }
}

static String format_identifier(StringView owner, String identifier)
{
    identifier.replace("-"sv, "_"sv, true);

    if (all_of(identifier, is_ascii_digit))
        return String::formatted("{}_{}", owner[0], identifier);
    return identifier.to_titlecase();
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
    generate_enum("Variant"sv, {}, locale_data.variants);

    generator.append(R"~~~(
namespace Detail {

Optional<Locale> locale_from_string(StringView const& locale);

Optional<StringView> get_locale_language_mapping(StringView locale, StringView language);
Optional<Language> language_from_string(StringView const& language);
Optional<StringView> resolve_language_alias(StringView const& language);

Optional<StringView> get_locale_territory_mapping(StringView locale, StringView territory);
Optional<Territory> territory_from_string(StringView const& territory);
Optional<StringView> resolve_territory_alias(StringView const& territory);

Optional<StringView> get_locale_script_tag_mapping(StringView locale, StringView script_tag);
Optional<ScriptTag> script_tag_from_string(StringView const& script_tag);
Optional<StringView> resolve_script_tag_alias(StringView const& script_tag);

Optional<StringView> get_locale_currency_mapping(StringView locale, StringView currency);
Optional<Currency> currency_from_string(StringView const& currency);

Optional<StringView> resolve_variant_alias(StringView const& variant);
Optional<StringView> resolve_subdivision_alias(StringView const& subdivision);

void resolve_complex_language_aliases(Unicode::LanguageID& language_id);

Optional<Unicode::LanguageID> add_likely_subtags(Unicode::LanguageID const& language_id);
Optional<String> resolve_most_likely_territory(Unicode::LanguageID const& language_id);

}

}
)~~~");

    write_to_file_if_different(file, generator.as_string_view());
}

static void generate_unicode_locale_implementation(Core::File& file, UnicodeLocaleData& locale_data)
{
    StringBuilder builder;
    SourceGenerator generator { builder };
    generator.set("locales_size"sv, String::number(locale_data.locales.size()));
    generator.set("territories_size", String::number(locale_data.territories.size()));
    generator.set("variants_size", String::number(locale_data.max_variant_size));

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/HashMap.h>
#include <AK/Span.h>
#include <LibUnicode/Locale.h>
#include <LibUnicode/UnicodeLocale.h>

namespace Unicode {
)~~~");

    auto format_mapping_name = [](StringView format, StringView name) {
        auto mapping_name = name.to_lowercase_string();
        mapping_name.replace("-"sv, "_"sv, true);
        return String::formatted(format, mapping_name);
    };

    auto append_string = [&](StringView value) {
        if (value.is_empty())
            generator.append(", {}"sv);
        else
            generator.append(String::formatted(", \"{}\"sv", value));
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
            generator.append(String::formatted("\"{}\"sv", item));
            first = false;
        }
        generator.append(String::formatted(" }}, {}", list.size()));
    };

    auto append_mapping_list = [&](String name, auto const& keys, auto const& mappings) {
        generator.set("name", name);
        generator.set("size", String::number(keys.size()));

        generator.append(R"~~~(
static constexpr Array<StringView, @size@> @name@ { {
    )~~~");

        constexpr size_t max_values_per_row = 10;
        size_t values_in_current_row = 0;

        for (auto const& key : keys) {
            if (values_in_current_row++ > 0)
                generator.append(" ");

            if (auto it = mappings.find(key); it != mappings.end())
                generator.set("mapping"sv, String::formatted("\"{}\"sv", it->value));
            else
                generator.set("mapping"sv, "{}"sv);
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

    auto append_mapping = [&](StringView name, StringView format, auto const& keys, auto get_mapping_callback) {
        Vector<String> mapping_names;

        for (auto const& locale : locale_data.locales) {
            auto mapping_name = format_mapping_name(format, locale.key);
            append_mapping_list(mapping_name, keys, get_mapping_callback(locale.value));
            mapping_names.append(move(mapping_name));
        }

        quick_sort(mapping_names);

        generator.set("name", name);
        generator.set("size", String::number(locale_data.locales.size()));
        generator.append(R"~~~(
static constexpr Array<Span<StringView const>, @size@> @name@ { {
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

    append_mapping("s_languages"sv, "s_languages_{}", locale_data.languages, [](auto const& value) { return value.languages; });
    append_mapping("s_territories"sv, "s_territories_{}", locale_data.territories, [](auto const& value) { return value.territories; });
    append_mapping("s_scripts"sv, "s_scripts_{}", locale_data.scripts, [](auto const& value) { return value.scripts; });
    append_mapping("s_currencies"sv, "s_currencies_{}", locale_data.currencies, [](auto const& value) { return value.currencies; });

    generator.append(R"~~~(
struct CanonicalLanguageID {
    Unicode::LanguageID to_unicode_language_id() const
    {
        Unicode::LanguageID language_id {};
        language_id.variants.ensure_capacity(variants_size);

        language_id.language = language.to_string();
        if (!script.is_empty())
            language_id.script = script.to_string();
        if (!region.is_empty())
            language_id.region = region.to_string();
        for (size_t i = 0; i < variants_size; ++i)
            language_id.variants.append(variants[i].to_string());

        return language_id;
    }

    bool matches_variants(Vector<String> const& other_variants) const {
        if (variants_size == 0)
            return true;
        if (other_variants.size() != variants_size)
            return false;

        for (size_t i = 0; i < variants_size; ++i) {
            if (variants[i] != other_variants[i])
                return false;
        }

        return true;
    };

    StringView language {};
    StringView script {};
    StringView region {};
    Array<StringView, @variants_size@> variants {};
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

        quick_sort(mappings, [](auto const& lhs, auto const& rhs) {
            auto const& lhs_language = lhs.key.language;
            auto const& rhs_language = rhs.key.language;

            // Sort the keys such that "und" language tags are at the end, as those are less specific.
            if (lhs_language.starts_with("und"sv) && !rhs_language.starts_with("und"sv))
                return false;
            if (!lhs_language.starts_with("und"sv) && rhs_language.starts_with("und"sv))
                return true;
            return lhs_language < rhs_language;
        });

        for (auto const& mapping : mappings) {
            generator.set("language"sv, mapping.key.language);
            generator.append("    { { \"@language@\"sv");

            append_string(mapping.key.script);
            append_string(mapping.key.region);
            append_list_and_size(mapping.key.variants);

            generator.set("language"sv, mapping.alias.language);
            generator.append(" }, { \"@language@\"sv");

            append_string(mapping.alias.script);
            append_string(mapping.alias.region);
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
        CanonicalLanguageID search_key;

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
            if (map.key.language != search_key.language)
                continue;
            if (map.key.script != search_key.script)
                continue;
            if (map.key.region != search_key.region)
                continue;

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
    auto @enum_snake@_mapping = mappings.at(@enum_snake@_index);

    if (@enum_snake@_mapping.is_empty())
        return {};
    return @enum_snake@_mapping;
}
)~~~");
    };

    auto append_from_string = [&](StringView enum_title, StringView enum_snake, Vector<String> const& values) {
        generator.set("enum_title", enum_title);
        generator.set("enum_snake", enum_snake);

        generator.append(R"~~~(
Optional<@enum_title@> @enum_snake@_from_string(StringView const& @enum_snake@)
{
    static HashMap<StringView, @enum_title@> @enum_snake@_values { {)~~~");

        for (auto const& value : values) {
            generator.set("key"sv, value);
            generator.set("value"sv, format_identifier(enum_title, value));

            generator.append(R"~~~(
        { "@key@"sv, @enum_title@::@value@ },)~~~");
        }

        generator.append(R"~~~(
    } };

    if (auto value = @enum_snake@_values.get(@enum_snake@); value.has_value())
        return value.value();
    return {};
}
)~~~");
    };

    auto append_alias_search = [&](StringView enum_snake, HashMap<String, String> const& aliases) {
        generator.set("enum_snake", enum_snake);

        generator.append(R"~~~(
Optional<StringView> resolve_@enum_snake@_alias(StringView const& @enum_snake@)
{
    static HashMap<StringView, StringView> @enum_snake@_aliases { {
        )~~~");

        constexpr size_t max_values_per_row = 10;
        size_t values_in_current_row = 0;

        for (auto const& alias : aliases) {
            if (values_in_current_row++ > 0)
                generator.append(" ");

            generator.set("key"sv, alias.key);
            generator.set("alias"sv, alias.value);
            generator.append("{ \"@key@\"sv, \"@alias@\"sv },");

            if (values_in_current_row == max_values_per_row) {
                generator.append("\n        ");
                values_in_current_row = 0;
            }
        }

        generator.append(R"~~~(
    } };

    if (auto alias = @enum_snake@_aliases.get(@enum_snake@); alias.has_value())
        return alias.value();
    return {};
}
)~~~");
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

    append_alias_search("variant"sv, locale_data.variant_aliases);
    append_alias_search("subdivision"sv, locale_data.subdivision_aliases);

    generator.append(R"~~~(
void resolve_complex_language_aliases(Unicode::LanguageID& language_id)
{
    for (auto const& map : s_complex_alias) {
        if ((map.key.language != language_id.language) && (map.key.language != "und"sv))
            continue;
        if (!map.key.script.is_empty() && (map.key.script != language_id.script))
            continue;
        if (!map.key.region.is_empty() && (map.key.region != language_id.region))
            continue;
        if (!map.key.matches_variants(language_id.variants))
            continue;

        auto alias = map.alias.to_unicode_language_id();

        if (alias.language == "und"sv)
            alias.language = move(language_id.language);
        if (map.key.script.is_empty() && !alias.script.has_value())
            alias.script = move(language_id.script);
        if (map.key.region.is_empty() && !alias.region.has_value())
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
    auto const& key = likely_subtag->key;
    auto const& alias = likely_subtag->alias;

    if (maximized.language == "und"sv)
        maximized.language = alias.language;
    if (!maximized.script.has_value() || (!key.script.is_empty() && !alias.script.is_empty()))
        maximized.script = alias.script;
    if (!maximized.region.has_value() || (!key.region.is_empty() && !alias.region.is_empty()))
        maximized.region = alias.region;

    return maximized;
}

Optional<String> resolve_most_likely_territory(Unicode::LanguageID const& language_id)
{
    if (auto const* likely_subtag = resolve_likely_subtag(language_id); likely_subtag != nullptr)
        return likely_subtag->alias.region;
    return {};
}

}

}
)~~~");

    write_to_file_if_different(file, generator.as_string_view());
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
    parse_all_locales(core_path, locale_names_path, numbers_path, locale_data);

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
