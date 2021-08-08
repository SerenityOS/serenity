/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Bitmap.h>
#include <AK/Debug.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Remote/RemoteGfx.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace RemoteGfx {

BitmapData::BitmapData(Gfx::Bitmap const& bitmap, Gfx::IntRect const& rect)
    : m_physical_rect(rect * bitmap.scale())
    , m_bytes(ByteBuffer::create_uninitialized(Gfx::Bitmap::minimum_pitch(m_physical_rect.width(), bitmap.format()) * (size_t)m_physical_rect.height()).release_value())
{
    VERIFY(bitmap.physical_rect().contains(m_physical_rect));
    auto* dst_bytes = m_bytes.data();
    size_t dst_bytes_per_line = Gfx::Bitmap::minimum_pitch(m_physical_rect.width(), bitmap.format());
    VERIFY(m_physical_rect.height() * dst_bytes_per_line <= m_bytes.size());
    size_t src_x_offset = Gfx::Bitmap::minimum_pitch(m_physical_rect.left(), bitmap.format());
    for (int y = m_physical_rect.top(); y <= m_physical_rect.bottom(); y++) {
        __builtin_memcpy(dst_bytes, (u8 const*)bitmap.scanline(y) + src_x_offset, dst_bytes_per_line);
        dst_bytes += dst_bytes_per_line;
    }
    VERIFY(dst_bytes <= m_bytes.data() + m_bytes.size());
}

void BitmapData::apply_to(Gfx::Bitmap& bitmap) const
{
    dbgln_if(REMOTE_GFX_DEBUG, "Applying {} bytes of BitmapData to physical rect: {} rect: {}", m_bytes.size(), m_physical_rect, Gfx::IntRect { m_physical_rect.left() / bitmap.scale(), m_physical_rect.top() / bitmap.scale(), m_physical_rect.width() / bitmap.scale(), m_physical_rect.height() / bitmap.scale() });
    VERIFY(bitmap.physical_rect().contains(m_physical_rect));
    auto* src_bytes = m_bytes.data();
    size_t src_bytes_per_line = Gfx::Bitmap::minimum_pitch(m_physical_rect.width(), bitmap.format());
    size_t dst_x_offset = Gfx::Bitmap::minimum_pitch(m_physical_rect.left(), bitmap.format());
    for (int y = m_physical_rect.top(); y <= m_physical_rect.bottom(); y++) {
        __builtin_memcpy((u8*)bitmap.scanline(y) + dst_x_offset, src_bytes, src_bytes_per_line);
        src_bytes += src_bytes_per_line;
    }
    VERIFY(src_bytes <= m_bytes.data() + m_bytes.size());
}

