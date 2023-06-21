/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class CompositeStyleValue final : public StyleValueWithDefaultOperators<CompositeStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<CompositeStyleValue>> create(Vector<PropertyID> sub_properties, Vector<ValueComparingNonnullRefPtr<StyleValue const>> values)
    {
        return adopt_nonnull_ref_or_enomem(new CompositeStyleValue(move(sub_properties), move(values)));
    }
    virtual ~CompositeStyleValue() override;

    Vector<PropertyID> const& sub_properties() const { return m_properties.sub_properties; }
    Vector<ValueComparingNonnullRefPtr<StyleValue const>> const& values() const { return m_properties.values; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(CompositeStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    CompositeStyleValue(Vector<PropertyID> sub_properties, Vector<ValueComparingNonnullRefPtr<StyleValue const>> values);

    struct Properties {
        Vector<PropertyID> sub_properties;
        Vector<ValueComparingNonnullRefPtr<StyleValue const>> values;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
