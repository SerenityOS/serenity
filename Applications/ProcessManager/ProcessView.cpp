#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <AK/FileSystemPath.h>
#include <AK/HashMap.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibGUI/GScrollBar.h>
#include "ProcessTableModel.h"
#include "ProcessView.h"

static HashMap<unsigned, String>* s_usernames;

ProcessView::ProcessView(GWidget* parent)
    : GWidget(parent)
{
    m_process_icon = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, "/res/icons/gear16.rgb", { 16, 16 });

    m_scrollbar = new GScrollBar(Orientation::Vertical, this);
    m_scrollbar->set_step(4);
    m_scrollbar->set_big_step(30);
    m_scrollbar->on_change = [this] (int) {
        update();
    };

    m_model = make<ProcessTableModel>();

    start_timer(1000);
    reload();
}

ProcessView::~ProcessView()
{
}

void ProcessView::timer_event(GTimerEvent&)
{
    reload();
}

void ProcessView::resize_event(GResizeEvent& event)
{
    m_scrollbar->set_relative_rect(event.size().width() - m_scrollbar->preferred_size().width(), 0, m_scrollbar->preferred_size().width(), event.size().height());
}

void ProcessView::reload()
{
    m_model->update();

    int excess_height = max(0, (item_count() * item_height()) - height());
    m_scrollbar->set_range(0, excess_height);

    set_status_message(String::format("%d processes", item_count()));
    update();
}

Rect ProcessView::row_rect(int item_index) const
{
    return { 0, header_height() + (item_index * item_height()), width(), item_height() };
}

void ProcessView::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left) {
        for (int i = 0; i < item_count(); ++i) {
            if (!row_rect(i).contains(event.position()))
                continue;
            m_model->set_selected_index({ i, 0 });
            update();
        }
    }
}

void ProcessView::paint_event(GPaintEvent&)
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
            Rect cell_rect(horizontal_padding + x_offset, y, m_model->column_width(column_index), item_height());
            painter.draw_text(cell_rect, m_model->data(row_index, column_index), TextAlignment::CenterLeft, text_color);
            x_offset += m_model->column_width(column_index) + horizontal_padding;
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
        Rect cell_rect(horizontal_padding + x_offset, 0, m_model->column_width(column_index), item_height());
        painter.draw_text(cell_rect, m_model->column_name(column_index), TextAlignment::CenterLeft, Color::Black);
        x_offset += m_model->column_width(column_index) + horizontal_padding;
    }
    painter.draw_line({ 0, 0 }, { width() - 1, 0 }, Color::White);
    painter.draw_line({ 0, header_height() - 1 }, { width() - 1, header_height() - 1 }, Color::DarkGray);
}

void ProcessView::set_status_message(String&& message)
{
    if (on_status_message)
        on_status_message(move(message));
}

int ProcessView::item_count() const
{
    return m_model->row_count();
}

pid_t ProcessView::selected_pid() const
{
    return m_model->selected_pid();
}
