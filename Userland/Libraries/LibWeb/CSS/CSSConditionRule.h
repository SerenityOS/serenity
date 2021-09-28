/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/CSSGroupingRule.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class CSSConditionRule : public CSSGroupingRule {
    AK_MAKE_NONCOPYABLE(CSSConditionRule);
    AK_MAKE_NONMOVABLE(CSSConditionRule);

public:
    ~CSSConditionRule();

    virtual String condition_text() const = 0;
    virtual void set_condition_text(String) = 0;

protected:
    explicit CSSConditionRule(NonnullRefPtrVector<CSSRule>&&);
};

}
