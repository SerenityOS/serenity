/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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
