/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Marble.h"
#include "MarblePath.h"
#include <AK/Array.h>
#include <AK/Function.h>
#include <AK/HashTable.h>
#include <AK/IterationDecision.h>
#include <AK/NumericLimits.h>
#include <AK/Queue.h>
#include <AK/Random.h>
#include <AK/Vector.h>
#include <LibGfx/Point.h>
#include <LibGfx/Size.h>

class MarbleBoard final {
public:
    using Color = Marble::Color;
    using Point = Gfx::IntPoint;
    using PointArray = Vector<Point>;
    using SelectedMarble = Marble;
    using PreviewMarble = Marble;
    using MarbleArray = Vector<Marble>;

    static constexpr Gfx::IntSize board_size { 9, 9 };
    static constexpr size_t number_of_preview_marbles = 3;
    static constexpr Color empty_cell = Marble::empty_cell;

    using PreviewMarbles = Array<PreviewMarble, number_of_preview_marbles>;

    MarbleBoard()
    {
        reset();
    }

    ~MarbleBoard() = default;

    MarbleBoard(MarbleBoard const&) = delete;

    [[nodiscard]] bool has_empty_cells() const
    {
        bool result = false;
        for_each_cell([&](Point point) {
            result = is_empty_cell_at(point);
            return result ? IterationDecision::Break : IterationDecision::Continue;
        });
        return result;
    }

    [[nodiscard]] PointArray get_empty_cells() const
    {
        PointArray result;
        for_each_cell([&](Point point) {
            if (is_empty_cell_at(point))
                result.append(point);
            return IterationDecision::Continue;
        });
        shuffle(result);
        return result;
    }

    void set_preview_marble(size_t i, PreviewMarble const& marble)
    {
        VERIFY(i < number_of_preview_marbles);
        m_preview_marbles[i] = marble;
    }

    [[nodiscard]] bool place_preview_marbles_on_board()
    {
        if (!ensure_all_preview_marbles_are_on_empty_cells())
            return false;
        for (auto const& marble : m_preview_marbles)
            if (!place_preview_marble_on_board(marble))
                return false;
        return true;
    }

    [[nodiscard]] bool check_preview_marbles_are_valid()
    {
        // Check marbles pairwise and also check the board cell under this marble is empty
        static_assert(number_of_preview_marbles == 3);
        return m_preview_marbles[0].position() != m_preview_marbles[1].position() && m_preview_marbles[0].position() != m_preview_marbles[2].position()
            && m_preview_marbles[1].position() != m_preview_marbles[2].position()
            && is_empty_cell_at(m_preview_marbles[0].position())
            && is_empty_cell_at(m_preview_marbles[1].position())
            && is_empty_cell_at(m_preview_marbles[2].position());
    }

    [[nodiscard]] bool update_preview_marbles(bool use_current)
    {
        auto empty_cells = get_empty_cells();
        for (size_t i = 0; i < number_of_preview_marbles; ++i) {
            auto marble = m_preview_marbles[i];
            // Check marbles pairwise and also check the board cell under this marble is empty
            auto const is_valid_marble = [&]() {
                switch (i) {
                case 0:
                    return marble.position() != m_preview_marbles[1].position() && marble.position() != m_preview_marbles[2].position() && is_empty_cell_at(marble.position());
                case 1:
                    return marble.position() != m_preview_marbles[0].position() && marble.position() != m_preview_marbles[2].position() && is_empty_cell_at(marble.position());
                case 2:
                    return marble.position() != m_preview_marbles[0].position() && marble.position() != m_preview_marbles[1].position() && is_empty_cell_at(marble.position());
                default:
                    VERIFY_NOT_REACHED();
                }
            };
            if (use_current && is_valid_marble()) {
                continue;
            }
            while (!empty_cells.is_empty()) {
                auto const position = empty_cells.take_last();
                Color const new_color = get_random_uniform(Marble::number_of_colors);
                marble = Marble { position, new_color };
                if (!is_valid_marble())
                    continue;
                set_preview_marble(i, marble);
                break;
            }
            if (empty_cells.is_empty())
                return false;
        }
        return empty_cells.size() > 0;
    }

    [[nodiscard]] bool ensure_all_preview_marbles_are_on_empty_cells()
    {
        if (check_preview_marbles_are_valid())
            return true;
        return update_preview_marbles(true);
    }

    [[nodiscard]] Color color_at(Point point) const
    {
        VERIFY(in_bounds(point));
        return m_board[point.y()][point.x()];
    }

    void set_color_at(Point point, Color color)
    {
        VERIFY(in_bounds(point));
        m_board[point.y()][point.x()] = color;
    }

    void clear_color_at(Point point)
    {
        set_color_at(point, empty_cell);
    }

    [[nodiscard]] bool is_empty_cell_at(Point point) const
    {
        return color_at(point) == empty_cell;
    }

    [[nodiscard]] static bool in_bounds(Point point)
    {
        return point.x() >= 0 && point.x() < board_size.width() && point.y() >= 0 && point.y() < board_size.height();
    }

