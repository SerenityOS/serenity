/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGUI/Icon.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

Icon::Icon()
    : m_impl(IconImpl::create())
{
}

Icon::Icon(IconImpl const& impl)
    : m_impl(const_cast<IconImpl&>(impl))
{
}

Icon::Icon(Icon const& other)
    : m_impl(other.m_impl)
{
}

Icon::Icon(RefPtr<Gfx::Bitmap>&& bitmap)
    : Icon()
{
    if (bitmap) {
        VERIFY(bitmap->width() == bitmap->height());
        int size = bitmap->width();
        set_bitmap_for_size(size, move(bitmap));
    }
}

Icon::Icon(RefPtr<Gfx::Bitmap>&& bitmap1, RefPtr<Gfx::Bitmap>&& bitmap2)
    : Icon(move(bitmap1))
{
    if (bitmap2) {
        VERIFY(bitmap2->width() == bitmap2->height());
        int size = bitmap2->width();
        set_bitmap_for_size(size, move(bitmap2));
    }
}

const Gfx::Bitmap* IconImpl::bitmap_for_size(int size) const
{
    auto it = m_bitmaps.find(size);
    if (it != m_bitmaps.end())
        return it->value.ptr();

    int best_diff_so_far = INT32_MAX;
    const Gfx::Bitmap* best_fit = nullptr;
    for (auto& it : m_bitmaps) {
        int abs_diff = abs(it.key - size);
        if (abs_diff < best_diff_so_far) {
            best_diff_so_far = abs_diff;
            best_fit = it.value.ptr();
        }
    }
    return best_fit;
}

void IconImpl::set_bitmap_for_size(int size, RefPtr<Gfx::Bitmap>&& bitmap)
{
    if (!bitmap) {
        m_bitmaps.remove(size);
        return;
    }
    m_bitmaps.set(size, move(bitmap));
}

Icon Icon::default_icon(StringView const& name)
{
    auto bitmap16 = Gfx::Bitmap::try_load_from_file(String::formatted("/res/icons/16x16/{}.png", name));
    auto bitmap32 = Gfx::Bitmap::try_load_from_file(String::formatted("/res/icons/32x32/{}.png", name));
    return Icon(move(bitmap16), move(bitmap32));
}

}
