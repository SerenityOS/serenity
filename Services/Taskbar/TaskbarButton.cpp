/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "TaskbarButton.h"
#include "WindowList.h"
#include <LibGUI/Action.h>
#include <LibGUI/Painter.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

TaskbarButton::TaskbarButton(const WindowIdentifier& identifier)
    : m_identifier(identifier)
{
}

TaskbarButton::~TaskbarButton()
{
}

void TaskbarButton::context_menu_event(GUI::ContextMenuEvent&)
{
    GUI::WindowServerConnection::the().post_message(Messages::WindowServer::WM_PopupWindowMenu(m_identifier.client_id(), m_identifier.window_id(), screen_relative_rect().location()));
}

void TaskbarButton::update_taskbar_rect()
{
    GUI::WindowServerConnection::the().post_message(
        Messages::WindowServer::WM_SetWindowTaskbarRect(
            m_identifier.client_id(),
            m_identifier.window_id(),
            screen_relative_rect()));
}

void TaskbarButton::clear_taskbar_rect()
{
    GUI::WindowServerConnection::the().post_message(
        Messages::WindowServer::WM_SetWindowTaskbarRect(
            m_identifier.client_id(),
            m_identifier.window_id(),
            {}));
}

void TaskbarButton::resize_event(GUI::ResizeEvent& event)
{
    update_taskbar_rect();
    return GUI::Button::resize_event(event);
}

static void paint_custom_progress_bar(GUI::Painter& painter, const Gfx::IntRect& rect, const Gfx::IntRect& text_rect, const Palette& palette, int min, int max, int value, const StringView& text, const Gfx::Font& font, Gfx::TextAlignment text_alignment)
{
    float range_size = max - min;
    float progress = (value - min) / range_size;
    float progress_width = progress * rect.width();

    Gfx::IntRect progress_rect { rect.x(), rect.y(), (int)progress_width, rect.height() };

    {
        Gfx::PainterStateSaver saver(painter);
        painter.add_clip_rect(progress_rect);

        Color start_color = palette.active_window_border1();
        Color end_color = palette.active_window_border2();
        painter.fill_rect_with_gradient(rect, start_color, end_color);

        if (!text.is_null()) {
            painter.draw_text(text_rect.translated(1, 1), text, font, text_alignment, palette.base_text(), Gfx::TextElision::Right);
            painter.draw_text(text_rect, text, font, text_alignment, palette.base_text().inverted(), Gfx::TextElision::Right);
        }
    }

    Gfx::IntRect hole_rect { (int)progress_width, 0, (int)(rect.width() - progress_width), rect.height() };
    hole_rect.move_by(rect.location());
    hole_rect.set_right_without_resize(rect.right());
    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(hole_rect);
    if (!text.is_null())
        painter.draw_text(text_rect, text, font, text_alignment, palette.base_text(), Gfx::TextElision::Right);
}

void TaskbarButton::paint_event(GUI::PaintEvent& event)
{
    ASSERT(icon());
    auto& icon = *this->icon();
    auto& font = is_checked() ? Gfx::Font::default_bold_font() : this->font();
    auto& window = WindowList::the().ensure_window(m_identifier);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), button_style(), is_being_pressed(), is_hovered(), is_checked(), is_enabled());

    if (text().is_empty())
        return;

    bool has_progress = window.progress() >= 0 && window.progress() <= 100;

    auto content_rect = rect().shrunken(8, 2);
    auto icon_location = content_rect.center().translated(-(icon.width() / 2), -(icon.height() / 2));
    if (!text().is_empty())
        icon_location.set_x(content_rect.x());

    if (!text().is_empty()) {
        content_rect.move_by(icon.width() + 4, 0);
        content_rect.set_width(content_rect.width() - icon.width() - 4);
    }

    Gfx::IntRect text_rect { 0, 0, font.width(text()), font.glyph_height() };
    if (text_rect.width() > content_rect.width())
        text_rect.set_width(content_rect.width());
    text_rect.align_within(content_rect, text_alignment());

    if (is_being_pressed() || is_checked()) {
        text_rect.move_by(1, 1);
        icon_location.move_by(1, 1);
    }

    if (has_progress) {
        auto adjusted_rect = rect().shrunken(4, 4);
        if (is_being_pressed() || is_checked()) {
            adjusted_rect.set_height(adjusted_rect.height() + 1);
        }
        paint_custom_progress_bar(painter, adjusted_rect, text_rect, palette(), 0, 100, window.progress(), text(), font, text_alignment());
    }

    if (is_enabled()) {
        if (is_hovered())
            painter.blit_brightened(icon_location, icon, icon.rect());
        else
            painter.blit(icon_location, icon, icon.rect());
    } else {
        painter.blit_dimmed(icon_location, icon, icon.rect());
    }

    if (!has_progress)
        paint_text(painter, text_rect, font, text_alignment());
}
