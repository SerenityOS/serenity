#include <LibGUI/GStackWidget.h>
#include <LibGUI/GBoxLayout.h>

GStackWidget::GStackWidget(GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(true);
    set_background_color(Color::Red);
}

GStackWidget::~GStackWidget()
{
}

void GStackWidget::set_active_widget(GWidget* widget)
{
    dbgprintf("XXX: GStackWidget: set_active_widget %p\n", widget);
    if (widget == m_active_widget)
        return;

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(rect());
        m_active_widget->set_visible(true);
    }
}

void GStackWidget::resize_event(GResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect({ { }, event.size() });
}

void GStackWidget::child_event(GChildEvent& event)
{
    if (!event.child() || !event.child()->is_widget())
        return;
    auto& child = static_cast<GWidget&>(*event.child());
    if (event.type() == GEvent::ChildAdded) {
        dbgprintf("XXX: GStackWidget: did_add_child %p\n", &child);
        if (!m_active_widget) {
            set_active_widget(&child);
        } else {
            child.set_visible(false);
        }
    } else if (event.type() == GEvent::ChildRemoved) {
        dbgprintf("XXX: GStackWidget: did_remove_child %p\n", &child);
        if (m_active_widget == &child) {
            GWidget* new_active_widget = nullptr;
            for (auto* new_child : children()) {
                if (new_child->is_widget()) {
                    new_active_widget = static_cast<GWidget*>(new_child);
                    break;
                }
            }
            set_active_widget(new_active_widget);
        }
    }
}
