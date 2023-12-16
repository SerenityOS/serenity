/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/StringBuilder.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Progressbar.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(GUI, Progressbar)
REGISTER_WIDGET(GUI, VerticalProgressbar)
REGISTER_WIDGET(GUI, HorizontalProgressbar)

namespace GUI {

Progressbar::Progressbar(Orientation orientation)
    : m_orientation(orientation)
{
    REGISTER_DEPRECATED_STRING_PROPERTY("text", text, set_text);
    REGISTER_ENUM_PROPERTY("format", format, set_format, Format,
        { Format::NoText, "NoText" },
        { Format::Percentage, "Percentage" },
        { Format::ValueSlashMax, "ValueSlashMax" });
    REGISTER_INT_PROPERTY("min", min, set_min);
    REGISTER_INT_PROPERTY("max", max, set_max);

    set_preferred_size(SpecialDimension::Fit);
}

void Progressbar::set_value(int value)
{
    if (m_value == value)
        return;
    m_value = value;
    update();
}

void Progressbar::set_range(int min, int max)
{
    VERIFY(min <= max);
    m_min = min;
    m_max = max;
    m_value = clamp(m_value, m_min, m_max);
}

void Progressbar::paint_event(PaintEvent& event)
{
    Frame::paint_event(event);

    Painter painter(*this);
    auto rect = frame_inner_rect();
    painter.add_clip_rect(rect);
    painter.add_clip_rect(event.rect());

    ByteString progress_text;
    if (m_format != Format::NoText) {
        // Then we draw the progress text over the gradient.
        // We draw it twice, once offset (1, 1) for a drop shadow look.
        StringBuilder builder;
        builder.append(m_text);
        if (m_format == Format::Percentage) {
            float range_size = m_max - m_min;
            float progress = (m_value - m_min) / range_size;
            builder.appendff("{}%", (int)(progress * 100));
        } else if (m_format == Format::ValueSlashMax) {
            builder.appendff("{}/{}", m_value, m_max);
        }
        progress_text = builder.to_byte_string();
    }

    Gfx::StylePainter::paint_progressbar(painter, rect, palette(), m_min, m_max, m_value, progress_text, m_orientation);
}

void Progressbar::set_orientation(Orientation value)
{
    if (m_orientation == value)
        return;
    m_orientation = value;
    update();
}

Optional<UISize> Progressbar::calculated_preferred_size() const
{
    if (orientation() == Gfx::Orientation::Vertical)
        return { { 22, SpecialDimension::OpportunisticGrow } };
    return { { SpecialDimension::OpportunisticGrow, 22 } };
}

}
