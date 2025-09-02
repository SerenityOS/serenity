/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
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

Icon::Icon(RefPtr<Gfx::Bitmap const>&& bitmap)
    : Icon()
{
    if (bitmap) {
        VERIFY(bitmap->width() == bitmap->height());
        int size = bitmap->width();
        set_bitmap_for_size(size, move(bitmap));
    }
}

Icon::Icon(RefPtr<Gfx::Bitmap const>&& bitmap1, RefPtr<Gfx::Bitmap const>&& bitmap2)
    : Icon(move(bitmap1))
{
    if (bitmap2) {
        VERIFY(bitmap2->width() == bitmap2->height());
        int size = bitmap2->width();
        set_bitmap_for_size(size, move(bitmap2));
    }
}

Gfx::Bitmap const* IconImpl::bitmap_for_size(int size) const
{
    auto it = m_bitmaps.find(size);
    if (it != m_bitmaps.end())
        return it->value.ptr();

    Gfx::Bitmap const* smallest_fit = nullptr;
    int smallest_size = INT32_MAX;
    Gfx::Bitmap const* largest_fit = nullptr;
    int largest_size = 0;
    for (auto const& it : m_bitmaps) {
        if (it.key < smallest_size && it.key >= size) {
            smallest_fit = it.value.ptr();
            smallest_size = it.key;
        }
        if (it.key > largest_size) {
            largest_fit = it.value.ptr();
            largest_size = it.key;
        }
    }
    if (smallest_fit)
        return smallest_fit;
    return largest_fit;
}

void IconImpl::set_bitmap_for_size(int size, RefPtr<Gfx::Bitmap const>&& bitmap)
{
    if (!bitmap) {
        m_bitmaps.remove(size);
        return;
    }
    m_bitmaps.set(size, move(bitmap));
}

Icon Icon::default_icon(StringView name)
{
    return MUST(try_create_default_icon(name));
}

ErrorOr<Icon> Icon::try_create_default_icon(StringView name)
{
    RefPtr<Gfx::Bitmap> bitmap16;
    RefPtr<Gfx::Bitmap> bitmap32;
    if (auto bitmap_or_error = Gfx::Bitmap::load_from_file(ByteString::formatted("/res/icons/16x16/{}.png", name)); !bitmap_or_error.is_error())
        bitmap16 = bitmap_or_error.release_value();
    if (auto bitmap_or_error = Gfx::Bitmap::load_from_file(ByteString::formatted("/res/icons/32x32/{}.png", name)); !bitmap_or_error.is_error())
        bitmap32 = bitmap_or_error.release_value();

    if (!bitmap16 && !bitmap32) {
        dbgln("Default icon not found: {}", name);
        return Error::from_string_literal("Default icon not found");
    }

    return Icon(move(bitmap16), move(bitmap32));
}

}
