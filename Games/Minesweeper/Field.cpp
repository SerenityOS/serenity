/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Field.h"
#include <AK/HashTable.h>
#include <AK/Queue.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>
#include <time.h>
#include <unistd.h>

class SquareButton final : public GUI::Button {
    C_OBJECT(SquareButton);

public:
    Function<void()> on_right_click;
    Function<void()> on_middle_click;

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Right) {
            if (on_right_click)
                on_right_click();
        }
        if (event.button() == GUI::MouseButton::Middle) {
            if (on_middle_click)
                on_middle_click();
        }
        GUI::Button::mousedown_event(event);
    }

private:
    SquareButton() { }
};

class SquareLabel final : public GUI::Label {
    C_OBJECT(SquareLabel);

public:
    Function<void()> on_chord_click;

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Right || event.button() == GUI::MouseButton::Left) {
            if (event.buttons() == (GUI::MouseButton::Right | GUI::MouseButton::Left) || m_square.field->is_single_chording()) {
                m_chord = true;
                m_square.field->set_chord_preview(m_square, true);
            }
        }
        if (event.button() == GUI::MouseButton::Middle) {
            m_square.field->for_each_square([](auto& square) {
                if (square.is_considering) {
                    square.is_considering = false;
                    square.button->set_icon(nullptr);
                }
            });
        }
        GUI::Label::mousedown_event(event);
    }

    virtual void mousemove_event(GUI::MouseEvent& event) override
    {
        if (m_chord) {
            if (rect().contains(event.position())) {
                m_square.field->set_chord_preview(m_square, true);
            } else {
                m_square.field->set_chord_preview(m_square, false);
            }
        }
        GUI::Label::mousemove_event(event);
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        if (m_chord) {
            if (event.button() == GUI::MouseButton::Left || event.button() == GUI::MouseButton::Right) {
                if (rect().contains(event.position())) {
                    if (on_chord_click)
                        on_chord_click();
                }
                m_chord = false;
            }
        }
        m_square.field->set_chord_preview(m_square, m_chord);
        GUI::Label::mouseup_event(event);
    }

private:
    explicit SquareLabel(Square& square)
        : m_square(square)
    {
    }

    Square& m_square;
    bool m_chord { false };
};

Field::Field(GUI::Label& flag_label, GUI::Label& time_label, GUI::Button& face_button, Function<void(Gfx::IntSize)> on_size_changed)
    : m_face_button(face_button)
    , m_flag_label(flag_label)
    , m_time_label(time_label)
    , m_on_size_changed(move(on_size_changed))
{
    srand(time(nullptr));
    m_timer = add<Core::Timer>();
    m_timer->on_timeout = [this] {
        ++m_time_elapsed;
        m_time_label.set_text(String::format("%u.%u", m_time_elapsed / 10, m_time_elapsed % 10));
    };
    m_timer->set_interval(100);
    m_mine_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/mine.png");
    m_flag_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/flag.png");
    m_badflag_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/badflag.png");
    m_consider_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/consider.png");
    m_default_face_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/face-default.png");
    m_good_face_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/face-good.png");
    m_bad_face_bitmap = Gfx::Bitmap::load_from_file("/res/icons/minesweeper/face-bad.png");
    for (int i = 0; i < 8; ++i)
        m_number_bitmap[i] = Gfx::Bitmap::load_from_file(String::format("/res/icons/minesweeper/%u.png", i + 1));

    set_fill_with_background_color(true);
    reset();

    m_face_button.on_click = [this](auto) { reset(); };
    set_face(Face::Default);

    {
        auto config = Core::ConfigFile::get_for_app("Minesweeper");
        bool single_chording = config->read_num_entry("Minesweeper", "SingleChording", false);
        int mine_count = config->read_num_entry("Game", "MineCount", 10);
        int rows = config->read_num_entry("Game", "Rows", 9);
        int columns = config->read_num_entry("Game", "Columns", 9);

        // Do a quick sanity check to make sure the user hasn't tried anything crazy
        if (mine_count > rows * columns || rows <= 0 || columns <= 0 || mine_count <= 0)
            set_field_size(9, 9, 10);
        else
            set_field_size(rows, columns, mine_count);

        set_single_chording(single_chording);
    }
}

Field::~Field()
{
}

void Field::set_face(Face face)
{
    switch (face) {
    case Face::Default:
        m_face_button.set_icon(*m_default_face_bitmap);
        break;
    case Face::Good:
        m_face_button.set_icon(*m_good_face_bitmap);
        break;
    case Face::Bad:
        m_face_button.set_icon(*m_bad_face_bitmap);
        break;
    }
}