BitmapDiff BitmapDiff::create(BitmapId id, Gfx::Bitmap const& original, Gfx::Bitmap const& changed, Gfx::DisjointRectSet const& change_rects)
{
    VERIFY(original.scale() == changed.scale());

    BitmapDiff diff { .id = id };

    // This creates a bitmap with one bit representing each modified tile in the
    // bounding rect of change_rects, followed by the new bitmap data of each such tile
    // However, only any tiles that are within change_rects are considered, any tile that
    // doesn't intersect with change_rects will be ignored (but still use a cleared bit in
    // the bitmap to be able to apply the diff without having to know these rectangles).
    auto bounds = change_rects.is_empty() ? changed.rect() : Gfx::IntRect::united(change_rects.rects()).intersected(changed.rect());
    if (bounds.is_empty()) {
        dbgln_if(REMOTE_GFX_DEBUG, "Created diff (empty bounds) for bitmap {}", diff.id);
        return diff;
    }
    diff.location = bounds.location();
    diff.size = bounds.size();
    Gfx::IntSize tiles { ceil_div(bounds.width(), max_tile_size), ceil_div(bounds.height(), max_tile_size) };
    VERIFY(!tiles.is_empty());
    int tiles_count = tiles.width() * tiles.height();
    size_t changed_tiles_bitmap_bytes = ceil_div(tiles_count, 8);
    auto pitch = changed.minimum_pitch(max_tile_size * changed.scale(), changed.format());
    auto last_column_width = bounds.intersected(changed.rect()).width() % max_tile_size;
    if (last_column_width == 0)
        last_column_width = max_tile_size;
    auto last_column_physical_pitch = changed.minimum_pitch(last_column_width * changed.scale(), changed.format());
    auto last_row_height = bounds.intersected(changed.rect()).height() % max_tile_size;
    if (last_row_height == 0)
        last_row_height = max_tile_size;
    auto last_row_physical_lines = last_row_height * changed.scale();
    auto one_tile_bytes = Gfx::Bitmap::size_in_bytes(pitch, max_tile_size);

    // Calculate how many bytes we would need if we copied the entire bounding rect
    auto pitch_for_one_bitmap = changed.minimum_pitch(bounds.width() * changed.scale(), changed.format());
    auto bytes_for_one_bitmap = Gfx::Bitmap::size_in_bytes(pitch_for_one_bitmap, bounds.height() * changed.scale());

    // TODO: We may over-allocate a bit
    diff.bytes = ByteBuffer::create_zeroed(changed_tiles_bitmap_bytes + one_tile_bytes * tiles_count).release_value();
    Bitmap changed_tile_bitmap(diff.bytes.data(), tiles_count);

    // Can't create a big bitmap due to the INT16_MAX limit in Gfx::Bitmap, so create one and then move the wrapper data pointer as we go
    u8* changed_bitmap_data_start_ptr = diff.bytes.data() + changed_tiles_bitmap_bytes;
    u8* changed_bitmap_data_ptr = changed_bitmap_data_start_ptr;
    auto changed_tiles = Gfx::Bitmap::try_create_wrapper(changed.format(), { max_tile_size, max_tile_size }, changed.scale(), pitch, changed_bitmap_data_ptr).release_value();                                                                                           // TODO: GUI::BitmapView?
    auto changed_tiles_last_column = last_column_width == max_tile_size ? changed_tiles : Gfx::Bitmap::try_create_wrapper(changed.format(), { last_column_width, max_tile_size }, changed.scale(), last_column_physical_pitch, changed_bitmap_data_ptr).release_value(); // TODO: GUI::BitmapView?
    Gfx::Painter painter(*changed_tiles);
    Gfx::Painter painter_last_column(*changed_tiles_last_column);
    int current_tile_top = bounds.top();
    size_t tiles_changed = 0;
    for (int y = 0; y < tiles.height(); y++) {
        for (int x = 0; x < tiles.width(); x++) {
            Gfx::IntRect tile_rect { bounds.left() + x * max_tile_size, current_tile_top, max_tile_size, max_tile_size };
            if ((change_rects.is_empty() || change_rects.intersects(tile_rect)) && !original.is_rect_equal(tile_rect, changed, tile_rect.location())) {
                changed_tile_bitmap.set(y * tiles.width() + x, true);
                if (x < tiles.width() - 1) {
                    changed_tiles->wrapper_set_data(changed_bitmap_data_ptr); // TODO: GUI::BitmapView?
                    painter.blit({}, changed, tile_rect, 1.0f, false);
                    changed_bitmap_data_ptr += y < tiles.height() - 1 ? one_tile_bytes : Gfx::Bitmap::size_in_bytes(pitch, last_row_physical_lines);
                } else {
                    changed_tiles_last_column->wrapper_set_data(changed_bitmap_data_ptr); // TODO: GUI::BitmapView?
                    painter_last_column.blit({}, changed, tile_rect, 1.0f, false);
                    changed_bitmap_data_ptr += y < tiles.height() - 1 ? one_tile_bytes : Gfx::Bitmap::size_in_bytes(last_column_physical_pitch, last_row_physical_lines);
                }
                tiles_changed++;
                if (changed_tiles_bitmap_bytes + (changed_bitmap_data_ptr - changed_bitmap_data_start_ptr) >= bytes_for_one_bitmap) {
                    // It's less expensive at this point to just do a straight copy of the bitmap
                    diff.flags = (DiffFlags)((u8)diff.flags | (u8)DiffFlags::OneBitmap);
                    VERIFY(diff.bytes.size() >= bytes_for_one_bitmap);
                    auto one_bitmap = Gfx::Bitmap::try_create_wrapper(changed.format(), bounds.size(), changed.scale(), pitch_for_one_bitmap, diff.bytes.data()).release_value(); // TODO: GUI::BitmapView?
                    VERIFY(one_bitmap->size_in_bytes() == bytes_for_one_bitmap);
                    Gfx::Painter one_bitmap_painter(*one_bitmap);
                    one_bitmap_painter.blit({}, changed, bounds, 1.0f, false);
                    diff.bytes.resize(bytes_for_one_bitmap);
                    dbgln_if(REMOTE_GFX_DEBUG, "Created diff (one bitmap) for bitmap {} with {} bytes {}%", diff.id, diff.bytes.size(), (bytes_for_one_bitmap * 100) / changed.size_in_bytes());
                    return diff;
                }
            }
        }
        current_tile_top += max_tile_size;
    }

    if (tiles_changed == 0) {
        dbgln_if(REMOTE_GFX_DEBUG, "Created diff (empty) for bitmap {}", diff.id);
        for (auto& r : change_rects.rects())
            dbgln_if(REMOTE_GFX_DEBUG, "    change_rects: {} same: {}", r, original.is_rect_equal(r, changed, r.location()));
        diff.bytes = {};
        return diff;
    }

    auto tiles_bytes = changed_bitmap_data_ptr - changed_bitmap_data_start_ptr;
    diff.bytes.resize(changed_tiles_bitmap_bytes + tiles_bytes);
    dbgln_if(REMOTE_GFX_DEBUG, "Created diff for bitmap {} with {} bytes ({}/{} tiles changed at {} {}, {} bytes per tile) {}%", diff.id, diff.bytes.size(), tiles_changed, tiles_count, diff.location, diff.size, one_tile_bytes, (tiles_bytes * 100) / (bounds.width() * bounds.height() * changed.bpp() / 8));
    return diff;
}

