#include <Kernel/API/KeyCode.h>
#include <AK/StdLibExtras.h>
#include <LibConfig/Client.h>
#include "Utilities.h"
#include "BoardWidget.h"

BoardWidget::BoardWidget(int rows, int columns, int cell_size, Gfx::Color cell_color, Gfx::Color cell_text_color) :
    background_color_for_cell{ cell_color }, text_color_for_cell { cell_text_color },
    m_rows { rows }, m_columns { columns }, m_cell_size { cell_size }
{
    generate_cells();
}

BoardWidget::~BoardWidget()
{
}

void BoardWidget::keydown_event(GUI::KeyEvent &event)
{
    CellWidget *cell { nullptr };

    switch (event.key())
    {
    case KeyCode::Key_Left:
        if (int(m_empty_cell_index % m_columns) < m_columns - 1)
            cell = (*m_cells)[m_empty_cell_index + 1];
        break;

    case KeyCode::Key_Up:
        if (int(m_empty_cell_index / m_rows) < m_rows - 1)
            cell = (*m_cells)[m_empty_cell_index + m_rows];
        break;

    case KeyCode::Key_Right:
        if (int(m_empty_cell_index % m_columns) > 0)
            cell = (*m_cells)[m_empty_cell_index - 1];
        break;

    case KeyCode::Key_Down:
        if (int(m_empty_cell_index) / m_rows > 0)
            cell = (*m_cells)[m_empty_cell_index - m_rows];
        break;

    default: break;
    }

    if (cell != nullptr)
        cell->fire_on_cell_move_request();
}

void BoardWidget::generate_cells()
{
    remove_all_children();
    cons.connections.clear();

    m_cells = make<AK::FixedArray<CellWidget*>>(size_t(m_columns * m_rows));

    auto nr_of_cells { int((*m_cells).size()) - 1 };

    for (auto idx { 0 }; idx < nr_of_cells; ++idx) {
        auto &cell = add<CellWidget>(this, idx);
        (*m_cells)[idx] = &cell;

        cons = cell.on_cell_move_request += [this, last_index = nr_of_cells](int current_cell_index) {
            auto diff = abs(current_cell_index - m_empty_cell_index);

            if (diff != m_columns && diff != 1) return;

            swap((*m_cells)[current_cell_index], (*m_cells)[m_empty_cell_index]);
            swap(current_cell_index, m_empty_cell_index);

            (*m_cells)[current_cell_index]->set_current_index(current_cell_index);
            (*m_cells)[current_cell_index]->position_cell();

            on_cell_moved();

            if (m_empty_cell_index == last_index && AK::all_of(m_cells->begin(), m_cells->end() - 1, [](auto &&cell) { return cell->is_in_place(); })) {
                // solved
                on_solved(m_rows, m_columns);
                shuffle_cells();
            }
        };
    }

    m_empty_cell_index = nr_of_cells;

    shuffle_cells();
}

void BoardWidget::shuffle_cells()
{
    auto nr_of_cells { int((*m_cells).size()) - 1 };

    shuffle(m_cells->data(), nr_of_cells);
    ensure_puzzle_is_solvable();

    for (auto idx { 0 }; idx < nr_of_cells; ++idx) {
        (*m_cells)[idx]->set_current_index(idx);
    }
}

void BoardWidget::ensure_puzzle_is_solvable()
{
    int parity { 0 };
    auto nr_of_cells { int((*m_cells).size()) - 1 };

    for (int idx { 0 }; idx < nr_of_cells - 1; ++idx) {
        for (int shift { idx + 1 }; shift < nr_of_cells; ++shift) {
            if ((*m_cells)[idx] > (*m_cells)[shift]) ++parity;
        }
    }

    if (parity & 1) swap((*m_cells)[0], (*m_cells)[1]);
}
