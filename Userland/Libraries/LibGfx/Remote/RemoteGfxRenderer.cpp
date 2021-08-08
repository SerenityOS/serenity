/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCrypto/Hash/SHA1.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Font.h>
#include <LibGfx/Remote/RemoteGfxRenderer.h>

namespace RemoteGfx {

RemoteGfxRenderer::BitmapData::BitmapData(i32 id, RemoteGfxRenderer& renderer, Gfx::BitmapFormat format, Gfx::IntSize const& size, int scale)
    : renderer(&renderer)
    , id(id)
    , bitmap(Gfx::Bitmap::try_create(format, size, scale).release_value())
    , dirty_rects(bitmap->rect())
    , painter(*bitmap)
{
    painter_state.clip_rect = bitmap->rect();
}

void RemoteGfxRenderer::BitmapData::update_painter()
{
    painter = Gfx::Painter(*bitmap);
    painter.set_state(painter_state.clip_rect, painter_state.translation, painter_state.draw_op);
};

void RemoteGfxRenderer::BitmapData::do_copy_on_write()
{
    VERIFY(copy_on_write);
    copy_on_write = false;
    VERIFY(bitmap_synced);

    if (bitmap == bitmap_synced) {
        bitmap = Gfx::Bitmap::try_create(bitmap_synced->format(), bitmap_synced->size(), bitmap_synced->scale()).release_value();
        Gfx::Painter bitmap_painter(*bitmap);
        bitmap_painter.blit({}, *bitmap_synced, bitmap_synced->rect(), 1.0f, false);
    } else {
        {
            // TODO: swap() can't handle NonnullRefPtr
            auto original_bitmap = move(bitmap);
            bitmap = bitmap_synced.release_nonnull();
            bitmap_synced = *original_bitmap;
        }
        Gfx::Painter bitmap_painter(*bitmap);
        for (auto& rect : dirty_rects.rects())
            bitmap_painter.blit(rect.location(), *bitmap_synced, rect, 1.0f, false);
    }

    update_painter();
    dirty_rects.clear_with_capacity();
}

void RemoteGfxRenderer::BitmapData::dirty_painter_rect(Gfx::IntRect const& rect)
{
    auto dirty_rect = painter.clipped_and_translated(rect);
    dirty_rects.add(dirty_rect);
    renderer->m_callbacks.bitmap_updated(renderer->m_client_id, id, &dirty_rect);
}

RemoteGfxRenderer::OneBitBitmapData::OneBitBitmapData(i32 id, RemoteGfxRenderer& renderer, Gfx::OneBitBitmap::Type type, Gfx::IntSize size, ByteBuffer const& bitmap_data)
    : renderer(&renderer)
    , id(id)
{
    BitmapView bitmap(const_cast<u8*>(bitmap_data.data()), size.width() * size.height());
    switch (type) {
    case Gfx::OneBitBitmap::Type::CharacterBitmap:
        character_bitmap = Gfx::CharacterBitmap::create_from_bitmap(size, bitmap);
        break;
    case Gfx::OneBitBitmap::Type::GlyphBitmap:
        glyph_bitmap = adopt_own(*new Gfx::GlyphBitmap(size, bitmap));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void RemoteGfxRenderer::OneBitBitmapData::set_bits(ByteBuffer const& bitmap_data)
{
    if (character_bitmap)
        character_bitmap->set_bits(bitmap_data);
    else if (glyph_bitmap)
        glyph_bitmap->set_bits(bitmap_data);
    else
        VERIFY_NOT_REACHED();
}

RemoteGfxRenderer::RemoteGfxRenderer(RemoteGfxRendererCallbacks& callbacks, RemoteGfxFontDatabase& font_database, u32 client_id)
    : RemoteGfxClientProxy<RemoteGfxServerEndpoint, RemoteGfxClientEndpoint, RemoteGfxRenderer>(*this, {})
    , m_callbacks(callbacks)
    , m_font_database(font_database)
    , m_client_id(client_id)
{
}

RemoteGfxRenderer::~RemoteGfxRenderer()
{
}

void RemoteGfxRenderer::create_bitmap(i32 id, Gfx::BitmapFormat const& format, Gfx::IntSize const& size, int scale)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_bitmap {} -> total bitmaps: {}", m_client_id, id, m_bitmaps.size() + 1);
    auto result = m_bitmaps.set(id, BitmapData(id, *this, format, size, scale));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
    m_callbacks.bitmap_updated(m_client_id, id, nullptr);
}

void RemoteGfxRenderer::destroy_bitmap(i32 id)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::destroy_bitmap {} -> remaining total bitmaps: {}", m_client_id, id, m_bitmaps.size() - 1);
    bool removed = m_bitmaps.remove(id);
    VERIFY(removed);
    m_callbacks.bitmap_updated(m_client_id, id, nullptr);
}

void RemoteGfxRenderer::sync_bitmap(i32 id, u32 tag)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::sync_bitmap {} tag {}", m_client_id, id, tag);
    auto& bitmap_data = this->bitmap_data(id);
    bitmap_data.sync_tag = tag;
    bitmap_data.bitmap_synced = bitmap_data.bitmap;
    bitmap_data.copy_on_write = true;
    m_callbacks.bitmap_was_synced(m_client_id, id, *bitmap_data.bitmap_synced, bitmap_data.dirty_rects);
}

