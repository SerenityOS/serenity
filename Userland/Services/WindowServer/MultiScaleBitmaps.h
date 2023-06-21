/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibGfx/Bitmap.h>

namespace WindowServer {

class MultiScaleBitmaps : public RefCounted<MultiScaleBitmaps> {
public:
    static RefPtr<MultiScaleBitmaps> create_empty();
    static RefPtr<MultiScaleBitmaps> create(StringView filename, StringView default_filename = {});

    Gfx::Bitmap const& default_bitmap() const { return bitmap(1); }
    Gfx::Bitmap const& bitmap(int scale_factor) const;
    Gfx::Bitmap const* find_bitmap(int scale_factor) const;
    Gfx::BitmapFormat format() const { return m_format; }
    bool load(StringView filename, StringView default_filename = {});
    void add_bitmap(int scale_factor, NonnullRefPtr<Gfx::Bitmap>&&);
    bool is_empty() const { return m_bitmaps.is_empty(); }

private:
    MultiScaleBitmaps() = default;

    HashMap<int, NonnullRefPtr<Gfx::Bitmap>> m_bitmaps;
    Gfx::BitmapFormat m_format { Gfx::BitmapFormat::Invalid };
};

}
