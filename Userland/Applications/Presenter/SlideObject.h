/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Color.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Rect.h>
#include <LibGfx/TextAlignment.h>
#include <LibImageDecoderClient/Client.h>

// Anything that can be on a slide.
// For properties set in the file, we re-use the Core::Object property facility.
class SlideObject : public Core::Object {
    C_OBJECT_ABSTRACT(SlideObject);

public:
    virtual ~SlideObject() = default;

    static ErrorOr<NonnullRefPtr<SlideObject>> parse_slide_object(JsonObject const& slide_object_json, NonnullRefPtr<GUI::Window> window);

    // FIXME: Actually determine this from the file data.
    bool is_visible_during_frame([[maybe_unused]] unsigned frame_number) const { return true; }

    virtual void paint(Gfx::Painter&, Gfx::FloatSize display_scale) const;
    ALWAYS_INLINE Gfx::IntRect transformed_bounding_box(Gfx::IntRect clip_rect, Gfx::FloatSize display_scale) const;

    void set_rect(Gfx::IntRect rect) { m_rect = rect; }
    Gfx::IntRect rect() const { return m_rect; }

protected:
    SlideObject();

    Gfx::IntRect m_rect;
};

// Objects with a foreground color.
class GraphicsObject : public SlideObject {
    C_OBJECT_ABSTRACT(SlideObject);

public:
    virtual ~GraphicsObject() = default;

    void set_color(Gfx::Color color) { m_color = color; }
    Gfx::Color color() const { return m_color; }

protected:
    GraphicsObject();

    // FIXME: Change the default color based on the color scheme
    Gfx::Color m_color { Gfx::Color::Black };
};

class Text : public GraphicsObject {
    C_OBJECT(SlideObject);

public:
    Text();
    virtual ~Text() = default;

    virtual void paint(Gfx::Painter&, Gfx::FloatSize display_scale) const override;

    void set_font(DeprecatedString font) { m_font = move(font); }
    StringView font() const { return m_font; }
    void set_font_size(int font_size) { m_font_size = font_size; }
    int font_size() const { return m_font_size; }
    void set_font_weight(unsigned font_weight) { m_font_weight = font_weight; }
    unsigned font_weight() const { return m_font_weight; }
    void set_text_alignment(Gfx::TextAlignment text_alignment) { m_text_alignment = text_alignment; }
    Gfx::TextAlignment text_alignment() const { return m_text_alignment; }
    void set_text(DeprecatedString text) { m_text = move(text); }
    StringView text() const { return m_text; }

protected:
    DeprecatedString m_text;
    // The font family, technically speaking.
    DeprecatedString m_font;
    int m_font_size { 18 };
    unsigned m_font_weight { Gfx::FontWeight::Regular };
    Gfx::TextAlignment m_text_alignment { Gfx::TextAlignment::CenterLeft };
};

// How to scale an image object.
enum class ImageScaling {
    // Fit the image into the bounding box, preserving its aspect ratio.
    FitSmallest,
    // Match the bounding box in width and height exactly; this will change the image's aspect ratio if the aspect ratio of the bounding box is not exactly the same.
    Stretch,
    // Make the image fill the bounding box, preserving its aspect ratio. This means that the image will be cut off on the top and bottom or left and right, depending on which dimension is "too large".
    FitLargest,
};

class Image : public SlideObject {
    C_OBJECT(Image);

public:
    Image(NonnullRefPtr<ImageDecoderClient::Client>, NonnullRefPtr<GUI::Window>);
    virtual ~Image() = default;

    virtual void paint(Gfx::Painter&, Gfx::FloatSize display_scale) const override;

    void set_image_path(DeprecatedString image_path)
    {
        m_image_path = move(image_path);
        auto result = reload_image();
        if (result.is_error())
            GUI::MessageBox::show_error(m_window, DeprecatedString::formatted("Loading image {} failed: {}", m_image_path, result.error()));
    }
    StringView image_path() const { return m_image_path; }
    void set_scaling(ImageScaling scaling) { m_scaling = scaling; }
    ImageScaling scaling() const { return m_scaling; }
    void set_scaling_mode(Gfx::Painter::ScalingMode scaling_mode) { m_scaling_mode = scaling_mode; }
    Gfx::Painter::ScalingMode scaling_mode() const { return m_scaling_mode; }

protected:
    DeprecatedString m_image_path;
    ImageScaling m_scaling { ImageScaling::FitSmallest };
    Gfx::Painter::ScalingMode m_scaling_mode { Gfx::Painter::ScalingMode::SmoothPixels };

private:
    ErrorOr<void> reload_image();

    RefPtr<Gfx::Bitmap> m_currently_loaded_image;
    NonnullRefPtr<ImageDecoderClient::Client> m_client;
    NonnullRefPtr<GUI::Window> m_window;
};
