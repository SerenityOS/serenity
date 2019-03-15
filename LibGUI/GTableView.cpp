#include <LibGUI/GTableView.h>
#include <LibGUI/GTableModel.h>
#include <LibGUI/GScrollBar.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>

GTableView::GTableView(GWidget* parent)
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

GTableView::~GTableView()
{
}

void GTableView::set_model(OwnPtr<GTableModel>&& model)
{
    if (model.ptr() == m_model.ptr())
        return;
    if (m_model)
        m_model->unregister_view(Badge<GTableView>(), *this);
    m_model = move(model);
    if (m_model)
        m_model->register_view(Badge<GTableView>(), *this);
}

void GTableView::resize_event(GResizeEvent& event)
{
    update_scrollbar_ranges();
    m_vertical_scrollbar->set_relative_rect(event.size().width() - m_vertical_scrollbar->preferred_size().width(), 0, m_vertical_scrollbar->preferred_size().width(), event.size().height() - m_horizontal_scrollbar->preferred_size().height());
    m_horizontal_scrollbar->set_relative_rect(0, event.size().height() - m_horizontal_scrollbar->preferred_size().height(), event.size().width() - m_vertical_scrollbar->preferred_size().width(), m_horizontal_scrollbar->preferred_size().height());
    m_corner_widget->set_relative_rect(m_horizontal_scrollbar->rect().right() + 1, m_vertical_scrollbar->rect().bottom() + 1, m_horizontal_scrollbar->height(), m_vertical_scrollbar->width());
}

void GTableView::update_scrollbar_ranges()
{
    int available_height = height() - header_height() - m_horizontal_scrollbar->height();
    int excess_height = max(0, (item_count() * item_height()) - available_height);
    m_vertical_scrollbar->set_range(0, excess_height);

    int available_width = width() - m_vertical_scrollbar->width();
    int excess_width = max(0, content_width() - available_width);
    m_horizontal_scrollbar->set_range(0, excess_width);

    m_vertical_scrollbar->set_big_step(visible_content_rect().height() - item_height());
}

int GTableView::content_width() const
{
    if (!m_model)
        return 0;
    int width = 0;
    int column_count = m_model->column_count();
    for (int i = 0; i < column_count; ++i)
        width += m_model->column_metadata(i).preferred_width + horizontal_padding() * 2;
    return width;
}

void GTableView::model_notification(const GModelNotification&)
{
}

void GTableView::did_update_model()
{
    update_scrollbar_ranges();
    update();
    model_notification(GModelNotification(GModelNotification::ModelUpdated));
}

Rect GTableView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), max(content_width(), width()), item_height() };
}

int GTableView::column_width(int column_index) const
{
    return m_model->column_metadata(column_index).preferred_width;
}

Rect GTableView::header_rect(int column_index) const
{
    int x_offset = 0;
    for (int i = 0; i < column_index; ++i)
        x_offset += column_width(i) + horizontal_padding() * 2;
    auto column_metadata = m_model->column_metadata(column_index);
    int column_width = column_metadata.preferred_width;
    return { x_offset, 0, column_width + horizontal_padding() * 2, header_height() };
}

void GTableView::mousedown_event(GMouseEvent& event)
{
    if (event.y() < header_height()) {
        auto adjusted_position = event.position().translated(m_horizontal_scrollbar->value(), 0);
        for (int i = 0; i < m_model->column_count(); ++i) {
            auto header_rect = this->header_rect(i);
            if (header_rect.contains(adjusted_position)) {
                auto new_sort_order = GSortOrder::Ascending;
                if (m_model->key_column() == i)
                    new_sort_order = m_model->sort_order() == GSortOrder::Ascending
                        ? GSortOrder::Descending
                        : GSortOrder::Ascending;
                m_model->set_key_column_and_sort_order(i, new_sort_order);
                return;
            }
        }
        return;
    }

    if (event.button() == GMouseButton::Left) {
        auto adjusted_position = event.position().translated(0, m_vertical_scrollbar->value());
        for (int i = 0; i < item_count(); ++i) {
            if (row_rect(i).contains(adjusted_position)) {
                m_model->set_selected_index({ i, 0 });
                update();
                return;
            }
        }
        m_model->set_selected_index({ });
        update();
    }
}