    [[nodiscard]] bool build_marble_path(Point from, Point to, MarblePath& path) const
    {
        path.reset();

        if (from == to || !MarbleBoard::in_bounds(from) || !MarbleBoard::in_bounds(to)) {
            return false;
        }

        struct Trace {
        public:
            using Value = u8;

            Trace() { reset(); }

            ~Trace() = default;

            [[nodiscard]] Value operator[](Point point) const
            {
                return m_map[point.y()][point.x()];
            }

            Value& operator[](Point point)
            {
                return m_map[point.y()][point.x()];
            }

            void reset()
            {
                for (size_t y = 0; y < board_size.height(); ++y)
                    for (size_t x = 0; x < board_size.width(); ++x)
                        m_map[y][x] = NumericLimits<Value>::max();
            }

        private:
            BoardMap m_map;
        };

        Trace trace;
        trace[from] = 1;

        Queue<Point> queue;
        queue.enqueue(from);

        auto add_path_point = [&](Point point, u8 value) {
            if (MarbleBoard::in_bounds(point) && is_empty_cell_at(point) && trace[point] > value) {
                trace[point] = value;
                queue.enqueue(point);
            }
        };

        constexpr Point connected_four_ways[4] = {
            { 0, -1 }, // to the top
            { 0, 1 },  // to the bottom
            { -1, 0 }, // to the left
            { 1, 0 }   // to the right
        };

        while (!queue.is_empty()) {
            auto current = queue.dequeue();
            if (current == to) {
                while (current != from) {
                    path.add_point(current);
                    for (auto delta : connected_four_ways)
                        if (auto next = current.translated(delta); MarbleBoard::in_bounds(next) && trace[next] < trace[current]) {
                            current = next;
                            break;
                        }
                }
                path.add_point(current);
                return true;
            }
            for (auto delta : connected_four_ways)
                add_path_point(current.translated(delta), trace[current] + 1);
        }
        return false;
    }

    [[nodiscard]] bool check_and_remove_marbles()
    {
        m_removed_marbles.clear();
        constexpr Point connected_four_ways[] = {
            { -1, 0 },  // to the left
            { 0, -1 },  // to the top
            { -1, -1 }, // to the top-left
            { 1, -1 }   // to the top-right
        };
        HashTable<Marble, Traits<Marble>> marbles;
        for_each_cell([&](Point current_point) {
            if (is_empty_cell_at(current_point))
                return IterationDecision::Continue;
            auto const color { color_at(current_point) };
            for (auto direction : connected_four_ways) {
                size_t marble_count = 0;
                for (auto p = current_point; in_bounds(p) && color_at(p) == color; p.translate_by(direction))
                    ++marble_count;
                if (marble_count >= number_of_marbles_to_remove)
                    for (auto p = current_point; in_bounds(p) && color_at(p) == color; p.translate_by(direction))
                        marbles.set({ p, color });
            }
            return IterationDecision::Continue;
        });
        m_removed_marbles.ensure_capacity(marbles.size());
        for (auto const& marble : marbles) {
            m_removed_marbles.append(marble);
            clear_color_at(marble.position());
        }
        return !m_removed_marbles.is_empty();
    }

    [[nodiscard]] PreviewMarbles const& preview_marbles() const
    {
        return m_preview_marbles;
    }

    [[nodiscard]] bool has_selected_marble() const
    {
        return m_selected_marble != nullptr;
    }

    [[nodiscard]] SelectedMarble const& selected_marble() const
    {
        VERIFY(has_selected_marble());
        return *m_selected_marble;
    }

    [[nodiscard]] bool select_marble(Point point)
    {
        if (!is_empty_cell_at(point)) {
            m_selected_marble = make<SelectedMarble>(point, color_at(point));
            return true;
        }
        return false;
    }

    void reset_selection()
    {
        m_selected_marble.clear();
    }

    [[nodiscard]] MarbleArray const& removed_marbles() const
    {
        return m_removed_marbles;
    }

    void reset()
    {
        reset_selection();
        for (size_t i = 0; i < number_of_preview_marbles; ++i)
            m_preview_marbles[i] = { { 0, 0 }, empty_cell };
        m_removed_marbles.clear();
        for_each_cell([&](Point point) {
            set_color_at(point, empty_cell);
            return IterationDecision::Continue;
        });
    }

private:
    static void for_each_cell(Function<IterationDecision(Point)> functor)
    {
        for (int y = 0; y < board_size.height(); ++y)
            for (int x = 0; x < board_size.width(); ++x)
                if (functor({ x, y }) == IterationDecision::Break)
                    return;
    }

    [[nodiscard]] bool place_preview_marble_on_board(PreviewMarble const& marble)
    {
        if (!is_empty_cell_at(marble.position()))
            return false;
        set_color_at(marble.position(), marble.color());
        return true;
    }

    static constexpr int number_of_marbles_to_remove { 5 };

    using Row = Array<Color, board_size.width()>;
    using BoardMap = Array<Row, board_size.height()>;

    BoardMap m_board;
    PreviewMarbles m_preview_marbles;
    MarbleArray m_removed_marbles;
    OwnPtr<SelectedMarble> m_selected_marble {};
};
