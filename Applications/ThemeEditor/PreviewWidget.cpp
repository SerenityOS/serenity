/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "PreviewWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/WindowTheme.h>

namespace ThemeEditor {

PreviewWidget::PreviewWidget(const Gfx::Palette& preview_palette)
    : m_preview_palette(preview_palette)
{
    m_active_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png");
    m_inactive_window_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/window.png");
}

PreviewWidget::~PreviewWidget()
{
}

void PreviewWidget::set_preview_palette(const Gfx::Palette& palette)
{
    m_preview_palette = palette;
    update();
}

void PreviewWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);

    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    painter.fill_rect(frame_inner_rect(), m_preview_palette.desktop_background());

    auto paint_window = [&](auto& title, const Gfx::IntRect& rect, auto state) {
        Gfx::IntRect leftmost_button_rect { 300, 4, 16, 16 };

        {
            Gfx::PainterStateSaver saver(painter);
            auto frame_rect = Gfx::WindowTheme::current().frame_rect_for_window(Gfx::WindowTheme::WindowType::Normal, rect, m_preview_palette);
            painter.translate(frame_rect.location());
            Gfx::WindowTheme::current().paint_normal_frame(painter, state, rect, title, *m_active_window_icon, m_preview_palette, leftmost_button_rect);
        }

        painter.fill_rect(rect, m_preview_palette.window());

        Gfx::IntRect button_rect { 0, 0, 100, 20 };
        button_rect.center_within(rect);
        Gfx::StylePainter::current().paint_button(painter, button_rect, m_preview_palette, Gfx::ButtonStyle::Normal, false);
    };

    Gfx::IntRect active_rect { 0, 0, 320, 240 };
    active_rect.center_within(frame_inner_rect());
    Gfx::IntRect inactive_rect = active_rect.translated(-20, -20);

    paint_window("Inactive window", inactive_rect, Gfx::WindowTheme::WindowState::Inactive);
    paint_window("Active window", active_rect, Gfx::WindowTheme::WindowState::Active);
}

}
