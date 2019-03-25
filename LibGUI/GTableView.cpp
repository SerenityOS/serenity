#include <LibGUI/GTableView.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GScrollBar.h>
#include <SharedGraphics/Painter.h>
#include <Kernel/KeyCode.h>

GTableView::GTableView(GWidget* parent)
    : GAbstractView(parent)
{
}

GTableView::~GTableView()
{
}

void GTableView::update_content_size()
{
    if (!model())
        return set_content_size({ });

    int content_width = 0;
    int column_count = model()->column_count();
    for (int i = 0; i < column_count; ++i)
        content_width += model()->column_metadata(i).preferred_width + horizontal_padding() * 2;
    int content_height = item_count() * item_height();

    set_content_size({ content_width, content_height });
    set_size_occupied_by_fixed_elements({ 0, header_height() });
}

void GTableView::did_update_model()
{
    GAbstractView::did_update_model();
    update_content_size();
    update();
}

Rect GTableView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), max(content_size().width(), width()), item_height() };
}

int GTableView::column_width(int column_index) const
{
    return model()->column_metadata(column_index).preferred_width;
}

Rect GTableView::header_rect(int column_index) const
{
    if (is_column_hidden(column_index))
        return { };
    int x_offset = 0;
    for (int i = 0; i < column_index; ++i) {
        if (is_column_hidden(i))
            continue;
        x_offset += column_width(i) + horizontal_padding() * 2;
    }
    auto column_metadata = model()->column_metadata(column_index);
    int column_width = column_metadata.preferred_width;
    return { x_offset, 0, column_width + horizontal_padding() * 2, header_height() };
}

void GTableView::mousedown_event(GMouseEvent& event)
{
    if (event.y() < header_height()) {
        auto adjusted_position = event.position().translated(horizontal_scrollbar().value(), 0);
        for (int i = 0; i < model()->column_count(); ++i) {
            auto header_rect = this->header_rect(i);
            if (header_rect.contains(adjusted_position)) {
                auto new_sort_order = GSortOrder::Ascending;
                if (model()->key_column() == i)
                    new_sort_order = model()->sort_order() == GSortOrder::Ascending
                        ? GSortOrder::Descending
                        : GSortOrder::Ascending;
                model()->set_key_column_and_sort_order(i, new_sort_order);
                return;
            }
        }
        return;
    }

    if (event.button() == GMouseButton::Left) {
        auto adjusted_position = event.position().translated(0, vertical_scrollbar().value());
        for (int i = 0; i < item_count(); ++i) {
            if (row_rect(i).contains(adjusted_position)) {
                model()->set_selected_index({ i, 0 });
                update();
                return;
            }
        }
        model()->set_selected_index({ });
        update();
    }
}

