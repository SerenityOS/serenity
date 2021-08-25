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

Optional<LanguageID> parse_unicode_language_id(StringView language)
{
    // https://unicode.org/reports/tr35/#Unicode_language_identifier
    //
    // unicode_language_id = "root"
    //     OR
    // unicode_language_id = ((unicode_language_subtag (sep unicode_script_subtag)?) | unicode_script_subtag)
    //                       (sep unicode_region_subtag)?
    //                       (sep unicode_variant_subtag)*
    LanguageID language_id {};

    if (language == "root"sv) {
        language_id.is_root = true;
        return language_id;
    }

    auto segments = language.split_view_if(is_any_of("-_"sv), true); // keep_empty=true to ensure valid data follows a separator.
    size_t index = 0;

    if (segments.size() == index)
        return {};

    if (is_unicode_language_subtag(segments[index])) {
        language_id.language = segments[index];
        if (segments.size() == ++index)
            return language_id;
    }

    if (is_unicode_script_subtag(segments[index])) {
        language_id.script = segments[index];
        if (segments.size() == ++index)
            return language_id;
    } else if (!language_id.language.has_value()) {
        return {};
    }

    if (is_unicode_region_subtag(segments[index])) {
        language_id.region = segments[index];
        if (segments.size() == ++index)
            return language_id;
    }

    while (index < segments.size()) {
        if (!is_unicode_variant_subtag(segments[index]))
            return {};
        language_id.variants.append(segments[index++]);
    }

    return language_id;
}

Optional<LocaleID> parse_unicode_locale_id(StringView locale)
{
    LocaleID locale_id {};

    // https://unicode.org/reports/tr35/#Unicode_locale_identifier
    //
    // unicode_locale_id = unicode_language_id
    //                     extensions*
    //                     pu_extensions?
    auto language_id = parse_unicode_language_id(locale);
    if (!language_id.has_value())
        return {};

    // FIXME: Handle extensions and pu_extensions.
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
    static auto const& available_locales = Detail::available_locales();
    return available_locales.contains(locale);
#else
    return false;
#endif
}

Optional<StringView> get_locale_territory_mapping([[maybe_unused]] StringView locale, [[maybe_unused]] StringView code)
{
#if ENABLE_UNICODE_DATA
    static auto const& available_locales = Detail::available_locales();

    auto it = available_locales.find(locale);
    if (it == available_locales.end())
        return {};

    if (auto territory = Detail::territory_from_string(code); territory.has_value())
        return it->value.territories[to_underlying(*territory)];
#endif

    return {};
}

}
