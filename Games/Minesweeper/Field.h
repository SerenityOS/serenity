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

#pragma once

#include <AK/Noncopyable.h>
#include <LibCore/Timer.h>
#include <LibGUI/Frame.h>

class Field;
class SquareButton;
class SquareLabel;

class Square {
    AK_MAKE_NONCOPYABLE(Square)
public:
    Square() {}
    ~Square();

    Field* field { nullptr };
    bool is_swept { false };
    bool has_mine { false };
    bool has_flag { false };
    bool is_considering { false };
    int row { 0 };
    int column { 0 };
    int number { 0 };
    SquareButton* button { nullptr };
    SquareLabel* label { nullptr };

    template<typename Callback>
    void for_each_neighbor(Callback);
};

class Field final : public GUI::Frame {
    C_OBJECT(Field)
    friend class Square;
    friend class SquareLabel;
public:
    Field(GUI::Label& flag_label, GUI::Label& time_label, GUI::Button& face_button, GUI::Widget* parent, Function<void(Gfx::Size)> on_size_changed);
    virtual ~Field() override;

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }
    int mine_count() const { return m_mine_count; }
    int square_size() const { return 15; }
    bool is_single_chording() const { return m_single_chording; }

    void set_field_size(int rows, int columns, int mine_count);
    void set_single_chording(bool new_val);

    void reset();

private:
    virtual void paint_event(GUI::PaintEvent&) override;

    void on_square_clicked(Square&);
    void on_square_right_clicked(Square&);
    void on_square_middle_clicked(Square&);
    void on_square_chorded(Square&);
    void game_over();
    void win();
    void reveal_mines();
    void set_chord_preview(Square&, bool);
    void set_flag(Square&, bool);

    Square& square(int row, int column) { return *m_squares[row * columns() + column]; }
    const Square& square(int row, int column) const { return *m_squares[row * columns() + column]; }

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

    int m_rows { 0 };
    int m_columns { 0 };
    int m_mine_count { 0 };
    int m_unswept_empties { 0 };
    Vector<OwnPtr<Square>> m_squares;
    RefPtr<Gfx::Bitmap> m_mine_bitmap;
    RefPtr<Gfx::Bitmap> m_flag_bitmap;
    RefPtr<Gfx::Bitmap> m_badflag_bitmap;
    RefPtr<Gfx::Bitmap> m_consider_bitmap;
    RefPtr<Gfx::Bitmap> m_default_face_bitmap;
    RefPtr<Gfx::Bitmap> m_good_face_bitmap;
    RefPtr<Gfx::Bitmap> m_bad_face_bitmap;
    RefPtr<Gfx::Bitmap> m_number_bitmap[8];
    GUI::Button& m_face_button;
    GUI::Label& m_flag_label;
    GUI::Label& m_time_label;
    RefPtr<Core::Timer> m_timer;
    int m_time_elapsed { 0 };
    int m_flags_left { 0 };
    Face m_face { Face::Default };
    bool m_chord_preview { false };
    bool m_first_click { true };
    bool m_single_chording { true };
    Function<void(Gfx::Size)> m_on_size_changed;
};
