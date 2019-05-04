#include <LibGUI/GTabWidget.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GPainter.h>
#include <SharedGraphics/StylePainter.h>

GTabWidget::GTabWidget(GWidget* parent)
    : GWidget(parent)
{
}

GTabWidget::~GTabWidget()
{
}

void GTabWidget::add_widget(const String& title, GWidget* widget)
{
    m_tabs.append({ title, widget });
    add_child(*widget);
}

void GTabWidget::set_active_widget(GWidget* widget)
{
    if (widget == m_active_widget)
        return;

    if (m_active_widget)
        m_active_widget->set_visible(false);
    m_active_widget = widget;
    if (m_active_widget) {
        m_active_widget->set_relative_rect(child_rect_for_size(size()));
        m_active_widget->set_visible(true);
    }

    update(bar_rect());
}

void GTabWidget::resize_event(GResizeEvent& event)
{
    if (!m_active_widget)
        return;
    m_active_widget->set_relative_rect(child_rect_for_size(event.size()));
}

Rect GTabWidget::child_rect_for_size(const Size& size) const
{
    return { { 0, bar_height() }, { size.width(), size.height() - bar_height() } };
}

void GTabWidget::child_event(CChildEvent& event)
{
    if (!event.child() || !event.child()->is_widget())
        return GWidget::child_event(event);
    auto& child = static_cast<GWidget&>(*event.child());
    if (event.type() == GEvent::ChildAdded) {
        if (!m_active_widget)
            set_active_widget(&child);
        else if (m_active_widget != &child)
            child.set_visible(false);
    } else if (event.type() == GEvent::ChildRemoved) {
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
    GWidget::child_event(event);
}

Rect GTabWidget::bar_rect() const
{
    return { 0, 0, width(), bar_height() };
}

void GTabWidget::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect(bar_rect(), Color::MidGray);

    for (int i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        StylePainter::paint_button(painter, button_rect, ButtonStyle::Normal, m_tabs[i].widget == m_active_widget);
        painter.draw_text(button_rect, m_tabs[i].title, TextAlignment::Center);
    }
}

Rect GTabWidget::button_rect(int index) const
{
    int x_offset = 0;
    for (int i = 0; i < index; ++i)
        x_offset += m_tabs[i].width(font());
    return { x_offset, 0, m_tabs[index].width(font()), bar_height() };
}

int GTabWidget::TabData::width(const Font& font) const
{
    return 16 + font.width(title);
}

void GTabWidget::mousedown_event(GMouseEvent& event)
{
    for (int i = 0; i < m_tabs.size(); ++i) {
        auto button_rect = this->button_rect(i);
        if (!button_rect.contains(event.position()))
            continue;
        set_active_widget(m_tabs[i].widget);
    }
}
