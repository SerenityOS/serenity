/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibGfx/Bitmap.h>

namespace GUI {

class IconImpl : public RefCounted<IconImpl> {
public:
    static NonnullRefPtr<IconImpl> create() { return adopt_ref(*new IconImpl); }
    ~IconImpl() { }

    const Gfx::Bitmap* bitmap_for_size(int) const;
    void set_bitmap_for_size(int, RefPtr<Gfx::Bitmap>&&);

    Vector<int> sizes() const
    {
        Vector<int> sizes;
        for (auto& it : m_bitmaps)
            sizes.append(it.key);
        return sizes;
    }

private:
    IconImpl() { }
    HashMap<int, RefPtr<Gfx::Bitmap>> m_bitmaps;
};

class Icon {
public:
    Icon();
    explicit Icon(RefPtr<Gfx::Bitmap>&&);
    explicit Icon(RefPtr<Gfx::Bitmap>&&, RefPtr<Gfx::Bitmap>&&);
    explicit Icon(const IconImpl&);
    Icon(const Icon&);
    ~Icon() { }

    static Icon default_icon(StringView);

    Icon& operator=(const Icon& other)
    {
        if (this != &other)
            m_impl = other.m_impl;
        return *this;
    }

    const Gfx::Bitmap* bitmap_for_size(int size) const { return m_impl->bitmap_for_size(size); }
    void set_bitmap_for_size(int size, RefPtr<Gfx::Bitmap>&& bitmap) { m_impl->set_bitmap_for_size(size, move(bitmap)); }

    const IconImpl& impl() const { return *m_impl; }

    Vector<int> sizes() const { return m_impl->sizes(); }

private:
    NonnullRefPtr<IconImpl> m_impl;
};

}
