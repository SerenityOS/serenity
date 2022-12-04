/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Slide.h"
#include <AK/JsonObject.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibGUI/Window.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Size.h>
#include <LibGfx/TextAlignment.h>

Slide::Slide(NonnullRefPtrVector<SlideObject> slide_objects, DeprecatedString title)
    : m_slide_objects(move(slide_objects))
    , m_title(move(title))
{
}

ErrorOr<Slide> Slide::parse_slide(JsonObject const& slide_json, NonnullRefPtr<GUI::Window> window)
{
    // FIXME: Use the text with the "title" role for a title, if there is no title given.
    auto title = slide_json.get("title"sv).as_string_or("Untitled slide");

    auto const& maybe_slide_objects = slide_json.get("objects"sv);
    if (!maybe_slide_objects.is_array())
        return Error::from_string_view("Slide objects must be an array"sv);

    auto const& json_slide_objects = maybe_slide_objects.as_array();
    NonnullRefPtrVector<SlideObject> slide_objects;
    for (auto const& maybe_slide_object_json : json_slide_objects.values()) {
        if (!maybe_slide_object_json.is_object())
            return Error::from_string_view("Slides must be objects"sv);
        auto const& slide_object_json = maybe_slide_object_json.as_object();

        auto slide_object = TRY(SlideObject::parse_slide_object(slide_object_json, window));
        slide_objects.append(move(slide_object));
    }

    return Slide { move(slide_objects), title };
}

void Slide::paint(Gfx::Painter& painter, unsigned int current_frame, Gfx::FloatSize display_scale) const
{
    for (auto const& object : m_slide_objects) {
        if (object.is_visible_during_frame(current_frame))
            object.paint(painter, display_scale);
    }

    // FIXME: Move this to user settings.
    painter.draw_text(painter.clip_rect(), title(), Gfx::TextAlignment::BottomCenter);
}
