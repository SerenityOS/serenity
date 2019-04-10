#include "GButton.h"
#include <LibGUI/GPainter.h>
#include <SharedGraphics/StylePainter.h>
#include <AK/StringBuilder.h>

//#define GBUTTON_DEBUG

GButton::GButton(GWidget* parent)
    : GWidget(parent)
{
}

GButton::~GButton()
{
}

void GButton::set_caption(const String& caption)
{
    if (caption == m_caption)
        return;
    m_caption = caption;
    update();
}

void GButton::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;
    update();
}

void GButton::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    StylePainter::paint_button(painter, rect(), m_button_style, m_being_pressed, m_hovered, m_checkable && m_checked);

    if (m_caption.is_empty() && !m_icon)
        return;

    auto content_rect = rect().shrunken(10, 2);
    auto icon_location = m_icon ? content_rect.center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2)) : Point();
    if (m_being_pressed) {
        content_rect.move_by(1, 1);
        icon_location.move_by(1, 1);
    }
    if (m_icon)
        painter.blit(icon_location, *m_icon, m_icon->rect());
    auto& font = (m_checkable && m_checked) ? Font::default_bold_font() : this->font();
    painter.draw_text(content_rect, m_caption, font, text_alignment(), foreground_color(), TextElision::Right);
}

void GButton::mousemove_event(GMouseEvent& event)
{
    if (event.buttons() == GMouseButton::Left) {
        bool being_pressed = rect().contains(event.position());
        if (being_pressed != m_being_pressed) {
            m_being_pressed = being_pressed;
            update();
        }
    }
    GWidget::mousemove_event(event);
}

void GButton::mousedown_event(GMouseEvent& event)
{
#ifdef GBUTTON_DEBUG
    dbgprintf("GButton::mouse_down_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        m_being_pressed = true;
        update();
    }
    GWidget::mousedown_event(event);
}

void GButton::click()
{
    if (on_click)
        on_click(*this);
}

void GButton::mouseup_event(GMouseEvent& event)
{
#ifdef GBUTTON_DEBUG
    dbgprintf("GButton::mouse_up_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        bool was_being_pressed = m_being_pressed;
        m_being_pressed = false;
        update();
        if (was_being_pressed)
            click();
    }
    GWidget::mouseup_event(event);
}

void GButton::enter_event(CEvent&)
{
    m_hovered = true;
    update();
}

void GButton::leave_event(CEvent&)
{
    m_hovered = false;
    update();
}
