/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ScoreCard.h"
#include <LibGUI/Button.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Font/Font.h>

namespace Hearts {

ScoreCard::ScoreCard(Player (&players)[4], bool game_over)
    : m_players(players)
    , m_game_over(game_over)
{
    set_min_size(recommended_size());
    resize(recommended_size());
}

Gfx::IntSize ScoreCard::recommended_size()
{
    auto& card_font = font().bold_variant();

    return Gfx::IntSize {
        4 * column_width + 3 * cell_padding,
        16 * card_font.pixel_size_rounded_up() + 15 * cell_padding
    };
}
void ScoreCard::paint_event(GUI::PaintEvent& event)
{
    GUI::Widget::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(frame_inner_rect());
    painter.add_clip_rect(event.rect());

    auto& font = painter.font().bold_variant();

    auto cell_rect = [this, &font](int x, int y) {
        return Gfx::IntRect {
            frame_inner_rect().left() + x * column_width + x * cell_padding,
            frame_inner_rect().top() + y * font.pixel_size_rounded_up() + y * cell_padding,
            column_width,
            font.pixel_size_rounded_up(),
        };
    };

    VERIFY(!m_players[0].scores.is_empty());

    int leading_score = -1;
    for (size_t player_index = 0; player_index < 4; player_index++) {
        auto& player = m_players[player_index];
        auto cumulative_score = player.scores[player.scores.size() - 1];
        if (leading_score == -1 || cumulative_score < leading_score)
            leading_score = cumulative_score;
    }

    for (int player_index = 0; player_index < 4; player_index++) {
        auto& player = m_players[player_index];
        auto cumulative_score = player.scores[player.scores.size() - 1];
        auto leading_color = m_game_over ? Color::Magenta : Color::Blue;
        auto text_color = cumulative_score == leading_score ? leading_color : Color::Black;
        dbgln("text_rect: {}", cell_rect(player_index, 0));
        painter.draw_text(cell_rect(player_index, 0),
            player.name,
            font, Gfx::TextAlignment::Center,
            text_color);
        for (int score_index = 0; score_index < (int)player.scores.size(); score_index++) {
            auto text_rect = cell_rect(player_index, 1 + score_index);
            auto score_text = ByteString::formatted("{}", player.scores[score_index]);
            auto score_text_width = font.width_rounded_up(score_text);
            if (score_index != (int)player.scores.size() - 1) {
                painter.draw_line(
                    { text_rect.left() + text_rect.width() / 2 - score_text_width / 2 - 3, text_rect.top() + font.pixel_size_rounded_up() / 2 },
                    { text_rect.right() - text_rect.width() / 2 + score_text_width / 2 + 2, text_rect.top() + font.pixel_size_rounded_up() / 2 },
                    text_color);
            }
            painter.draw_text(text_rect,
                score_text,
                font, Gfx::TextAlignment::Center,
                text_color);
        }
    }
}

}
