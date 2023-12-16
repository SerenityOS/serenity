/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SlideObject.h"
#include <AK/ByteString.h>
#include <AK/Forward.h>

// A single slide of a presentation.
class Slide final {
public:
    static ErrorOr<Slide> parse_slide(JsonObject const& slide_json, unsigned slide_index);

    unsigned frame_count() const { return m_frame_count; }
    StringView title() const { return m_title; }

    ErrorOr<HTMLElement> render(Presentation const&) const;

private:
    Slide(unsigned frame_count, Vector<NonnullRefPtr<SlideObject>> slide_objects, ByteString title);

    unsigned m_frame_count;
    Vector<NonnullRefPtr<SlideObject>> m_slide_objects;
    ByteString m_title;
};
