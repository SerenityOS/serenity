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

    Rect text_rect { 0, 0, font.width(text()), font.glyph_height() };
    if (text_rect.width() > content_rect.width())
        text_rect.set_width(content_rect.width());
    text_rect.align_within(content_rect, text_alignment());
    paint_text(painter, text_rect, font, TextAlignment::Center);
}

void GButton::click()
{
    if (!is_enabled())
        return;
    if (on_click)
        on_click(*this);
}

bool GButton::accepts_keyboard_select() const {
  return is_enabled();
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
