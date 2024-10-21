/*
 * Copyright (c) 2022, Oleg Kosenkov <oleg@kosenkov.ca>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ColorLines.h"
#include "HueFilter.h"
#include "Marble.h"
#include "MarbleBoard.h"
#include <AK/String.h>
#include <LibConfig/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/Emoji.h>

ColorLines::BitmapArray ColorLines::build_marble_color_bitmaps()
{
    auto marble_bitmap = MUST(Gfx::Bitmap::load_from_file("/res/graphics/colorlines/colorlines.png"sv));
    float constexpr hue_degrees[Marble::number_of_colors] = {
        0,   // Red
        45,  // Brown/Yellow
        90,  // Green
        180, // Cyan
        225, // Blue
        300  // Purple
    };
    BitmapArray colored_bitmaps;
    colored_bitmaps.ensure_capacity(Marble::number_of_colors);
    for (int i = 0; i < Marble::number_of_colors; ++i) {
        auto bitmap = MUST(marble_bitmap->clone());
        HueFilter filter { hue_degrees[i] };
        filter.apply(*bitmap, bitmap->rect(), *marble_bitmap, marble_bitmap->rect());
        colored_bitmaps.append(bitmap);
    }
    return colored_bitmaps;
}

ColorLines::BitmapArray ColorLines::build_marble_trace_bitmaps()
{
    // Use "Paw Prints" Unicode Character (U+1F43E)
    auto trace_bitmap = NonnullRefPtr<Gfx::Bitmap const>(*Gfx::Emoji::emoji_for_code_point(0x1F43E));
    BitmapArray result;
    result.ensure_capacity(number_of_marble_trace_bitmaps);
    result.append(trace_bitmap);
    result.append(MUST(result.last()->rotated(Gfx::RotationDirection::Clockwise)));
    result.append(MUST(result.last()->rotated(Gfx::RotationDirection::Clockwise)));
    result.append(MUST(result.last()->rotated(Gfx::RotationDirection::Clockwise)));
    return result;
}

ColorLines::ColorLines(StringView app_name)
    : m_app_name { app_name }
    , m_game_state { GameState::Idle }
    , m_board { make<MarbleBoard>() }
    , m_marble_bitmaps { build_marble_color_bitmaps() }
    , m_trace_bitmaps { build_marble_trace_bitmaps() }
    , m_score_font { Gfx::BitmapFont::load_from_uri("resource://fonts/MarietaBold24.font"sv) }
{
    VERIFY(m_marble_bitmaps.size() == Marble::number_of_colors);
    set_font(Gfx::FontDatabase::default_fixed_width_font().bold_variant());
    m_high_score = Config::read_i32(m_app_name, m_app_name, "HighScore"sv, 0);
    reset();
}

void ColorLines::reset()
{
    set_game_state(GameState::StartingGame);
}

void ColorLines::mousedown_event(GUI::MouseEvent& event)
{
    if (m_game_state != GameState::Idle && m_game_state != GameState::MarbleSelected)
        return;
    auto const event_position = event.position().translated(
        -frame_inner_rect().x(),
        -frame_inner_rect().y() - board_vertical_margin);
    if (event_position.x() < 0 || event_position.y() < 0)
        return;
    auto const clicked_cell = Point { event_position.x() / board_cell_dimension.width(),
        event_position.y() / board_cell_dimension.height() };
    if (!MarbleBoard::in_bounds(clicked_cell))
        return;
    if (m_board->has_selected_marble()) {
        auto const selected_cell = m_board->selected_marble().position();
        if (selected_cell == clicked_cell) {
            m_board->reset_selection();
            set_game_state(GameState::Idle);
            return;
        }
        if (m_board->is_empty_cell_at(clicked_cell)) {
            if (m_board->build_marble_path(selected_cell, clicked_cell, m_marble_path))
                set_game_state(GameState::MarbleMoving);
            return;
        }
        if (m_board->select_marble(clicked_cell))
            set_game_state(GameState::MarbleSelected);
        return;
    }
    if (m_board->select_marble(clicked_cell))
        set_game_state(GameState::MarbleSelected);
}

void ColorLines::timer_event(Core::TimerEvent&)
{
    switch (m_game_state) {
    case GameState::GeneratingMarbles:
        update();
        if (--m_marble_animation_frame < AnimationFrames::marble_generating_end) {
            m_marble_animation_frame = AnimationFrames::marble_default;
            set_game_state(GameState::CheckingMarbles);
        }
        break;

    case GameState::MarbleSelected:
        m_marble_animation_frame = (m_marble_animation_frame + 1) % AnimationFrames::number_of_marble_bounce_frames;
        update();
        break;

    case GameState::MarbleMoving:
        m_marble_animation_frame = (m_marble_animation_frame + 1) % AnimationFrames::number_of_marble_bounce_frames;
        update();
        if (m_marble_path.remaining_steps() != 1 && m_marble_animation_frame != AnimationFrames::marble_at_top)
            break;
        if (auto const point = m_marble_path.next_point(); m_marble_path.is_empty()) {
            auto const color = m_board->selected_marble().color();
            m_board->reset_selection();
            m_board->set_color_at(point, color);
            if (m_board->check_and_remove_marbles())
                set_game_state(GameState::MarblesRemoving);
            else
                set_game_state(GameState::GeneratingMarbles);
        }
        break;

    case GameState::MarblesRemoving:
        update();
        if (++m_marble_animation_frame > AnimationFrames::marble_removing_end) {
            m_marble_animation_frame = AnimationFrames::marble_default;
            m_score += 2 * m_board->removed_marbles().size();
            set_game_state(GameState::Idle);
        }
        break;

    case GameState::StartingGame:
    case GameState::Idle:
    case GameState::CheckingMarbles:
        break;

    case GameState::GameOver: {
        stop_timer();
        update();
        StringBuilder text;
        text.appendff("Your score is {}", m_score);
        if (m_score > m_high_score) {
            text.append("\nThis is a new high score!"sv);
            Config::write_i32(m_app_name, m_app_name, "HighScore"sv, int(m_high_score = m_score));
        }
        GUI::MessageBox::show(window(),
            text.string_view(),
            "Game Over"sv,
            GUI::MessageBox::Type::Information);
        reset();
        break;
    }

    default:
        VERIFY_NOT_REACHED();
    }
}

void ColorLines::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    auto paint_cell = [&](GUI::Painter& painter, Gfx::IntRect rect, int color, int animation_frame) {
        painter.draw_rect(rect, Color::Black);
        rect.shrink(0, 2, 2, 0);
        painter.draw_line(rect.bottom_left(), rect.top_left(), Color::White);
        painter.draw_line(rect.top_left(), rect.top_right(), Color::White);
        painter.draw_line(rect.top_right(), rect.bottom_right(), Color::DarkGray);
        painter.draw_line(rect.bottom_right(), rect.bottom_left(), Color::DarkGray);
        rect.shrink(1, 1, 1, 1);
        painter.draw_line(rect.bottom_left(), rect.top_left(), Color::LightGray);
        painter.draw_line(rect.top_left(), rect.top_right(), Color::LightGray);
        painter.draw_line(rect.top_right(), rect.bottom_right(), Color::MidGray);
        painter.draw_line(rect.bottom_right(), rect.bottom_left(), Color::MidGray);
        painter.fill_rect(rect, tile_color);
        rect.shrink(1, 1, 1, 1);
        if (color >= 0 && color < Marble::number_of_colors) {
            auto const source_rect = Gfx::IntRect { animation_frame * marble_pixel_size, 0, marble_pixel_size, marble_pixel_size };
            painter.draw_scaled_bitmap(rect, *m_marble_bitmaps[color], source_rect,
                1.0f, Gfx::ScalingMode::BilinearBlend);
        }
    };

    painter.set_font(*m_score_font);

    // Draw board header with score, high score
    auto board_header_size = frame_inner_rect().size();
    board_header_size.set_height(board_vertical_margin);
    auto const board_header_rect = Gfx::IntRect { frame_inner_rect().top_left(), board_header_size };
    painter.fill_rect(board_header_rect, Color::Black);

    auto const text_margin = 8;

    // Draw score
    auto const score_text = MUST(String::formatted("{:05}"sv, m_score));
    auto text_width = static_cast<int>(ceilf(m_score_font->width(score_text)));
    auto const score_text_rect = Gfx::IntRect {
        frame_inner_rect().top_left().translated(text_margin),
        Gfx::IntSize { text_width, font().pixel_size_rounded_up() }
    };
    painter.draw_text(score_text_rect, score_text, Gfx::TextAlignment::CenterLeft, text_color);

    // Draw high score
    auto const high_score_text = MUST(String::formatted("{:05}"sv, m_high_score));
    text_width = m_score_font->width(high_score_text);
    auto const high_score_text_rect = Gfx::IntRect {
        frame_inner_rect().top_right().translated(-(text_margin + text_width) - 1, text_margin),
        Gfx::IntSize { text_width, font().pixel_size_rounded_up() }
    };
    painter.draw_text(high_score_text_rect, high_score_text, Gfx::TextAlignment::CenterLeft, text_color);

    auto const cell_rect
        = Gfx::IntRect(frame_inner_rect().top_left(), board_cell_dimension)
              .translated(0, board_vertical_margin);

    // Draw all cells and the selected marble if it exists
    for (int y = 0; y < MarbleBoard::board_size.height(); ++y)
        for (int x = 0; x < MarbleBoard::board_size.width(); ++x) {
            auto const& destination_rect = cell_rect.translated(
                x * board_cell_dimension.width(),
                y * board_cell_dimension.height());
            auto const point = Point { x, y };
            auto const animation_frame = m_game_state == GameState::MarbleSelected && m_board->has_selected_marble()
                    && m_board->selected_marble().position() == point
                ? m_marble_animation_frame
                : AnimationFrames::marble_default;
            paint_cell(painter, destination_rect, m_board->color_at(point), animation_frame);
        }

    // Draw preview marbles in the board
    for (auto const& marble : m_board->preview_marbles()) {
        auto const& point = marble.position();
        if (m_marble_path.contains(point) || !m_board->is_empty_cell_at(point))
            continue;
        auto const& destination_rect = cell_rect.translated(
            point.x() * board_cell_dimension.width(),
            point.y() * board_cell_dimension.height());
        auto get_animation_frame = [this]() -> int {
            switch (m_game_state) {
            case GameState::GameOver:
                return AnimationFrames::marble_default;
            case GameState::GeneratingMarbles:
            case GameState::CheckingMarbles:
                return m_marble_animation_frame;
            default:
                return AnimationFrames::marble_generating_start;
            }
        };
        paint_cell(painter, destination_rect, marble.color(), get_animation_frame());
    }

    // Draw preview marbles in the board header
    for (size_t i = 0; i < MarbleBoard::number_of_preview_marbles; ++i) {
        auto const& marble = m_board->preview_marbles()[i];
        auto const& destination_rect = cell_rect.translated(
                                                    int(i + 3) * board_cell_dimension.width(),
                                                    -board_vertical_margin)
                                           .shrunken(10, 10);
        paint_cell(painter, destination_rect, marble.color(), AnimationFrames::marble_preview);
    }

    // Draw moving marble
    if (!m_marble_path.is_empty()) {
        auto const point = m_marble_path.current_point();
        auto const& destination_rect = cell_rect.translated(
            point.x() * board_cell_dimension.width(),
            point.y() * board_cell_dimension.height());
        paint_cell(painter, destination_rect, m_board->selected_marble().color(), m_marble_animation_frame);
    }

    // Draw removing marble
    if (m_game_state == GameState::MarblesRemoving)
        for (auto const& marble : m_board->removed_marbles()) {
            auto const& point = marble.position();
            auto const& destination_rect = cell_rect.translated(
                point.x() * board_cell_dimension.width(),
                point.y() * board_cell_dimension.height());
            paint_cell(painter, destination_rect, marble.color(), m_marble_animation_frame);
        }

    // Draw marble move trace
    if (m_game_state == GameState::MarbleMoving && m_marble_path.remaining_steps() > 1) {
        auto const trace_size = Gfx::IntSize { m_trace_bitmaps.first()->width(), m_trace_bitmaps.first()->height() };
        auto const target_trace_size = Gfx::IntSize { 14, 14 };
        auto const source_rect = Gfx::FloatRect(Gfx::IntPoint {}, trace_size);
        for (size_t i = 0; i < m_marble_path.remaining_steps() - 1; ++i) {
            auto const& current_step = m_marble_path[i];
            auto const destination_rect = Gfx::IntRect(frame_inner_rect().top_left(), target_trace_size)
                                              .translated(
                                                  current_step.x() * board_cell_dimension.width(),
                                                  board_vertical_margin + current_step.y() * board_cell_dimension.height())
                                              .translated(
                                                  (board_cell_dimension.width() - target_trace_size.width()) / 2,
                                                  (board_cell_dimension.height() - target_trace_size.height()) / 2);
            auto get_direction_bitmap_index = [&]() -> size_t {
                auto const& previous_step = m_marble_path[i + 1];
                if (previous_step.x() > current_step.x())
                    return 3;
                if (previous_step.x() < current_step.x())
                    return 1;
                if (previous_step.y() > current_step.y())
                    return 0;
                return 2;
            };
            painter.draw_scaled_bitmap(destination_rect, *m_trace_bitmaps[get_direction_bitmap_index()], source_rect,
                1.0f, Gfx::ScalingMode::BilinearBlend);
        }
    }
}

void ColorLines::restart_timer(int milliseconds)
{
    stop_timer();
    start_timer(milliseconds);
}

void ColorLines::set_game_state(GameState state)
{
    m_game_state = state;
    switch (state) {
    case GameState::StartingGame:
        m_marble_path.reset();
        m_board->reset();
        m_score = 0;
        m_marble_animation_frame = AnimationFrames::marble_default;
        update();
        if (m_board->update_preview_marbles(false))
            set_game_state(GameState::GeneratingMarbles);
        else
            set_game_state(GameState::GameOver);
        break;
    case GameState::GeneratingMarbles:
        m_board->reset_selection();
        m_marble_animation_frame = AnimationFrames::marble_generating_start;
        update();
        if (m_board->ensure_all_preview_marbles_are_on_empty_cells())
            restart_timer(TimerIntervals::generating_marbles);
        else
            set_game_state(GameState::GameOver);
        break;
    case GameState::MarblesRemoving:
        m_marble_animation_frame = AnimationFrames::marble_removing_start;
        update();
        restart_timer(TimerIntervals::removing_marbles);
        break;
    case GameState::Idle:
        m_marble_animation_frame = AnimationFrames::marble_default;
        update();
        if (m_board->ensure_all_preview_marbles_are_on_empty_cells() && m_board->has_empty_cells())
            stop_timer();
        else
            set_game_state(GameState::GameOver);
        break;
    case GameState::MarbleSelected:
        restart_timer(TimerIntervals::selected_marble);
        m_marble_animation_frame = AnimationFrames::marble_default;
        update();
        break;
    case GameState::CheckingMarbles:
        m_marble_animation_frame = AnimationFrames::marble_default;
        update();
        if (!m_board->place_preview_marbles_on_board())
            set_game_state(GameState::GameOver);
        else if (m_board->check_and_remove_marbles())
            set_game_state(GameState::MarblesRemoving);
        else
            set_game_state(GameState::Idle);
        break;
    case GameState::MarbleMoving:
        restart_timer(TimerIntervals::moving_marble);
        m_board->clear_color_at(m_board->selected_marble().position());
        update();
        break;
    case GameState::GameOver:
        m_marble_animation_frame = AnimationFrames::marble_default;
        update();
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}
