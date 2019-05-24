#include "GButton.h"
#include <LibGUI/GPainter.h>
#include <SharedGraphics/StylePainter.h>
#include <AK/StringBuilder.h>
#include <LibGUI/GAction.h>
#include <Kernel/KeyCode.h>

GButton::GButton(GWidget* parent)
    : GAbstractButton(parent)
{
}

GButton::GButton(const String& text, GWidget* parent)
    : GAbstractButton(text, parent)
{
}

GButton::~GButton()
{
    if (m_action)
        m_action->unregister_button({ }, *this);
}

void GButton::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    StylePainter::paint_button(painter, rect(), m_button_style, is_being_pressed(), is_hovered(), is_checkable() && is_checked(), is_enabled());

    if (text().is_empty() && !m_icon)
        return;

    auto content_rect = rect().shrunken(10, 2);
    auto icon_location = m_icon ? content_rect.center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2)) : Point();
    if (m_icon && !text().is_empty())
        icon_location.set_x(content_rect.x());
    if (is_being_pressed())
        painter.translate(1, 1);
    if (m_icon) {
        if (is_enabled())
            painter.blit(icon_location, *m_icon, m_icon->rect());
        else
            painter.blit_dimmed(icon_location, *m_icon, m_icon->rect());
    }
    auto& font = (is_checkable() && is_checked()) ? Font::default_bold_font() : this->font();
    if (m_icon && !text().is_empty()) {
        content_rect.move_by(m_icon->width() + 4, 0);
        content_rect.set_width(content_rect.width() - m_icon->width() - 4);
    }
    if (is_enabled()) {
        if (!text().is_empty()) {
            painter.draw_text(content_rect, text(), font, text_alignment(), foreground_color(), TextElision::Right);
            if (is_focused()) {
                Rect focus_rect = { 0, 0, font.width(text()), font.glyph_height() };
                focus_rect.inflate(6, 4);
                focus_rect.center_within(content_rect);
                painter.draw_rect(focus_rect, Color(140, 140, 140));
            }
        }
    } else {
        painter.draw_text(content_rect.translated(1, 1), text(), font, text_alignment(), Color::White, TextElision::Right);
        painter.draw_text(content_rect, text(), font, text_alignment(), Color::from_rgb(0x808080), TextElision::Right);
    }
}

void GButton::click()
{
    if (!is_enabled())
        return;
    if (on_click)
        on_click(*this);
}

void GButton::set_action(GAction& action)
{
    m_action = action.make_weak_ptr();
    action.register_button({ }, *this);
    set_enabled(action.is_enabled());
    set_checkable(action.is_checkable());
    if (action.is_checkable())
        set_checked(action.is_checked());
}

void GButton::set_icon(RetainPtr<GraphicsBitmap>&& icon)
{
    if (m_icon == icon)
        return;
    m_icon = move(icon);
    update();
}
