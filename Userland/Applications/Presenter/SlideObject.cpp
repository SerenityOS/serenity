/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SlideObject.h"
#include "Presentation.h"
#include <AK/JsonObject.h>
#include <LibGUI/PropertyDeserializer.h>
#include <LibGfx/Font/FontStyleMapping.h>
#include <LibGfx/Rect.h>
#include <LibURL/URL.h>

static ByteString to_css_length(float design_value, Presentation const& presentation)
{
    float length_in_vw = design_value / static_cast<float>(presentation.normative_size().width()) * 100.0f;
    return ByteString::formatted("{}vw", length_in_vw);
}

ErrorOr<NonnullRefPtr<SlideObject>> SlideObject::parse_slide_object(JsonObject const& slide_object_json, unsigned slide_index)
{
    auto frame = slide_object_json.get_u32("frame"sv).value_or(0);
    auto maybe_type = slide_object_json.get_byte_string("type"sv);
    if (!maybe_type.has_value())
        return Error::from_string_view("Slide object must have a type"sv);

    auto type = maybe_type.value();
    RefPtr<SlideObject> object;
    if (type == "text"sv)
        object = TRY(try_make_ref_counted<Text>(Index { slide_index, frame }));
    else if (type == "image"sv)
        object = TRY(try_make_ref_counted<Image>(Index { slide_index, frame }));
    else
        return Error::from_string_view("Unsupported slide object type"sv);

    slide_object_json.for_each_member([&](auto const& key, auto const& value) {
        object->set_property(key, value);
    });

    return object.release_nonnull();
}

void SlideObject::set_property(StringView name, JsonValue value)
{
    if (name == "rect"sv)
        m_rect = GUI::PropertyDeserializer<Gfx::IntRect> {}(value).release_value_but_fixme_should_propagate_errors();
    m_properties.set(name, move(value));
}

void GraphicsObject::set_property(StringView name, JsonValue value)
{
    if (name == "color"sv) {
        if (auto color = Gfx::Color::from_string(value.as_string()); color.has_value()) {
            m_color = color.release_value();
        }
    }
    SlideObject::set_property(name, move(value));
}

void Text::set_property(StringView name, JsonValue value)
{
    if (name == "text"sv) {
        m_text = value.as_string();
    } else if (name == "font"sv) {
        m_font_family = value.as_string();
    } else if (name == "font-weight"sv) {
        m_font_weight = Gfx::name_to_weight(value.as_string());
    } else if (name == "font-size"sv) {
        m_font_size_in_pt = value.get_float_with_precision_loss().value();
    } else if (name == "text-alignment"sv) {
        m_text_align = value.as_string();
    }
    GraphicsObject::set_property(name, move(value));
}

void Image::set_property(StringView name, JsonValue value)
{
    if (name == "path"sv) {
        m_src = value.as_string();
    } else if (name == "scaling-mode"sv) {
        if (value.as_string() == "nearest-neighbor"sv)
            m_image_rendering = "crisp-edges"sv;
        else if (value.as_string() == "smooth-pixels"sv)
            m_image_rendering = "pixelated"sv;
    }
    SlideObject::set_property(name, move(value));
}

ErrorOr<HTMLElement> Text::render(Presentation const& presentation) const
{
    HTMLElement div;
    div.tag_name = "div"sv;
    TRY(div.attributes.try_set("class"sv, ByteString::formatted("frame slide{}-frame{}", m_slide_index, m_frame_index)));
    div.style.set("color"sv, m_color.to_byte_string());
    div.style.set("font-family"sv, ByteString::formatted("'{}'", m_font_family));
    div.style.set("font-size"sv, to_css_length(m_font_size_in_pt * 1.33333333f, presentation));
    div.style.set("font-weight"sv, ByteString::number(m_font_weight));
    div.style.set("text-align"sv, m_text_align);
    div.style.set("white-space"sv, "pre-wrap"sv);
    div.style.set("width"sv, to_css_length(m_rect.width(), presentation));
    div.style.set("height"sv, to_css_length(m_rect.height(), presentation));
    div.style.set("position"sv, "absolute"sv);
    div.style.set("left"sv, to_css_length(m_rect.left(), presentation));
    div.style.set("top"sv, to_css_length(m_rect.top(), presentation));
    div.inner_text = m_text;
    return div;
}

ErrorOr<HTMLElement> Image::render(Presentation const& presentation) const
{
    HTMLElement img;
    img.tag_name = "img"sv;
    img.attributes.set("src"sv, URL::create_with_file_scheme(m_src).to_byte_string());
    img.style.set("image-rendering"sv, m_image_rendering);
    if (m_rect.width() > m_rect.height())
        img.style.set("height"sv, "100%"sv);
    else
        img.style.set("width"sv, "100%"sv);

    HTMLElement image_wrapper;
    image_wrapper.tag_name = "div"sv;
    TRY(image_wrapper.attributes.try_set("class"sv, ByteString::formatted("frame slide{}-frame{}", m_slide_index, m_frame_index)));
    image_wrapper.children.append(move(img));
    image_wrapper.style.set("position"sv, "absolute"sv);
    image_wrapper.style.set("left"sv, to_css_length(m_rect.left(), presentation));
    image_wrapper.style.set("top"sv, to_css_length(m_rect.top(), presentation));
    image_wrapper.style.set("width"sv, to_css_length(m_rect.width(), presentation));
    image_wrapper.style.set("height"sv, to_css_length(m_rect.height(), presentation));
    image_wrapper.style.set("text-align"sv, "center"sv);
    return image_wrapper;
}

ErrorOr<void> HTMLElement::serialize(StringBuilder& builder) const
{
    TRY(builder.try_appendff("<{}", tag_name));
    for (auto const& [key, value] : attributes) {
        // FIXME: Escape the value string as necessary.
        TRY(builder.try_appendff(" {}='{}'", key, value));
    }
    if (!style.is_empty()) {
        TRY(builder.try_append(" style=\""sv));
        for (auto const& [key, value] : style) {
            // FIXME: Escape the value string as necessary.
            TRY(builder.try_appendff(" {}: {};", key, value));
        }
        TRY(builder.try_append("\""sv));
    }
    TRY(builder.try_append(">"sv));
    if (!inner_text.is_empty())
        TRY(builder.try_append(inner_text));
    for (auto const& child : children) {
        TRY(child.serialize(builder));
    }
    TRY(builder.try_appendff("</{}>", tag_name));
    return {};
}
