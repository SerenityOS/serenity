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

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibGfx/Forward.h>

class GIconImpl : public RefCounted<GIconImpl> {
public:
    static NonnullRefPtr<GIconImpl> create() { return adopt(*new GIconImpl); }
    ~GIconImpl() {}

    const Gfx::Bitmap* bitmap_for_size(int) const;
    void set_bitmap_for_size(int, RefPtr<Gfx::Bitmap>&&);

private:
    GIconImpl() {}
    HashMap<int, RefPtr<Gfx::Bitmap>> m_bitmaps;
};

class GIcon {
public:
    GIcon();
    explicit GIcon(RefPtr<Gfx::Bitmap>&&);
    explicit GIcon(RefPtr<Gfx::Bitmap>&&, RefPtr<Gfx::Bitmap>&&);
    explicit GIcon(const GIconImpl&);
    GIcon(const GIcon&);
    ~GIcon() {}

    static GIcon default_icon(const StringView&);

    GIcon& operator=(const GIcon& other)
    {
        if (this != &other)
            m_impl = other.m_impl;
        return *this;
    }

    const Gfx::Bitmap* bitmap_for_size(int size) const { return m_impl->bitmap_for_size(size); }
    void set_bitmap_for_size(int size, RefPtr<Gfx::Bitmap>&& bitmap) { m_impl->set_bitmap_for_size(size, move(bitmap)); }

    const GIconImpl& impl() const { return *m_impl; }

private:
    NonnullRefPtr<GIconImpl> m_impl;
};
