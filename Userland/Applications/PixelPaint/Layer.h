/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Weakable.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

class Image;
class Selection;

class Layer
    : public RefCounted<Layer>
    , public Weakable<Layer> {

    AK_MAKE_NONCOPYABLE(Layer);
    AK_MAKE_NONMOVABLE(Layer);

public:
    static ErrorOr<NonnullRefPtr<Layer>> try_create_with_size(Image&, Gfx::IntSize const&, String name);
    static ErrorOr<NonnullRefPtr<Layer>> try_create_with_bitmap(Image&, NonnullRefPtr<Gfx::Bitmap>, String name);
    static ErrorOr<NonnullRefPtr<Layer>> try_create_snapshot(Image&, Layer const&);

    ~Layer() { }

    Gfx::IntPoint const& location() const { return m_location; }
    void set_location(Gfx::IntPoint const& location) { m_location = location; }

    Gfx::Bitmap const& bitmap() const { return *m_bitmap; }
    Gfx::Bitmap& bitmap() { return *m_bitmap; }
    Gfx::IntSize size() const { return bitmap().size(); }

    Gfx::IntRect relative_rect() const { return { location(), size() }; }
    Gfx::IntRect rect() const { return { {}, size() }; }

    String const& name() const { return m_name; }
    void set_name(String);

    void set_bitmap(NonnullRefPtr<Gfx::Bitmap> bitmap) { m_bitmap = move(bitmap); }

    void did_modify_bitmap(Gfx::IntRect const& = {});

    void set_selected(bool selected) { m_selected = selected; }
    bool is_selected() const { return m_selected; }

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible);

    int opacity_percent() const { return m_opacity_percent; }
    void set_opacity_percent(int);

    RefPtr<Gfx::Bitmap> try_copy_bitmap(Selection const&) const;

    Image const& image() const { return m_image; }

    void erase_selection(Selection const&);

private:
    Layer(Image&, NonnullRefPtr<Gfx::Bitmap>, String name);

    Image& m_image;

    String m_name;
    Gfx::IntPoint m_location;
    NonnullRefPtr<Gfx::Bitmap> m_bitmap;

    bool m_selected { false };
    bool m_visible { true };

    int m_opacity_percent { 100 };
};

}
