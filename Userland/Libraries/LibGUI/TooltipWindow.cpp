/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Label.h>
#include <LibGUI/Window.h>
#include <LibGfx/Painter.h>
#include <LibGfx/SystemTheme.h>
#include <LibGfx/TextAlignment.h>

namespace GUI {

void TooltipWindow::set_tooltip(const String& tooltip)
{
    m_label->set_text(Gfx::parse_ampersand_string(tooltip));
    int tooltip_width = m_label->min_width() + 10;
    int line_count = m_label->text().count("\n");
    int glyph_height = m_label->font().glyph_height();
    int tooltip_height = glyph_height * (1 + line_count) + ((glyph_height + 1) / 2) * line_count + 8;

    Gfx::IntRect desktop_rect = Desktop::the().rect();
    if (tooltip_width > desktop_rect.width())
        tooltip_width = desktop_rect.width();

    set_rect(rect().x(), rect().y(), tooltip_width, tooltip_height);
}

TooltipWindow::TooltipWindow()
{
    set_window_type(WindowType::Tooltip);
    set_double_buffering_enabled(false);

    m_label = set_main_widget<Label>();
    m_label->set_background_role(Gfx::ColorRole::Tooltip);
    m_label->set_foreground_role(Gfx::ColorRole::TooltipText);
    m_label->set_fill_with_background_color(true);
    m_label->set_frame_thickness(1);
    m_label->set_frame_shape(Gfx::FrameShape::Container);
    m_label->set_frame_shadow(Gfx::FrameShadow::Plain);
    m_label->set_autosize(true);
}

}
