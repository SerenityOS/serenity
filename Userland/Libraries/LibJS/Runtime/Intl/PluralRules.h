/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/StringView.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/Intl/NumberFormat.h>
#include <LibJS/Runtime/Object.h>
#include <LibLocale/PluralRules.h>

namespace JS::Intl {

class PluralRules final : public NumberFormatBase {
    JS_OBJECT(PluralRules, NumberFormatBase);

public:
    virtual ~PluralRules() override = default;

    ::Locale::PluralForm type() const { return m_type; }
    StringView type_string() const { return ::Locale::plural_form_to_string(m_type); }
    void set_type(StringView type) { m_type = ::Locale::plural_form_from_string(type); }

private:
    explicit PluralRules(Object& prototype);

    ::Locale::PluralForm m_type { ::Locale::PluralForm::Cardinal }; // [[Type]]
};

::Locale::PluralOperands get_operands(DeprecatedString const& string);
::Locale::PluralCategory plural_rule_select(StringView locale, ::Locale::PluralForm type, Value number, ::Locale::PluralOperands operands);
::Locale::PluralCategory resolve_plural(PluralRules const&, Value number);
::Locale::PluralCategory resolve_plural(NumberFormatBase const& number_format, ::Locale::PluralForm type, Value number);
::Locale::PluralCategory plural_rule_select_range(StringView locale, ::Locale::PluralForm, ::Locale::PluralCategory start, ::Locale::PluralCategory end);
ThrowCompletionOr<::Locale::PluralCategory> resolve_plural_range(VM&, PluralRules const&, Value start, Value end);

}
