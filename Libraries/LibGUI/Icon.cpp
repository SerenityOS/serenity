/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/String.h>
#include <LibGUI/Icon.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

Icon::Icon()
    : m_impl(IconImpl::create())
{
}

Icon::Icon(const IconImpl& impl)
    : m_impl(const_cast<IconImpl&>(impl))
{
}

Icon::Icon(const Icon& other)
    : m_impl(other.m_impl)
{
}

Icon::Icon(RefPtr<Gfx::Bitmap>&& bitmap)
    : Icon()
{
    if (bitmap) {
        ASSERT(bitmap->width() == bitmap->height());
        int size = bitmap->width();
        set_bitmap_for_size(size, move(bitmap));
    }
}

Icon::Icon(RefPtr<Gfx::Bitmap>&& bitmap1, RefPtr<Gfx::Bitmap>&& bitmap2)
    : Icon(move(bitmap1))
{
    if (bitmap2) {
        ASSERT(bitmap2->width() == bitmap2->height());
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

Icon Icon::default_icon(const StringView& name)
{
    auto bitmap16 = Gfx::Bitmap::load_from_file(String::format("/res/icons/16x16/%s.png", name.to_string().characters()));
    auto bitmap32 = Gfx::Bitmap::load_from_file(String::format("/res/icons/32x32/%s.png", name.to_string().characters()));
    return Icon(move(bitmap16), move(bitmap32));
}

}