void BitmapDiff::apply_to_bitmap(Gfx::Bitmap& bitmap, Gfx::DisjointRectSet* changed_rects) const
{
    if (bytes.is_empty())
        return; // nothing to apply
    if ((u8)flags & (u8)DiffFlags::OneBitmap) {
        // size is in pixels for DiffFlags::OneBitmap
        Gfx::IntRect bounds { location, size };
        VERIFY(bitmap.rect().contains(bounds));
        auto pitch_for_one_bitmap = bitmap.minimum_pitch(bounds.width() * bitmap.scale(), bitmap.format());
        auto one_bitmap = Gfx::Bitmap::try_create_wrapper(bitmap.format(), bounds.size(), bitmap.scale(), pitch_for_one_bitmap, const_cast<u8*>(bytes.data())).release_value(); // TODO: GUI::BitmapView?
        Gfx::Painter painter(bitmap);
        painter.blit(bounds.location(), one_bitmap, { {}, bounds.size() }, 1.0f, false);
        if (changed_rects)
            changed_rects->add(bounds);
        return;
    }
    Gfx::IntSize tiles { ceil_div(size.width(), max_tile_size), ceil_div(size.height(), max_tile_size) };
    int tiles_count = tiles.width() * tiles.height();
    size_t changed_tiles_bitmap_bytes = ceil_div(tiles_count, 8);
    BitmapView changed_tile_bitmap(const_cast<u8*>(bytes.data()), tiles_count);
    auto bounds = Gfx::IntRect { location, size }.intersected(bitmap.rect());
    auto pitch = bitmap.minimum_pitch(max_tile_size * bitmap.scale(), bitmap.format());
    auto last_column_width = bounds.width() % max_tile_size;
    if (last_column_width == 0)
        last_column_width = max_tile_size;
    auto last_column_physical_pitch = bitmap.minimum_pitch(last_column_width * bitmap.scale(), bitmap.format());
    auto last_row_height = bounds.height() % max_tile_size;
    if (last_row_height == 0)
        last_row_height = max_tile_size;
    auto last_row_physical_lines = last_row_height * bitmap.scale();
    auto one_tile_bytes = Gfx::Bitmap::size_in_bytes(pitch, max_tile_size);

    // Can't create a big bitmap due to the INT16_MAX limit in Gfx::Bitmap, so create one and then move the wrapper data pointer as we go
    auto* changed_bitmap_data_ptr = bytes.data() + changed_tiles_bitmap_bytes;
    auto changed_tiles = Gfx::Bitmap::try_create_wrapper(bitmap.format(), { max_tile_size, max_tile_size }, bitmap.scale(), pitch, const_cast<u8*>(changed_bitmap_data_ptr)).release_value();                                                                                           // TODO: GUI::BitmapView?
    auto changed_tiles_last_column = last_column_width == max_tile_size ? changed_tiles : Gfx::Bitmap::try_create_wrapper(bitmap.format(), { last_column_width, max_tile_size }, bitmap.scale(), last_column_physical_pitch, const_cast<u8*>(changed_bitmap_data_ptr)).release_value(); // TODO: GUI::BitmapView?
    Gfx::Painter painter(bitmap);
    for (int y = 0; y < tiles.height(); y++) {
        for (int x = 0; x < tiles.width(); x++) {
            // TODO: This is really slow. BitmapView should help us out with a for_each rather than checking every single bit one at a time...
            if (!changed_tile_bitmap.get(y * tiles.width() + x))
                continue;

            Gfx::IntPoint change_location { location.x() + (x * max_tile_size), location.y() + (y * max_tile_size) };
            Gfx::IntSize tile_size {
                (x < tiles.width() - 1) ? max_tile_size : last_column_width,
                (y < tiles.height() - 1) ? max_tile_size : last_row_height
            };
            if (x < tiles.width() - 1) {
                changed_tiles->wrapper_set_data(const_cast<u8*>(changed_bitmap_data_ptr)); // TODO: GUI::BitmapView?
                painter.blit(change_location, *changed_tiles, { {}, tile_size }, 1.0f, false);
                changed_bitmap_data_ptr += y < tiles.height() - 1 ? one_tile_bytes : Gfx::Bitmap::size_in_bytes(pitch, last_row_physical_lines);
            } else {
                changed_tiles_last_column->wrapper_set_data(const_cast<u8*>(changed_bitmap_data_ptr)); // TODO: GUI::BitmapView?
                painter.blit(change_location, *changed_tiles_last_column, { {}, tile_size }, 1.0f, false);
                changed_bitmap_data_ptr += y < tiles.height() - 1 ? one_tile_bytes : Gfx::Bitmap::size_in_bytes(last_column_physical_pitch, last_row_physical_lines);
            }
            if (changed_rects)
                changed_rects->add({ change_location, tile_size });
        }
    }
}

