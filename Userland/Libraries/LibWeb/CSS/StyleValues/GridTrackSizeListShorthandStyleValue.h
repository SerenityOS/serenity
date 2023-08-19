/*
 * Copyright (c) 2023, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/StyleValue.h>
#include <LibWeb/CSS/StyleValues/GridTemplateAreaStyleValue.h>

namespace Web::CSS {

class GridTrackSizeListShorthandStyleValue final : public StyleValueWithDefaultOperators<GridTrackSizeListShorthandStyleValue> {
public:
    static ValueComparingNonnullRefPtr<GridTrackSizeListShorthandStyleValue> create(
        ValueComparingNonnullRefPtr<GridTemplateAreaStyleValue const> areas,
        ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> rows,
        ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> columns);
    virtual ~GridTrackSizeListShorthandStyleValue() override = default;

    auto rows() const { return m_properties.rows; }
    auto columns() const { return m_properties.columns; }
    auto areas() const { return m_properties.areas; }

    virtual ErrorOr<String> to_string() const override;

    bool properties_equal(GridTrackSizeListShorthandStyleValue const& other) const { return m_properties == other.m_properties; }

private:
    GridTrackSizeListShorthandStyleValue(
        ValueComparingNonnullRefPtr<GridTemplateAreaStyleValue const> areas,
        ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> rows,
        ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> columns)
        : StyleValueWithDefaultOperators(Type::GridTrackSizeListShorthand)
        , m_properties { .areas = move(areas), .rows = move(rows), .columns = move(columns) }
    {
    }

    struct Properties {
        ValueComparingNonnullRefPtr<GridTemplateAreaStyleValue const> areas;
        ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> rows;
        ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> columns;
        bool operator==(Properties const&) const = default;
    } m_properties;
};

}
