/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "SlideObject.h"
#include <AK/DeprecatedString.h>
#include <AK/Forward.h>

// A single slide of a presentation.
class Slide final {
public:
    static ErrorOr<Slide> parse_slide(JsonObject const& slide_json);

    // FIXME: shouldn't be hard-coded to 1.
    unsigned frame_count() const { return 1; }
    StringView title() const { return m_title; }

    ErrorOr<HTMLElement> render(Presentation const&) const;

private:
    Slide(Vector<NonnullRefPtr<SlideObject>> slide_objects, DeprecatedString title);

    Vector<NonnullRefPtr<SlideObject>> m_slide_objects;
    DeprecatedString m_title;
};
