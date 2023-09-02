/*
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EventCalendar.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Palette.h>

REGISTER_WIDGET(::Calendar, EventCalendar);

namespace Calendar {

static constexpr int tile_breakpoint = 50;

EventCalendar::EventCalendar(Core::DateTime date_time, Mode mode)
    : Calendar(date_time, mode)
    , m_event_manager(EventManager::create())
{
}

void EventCalendar::paint_tile(GUI::Painter& painter, GUI::Calendar::Tile& tile, Gfx::IntRect& tile_rect, int x_offset, int y_offset, int day_offset)
{
    Calendar::paint_tile(painter, tile, tile_rect, x_offset, y_offset, day_offset);

    auto events = m_event_manager->events();

    if (tile.width > tile_breakpoint && tile.height > tile_breakpoint) {
        auto index = 0;
        auto font_height = font().x_height();
        events.for_each([&](JsonValue const& value) {
            auto const& event = value.as_object();

            if (!event.has("start_date"sv) || !event.has("start_date"sv) || !event.has("summary"sv))
                return;

            auto start_date = event.get("start_date"sv).value().to_deprecated_string();
            auto start_time = event.get("start_time"sv).value().to_deprecated_string();
            auto summary = event.get("summary"sv).value().to_deprecated_string();
            auto combined_text = DeprecatedString::formatted("{} {}", start_time, summary);

            if (start_date == DeprecatedString::formatted("{}-{:0>2d}-{:0>2d}", tile.year, tile.month, tile.day)) {

                auto text_rect = tile.rect.translated(4, 4 + (font_height + 4) * ++index);
                painter.draw_text(text_rect, combined_text, Gfx::FontDatabase::default_font(), Gfx::TextAlignment::TopLeft, palette().base_text(), Gfx::TextElision::Right);
            }
        });
    }
}

}
