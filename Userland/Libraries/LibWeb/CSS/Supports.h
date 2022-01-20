/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/GeneralEnclosed.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-conditional-3/#at-supports
class Supports final : public RefCounted<Supports> {
    friend class Parser;

public:
    struct Feature {
        String declaration;
        bool evaluate() const;
    };

    struct Condition;
    struct InParens {
        Variant<NonnullOwnPtr<Condition>, Feature, GeneralEnclosed> value;

        bool evaluate() const;
    };

    struct Condition {
        enum class Type {
            Not,
            And,
            Or,
        };
        Type type;
        Vector<InParens> children;

        bool evaluate() const;
    };

    static NonnullRefPtr<Supports> create(NonnullOwnPtr<Condition>&& condition)
    {
        return adopt_ref(*new Supports(move(condition)));
    }

    bool matches() const { return m_matches; }

private:
    Supports(NonnullOwnPtr<Condition>&&);

    NonnullOwnPtr<Condition> m_condition;
    bool m_matches { false };
};

}