void RemoteGfxRenderer::set_bitmap_data(i32 id, RemoteGfx::BitmapData const& data)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::set_bitmap_data {} bitmap_data physical_rect: {} bytes: {}", m_client_id, id, data.physical_rect(), data.bytes().size());
    auto& bitmap_data = bitmap_data_for_write(id);
    data.apply_to(*bitmap_data.bitmap);
    auto scale = bitmap_data.bitmap->scale();
    // TODO: we shouldn't call bitmap_data.dirty_painter_rect() here as we don't want to apply clipping or translation!
    bitmap_data.dirty_painter_rect({ data.physical_rect().left() / scale, data.physical_rect().top() / scale, data.physical_rect().width() / scale, data.physical_rect().height() / scale });
}

void RemoteGfxRenderer::apply_bitmap_diff(i32 id, BitmapDiff const& diff)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::apply_bitmap_diff {} rect: {} bytes: {}", m_client_id, id, Gfx::IntRect { diff.location, diff.size }, diff.bytes.size());
    auto& bitmap_data = bitmap_data_for_write(id);
    // We shouldn't call bitmap_data.dirty_painter_rect() here as we don't want to apply clipping or translation!
    Gfx::DisjointRectSet applied_rects;
    diff.apply_to_bitmap(bitmap_data.bitmap, &applied_rects);
    bitmap_data.dirty_rects.add(applied_rects);
    for (auto& applied_rect : applied_rects.rects())
        m_callbacks.bitmap_updated(m_client_id, id, &applied_rect);
}

