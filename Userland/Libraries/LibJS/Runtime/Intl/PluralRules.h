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
#include <LibUnicode/PluralRules.h>

namespace JS::Intl {

class PluralRules final : public NumberFormatBase {
    JS_OBJECT(PluralRules, NumberFormatBase);

public:
    virtual ~PluralRules() override = default;

    Unicode::PluralForm type() const { return m_type; }
    StringView type_string() const { return Unicode::plural_form_to_string(m_type); }
    void set_type(StringView type) { m_type = Unicode::plural_form_from_string(type); }

private:
    explicit PluralRules(Object& prototype);

    Unicode::PluralForm m_type { Unicode::PluralForm::Cardinal }; // [[Type]]
};

Unicode::PluralOperands get_operands(String const& string);
Unicode::PluralCategory plural_rule_select(StringView locale, Unicode::PluralForm type, Value number, Unicode::PluralOperands operands);
Unicode::PluralCategory resolve_plural(PluralRules const&, Value number);
Unicode::PluralCategory resolve_plural(NumberFormatBase const& number_format, Unicode::PluralForm type, Value number);
Unicode::PluralCategory plural_rule_select_range(StringView locale, Unicode::PluralForm, Unicode::PluralCategory start, Unicode::PluralCategory end);
ThrowCompletionOr<Unicode::PluralCategory> resolve_plural_range(VM&, PluralRules const&, Value start, Value end);

}
