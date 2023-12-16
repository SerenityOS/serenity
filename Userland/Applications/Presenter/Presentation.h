/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Slide.h"
#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Size.h>

static constexpr int const PRESENTATION_FORMAT_VERSION = 1;

// In-memory representation of the presentation stored in a file.
// This class also contains all the parser code for loading .presenter files.
class Presentation {
public:
    ~Presentation() = default;

    static ErrorOr<NonnullOwnPtr<Presentation>> load_from_file(StringView file_name);

    StringView title() const;
    StringView author() const;
    Gfx::IntSize normative_size() const { return m_normative_size; }

    Slide const& current_slide() const { return m_slides[m_current_slide.value()]; }
    unsigned current_slide_number() const { return m_current_slide.value(); }
    unsigned current_frame_in_slide_number() const { return m_current_frame_in_slide.value(); }

    bool has_next_frame() const;
    bool has_previous_frame() const;
    void next_frame();
    void previous_frame();
    void go_to_first_slide();

    ErrorOr<ByteString> render();

private:
    static HashMap<ByteString, ByteString> parse_metadata(JsonObject const& metadata_object);
    static ErrorOr<Gfx::IntSize> parse_presentation_size(JsonObject const& metadata_object);

    Presentation(Gfx::IntSize normative_size, HashMap<ByteString, ByteString> metadata);
    static NonnullOwnPtr<Presentation> construct(Gfx::IntSize normative_size, HashMap<ByteString, ByteString> metadata);

    void append_slide(Slide slide);

    Vector<Slide> m_slides {};
    // This is not a pixel size, but an abstract size used by the slide objects for relative positioning.
    Gfx::IntSize m_normative_size;
    HashMap<ByteString, ByteString> m_metadata;

    Checked<unsigned> m_current_slide { 0 };
    Checked<unsigned> m_current_frame_in_slide { 0 };
};
