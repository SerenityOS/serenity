#pragma once

#include <AK/CircularQueue.h>
#include <AK/NonnullRefPtrVector.h>
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

    struct Velocity {
        int vertical { 0 };
        int horizontal { 0 };
    };

    void game_over();
    void spawn_fruit();
    bool is_available(const Coordinate&);
    void queue_velocity(int v, int h);
    const Velocity& last_velocity() const;
    Rect cell_rect(const Coordinate&) const;
    Rect score_rect() const;
    Rect high_score_rect() const;

    int m_rows { 20 };
    int m_columns { 20 };

    Velocity m_velocity { 0, 1 };
    Velocity m_last_velocity { 0, 1 };

    CircularQueue<Velocity, 10> m_velocity_queue;

    Coordinate m_head;
    Vector<Coordinate> m_tail;

    Coordinate m_fruit;
    int m_fruit_type { 0 };

    int m_length { 0 };
    unsigned m_score { 0 };
    String m_score_text;
    unsigned m_high_score { 0 };
    String m_high_score_text;

    NonnullRefPtrVector<GraphicsBitmap> m_fruit_bitmaps;
};
