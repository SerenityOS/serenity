#include "GButton.h"
#include <LibGUI/GPainter.h>
#include <SharedGraphics/StylePainter.h>
#include <AK/StringBuilder.h>
#include <LibGUI/GAction.h>
#include <Kernel/KeyCode.h>

//#define GBUTTON_DEBUG

GButton::GButton(GWidget* parent)
    : GWidget(parent)
{
}

GButton::~GButton()
{
    if (m_action)
        m_action->unregister_button({ }, *this);
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

    StylePainter::paint_button(painter, rect(), m_button_style, m_being_pressed, m_hovered, m_checkable && m_checked, is_enabled());

    if (m_caption.is_empty() && !m_icon)
        return;

    auto content_rect = rect().shrunken(10, 2);
    auto icon_location = m_icon ? content_rect.center().translated(-(m_icon->width() / 2), -(m_icon->height() / 2)) : Point();
    if (m_icon && !m_caption.is_empty())
        icon_location.set_x(content_rect.x());
    if (m_being_pressed)
        painter.translate(1, 1);
    if (m_icon) {
        if (is_enabled())
            painter.blit(icon_location, *m_icon, m_icon->rect());
        else
            painter.blit_dimmed(icon_location, *m_icon, m_icon->rect());
    }
    auto& font = (m_checkable && m_checked) ? Font::default_bold_font() : this->font();
    if (m_icon && !m_caption.is_empty()) {
        content_rect.move_by(m_icon->width() + 4, 0);
        content_rect.set_width(content_rect.width() - m_icon->width() - 4);
    }
    if (is_enabled()) {
        if (!m_caption.is_empty()) {
            painter.draw_text(content_rect, m_caption, font, text_alignment(), foreground_color(), TextElision::Right);
            if (is_focused()) {
                Rect focus_rect = { 0, 0, font.width(m_caption), font.glyph_height() };
                focus_rect.inflate(6, 4);
                focus_rect.center_within(content_rect);
                painter.draw_rect(focus_rect, Color(140, 140, 140));
            }
        }
    } else {
        painter.draw_text(content_rect.translated(1, 1), m_caption, font, text_alignment(), Color::White, TextElision::Right);
        painter.draw_text(content_rect, m_caption, font, text_alignment(), Color::from_rgb(0x808080), TextElision::Right);
    }
}

void GButton::mousemove_event(GMouseEvent& event)
{
    bool is_over = rect().contains(event.position());
    m_hovered = is_over;
    if (event.buttons() & GMouseButton::Left) {
        if (is_enabled()) {
            bool being_pressed = is_over;
            if (being_pressed != m_being_pressed) {
                m_being_pressed = being_pressed;
                update();
            }
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
        if (is_enabled()) {
            m_being_pressed = true;
            update();
        }
    }
    GWidget::mousedown_event(event);
}

void GButton::click()
{
    if (!is_enabled())
        return;
    if (on_click)
        on_click(*this);
}

void GButton::mouseup_event(GMouseEvent& event)
{
#ifdef GBUTTON_DEBUG
    dbgprintf("GButton::mouse_up_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        if (is_enabled()) {
            bool was_being_pressed = m_being_pressed;
            m_being_pressed = false;
            update();
            if (was_being_pressed)
                click();
        }
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

void GButton::keydown_event(GKeyEvent& event)
{
    if (event.key() == KeyCode::Key_Return)
        click();
    GWidget::keydown_event(event);
}
