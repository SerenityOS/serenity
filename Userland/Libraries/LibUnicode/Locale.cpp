/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AllOf.h>
#include <AK/CharacterTypes.h>
#include <AK/GenericLexer.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <LibUnicode/Locale.h>

#if ENABLE_UNICODE_DATA
#    include <LibUnicode/UnicodeLocale.h>
#endif

namespace Unicode {

bool is_unicode_language_subtag(StringView subtag)
{
    // unicode_language_subtag = alpha{2,3} | alpha{5,8}
    if ((subtag.length() < 2) || (subtag.length() == 4) || (subtag.length() > 8))
        return false;
    return all_of(subtag, is_ascii_alpha);
}

bool is_unicode_script_subtag(StringView subtag)
{
    // unicode_script_subtag = alpha{4}
    if (subtag.length() != 4)
        return false;
    return all_of(subtag, is_ascii_alpha);
}

bool is_unicode_region_subtag(StringView subtag)
{
    // unicode_region_subtag = (alpha{2} | digit{3})
    if (subtag.length() == 2)
        return all_of(subtag, is_ascii_alpha);
    if (subtag.length() == 3)
        return all_of(subtag, is_ascii_digit);
    return false;
}

bool is_unicode_variant_subtag(StringView subtag)
{
    // unicode_variant_subtag = (alphanum{5,8} | digit alphanum{3})
    if ((subtag.length() >= 5) && (subtag.length() <= 8))
        return all_of(subtag, is_ascii_alphanumeric);
    if (subtag.length() == 4)
        return is_ascii_digit(subtag[0]) && all_of(subtag.substring_view(1), is_ascii_alphanumeric);
    return false;
}

static Optional<StringView> consume_next_segment(GenericLexer& lexer, bool with_separator)
{
    constexpr auto is_separator = is_any_of("-_"sv);

    if (with_separator) {
        if (!lexer.next_is(is_separator))
            return {};
        lexer.ignore();
    }

    auto segment = lexer.consume_until(is_separator);
    if (segment.is_empty()) {
        lexer.retreat(with_separator);
        return {};
    }

    return segment;
}

static Optional<LanguageID> parse_unicode_language_id(GenericLexer& lexer)
{
    // https://unicode.org/reports/tr35/#Unicode_language_identifier
    //
    // unicode_language_id = "root"
    //     OR
    // unicode_language_id = ((unicode_language_subtag (sep unicode_script_subtag)?) | unicode_script_subtag)
    //                       (sep unicode_region_subtag)?
    //                       (sep unicode_variant_subtag)*
    LanguageID language_id {};

    if (lexer.consume_specific("root"sv)) {
        language_id.is_root = true;
        return language_id;
    }

    enum class ParseState {
        ParsingLanguageOrScript,
        ParsingScript,
        ParsingRegion,
        ParsingVariant,
        Done,
    };

    auto state = ParseState::ParsingLanguageOrScript;

    while (!lexer.is_eof() && (state != ParseState::Done)) {
        auto segment = consume_next_segment(lexer, state != ParseState::ParsingLanguageOrScript);
        if (!segment.has_value())
            return {};

        switch (state) {
        case ParseState::ParsingLanguageOrScript:
            if (is_unicode_language_subtag(*segment)) {
                state = ParseState::ParsingScript;
                language_id.language = *segment;
            } else if (is_unicode_script_subtag(*segment)) {
                state = ParseState::ParsingRegion;
                language_id.script = *segment;
            } else {
                return {};
            }
            break;

        case ParseState::ParsingScript:
            if (is_unicode_script_subtag(*segment)) {
                state = ParseState::ParsingRegion;
                language_id.script = *segment;
                break;
            }

            state = ParseState::ParsingRegion;
            [[fallthrough]];

        case ParseState::ParsingRegion:
            if (is_unicode_region_subtag(*segment)) {
                state = ParseState::ParsingVariant;
                language_id.region = *segment;
                break;
            }

            state = ParseState::ParsingVariant;
            [[fallthrough]];

        case ParseState::ParsingVariant:
            if (is_unicode_variant_subtag(*segment)) {
                language_id.variants.append(*segment);
            } else {
                lexer.retreat(segment->length() + 1);
                state = ParseState::Done;
            }
            break;

        default:
            VERIFY_NOT_REACHED();
        }
    }

    return language_id;
}

Optional<LanguageID> parse_unicode_language_id(StringView language)
{
    GenericLexer lexer { language };

    auto language_id = parse_unicode_language_id(lexer);
    if (!lexer.is_eof())
        return {};

    return language_id;
}

Optional<LocaleID> parse_unicode_locale_id(StringView locale)
{
    GenericLexer lexer { locale };
    LocaleID locale_id {};

    // https://unicode.org/reports/tr35/#Unicode_locale_identifier
    //
    // unicode_locale_id = unicode_language_id
    //                     extensions*
    //                     pu_extensions?
    auto language_id = parse_unicode_language_id(lexer);
    if (!language_id.has_value())
        return {};

    // FIXME: Handle extensions and pu_extensions.

    if (!lexer.is_eof())
        return {};

    return LocaleID { language_id.release_value() };
}

Optional<String> canonicalize_unicode_locale_id(LocaleID& locale_id)
{
    // https://unicode.org/reports/tr35/#Canonical_Unicode_Locale_Identifiers
    StringBuilder builder;

    if (!locale_id.language_id.language.has_value())
        return {};

    builder.append(locale_id.language_id.language->to_lowercase_string());

    if (locale_id.language_id.script.has_value()) {
        builder.append('-');
        builder.append(locale_id.language_id.script->to_titlecase_string());
    }

    if (locale_id.language_id.region.has_value()) {
        builder.append('-');
        builder.append(locale_id.language_id.region->to_uppercase_string());
    }

    quick_sort(locale_id.language_id.variants);

    for (auto const& variant : locale_id.language_id.variants) {
        builder.append('-');
        builder.append(variant.to_lowercase_string());
    }

    // FIXME: Handle extensions and pu_extensions.

    return builder.build();
}

String const& default_locale()
{
    static String locale = "en"sv;
    return locale;
}

bool is_locale_available([[maybe_unused]] StringView locale)
{
#if ENABLE_UNICODE_DATA
    return Detail::locale_from_string(locale).has_value();
#else
    return false;
#endif
}

Optional<StringView> get_locale_language_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView language)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_language_mapping(locale, language);
#else
    return {};
#endif
}

Optional<StringView> get_locale_territory_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView territory)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_territory_mapping(locale, territory);
#else
    return {};
#endif
}

Optional<StringView> get_locale_script_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView script)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_script_tag_mapping(locale, script);
#else
    return {};
#endif
}

Optional<StringView> get_locale_currency_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView currency)
{
#if ENABLE_UNICODE_DATA
    return Detail::get_locale_currency_mapping(locale, currency);
#else
    return {};
#endif
}

}