void GTableView::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    painter.save();
    painter.translate(-m_horizontal_scrollbar->value(), -m_vertical_scrollbar->value());

    int exposed_width = max(content_width(), width());
    int painted_item_index = 0;
    int y_offset = header_height();

    for (int row_index = 0; row_index < m_model->row_count(); ++row_index) {
        int y = y_offset + painted_item_index * item_height();

        Color background_color;
        Color key_column_background_color;
        Color text_color;
        if (row_index == m_model->selected_index().row()) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
            key_column_background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
            text_color = Color::White;
        } else {
            background_color = painted_item_index % 2 ? Color(210, 210, 210) : Color::White;
            key_column_background_color = painted_item_index % 2 ? Color(190, 190, 190) : Color(235, 235, 235);
            text_color = Color::Black;
        }

        painter.fill_rect(row_rect(painted_item_index), background_color);
        int x_offset = 0;
        for (int column_index = 0; column_index < m_model->column_count(); ++column_index) {
            auto column_metadata = m_model->column_metadata(column_index);
            int column_width = column_metadata.preferred_width;
            const Font& font = column_metadata.font ? *column_metadata.font : this->font();
            bool is_key_column = m_model->key_column() == column_index;
            Rect cell_rect(horizontal_padding() + x_offset, y, column_width, item_height());
            if (is_key_column) {
                auto cell_rect_for_fill = cell_rect.inflated(horizontal_padding() * 2, 0);
                painter.fill_rect(cell_rect_for_fill, key_column_background_color);
            }
            auto data = m_model->data({ row_index, column_index });
            if (data.is_bitmap())
                painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
            else
                painter.draw_text(cell_rect, data.to_string(), font, column_metadata.text_alignment, text_color);
            x_offset += column_width + horizontal_padding() * 2;
        }
        ++painted_item_index;
    };

    Rect unpainted_rect(0, header_height() + painted_item_index * item_height(), exposed_width, height());
    painter.fill_rect(unpainted_rect, Color::White);

    // Untranslate the painter vertically and do the column headers.
    painter.translate(0, m_vertical_scrollbar->value());
    if (headers_visible())
        paint_headers(painter);

    painter.restore();

    if (is_focused()) {
        Rect item_area_rect {
            0,
            header_height(),
            width() - m_vertical_scrollbar->width(),
            height() - header_height() - m_horizontal_scrollbar->height()
        };
        painter.draw_rect(item_area_rect, Color::from_rgb(0x84351a));
    };
}

void GTableView::paint_headers(Painter& painter)
{
    int exposed_width = max(content_width(), width());
    painter.fill_rect({ 0, 0, exposed_width, header_height() }, Color::LightGray);
    painter.draw_line({ 0, 0 }, { exposed_width - 1, 0 }, Color::White);
    painter.draw_line({ 0, header_height() - 1 }, { exposed_width - 1, header_height() - 1 }, Color::DarkGray);
    int x_offset = 0;
    for (int column_index = 0; column_index < m_model->column_count(); ++column_index) {
        auto column_metadata = m_model->column_metadata(column_index);
        int column_width = column_metadata.preferred_width;
        bool is_key_column = m_model->key_column() == column_index;
        Rect cell_rect(x_offset, 0, column_width + horizontal_padding() * 2, header_height());
        if (is_key_column) {
            painter.fill_rect(cell_rect.shrunken(2, 2), Color::from_rgb(0xdddddd));
        }
        painter.draw_text(cell_rect.translated(horizontal_padding(), 0), m_model->column_name(column_index), Font::default_bold_font(), TextAlignment::CenterLeft, Color::Black);
        x_offset += column_width + horizontal_padding() * 2;
        // Draw column separator.
        painter.draw_line(cell_rect.top_left().translated(0, 1), cell_rect.bottom_left().translated(0, -1), Color::White);
        painter.draw_line(cell_rect.top_right(), cell_rect.bottom_right().translated(0, -1), Color::DarkGray);
    }
    // Draw the "start" of a new column to make the last separator look right.
    painter.draw_line({ x_offset, 1 }, { x_offset, header_height() - 2 }, Color::White);
}

int GTableView::item_count() const
{
    return m_model->row_count();
}

void GTableView::keydown_event(GKeyEvent& event)
{
    if (!model())
        return;
    auto& model = *this->model();
    if (event.key() == KeyCode::Key_Return) {
        model.activate(model.selected_index());
        return;
    }
    if (event.key() == KeyCode::Key_Up) {
        GModelIndex new_index;
        if (model.selected_index().is_valid())
            new_index = { model.selected_index().row() - 1, model.selected_index().column() };
        else
            new_index = { 0, 0 };
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_Down) {
        GModelIndex new_index;
        if (model.selected_index().is_valid())
            new_index = { model.selected_index().row() + 1, model.selected_index().column() };
        else
            new_index = { 0, 0 };
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageUp) {
        int items_per_page = visible_content_rect().height() / item_height();
        GModelIndex new_index(max(0, model.selected_index().row() - items_per_page), model.selected_index().column());
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    if (event.key() == KeyCode::Key_PageDown) {
        int items_per_page = visible_content_rect().height() / item_height();
        GModelIndex new_index(min(model.row_count() - 1, model.selected_index().row() + items_per_page), model.selected_index().column());
        if (model.is_valid(new_index)) {
            model.set_selected_index(new_index);
            scroll_into_view(new_index, Orientation::Vertical);
            update();
        }
        return;
    }
    return GWidget::keydown_event(event);
}

Rect GTableView::visible_content_rect() const
{
    return {
        m_horizontal_scrollbar->value(),
        m_vertical_scrollbar->value(),
        width() - m_vertical_scrollbar->width(),
        height() - header_height() - m_horizontal_scrollbar->height()
    };
}

void GTableView::scroll_into_view(const GModelIndex& index, Orientation orientation)
{
    auto visible_content_rect = this->visible_content_rect();
    auto rect = row_rect(index.row()).translated(0, -header_height());

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

