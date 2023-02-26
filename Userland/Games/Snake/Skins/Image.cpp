/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Image.h"
#include <LibConfig/Client.h>

namespace Snake {

ImageSkin::ImageSkin()
{
    m_skin_name = Config::read_string("Snake"sv, "Snake"sv, "SnakeSkin"sv);
    auto result = load_skins();
    auto path = DeprecatedString::formatted("/res/icons/snake/skins/{}/", m_skin_name);
    dbgln("Path to skin files: {}", path);
    if (result.is_error())
        dbgln("Error: Could not load skin: {}", result.error());
}

void ImageSkin::set_skin_name(StringView const& skin)
{
    auto old_skin_name = m_skin_name;
    m_skin_name = skin;
    Config::write_string("Snake"sv, "Snake"sv, "SnakeSkin"sv, skin);

    if (old_skin_name != skin)
        MUST(load_skins());
}

ErrorOr<NonnullRefPtr<Gfx::Bitmap>> ImageSkin::load_skin_bitmap(StringView const& file)
{
    auto path = DeprecatedString::formatted("/res/icons/snake/skins/{}/{}", m_skin_name, file);
    return Gfx::Bitmap::load_from_file(path);
}

ErrorOr<void> ImageSkin::load_skins()
{
    m_body_bitmaps.clear_with_capacity();
    auto tail = TRY(load_skin_bitmap("tail.png"sv));
    auto corner = TRY(load_skin_bitmap("corner.png"sv));
    auto horizontal = TRY(load_skin_bitmap("horizontal.png"sv));
    auto vertical = TRY(load_skin_bitmap("vertical.png"sv));

    m_body_bitmaps.append(tail);
    m_body_bitmaps.append(TRY(tail->rotated(Gfx::RotationDirection::Clockwise)));
    m_body_bitmaps.append(corner);
    m_body_bitmaps.append(TRY(m_body_bitmaps[1].rotated(Gfx::RotationDirection::Clockwise)));
    m_body_bitmaps.append(vertical);
    m_body_bitmaps.append(TRY(corner->rotated(Gfx::RotationDirection::Clockwise)));
    m_body_bitmaps.append(tail); // Not possible
    m_body_bitmaps.append(TRY(m_body_bitmaps[3].rotated(Gfx::RotationDirection::Clockwise)));
    m_body_bitmaps.append(TRY(corner->rotated(Gfx::RotationDirection::CounterClockwise)));
    m_body_bitmaps.append(horizontal);
    m_body_bitmaps.append(tail); // Not possible
    m_body_bitmaps.append(TRY(m_body_bitmaps[5].rotated(Gfx::RotationDirection::Clockwise)));

    m_head_bitmaps.clear_with_capacity();
    auto head = TRY(load_skin_bitmap("head.png"sv));
    m_head_bitmaps.append(head);
    m_head_bitmaps.append(TRY(head->rotated(Gfx::RotationDirection::Clockwise)));
    m_head_bitmaps.append(TRY(m_head_bitmaps[1].rotated(Gfx::RotationDirection::Clockwise)));
    m_head_bitmaps.append(TRY(m_head_bitmaps[2].rotated(Gfx::RotationDirection::Clockwise)));

    return {};
}

int configure_direction(int value, Gfx::Point<int> direction)
{
    if (direction.y() < 0)
        value |= 1;
    if (direction.x() > 0)
        value |= 2;
    if (direction.y() > 1)
        value |= 4;
    if (direction.x() < 0)
        value |= 8;
    return value;
}

void ImageSkin::draw_head(Gfx::Painter& painter, Gfx::IntRect const& head, Gfx::IntRect const& body)
{
    auto configuration = configure_direction(0, head.location() - body.location());

    auto& bitmap = m_head_bitmaps[(size_t)log2(configuration)];
    painter.draw_scaled_bitmap(head, bitmap, bitmap.rect());
}

void ImageSkin::draw_body(Gfx::Painter& painter, Gfx::IntRect const& head, Gfx::IntRect const& body, Gfx::IntRect const& tail)
{
    auto configuration = configure_direction(0, head.location() - body.location());
    configuration = configure_direction(configuration, tail.location() - body.location());
    configuration -= 1; // Bitmaps are zero indexed

    auto& bitmap = m_body_bitmaps[configuration];
    painter.draw_scaled_bitmap(body, bitmap, bitmap.rect());
}
void ImageSkin::draw_tail(Gfx::Painter& painter, Gfx::IntRect const& body, Gfx::IntRect const& tail)
{
    draw_body(painter, body, tail, tail);
}

}
