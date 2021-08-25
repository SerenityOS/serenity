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

namespace JS::Intl {

using Fallback = Variant<Empty, bool, StringView>;

struct LocaleOptions {
    Value locale_matcher;
};

struct LocaleResult {
    String locale;
};

Vector<String> canonicalize_locale_list(GlobalObject&, Value locales);
Value get_option(GlobalObject& global_object, Value options, PropertyName const& property, Value::Type type, Vector<StringView> const& values, Fallback fallback);
LocaleResult resolve_locale(Vector<String> const& requested_locales, LocaleOptions const& options, Vector<StringView> relevant_extension_keys);

}
