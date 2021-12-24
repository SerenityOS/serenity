/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Noncopyable.h>
#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>
#include <LibGfx/Palette.h>

class Field;
class SquareButton;
class SquareLabel;

class Square {
    AK_MAKE_NONCOPYABLE(Square);

public:
    Square();
    ~Square();

    Field* field { nullptr };
    bool is_swept { false };
    bool has_mine { false };
    bool has_flag { false };
    bool is_considering { false };
    size_t row { 0 };
    size_t column { 0 };
    size_t number { 0 };
    RefPtr<SquareButton> button;
    RefPtr<SquareLabel> label;

    template<typename Callback>
    void for_each_neighbor(Callback);
};

struct Bitmaps {
    NonnullRefPtr<Gfx::Bitmap> m_mine_bitmap;
    NonnullRefPtr<Gfx::Bitmap> m_flag_bitmap;
    NonnullRefPtr<Gfx::Bitmap> m_badflag_bitmap;
    NonnullRefPtr<Gfx::Bitmap> m_consider_bitmap;
    NonnullRefPtr<Gfx::Bitmap> m_default_face_bitmap;
    NonnullRefPtr<Gfx::Bitmap> m_good_face_bitmap;
    NonnullRefPtr<Gfx::Bitmap> m_bad_face_bitmap;
    Array<NonnullRefPtr<Gfx::Bitmap>, 8> m_number_bitmap;

private:
    Bitmaps(NonnullRefPtr<Gfx::Bitmap> mine_bitmap,
        NonnullRefPtr<Gfx::Bitmap> flag_bitmap,
        NonnullRefPtr<Gfx::Bitmap> badflag_bitmap,
        NonnullRefPtr<Gfx::Bitmap> consider_bitmap,
        NonnullRefPtr<Gfx::Bitmap> default_face_bitmap,
        NonnullRefPtr<Gfx::Bitmap> good_face_bitmap,
        NonnullRefPtr<Gfx::Bitmap> bad_face_bitmap,
        Array<NonnullRefPtr<Gfx::Bitmap>, 8> number_bitmap)
        : m_mine_bitmap(move(mine_bitmap))
        , m_flag_bitmap(move(flag_bitmap))
        , m_badflag_bitmap(move(badflag_bitmap))
        , m_consider_bitmap(move(consider_bitmap))
        , m_default_face_bitmap(move(default_face_bitmap))
        , m_good_face_bitmap(move(good_face_bitmap))
        , m_bad_face_bitmap(move(bad_face_bitmap))
        , m_number_bitmap(move(number_bitmap))
    {
    }

public:
    static ErrorOr<Bitmaps> construct()
    {
        NonnullRefPtr<Gfx::Bitmap> mine_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/mine.png"));
        NonnullRefPtr<Gfx::Bitmap> flag_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/flag.png"));
        NonnullRefPtr<Gfx::Bitmap> badflag_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/badflag.png"));
        NonnullRefPtr<Gfx::Bitmap> consider_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/consider.png"));
        NonnullRefPtr<Gfx::Bitmap> default_face_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/face-default.png"));
        NonnullRefPtr<Gfx::Bitmap> good_face_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/face-good.png"));
        NonnullRefPtr<Gfx::Bitmap> bad_face_bitmap = TRY(Gfx::Bitmap::try_load_from_file("/res/icons/minesweeper/face-bad.png"));
        auto number_bitmap_entry = [](size_t n) {
            return Gfx::Bitmap::try_load_from_file(String::formatted("/res/icons/minesweeper/{}.png", n + 1));
        };
        Array<NonnullRefPtr<Gfx::Bitmap>, 8> number_bitmap = {
            TRY(number_bitmap_entry(0)),
            TRY(number_bitmap_entry(1)),
            TRY(number_bitmap_entry(2)),
            TRY(number_bitmap_entry(3)),
            TRY(number_bitmap_entry(4)),
            TRY(number_bitmap_entry(5)),
            TRY(number_bitmap_entry(6)),
            TRY(number_bitmap_entry(7))
        };
        return Bitmaps(
            move(mine_bitmap),
            move(flag_bitmap),
            move(badflag_bitmap),
            move(consider_bitmap),
            move(default_face_bitmap),
            move(good_face_bitmap),
            move(bad_face_bitmap),
            move(number_bitmap));
    }
};

