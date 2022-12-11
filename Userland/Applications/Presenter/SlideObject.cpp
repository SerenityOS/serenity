/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SlideObject.h"
#include <AK/JsonObject.h>
#include <AK/RefPtr.h>
#include <LibCore/Object.h>
#include <LibCore/Stream.h>
#include <LibGUI/Margins.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Size.h>
#include <LibGfx/TextWrapping.h>
#include <LibImageDecoderClient/Client.h>

ErrorOr<NonnullRefPtr<SlideObject>> SlideObject::parse_slide_object(JsonObject const& slide_object_json, NonnullRefPtr<GUI::Window> window)
{
    auto image_decoder_client = TRY(ImageDecoderClient::Client::try_create());

    auto const& maybe_type = slide_object_json.get("type"sv);
    if (!maybe_type.is_string())
        return Error::from_string_view("Slide object must have a type"sv);

    auto type = maybe_type.as_string();
    RefPtr<SlideObject> object;
    if (type == "text"sv)
        object = TRY(try_make_ref_counted<Text>());
    else if (type == "image"sv)
        object = TRY(try_make_ref_counted<Image>(image_decoder_client, window));
    else
        return Error::from_string_view("Unsupported slide object type"sv);

    slide_object_json.for_each_member([&](auto const& key, auto const& value) {
        if (key == "type"sv)
            return;
        auto successful = object->set_property(key, value);
        if (!successful)
            dbgln("Storing {:15} = {:20} on slide object type {:8} failed, ignoring.", key, value, type);
    });

    return object.release_nonnull();
}

SlideObject::SlideObject()
{
    REGISTER_RECT_PROPERTY("rect", rect, set_rect);
}

// FIXME: Consider drawing a placeholder box instead.
void SlideObject::paint(Gfx::Painter&, Gfx::FloatSize) const { }

Gfx::IntRect SlideObject::transformed_bounding_box(Gfx::IntRect clip_rect, Gfx::FloatSize display_scale) const
{
    return m_rect.to_type<float>().scaled(display_scale.width(), display_scale.height()).to_rounded<int>().translated(clip_rect.top_left());
}

GraphicsObject::GraphicsObject()
{
    register_property(
        "color", [this]() { return this->color().to_deprecated_string(); },
        [this](auto& value) {
            auto color = Color::from_string(value.to_deprecated_string());
            if (color.has_value()) {
                this->set_color(color.value());
                return true;
            }
            return false;
        });
}

Text::Text()
{
    REGISTER_STRING_PROPERTY("text", text, set_text);
    REGISTER_FONT_WEIGHT_PROPERTY("font-weight", font_weight, set_font_weight);
    REGISTER_TEXT_ALIGNMENT_PROPERTY("text-alignment", text_alignment, set_text_alignment);
    REGISTER_INT_PROPERTY("font-size", font_size, set_font_size);
    REGISTER_STRING_PROPERTY("font", font, set_font);
}

void Text::paint(Gfx::Painter& painter, Gfx::FloatSize display_scale) const
{
    auto scaled_bounding_box = this->transformed_bounding_box(painter.clip_rect(), display_scale);

    auto scaled_font_size = display_scale.height() * static_cast<float>(m_font_size);
    auto font = Gfx::FontDatabase::the().get(m_font, scaled_font_size, m_font_weight, 0, Gfx::Font::AllowInexactSizeMatch::Yes);
    if (font.is_null())
        font = Gfx::FontDatabase::default_font();

    painter.draw_text(scaled_bounding_box, m_text.view(), *font, m_text_alignment, m_color, Gfx::TextElision::None, Gfx::TextWrapping::Wrap);
}

Image::Image(NonnullRefPtr<ImageDecoderClient::Client> client, NonnullRefPtr<GUI::Window> window)
    : m_client(move(client))
    , m_window(move(window))
{
    REGISTER_STRING_PROPERTY("path", image_path, set_image_path);
    REGISTER_ENUM_PROPERTY("scaling", scaling, set_scaling, ImageScaling,
        { ImageScaling::FitSmallest, "fit-smallest" },
        { ImageScaling::FitLargest, "fit-largest" },
        { ImageScaling::Stretch, "stretch" }, );
    REGISTER_ENUM_PROPERTY("scaling-mode", scaling_mode, set_scaling_mode, Gfx::Painter::ScalingMode,
        { Gfx::Painter::ScalingMode::SmoothPixels, "smooth-pixels" },
        { Gfx::Painter::ScalingMode::NearestNeighbor, "nearest-neighbor" },
        { Gfx::Painter::ScalingMode::BilinearBlend, "bilinear-blend" }, );
}

// FIXME: Should run on another thread and report errors.
ErrorOr<void> Image::reload_image()
{
    auto file = TRY(Core::Stream::File::open(m_image_path, Core::Stream::OpenMode::Read));
    auto data = TRY(file->read_until_eof());
    auto maybe_decoded = m_client->decode_image(data);
    if (!maybe_decoded.has_value() || maybe_decoded.value().frames.size() < 1)
        return Error::from_string_view("Could not decode image"sv);
    // FIXME: Handle multi-frame images.
    m_currently_loaded_image = maybe_decoded.value().frames.first().bitmap;
    return {};
}

void Image::paint(Gfx::Painter& painter, Gfx::FloatSize display_scale) const
{
    if (!m_currently_loaded_image)
        return;

    auto transformed_bounding_box = this->transformed_bounding_box(painter.clip_rect(), display_scale);

    auto image_size = m_currently_loaded_image->size();
    auto image_aspect_ratio = image_size.aspect_ratio();

    auto image_box = transformed_bounding_box;
    if (m_scaling != ImageScaling::Stretch) {
        auto width_corresponding_to_height = image_box.height() * image_aspect_ratio;
        auto direction_to_preserve_for_fit = width_corresponding_to_height > image_box.width() ? Orientation::Horizontal : Orientation::Vertical;
        // Fit largest and fit smallest are the same, except with inverted preservation conditions.
        if (m_scaling == ImageScaling::FitLargest)
            direction_to_preserve_for_fit = direction_to_preserve_for_fit == Orientation::Vertical ? Orientation::Horizontal : Orientation::Vertical;

        image_box.set_size(image_box.size().match_aspect_ratio(image_aspect_ratio, direction_to_preserve_for_fit));
    }

    image_box = image_box.centered_within(transformed_bounding_box);

    auto original_clip_rect = painter.clip_rect();
    painter.clear_clip_rect();
    painter.add_clip_rect(image_box);

    // FIXME: Allow to set the scaling mode.
    painter.draw_scaled_bitmap(image_box, *m_currently_loaded_image, m_currently_loaded_image->rect(), 1.0f, m_scaling_mode);

    painter.clear_clip_rect();
    painter.add_clip_rect(original_clip_rect);
}
