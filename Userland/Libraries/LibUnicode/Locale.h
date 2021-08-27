/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibUnicode/Forward.h>

namespace Unicode {

struct LanguageID {
    bool is_root { false };
    Optional<StringView> language {};
    Optional<StringView> script {};
    Optional<StringView> region {};
    Vector<StringView> variants {};
};

struct Keyword {
    StringView key {};
    Vector<StringView> types {};
};

struct LocaleExtension {
    Vector<StringView> attributes {};
    Vector<Keyword> keywords {};
};

struct TransformedField {
    StringView key;
    Vector<StringView> values {};
};

struct TransformedExtension {
    Optional<LanguageID> language {};
    Vector<TransformedField> fields {};
};

struct OtherExtension {
    char key {};
    Vector<StringView> values {};
};

using Extension = Variant<LocaleExtension, TransformedExtension, OtherExtension>;

struct LocaleID {
    LanguageID language_id {};
    Vector<Extension> extensions {};
};

// Note: These methods only verify that the provided strings match the EBNF grammar of the
// Unicode identifier subtag (i.e. no validation is done that the tags actually exist).
bool is_unicode_language_subtag(StringView);
bool is_unicode_script_subtag(StringView);
bool is_unicode_region_subtag(StringView);
bool is_unicode_variant_subtag(StringView);

Optional<LanguageID> parse_unicode_language_id(StringView);
Optional<LocaleID> parse_unicode_locale_id(StringView);
Optional<String> canonicalize_unicode_locale_id(LocaleID&);

String const& default_locale();
bool is_locale_available(StringView locale);

Optional<StringView> get_locale_language_mapping(StringView locale, StringView language);
Optional<StringView> get_locale_territory_mapping(StringView locale, StringView territory);
Optional<StringView> get_locale_script_mapping(StringView locale, StringView script);
Optional<StringView> get_locale_currency_mapping(StringView locale, StringView currency);

}
