#pragma once

#include <LibGUI/GFrame.h>

class SquareButton;
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
    explicit Field(GWidget* parent);
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

    Square& square(int row, int column) { return m_squares[row * columns() + column]; }
    const Square& square(int row, int column) const { return m_squares[row * columns() + column]; }

    void flood_fill(Square&);

    template<typename Callback> void for_each_neighbor_of(const Square&, Callback);

    int m_rows { 9 };
    int m_columns { 9 };
    int m_mine_count { 10 };
    Vector<Square> m_squares;
    RetainPtr<GraphicsBitmap> m_mine_bitmap;
    RetainPtr<GraphicsBitmap> m_flag_bitmap;
    RetainPtr<GraphicsBitmap> m_number_bitmap[8];
};
