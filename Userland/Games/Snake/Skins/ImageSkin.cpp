/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageSkin.h"
#include <LibCore/Directory.h>

namespace Snake {

ErrorOr<NonnullOwnPtr<ImageSkin>> ImageSkin::create(StringView skin_name)
{
    auto skin_directory = TRY(Core::Directory::create(ByteString::formatted("/res/graphics/snake/skins/{}", skin_name), Core::Directory::CreateDirectories::No));

    auto head = TRY(Gfx::Bitmap::load_from_file(TRY(skin_directory.open("head.png"sv, Core::File::OpenMode::Read)), "head.png"sv));
    Vector<NonnullRefPtr<Gfx::Bitmap>> head_bitmaps;
    TRY(head_bitmaps.try_ensure_capacity(4));
    TRY(head_bitmaps.try_append(head));
    TRY(head_bitmaps.try_append(TRY(head->rotated(Gfx::RotationDirection::Clockwise))));
    TRY(head_bitmaps.try_append(TRY(head_bitmaps[1]->rotated(Gfx::RotationDirection::Clockwise))));
    TRY(head_bitmaps.try_append(TRY(head_bitmaps[2]->rotated(Gfx::RotationDirection::Clockwise))));

    Vector<NonnullRefPtr<Gfx::Bitmap>> body_bitmaps;
    TRY(body_bitmaps.try_ensure_capacity(16));
    auto tail_up = TRY(Gfx::Bitmap::load_from_file(TRY(skin_directory.open("tail.png"sv, Core::File::OpenMode::Read)), "tail.png"sv));
    auto tail_right = TRY(tail_up->rotated(Gfx::RotationDirection::Clockwise));
    auto tail_down = TRY(tail_right->rotated(Gfx::RotationDirection::Clockwise));
    auto tail_left = TRY(tail_down->rotated(Gfx::RotationDirection::Clockwise));
    auto corner_ur = TRY(Gfx::Bitmap::load_from_file(TRY(skin_directory.open("corner.png"sv, Core::File::OpenMode::Read)), "corner.png"sv));
    auto corner_dr = TRY(corner_ur->rotated(Gfx::RotationDirection::Clockwise));
    auto corner_dl = TRY(corner_dr->rotated(Gfx::RotationDirection::Clockwise));
    auto corner_ul = TRY(corner_dl->rotated(Gfx::RotationDirection::Clockwise));
    auto horizontal = TRY(Gfx::Bitmap::load_from_file(TRY(skin_directory.open("horizontal.png"sv, Core::File::OpenMode::Read)), "horizontal.png"sv));
    auto vertical = TRY(Gfx::Bitmap::load_from_file(TRY(skin_directory.open("vertical.png"sv, Core::File::OpenMode::Read)), "vertical.png"sv));

    TRY(body_bitmaps.try_append(tail_up));
    TRY(body_bitmaps.try_append(corner_ur));
    TRY(body_bitmaps.try_append(vertical));
    TRY(body_bitmaps.try_append(corner_ul));

    TRY(body_bitmaps.try_append(corner_ur));
    TRY(body_bitmaps.try_append(tail_right));
    TRY(body_bitmaps.try_append(corner_dr));
    TRY(body_bitmaps.try_append(horizontal));

    TRY(body_bitmaps.try_append(vertical));
    TRY(body_bitmaps.try_append(corner_dr));
    TRY(body_bitmaps.try_append(tail_down));
    TRY(body_bitmaps.try_append(corner_dl));

    TRY(body_bitmaps.try_append(corner_ul));
    TRY(body_bitmaps.try_append(horizontal));
    TRY(body_bitmaps.try_append(corner_dl));
    TRY(body_bitmaps.try_append(tail_left));

    return adopt_nonnull_own_or_enomem(new (nothrow) ImageSkin(skin_name, move(head_bitmaps), move(body_bitmaps)));
}

ImageSkin::ImageSkin(StringView skin_name, Vector<NonnullRefPtr<Gfx::Bitmap>> head_bitmaps, Vector<NonnullRefPtr<Gfx::Bitmap>> body_bitmaps)
    : m_head_bitmaps(move(head_bitmaps))
    , m_body_bitmaps(move(body_bitmaps))
{
    m_skin_name = String::from_utf8(skin_name).release_value_but_fixme_should_propagate_errors();
}

static int image_index_from_directions(Direction from, Direction to)
{
    // Sprites are ordered in memory like this, to make the calculation easier:
    //
    // From direction
    // U R D L
    // ╹ ┗ ┃ ┛  Up    To direction
    // ┗ ╺ ┏ ━  Right
    // ┃ ┏ ╻ ┓  Down
    // ┛ ━ ┓ ╸  Left
    // (Numbered 0-15, starting top left, one row at a time.)
    //
    // This does cause some redundancy for now, but RefPtrs are small.
    return to_underlying(to) * 4 + to_underlying(from);
}

void ImageSkin::draw_head(Gfx::Painter& painter, Gfx::IntRect const& head, Direction facing_direction)
{
    auto& bitmap = m_head_bitmaps[to_underlying(facing_direction)];
    painter.draw_scaled_bitmap(head, bitmap, bitmap->rect());
}

void ImageSkin::draw_body(Gfx::Painter& painter, Gfx::IntRect const& rect, Direction previous_direction, Direction next_direction)
{
    auto& bitmap = m_body_bitmaps[image_index_from_directions(previous_direction, next_direction)];
    painter.draw_scaled_bitmap(rect, bitmap, bitmap->rect());
}

void ImageSkin::draw_tail(Gfx::Painter& painter, Gfx::IntRect const& rect, Direction body_direction)
{
    draw_body(painter, rect, body_direction, body_direction);
}

}
