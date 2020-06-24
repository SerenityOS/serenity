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

#include <AK/Optional.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web {

class LayoutStyle {
public:
    Optional<int> z_index() const { return m_z_index; }
    CSS::TextAlign text_align() const { return m_text_align; }
    CSS::Position position() const { return m_position; }
    const Length& width() const { return m_width; }
    const Length& min_width() const { return m_min_width; }
    const Length& max_width() const { return m_max_width; }

protected:
    Optional<int> m_z_index;
    CSS::TextAlign m_text_align;
    CSS::Position m_position;
    Length m_width;
    Length m_min_width;
    Length m_max_width;
};

class ImmutableLayoutStyle final : public LayoutStyle {
};

class MutableLayoutStyle final : public LayoutStyle {
public:
    void set_z_index(Optional<int> value) { m_z_index = value; }
    void set_text_align(CSS::TextAlign text_align) { m_text_align = text_align; }
    void set_position(CSS::Position position) { m_position = position; }
    void set_width(const Length& width) { m_width = width; }
    void set_min_width(const Length& width) { m_min_width = width; }
    void set_max_width(const Length& width) { m_max_width = width; }
};

}
