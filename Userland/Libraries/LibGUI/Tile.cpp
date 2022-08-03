/*
 * Copyright (c) 2022, Filiph Sandstrom <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tile.h"
#include <AK/Random.h>
#include <LibCore/DateTime.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace GUI {

Tile::Tile()
{
    TileContent tile_content = {
        TileContent::ContentKind::Branding,
        TileContent::ContentAlignment::Center
    };
    m_contents.append(tile_content);

    m_animation_start = get_random_uniform(60 * 8) + animation_idle();
    m_tick = animation_idle();
    start_timer(16);
}

void Tile::tick()
{
    if (!animated())
        return;

    m_tick += 1;

    if (!m_animation_started && m_tick >= m_animation_start) {
        m_animation_started = true;
        m_tick = 0;
        return;
    } else if (!m_animation_started) {
        return;
    }

    repaint();
}

void Tile::draw_branding_tile(Painter painter, Gfx::IntRect content_rect)
{
    painter.blit(content_rect.center().translated(-(icon()->width() / 2), -(icon()->height() / 2)), *icon(), icon()->rect());
    paint_text(painter, content_rect.translated(6, -6), this->font(), Gfx::TextAlignment::BottomLeft);
}
void Tile::draw_normal_tile(Painter painter, Gfx::IntRect content_rect, TileContent content)
{
    auto alignment = Gfx::TextAlignment::BottomLeft;
    switch (content.content_alignment) {
    case TileContent::ContentAlignment::Center:
        alignment = Gfx::TextAlignment::Center;
        break;
    case TileContent::ContentAlignment::Bottom:
    default:
        alignment = Gfx::TextAlignment::BottomLeft;
        break;
    }

    painter.draw_text(content_rect.shrunken(12, 12), content.content, this->font(), alignment, palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::Wrap);
}
void Tile::draw_date_tile(Painter painter, Gfx::IntRect content_rect)
{
    painter.draw_text(content_rect.translated(0, -(this->font().x_height())), Core::DateTime::now().to_string("%A"sv), this->font(), Gfx::TextAlignment::Center, palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::Wrap);
    painter.draw_text(content_rect.translated(0, this->font().x_height()), Core::DateTime::now().to_string("%e"sv), this->font().bold_variant(), Gfx::TextAlignment::Center, palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::Wrap);
}

void Tile::tick_tile(Painter painter)
{
    int const stages = m_contents.size() - 1;
    VERIFY(stages >= 0);

    auto const stage_duration = animation_idle() + animation_duration();

    // Include the fade to first stage duration too
    if (m_tick >= (stages + 1) * stage_duration)
        m_tick = 0;

    // Each stage has one in animation and one idle wait
    auto const stage = m_tick / stage_duration;

    // Find the curent stage's actual tick
    auto const actual_tick = m_tick - (stage * stage_duration);
    auto const in_transition = actual_tick <= animation_duration();

    TileContent previous_content;
    TileContent current_content;

    if (stage == 0) {
        previous_content = m_contents.last();
    } else {
        previous_content = m_contents[stage - 1];
    }

    if (stage == stages) {
        current_content = m_contents.last();
    } else {
        current_content = m_contents[stage];
    }

    auto previous_content_rect = rect().translated(0, -height());
    auto current_content_rect = rect();

    if (in_transition) {
        auto animation_state = process_animation(actual_tick);
        previous_content_rect = animation_state.previous_rect;
        current_content_rect = animation_state.current_rect;
    }

    for (int i = in_transition ? 0 : 1; i < 2; i++) {
        auto content = i == 0 ? previous_content : current_content;
        auto content_rect = i == 0 ? previous_content_rect : current_content_rect;

        switch (content.content_kind) {
        case TileContent::ContentKind::Branding:
            draw_branding_tile(painter, content_rect);
            break;
        case TileContent::ContentKind::Normal:
            draw_normal_tile(painter, content_rect, content);
            break;
        case TileContent::ContentKind::Date:
            draw_date_tile(painter, content_rect);
            break;
        default:
            VERIFY_NOT_REACHED();
        }
    }
}

Tile::Animation Tile::process_animation(int tick)
{
    // TODO: type & direction
    auto previous_rect = rect();
    auto current_rect = rect();

    auto y_translation = current_rect.height() - current_rect.height() * pow(0.8, tick);
    if (y_translation > current_rect.height())
        y_translation = current_rect.height();

    previous_rect.translate_by(0, -y_translation);
    current_rect.translate_by(0, current_rect.bottom() - y_translation);

    return {
        previous_rect,
        current_rect
    };
}

void Tile::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());

    Gfx::StylePainter::paint_button(painter, rect(), palette(), button_style(), is_being_pressed(), is_hovered(), is_checked(), is_enabled());

    tick_tile(painter);
}

}
