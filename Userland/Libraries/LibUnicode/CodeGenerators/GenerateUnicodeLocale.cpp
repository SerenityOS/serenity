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

struct Locale {
    String language;
    Optional<String> territory;
    Optional<String> variant;
    HashMap<String, String> languages;
    HashMap<String, String> territories;
    HashMap<String, String> scripts;
};

struct UnicodeLocaleData {
    HashMap<String, Locale> locales;
    Vector<String> languages;
    Vector<String> territories;
    Vector<String> scripts;
    Vector<String> variants;
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

static void parse_all_locales(String locale_names_path, UnicodeLocaleData& locale_data)
{
    LexicalPath locale_names(move(locale_names_path));
    locale_names = locale_names.append("main"sv);
    VERIFY(Core::File::is_directory(locale_names.string()));

    Core::DirIterator iterator(locale_names.string(), Core::DirIterator::SkipParentAndBaseDir);
    if (iterator.has_error()) {
        warnln("{}: {}", locale_names.string(), iterator.error_string());
        VERIFY_NOT_REACHED();
    }

    while (iterator.has_next()) {
        auto locale_path = iterator.next_full_path();
        VERIFY(Core::File::is_directory(locale_path));

        auto& locale = locale_data.locales.ensure(LexicalPath::basename(locale_path));
        parse_identity(locale_path, locale_data, locale);
        parse_locale_languages(locale_path, locale);
        parse_locale_territories(locale_path, locale);
        parse_locale_scripts(locale_path, locale_data, locale);
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
    generate_enum("Variant"sv, {}, locale_data.variants);

    generator.append(R"~~~(
namespace Detail {

Optional<Locale> locale_from_string(StringView const& locale);

Optional<StringView> get_locale_language_mapping(StringView locale, StringView language);
Optional<Language> language_from_string(StringView const& language);

Optional<StringView> get_locale_territory_mapping(StringView locale, StringView territory);
Optional<Territory> territory_from_string(StringView const& territory);

Optional<StringView> get_locale_script_tag_mapping(StringView locale, StringView script_tag);
Optional<ScriptTag> script_tag_from_string(StringView const& script_tag);

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

    generator.append(R"~~~(
#include <AK/Array.h>
#include <AK/HashMap.h>
#include <AK/Span.h>
#include <LibUnicode/UnicodeLocale.h>

namespace Unicode {
)~~~");

    auto format_mapping_name = [](StringView format, StringView name) {
        auto mapping_name = name.to_lowercase_string();
        mapping_name.replace("-"sv, "_"sv, true);
        return String::formatted(format, mapping_name);
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

    generator.append(R"~~~(
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

    append_from_string("Locale"sv, "locale"sv, locale_data.locales.keys());

    append_mapping_search("Language"sv, "language"sv, "s_languages"sv);
    append_from_string("Language"sv, "language"sv, locale_data.languages);

    append_mapping_search("Territory"sv, "territory"sv, "s_territories"sv);
    append_from_string("Territory"sv, "territory"sv, locale_data.territories);

    append_mapping_search("ScriptTag"sv, "script_tag"sv, "s_scripts"sv);
    append_from_string("ScriptTag"sv, "script_tag"sv, locale_data.scripts);

    generator.append(R"~~~(
}

}
)~~~");

    write_to_file_if_different(file, generator.as_string_view());
}

int main(int argc, char** argv)
{
    char const* generated_header_path = nullptr;
    char const* generated_implementation_path = nullptr;
    char const* locale_names_path = nullptr;
    char const* numbers_path = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_option(generated_header_path, "Path to the Unicode locale header file to generate", "generated-header-path", 'h', "generated-header-path");
    args_parser.add_option(generated_implementation_path, "Path to the Unicode locale implementation file to generate", "generated-implementation-path", 'c', "generated-implementation-path");
    args_parser.add_option(locale_names_path, "Path to cldr-localenames directory", "locale-names-path", 'l', "locale-names-path");
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
    parse_all_locales(locale_names_path, locale_data);

    generate_unicode_locale_header(generated_header_file, locale_data);
    generate_unicode_locale_implementation(generated_implementation_file, locale_data);

    return 0;
}
