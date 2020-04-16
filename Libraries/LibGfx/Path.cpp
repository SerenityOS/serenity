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

#include <AK/StringBuilder.h>
#include <LibGfx/Path.h>

namespace Gfx {

void Path::close()
{
    if (m_segments.size() <= 1)
        return;

    auto& last_point = m_segments.last().point;

    for (ssize_t i = m_segments.size() - 1; i >= 0; --i) {
        auto& segment = m_segments[i];
        if (segment.type == Segment::Type::MoveTo) {
            if (last_point == segment.point)
                return;
            m_segments.append({ Segment::Type::LineTo, segment.point });
            return;
        }
    }
}

String Path::to_string() const
{
    StringBuilder builder;
    builder.append("Path { ");
    for (auto& segment : m_segments) {
        switch (segment.type) {
        case Segment::Type::MoveTo:
            builder.append("MoveTo");
            break;
        case Segment::Type::LineTo:
            builder.append("LineTo");
            break;
        case Segment::Type::Invalid:
            builder.append("Invalid");
            break;
        }
        builder.append('(');
        builder.append(segment.point.to_string());
        builder.append(')');

        builder.append(' ');
    }
    builder.append("}");
    return builder.to_string();
}

}
