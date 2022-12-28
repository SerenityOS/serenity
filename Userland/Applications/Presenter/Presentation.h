/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Slide.h"
#include <AK/DeprecatedString.h>
#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>
#include <LibCore/DateTime.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Size.h>

static constexpr int const PRESENTATION_FORMAT_VERSION = 1;

// In-memory representation of the presentation stored in a file.
// This class also contains all the parser code for loading .presenter files.
class Presentation {
public:
    ~Presentation() = default;

    // We can't pass this class directly in an ErrorOr because some of the components are not properly moveable under these conditions.
    static ErrorOr<NonnullOwnPtr<Presentation>> load_from_file(StringView file_name, NonnullRefPtr<GUI::Window> window);

    StringView title() const;
    StringView author() const;
    Core::DateTime last_modified() const;
    Gfx::IntSize normative_size() const { return m_normative_size; }

    Slide const& current_slide() const { return m_slides[m_current_slide.value()]; }
    unsigned current_slide_number() const { return m_current_slide.value(); }
    unsigned current_frame_in_slide_number() const { return m_current_frame_in_slide.value(); }

    unsigned total_frame_count() const;
    unsigned total_slide_count() const;
    unsigned global_frame_number() const;

    void next_frame();
    void previous_frame();
    void go_to_first_slide();
    void go_to_slide(unsigned slide_index);

    // This assumes that the caller has clipped the painter to exactly the display area.
    void paint(Gfx::Painter& painter) const;

    // Formats a footer with user-supplied formatting parameters.
    // {presentation_title}, {slide_title}, {author}, {slides_total}, {frames_total}, {date}
    // {slide_number}: Slide number
    // {slide_frame_number}: Number of frame within slide
    // {slide_frames_total}: Total number of frames within the current slide
    // {frame_number}: Counts all frames on all slides
    DeprecatedString format_footer(StringView format) const;

    Optional<DeprecatedString> footer_text() const;

private:
    static HashMap<DeprecatedString, DeprecatedString> parse_metadata(JsonObject const& metadata_object);
    static ErrorOr<Gfx::IntSize> parse_presentation_size(JsonObject const& metadata_object);

    Presentation(Gfx::IntSize normative_size, HashMap<DeprecatedString, DeprecatedString> metadata, HashMap<DeprecatedString, JsonObject> templates);
    static NonnullOwnPtr<Presentation> construct(Gfx::IntSize normative_size, HashMap<DeprecatedString, DeprecatedString> metadata, HashMap<DeprecatedString, JsonObject> templates);

    void append_slide(Slide slide);

    Vector<Slide> m_slides {};
    // This is not a pixel size, but an abstract size used by the slide objects for relative positioning.
    Gfx::IntSize m_normative_size;
    HashMap<DeprecatedString, DeprecatedString> m_metadata;
    HashMap<DeprecatedString, JsonObject> m_templates;

    Checked<unsigned> m_current_slide { 0 };
    Checked<unsigned> m_current_frame_in_slide { 0 };
};
