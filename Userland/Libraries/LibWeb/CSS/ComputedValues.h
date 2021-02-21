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

namespace Web::CSS {

class InitialValues {
public:
    static CSS::Float float_() { return CSS::Float::None; }
    static CSS::Clear clear() { return CSS::Clear::None; }
    static CSS::Cursor cursor() { return CSS::Cursor::Auto; }
    static CSS::WhiteSpace white_space() { return CSS::WhiteSpace::Normal; }
    static CSS::TextAlign text_align() { return CSS::TextAlign::Left; }
    static CSS::Position position() { return CSS::Position::Static; }
    static CSS::TextDecorationLine text_decoration_line() { return CSS::TextDecorationLine::None; }
    static CSS::TextTransform text_transform() { return CSS::TextTransform::None; }
    static CSS::Display display() { return CSS::Display::Inline; }
    static Color color() { return Color::Black; }
    static Color background_color() { return Color::Transparent; }
    static CSS::ListStyleType list_style_type() { return CSS::ListStyleType::Disc; }
    static CSS::FlexDirection flex_direction() { return CSS::FlexDirection::Row; }
    static CSS::Overflow overflow() { return CSS::Overflow::Visible; }
};

struct BorderData {
public:
    Color color { Color::Transparent };
    CSS::LineStyle line_style { CSS::LineStyle::None };
    float width { 0 };
};

class ComputedValues {
public:
    CSS::Float float_() const { return m_noninherited.float_; }
    CSS::Clear clear() const { return m_noninherited.clear; }
    CSS::Cursor cursor() const { return m_inherited.cursor; }
    CSS::Display display() const { return m_noninherited.display; }
    Optional<int> z_index() const { return m_noninherited.z_index; }
    CSS::TextAlign text_align() const { return m_inherited.text_align; }
    CSS::TextDecorationLine text_decoration_line() const { return m_noninherited.text_decoration_line; }
    CSS::TextTransform text_transform() const { return m_inherited.text_transform; }
    CSS::Position position() const { return m_noninherited.position; }
    CSS::WhiteSpace white_space() const { return m_inherited.white_space; }
    CSS::FlexDirection flex_direction() const { return m_noninherited.flex_direction; }
    const CSS::Length& width() const { return m_noninherited.width; }
    const CSS::Length& min_width() const { return m_noninherited.min_width; }
    const CSS::Length& max_width() const { return m_noninherited.max_width; }
    const CSS::Length& height() const { return m_noninherited.height; }
    const CSS::Length& min_height() const { return m_noninherited.min_height; }
    const CSS::Length& max_height() const { return m_noninherited.max_height; }

    const CSS::LengthBox& offset() const { return m_noninherited.offset; }
    const CSS::LengthBox& margin() const { return m_noninherited.margin; }
    const CSS::LengthBox& padding() const { return m_noninherited.padding; }

    const BorderData& border_left() const { return m_noninherited.border_left; }
    const BorderData& border_top() const { return m_noninherited.border_top; }
    const BorderData& border_right() const { return m_noninherited.border_right; }
    const BorderData& border_bottom() const { return m_noninherited.border_bottom; }

    CSS::Overflow overflow_x() const { return m_noninherited.overflow_x; }
    CSS::Overflow overflow_y() const { return m_noninherited.overflow_y; }

    Color color() const { return m_inherited.color; }
    Color background_color() const { return m_noninherited.background_color; }

    CSS::ListStyleType list_style_type() const { return m_inherited.list_style_type; }

    ComputedValues clone_inherited_values() const
    {
        ComputedValues clone;
        clone.m_inherited = m_inherited;
        return clone;
    }

protected:
    struct {
        Color color { InitialValues::color() };
        CSS::Cursor cursor { InitialValues::cursor() };
        CSS::TextAlign text_align { InitialValues::text_align() };
        CSS::TextTransform text_transform { InitialValues::text_transform() };
        CSS::WhiteSpace white_space { InitialValues::white_space() };
        CSS::ListStyleType list_style_type { InitialValues::list_style_type() };
    } m_inherited;

    struct {
        CSS::Float float_ { InitialValues::float_() };
        CSS::Clear clear { InitialValues::clear() };
        CSS::Display display { InitialValues::display() };
        Optional<int> z_index;
        CSS::TextDecorationLine text_decoration_line { InitialValues::text_decoration_line() };
        CSS::Position position { InitialValues::position() };
        CSS::Length width;
        CSS::Length min_width;
        CSS::Length max_width;
        CSS::Length height;
        CSS::Length min_height;
        CSS::Length max_height;
        CSS::LengthBox offset;
        CSS::LengthBox margin;
        CSS::LengthBox padding;
        BorderData border_left;
        BorderData border_top;
        BorderData border_right;
        BorderData border_bottom;
        Color background_color { InitialValues::background_color() };
        CSS::FlexDirection flex_direction { InitialValues::flex_direction() };
        CSS::Overflow overflow_x { InitialValues::overflow() };
        CSS::Overflow overflow_y { InitialValues::overflow() };
    } m_noninherited;
};

class ImmutableComputedValues final : public ComputedValues {
};

class MutableComputedValues final : public ComputedValues {
public:
    void set_color(const Color& color) { m_inherited.color = color; }
    void set_cursor(CSS::Cursor cursor) { m_inherited.cursor = cursor; }
    void set_background_color(const Color& color) { m_noninherited.background_color = color; }
    void set_float(CSS::Float value) { m_noninherited.float_ = value; }
    void set_clear(CSS::Clear value) { m_noninherited.clear = value; }
    void set_z_index(Optional<int> value) { m_noninherited.z_index = value; }
    void set_text_align(CSS::TextAlign text_align) { m_inherited.text_align = text_align; }
    void set_text_decoration_line(CSS::TextDecorationLine value) { m_noninherited.text_decoration_line = value; }
    void set_text_transform(CSS::TextTransform value) { m_inherited.text_transform = value; }
    void set_position(CSS::Position position) { m_noninherited.position = position; }
    void set_white_space(CSS::WhiteSpace value) { m_inherited.white_space = value; }
    void set_width(const CSS::Length& width) { m_noninherited.width = width; }
    void set_min_width(const CSS::Length& width) { m_noninherited.min_width = width; }
    void set_max_width(const CSS::Length& width) { m_noninherited.max_width = width; }
    void set_height(const CSS::Length& height) { m_noninherited.height = height; }
    void set_min_height(const CSS::Length& height) { m_noninherited.min_height = height; }
    void set_max_height(const CSS::Length& height) { m_noninherited.max_height = height; }
    void set_offset(const CSS::LengthBox& offset) { m_noninherited.offset = offset; }
    void set_margin(const CSS::LengthBox& margin) { m_noninherited.margin = margin; }
    void set_padding(const CSS::LengthBox& padding) { m_noninherited.padding = padding; }
    void set_overflow_x(CSS::Overflow value) { m_noninherited.overflow_x = value; }
    void set_overflow_y(CSS::Overflow value) { m_noninherited.overflow_y = value; }
    void set_list_style_type(CSS::ListStyleType value) { m_inherited.list_style_type = value; }
    void set_display(CSS::Display value) { m_noninherited.display = value; }
    BorderData& border_left() { return m_noninherited.border_left; }
    BorderData& border_top() { return m_noninherited.border_top; }
    BorderData& border_right() { return m_noninherited.border_right; }
    BorderData& border_bottom() { return m_noninherited.border_bottom; }
    void set_flex_direction(CSS::FlexDirection value) { m_noninherited.flex_direction = value; }
};

}
