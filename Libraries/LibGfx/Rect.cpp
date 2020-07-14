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

#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/Rect.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

void IntRect::intersect(const IntRect& other)
{
    int l = max(left(), other.left());
    int r = min(right(), other.right());
    int t = max(top(), other.top());
    int b = min(bottom(), other.bottom());

    if (l > r || t > b) {
        m_location = {};
        m_size = {};
        return;
    }

    m_location.set_x(l);
    m_location.set_y(t);
    m_size.set_width((r - l) + 1);
    m_size.set_height((b - t) + 1);
}

IntRect IntRect::united(const IntRect& other) const
{
    if (is_null())
        return other;
    if (other.is_null())
        return *this;
    IntRect rect;
    rect.set_left(min(left(), other.left()));
    rect.set_top(min(top(), other.top()));
    rect.set_right(max(right(), other.right()));
    rect.set_bottom(max(bottom(), other.bottom()));
    return rect;
}

Vector<IntRect, 4> IntRect::shatter(const IntRect& hammer) const
{
    Vector<IntRect, 4> pieces;
    if (!intersects(hammer)) {
        pieces.unchecked_append(*this);
        return pieces;
    }
    IntRect top_shard {
        x(),
        y(),
        width(),
        hammer.y() - y()
    };
    IntRect bottom_shard {
        x(),
        hammer.y() + hammer.height(),
        width(),
        (y() + height()) - (hammer.y() + hammer.height())
    };
    IntRect left_shard {
        x(),
        max(hammer.y(), y()),
        hammer.x() - x(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    IntRect right_shard {
        hammer.x() + hammer.width(),
        max(hammer.y(), y()),
        right() - hammer.right(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    if (!top_shard.is_empty())
        pieces.unchecked_append(top_shard);
    if (!bottom_shard.is_empty())
        pieces.unchecked_append(bottom_shard);
    if (!left_shard.is_empty())
        pieces.unchecked_append(left_shard);
    if (!right_shard.is_empty())
        pieces.unchecked_append(right_shard);

    return pieces;
}

void IntRect::align_within(const IntRect& other, TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::Center:
        center_within(other);
        return;
    case TextAlignment::TopLeft:
        set_location(other.location());
        return;
    case TextAlignment::TopRight:
        set_x(other.x() + other.width() - width());
        set_y(other.y());
        return;
    case TextAlignment::CenterLeft:
        set_x(other.x());
        center_vertically_within(other);
        return;
    case TextAlignment::CenterRight:
        set_x(other.x() + other.width() - width());
        center_vertically_within(other);
        return;
    }
}

String IntRect::to_string() const
{
    return String::format("[%d,%d %dx%d]", x(), y(), width(), height());
}

const LogStream& operator<<(const LogStream& stream, const IntRect& value)
{
    return stream << value.to_string();
}

}

namespace IPC {

bool encode(Encoder& encoder, const Gfx::IntRect& rect)
{
    encoder << rect.location() << rect.size();
    return true;
}

bool decode(Decoder& decoder, Gfx::IntRect& rect)
{
    Gfx::IntPoint point;
    Gfx::IntSize size;
    if (!decoder.decode(point))
        return false;
    if (!decoder.decode(size))
        return false;
    rect = { point, size };
    return true;
}

}