PaletteData::PaletteData(Gfx::Palette const& palette)
    : m_bytes(ByteBuffer::create_uninitialized(palette.impl().internal_buffer().size()).release_value())
{
    __builtin_memcpy(m_bytes.data(), palette.impl().internal_buffer().data<u8>(), palette.impl().internal_buffer().size());
}

NonnullOwnPtr<Gfx::Palette> PaletteData::create_palette() const
{
    auto anonymous_buffer = Core::AnonymousBuffer::create_with_size(m_bytes.size()).release_value();
    __builtin_memcpy(anonymous_buffer.data<u8>(), m_bytes.data(), m_bytes.size());
    return adopt_own(*new Gfx::Palette(*Gfx::PaletteImpl::create_with_anonymous_buffer(move(anonymous_buffer))));
}

}

namespace IPC {

bool encode(Encoder& encoder, Gfx::Painter::LineStyle const& line_style)
{
    VERIFY((unsigned)line_style <= 0xff);
    encoder << (u8)line_style;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::Painter::LineStyle& line_style)
{
    u8 line_style_u8 = 0;
    if (auto result = decoder.decode(line_style_u8); result.is_error())
        return result;
    line_style = (Gfx::Painter::LineStyle)line_style_u8;
    return {};
}

bool encode(Encoder& encoder, Gfx::Painter::DrawOp const& draw_op)
{
    VERIFY((unsigned)draw_op <= 0xff);
    encoder << (u8)draw_op;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::Painter::DrawOp& draw_op)
{
    u8 draw_op_u8 = 0;
    if (auto result = decoder.decode(draw_op_u8); result.is_error())
        return result;
    draw_op = (Gfx::Painter::DrawOp)draw_op_u8;
    return {};
}

bool encode(Encoder& encoder, Gfx::Orientation const& orientation)
{
    VERIFY((unsigned)orientation <= 0xff);
    encoder << (u8)orientation;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::Orientation& orientation)
{
    u8 orientation_u8 = 0;
    if (auto result = decoder.decode(orientation_u8); result.is_error())
        return result;
    orientation = (Gfx::Orientation)orientation_u8;
    return {};
}

bool encode(Encoder& encoder, RemoteGfx::BitmapData const& bitmap_data)
{
    encoder << bitmap_data.physical_rect() << bitmap_data.bytes();
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteGfx::BitmapData& bitmap_data)
{
    if (auto result = decoder.decode(bitmap_data.physical_rect()); result.is_error())
        return result;
    if (auto result = decoder.decode(bitmap_data.bytes()); result.is_error())
        return result;
    return {};
}

bool encode(Encoder& encoder, RemoteGfx::BitmapDiff const& bitmap_diff)
{
    encoder << bitmap_diff.id << (u8)bitmap_diff.flags << bitmap_diff.location << bitmap_diff.size << bitmap_diff.bytes;
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteGfx::BitmapDiff& bitmap_diff)
{
    if (auto result = decoder.decode(bitmap_diff.id); result.is_error())
        return result;
    u8 flags_u8 = 0;
    if (auto result = decoder.decode(flags_u8); result.is_error())
        return result;
    bitmap_diff.flags = (RemoteGfx::BitmapDiff::DiffFlags)flags_u8;
    if (auto result = decoder.decode(bitmap_diff.location); result.is_error())
        return result;
    if (auto result = decoder.decode(bitmap_diff.size); result.is_error())
        return result;
    if (auto result = decoder.decode(bitmap_diff.bytes); result.is_error())
        return result;
    return {};
}

bool encode(Encoder& encoder, RemoteGfx::PaletteData const& palette_data)
{
    encoder << palette_data.bytes();
    return true;
}

ErrorOr<void> decode(Decoder& decoder, RemoteGfx::PaletteData& palette_data)
{
    if (auto result = decoder.decode(palette_data.bytes()); result.is_error())
        return result;
    return {};
}

}
