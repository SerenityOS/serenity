#include "CellWidget.h"
#include "BoardWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/FontDatabase.h>

CellWidget::CellWidget(BoardWidget *board, int real_index) :
    m_board { board },
    background_color_for_cell { m_board->get_background_color_for_cell() }, text_color_for_cell { m_board->get_text_color_for_cell() },
    m_real_index { real_index }, m_current_index { real_index }
{
    set_font(Gfx::FontDatabase::default_font().bold_variant());

    position_cell();
    resize_cell();

    cons = m_board->on_cell_color_changed += [this](auto &&new_cell_color) {
        background_color_for_cell = new_cell_color;
        update();
    };

    cons = m_board->on_cell_text_color_changed += [this](auto &&new_cell_text_color) {
        text_color_for_cell = new_cell_text_color;
        update();
    };

    cons = m_board->on_cell_size_changed += [this](auto &&) {
        position_cell();
        resize_cell();
    };
}

CellWidget::~CellWidget()
{
}

void CellWidget::paint_event(GUI::PaintEvent &event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.clear_rect(event.rect(), background_color_for_cell);
    painter.draw_text(rect(), String::formatted("{}", m_real_index + 1), Gfx::TextAlignment::Center, text_color_for_cell, Gfx::TextElision::None, Gfx::TextWrapping::DontWrap);
    painter.draw_rect(rect(), Gfx::Color::NamedColor::Black, true);
}

void CellWidget::fire_on_cell_move_request()
{
    on_cell_move_request(m_current_index);
}

void CellWidget::mousedown_event(GUI::MouseEvent&)
{
    fire_on_cell_move_request();
}

void CellWidget::set_current_index(int current_index)
{
    m_current_index = current_index;

    position_cell();
}

bool CellWidget::is_in_place() const
{
    return m_current_index == m_real_index;
}

void CellWidget::position_cell()
{
    move_to(get_current_index_screen_position());
}

void CellWidget::resize_cell()
{
    auto cell_size { m_board->cell_size() };
    resize(cell_size, cell_size);
}

Gfx::IntPoint CellWidget::get_current_index_screen_position()
{
    return { int(m_current_index % m_board->columns()) * m_board->cell_size(), int(m_current_index / m_board->rows()) * m_board->cell_size() };
}
