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

namespace GUI {

class Margins {
public:
    Margins() {}
    Margins(int left, int top, int right, int bottom)
        : m_left(left)
        , m_top(top)
        , m_right(right)
        , m_bottom(bottom)
    {
    }
    ~Margins() {}

    bool is_null() const { return !m_left && !m_top && !m_right && !m_bottom; }

    int left() const { return m_left; }
    int top() const { return m_top; }
    int right() const { return m_right; }
    int bottom() const { return m_bottom; }

    void set_left(int value) { m_left = value; }
    void set_top(int value) { m_top = value; }
    void set_right(int value) { m_right = value; }
    void set_bottom(int value) { m_bottom = value; }

    bool operator==(const Margins& other) const
    {
        return m_left == other.m_left
            && m_top == other.m_top
            && m_right == other.m_right
            && m_bottom == other.m_bottom;
    }

private:
    int m_left { 0 };
    int m_top { 0 };
    int m_right { 0 };
    int m_bottom { 0 };
};
}
