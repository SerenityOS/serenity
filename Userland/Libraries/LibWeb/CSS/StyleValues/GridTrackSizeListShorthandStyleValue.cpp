/*
 * Copyright (c) 2023, Martin Falisse <mfalisse@outlook.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GridTrackSizeListShorthandStyleValue.h"
#include <LibWeb/CSS/StyleValues/GridTrackSizeListStyleValue.h>

namespace Web::CSS {

ValueComparingNonnullRefPtr<GridTrackSizeListShorthandStyleValue> GridTrackSizeListShorthandStyleValue::create(
    ValueComparingNonnullRefPtr<GridTemplateAreaStyleValue const> areas,
    ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> rows,
    ValueComparingNonnullRefPtr<GridTrackSizeListStyleValue const> columns)
{
    return adopt_ref(*new (nothrow) GridTrackSizeListShorthandStyleValue(move(areas), move(rows), move(columns)));
}

ErrorOr<String> GridTrackSizeListShorthandStyleValue::to_string() const
{
    auto construct_rows_string = [&]() -> ErrorOr<String> {
        StringBuilder builder;
        size_t idx = 0;
        for (auto const& row : m_properties.rows->grid_track_size_list().track_list()) {
            if (m_properties.areas->grid_template_area().size() > idx) {
                TRY(builder.try_append("\""sv));
                for (size_t y = 0; y < m_properties.areas->grid_template_area()[idx].size(); ++y) {
                    TRY(builder.try_append(m_properties.areas->grid_template_area()[idx][y]));
                    if (y != m_properties.areas->grid_template_area()[idx].size() - 1)
                        TRY(builder.try_append(" "sv));
                }
                TRY(builder.try_append("\" "sv));
            }
            TRY(builder.try_append(TRY(row.to_string())));
            if (idx < m_properties.rows->grid_track_size_list().track_list().size() - 1)
                TRY(builder.try_append(' '));
            idx++;
        }
        return TRY(builder.to_string());
    };

    if (m_properties.columns->grid_track_size_list().track_list().size() == 0)
        return String::formatted("{}", TRY(construct_rows_string()));
    return String::formatted("{} / {}", TRY(construct_rows_string()), TRY(m_properties.columns->grid_track_size_list().to_string()));
}

}
