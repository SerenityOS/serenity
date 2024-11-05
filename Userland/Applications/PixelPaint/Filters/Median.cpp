/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Median.h"
#include <AK/QuickSort.h>
#include <Applications/PixelPaint/Filters/MedianSettingsGML.h>
#include <LibGUI/SpinBox.h>
#include <LibGfx/Painter.h>

namespace PixelPaint::Filters {

void Median::apply(Gfx::Bitmap& target_bitmap, Gfx::Bitmap const& source_bitmap) const
{
    // FIXME: Is there a better way to work around aliasing in the source and target?
    auto target = MUST(source_bitmap.clone());

    int filter_size = static_cast<int>(this->filter_size());
    for (int x = 0; x < target_bitmap.width(); ++x) {
        for (int y = 0; y < target_bitmap.height(); ++y) {
            int left = x - static_cast<int>(m_filter_radius - 1);
            int top = y - static_cast<int>(m_filter_radius - 1);
            Vector<Color, 16> values;
            values.ensure_capacity(static_cast<size_t>(filter_size * filter_size));
            for (int i = left; i < left + filter_size; ++i) {
                for (int j = top; j < top + filter_size; ++j) {
                    if (j < 0 || i < 0 || j >= source_bitmap.height() || i >= source_bitmap.width())
                        continue;
                    values.unchecked_append(source_bitmap.get_pixel(i, j));
                }
            }
            // FIXME: If there was an insertion sort in AK, we should better use that here.
            // Sort the values to be able to extract the median. The median is determined by grey value (luminosity).
            quick_sort(values, [](auto& a, auto& b) { return a.luminosity() < b.luminosity(); });
            target->set_pixel(x, y, values[values.size() / 2]);
        }
    }

    // FIXME: Can we move the `target`s data into the actual target bitmap? Can't be too hard, right?
    Gfx::Painter painter(target_bitmap);
    painter.blit({}, target, target->rect());
}

ErrorOr<RefPtr<GUI::Widget>> Median::get_settings_widget()
{
    if (!m_settings_widget) {
        auto settings_widget = GUI::Widget::construct();
        TRY(settings_widget->load_from_gml(median_settings_gml));
        settings_widget->find_descendant_of_type_named<GUI::SpinBox>("filter_radius")->on_change = [this](auto value) {
            m_filter_radius = value;
            update_preview();
        };
        m_settings_widget = settings_widget;
    }

    return m_settings_widget;
}

}
