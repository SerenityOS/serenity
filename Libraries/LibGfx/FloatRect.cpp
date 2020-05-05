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

#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/FloatRect.h>
#include <LibIPC/Decoder.h>

namespace Gfx {

void FloatRect::intersect(const FloatRect& other)
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

FloatRect FloatRect::united(const FloatRect& other) const
{
    if (is_null())
        return other;
    if (other.is_null())
        return *this;
    FloatRect rect;
    rect.set_left(min(left(), other.left()));
    rect.set_top(min(top(), other.top()));
    rect.set_right(max(right(), other.right()));
    rect.set_bottom(max(bottom(), other.bottom()));
    return rect;
}

Vector<FloatRect, 4> FloatRect::shatter(const FloatRect& hammer) const
{
    Vector<FloatRect, 4> pieces;
    if (!intersects(hammer)) {
        pieces.unchecked_append(*this);
        return pieces;
    }
    FloatRect top_shard {
        x(),
        y(),
        width(),
        hammer.y() - y()
    };
    FloatRect bottom_shard {
        x(),
        hammer.y() + hammer.height(),
        width(),
        (y() + height()) - (hammer.y() + hammer.height())
    };
    FloatRect left_shard {
        x(),
        max(hammer.y(), y()),
        hammer.x() - x(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    FloatRect right_shard {
        hammer.x() + hammer.width(),
        max(hammer.y(), y()),
        right() - hammer.right(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    if (intersects(top_shard))
        pieces.unchecked_append(top_shard);
    if (intersects(bottom_shard))
        pieces.unchecked_append(bottom_shard);
    if (intersects(left_shard))
        pieces.unchecked_append(left_shard);
    if (intersects(right_shard))
        pieces.unchecked_append(right_shard);

    return pieces;
}

void FloatRect::align_within(const FloatRect& other, TextAlignment alignment)
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

}
