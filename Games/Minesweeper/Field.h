#pragma once

#include <AK/Noncopyable.h>
#include <LibCore/CTimer.h>
#include <LibGUI/GFrame.h>

class Field;
class GButton;
class GLabel;
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

class Field final : public GFrame {
    friend class Square;
    friend class SquareLabel;

public:
    Field(GLabel& flag_label, GLabel& time_label, GButton& face_button, GWidget* parent, Function<void(Size)> on_size_changed);
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
    virtual void paint_event(GPaintEvent&) override;

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
    void flood_mark(Square&);

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
    RefPtr<GraphicsBitmap> m_mine_bitmap;
    RefPtr<GraphicsBitmap> m_flag_bitmap;
    RefPtr<GraphicsBitmap> m_badflag_bitmap;
    RefPtr<GraphicsBitmap> m_consider_bitmap;
    RefPtr<GraphicsBitmap> m_default_face_bitmap;
    RefPtr<GraphicsBitmap> m_good_face_bitmap;
    RefPtr<GraphicsBitmap> m_bad_face_bitmap;
    RefPtr<GraphicsBitmap> m_number_bitmap[8];
    GButton& m_face_button;
    GLabel& m_flag_label;
    GLabel& m_time_label;
    CTimer m_timer;
    int m_time_elapsed { 0 };
    int m_flags_left { 0 };
    Face m_face { Face::Default };
    bool m_chord_preview { false };
    bool m_first_click { true };
    bool m_single_chording { true };
    Function<void(Size)> m_on_size_changed;
};
