/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Button.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Button)

namespace GUI {

Button::Button(String text)
    : AbstractButton(move(text))
{
    set_min_width(32);
    set_fixed_height(22);
    set_focus_policy(GUI::FocusPolicy::StrongFocus);

    on_focus_change = [this](bool has_focus, auto) {
        if (!is_default())
            return;
        if (!has_focus && is<Button>(window()->focused_widget()))
            m_another_button_has_focus = true;
        else
            m_another_button_has_focus = false;
        update();
    };

    REGISTER_ENUM_PROPERTY(
        "button_style", button_style, set_button_style, Gfx::ButtonStyle,
        { Gfx::ButtonStyle::Normal, "Normal" },
        { Gfx::ButtonStyle::Coolbar, "Coolbar" });

    REGISTER_STRING_PROPERTY("icon", icon, set_icon_from_path);
}

Button::~Button()
{
    if (m_action)
        m_action->unregister_button({}, *this);
}

void Button::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    bool paint_pressed = is_being_pressed() || (m_menu && m_menu->is_visible());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), m_button_style, paint_pressed, is_hovered(), is_checked(), is_enabled(), is_focused(), is_default() & !another_button_has_focus());

    if (text().is_empty() && !m_icon)
        return;

    auto content_rect = rect().shrunken(8, 2);
    auto icon_location = m_icon ? content_rect.center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2)) : Gfx::IntPoint();
    if (m_icon && !text().is_empty())
        icon_location.set_x(content_rect.x());

    if (paint_pressed || is_checked()) {
        painter.translate(1, 1);
    } else if (m_icon && is_enabled() && is_hovered() && button_style() == Gfx::ButtonStyle::Coolbar) {
        auto shadow_color = palette().button().darkened(0.7f);
        painter.blit_filtered(icon_location.translated(1, 1), *m_icon, m_icon->rect(), [&shadow_color](auto) {
            return shadow_color;
        });
        icon_location.translate_by(-1, -1);
    }

    if (m_icon) {
        if (is_enabled()) {
            if (is_hovered())
                painter.blit_brightened(icon_location, *m_icon, m_icon->rect());
            else
                painter.blit(icon_location, *m_icon, m_icon->rect());
        } else {
            painter.blit_disabled(icon_location, *m_icon, m_icon->rect(), palette());
        }
    }
    auto& font = is_checked() ? this->font().bold_variant() : this->font();
    if (m_icon && !text().is_empty()) {
        content_rect.translate_by(m_icon->width() + icon_spacing(), 0);
        content_rect.set_width(content_rect.width() - m_icon->width() - icon_spacing());
    }

    Gfx::IntRect text_rect { 0, 0, font.width(text()), font.glyph_height() };
    if (text_rect.width() > content_rect.width())
        text_rect.set_width(content_rect.width());
    text_rect.align_within(content_rect, text_alignment());
    paint_text(painter, text_rect, font, text_alignment());

    if (is_focused()) {
        Gfx::IntRect focus_rect;
        if (m_icon && !text().is_empty())
            focus_rect = text_rect.inflated(4, 4);
        else
            focus_rect = rect().shrunken(8, 8);
        painter.draw_focus_rect(focus_rect, palette().focus_outline());
    }
}

void Button::click(unsigned modifiers)
{
    if (!is_enabled())
        return;

    NonnullRefPtr protector = *this;

    if (is_checkable()) {
        if (is_checked() && !is_uncheckable())
            return;
        set_checked(!is_checked());
    }
    if (on_click)
        on_click(modifiers);
    if (m_action)
        m_action->activate(this);
}

void Button::context_menu_event(ContextMenuEvent& context_menu_event)
{
    if (!is_enabled())
        return;
    if (on_context_menu_request)
        on_context_menu_request(context_menu_event);
}

void Button::set_action(Action& action)
{
    m_action = action;
    action.register_button({}, *this);
    set_enabled(action.is_enabled());
    set_checkable(action.is_checkable());
    if (action.is_checkable())
        set_checked(action.is_checked());
}

void Button::set_icon(RefPtr<Gfx::Bitmap> icon)
{
    if (m_icon == icon)
        return;
    m_icon = move(icon);
    update();
}

void Button::set_icon_from_path(String const& path)
{
    auto maybe_bitmap = Gfx::Bitmap::try_load_from_file(path);
    if (maybe_bitmap.is_error()) {
        dbgln("Unable to load bitmap `{}` for button icon", path);
        return;
    }
    set_icon(maybe_bitmap.release_value());
}

bool Button::is_uncheckable() const
{
    if (!m_action)
        return true;
    if (!m_action->group())
        return true;
    return m_action->group()->is_unchecking_allowed();
}

void Button::set_menu(RefPtr<GUI::Menu> menu)
{
    if (m_menu == menu)
        return;
    if (m_menu)
        m_menu->on_visibility_change = nullptr;
    m_menu = menu;
    if (m_menu) {
        m_menu->on_visibility_change = [&](bool) {
            update();
        };
    }
}

void Button::mousedown_event(MouseEvent& event)
{
    if (m_menu) {
        if (button_style() == Gfx::ButtonStyle::Tray)
            m_menu->popup(screen_relative_rect().top_right());
        else
            m_menu->popup(screen_relative_rect().top_left());
        update();
        return;
    }
    AbstractButton::mousedown_event(event);
}

void Button::mousemove_event(MouseEvent& event)
{
    if (m_menu) {
        return;
    }
    AbstractButton::mousemove_event(event);
}

bool Button::is_default() const
{
    if (!window())
        return false;
    return this == window()->default_return_key_widget();
}

void Button::set_default(bool default_button)
{
    deferred_invoke([this, default_button] {
        VERIFY(window());
        window()->set_default_return_key_widget(default_button ? this : nullptr);
    });
}

}
