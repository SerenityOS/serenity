/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Color.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Rect.h>

class Presentation;

struct HTMLElement {
    StringView tag_name;
    HashMap<StringView, DeprecatedString> attributes;
    HashMap<StringView, DeprecatedString> style;
    DeprecatedString inner_text;
    Vector<HTMLElement> children;

    ErrorOr<void> serialize(StringBuilder&) const;
};

// Anything that can be on a slide.
class SlideObject : public RefCounted<SlideObject> {
public:
    virtual ~SlideObject() = default;
    static ErrorOr<NonnullRefPtr<SlideObject>> parse_slide_object(JsonObject const& slide_object_json);
    virtual ErrorOr<HTMLElement> render(Presentation const&) const = 0;

protected:
    SlideObject() = default;

    virtual void set_property(StringView name, JsonValue);

    HashMap<DeprecatedString, JsonValue> m_properties;
    Gfx::IntRect m_rect;
};

// Objects with a foreground color.
class GraphicsObject : public SlideObject {
public:
    virtual ~GraphicsObject() = default;

protected:
    GraphicsObject() = default;
    virtual void set_property(StringView name, JsonValue) override;

    // FIXME: Change the default color based on the color scheme
    Gfx::Color m_color { Gfx::Color::Black };
};

class Text final : public GraphicsObject {
public:
    Text() = default;
    virtual ~Text() = default;

private:
    virtual ErrorOr<HTMLElement> render(Presentation const&) const override;
    virtual void set_property(StringView name, JsonValue) override;

    DeprecatedString m_text;
    DeprecatedString m_font_family;
    DeprecatedString m_text_align;
    float m_font_size_in_pt { 18 };
    unsigned m_font_weight { Gfx::FontWeight::Regular };
};

class Image final : public SlideObject {
public:
    Image() = default;
    virtual ~Image() = default;

private:
    DeprecatedString m_src;
    StringView m_image_rendering;

    virtual ErrorOr<HTMLElement> render(Presentation const&) const override;
    virtual void set_property(StringView name, JsonValue) override;
};
