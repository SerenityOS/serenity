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

#pragma once

#include <AK/RefPtr.h>

namespace Web {

class LayoutNode;

struct LayoutPosition {
    RefPtr<LayoutNode> layout_node;
    int index_in_node { 0 };
};

class LayoutRange {
public:
    LayoutRange() { }
    LayoutRange(const LayoutPosition& start, const LayoutPosition& end)
        : m_start(start)
        , m_end(end)
    {
    }

    bool is_valid() const { return m_start.layout_node && m_end.layout_node; }

    void set(const LayoutPosition& start, const LayoutPosition& end)
    {
        m_start = start;
        m_end = end;
    }

    void set_start(const LayoutPosition& start) { m_start = start; }
    void set_end(const LayoutPosition& end) { m_end = end; }

    const LayoutPosition& start() const { return m_start; }
    const LayoutPosition& end() const { return m_end; }

    LayoutRange normalized() const;

private:
    LayoutPosition m_start;
    LayoutPosition m_end;
};

}
