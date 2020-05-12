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
#include <LibGfx/Bitmap.h>

namespace PaintBrush {

class Layer : public RefCounted<Layer> {
    AK_MAKE_NONCOPYABLE(Layer);
    AK_MAKE_NONMOVABLE(Layer);

public:
    static RefPtr<Layer> create_with_size(const Gfx::Size&, const String& name);

    ~Layer() {}

    const Gfx::Point& location() const { return m_location; }
    void set_location(const Gfx::Point& location) { m_location = location; }

    const Gfx::Bitmap& bitmap() const { return *m_bitmap; }
    Gfx::Bitmap& bitmap() { return *m_bitmap; }
    Gfx::Size size() const { return bitmap().size(); }

    Gfx::Rect relative_rect() const { return { location(), size() }; }
    Gfx::Rect rect() const { return { {}, size() }; }

    const String& name() const { return m_name; }
    void set_name(const String& name) { m_name = name; }

private:
    explicit Layer(const Gfx::Size&, const String& name);

    String m_name;
    Gfx::Point m_location;
    RefPtr<Gfx::Bitmap> m_bitmap;
};

}
