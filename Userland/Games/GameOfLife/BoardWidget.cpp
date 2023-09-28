/*
 * Copyright (c) 2021, Andres Crucitti <dasc495@gmail.com>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Ryan Wilson <ryan@rdwilson.xyz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BoardWidget.h"
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>

BoardWidget::BoardWidget(size_t rows, size_t columns)
    : m_board(make<Board>(rows, columns))
{
    m_timer = add<Core::Timer>();
    m_pattern_preview_timer = add<Core::Timer>();
    m_timer->stop();
    m_pattern_preview_timer->stop();
    m_timer->on_timeout = [this] {
        run_generation();
    };
    m_pattern_preview_timer->on_timeout = [this] {
        update();
    };
    m_timer->set_interval(m_running_timer_interval);
    m_pattern_preview_timer->set_interval(m_running_pattern_preview_timer_interval);

    on_pattern_selection = [this](auto* pattern) {
        m_selected_pattern = pattern;
        if (on_pattern_selection_state_change)
            on_pattern_selection_state_change();
    };

    setup_patterns();
}

void BoardWidget::run_generation()
{
    m_board->run_generation();
    if (!m_board->is_stalled())
        m_ticks++;

    if (on_tick)
        on_tick(m_ticks);

    update();
    if (m_board->is_stalled()) {
        if (on_stall)
            on_stall();
        update();
    };
}

void BoardWidget::resize_board(size_t rows, size_t columns)
{
    if (columns == m_board->columns() && rows == m_board->rows())
        return;
    m_board->resize(rows, columns);
    m_last_cell_toggled = { rows, columns };
    set_min_size(columns, rows);
}

void BoardWidget::set_running_timer_interval(int interval)
{
    if (is_running())
        return;

    m_running_timer_interval = interval;
    m_timer->set_interval(m_running_timer_interval);

    if (on_running_state_change)
        on_running_state_change();
}

void BoardWidget::set_running(bool running)
{
    if (running == m_running)
        return;

    clear_selected_pattern();

    m_running = running;

    if (m_running) {
        m_timer->start();
    } else {
        m_timer->stop();
    }

    if (on_running_state_change)
        on_running_state_change();

    update();
}

void BoardWidget::toggle_cell(size_t row, size_t column)
{
    if (m_running || !m_toggling_cells || (m_last_cell_toggled.row == row && m_last_cell_toggled.column == column))
        return;

    m_ticks = 0;

    m_last_cell_toggled = { row, column };
    m_board->toggle_cell(row, column);

    if (on_cell_toggled)
        on_cell_toggled(m_board, row, column);

    update();
}

void BoardWidget::clear_cells()
{
    m_ticks = 0;
    m_board->clear();
}

void BoardWidget::randomize_cells()
{
    m_ticks = 0;
    m_board->randomize();
}

int BoardWidget::get_cell_size() const
{
    int width = rect().width() / m_board->columns();
    int height = rect().height() / m_board->rows();

    return min(width, height);
}

Gfx::IntSize BoardWidget::get_board_offset() const
{
    int cell_size = get_cell_size();
    return {
        (width() - cell_size * m_board->columns()) / 2,
        (height() - cell_size * m_board->rows()) / 2,
    };
}

void BoardWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Black);

    int cell_size = get_cell_size();
    Gfx::IntSize board_offset = get_board_offset();

    for (size_t row = 0; row < m_board->rows(); ++row) {
        for (size_t column = 0; column < m_board->columns(); ++column) {
            int cell_x = column * cell_size + board_offset.width();
            int cell_y = row * cell_size + board_offset.height();

            Gfx::Rect cell_rect(cell_x, cell_y, cell_size, cell_size);

            Color border_color = Color::DarkGray;
            Color fill_color;

            bool on = m_board->cell(row, column);
            if (on) {
                fill_color = Color::from_rgb(0xdcdc50);
            } else {
                fill_color = Color::MidGray;
            }

            if (m_selected_pattern != nullptr) {
                int y_offset = 0;
                for (auto const& line : m_selected_pattern->pattern()) {
                    int x_offset = 0;
                    for (auto c : line.bytes_as_string_view()) {
                        if (c == 'O' && (m_last_cell_hovered.row + y_offset) < m_board->rows()
                            && (m_last_cell_hovered.column + x_offset) < m_board->columns() && row == (m_last_cell_hovered.row + y_offset) && column == (m_last_cell_hovered.column + x_offset))
                            fill_color = Color::Green;
                        x_offset++;
                    }
                    y_offset++;
                }
            }

            painter.fill_rect(cell_rect, fill_color);
            if (cell_size > 4) {
                painter.draw_rect(cell_rect, border_color);
            }
        }
    }
}

void BoardWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() == GUI::MouseButton::Primary) {
        m_dragging_enabled = (m_selected_pattern == nullptr);
        set_toggling_cells(true);
        auto row_and_column = get_row_and_column_for_point(event.x(), event.y());
        if (!row_and_column.has_value())
            return;
        auto [row, column] = row_and_column.value();

        if (m_selected_pattern) {
            place_pattern(row, column);
            if (!event.ctrl()) {
                clear_selected_pattern();
            }
        } else {
            toggle_cell(row, column);
        }
    }
}

void BoardWidget::keydown_event(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape) {
        clear_selected_pattern();
        update();
        return;
    }

    event.ignore();
}

void BoardWidget::context_menu_event(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();

        auto insert_pattern_menu = m_context_menu->add_submenu("&Insert Pattern"_string);
        for_each_pattern([&](auto& pattern) {
            if (pattern.action())
                insert_pattern_menu->add_action(*pattern.action());
        });
    }
    if (!m_running)
        m_context_menu->popup(event.screen_position());
}

void BoardWidget::mousemove_event(GUI::MouseEvent& event)
{
    auto row_and_column = get_row_and_column_for_point(event.x(), event.y());
    if (!row_and_column.has_value())
        return;
    auto [row, column] = row_and_column.value();
    if (m_toggling_cells && m_dragging_enabled) {
        if (m_last_cell_toggled.row != row || m_last_cell_toggled.column != column)
            toggle_cell(row, column);
    }
    m_last_cell_hovered = { row, column };
    if (m_selected_pattern != nullptr) {
        if (!m_pattern_preview_timer->is_active())
            m_pattern_preview_timer->start();
    }
}

void BoardWidget::mouseup_event(GUI::MouseEvent&)
{
    set_toggling_cells(false);
    m_dragging_enabled = true;
}

Optional<Board::RowAndColumn> BoardWidget::get_row_and_column_for_point(int x, int y) const
{
    auto board_offset = get_board_offset();
    auto cell_size = get_cell_size();
    auto board_width = m_board->columns() * cell_size;
    auto board_height = m_board->rows() * cell_size;
    if (x <= board_offset.width() || static_cast<size_t>(x) >= board_offset.width() + board_width)
        return {};
    if (y <= board_offset.height() || static_cast<size_t>(y) >= board_offset.height() + board_height)
        return {};
    return { {
        .row = static_cast<size_t>((y - board_offset.height()) / cell_size),
        .column = static_cast<size_t>((x - board_offset.width()) / cell_size),
    } };
}

void BoardWidget::place_pattern(size_t row, size_t column)
{
    int y_offset = 0;
    for (auto const& line : m_selected_pattern->pattern()) {
        int x_offset = 0;
        for (auto c : line.bytes_as_string_view()) {
            if (c == 'O' && (row + y_offset) < m_board->rows() && (column + x_offset) < m_board->columns())
                toggle_cell(row + y_offset, column + x_offset);
            x_offset++;
        }
        y_offset++;
    }
}

void BoardWidget::clear_selected_pattern()
{
    if (!m_selected_pattern)
        return;

    m_selected_pattern = nullptr;
    if (on_pattern_selection_state_change)
        on_pattern_selection_state_change();

    if (m_pattern_preview_timer->is_active())
        m_pattern_preview_timer->stop();
}

void BoardWidget::setup_patterns()
{
    auto add_pattern = [&](auto name, NonnullOwnPtr<Pattern> pattern) {
        auto action = GUI::Action::create(name, [this, pattern = pattern.ptr()](const GUI::Action&) {
            on_pattern_selection(pattern);
        });
        pattern->set_action(action);
        m_patterns.append(move(pattern));
    };

    Vector<String> blinker = {
        "OOO"_string
    };

    Vector<String> toad = {
        ".OOO"_string,
        "OOO."_string
    };

    Vector<String> glider = {
        ".O."_string,
        "..O"_string,
        "OOO"_string,
    };

    Vector<String> lightweight_spaceship = {
        ".OO.."_string,
        "OOOO."_string,
        "OO.OO"_string,
        "..OO."_string
    };

    Vector<String> middleweight_spaceship = {
        ".OOOOO"_string,
        "O....O"_string,
        ".....O"_string,
        "O...O."_string,
        "..O..."_string
    };

    Vector<String> heavyweight_spaceship = {
        "..OO..."_string,
        "O....O."_string,
        "......O"_string,
        "O.....O"_string,
        ".OOOOOO"_string
    };

    Vector<String> infinite_1 = { "OOOOOOOO.OOOOO...OOO......OOOOOOO.OOOOO"_string };

    Vector<String> infinite_2 = {
        "......O."_string,
        "....O.OO"_string,
        "....O.O."_string,
        "....O..."_string,
        "..O....."_string,
        "O.O....."_string
    };

    Vector<String> infinite_3 = {
        "OOO.O"_string,
        "O...."_string,
        "...OO"_string,
        ".OO.O"_string,
        "O.O.O"_string
    };

    Vector<String> simkin_glider_gun = {
        "OO.....OO........................"_string,
        "OO.....OO........................"_string,
        "................................."_string,
        "....OO..........................."_string,
        "....OO..........................."_string,
        "................................."_string,
        "................................."_string,
        "................................."_string,
        "................................."_string,
        "......................OO.OO......"_string,
        ".....................O.....O....."_string,
        ".....................O......O..OO"_string,
        ".....................OOO...O...OO"_string,
        "..........................O......"_string,
        "................................."_string,
        "................................."_string,
        "................................."_string,
        "....................OO..........."_string,
        "....................O............"_string,
        ".....................OOO........."_string,
        ".......................O........."_string
    };
    Vector<String> gosper_glider_gun = {
        "........................O..........."_string,
        "......................O.O..........."_string,
        "............OO......OO............OO"_string,
        "...........O...O....OO............OO"_string,
        "OO........O.....O...OO.............."_string,
        "OO........O...O.OO....O.O..........."_string,
        "..........O.....O.......O..........."_string,
        "...........O...O...................."_string,
        "............OO......................"_string
    };

    Vector<String> r_pentomino = {
        ".OO"_string,
        "OO."_string,
        ".O."_string
    };

    Vector<String> diehard = {
        "......O."_string,
        "OO......"_string,
        ".O...OOO"_string
    };

    Vector<String> acorn = {
        ".O....."_string,
        "...O..."_string,
        "OO..OOO"_string
    };

    add_pattern("Blinker", make<Pattern>(move(blinker)));
    add_pattern("Toad", make<Pattern>(move(toad)));
    add_pattern("Glider", make<Pattern>(move(glider)));
    add_pattern("Lightweight Spaceship", make<Pattern>(move(lightweight_spaceship)));
    add_pattern("Middleweight Spaceship", make<Pattern>(move(middleweight_spaceship)));
    add_pattern("Heavyweight Spaceship", make<Pattern>(move(heavyweight_spaceship)));
    add_pattern("Infinite 1", make<Pattern>(move(infinite_1)));
    add_pattern("Infinite 2", make<Pattern>(move(infinite_2)));
    add_pattern("Infinite 3", make<Pattern>(move(infinite_3)));
    add_pattern("R-Pentomino", make<Pattern>(move(r_pentomino)));
    add_pattern("Diehard", make<Pattern>(move(diehard)));
    add_pattern("Acorn", make<Pattern>(move(acorn)));
    add_pattern("Simkin's Glider Gun", make<Pattern>(move(simkin_glider_gun)));
    add_pattern("Gosper's Glider Gun", make<Pattern>(move(gosper_glider_gun)));
}
