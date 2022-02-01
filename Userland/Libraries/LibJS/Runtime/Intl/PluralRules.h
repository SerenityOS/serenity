/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/StringView.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Intl {

class PluralRules final : public NumberFormatBase {
    JS_OBJECT(PluralRules, NumberFormatBase);

public:
    enum class Type {
        Cardinal,
        Ordinal,
    };

    PluralRules(Object& prototype);
    virtual ~PluralRules() override = default;

    Type type() const { return m_type; }
    StringView type_string() const;
    void set_type(StringView type);

private:
    Type m_type { Type::Cardinal }; // [[Type]]
};

ThrowCompletionOr<PluralRules*> initialize_plural_rules(GlobalObject& global_object, PluralRules& plural_rules, Value locales_value, Value options_value);

}
