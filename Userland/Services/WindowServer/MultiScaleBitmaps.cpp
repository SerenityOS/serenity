/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MultiScaleBitmaps.h"
#include "Screen.h"

namespace WindowServer {

Gfx::Bitmap const& MultiScaleBitmaps::bitmap(int scale_factor) const
{
    auto it = m_bitmaps.find(scale_factor);
    if (it == m_bitmaps.end()) {
        it = m_bitmaps.find(1);
        if (it == m_bitmaps.end())
            it = m_bitmaps.begin();
    }
    // We better found *something*
    if (it == m_bitmaps.end()) {
        dbgln("Could not find any bitmap in this MultiScaleBitmaps");
        VERIFY_NOT_REACHED();
    }
    return it->value;
}

Gfx::Bitmap const* MultiScaleBitmaps::find_bitmap(int scale_factor) const
{
    auto it = m_bitmaps.find(scale_factor);
    return it != m_bitmaps.end() ? it->value.ptr() : nullptr;
}

RefPtr<MultiScaleBitmaps> MultiScaleBitmaps::create_empty()
{
    return adopt_ref(*new MultiScaleBitmaps());
}

RefPtr<MultiScaleBitmaps> MultiScaleBitmaps::create(StringView filename, StringView default_filename)
{
    auto per_scale_bitmap = adopt_ref(*new MultiScaleBitmaps());
    if (per_scale_bitmap->load(filename, default_filename))
        return per_scale_bitmap;
    return {};
}

bool MultiScaleBitmaps::load(StringView filename, StringView default_filename)
{
    Optional<Gfx::BitmapFormat> bitmap_format;
    bool did_load_any = false;

    // If we're reloading the bitmaps get rid of the old ones.
    m_bitmaps.clear();
    m_format = Gfx::BitmapFormat::Invalid;

    auto add_bitmap = [&](StringView path, int scale_factor) {
        auto bitmap_or_error = Gfx::Bitmap::load_from_file(path, scale_factor);
        if (bitmap_or_error.is_error())
            return;
        auto bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        auto bitmap_format = bitmap->format();
        if (m_format == Gfx::BitmapFormat::Invalid || m_format == bitmap_format) {
            if (m_format == Gfx::BitmapFormat::Invalid)
                m_format = bitmap_format;

            did_load_any = true;
            m_bitmaps.set(scale_factor, move(bitmap));
        } else {
            // Gracefully ignore, we have at least one bitmap already
            dbgln("Bitmap {} (scale {}) has format inconsistent with the other per-scale bitmaps", path, bitmap->scale());
        }
    };

    Screen::for_each_scale_factor_in_use([&](int scale_factor) {
        add_bitmap(filename, scale_factor);
        return IterationDecision::Continue;
    });
    if (!did_load_any && !default_filename.is_null() && !default_filename.is_empty()) {
        Screen::for_each_scale_factor_in_use([&](int scale_factor) {
            add_bitmap(default_filename, scale_factor);
            return IterationDecision::Continue;
        });
    }
    return did_load_any;
}

void MultiScaleBitmaps::add_bitmap(int scale_factor, NonnullRefPtr<Gfx::Bitmap>&& bitmap)
{
    auto bitmap_format = bitmap->format();
    if (m_format == Gfx::BitmapFormat::Invalid || m_format == bitmap_format) {
        if (m_format == Gfx::BitmapFormat::Invalid)
            m_format = bitmap_format;
        m_bitmaps.set(scale_factor, move(bitmap));
    } else {
        dbgln("MultiScaleBitmaps::add_bitmap (scale {}) has format inconsistent with the other per-scale bitmaps", bitmap->scale());
        VERIFY_NOT_REACHED(); // The caller of this function should have made sure it is consistent!
    }
}

}
