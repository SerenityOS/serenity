/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Presentation.h"
#include <AK/JsonObject.h>
#include <LibCore/Stream.h>
#include <LibGUI/Window.h>
#include <errno_codes.h>

Presentation::Presentation(Gfx::IntSize normative_size, HashMap<DeprecatedString, DeprecatedString> metadata)
    : m_normative_size(normative_size)
    , m_metadata(move(metadata))
{
}

NonnullOwnPtr<Presentation> Presentation::construct(Gfx::IntSize normative_size, HashMap<DeprecatedString, DeprecatedString> metadata)
{
    return NonnullOwnPtr<Presentation>(NonnullOwnPtr<Presentation>::Adopt, *new Presentation(normative_size, move(metadata)));
}

void Presentation::append_slide(Slide slide)
{
    m_slides.append(move(slide));
}

StringView Presentation::title() const
{
    if (m_metadata.contains("title"sv))
        return m_metadata.get("title"sv)->view();
    return "Untitled Presentation"sv;
}

StringView Presentation::author() const
{
    if (m_metadata.contains("author"sv))
        return m_metadata.get("author"sv)->view();
    return "Unknown Author"sv;
}

bool Presentation::has_a_next_frame() const
{
    return m_current_slide < u32(m_slides.size() > 1 ? m_slides.size() - 1 : 0);
}

bool Presentation::has_a_previous_frame() const
{
    return m_current_slide > 0u;
}

void Presentation::next_frame()
{
    m_current_frame_in_slide++;
    if (m_current_frame_in_slide >= current_slide().frame_count()) {
        m_current_frame_in_slide = 0;
        m_current_slide = min(m_current_slide.value() + 1u, m_slides.size() - 1);
    }
}

void Presentation::previous_frame()
{
    m_current_frame_in_slide.sub(1);
    if (m_current_frame_in_slide.has_overflow()) {
        m_current_slide.saturating_sub(1);
        m_current_frame_in_slide = m_current_slide == 0u ? 0 : current_slide().frame_count() - 1;
    }
}

void Presentation::go_to_first_slide()
{
    m_current_frame_in_slide = 0;
    m_current_slide = 0;
}

ErrorOr<NonnullOwnPtr<Presentation>> Presentation::load_from_file(StringView file_name)
{
    if (file_name.is_empty())
        return ENOENT;
    auto file = TRY(Core::Stream::File::open_file_or_standard_stream(file_name, Core::Stream::OpenMode::Read));
    auto contents = TRY(file->read_until_eof());
    auto content_string = StringView { contents };
    auto json = TRY(JsonValue::from_string(content_string));

    if (!json.is_object())
        return Error::from_string_view("Presentation must contain a global JSON object"sv);

    auto const& global_object = json.as_object();
    if (!global_object.has_number("version"sv))
        return Error::from_string_view("Presentation file is missing a version specification"sv);

    auto const version = global_object.get("version"sv).to_int(-1);
    if (version != PRESENTATION_FORMAT_VERSION)
        return Error::from_string_view("Presentation file has incompatible version"sv);

    auto const& maybe_metadata = global_object.get("metadata"sv);
    auto const& maybe_slides = global_object.get("slides"sv);

    if (maybe_metadata.is_null() || !maybe_metadata.is_object() || maybe_slides.is_null() || !maybe_slides.is_array())
        return Error::from_string_view("Metadata or slides in incorrect format"sv);

    auto const& raw_metadata = maybe_metadata.as_object();
    auto metadata = parse_metadata(raw_metadata);
    auto size = TRY(parse_presentation_size(raw_metadata));

    auto presentation = Presentation::construct(size, metadata);

    auto const& slides = maybe_slides.as_array();
    for (auto const& maybe_slide : slides.values()) {
        if (!maybe_slide.is_object())
            return Error::from_string_view("Slides must be objects"sv);
        auto const& slide_object = maybe_slide.as_object();

        auto slide = TRY(Slide::parse_slide(slide_object));
        presentation->append_slide(move(slide));
    }

    return presentation;
}

HashMap<DeprecatedString, DeprecatedString> Presentation::parse_metadata(JsonObject const& metadata_object)
{
    HashMap<DeprecatedString, DeprecatedString> metadata;

    metadata_object.for_each_member([&](auto const& key, auto const& value) {
        metadata.set(key, value.to_deprecated_string());
    });

    return metadata;
}

ErrorOr<Gfx::IntSize> Presentation::parse_presentation_size(JsonObject const& metadata_object)
{
    auto const& maybe_width = metadata_object.get("width"sv);
    auto const& maybe_aspect = metadata_object.get("aspect"sv);

    if (maybe_width.is_null() || !maybe_width.is_number() || maybe_aspect.is_null() || !maybe_aspect.is_string())
        return Error::from_string_view("Width or aspect in incorrect format"sv);

    // We intentionally discard floating-point data here. If you need more resolution, just use a larger width.
    auto const width = maybe_width.to_int();
    auto const aspect_parts = maybe_aspect.as_string().split_view(':');
    if (aspect_parts.size() != 2)
        return Error::from_string_view("Aspect specification must have the exact format `width:height`"sv);
    auto aspect_width = aspect_parts[0].to_int<int>();
    auto aspect_height = aspect_parts[1].to_int<int>();
    if (!aspect_width.has_value() || !aspect_height.has_value() || aspect_width.value() == 0 || aspect_height.value() == 0)
        return Error::from_string_view("Aspect width and height must be non-zero integers"sv);

    auto aspect_ratio = static_cast<double>(aspect_height.value()) / static_cast<double>(aspect_width.value());
    return Gfx::IntSize {
        width,
        static_cast<int>(round(static_cast<double>(width) * aspect_ratio)),
    };
}

ErrorOr<DeprecatedString> Presentation::render()
{
    HTMLElement main_element;
    main_element.tag_name = "main"sv;
    for (size_t i = 0; i < m_slides.size(); ++i) {
        HTMLElement slide_div;
        slide_div.tag_name = "div"sv;
        TRY(slide_div.style.try_set("display"sv, "none"sv));
        TRY(slide_div.attributes.try_set("id"sv, DeprecatedString::formatted("slide{}", i)));
        TRY(slide_div.attributes.try_set("class"sv, "slide"));
        auto& slide = m_slides[i];
        TRY(slide_div.children.try_append(TRY(slide.render(*this))));
        main_element.children.append(move(slide_div));
    }

    StringBuilder builder;
    TRY(builder.try_append(R"(
<!DOCTYPE html><html><head><style>
    .slide {
        position: absolute;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
    }
</style><script>
    function goto(slideIndex, frameIndex) {
        // FIXME: Honor the frameIndex.
        let slide;
        for (slide of document.getElementsByClassName("slide")) {
            slide.style.display = "none";
        }
        if (slide = document.getElementById(`slide${slideIndex}`))
            slide.style.display = "block";
    }
    window.onload = function() { goto(0, 0) }
</script><body>
)"sv));
    TRY(main_element.serialize(builder));
    TRY(builder.try_append("</body></html>"sv));
    return builder.to_deprecated_string();
}