void GTableView::paint_event(GPaintEvent& event)
{
    Painter painter(*this);
    painter.set_clip_rect(event.rect());
    painter.save();
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    int exposed_width = max(content_size().width(), width());
    int painted_item_index = 0;
    int y_offset = header_height();

    for (int row_index = 0; row_index < model()->row_count(); ++row_index) {
        bool is_selected_row = row_index == model()->selected_index().row();
        int y = y_offset + painted_item_index * item_height();

        Color background_color;
        Color key_column_background_color;
        if (is_selected_row) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
            key_column_background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
        } else {
            if (alternating_row_colors() && (painted_item_index % 2)) {
                background_color = Color(210, 210, 210);
                key_column_background_color = Color(190, 190, 190);
            } else {
                background_color = Color::White;
                key_column_background_color = Color(235, 235, 235);
            }
        }
        painter.fill_rect(row_rect(painted_item_index), background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < model()->column_count(); ++column_index) {
            if (is_column_hidden(column_index))
                continue;
            auto column_metadata = model()->column_metadata(column_index);
            int column_width = column_metadata.preferred_width;
            const Font& font = column_metadata.font ? *column_metadata.font : this->font();
            bool is_key_column = model()->key_column() == column_index;
            Rect cell_rect(horizontal_padding() + x_offset, y, column_width, item_height());
            if (is_key_column) {
                auto cell_rect_for_fill = cell_rect.inflated(horizontal_padding() * 2, 0);
                painter.fill_rect(cell_rect_for_fill, key_column_background_color);
            }
            GModelIndex cell_index(row_index, column_index);
            auto data = model()->data(cell_index);
            if (data.is_bitmap()) {
                painter.blit(cell_rect.location(), data.as_bitmap(), data.as_bitmap().rect());
            } else if (data.is_icon()) {
                if (auto bitmap = data.as_icon().bitmap_for_size(16))
                    painter.blit(cell_rect.location(), *bitmap, bitmap->rect());
            } else {
                Color text_color;
                if (is_selected_row)
                    text_color = Color::White;
                else
                    text_color = model()->data(cell_index, GModel::Role::ForegroundColor).to_color(Color::Black);
                painter.draw_text(cell_rect, data.to_string(), font, column_metadata.text_alignment, text_color);
            }
            x_offset += column_width + horizontal_padding() * 2;
        }
        ++painted_item_index;
    };

    Rect unpainted_rect(0, header_height() + painted_item_index * item_height(), exposed_width, height());
    painter.fill_rect(unpainted_rect, Color::White);

    // Untranslate the painter vertically and do the column headers.
    painter.translate(0, vertical_scrollbar().value());
    if (headers_visible())
        paint_headers(painter);

    painter.restore();

    if (is_focused()) {
        Rect item_area_rect {
            0,
            header_height(),
            width() - vertical_scrollbar().width(),
            height() - header_height() - horizontal_scrollbar().height()
        };
        painter.draw_rect(item_area_rect, Color::from_rgb(0x84351a));
    };
}

void GTableView::paint_headers(Painter& painter)
{
    int exposed_width = max(content_size().width(), width());
    painter.fill_rect({ 0, 0, exposed_width, header_height() }, Color::LightGray);
    painter.draw_line({ 0, 0 }, { exposed_width - 1, 0 }, Color::White);
    painter.draw_line({ 0, header_height() - 1 }, { exposed_width - 1, header_height() - 1 }, Color::DarkGray);
    int x_offset = 0;
    for (int column_index = 0; column_index < model()->column_count(); ++column_index) {
        if (is_column_hidden(column_index))
            continue;
        auto column_metadata = model()->column_metadata(column_index);
        int column_width = column_metadata.preferred_width;
        bool is_key_column = model()->key_column() == column_index;
        Rect cell_rect(x_offset, 0, column_width + horizontal_padding() * 2, header_height());
        if (is_key_column) {
            painter.fill_rect(cell_rect.shrunken(2, 2), Color::from_rgb(0xdddddd));
        }
        painter.draw_text(cell_rect.translated(horizontal_padding(), 0), model()->column_name(column_index), Font::default_bold_font(), TextAlignment::CenterLeft, Color::Black);
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
    return model()->row_count();
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

void GTableView::scroll_into_view(const GModelIndex& index, Orientation orientation)
{
    auto rect = row_rect(index.row()).translated(0, -header_height());
    GScrollableWidget::scroll_into_view(rect, orientation);
}

bool GTableView::is_column_hidden(int column) const
{
    if (column >= 0 && column < m_column_visibility.size())
        return !m_column_visibility[column];
    return false;
}

void GTableView::set_column_hidden(int column, bool hidden)
{
    ASSERT(column >= 0);
    if (m_column_visibility.size() <= column) {
        int previous_column_count = m_column_visibility.size();
        m_column_visibility.resize(column + 1);
        for (int i = previous_column_count; i < m_column_visibility.size(); ++i)
            m_column_visibility[i] = true;
    }
    m_column_visibility[column] = !hidden;
}

void GTableView::doubleclick_event(GMouseEvent& event)
{
    if (!model())
        return;
    if (event.button() == GMouseButton::Left) {
        mousedown_event(event);
        model()->activate(model()->selected_index());
    }
}
