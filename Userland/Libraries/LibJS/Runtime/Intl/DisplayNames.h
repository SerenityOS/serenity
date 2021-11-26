/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class DisplayNames final : public Object {
    JS_OBJECT(DisplayNames, Object);

    enum class Style {
        Invalid,
        Narrow,
        Short,
        Long,
    };

    enum class Type {
        Invalid,
        Language,
        Region,
        Script,
        Currency,
    };

    enum class Fallback {
        Invalid,
        None,
        Code,
    };

public:
    DisplayNames(Object& prototype);
    virtual ~DisplayNames() override = default;

    String const& locale() const { return m_locale; }
    void set_locale(String locale) { m_locale = move(locale); }

    Style style() const { return m_style; }
    void set_style(StringView style);
    StringView style_string() const;

    Type type() const { return m_type; }
    void set_type(StringView type);
    StringView type_string() const;

    Fallback fallback() const { return m_fallback; }
    void set_fallback(StringView fallback);
    StringView fallback_string() const;

private:
    String m_locale;                           // [[Locale]]
    Style m_style { Style::Invalid };          // [[Style]]
    Type m_type { Type::Invalid };             // [[Type]]
    Fallback m_fallback { Fallback::Invalid }; // [[Fallback]]
};

ThrowCompletionOr<Value> canonical_code_for_display_names(GlobalObject& global_object, DisplayNames::Type type, StringView code);

}