void RemoteGfxRenderer::create_bitmap_font_from_data(i32 id, ByteBuffer const& data)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_bitmap_font_from_data {} bytes: {}", m_client_id, id, data.size());
    auto font_data = m_font_database.add_font(Gfx::Font::Type::Bitmap, data);
    auto font = font_data->bitmap_font();
    auto result = m_fonts.set(id, adopt_own(*new FontData(move(font_data), move(font))));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void RemoteGfxRenderer::create_scalable_font_from_data(i32 id, ByteBuffer const& data, u32 font_size)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_scalable_font_from_data {} bytes: {} font_size: {}", m_client_id, id, data.size(), font_size);
    auto font_data = m_font_database.add_font(Gfx::Font::Type::Scaled, data);
    auto font = font_data->scaled_font(font_size);
    auto result = m_fonts.set(id, adopt_own(*new FontData(move(font_data), move(font))));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void RemoteGfxRenderer::create_bitmap_font_from_digest(i32 id, ByteBuffer const& digest_bytes)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_bitmap_font_from_digest {} digest size: {}", m_client_id, id, digest_bytes.size());
    VERIFY(digest_bytes.size() == RemoteGfxFontDatabase::FontDigestType::Size);
    RemoteGfxFontDatabase::FontDigestType digest;
    __builtin_memcpy(digest.data, digest_bytes.data(), RemoteGfxFontDatabase::FontDigestType::Size);
    auto font_data = m_font_database.find_font(digest);
    VERIFY(font_data);
    auto font = font_data->bitmap_font();
    auto result = m_fonts.set(id, adopt_own(*new FontData(font_data.release_nonnull(), move(font))));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void RemoteGfxRenderer::create_scalable_font_from_digest(i32 id, ByteBuffer const& digest_bytes, u32 font_size)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_scalable_font_from_digest {} digest size: {} font_size: {}", m_client_id, id, digest_bytes.size(), font_size);
    VERIFY(digest_bytes.size() == RemoteGfxFontDatabase::FontDigestType::Size);
    RemoteGfxFontDatabase::FontDigestType digest;
    __builtin_memcpy(digest.data, digest_bytes.data(), RemoteGfxFontDatabase::FontDigestType::Size);
    auto font_data = m_font_database.find_font(digest);
    VERIFY(font_data);
    auto font = font_data->scaled_font(font_size);
    auto result = m_fonts.set(id, adopt_own(*new FontData(font_data.release_nonnull(), move(font))));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void RemoteGfxRenderer::create_onebit_bitmap(i32 onebit_bitmap_id, Gfx::IntSize const& size, Gfx::OneBitBitmap::Type const& type, ByteBuffer const& bitmap_data)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_onebit_bitmap {} size: {} type: {} data_size: {}", m_client_id, onebit_bitmap_id, size, (int)type, bitmap_data.size());
    VERIFY(type != Gfx::OneBitBitmap::Type::Empty);
    auto result = m_onebit_bitmaps.set(onebit_bitmap_id, OneBitBitmapData(onebit_bitmap_id, *this, type, size, bitmap_data));
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void RemoteGfxRenderer::destroy_onebit_bitmap(i32 onebit_bitmap_id)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::destroy_onebit_bitmap {}", m_client_id, onebit_bitmap_id);
    bool removed = m_onebit_bitmaps.remove(onebit_bitmap_id);
    VERIFY(removed);
}

void RemoteGfxRenderer::set_onebit_bitmap_data(i32 onebit_bitmap_id, ByteBuffer const& bitmap_data)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::set_onebit_bitmap_data {} data_size: {}", m_client_id, onebit_bitmap_id, bitmap_data.size());
    auto& onebit_bitmap_data = this->onebit_bitmap_data(onebit_bitmap_id);
    onebit_bitmap_data.set_bits(bitmap_data);
}

void RemoteGfxRenderer::create_palette(i32 palette_id, RemoteGfx::PaletteData const& palette)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::create_palette {}", m_client_id, palette_id);
    auto result = m_palettes.set(palette_id, palette.create_palette());
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
}

void RemoteGfxRenderer::destroy_palette(i32 palette_id)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::destroy_palette {}", m_client_id, palette_id);
    bool removed = m_palettes.remove(palette_id);
    VERIFY(removed);
}

void RemoteGfxRenderer::set_painter_state(i32 bitmap_id, Gfx::IntRect const& clip_rect, Gfx::IntPoint const& translation, Gfx::Painter::DrawOp const& draw_op)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::set_painter_state bitmap_id {} clip_rect {} translation {} draw_op {}", m_client_id, bitmap_id, clip_rect, translation, (int)draw_op);
    auto& bitmap_data = this->bitmap_data(bitmap_id);
    bitmap_data.painter_state.clip_rect = clip_rect;
    bitmap_data.painter_state.translation = translation;
    bitmap_data.painter_state.draw_op = draw_op;
    bitmap_data.painter.set_state(clip_rect, translation, draw_op);
}

void RemoteGfxRenderer::clear_rect(i32 bitmap_id, Gfx::IntRect const& rect, Gfx::Color const& color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::clear_rect bitmap_id: {} rect: {} color: {}", m_client_id, bitmap_id, rect, color);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.clear_rect(rect, color);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::fill_rect(i32 bitmap_id, Gfx::IntRect const& rect, Gfx::Color const& color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::fill_rect bitmap_id: {} rect: {} color: {}", m_client_id, bitmap_id, rect, color);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.fill_rect(rect, color);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::draw_line(i32 bitmap_id, Gfx::IntPoint const& point1, Gfx::IntPoint const& point2, Gfx::Color const& color, int thickness, Gfx::Painter::LineStyle const& line_style, Optional<Gfx::Color> const& alternate_color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::draw_line bitmap_id: {} point1: {} point2: {} color: {} thickness: {} line_style: {}", m_client_id, bitmap_id, point1, point2, color, thickness, (int)line_style);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.draw_line(point1, point2, color, thickness, line_style, alternate_color.has_value() ? alternate_color.value() : Gfx::Color::Transparent);
    auto dirty_rect = Gfx::IntRect::from_two_points(point1, point2).inflated(2, 2);
    bitmap_data.dirty_painter_rect(dirty_rect);
}

