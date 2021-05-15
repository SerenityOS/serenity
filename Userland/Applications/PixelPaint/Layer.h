/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

class Layer
    : public RefCounted<Layer>
    , public Weakable<Layer> {

    AK_MAKE_NONCOPYABLE(Layer);
    AK_MAKE_NONMOVABLE(Layer);

public:
    static RefPtr<Layer> create_with_size(Image&, const Gfx::IntSize&, const String& name);
    static RefPtr<Layer> create_with_bitmap(Image&, const Gfx::Bitmap&, const String& name);
    static RefPtr<Layer> create_snapshot(Image&, const Layer&);

    ~Layer() { }

    const Gfx::IntPoint& location() const { return m_location; }
    void set_location(const Gfx::IntPoint& location) { m_location = location; }

    const Gfx::Bitmap& bitmap() const { return *m_bitmap; }
    Gfx::Bitmap& bitmap() { return *m_bitmap; }
    Gfx::IntSize size() const { return bitmap().size(); }

    Gfx::IntRect relative_rect() const { return { location(), size() }; }
    Gfx::IntRect rect() const { return { {}, size() }; }

    const String& name() const { return m_name; }
    void set_name(const String&);

    void set_bitmap(Gfx::Bitmap& bitmap) { m_bitmap = bitmap; }

    void did_modify_bitmap(Image&);

    void set_selected(bool selected) { m_selected = selected; }
    bool is_selected() const { return m_selected; }

    bool is_visible() const { return m_visible; }
    void set_visible(bool visible);

    int opacity_percent() const { return m_opacity_percent; }
    void set_opacity_percent(int);

private:
    Layer(Image&, const Gfx::IntSize&, const String& name);
    Layer(Image&, const Gfx::Bitmap&, const String& name);

    Image& m_image;

    String m_name;
    Gfx::IntPoint m_location;
    RefPtr<Gfx::Bitmap> m_bitmap;

    bool m_selected { false };
    bool m_visible { true };

    int m_opacity_percent { 100 };
};

}
