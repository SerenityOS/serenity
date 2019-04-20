#include "SnakeGame.h"
#include <LibGUI/GPainter.h>
#include <LibGUI/GFontDatabase.h>
#include <SharedGraphics/GraphicsBitmap.h>
#include <stdlib.h>
#include <time.h>

SnakeGame::SnakeGame(GWidget* parent)
    : GWidget(parent)
{
    set_font(GFontDatabase::the().get_by_name("Liza Regular"));
    m_fruit_bitmaps.append(*GraphicsBitmap::load_from_file("/res/icons/snake/paprika.png"));
    m_fruit_bitmaps.append(*GraphicsBitmap::load_from_file("/res/icons/snake/eggplant.png"));
    m_fruit_bitmaps.append(*GraphicsBitmap::load_from_file("/res/icons/snake/cauliflower.png"));
    m_fruit_bitmaps.append(*GraphicsBitmap::load_from_file("/res/icons/snake/tomato.png"));
    srand(time(nullptr));
    reset();

    m_high_score = 0;
    m_high_score_text = "Best: 0";
}

SnakeGame::~SnakeGame()
{
}

void SnakeGame::reset()
{
    m_head = { m_rows / 2, m_columns / 2 };
    m_tail.clear_with_capacity();
    m_length = 2;
    m_score = 0;
    m_score_text = "Score: 0";
    m_velocity_queue.clear();
    stop_timer();
    start_timer(100);
    spawn_fruit();
    update();
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
    m_fruit_type = rand() % m_fruit_bitmaps.size();
}

Rect SnakeGame::score_rect() const
{
    int score_width = font().width(m_score_text);
    return { width() - score_width - 2, height() - font().glyph_height() - 2, score_width, font().glyph_height() };
}

Rect SnakeGame::high_score_rect() const
{
    int high_score_width = font().width(m_high_score_text);
    return { 2, height() - font().glyph_height() - 2, high_score_width, font().glyph_height() };
}

void SnakeGame::timer_event(CTimerEvent&)
{
    Vector<Coordinate> dirty_cells;

    m_tail.prepend(m_head);

    if (m_tail.size() > m_length) {
        dirty_cells.append(m_tail.last());
        m_tail.take_last();
    }

    if (!m_velocity_queue.is_empty())
        m_velocity = m_velocity_queue.dequeue();

    dirty_cells.append(m_head);

    m_head.row += m_velocity.vertical;
    m_head.column += m_velocity.horizontal;

    m_last_velocity = m_velocity;

    if (m_head.row >= m_rows)
        m_head.row = 0;
    if (m_head.row < 0)
        m_head.row = m_rows - 1;
    if (m_head.column >= m_columns)
        m_head.column = 0;
    if (m_head.column < 0)
        m_head.column = m_columns - 1;

    dirty_cells.append(m_head);

    for (int i = 0; i < m_tail.size(); ++i) {
        if (m_head == m_tail[i]) {
            game_over();
            return;
        }
    }

    if (m_head == m_fruit) {
        ++m_length;
        ++m_score;
        m_score_text = String::format("Score: %u", m_score);
        if (m_score > m_high_score) {
            m_high_score = m_score;
            m_high_score_text = String::format("Best: %u", m_high_score);
            update(high_score_rect());
        }
        update(score_rect());
        dirty_cells.append(m_fruit);
        spawn_fruit();
        dirty_cells.append(m_fruit);
    }

    for (auto& coord : dirty_cells) {
        update(cell_rect(coord));
    }
}

void SnakeGame::keydown_event(GKeyEvent& event)
{
    switch (event.key()) {
    case KeyCode::Key_A:
    case KeyCode::Key_Left:
        if (last_velocity().horizontal == 1)
            break;
        queue_velocity(0, -1);
        break;
    case KeyCode::Key_D:
    case KeyCode::Key_Right:
        if (last_velocity().horizontal == -1)
            break;
        queue_velocity(0, 1);
        break;
    case KeyCode::Key_W:
    case KeyCode::Key_Up:
        if (last_velocity().vertical == 1)
            break;
        queue_velocity(-1, 0);
        break;
    case KeyCode::Key_S:
    case KeyCode::Key_Down:
        if (last_velocity().vertical == -1)
            break;
        queue_velocity(1, 0);
        break;
    default:
        break;
    }
}

Rect SnakeGame::cell_rect(const Coordinate& coord) const
{
    auto game_rect = rect();
    auto cell_size = Size(game_rect.width() / m_columns, game_rect.height() / m_rows);
    return {
        coord.column * cell_size.width(),
        coord.row * cell_size.height(),
        cell_size.width(),
        cell_size.height()
    };
}

void SnakeGame::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(event.rect(), Color::Black);

    painter.fill_rect(cell_rect(m_head), Color::Yellow);
    for (auto& part : m_tail) {
        auto rect = cell_rect(part);
        painter.fill_rect(rect, Color::from_rgb(0xaaaa00));

        Rect left_side(rect.x(), rect.y(), 2, rect.height());
        Rect top_side(rect.x(), rect.y(), rect.width(), 2);
        Rect right_side(rect.right() - 1, rect.y(), 2, rect.height());
        Rect bottom_side(rect.x(), rect.bottom() - 1, rect.width(), 2);
        painter.fill_rect(left_side, Color::from_rgb(0xcccc00));
        painter.fill_rect(right_side, Color::from_rgb(0x888800));
        painter.fill_rect(top_side, Color::from_rgb(0xcccc00));
        painter.fill_rect(bottom_side, Color::from_rgb(0x888800));

    }

    painter.draw_scaled_bitmap(cell_rect(m_fruit), *m_fruit_bitmaps[m_fruit_type], m_fruit_bitmaps[m_fruit_type]->rect());

    painter.draw_text(high_score_rect(), m_high_score_text, TextAlignment::TopLeft, Color::from_rgb(0xfafae0));
    painter.draw_text(score_rect(), m_score_text, TextAlignment::TopLeft, Color::White);
}

void SnakeGame::game_over()
{
    reset();
}

void SnakeGame::queue_velocity(int v, int h)
{
    if (last_velocity().vertical == v && last_velocity().horizontal == h)
        return;
    m_velocity_queue.enqueue({ v, h });
}

const SnakeGame::Velocity& SnakeGame::last_velocity() const
{
    if (!m_velocity_queue.is_empty())
        return m_velocity_queue.last();

    return m_last_velocity;
}
