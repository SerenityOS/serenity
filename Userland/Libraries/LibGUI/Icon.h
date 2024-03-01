/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Julius Heijmen <julius.heijmen@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
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
    ~IconImpl() = default;

    Gfx::Bitmap const* bitmap_for_size(int) const;
    void set_bitmap_for_size(int, RefPtr<Gfx::Bitmap const>&&);

    Vector<int> sizes() const { return m_bitmaps.keys(); }

private:
    IconImpl() = default;
    HashMap<int, RefPtr<Gfx::Bitmap const>> m_bitmaps;
};

class Icon {
public:
    Icon();
    explicit Icon(RefPtr<Gfx::Bitmap const>&&);
    explicit Icon(RefPtr<Gfx::Bitmap const>&&, RefPtr<Gfx::Bitmap const>&&);
    explicit Icon(IconImpl const&);
    Icon(Icon const&);
    ~Icon() = default;

    static Icon default_icon(StringView);
    static ErrorOr<Icon> try_create_default_icon(StringView);

    Icon& operator=(Icon const& other)
    {
        if (this != &other)
            m_impl = other.m_impl;
        return *this;
    }

    Gfx::Bitmap const* bitmap_for_size(int size) const { return m_impl->bitmap_for_size(size); }
    void set_bitmap_for_size(int size, RefPtr<Gfx::Bitmap const>&& bitmap) { m_impl->set_bitmap_for_size(size, move(bitmap)); }

    IconImpl const& impl() const { return *m_impl; }

    Vector<int> sizes() const { return m_impl->sizes(); }

private:
    NonnullRefPtr<IconImpl> m_impl;
};

}
