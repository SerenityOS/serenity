#include <LibGUI/GTableView.h>
#include <LibGUI/GTableModel.h>
#include <LibGUI/GScrollBar.h>
#include <SharedGraphics/Painter.h>

GTableView::GTableView(GWidget* parent)
    : GWidget(parent)
{
    set_fill_with_background_color(false);
    m_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_scrollbar->set_step(4);
    m_scrollbar->set_big_step(30);
    m_scrollbar->on_change = [this] (int) {
        update();
    };
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
    m_scrollbar->set_relative_rect(event.size().width() - m_scrollbar->preferred_size().width(), 0, m_scrollbar->preferred_size().width(), event.size().height());
    update_scrollbar_range();
}

void GTableView::update_scrollbar_range()
{
    int excess_height = max(0, (item_count() * item_height()) - height());
    m_scrollbar->set_range(0, excess_height);
}

void GTableView::did_update_model()
{
    update_scrollbar_range();
    update();
}

Rect GTableView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), width(), item_height() };
}

void GTableView::mousedown_event(GMouseEvent& event)
{
    auto adjusted_position = event.position().translated(0, m_scrollbar->value());
    if (event.button() == GMouseButton::Left) {
        for (int i = 0; i < item_count(); ++i) {
            if (row_rect(i).contains(adjusted_position)) {
                m_model->set_selected_index({ i, 0 });
                update();
                return;
            }
        }
    }
    m_model->set_selected_index({ });
    update();
}

void GTableView::paint_event(GPaintEvent&)
{
    Painter painter(*this);

    painter.translate(0, -m_scrollbar->value());

    int horizontal_padding = 5;
    int painted_item_index = 0;

    int y_offset = header_height();

    for (int row_index = 0; row_index < m_model->row_count(); ++row_index) {
        int y = y_offset + painted_item_index * item_height();

        Color background_color;
        Color text_color;
        if (row_index == m_model->selected_index().row()) {
            background_color = Color::from_rgb(0x84351a);
            text_color = Color::White;
        } else {
            background_color = painted_item_index % 2 ? Color(210, 210, 210) : Color::White;
            text_color = Color::Black;
        }

        painter.fill_rect(row_rect(painted_item_index), background_color);

        int x_offset = 0;
        for (int column_index = 0; column_index < m_model->column_count(); ++column_index) {
            auto column_metadata = m_model->column_metadata(column_index);
            int column_width = column_metadata.preferred_width;
            Rect cell_rect(horizontal_padding + x_offset, y, column_width, item_height());
            painter.draw_text(cell_rect, m_model->data(row_index, column_index), column_metadata.text_alignment, text_color);
            x_offset += column_width + horizontal_padding * 2;
        }
        ++painted_item_index;
    };

    Rect unpainted_rect(0, painted_item_index * item_height(), width(), height());
    unpainted_rect.intersect(rect());
    painter.fill_rect(unpainted_rect, Color::White);

    // Untranslate the painter and paint the column headers.
    painter.translate(0, m_scrollbar->value());
    painter.fill_rect({ 0, 0, width(), header_height() }, Color::LightGray);
    int x_offset = 0;
    for (int column_index = 0; column_index < m_model->column_count(); ++column_index) {
        auto column_metadata = m_model->column_metadata(column_index);
        int column_width = column_metadata.preferred_width;
        Rect cell_rect(x_offset, 0, column_width + horizontal_padding * 2, item_height());
        painter.draw_text(cell_rect.translated(horizontal_padding, 0), m_model->column_name(column_index), TextAlignment::CenterLeft, Color::Black);
        x_offset += column_width + horizontal_padding * 2;
        painter.draw_line(cell_rect.top_left(), cell_rect.bottom_left(), Color::White);
        painter.draw_line(cell_rect.top_right(), cell_rect.bottom_right(), Color::DarkGray);
    }
    painter.draw_line({ 0, 0 }, { width() - 1, 0 }, Color::White);
    painter.draw_line({ 0, header_height() - 1 }, { width() - 1, header_height() - 1 }, Color::DarkGray);
}

int GTableView::item_count() const
{
    return m_model->row_count();
}