class Field final : public GUI::Frame {
    C_OBJECT_ABSTRACT(Field)
    friend class Square;
    friend class SquareLabel;

public:
    virtual ~Field() override;

    enum class Difficulty {
        Beginner,
        Intermediate,
        Expert,
        Madwoman,
        Custom
    };

    StringView difficulty_to_string(Difficulty difficulty) const
    {
        switch (difficulty) {
        case Difficulty::Beginner:
            return "beginner"sv;
        case Difficulty::Intermediate:
            return "intermediate"sv;
        case Difficulty::Expert:
            return "expert"sv;
        case Difficulty::Madwoman:
            return "madwoman"sv;
        case Difficulty::Custom:
            return "custom"sv;
        default:
            VERIFY_NOT_REACHED();
        }
    }

    Optional<Difficulty> difficulty_from_string(StringView difficulty_string) const
    {
        if (difficulty_string.matches("beginner"))
            return Difficulty::Beginner;

        if (difficulty_string.equals_ignoring_case("intermediate"))
            return Difficulty::Intermediate;

        if (difficulty_string.equals_ignoring_case("expert"))
            return Difficulty::Expert;

        if (difficulty_string.equals_ignoring_case("madwoman"))
            return Difficulty::Madwoman;

        if (difficulty_string.equals_ignoring_case("custom"))
            return Difficulty::Custom;

        return {};
    }

    Difficulty difficulty() const { return m_difficulty; }
    size_t rows() const { return m_rows; }
    size_t columns() const { return m_columns; }
    size_t mine_count() const { return m_mine_count; }
    int square_size() const { return 15; }
    bool is_single_chording() const { return m_single_chording; }

    void set_field_difficulty(Difficulty difficulty);
    void set_field_size(Difficulty difficulty, size_t rows, size_t columns, size_t mine_count);

    void set_single_chording(bool new_val);

    void reset();

    static ErrorOr<NonnullRefPtr<Field>> try_create(GUI::Label& flag_label, GUI::Label& time_label, GUI::Button& face_button, Function<void(Gfx::IntSize)> on_size_changed);

private:
    Field(GUI::Label& flag_label, GUI::Label& time_label, GUI::Button& face_button, Bitmaps bitmaps, Function<void(Gfx::IntSize)> on_size_changed);

    virtual void paint_event(GUI::PaintEvent&) override;

    void on_square_clicked(Square&);
    void on_square_secondary_clicked(Square&);
    void on_square_middle_clicked(Square&);
    void on_square_chorded(Square&);
    void game_over();
    void win();
    void reveal_mines();
    void set_chord_preview(Square&, bool);
    void set_flag(Square&, bool);

    Square& square(size_t row, size_t column) { return *m_squares[row * columns() + column]; }
    const Square& square(size_t row, size_t column) const { return *m_squares[row * columns() + column]; }

    void flood_fill(Square&);
    void on_square_clicked_impl(Square&, bool);

    template<typename Callback>
    void for_each_square(Callback);

    enum class Face {
        Default,
        Good,
        Bad
    };
    void set_face(Face);

    Difficulty m_difficulty { Difficulty::Beginner };
    size_t m_rows { 0 };
    size_t m_columns { 0 };
    size_t m_mine_count { 0 };
    size_t m_unswept_empties { 0 };
    Vector<OwnPtr<Square>> m_squares;
    Bitmaps m_bitmaps;
    Gfx::Palette m_mine_palette;
    GUI::Button& m_face_button;
    GUI::Label& m_flag_label;
    GUI::Label& m_time_label;
    RefPtr<Core::Timer> m_timer;
    size_t m_time_elapsed { 0 };
    size_t m_flags_left { 0 };
    Face m_face { Face::Default };
    bool m_chord_preview { false };
    bool m_first_click { true };
    bool m_single_chording { true };
    Function<void(Gfx::IntSize)> m_on_size_changed;
};
