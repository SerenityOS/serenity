/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class PlaceSelfStyleValue final : public StyleValueWithDefaultOperators<PlaceSelfStyleValue> {
public:
    static ErrorOr<ValueComparingNonnullRefPtr<PlaceSelfStyleValue>> create(ValueComparingNonnullRefPtr<StyleValue> align_self, ValueComparingNonnullRefPtr<StyleValue> justify_self)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) PlaceSelfStyleValue(move(align_self), move(justify_self)));
    }
    virtual ~PlaceSelfStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> align_self() const { return m_properties.align_self; }
    ValueComparingNonnullRefPtr<StyleValue> justify_self() const { return m_properties.justify_self; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(PlaceSelfStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    PlaceSelfStyleValue(ValueComparingNonnullRefPtr<StyleValue> align_self, ValueComparingNonnullRefPtr<StyleValue> justify_self)
        : StyleValueWithDefaultOperators(Type::PlaceSelf)
        , m_properties { .align_self = move(align_self), .justify_self = move(justify_self) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> align_self;
        ValueComparingNonnullRefPtr<StyleValue> justify_self;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
