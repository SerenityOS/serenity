/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
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
#include <LibGfx/Font/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

REGISTER_WIDGET(GUI, Button)
REGISTER_WIDGET(GUI, DialogButton)

namespace GUI {

Button::Button(String text)
    : AbstractButton(move(text))
{
    set_min_size({ SpecialDimension::Shrink });
    set_preferred_size({ SpecialDimension::OpportunisticGrow, SpecialDimension::Shrink });
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

    REGISTER_WRITE_ONLY_STRING_PROPERTY("icon", set_icon_from_path);
    REGISTER_BOOL_PROPERTY("default", is_default, set_default);
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

    bool paint_pressed = is_being_pressed() || m_mimic_pressed || (m_menu && m_menu->is_visible());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), m_button_style, paint_pressed, is_hovered(), is_checked(), is_enabled(), is_focused(), is_default() && !another_button_has_focus());

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
        auto solid_color = m_icon->solid_color(60);
        bool should_invert_icon = false;
        if (solid_color.has_value()) {
            auto contrast_ratio = palette().button().contrast_ratio(*solid_color);
            // Note: 4.5 is the minimum recommended contrast ratio for text on the web:
            // (https://developer.mozilla.org/en-US/docs/Web/Accessibility/Understanding_WCAG/Perceivable/Color_contrast)
            // Reusing that threshold here as it seems to work reasonably well.
            should_invert_icon = contrast_ratio < 4.5f && contrast_ratio < palette().button().contrast_ratio(solid_color->inverted());
        }
        auto icon = should_invert_icon ? m_icon->inverted().release_value_but_fixme_should_propagate_errors() : NonnullRefPtr { *m_icon };
        if (is_enabled()) {
            if (is_hovered())
                painter.blit_brightened(icon_location, *icon, icon->rect());
            else
                painter.blit(icon_location, *icon, icon->rect());
        } else {
            painter.blit_disabled(icon_location, *icon, icon->rect(), palette());
        }
    }
    auto& font = is_checked() ? this->font().bold_variant() : this->font();
    if (m_icon && !text().is_empty()) {
        content_rect.translate_by(m_icon->width() + icon_spacing(), 0);
        content_rect.set_width(content_rect.width() - m_icon->width() - icon_spacing());
    }

    Gfx::IntRect text_rect { 0, 0, font.width_rounded_up(text()), font.pixel_size_rounded_up() };
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

    mimic_pressed();

    if (on_click)
        on_click(modifiers);
    if (m_action)
        m_action->activate(this);
}

void Button::double_click(unsigned int modifiers)
{
    if (on_double_click)
        on_double_click(modifiers);
}

void Button::middle_mouse_click(unsigned int modifiers)
{
    if (!is_enabled())
        return;

    NonnullRefPtr protector = *this;

    if (on_middle_mouse_click)
        on_middle_mouse_click(modifiers);
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
    set_visible(action.is_visible());
    set_enabled(action.is_enabled());
    set_checkable(action.is_checkable());
    if (action.is_checkable())
        set_checked(action.is_checked());
}

void Button::set_icon(RefPtr<Gfx::Bitmap const> icon)
{
    if (m_icon == icon)
        return;
    m_icon = move(icon);
    update();
}

void Button::set_icon_from_path(ByteString const& path)
{
    auto maybe_bitmap = Gfx::Bitmap::load_from_file(path);
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
        m_menu->popup(screen_relative_rect().bottom_left().moved_up(1), {}, rect());
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

void Button::mimic_pressed()
{
    if (!is_being_pressed() && !was_being_pressed()) {
        m_mimic_pressed = true;

        stop_timer();
        start_timer(80, Core::TimerShouldFireWhenNotVisible::Yes);

        update();
    }
}

void Button::timer_event(Core::TimerEvent&)
{
    if (m_mimic_pressed) {
        m_mimic_pressed = false;

        update();
    }
}

Optional<UISize> Button::calculated_min_size() const
{
    int width = 22;
    int height = 22;
    int constexpr padding = 6;
    if (!text().is_empty()) {
        width = max(width, font().width_rounded_up("..."sv) + padding);
        height = max(height, font().pixel_size_rounded_up() + padding);
    }
    if (icon()) {
        int icon_width = icon()->width() + icon_spacing();
        width = text().is_empty() ? max(width, icon_width) : width + icon_width;
        height = max(height, icon()->height() + padding);
    }

    return UISize(width, height);
}

Optional<UISize> DialogButton::calculated_min_size() const
{
    int constexpr scale = 8;
    int constexpr padding = 6;
    int width = max(80, font().presentation_size() * scale);
    int height = max(22, font().pixel_size_rounded_up() + padding);

    return UISize(width, height);
}

}
