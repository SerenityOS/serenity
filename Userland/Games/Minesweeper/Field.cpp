/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Field.h"
#include <AK/Assertions.h>
#include <AK/HashTable.h>
#include <AK/NumberFormat.h>
#include <AK/Queue.h>
#include <AK/Random.h>
#include <AK/Types.h>
#include <LibConfig/Client.h>
#include <LibGUI/Application.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

class SquareButton final : public GUI::Button {
    C_OBJECT(SquareButton);

public:
    Function<void()> on_secondary_click;
    Function<void()> on_middle_click;

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Secondary) {
            if (on_secondary_click)
                on_secondary_click();
        }
        if (event.button() == GUI::MouseButton::Middle) {
            if (on_middle_click)
                on_middle_click();
        }
        GUI::Button::mousedown_event(event);
    }

private:
    SquareButton()
    {
        set_focus_policy(GUI::FocusPolicy::TabFocus);
    }
};

class SquareImage final : public GUI::ImageWidget {
    C_OBJECT(SquareImage);

public:
    Function<void()> on_chord_click;

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        if (event.button() == GUI::MouseButton::Secondary || event.button() == GUI::MouseButton::Primary) {
            if (event.buttons() == (GUI::MouseButton::Secondary | GUI::MouseButton::Primary) || m_square.field->is_single_chording()) {
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
        GUI::ImageWidget::mousedown_event(event);
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
        GUI::ImageWidget::mousemove_event(event);
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        if (m_chord) {
            if (event.button() == GUI::MouseButton::Primary || event.button() == GUI::MouseButton::Secondary) {
                if (rect().contains(event.position())) {
                    if (on_chord_click)
                        on_chord_click();
                }
                m_chord = false;
            }
        }
        m_square.field->set_chord_preview(m_square, m_chord);
        GUI::ImageWidget::mouseup_event(event);
    }

private:
    explicit SquareImage(Square& square)
        : m_square(square)
    {
    }

    Square& m_square;
    bool m_chord { false };
};

ErrorOr<NonnullRefPtr<Field>> Field::create(GUI::Label& flag_label, GUI::Label& time_label, GUI::Button& face_button)
{
    auto field = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) Field(flag_label, time_label, face_button)));
    field->m_mine_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/mine.png"sv));
    field->m_flag_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/flag.png"sv));
    field->m_badflag_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/badflag.png"sv));
    field->m_consider_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/consider.png"sv));
    field->m_default_face_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/face-default.png"sv));
    field->m_good_face_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/face-good.png"sv));
    field->m_bad_face_bitmap = TRY(Gfx::Bitmap::load_from_file("/res/graphics/minesweeper/face-bad.png"sv));
    for (int i = 0; i < 8; ++i)
        field->m_number_bitmap[i] = TRY(Gfx::Bitmap::load_from_file(ByteString::formatted("/res/graphics/minesweeper/{}.png", i + 1)));
    field->initialize();
    return field;
}

Field::Field(GUI::Label& flag_label, GUI::Label& time_label, GUI::Button& face_button)
    : m_mine_palette(GUI::Application::the()->palette().impl().clone())
    , m_face_button(face_button)
    , m_flag_label(flag_label)
    , m_time_label(time_label)
{
}

