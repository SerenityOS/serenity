#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GStackWidget.h>

GStackWidget::GStackWidget(GWidget* parent)
    : GWidget(parent)
{
}

GStackWidget::~GStackWidget()
{
}

void GStackWidget::set_active_widget(GWidget* widget)
{
    if (widget == m_active_widget)
        return;

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(rect());
        m_active_widget->set_visible(true);
    }
    if (on_active_widget_change)
        on_active_widget_change(m_active_widget);
}

void GStackWidget::resize_event(GResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect({ {}, event.size() });
}

void GStackWidget::child_event(CChildEvent& event)
{
    if (!event.child() || !is<GWidget>(*event.child()))
        return GWidget::child_event(event);
    auto& child = to<GWidget>(*event.child());
    if (event.type() == GEvent::ChildAdded) {
        if (!m_active_widget)
            set_active_widget(&child);
        else if (m_active_widget != &child)
            child.set_visible(false);
    } else if (event.type() == GEvent::ChildRemoved) {
        if (m_active_widget == &child) {
            GWidget* new_active_widget = nullptr;
            for_each_child_widget([&](auto& new_child) {
                new_active_widget = &new_child;
                return IterationDecision::Break;
            });
            set_active_widget(new_active_widget);
        }
    }
    GWidget::child_event(event);
}
