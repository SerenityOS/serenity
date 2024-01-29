/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventCalendar.h"
#include <LibDateTime/ISOCalendar.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(::Calendar, EventCalendar);

namespace Calendar {

static constexpr int tile_breakpoint = 50;

EventCalendar::EventCalendar(DateTime::LocalDateTime date_time, Mode mode)
    : Calendar(date_time, mode)
    , m_event_manager(EventManager::create())
{
}

void EventCalendar::paint_tile(GUI::Painter& painter, GUI::Calendar::Tile& tile, Gfx::IntRect& tile_rect, int x_offset, int y_offset, int day_offset)
{
    Calendar::paint_tile(painter, tile, tile_rect, x_offset, y_offset, day_offset);

    if (tile.width < tile_breakpoint || tile.height < tile_breakpoint)
        return;

    auto index = 0;
    auto font_height = font().x_height();
    for (auto const& event : m_event_manager->events()) {
        auto start = event.start.to_parts<DateTime::ISOCalendar>();
        if (start.year == static_cast<i32>(tile.year) && start.month == tile.month && start.day_of_month == tile.day) {
            auto text_rect = tile.rect.translated(4, 4 + (font_height + 4) * ++index);

            auto event_text = String::formatted("{:{H}:{M}} {}", event.start, event.summary);
            if (event_text.is_error())
                continue;

            painter.draw_text(
                text_rect,
                event_text.release_value(),
                Gfx::FontDatabase::default_font(),
                Gfx::TextAlignment::TopLeft,
                palette().base_text(),
                Gfx::TextElision::Right);
        }
    }
}

}
