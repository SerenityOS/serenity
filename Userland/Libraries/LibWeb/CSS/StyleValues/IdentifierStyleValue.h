/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/ValueID.h>

namespace Web::CSS {

class IdentifierStyleValue final : public StyleValueWithDefaultOperators<IdentifierStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<IdentifierStyleValue>> create(ValueID id)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) IdentifierStyleValue(id));
    }
    virtual ~IdentifierStyleValue() override = default;

    ValueID id() const { return m_id; }

    virtual bool has_color() const override;
    virtual Color to_color(Optional<Layout::NodeWithStyle const&> node) const override;
    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(IdentifierStyleValue const& other) const { return m_id == other.m_id; }

private:
    explicit IdentifierStyleValue(ValueID id)
        : StyleValueWithDefaultOperators(Type::Identifier)
        , m_id(id)
    {
    }

    ValueID m_id { ValueID::Invalid };
};

}
