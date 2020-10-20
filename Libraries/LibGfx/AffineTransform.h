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

#include <AK/Forward.h>
#include <AK/LogStream.h>
#include <LibGfx/Forward.h>

namespace Gfx {

class AffineTransform {
public:
    AffineTransform()
        : m_values { 1, 0, 0, 1, 0, 0 }
    {
    }

    AffineTransform(float a, float b, float c, float d, float e, float f)
        : m_values { a, b, c, d, e, f }
    {
    }

    bool is_identity() const;

    void map(float unmapped_x, float unmapped_y, float& mapped_x, float& mapped_y) const;

    template<typename T>
    Point<T> map(const Point<T>&) const;

    template<typename T>
    Size<T> map(const Size<T>&) const;

    template<typename T>
    Rect<T> map(const Rect<T>&) const;

    float a() const { return m_values[0]; }
    float b() const { return m_values[1]; }
    float c() const { return m_values[2]; }
    float d() const { return m_values[3]; }
    float e() const { return m_values[4]; }
    float f() const { return m_values[5]; }

    float x_scale() const;
    float y_scale() const;

    AffineTransform& scale(float sx, float sy);
    AffineTransform& translate(float tx, float ty);
    AffineTransform& rotate_radians(float);
    AffineTransform& multiply(const AffineTransform&);

private:
    float m_values[6] { 0 };
};

const LogStream& operator<<(const LogStream&, const AffineTransform&);

}
