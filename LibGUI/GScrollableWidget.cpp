#include <LibGUI/GScrollableWidget.h>
#include <LibGUI/GScrollBar.h>

GScrollableWidget::GScrollableWidget(GWidget* parent)
    : GWidget(parent)
{
    m_vertical_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_vertical_scrollbar->set_step(4);
    m_vertical_scrollbar->on_change = [this] (int) {
        update();
    };

    m_horizontal_scrollbar = new GScrollBar(Orientation::Horizontal, this);
    m_horizontal_scrollbar->set_step(4);
    m_horizontal_scrollbar->set_big_step(30);
    m_horizontal_scrollbar->on_change = [this] (int) {
        update();
    };

    m_corner_widget = new GWidget(this);
    m_corner_widget->set_fill_with_background_color(true);
}

GScrollableWidget::~GScrollableWidget()
{
}

void GScrollableWidget::resize_event(GResizeEvent& event)
{
    update_scrollbar_ranges();

    m_vertical_scrollbar->set_relative_rect(event.size().width() - m_vertical_scrollbar->preferred_size().width(), 0, m_vertical_scrollbar->preferred_size().width(), event.size().height() - m_horizontal_scrollbar->preferred_size().height());
    m_horizontal_scrollbar->set_relative_rect(0, event.size().height() - m_horizontal_scrollbar->preferred_size().height(), event.size().width() - m_vertical_scrollbar->preferred_size().width(), m_horizontal_scrollbar->preferred_size().height());

    m_corner_widget->set_visible(m_vertical_scrollbar->is_visible() && m_horizontal_scrollbar->is_visible());
    if (m_corner_widget->is_visible()) {
        Rect corner_rect { m_horizontal_scrollbar->rect().right() + 1, m_vertical_scrollbar->rect().bottom() + 1, m_horizontal_scrollbar->height(), m_vertical_scrollbar->width() };
        m_corner_widget->set_relative_rect(corner_rect);
    }
}

void GScrollableWidget::update_scrollbar_ranges()
{
    int available_height = height() - m_size_occupied_by_fixed_elements.height() - m_horizontal_scrollbar->height();
    int excess_height = max(0, m_content_size.height() - available_height);
    m_vertical_scrollbar->set_range(0, excess_height);

    int available_width = width() - m_size_occupied_by_fixed_elements.width() - m_vertical_scrollbar->width();
    int excess_width = max(0, m_content_size.width() - available_width);
    m_horizontal_scrollbar->set_range(0, excess_width);

    m_vertical_scrollbar->set_big_step(visible_content_rect().height() - m_vertical_scrollbar->step());
}

void GScrollableWidget::set_content_size(const Size& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    update_scrollbar_ranges();
}

void GScrollableWidget::set_size_occupied_by_fixed_elements(const Size& size)
{
    if (m_size_occupied_by_fixed_elements == size)
        return;
    m_size_occupied_by_fixed_elements = size;
    update_scrollbar_ranges();
}

Rect GScrollableWidget::visible_content_rect() const
{
    return {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        width() - m_vertical_scrollbar->width(),
        height() - m_size_occupied_by_fixed_elements.height() - m_horizontal_scrollbar->height()
    };
}

void GScrollableWidget::scroll_into_view(const Rect& rect, Orientation orientation)
{
    auto visible_content_rect = this->visible_content_rect();
    if (visible_content_rect.contains(rect))
        return;

    if (orientation == Orientation::Vertical) {
        if (rect.top() < visible_content_rect.top())
            m_vertical_scrollbar->set_value(rect.top());
        else if (rect.bottom() > visible_content_rect.bottom())
            m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height());
    } else {
        if (rect.left() < visible_content_rect.left())
            m_horizontal_scrollbar->set_value(rect.left());
        else if (rect.right() > visible_content_rect.right())
            m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width());
    }
}
