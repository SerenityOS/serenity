/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/Intl/DisplayNames.h>
#include <LibJS/Runtime/Value.h>
#include <LibUnicode/Forward.h>

namespace JS::Intl {

using Fallback = Variant<Empty, bool, StringView>;

struct LocaleOptions {
    Value locale_matcher;
};

struct LocaleResult {
    String locale;
};

Optional<Unicode::LocaleID> is_structurally_valid_language_tag(StringView locale);
String canonicalize_unicode_locale_id(Unicode::LocaleID& locale);
Vector<String> canonicalize_locale_list(GlobalObject&, Value locales);
Optional<String> best_available_locale(StringView const& locale);
Vector<String> best_fit_supported_locales(Vector<String> const& requested_locales);
Vector<String> lookup_supported_locales(Vector<String> const& requested_locales);
Array* supported_locales(GlobalObject&, Vector<String> const& requested_locales, Value options);
Object* coerce_options_to_object(GlobalObject& global_object, Value options);
Value get_option(GlobalObject& global_object, Value options, PropertyName const& property, Value::Type type, Vector<StringView> const& values, Fallback fallback);
String insert_unicode_extension_and_canonicalize(Unicode::LocaleID locale_id, Unicode::LocaleExtension extension);
LocaleResult resolve_locale(Vector<String> const& requested_locales, LocaleOptions const& options, Vector<StringView> relevant_extension_keys);
Value canonical_code_for_display_names(GlobalObject&, DisplayNames::Type type, StringView code);

}