void Field::initialize()
{
    m_timer = Core::Timer::create_repeating(
        1000, [this] {
            ++m_time_elapsed;
            m_time_label.set_text(human_readable_digital_time(m_time_elapsed));
        },
        this);

    // Square with mine will be filled with background color later, i.e. red
    m_mine_palette.set_color(Gfx::ColorRole::Base, Color::from_rgb(0xff4040));

    set_fill_with_background_color(true);

    m_face_button.on_click = [this](auto) { reset(); };
    set_face(Face::Default);

    {
        bool single_chording = Config::read_bool("Minesweeper"sv, "Game"sv, "SingleChording"sv, false);
        int mine_count = Config::read_i32("Minesweeper"sv, "Game"sv, "MineCount"sv, 10);
        int rows = Config::read_i32("Minesweeper"sv, "Game"sv, "Rows"sv, 9);
        int columns = Config::read_i32("Minesweeper"sv, "Game"sv, "Columns"sv, 9);
        auto difficulty_string = Config::read_string("Minesweeper"sv, "Game"sv, "Difficulty"sv, "beginner"sv);
        auto difficulty = difficulty_from_string(difficulty_string);

        // Do a quick sanity check to make sure the user hasn't tried anything crazy
        if (!difficulty.has_value() || mine_count > rows * columns || rows <= 0 || columns <= 0 || mine_count <= 0)
            set_field_difficulty(Difficulty::Beginner);
        else if (difficulty.value() == Difficulty::Custom)
            set_field_size(Difficulty::Custom, rows, columns, mine_count);
        else
            set_field_difficulty(difficulty.value());

        set_single_chording(single_chording);
    }
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
    m_time_label.set_text("00:00"_string);
    m_flags_left = m_mine_count;
    m_flag_label.set_text(String::number(m_flags_left));
    m_timer->stop();
    set_greedy_for_hits(false);
    set_face(Face::Default);

    m_squares.resize(max(m_squares.size(), rows() * columns()));

    for (int i = rows() * columns(); i < static_cast<int>(m_squares.size()); ++i) {
        auto& square = m_squares[i];
        square->button->set_visible(false);
        square->image->set_visible(false);
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
            square.has_mine = false;
            square.has_flag = false;
            square.is_considering = false;
            square.is_swept = false;
            if (!square.image) {
                square.image = add<SquareImage>(square);
                square.image->set_palette(m_mine_palette);
                square.image->set_background_role(Gfx::ColorRole::Base);
            }
            square.image->set_fill_with_background_color(false);
            square.image->set_relative_rect(rect);
            square.image->set_visible(false);
            square.image->set_bitmap(nullptr);
            if (!square.button) {
                square.button = add<SquareButton>();
                square.button->on_click = [this, &square](auto) {
                    on_square_clicked(square);
                };
                square.button->on_secondary_click = [this, &square] {
                    on_square_secondary_clicked(square);
                };
                square.button->on_middle_click = [this, &square] {
                    on_square_middle_clicked(square);
                };
                square.image->on_chord_click = [this, &square] {
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

    set_updates_enabled(true);
}

void Field::generate_field(size_t start_row, size_t start_column)
{
    VERIFY(m_squares.size() >= rows() * columns());
    size_t board_size = rows() * columns();

    // FIXME: Handle possible errors
    HashTable<size_t> free_squares;

    size_t start_index = start_row * columns() + start_column;
    free_squares.set(start_index);

    square(start_row, start_column).for_each_neighbor([&](auto const& neighbor) {
        size_t neighbor_index = neighbor.row * columns() + neighbor.column;
        free_squares.set(neighbor_index);
    });

    VERIFY(m_mine_count <= board_size - free_squares.size());

    Vector<size_t> possible_mine_positions;
    possible_mine_positions.ensure_capacity(board_size - free_squares.size());

    for (size_t i = 0; i < board_size; ++i) {
        m_squares[i]->has_mine = false;
        m_squares[i]->has_flag = false;
        m_squares[i]->is_considering = false;
        m_squares[i]->is_swept = false;
        m_squares[i]->number = 0;
        if (!free_squares.contains(i))
            possible_mine_positions.unchecked_append(i);
    }

    AK::shuffle(possible_mine_positions);

    for (size_t i = 0; i < m_mine_count; i++) {
        size_t mine_location = possible_mine_positions[i];
        m_squares[mine_location]->has_mine = true;
    }

    for (size_t r = 0; r < rows(); ++r) {
        for (size_t c = 0; c < columns(); ++c) {
            auto& square = this->square(r, c);
            size_t number = 0;
            square.for_each_neighbor([&number](auto& neighbor) {
                number += neighbor.has_mine;
            });
            square.number = number;
            if (square.has_mine) {
                square.image->set_bitmap(m_mine_bitmap);
            } else if (square.number) {
                square.image->set_bitmap(m_number_bitmap[square.number - 1]);
            }
        }
    }

    m_unswept_empties = rows() * columns() - m_mine_count;
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

    for (int y = inner_rect.top() - 1; y < inner_rect.bottom(); y += square_size()) {
        Gfx::IntPoint a { inner_rect.left(), y };
        Gfx::IntPoint b { inner_rect.right() - 1, y };
        painter.draw_line(a, b, palette().threed_shadow1());
    }
    for (int x = frame_inner_rect().left() - 1; x < frame_inner_rect().right(); x += square_size()) {
        Gfx::IntPoint a { x, inner_rect.top() };
        Gfx::IntPoint b { x, inner_rect.bottom() - 1 };
        painter.draw_line(a, b, palette().threed_shadow1());
    }
}

void Field::on_square_clicked_impl(Square& square, bool should_flood_fill)
{
    if (m_first_click) {
        reset();
        generate_field(square.row, square.column);
    }
    m_first_click = false;

    if (square.is_swept)
        return;
    if (square.has_flag)
        return;
    if (square.is_considering)
        return;
    if (!m_timer->is_active()) {
        m_timer->on_timeout();
        m_timer->start();
    }
    update();
    square.is_swept = true;
    square.button->set_visible(false);
    square.image->set_visible(true);
    if (square.has_mine) {
        square.image->set_fill_with_background_color(true);
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

void Field::on_square_secondary_clicked(Square& square)
{
    if (square.is_swept)
        return;
    if (!square.has_flag && !m_flags_left)
        return;

    set_flag(square, !square.has_flag);
}

void Field::set_flag(Square& square, bool flag)
{
    VERIFY(!square.is_swept);
    if (square.has_flag == flag)
        return;
    square.is_considering = false;

    if (!flag) {
        ++m_flags_left;
    } else {

        VERIFY(m_flags_left);
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
                square.image->set_visible(true);
            }
            if (!square.has_mine && square.has_flag) {
                square.button->set_icon(*m_badflag_bitmap);
                square.button->set_visible(true);
                square.image->set_visible(false);
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

void Field::set_field_difficulty(Difficulty difficulty)
{
    switch (difficulty) {
    case Difficulty::Beginner:
        set_field_size(difficulty, 9, 9, 10);
        break;
    case Difficulty::Intermediate:
        set_field_size(difficulty, 16, 16, 40);
        break;
    case Difficulty::Expert:
        set_field_size(difficulty, 16, 30, 99);
        break;
    case Difficulty::Madwoman:
        set_field_size(difficulty, 32, 60, 350);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void Field::set_field_size(Difficulty difficulty, size_t rows, size_t columns, size_t mine_count)
{
    if (m_rows == rows && m_columns == columns && m_mine_count == mine_count)
        return;
    {
        Config::write_i32("Minesweeper"sv, "Game"sv, "MineCount"sv, mine_count);
        Config::write_i32("Minesweeper"sv, "Game"sv, "Rows"sv, rows);
        Config::write_i32("Minesweeper"sv, "Game"sv, "Columns"sv, columns);
        Config::write_string("Minesweeper"sv, "Game"sv, "Difficulty"sv, difficulty_to_string(difficulty));
    }
    m_difficulty = difficulty;
    m_rows = rows;
    m_columns = columns;
    m_mine_count = mine_count;
    set_fixed_size(frame_thickness() * 2 + m_columns * square_size(), frame_thickness() * 2 + m_rows * square_size());
    reset();
}

void Field::set_single_chording(bool enabled)
{
    m_single_chording = enabled;
    Config::write_bool("Minesweeper"sv, "Game"sv, "SingleChording"sv, m_single_chording);
}

template<typename Callback>
void Field::for_each_square(Callback callback)
{
    for (size_t i = 0; i < rows() * columns(); ++i)
        callback(*m_squares[i]);
}
