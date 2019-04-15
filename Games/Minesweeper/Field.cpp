#include "Field.h"
#include <LibGUI/GButton.h>
#include <LibGUI/GLabel.h>
#include <AK/HashTable.h>
#include <LibCore/CConfigFile.h>
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

Field::Field(GLabel& flag_label, GLabel& time_label, GButton& face_button, GWidget* parent)
    : GFrame(parent)
    , m_face_button(face_button)
    , m_flag_label(flag_label)
    , m_time_label(time_label)
{
    auto config = CConfigFile::get_for_app("Minesweeper");

    m_mine_count = config->read_num_entry("Game", "MineCount", 10);
    m_rows = config->read_num_entry("Game", "Rows", 9);
    m_columns = config->read_num_entry("Game", "Columns", 9);

    m_timer.on_timeout = [this] {
        m_time_label.set_text(String::format("%u", ++m_seconds_elapsed));
    };
    m_timer.set_interval(1000);
    set_frame_thickness(2);
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
    m_mine_bitmap = GraphicsBitmap::load_from_file("/res/icons/minesweeper/mine.png");
    m_flag_bitmap = GraphicsBitmap::load_from_file("/res/icons/minesweeper/flag.png");
    for (int i = 0; i < 8; ++i)
        m_number_bitmap[i] = GraphicsBitmap::load_from_file(String::format("/res/icons/minesweeper/%u.png", i + 1));

    set_fill_with_background_color(true);
    set_background_color(Color::LightGray);
    reset();

    m_face_button.on_click = [this] (auto&) { reset(); };
    set_face(Face::Default);
}

Field::~Field()
{
}

void Field::set_face(Face face)
{
    switch (face) {
    case Face::Default:
        m_face_button.set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/face-default.png"));
        break;
    case Face::Good:
        m_face_button.set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/face-good.png"));
        break;
    case Face::Bad:
        m_face_button.set_icon(GraphicsBitmap::load_from_file("/res/icons/minesweeper/face-bad.png"));
        break;
    }
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
    if (r < (m_rows - 1)) // Down
        callback(this->square(r + 1, c));
    if (c < (m_columns - 1)) // Right
        callback(this->square(r, c + 1));
    if (r > 0 && c > 0) // UpLeft
        callback(this->square(r - 1, c - 1));
    if (r > 0 && c < (m_columns - 1)) // UpRight
        callback(this->square(r - 1, c + 1));
    if (r < (m_rows - 1) && c > 0) // DownLeft
        callback(this->square(r + 1, c - 1));
    if (r < (m_rows - 1) && c < (m_columns - 1)) // DownRight
        callback(this->square(r + 1, c + 1));
}

void Field::reset()
{
    m_seconds_elapsed = 0;
    m_time_label.set_text("0");
    m_flags_left = m_mine_count;
    m_flag_label.set_text(String::format("%u", m_flags_left));
    m_timer.start();
    set_greedy_for_hits(false);
    set_face(Face::Default);
    srand(time(nullptr));
    m_squares.resize(rows() * columns());

    HashTable<int> mines;
    while (mines.size() != m_mine_count) {
        int location = rand() % (rows() * columns());
        if (!mines.contains(location))
            mines.set(location);
    }

    int i = 0;
    for (int r = 0; r < rows(); ++r) {
        for (int c = 0; c < columns(); ++c) {
            Rect rect = { frame_thickness() + c * square_size(), frame_thickness() + r * square_size(), square_size(), square_size() };
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

    m_unswept_empties = rows() * columns() - m_mine_count;

    update();
}

void Field::flood_fill(Square& square)
{
    on_square_clicked(square);
    for_each_neighbor_of(square, [this] (auto& neighbor) {
        if (!neighbor.is_swept && !neighbor.has_mine && neighbor.number == 0)
            flood_fill(neighbor);
        if (!neighbor.has_mine && neighbor.number)
            on_square_clicked(neighbor);
    });
}

void Field::on_square_clicked(Square& square)
{
    if (square.is_swept)
        return;
    if (square.has_flag)
        return;
    update();
    square.is_swept = true;
    square.button->set_visible(false);
    square.label->set_visible(true);
    if (square.has_mine) {
        square.label->set_fill_with_background_color(true);
        game_over();
    } else {
        --m_unswept_empties;
        if (square.number == 0)
            flood_fill(square);
    }

    if (!m_unswept_empties)
        win();
}

void Field::on_square_right_clicked(Square& square)
{
    if (square.is_swept)
        return;
    if (!square.has_flag && !m_flags_left)
        return;

    if (!square.has_flag) {
        --m_flags_left;
        square.has_flag = true;
    } else {
        ++m_flags_left;
        square.has_flag = false;
    }

    m_flag_label.set_text(String::format("%u", m_flags_left));
    square.button->set_icon(square.has_flag ? m_flag_bitmap : nullptr);
    square.button->update();
}

void Field::win()
{
    m_timer.stop();
    set_greedy_for_hits(true);
    set_face(Face::Good);
    reveal_mines();
}

void Field::game_over()
{
    m_timer.stop();
    set_greedy_for_hits(true);
    set_face(Face::Bad);
    reveal_mines();
}

void Field::reveal_mines()
{
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
