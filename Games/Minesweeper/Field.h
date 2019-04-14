#pragma once

#include <LibGUI/GFrame.h>
#include <LibCore/CTimer.h>

class SquareButton;
class GButton;
class GLabel;

struct Square {
    bool is_swept { false };
    bool has_mine { false };
    bool has_flag { false };
    int row { 0 };
    int column { 0 };
    int number { 0 };
    SquareButton* button { nullptr };
    GLabel* label { nullptr };
};

class Field final : public GFrame {
public:
    Field(GLabel& flag_label, GLabel& time_label, GButton& face_button, GWidget* parent);
    virtual ~Field() override;

    int rows() const { return m_rows; }
    int columns() const { return m_columns; }
    int mine_count() const { return m_mine_count; }
    int square_size() const { return 15; }

    void reset();

private:
    void on_square_clicked(Square&);
    void on_square_right_clicked(Square&);
    void game_over();
    void win();
    void reveal_mines();

    Square& square(int row, int column) { return m_squares[row * columns() + column]; }
    const Square& square(int row, int column) const { return m_squares[row * columns() + column]; }

    void flood_fill(Square&);

    template<typename Callback> void for_each_neighbor_of(const Square&, Callback);

    enum class Face { Default, Good, Bad };
    void set_face(Face);

    int m_rows { 9 };
    int m_columns { 9 };
    int m_mine_count { 10 };
    int m_unswept_empties { 0 };
    Vector<Square> m_squares;
    RetainPtr<GraphicsBitmap> m_mine_bitmap;
    RetainPtr<GraphicsBitmap> m_flag_bitmap;
    RetainPtr<GraphicsBitmap> m_number_bitmap[8];
    GButton& m_face_button;
    GLabel& m_flag_label;
    GLabel& m_time_label;
    CTimer m_timer;
    int m_seconds_elapsed { 0 };
    int m_flags_left { 0 };
    Face m_face { Face::Default };
};
