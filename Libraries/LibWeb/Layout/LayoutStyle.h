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
#include <LibWeb/CSS/LengthBox.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web {

class InitialValues {
public:
    static CSS::Float float_() { return CSS::Float::None; }
    static CSS::WhiteSpace white_space() { return CSS::WhiteSpace::Normal; }
};

struct BorderData {
public:
    Color color { Color::Transparent };
    float width { 0 };
};

class LayoutStyle {
public:
    CSS::Float float_() const { return m_float; }
    Optional<int> z_index() const { return m_z_index; }
    CSS::TextAlign text_align() const { return m_text_align; }
    CSS::Position position() const { return m_position; }
    CSS::WhiteSpace white_space() const { return m_white_space; }
    const CSS::Length& width() const { return m_width; }
    const CSS::Length& min_width() const { return m_min_width; }
    const CSS::Length& max_width() const { return m_max_width; }
    const CSS::Length& height() const { return m_height; }
    const CSS::Length& min_height() const { return m_min_height; }
    const CSS::Length& max_height() const { return m_max_height; }

    const CSS::LengthBox& offset() const { return m_offset; }
    const CSS::LengthBox& margin() const { return m_margin; }
    const CSS::LengthBox& padding() const { return m_padding; }

    const BorderData& border_left() const { return m_border_left; }
    const BorderData& border_top() const { return m_border_top; }
    const BorderData& border_right() const { return m_border_right; }
    const BorderData& border_bottom() const { return m_border_bottom; }

protected:
    CSS::Float m_float { InitialValues::float_() };
    Optional<int> m_z_index;
    CSS::TextAlign m_text_align;
    CSS::Position m_position;
    CSS::WhiteSpace m_white_space { InitialValues::white_space() };
    CSS::Length m_width;
    CSS::Length m_min_width;
    CSS::Length m_max_width;
    CSS::Length m_height;
    CSS::Length m_min_height;
    CSS::Length m_max_height;
    CSS::LengthBox m_offset;
    CSS::LengthBox m_margin;
    CSS::LengthBox m_padding;
    BorderData m_border_left;
    BorderData m_border_top;
    BorderData m_border_right;
    BorderData m_border_bottom;
};

class ImmutableLayoutStyle final : public LayoutStyle {
};

class MutableLayoutStyle final : public LayoutStyle {
public:
    void set_float(CSS::Float value) { m_float = value; }
    void set_z_index(Optional<int> value) { m_z_index = value; }
    void set_text_align(CSS::TextAlign text_align) { m_text_align = text_align; }
    void set_position(CSS::Position position) { m_position = position; }
    void set_white_space(CSS::WhiteSpace value) { m_white_space = value; }
    void set_width(const CSS::Length& width) { m_width = width; }
    void set_min_width(const CSS::Length& width) { m_min_width = width; }
    void set_max_width(const CSS::Length& width) { m_max_width = width; }
    void set_height(const CSS::Length& height) { m_height = height; }
    void set_min_height(const CSS::Length& height) { m_min_height = height; }
    void set_max_height(const CSS::Length& height) { m_max_height = height; }
    void set_offset(const CSS::LengthBox& offset) { m_offset = offset; }
    void set_margin(const CSS::LengthBox& margin) { m_margin = margin; }
    void set_padding(const CSS::LengthBox& padding) { m_padding = padding; }
    BorderData& border_left() { return m_border_left; }
    BorderData& border_top() { return m_border_top; }
    BorderData& border_right() { return m_border_right; }
    BorderData& border_bottom() { return m_border_bottom; }
};

}
