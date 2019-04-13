#include "Field.h"
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <AK/HashTable.h>
#include <unistd.h>
#include <time.h>

class SquareButton final : public GButton {
public:
    SquareButton(GWidget* parent)
        : GButton(parent)
    {
    }

    Function<void()> on_right_click;

    virtual void mousedown_event(GMouseEvent& event) override
    {
        if (event.button() == GMouseButton::Right) {
            if (on_right_click)
                on_right_click();
        }
        GButton::mousedown_event(event);
    }
};

Field::Field(GWidget* parent)
    : GWidget(parent)
{
    m_mine_bitmap = GraphicsBitmap::load_from_file("/res/icons/minesweeper/mine.png");
    m_flag_bitmap = GraphicsBitmap::load_from_file("/res/icons/minesweeper/flag.png");
    for (int i = 0; i < 8; ++i)
        m_number_bitmap[i] = GraphicsBitmap::load_from_file(String::format("/res/icons/minesweeper/%u.png", i + 1));

    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);
    reset();
}

Field::~Field()
{
}

template<typename Callback>
void Field::for_each_neighbor_of(const Square& square, Callback callback)
{
    int r = square.row;
    int c = square.column;
    if (r > 0) // Up
        callback(this->square(r - 1, c));
    if (c > 0) // Left
        callback(this->square(r, c - 1));
    if (r < (m_rows - 2)) // Down
        callback(this->square(r + 1, c));
    if (c < (m_columns - 2)) // Right
        callback(this->square(r, c + 1));
    if (r > 0 && c > 0) // UpLeft
        callback(this->square(r - 1, c - 1));
    if (r > 0 && c < (m_columns - 2)) // UpRight
        callback(this->square(r - 1, c + 1));
    if (r < (m_rows - 2) && c > 0) // DownLeft
        callback(this->square(r + 1, c - 1));
    if (r < (m_rows - 2) && c < (m_columns - 2)) // DownRight
        callback(this->square(r + 1, c + 1));
}

void Field::reset()
{
    set_greedy_for_hits(false);
    srand(time(nullptr));
    m_squares.resize(rows() * columns());

    HashTable<int> mines;
    for (int i = 0; i < m_mine_count; ++i)
        mines.set(rand() % (rows() * columns()));

    int i = 0;
    for (int r = 0; r < rows(); ++r) {
        for (int c = 0; c < columns(); ++c) {
            Rect rect = { c * square_size(), r * square_size(), square_size(), square_size() };
            auto& square = this->square(r, c);
            square.row = r;
            square.column = c;
            square.has_mine = mines.contains(i);
            square.has_flag = false;
            square.is_swept = false;
            if (!square.label)
                square.label = new GLabel(this);
            square.label->set_relative_rect(rect);
            square.label->set_visible(false);
            square.label->set_icon(square.has_mine ? m_mine_bitmap : nullptr);
            square.label->set_background_color(Color::from_rgb(0xff4040));
            square.label->set_fill_with_background_color(false);
            if (!square.button)
                square.button = new SquareButton(this);
            square.button->set_icon(nullptr);
            square.button->set_relative_rect(rect);
            square.button->set_visible(true);
            square.button->on_click = [this, &square] (GButton&) {
                on_square_clicked(square);
            };
            square.button->on_right_click = [this, &square] {
                on_square_right_clicked(square);
            };
            ++i;
        }
    }
    for (int r = 0; r < rows(); ++r) {
        for (int c = 0; c < columns(); ++c) {
            auto& square = this->square(r, c);
            int number = 0;
            for_each_neighbor_of(square, [&number] (auto& neighbor) {
                number += neighbor.has_mine;
            });
            square.number = number;
            if (square.has_mine)
                continue;
            if (square.number)
                square.label->set_icon(m_number_bitmap[square.number - 1].copy_ref());
        }
    }

    update();
}

void Field::flood_fill(Square& square)
{
    on_square_clicked(square);
    for_each_neighbor_of(square, [this] (auto& neighbor) {
        if (!neighbor.is_swept && !neighbor.has_mine && neighbor.number == 0)
            flood_fill(neighbor);
    });
}

void Field::on_square_clicked(Square& square)
{
    if (square.is_swept)
        return;
    if (square.has_flag)
        return;
    square.is_swept = true;
    square.button->set_visible(false);
    square.label->set_visible(true);
    if (square.has_mine) {
        square.label->set_fill_with_background_color(true);
        game_over();
    } else if (square.number == 0) {
        flood_fill(square);
    }
}

void Field::on_square_right_clicked(Square& square)
{
    if (square.is_swept)
        return;
    square.has_flag = !square.has_flag;
    square.button->set_icon(square.has_flag ? m_flag_bitmap : nullptr);
    square.button->update();
}

void Field::game_over()
{
    set_greedy_for_hits(true);
    for (int r = 0; r < rows(); ++r) {
        for (int c = 0; c < columns(); ++c) {
            auto& square = this->square(r, c);
            if (square.has_mine) {
                square.button->set_visible(false);
                square.label->set_visible(true);
            }
        }
    }
    update();
}
