#include <LibGUI/GScrollableWidget.h>
#include <LibGUI/GScrollBar.h>

GScrollableWidget::GScrollableWidget(GWidget* parent)
    : GFrame(parent)
{
    m_vertical_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_vertical_scrollbar->set_step(4);
    m_vertical_scrollbar->on_change = [this] (int) {
        did_scroll();
        update();
    };

    m_horizontal_scrollbar = new GScrollBar(Orientation::Horizontal, this);
    m_horizontal_scrollbar->set_step(4);
    m_horizontal_scrollbar->set_big_step(30);
    m_horizontal_scrollbar->on_change = [this] (int) {
        did_scroll();
        update();
    };

    m_corner_widget = new GWidget(this);
    m_corner_widget->set_fill_with_background_color(true);
}

GScrollableWidget::~GScrollableWidget()
{
}

void GScrollableWidget::mousewheel_event(GMouseEvent& event)
{
    // FIXME: The wheel delta multiplier should probably come from... somewhere?
    vertical_scrollbar().set_value(vertical_scrollbar().value() + event.wheel_delta() * 20);
}

void GScrollableWidget::resize_event(GResizeEvent& event)
{
    auto inner_rect = frame_inner_rect_for_size(event.size());
    update_scrollbar_ranges();

    int height_wanted_by_horizontal_scrollbar = m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->preferred_size().height() : 0;
    int width_wanted_by_vertical_scrollbar = m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->preferred_size().width() : 0;

    m_vertical_scrollbar->set_relative_rect(inner_rect.right() + 1 - m_vertical_scrollbar->preferred_size().width(), inner_rect.top(), m_vertical_scrollbar->preferred_size().width(), inner_rect.height() - height_wanted_by_horizontal_scrollbar);
    m_horizontal_scrollbar->set_relative_rect(inner_rect.left(), inner_rect.bottom() + 1 - m_horizontal_scrollbar->preferred_size().height(), inner_rect.width() - m_vertical_scrollbar->preferred_size().width(), width_wanted_by_vertical_scrollbar);

    m_corner_widget->set_visible(m_vertical_scrollbar->is_visible() && m_horizontal_scrollbar->is_visible());
    if (m_corner_widget->is_visible()) {
        Rect corner_rect { m_horizontal_scrollbar->relative_rect().right() + 1, m_vertical_scrollbar->relative_rect().bottom() + 1, width_occupied_by_vertical_scrollbar(), height_occupied_by_horizontal_scrollbar() };
        m_corner_widget->set_relative_rect(corner_rect);
    }
}

Size GScrollableWidget::available_size() const
{
    int available_width = frame_inner_rect().width() - m_size_occupied_by_fixed_elements.width() - width_occupied_by_vertical_scrollbar();
    int available_height = frame_inner_rect().height() - m_size_occupied_by_fixed_elements.height() - height_occupied_by_horizontal_scrollbar();
    return { available_width, available_height };
}

void GScrollableWidget::update_scrollbar_ranges()
{
    auto available_size = this->available_size();

    int excess_height = max(0, m_content_size.height() - available_size.height());
    m_vertical_scrollbar->set_range(0, excess_height);

    int excess_width = max(0, m_content_size.width() - available_size.width());
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

int GScrollableWidget::height_occupied_by_horizontal_scrollbar() const
{
    return m_horizontal_scrollbar->is_visible() ? m_horizontal_scrollbar->height() : 0;
}

int GScrollableWidget::width_occupied_by_vertical_scrollbar() const
{
    return m_vertical_scrollbar->is_visible() ? m_vertical_scrollbar->width() : 0;
}

Rect GScrollableWidget::visible_content_rect() const
{
    return {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        min(m_content_size.width(), frame_inner_rect().width() - width_occupied_by_vertical_scrollbar() - m_size_occupied_by_fixed_elements.width()),
        min(m_content_size.height(), frame_inner_rect().height() - height_occupied_by_horizontal_scrollbar() - m_size_occupied_by_fixed_elements.height())
    };
}

void GScrollableWidget::scroll_into_view(const Rect& rect, Orientation orientation)
{
    if (orientation == Orientation::Vertical)
        return scroll_into_view(rect, false, true);
    return scroll_into_view(rect, true, false);
}

void GScrollableWidget::scroll_into_view(const Rect& rect, bool scroll_horizontally, bool scroll_vertically)
{
    auto visible_content_rect = this->visible_content_rect();
    if (visible_content_rect.contains(rect))
        return;

    if (scroll_vertically) {
        if (rect.top() < visible_content_rect.top())
            m_vertical_scrollbar->set_value(rect.top());
        else if (rect.bottom() > visible_content_rect.bottom())
            m_vertical_scrollbar->set_value(rect.bottom() - visible_content_rect.height());
    }
    if (scroll_horizontally) {
        if (rect.left() < visible_content_rect.left())
            m_horizontal_scrollbar->set_value(rect.left());
        else if (rect.right() > visible_content_rect.right())
            m_horizontal_scrollbar->set_value(rect.right() - visible_content_rect.width());
    }
}

void GScrollableWidget::set_scrollbars_enabled(bool scrollbars_enabled)
{
    if (m_scrollbars_enabled == scrollbars_enabled)
        return;
    m_scrollbars_enabled = scrollbars_enabled;
    m_vertical_scrollbar->set_visible(m_scrollbars_enabled);
    m_horizontal_scrollbar->set_visible(m_scrollbars_enabled);
    m_corner_widget->set_visible(m_scrollbars_enabled);
}

void GScrollableWidget::scroll_to_top()
{
    scroll_into_view({ 0, 0, 1, 1 }, Orientation::Vertical);
}

void GScrollableWidget::scroll_to_bottom()
{
    scroll_into_view({ 0, content_height(), 1, 1 }, Orientation::Vertical);
}

Rect GScrollableWidget::widget_inner_rect() const
{
    auto rect = frame_inner_rect();
    rect.set_width(rect.width() - width_occupied_by_vertical_scrollbar());
    rect.set_height(rect.height() - height_occupied_by_horizontal_scrollbar());
    return rect;
}