template<typename Callback>
void Square::for_each_neighbor(Callback callback)
{
    size_t r = row;
    size_t c = column;
    if (r > 0) // Up
        callback(field->square(r - 1, c));
    if (c > 0) // Left
        callback(field->square(r, c - 1));
    if (r < (field->m_rows - 1)) // Down
        callback(field->square(r + 1, c));
    if (c < (field->m_columns - 1)) // Right
        callback(field->square(r, c + 1));
    if (r > 0 && c > 0) // UpLeft
        callback(field->square(r - 1, c - 1));
    if (r > 0 && c < (field->m_columns - 1)) // UpRight
        callback(field->square(r - 1, c + 1));
    if (r < (field->m_rows - 1) && c > 0) // DownLeft
        callback(field->square(r + 1, c - 1));
    if (r < (field->m_rows - 1) && c < (field->m_columns - 1)) // DownRight
        callback(field->square(r + 1, c + 1));
}

void Field::reset()
{
    m_first_click = true;
    set_updates_enabled(false);
    m_time_elapsed = 0;
    m_time_label.set_text("0");
    m_flags_left = m_mine_count;
    m_flag_label.set_text(String::number(m_flags_left));
    m_timer->stop();
    set_greedy_for_hits(false);
    set_face(Face::Default);

    m_squares.resize(max(m_squares.size(), rows() * columns()));

    for (int i = rows() * columns(); i < static_cast<int>(m_squares.size()); ++i) {
        auto& square = m_squares[i];
        square->button->set_visible(false);
        square->label->set_visible(false);
    }

    HashTable<int> mines;
    while (mines.size() != m_mine_count) {
        int location = rand() % (rows() * columns());
        if (!mines.contains(location))
            mines.set(location);
    }

    size_t i = 0;
    for (size_t r = 0; r < rows(); ++r) {
        for (size_t c = 0; c < columns(); ++c) {
            if (!m_squares[i])
                m_squares[i] = make<Square>();
            Gfx::IntRect rect = { frame_thickness() + static_cast<int>(c) * square_size(), frame_thickness() + static_cast<int>(r) * square_size(), square_size(), square_size() };
            auto& square = this->square(r, c);
            square.field = this;
            square.row = r;
            square.column = c;
            square.has_mine = mines.contains(i);
            square.has_flag = false;
            square.is_considering = false;
            square.is_swept = false;
            if (!square.label) {
                square.label = add<SquareLabel>(square);
                square.label->set_background_color(Color::from_rgb(0xff4040));
            }
            square.label->set_fill_with_background_color(false);
            square.label->set_relative_rect(rect);
            square.label->set_visible(false);
            square.label->set_icon(square.has_mine ? m_mine_bitmap : nullptr);
            if (!square.button) {
                square.button = add<SquareButton>();
                square.button->on_click = [this, &square](auto) {
                    on_square_clicked(square);
                };
                square.button->on_right_click = [this, &square] {
                    on_square_right_clicked(square);
                };
                square.button->on_middle_click = [this, &square] {
                    on_square_middle_clicked(square);
                };
                square.label->on_chord_click = [this, &square] {
                    on_square_chorded(square);
                };
            }
            square.button->set_checked(false);
            square.button->set_icon(nullptr);
            square.button->set_relative_rect(rect);
            square.button->set_visible(true);

            ++i;
        }
    }

    for (size_t r = 0; r < rows(); ++r) {
        for (size_t c = 0; c < columns(); ++c) {
            auto& square = this->square(r, c);
            size_t number = 0;
            square.for_each_neighbor([&number](auto& neighbor) {
                number += neighbor.has_mine;
            });
            square.number = number;
            if (square.has_mine)
                continue;
            if (square.number)
                square.label->set_icon(m_number_bitmap[square.number - 1]);
        }
    }

    m_unswept_empties = rows() * columns() - m_mine_count;
    set_updates_enabled(true);
}

void Field::flood_fill(Square& square)
{
    Queue<Square*> queue;
    queue.enqueue(&square);

    while (!queue.is_empty()) {
        Square* s = queue.dequeue();
        s->for_each_neighbor([this, &queue](Square& neighbor) {
            if (!neighbor.is_swept && !neighbor.has_mine && neighbor.number == 0) {
                on_square_clicked_impl(neighbor, false);
                queue.enqueue(&neighbor);
            }
            if (!neighbor.has_mine && neighbor.number)
                on_square_clicked_impl(neighbor, false);
        });
    }
}

void Field::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    auto inner_rect = frame_inner_rect();
    painter.add_clip_rect(inner_rect);

    for (int y = inner_rect.top() - 1; y <= inner_rect.bottom(); y += square_size()) {
        Gfx::IntPoint a { inner_rect.left(), y };
        Gfx::IntPoint b { inner_rect.right(), y };
        painter.draw_line(a, b, palette().threed_shadow1());
    }
    for (int x = frame_inner_rect().left() - 1; x <= frame_inner_rect().right(); x += square_size()) {
        Gfx::IntPoint a { x, inner_rect.top() };
        Gfx::IntPoint b { x, inner_rect.bottom() };
        painter.draw_line(a, b, palette().threed_shadow1());
    }
}

