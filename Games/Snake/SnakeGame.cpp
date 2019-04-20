#include "SnakeGame.h"
#include <LibGUI/GPainter.h>
#include <stdlib.h>
#include <time.h>

SnakeGame::SnakeGame(GWidget* parent)
    : GWidget(parent)
{
    srand(time(nullptr));
    reset();
}

SnakeGame::~SnakeGame()
{
}

void SnakeGame::reset()
{
    m_head = { m_rows / 2, m_columns / 2 };
    m_tail.clear_with_capacity();
    m_length = 2;
    stop_timer();
    start_timer(120);
    spawn_fruit();
}

bool SnakeGame::is_available(const Coordinate& coord)
{
    for (int i = 0; i < m_tail.size(); ++i) {
        if (m_tail[i] == coord)
            return false;
    }
    if (m_head == coord)
        return false;
    if (m_fruit == coord)
        return false;
    return true;
}

void SnakeGame::spawn_fruit()
{
    Coordinate coord;
    for (;;) {
        coord.row = rand() % m_rows;
        coord.column = rand() % m_columns;
        if (is_available(coord))
            break;
    }
    m_fruit = coord;
}

void SnakeGame::timer_event(CTimerEvent&)
{
    m_tail.prepend(m_head);

    if (m_tail.size() > m_length)
        m_tail.take_last();

    m_head.row += m_vertical_velocity;
    m_head.column += m_horizontal_velocity;

    m_last_vertical_velocity = m_vertical_velocity;
    m_last_horizontal_velocity = m_horizontal_velocity;

    if (m_head.row >= m_rows)
        m_head.row = 0;
    if (m_head.row < 0)
        m_head.row = m_rows - 1;
    if (m_head.column >= m_columns)
        m_head.column = 0;
    if (m_head.column < 0)
        m_head.column = m_columns - 1;

    for (int i = 0; i < m_tail.size(); ++i) {
        if (m_head == m_tail[i]) {
            game_over();
            return;
        }
    }

    if (m_head == m_fruit) {
        ++m_length;
        spawn_fruit();
    }
    update();
}

void SnakeGame::keydown_event(GKeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_A:
    case KeyCode::Key_Left:
        if (m_last_horizontal_velocity == 1)
            break;
        m_vertical_velocity = 0;
        m_horizontal_velocity = -1;
        break;
    case KeyCode::Key_D:
    case KeyCode::Key_Right:
        if (m_last_horizontal_velocity == -1)
            break;
        m_vertical_velocity = 0;
        m_horizontal_velocity = 1;
        break;
    case KeyCode::Key_W:
    case KeyCode::Key_Up:
        if (m_last_vertical_velocity == 1)
            break;
        m_vertical_velocity = -1;
        m_horizontal_velocity = 0;
        break;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        if (m_last_vertical_velocity == -1)
            break;
        m_vertical_velocity = 1;
        m_horizontal_velocity = 0;
        break;
    default:
        break;
    }
}

void SnakeGame::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.fill_rect(event.rect(), Color::Black);

    auto game_rect = rect();
    auto cell_size = Size(game_rect.width() / m_columns, game_rect.height() / m_rows);

    auto cell_rect = [&] (const Coordinate& coord) -> Rect {
        return {
            coord.column * cell_size.width(),
            coord.row * cell_size.height(),
            cell_size.width(),
            cell_size.height()
        };
    };

    painter.fill_rect(cell_rect(m_head), Color::Yellow);
    for (auto& coord : m_tail)
        painter.fill_rect(cell_rect(coord), Color::from_rgb(0xaaaa00));

    painter.fill_rect(cell_rect(m_fruit), Color::Red);
}

void SnakeGame::game_over()
{
    reset();
}
