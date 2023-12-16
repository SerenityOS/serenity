/*
 * Copyright (c) 2021-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/Tray.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Tray);

namespace GUI {

Tray::Tray()
{
    set_fill_with_background_color(true);
    set_background_role(Gfx::ColorRole::Tray);
    set_focus_policy(GUI::FocusPolicy::TabFocus);
}

Gfx::IntRect Tray::Item::rect(Tray const& tray) const
{
    int item_height = tray.font().pixel_size_rounded_up() + 12;
    return Gfx::IntRect {
        tray.frame_thickness(),
        tray.frame_thickness() + static_cast<int>(index) * item_height,
        tray.frame_inner_rect().width(),
        item_height,
    };
}

size_t Tray::add_item(ByteString text, RefPtr<Gfx::Bitmap const> bitmap, ByteString custom_data)
{
    auto new_index = m_items.size();

    m_items.append(Item {
        .text = move(text),
        .bitmap = move(bitmap),
        .custom_data = move(custom_data),
        .index = new_index,
    });
    update();

    return new_index;
}

void Tray::set_item_checked(size_t index, bool checked)
{
    if (checked) {
        m_checked_item_index = index;
    } else {
        if (m_checked_item_index == index)
            m_checked_item_index = {};
    }
    update();
}

void Tray::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    for (auto& item : m_items) {
        auto rect = item.rect(*this);
        bool is_pressed = item.index == m_pressed_item_index;
        bool is_hovered = item.index == m_hovered_item_index;
        bool is_checked = item.index == m_checked_item_index;
        Gfx::StylePainter::paint_button(painter, rect, palette(), Gfx::ButtonStyle::Tray, is_pressed && is_hovered, is_hovered, is_checked, is_enabled());

        Gfx::IntRect icon_rect {
            rect.x() + 4,
            0,
            16,
            16,
        };
        icon_rect.center_vertically_within(rect);

        Gfx::IntRect text_rect {
            icon_rect.right() + 4,
            rect.y(),
            rect.width(),
            rect.height(),
        };
        text_rect.intersect(rect);

        if (is_pressed && is_hovered) {
            icon_rect.translate_by(1, 1);
            text_rect.translate_by(1, 1);
        }

        if (item.bitmap) {
            if (is_hovered)
                painter.blit_brightened(icon_rect.location(), *item.bitmap, item.bitmap->rect());
            else
                painter.blit(icon_rect.location(), *item.bitmap, item.bitmap->rect());
        }

        auto const& font = is_checked ? this->font().bold_variant() : this->font();
        painter.draw_text(text_rect, item.text, font, Gfx::TextAlignment::CenterLeft, palette().tray_text());
    }
}

void Tray::mousemove_event(GUI::MouseEvent& event)
{
    auto* hovered_item = item_at(event.position());
    if (!hovered_item) {
        if (m_hovered_item_index.has_value())
            update();
        m_hovered_item_index = {};
        return;
    }
    if (m_hovered_item_index != hovered_item->index) {
        m_hovered_item_index = hovered_item->index;
        update();
    }
}

void Tray::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;

    auto* pressed_item = item_at(event.position());
    if (!pressed_item)
        return;

    if (m_pressed_item_index != pressed_item->index) {
        m_pressed_item_index = pressed_item->index;
        update();
    }
}

void Tray::mouseup_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;

    if (auto* pressed_item = item_at(event.position()); pressed_item && m_pressed_item_index == pressed_item->index) {
        on_item_activation(pressed_item->custom_data);
    }

    m_pressed_item_index = {};
    update();
}

void Tray::leave_event(Core::Event&)
{
    m_hovered_item_index = {};
    update();
}

Tray::Item* Tray::item_at(Gfx::IntPoint position)
{
    for (auto& item : m_items) {
        if (item.rect(*this).contains(position))
            return &item;
    }
    return nullptr;
}

void Tray::focusin_event(GUI::FocusEvent&)
{
    if (m_items.is_empty())
        return;
    m_hovered_item_index = 0;
    update();
}

void Tray::focusout_event(GUI::FocusEvent&)
{
    if (m_items.is_empty())
        return;
    m_hovered_item_index = {};
    update();
}

void Tray::keydown_event(GUI::KeyEvent& event)
{
    if (m_items.is_empty() || event.modifiers())
        return Frame::keydown_event(event);

    if (event.key() == KeyCode::Key_Down) {
        if (!m_hovered_item_index.has_value())
            m_hovered_item_index = 0;
        else
            m_hovered_item_index = (*m_hovered_item_index + 1) % m_items.size();
        update();
        return;
    }

    if (event.key() == KeyCode::Key_Up) {
        if (!m_hovered_item_index.has_value() || m_hovered_item_index == 0u)
            m_hovered_item_index = m_items.size() - 1;
        else
            m_hovered_item_index = *m_hovered_item_index - 1;
        update();
        return;
    }

    if (event.key() == KeyCode::Key_Return) {
        if (m_hovered_item_index.has_value())
            on_item_activation(m_items[*m_hovered_item_index].custom_data);
        return;
    }

    Frame::keydown_event(event);
}

}