void RemoteGfxRenderer::fill_rect_with_dither_pattern(i32 bitmap_id, Gfx::IntRect const& rect, Gfx::Color const& color1, Gfx::Color const& color2)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::fill_rect_with_dither_pattern bitmap_id: {} rect: {} color1: {} color2: {}", m_client_id, bitmap_id, rect, color1, color2);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.fill_rect_with_dither_pattern(rect, color1, color2);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::fill_rect_with_checkerboard(i32 bitmap_id, Gfx::IntRect const& rect, Gfx::IntSize const& cell_size, Gfx::Color const& color_dark, Gfx::Color const& color_light)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::fill_rect_with_checkerboard bitmap_id: {} rect: {} cell_size: {} colordark: {} color_light: {}", m_client_id, bitmap_id, rect, cell_size, color_dark, color_light);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.fill_rect_with_checkerboard(rect, cell_size, color_dark, color_light);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::fill_rect_with_gradient(i32 bitmap_id, Gfx::Orientation const& orientation, Gfx::IntRect const& rect, Gfx::Color const& gradient_start, Gfx::Color const& gradient_end)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::fill_rect_with_gradient bitmap_id: {} orientation: {} rect: {} gradient_start: {} gradient_end: {}", m_client_id, bitmap_id, orientation == Gfx::Orientation::Horizontal ? "horizontal" : "vertical", rect, gradient_start, gradient_end);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.fill_rect_with_gradient(orientation, rect, gradient_start, gradient_end);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::blit_opaque(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& src_rect, bool apply_alpha)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_opaque bitmap_id: {} position: {} from_bitmap_id: {} src_rect: {} apply_alpha: {}", m_client_id, bitmap_id, position, from_bitmap_id, src_rect, apply_alpha);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    bitmap_data.painter.blit(position, *from_bitmap, src_rect, 1.0f, apply_alpha);
    bitmap_data.dirty_painter_rect({ position, src_rect.size() });
}

void RemoteGfxRenderer::blit_with_opacity(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& src_rect, float opacity, bool apply_alpha)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_with_opacity bitmap_id: {} position: {} from_bitmap_id: {} src_rect: {} opacity: {} apply_alpha: {}", m_client_id, bitmap_id, position, from_bitmap_id, src_rect, opacity, apply_alpha);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    bitmap_data.painter.blit(position, *from_bitmap, src_rect, opacity, apply_alpha);
    bitmap_data.dirty_painter_rect({ position, src_rect.size() });
}

void RemoteGfxRenderer::blit_dimmed(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& src_rect)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_dimmed bitmap_id: {} position: {} from_bitmap_id: {} src_rect: {}", m_client_id, bitmap_id, position, from_bitmap_id, src_rect);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    bitmap_data.painter.blit_dimmed(position, *from_bitmap, src_rect);
    bitmap_data.dirty_painter_rect({ position, src_rect.size() });
}

void RemoteGfxRenderer::blit_brightened(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& src_rect)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_brightened bitmap_id: {} position: {} from_bitmap_id: {} src_rect: {}", m_client_id, bitmap_id, position, from_bitmap_id, src_rect);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    bitmap_data.painter.blit_brightened(position, *from_bitmap, src_rect);
    bitmap_data.dirty_painter_rect({ position, src_rect.size() });
}

void RemoteGfxRenderer::blit_blended(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& src_rect, Gfx::Color const& color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_blended bitmap_id: {} position: {} from_bitmap_id: {} src_rect: {} color: {}", m_client_id, bitmap_id, position, from_bitmap_id, src_rect, color);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    bitmap_data.painter.blit_blended(position, *from_bitmap, src_rect, color);
    bitmap_data.dirty_painter_rect({ position, src_rect.size() });
}