void Field::on_square_clicked_impl(Square& square, bool should_flood_fill)
{
    if (m_first_click) {
        while (square.has_mine || square.number != 0) {
            reset();
        }
    }
    m_first_click = false;

    if (square.is_swept)
        return;
    if (square.has_flag)
        return;
    if (square.is_considering)
        return;
    if (!m_timer->is_active())
        m_timer->start();
    update();
    square.is_swept = true;
    square.button->set_visible(false);
    square.label->set_visible(true);
    if (square.has_mine) {
        square.label->set_fill_with_background_color(true);
        game_over();
        return;
    }

    --m_unswept_empties;
    if (should_flood_fill && square.number == 0)
        flood_fill(square);

    if (!m_unswept_empties)
        win();
}

void Field::on_square_clicked(Square& square)
{
    on_square_clicked_impl(square, true);
}

void Field::on_square_chorded(Square& square)
{
    if (!square.is_swept)
        return;
    if (!square.number)
        return;
    size_t adjacent_flags = 0;
    square.for_each_neighbor([&](auto& neighbor) {
        if (neighbor.has_flag)
            ++adjacent_flags;
    });
    if (square.number != adjacent_flags)
        return;
    square.for_each_neighbor([&](auto& neighbor) {
        if (neighbor.has_flag)
            return;
        on_square_clicked(neighbor);
    });
}

void Field::on_square_right_clicked(Square& square)
{
    if (square.is_swept)
        return;
    if (!square.has_flag && !m_flags_left)
        return;

    set_flag(square, !square.has_flag);
}

void Field::set_flag(Square& square, bool flag)
{
    ASSERT(!square.is_swept);
    if (square.has_flag == flag)
        return;
    square.is_considering = false;

    if (!flag) {
        ++m_flags_left;
    } else {

        ASSERT(m_flags_left);
        --m_flags_left;
    }
    square.has_flag = flag;

    m_flag_label.set_text(String::number(m_flags_left));
    square.button->set_icon(square.has_flag ? m_flag_bitmap : nullptr);
    square.button->update();
}

void Field::on_square_middle_clicked(Square& square)
{
    if (square.is_swept)
        return;
    if (square.has_flag) {
        ++m_flags_left;
        square.has_flag = false;
        m_flag_label.set_text(String::number(m_flags_left));
    }
    square.is_considering = !square.is_considering;
    square.button->set_icon(square.is_considering ? m_consider_bitmap : nullptr);
    square.button->update();
}

void Field::win()
{
    m_timer->stop();
    set_greedy_for_hits(true);
    set_face(Face::Good);
    for_each_square([&](auto& square) {
        if (!square.has_flag && square.has_mine)
            set_flag(square, true);
    });
    reveal_mines();
}

void Field::game_over()
{
    m_timer->stop();
    set_greedy_for_hits(true);
    set_face(Face::Bad);
    reveal_mines();
}

void Field::reveal_mines()
{
    for (size_t r = 0; r < rows(); ++r) {
        for (size_t c = 0; c < columns(); ++c) {
            auto& square = this->square(r, c);
            if (square.has_mine && !square.has_flag) {
                square.button->set_visible(false);
                square.label->set_visible(true);
            }
            if (!square.has_mine && square.has_flag) {
                square.button->set_icon(*m_badflag_bitmap);
                square.button->set_visible(true);
                square.label->set_visible(false);
            }
        }
    }
    update();
}

void Field::set_chord_preview(Square& square, bool chord_preview)
{
    if (m_chord_preview == chord_preview)
        return;
    m_chord_preview = chord_preview;
    square.for_each_neighbor([&](auto& neighbor) {
        neighbor.button->set_checked(false);
        if (!neighbor.has_flag && !neighbor.is_considering)
            neighbor.button->set_checked(chord_preview);
    });
}

void Field::set_field_size(size_t rows, size_t columns, size_t mine_count)
{
    if (m_rows == rows && m_columns == columns && m_mine_count == mine_count)
        return;
    {
        auto config = Core::ConfigFile::get_for_app("Minesweeper");
        config->write_num_entry("Game", "MineCount", mine_count);
        config->write_num_entry("Game", "Rows", rows);
        config->write_num_entry("Game", "Columns", columns);
    }
    m_rows = rows;
    m_columns = columns;
    m_mine_count = mine_count;
    set_preferred_size(frame_thickness() * 2 + m_columns * square_size(), frame_thickness() * 2 + m_rows * square_size());
    reset();
    m_on_size_changed(preferred_size());
}

void Field::set_single_chording(bool enabled)
{
    auto config = Core::ConfigFile::get_for_app("Minesweeper");
    m_single_chording = enabled;
    config->write_bool_entry("Minesweeper", "SingleChording", m_single_chording);
}

Square::Square()
{
}

Square::~Square()
{
}

template<typename Callback>
void Field::for_each_square(Callback callback)
{
    for (size_t i = 0; i < rows() * columns(); ++i)
        callback(*m_squares[i]);
}
