#pragma once

#include <LibGUI/GFrame.h>
#include <LibCore/CTimer.h>
#include <AK/Noncopyable.h>

class Field;
class GButton;
class GLabel;
class SquareButton;
class SquareLabel;

class Square {
    AK_MAKE_NONCOPYABLE(Square)
public:
    Square() { }
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

    template<typename Callback> void for_each_neighbor(Callback);
};

class Field final : public GFrame {
    friend class Square;
    friend class SquareLabel;
public:
    Field(GLabel& flag_label, GLabel& time_label, GButton& face_button, GWidget* parent);
    virtual ~Field() override;

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }
    int mine_count() const { return m_mine_count; }
    int square_size() const { return 15; }

    void set_field_size(int rows, int columns, int mine_count);

    void reset();

    Function<void()> on_size_changed;

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

    template<typename Callback> void for_each_square(Callback);

    enum class Face { Default, Good, Bad };
    void set_face(Face);

    int m_rows { 9 };
    int m_columns { 9 };
    int m_mine_count { 10 };
    int m_unswept_empties { 0 };
    Vector<OwnPtr<Square>> m_squares;
    RetainPtr<GraphicsBitmap> m_mine_bitmap;
    RetainPtr<GraphicsBitmap> m_flag_bitmap;
    RetainPtr<GraphicsBitmap> m_badflag_bitmap;
    RetainPtr<GraphicsBitmap> m_consider_bitmap;
    RetainPtr<GraphicsBitmap> m_default_face_bitmap;
    RetainPtr<GraphicsBitmap> m_good_face_bitmap;
    RetainPtr<GraphicsBitmap> m_bad_face_bitmap;
    RetainPtr<GraphicsBitmap> m_number_bitmap[8];
    GButton& m_face_button;
    GLabel& m_flag_label;
    GLabel& m_time_label;
    CTimer m_timer;
    int m_time_elapsed { 0 };
    int m_flags_left { 0 };
    Face m_face { Face::Default };
    bool m_chord_preview { false };
    bool m_first_click { true };
};
