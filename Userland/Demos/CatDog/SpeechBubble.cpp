/*
 * Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SpeechBubble.h"
#include <AK/Array.h>
#include <AK/Random.h>
#include <AK/StringView.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Palette.h>

static Array<StringView, 3> default_messages = {
    "It looks like you're trying to debug\na program. Would you like some help?"sv,
    "It looks like you're trying to shave\na yak. Would you like some help?"sv,
    "Well Hello Friend!"sv,
};

static Array<StringView, 3> artist_messages = {
    "It looks like you're creating art.\nWould you like some help?"sv,
    "It looks like you're making a meme\nfor Discord. \U0010CD65"sv,
    "It looks like you're using the filter\ngallery. Would you like a suggestion?"sv,
};
static Array<StringView, 3> inspector_messages = {
    "It looks like you're trying to kill\na program. Would you like some help?"sv,
    "It looks like you're profiling a\nprogram. Would you like some help?"sv,
    "It looks like you're interested in\nCPU usage. Would you like some help?"sv,
};

void SpeechBubble::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    painter.clear_rect(rect(), Gfx::Color());

    constexpr auto background_color = Gfx::Color::from_rgb(0xeaf688);

    auto text_area = rect();
    text_area.set_height(text_area.height() - 10);
    painter.draw_rect(text_area, palette().active_window_border1());
    text_area.shrink(2, 2);
    painter.fill_rect(text_area, background_color);

    auto connector_top_left = Gfx::IntPoint { rect().width() / 2 - 5, text_area.height() + 1 };
    auto connector_top_right = Gfx::IntPoint { rect().width() / 2 + 5, text_area.height() + 1 };
    auto connector_bottom = Gfx::IntPoint { rect().width() / 2 + 10, rect().height() };
    painter.draw_triangle(connector_top_left, connector_top_right, connector_bottom, background_color);
    painter.draw_line(connector_top_left, Gfx::IntPoint { connector_bottom.x() - 1, connector_bottom.y() }, palette().active_window_border1());
    painter.draw_line(connector_top_right, connector_bottom, palette().active_window_border1());

    auto& message_list = m_cat_dog->is_artist() ? artist_messages : (m_cat_dog->is_inspector() ? inspector_messages : default_messages);
    auto message = message_list[get_random<u8>() % message_list.size()];
    painter.draw_text(text_area, message, Gfx::TextAlignment::Center);
}

void SpeechBubble::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    if (on_dismiss)
        on_dismiss();
}
