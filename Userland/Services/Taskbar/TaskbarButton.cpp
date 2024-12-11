/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TaskbarButton.h"
#include "WindowList.h"
#include <LibGUI/Action.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/WindowTheme.h>

TaskbarButton::TaskbarButton(WindowIdentifier const& identifier)
    : m_identifier(identifier)
{
    set_checkable(true);
}

void TaskbarButton::context_menu_event(GUI::ContextMenuEvent&)
{
    GUI::ConnectionToWindowManagerServer::the().async_popup_window_menu(
        m_identifier.client_id(),
        m_identifier.window_id(),
        screen_relative_rect().location());
}

void TaskbarButton::update_taskbar_rect()
{
    GUI::ConnectionToWindowManagerServer::the().async_set_window_taskbar_rect(
        m_identifier.client_id(),
        m_identifier.window_id(),
        screen_relative_rect());
}

void TaskbarButton::clear_taskbar_rect()
{
    GUI::ConnectionToWindowManagerServer::the().async_set_window_taskbar_rect(
        m_identifier.client_id(),
        m_identifier.window_id(),
        {});
}

void TaskbarButton::resize_event(GUI::ResizeEvent& event)
{
    update_taskbar_rect();
    return GUI::Button::resize_event(event);
}

static void paint_custom_progressbar(GUI::Painter& painter, Gfx::IntRect const& rect, Gfx::IntRect const& text_rect, Palette const& palette, int min, int max, int value, StringView text, Gfx::Font const& font, Gfx::TextAlignment text_alignment)
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
    hole_rect.translate_by(rect.location());
    hole_rect.set_right_without_resize(rect.right());
    Gfx::PainterStateSaver saver(painter);
    painter.add_clip_rect(hole_rect);
    if (!text.is_null())
        painter.draw_text(text_rect, text, font, text_alignment, palette.base_text(), Gfx::TextElision::Right);
}

void TaskbarButton::paint_event(GUI::PaintEvent& event)
{
    VERIFY(icon());
    auto& icon = *this->icon();
    auto& font = is_checked() ? this->font().bold_variant() : this->font();
    auto& window = WindowList::the().ensure_window(m_identifier);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    palette().window_theme().paint_button(painter, rect(), palette(), button_style(), is_being_pressed(), is_hovered(), is_checked(), is_enabled());

    if (text().is_empty())
        return;

    auto content_rect = rect().shrunken(8, 2);
    auto icon_location = content_rect.center().translated(-(icon.width() / 2), -(icon.height() / 2));
    if (!text().is_empty())
        icon_location.set_x(content_rect.x());

    if (!text().is_empty()) {
        content_rect.translate_by(icon.width() + 4, 0);
        content_rect.set_width(content_rect.width() - icon.width() - 4);
    }

    Gfx::IntRect text_rect { 0, 0, font.width_rounded_up(text()), font.pixel_size_rounded_up() };
    if (text_rect.width() > content_rect.width())
        text_rect.set_width(content_rect.width());
    text_rect.align_within(content_rect, text_alignment());

    if (is_being_pressed() || is_checked()) {
        text_rect.translate_by(1, 1);
        icon_location.translate_by(1, 1);
    }

    if (window.progress().has_value()) {
        auto adjusted_rect = rect().shrunken(4, 4);
        if (!is_being_pressed() && !is_checked()) {
            adjusted_rect.translate_by(-1, -1);
            adjusted_rect.set_width(adjusted_rect.width() + 1);
            adjusted_rect.set_height(adjusted_rect.height() + 1);
        }
        paint_custom_progressbar(painter, adjusted_rect, text_rect, palette(), 0, 100, window.progress().value(), text(), font, text_alignment());
    }

    if (is_enabled()) {
        if (is_hovered())
            painter.blit_brightened(icon_location, icon, icon.rect());
        else
            painter.blit(icon_location, icon, icon.rect());
    } else {
        painter.blit_disabled(icon_location, icon, icon.rect(), palette());
    }

    if (!window.progress().has_value())
        paint_text(painter, text_rect, font, text_alignment());
}
