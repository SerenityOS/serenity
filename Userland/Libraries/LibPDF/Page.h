/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibPDF/Forward.h>

namespace PDF {

struct Rectangle {
    float lower_left_x;
    float lower_left_y;
    float upper_right_x;
    float upper_right_y;

    float width() const { return upper_right_x - lower_left_x; }
    float height() const { return upper_right_y - lower_left_y; }
};

struct Page {
    NonnullRefPtr<DictObject> resources;
    RefPtr<Object> contents;
    Rectangle media_box;
    Rectangle crop_box;
    float user_unit;
    int rotate;

    PDFErrorOr<ByteBuffer> page_contents(Document&) const;
};

}

namespace AK {

template<>
struct Formatter<PDF::Rectangle> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::Rectangle const& rectangle)
    {
        return Formatter<FormatString>::format(builder,
            "Rectangle {{ ll=({}, {}), ur=({}, {}) }}"sv,
            rectangle.lower_left_x,
            rectangle.lower_left_y,
            rectangle.upper_right_x,
            rectangle.upper_right_y);
    }
};

template<>
struct Formatter<PDF::Page> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, PDF::Page const& page)
    {
        return Formatter<FormatString>::format(builder,
            "Page {{\n  resources={}\n  contents={}\n  media_box={}\n  crop_box={}\n  user_unit={}\n  rotate={}\n}}"sv,
            page.resources->to_byte_string(1),
            page.contents->to_byte_string(1),
            page.media_box,
            page.crop_box,
            page.user_unit,
            page.rotate);
    }
};

}
