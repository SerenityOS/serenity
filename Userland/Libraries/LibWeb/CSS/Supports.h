/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/GeneralEnclosed.h>
#include <LibWeb/CSS/Parser/Declaration.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-conditional-4/#at-supports
class Supports final : public RefCounted<Supports> {
    friend class Parser::Parser;

public:
    struct Declaration {
        DeprecatedString declaration;
        bool evaluate() const;
        DeprecatedString to_deprecated_string() const;
    };

    struct Selector {
        DeprecatedString selector;
        bool evaluate() const;
        DeprecatedString to_deprecated_string() const;
    };

    struct Feature {
        Variant<Declaration, Selector> value;
        bool evaluate() const;
        DeprecatedString to_deprecated_string() const;
    };

    struct Condition;
    struct InParens {
        Variant<NonnullOwnPtr<Condition>, Feature, GeneralEnclosed> value;

        bool evaluate() const;
        DeprecatedString to_deprecated_string() const;
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
        DeprecatedString to_deprecated_string() const;
    };

    static NonnullRefPtr<Supports> create(NonnullOwnPtr<Condition>&& condition)
    {
        return adopt_ref(*new Supports(move(condition)));
    }

    bool matches() const { return m_matches; }
    DeprecatedString to_deprecated_string() const;

private:
    Supports(NonnullOwnPtr<Condition>&&);

    NonnullOwnPtr<Condition> m_condition;
    bool m_matches { false };
};

}

template<>
struct AK::Formatter<Web::CSS::Supports::InParens> : AK::Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Web::CSS::Supports::InParens const& in_parens)
    {
        return Formatter<StringView>::format(builder, in_parens.to_deprecated_string());
    }
};
