/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/HashMap.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font/FontWeight.h>
#include <LibGfx/Rect.h>

class Presentation;

struct HTMLElement {
    StringView tag_name;
    HashMap<StringView, ByteString> attributes;
    HashMap<StringView, ByteString> style;
    ByteString inner_text;
    Vector<HTMLElement> children;

    ErrorOr<void> serialize(StringBuilder&) const;
};
struct Index {
    unsigned slide;
    unsigned frame;
};

// Anything that can be on a slide.
class SlideObject : public RefCounted<SlideObject> {
public:
    virtual ~SlideObject() = default;
    static ErrorOr<NonnullRefPtr<SlideObject>> parse_slide_object(JsonObject const& slide_object_json, unsigned slide_index);
    virtual ErrorOr<HTMLElement> render(Presentation const&) const = 0;

protected:
    SlideObject(Index index)
        : m_frame_index(index.frame)
        , m_slide_index(index.slide)
    {
    }

    virtual void set_property(StringView name, JsonValue);

    unsigned m_frame_index;
    unsigned m_slide_index;
    HashMap<ByteString, JsonValue> m_properties;
    Gfx::IntRect m_rect;
};

// Objects with a foreground color.
class GraphicsObject : public SlideObject {
public:
    virtual ~GraphicsObject() = default;

protected:
    GraphicsObject(Index index)
        : SlideObject(index)
    {
    }
    virtual void set_property(StringView name, JsonValue) override;

    // FIXME: Change the default color based on the color scheme
    Gfx::Color m_color { Gfx::Color::Black };
};

class Text final : public GraphicsObject {
public:
    Text(Index index)
        : GraphicsObject(index)
    {
    }
    virtual ~Text() = default;

private:
    virtual ErrorOr<HTMLElement> render(Presentation const&) const override;
    virtual void set_property(StringView name, JsonValue) override;

    ByteString m_text;
    ByteString m_font_family;
    ByteString m_text_align;
    float m_font_size_in_pt { 18 };
    unsigned m_font_weight { Gfx::FontWeight::Regular };
};

class Image final : public SlideObject {
public:
    Image(Index index)
        : SlideObject(index)
    {
    }
    virtual ~Image() = default;

private:
    ByteString m_src;
    StringView m_image_rendering;

    virtual ErrorOr<HTMLElement> render(Presentation const&) const override;
    virtual void set_property(StringView name, JsonValue) override;
};
