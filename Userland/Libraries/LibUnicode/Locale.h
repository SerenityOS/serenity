/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/CharacterTypes.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

struct LanguageID {
    String to_string() const;
    bool operator==(LanguageID const&) const = default;

    bool is_root { false };
    Optional<String> language {};
    Optional<String> script {};
    Optional<String> region {};
    Vector<String> variants {};
};

struct Keyword {
    String key {};
    String value {};
};

struct LocaleExtension {
    Vector<String> attributes {};
    Vector<Keyword> keywords {};
};

struct TransformedField {
    String key {};
    String value {};
};

struct TransformedExtension {
    Optional<LanguageID> language {};
    Vector<TransformedField> fields {};
};

struct OtherExtension {
    char key {};
    String value {};
};

using Extension = Variant<LocaleExtension, TransformedExtension, OtherExtension>;

struct LocaleID {
    String to_string() const;

    template<typename ExtensionType>
    Vector<Extension> remove_extension_type()
    {
        Vector<Extension> removed_extensions {};
        auto tmp_extensions = move(extensions);

        for (auto& extension : tmp_extensions) {
            if (extension.has<ExtensionType>())
                removed_extensions.append(move(extension));
            else
                extensions.append(move(extension));
        }

        return removed_extensions;
    }

    LanguageID language_id {};
    Vector<Extension> extensions {};
    Vector<String> private_use_extensions {};
};

enum class Style : u8 {
    Long,
    Short,
    Narrow,
    Numeric,
};

struct ListPatterns {
    StringView start;
    StringView middle;
    StringView end;
    StringView pair;
};

// Note: These methods only verify that the provided strings match the EBNF grammar of the
// Unicode identifier subtag (i.e. no validation is done that the tags actually exist).
constexpr bool is_unicode_language_subtag(StringView subtag)
{
    // unicode_language_subtag = alpha{2,3} | alpha{5,8}
    if ((subtag.length() < 2) || (subtag.length() == 4) || (subtag.length() > 8))
        return false;
    return all_of(subtag, is_ascii_alpha);
}

constexpr bool is_unicode_script_subtag(StringView subtag)
{
    // unicode_script_subtag = alpha{4}
    if (subtag.length() != 4)
        return false;
    return all_of(subtag, is_ascii_alpha);
}

constexpr bool is_unicode_region_subtag(StringView subtag)
{
    // unicode_region_subtag = (alpha{2} | digit{3})
    if (subtag.length() == 2)
        return all_of(subtag, is_ascii_alpha);
    if (subtag.length() == 3)
        return all_of(subtag, is_ascii_digit);
    return false;
}

constexpr bool is_unicode_variant_subtag(StringView subtag)
{
    // unicode_variant_subtag = (alphanum{5,8} | digit alphanum{3})
    if ((subtag.length() >= 5) && (subtag.length() <= 8))
        return all_of(subtag, is_ascii_alphanumeric);
    if (subtag.length() == 4)
        return is_ascii_digit(subtag[0]) && all_of(subtag.substring_view(1), is_ascii_alphanumeric);
    return false;
}

bool is_type_identifier(StringView);

Optional<LanguageID> parse_unicode_language_id(StringView);
Optional<LocaleID> parse_unicode_locale_id(StringView);

void canonicalize_unicode_extension_values(StringView key, String& value, bool remove_true);
Optional<String> canonicalize_unicode_locale_id(LocaleID&);

String const& default_locale();
bool is_locale_available(StringView locale);
Optional<Locale> locale_from_string(StringView locale);

Optional<StringView> get_locale_language_mapping(StringView locale, StringView language);
Optional<StringView> get_locale_territory_mapping(StringView locale, StringView territory);
Optional<StringView> get_locale_script_mapping(StringView locale, StringView script);
Optional<StringView> get_locale_currency_mapping(StringView locale, StringView currency, Style style);
Vector<StringView> get_locale_key_mapping(StringView locale, StringView keyword);
Optional<StringView> get_number_system_symbol(StringView locale, StringView system, StringView symbol);
Optional<ListPatterns> get_locale_list_patterns(StringView locale, StringView type, StringView style);

Optional<StringView> resolve_language_alias(StringView language);
Optional<StringView> resolve_territory_alias(StringView territory);
Optional<StringView> resolve_script_tag_alias(StringView script_tag);
Optional<StringView> resolve_variant_alias(StringView variant);
Optional<StringView> resolve_subdivision_alias(StringView subdivision);

Optional<LanguageID> add_likely_subtags(LanguageID const& language_id);
Optional<LanguageID> remove_likely_subtags(LanguageID const& language_id);
String resolve_most_likely_territory(LanguageID const& language_id, StringView territory_alias);

}
