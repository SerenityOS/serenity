#include <AK/StringBuilder.h>
#include <Kernel/KeyCode.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GScrollBar.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWindow.h>

GTableView::GTableView(GWidget* parent)
    : GAbstractColumnView(parent)
{
}

GTableView::~GTableView()
{
}

void GTableView::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);

    GPainter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), SystemColor::Base);
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(-horizontal_scrollbar().value(), -vertical_scrollbar().value());

    if (!model())
        return;

    int exposed_width = max(content_size().width(), width());
    int y_offset = header_height();

    bool dummy;
    int first_visible_row = index_at_event_position(frame_inner_rect().top_left(), dummy).row();
    int last_visible_row = index_at_event_position(frame_inner_rect().bottom_right(), dummy).row();

    if (first_visible_row == -1)
        first_visible_row = 0;
    if (last_visible_row == -1)
        last_visible_row = model()->row_count() - 1;

    int painted_item_index = first_visible_row;

    for (int row_index = first_visible_row; row_index <= last_visible_row; ++row_index) {
        bool is_selected_row = selection().contains_row(row_index);
        int y = y_offset + painted_item_index * item_height();

        Color background_color;
        Color key_column_background_color;
        if (is_selected_row) {
            background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
            key_column_background_color = is_focused() ? Color::from_rgb(0x84351a) : Color::from_rgb(0x606060);
        } else {
            if (alternating_row_colors() && (painted_item_index % 2)) {
                background_color = Color(220, 220, 220);
                key_column_background_color = Color(200, 200, 200);
            } else {
                background_color = SystemColor::Base;
                key_column_background_color = Color(220, 220, 220);
            }
        }
        painter.fill_rect(row_rect(painted_item_index), background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < model()->column_count(); ++column_index) {
            if (is_column_hidden(column_index))
                continue;
            auto column_metadata = model()->column_metadata(column_index);
            int column_width = this->column_width(column_index);
            const Font& font = column_metadata.font ? *column_metadata.font : this->font();
            bool is_key_column = model()->key_column() == column_index;
            Rect cell_rect(horizontal_padding() + x_offset, y, column_width, item_height());
            if (is_key_column) {
                auto cell_rect_for_fill = cell_rect.inflated(horizontal_padding() * 2, 0);
                painter.fill_rect(cell_rect_for_fill, key_column_background_color);
            }
            auto cell_index = model()->index(row_index, column_index);

            if (auto* delegate = column_data(column_index).cell_painting_delegate.ptr()) {
                delegate->paint(painter, cell_rect, *model(), cell_index);
            } else {
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
                        text_color = model()->data(cell_index, GModel::Role::ForegroundColor).to_color(SystemColor::WindowText);
                    painter.draw_text(cell_rect, data.to_string(), font, column_metadata.text_alignment, text_color, TextElision::Right);
                }
            }
            x_offset += column_width + horizontal_padding() * 2;
        }
        ++painted_item_index;
    };

    Rect unpainted_rect(0, header_height() + painted_item_index * item_height(), exposed_width, height());
    painter.fill_rect(unpainted_rect, SystemColor::Base);

    // Untranslate the painter vertically and do the column headers.
    painter.translate(0, vertical_scrollbar().value());
    if (headers_visible())
        paint_headers(painter);
}
