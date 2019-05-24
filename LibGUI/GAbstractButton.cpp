#include <LibGUI/GAbstractButton.h>

GAbstractButton::GAbstractButton(GWidget* parent)
    : GWidget(parent)
{
}

GAbstractButton::GAbstractButton(const String& text, GWidget* parent)
    : GWidget(parent)
    , m_text(text)
{
}

GAbstractButton::~GAbstractButton()
{
}

void GAbstractButton::set_text(const String& text)
{
    if (m_text == text)
        return;
    m_text = text;
    update();
}

void GAbstractButton::set_checked(bool checked)
{
    if (m_checked == checked)
        return;
    m_checked = checked;
    update();
    if (on_checked)
        on_checked(checked);
}

void GAbstractButton::set_checkable(bool checkable)
{
    if (m_checkable == checkable)
        return;
    m_checkable = checkable;
    update();
}

void GAbstractButton::mousemove_event(GMouseEvent& event)
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

void GAbstractButton::mousedown_event(GMouseEvent& event)
{
#ifdef GABSTRACTBUTTON_DEBUG
    dbgprintf("GAbstractButton::mouse_down_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
#endif
    if (event.button() == GMouseButton::Left) {
        if (is_enabled()) {
            m_being_pressed = true;
            update();
        }
    }
    GWidget::mousedown_event(event);
}

void GAbstractButton::mouseup_event(GMouseEvent& event)
{
#ifdef GABSTRACTBUTTON_DEBUG
    dbgprintf("GAbstractButton::mouse_up_event: x=%d, y=%d, button=%u\n", event.x(), event.y(), (unsigned)event.button());
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

void GAbstractButton::enter_event(CEvent&)
{
    m_hovered = true;
    update();
}

void GAbstractButton::leave_event(CEvent&)
{
    m_hovered = false;
    update();
}

void GAbstractButton::keydown_event(GKeyEvent& event)
{
    if (event.key() == KeyCode::Key_Return)
        click();
    GWidget::keydown_event(event);
}
