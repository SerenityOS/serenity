#pragma once

#include <LibGUI/GWidget.h>

class SnakeGame : public GWidget {
public:
    explicit SnakeGame(GWidget* parent = nullptr);
    virtual ~SnakeGame() override;

    void reset();

private:
    virtual void paint_event(GPaintEvent&) override;
    virtual void keydown_event(GKeyEvent&) override;
    virtual void timer_event(CTimerEvent&) override;

    struct Coordinate {
        int row { 0 };
        int column { 0 };

        bool operator==(const Coordinate& other) const
        {
            return row == other.row && column == other.column;
        }
    };

    void game_over();
    void spawn_fruit();
    bool is_available(const Coordinate&);

    int m_rows { 20 };
    int m_columns { 20 };

    int m_horizontal_velocity { 1 };
    int m_vertical_velocity { 0 };

    int m_last_horizontal_velocity { 1 };
    int m_last_vertical_velocity { 0 };

    Coordinate m_head;
    Vector<Coordinate> m_tail;

    Coordinate m_fruit;

    int m_length { 0 };
};
