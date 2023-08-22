/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

class PlaceItemsStyleValue final : public StyleValueWithDefaultOperators<PlaceItemsStyleValue> {
public:
    static ValueComparingNonnullRefPtr<PlaceItemsStyleValue> create(ValueComparingNonnullRefPtr<StyleValue> align_items, ValueComparingNonnullRefPtr<StyleValue> justify_items)
    {
        return adopt_ref(*new (nothrow) PlaceItemsStyleValue(move(align_items), move(justify_items)));
    }
    virtual ~PlaceItemsStyleValue() override = default;

    ValueComparingNonnullRefPtr<StyleValue> align_items() const { return m_properties.align_items; }
    ValueComparingNonnullRefPtr<StyleValue> justify_items() const { return m_properties.justify_items; }

    virtual String to_string() const override;

    bool properties_equal(PlaceItemsStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    PlaceItemsStyleValue(ValueComparingNonnullRefPtr<StyleValue> align_items, ValueComparingNonnullRefPtr<StyleValue> justify_items)
        : StyleValueWithDefaultOperators(Type::PlaceItems)
        , m_properties { .align_items = move(align_items), .justify_items = move(justify_items) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<StyleValue> align_items;
        ValueComparingNonnullRefPtr<StyleValue> justify_items;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