void RemoteGfxRenderer::blit_multiplied(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& src_rect, Gfx::Color const& color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_multiplied bitmap_id: {} position: {} from_bitmap_id: {} src_rect: {} color: {}", m_client_id, bitmap_id, position, from_bitmap_id, src_rect, color);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    bitmap_data.painter.blit_multiplied(position, *from_bitmap, src_rect, color);
    bitmap_data.dirty_painter_rect({ position, src_rect.size() });
}

void RemoteGfxRenderer::blit_disabled(i32 bitmap_id, Gfx::IntPoint const& position, i32 from_bitmap_id, Gfx::IntRect const& rect, i32 palette_id)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::blit_disabled bitmap_id: {} position: {} from_bitmap_id: {} rect: {} palette_id: {}", m_client_id, bitmap_id, position, from_bitmap_id, rect, palette_id);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto* from_bitmap = this->find_bitmap(from_bitmap_id);
    VERIFY(from_bitmap);
    auto& palette = this->palette(palette_id);
    bitmap_data.painter.blit_disabled(position, *from_bitmap, rect, palette);
    bitmap_data.dirty_painter_rect({ position, rect.size() });
}

void RemoteGfxRenderer::draw_rect(i32 bitmap_id, Gfx::IntRect const& rect, Gfx::Color const& color, bool rough)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::draw_rect bitmap_id: {} rect: {} color: {} rough: {}", m_client_id, bitmap_id, rect, color, rough);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    bitmap_data.painter.draw_rect(rect, color, rough);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::draw_text(i32 bitmap_id, Gfx::IntRect const& rect, String const& raw_text, i32 font_id, Gfx::TextAlignment const& text_alignment, Gfx::Color const& color, Gfx::TextElision const& elision, Gfx::TextWrapping const& text_wrapping)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::draw_text bitmap_id: {} rect: {} raw_text: {} font_id: {} text_alignment: {} color: {} elision: {} text_wrapping: {}", m_client_id, bitmap_id, rect, raw_text, font_id, (int)text_alignment, color, (int)elision, (int)text_wrapping);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto& font = this->font(font_id);
    bitmap_data.painter.draw_text(rect, raw_text, font, text_alignment, color, elision, text_wrapping);
    bitmap_data.dirty_painter_rect(rect);
}

void RemoteGfxRenderer::draw_glyph(i32 bitmap_id, Gfx::IntRect const& rect, u32 code_point, i32 font_id, Gfx::Color const& color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::draw_glyph bitmap_id: {} rect: {} code_point: {} font_id: {} color: {}", m_client_id, bitmap_id, rect, code_point, font_id, color);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto& font = this->font(font_id);
    bitmap_data.painter.draw_glyph(rect.location(), code_point, font, color);
    bitmap_data.dirty_painter_rect(rect); // TODO: We could save 8 bytes per message if we only passed the location and calculated the rect here
}

void RemoteGfxRenderer::draw_bitmap(i32 bitmap_id, Gfx::IntPoint const& position, i32 onebit_bitmap_id, Gfx::Color const& color)
{
    dbgln_if(REMOTE_GFX_RENDERER_DEBUG, "RemoteGfx::RemoteGfxClient[{}]::draw_bitmap bitmap_id: {} position: {} onebit_bitmap_id: {} color: {}", m_client_id, bitmap_id, position, onebit_bitmap_id, color);
    auto& bitmap_data = bitmap_data_for_write(bitmap_id);
    auto& onebit_bitmap_data = this->onebit_bitmap_data(onebit_bitmap_id);
    Gfx::IntSize bitmap_size;
    if (onebit_bitmap_data.character_bitmap) {
        bitmap_data.painter.draw_bitmap(position, *onebit_bitmap_data.character_bitmap, color);
        bitmap_size = onebit_bitmap_data.character_bitmap->size();
    } else if (onebit_bitmap_data.glyph_bitmap) {
        bitmap_data.painter.draw_bitmap(position, *onebit_bitmap_data.glyph_bitmap, color);
        bitmap_size = onebit_bitmap_data.glyph_bitmap->size();
    } else {
        VERIFY_NOT_REACHED();
    }
    bitmap_data.dirty_painter_rect({ position, bitmap_size });
}

}
