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

#include <AK/StringBuilder.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

Button::Button(const StringView& text)
    : AbstractButton(text)
{
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

    Gfx::StylePainter::paint_button(painter, rect(), palette(), m_button_style, is_being_pressed(), is_hovered(), is_checked(), is_enabled());

    if (text().is_empty() && !m_icon)
        return;

    auto content_rect = rect().shrunken(8, 2);
    auto icon_location = m_icon ? content_rect.center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2)) : Gfx::IntPoint();
    if (m_icon && !text().is_empty())
        icon_location.set_x(content_rect.x());
    if (is_being_pressed() || is_checked())
        painter.translate(1, 1);
    if (m_icon) {
        if (is_enabled()) {
            if (is_hovered())
                painter.blit_brightened(icon_location, *m_icon, m_icon->rect());
            else
                painter.blit(icon_location, *m_icon, m_icon->rect());
        } else {
            painter.blit_dimmed(icon_location, *m_icon, m_icon->rect());
        }
    }
    auto& font = is_checked() ? Gfx::Font::default_bold_font() : this->font();
    if (m_icon && !text().is_empty()) {
        content_rect.move_by(m_icon->width() + 4, 0);
        content_rect.set_width(content_rect.width() - m_icon->width() - 4);
    }

    Gfx::IntRect text_rect { 0, 0, font.width(text()), font.glyph_height() };
    if (text_rect.width() > content_rect.width())
        text_rect.set_width(content_rect.width());
    text_rect.align_within(content_rect, text_alignment());
    paint_text(painter, text_rect, font, text_alignment());
}

void Button::click(unsigned modifiers)
{
    if (!is_enabled())
        return;
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
    m_action = action.make_weak_ptr();
    action.register_button({}, *this);
    set_enabled(action.is_enabled());
    set_checkable(action.is_checkable());
    if (action.is_checkable())
        set_checked(action.is_checked());
}

void Button::set_icon(RefPtr<Gfx::Bitmap>&& icon)
{
    if (m_icon == icon)
        return;
    m_icon = move(icon);
    update();
}

bool Button::is_uncheckable() const
{
    if (!m_action)
        return true;
    if (!m_action->group())
        return true;
    return m_action->group()->is_unchecking_allowed();
}

}
