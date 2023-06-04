/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Function.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Loader/ImageResource.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>
#include <LibWeb/SVG/SVGDecodedImageData.h>

namespace Web {

NonnullRefPtr<ImageResource> ImageResource::convert_from_resource(Resource& resource)
{
    return adopt_ref(*new ImageResource(resource));
}

ImageResource::ImageResource(LoadRequest const& request)
    : Resource(Type::Image, request)
{
}

ImageResource::ImageResource(Resource& resource)
    : Resource(Type::Image, resource)
{
}

ImageResource::~ImageResource() = default;

int ImageResource::frame_duration(size_t frame_index) const
{
    decode_if_needed();
    if (frame_index >= m_decoded_frames.size())
        return 0;
    return m_decoded_frames[frame_index].duration;
}

void ImageResource::decode_if_needed() const
{
    if (!has_encoded_data())
        return;

    if (m_has_attempted_decode)
        return;

    if (!m_decoded_frames.is_empty())
        return;

    bool is_svg_image = mime_type().starts_with("image/svg+xml"sv) || url().basename().ends_with(".svg"sv);
    if (is_svg_image) {
        decode_svg_image();
    } else {
        decode_image();
    }

    m_has_attempted_decode = true;
}

void ImageResource::decode_svg_image() const
{
    auto page = request().page();
    if (!page.has_value())
        return;

    auto svg_or_error = SVG::SVGDecodedImageData::create(page.value(), url(), encoded_data());
    if (svg_or_error.is_error()) {
        dbgln("Could not decode svg image resource {}", url());
        return;
    }

    auto svg = svg_or_error.release_value();
    m_loop_count = svg->loop_count();
    m_animated = svg->is_animated();
    m_decoded_frames.resize(svg->frame_count());
    for (size_t i = 0; i < m_decoded_frames.size(); ++i) {
        auto& frame = m_decoded_frames[i];
        // FIXME: Decide on what to do when there is no intrinsic width or height
        auto width = svg->intrinsic_width();
        auto height = svg->intrinsic_height();
        if (width.has_value() && height.has_value()) {
            auto bitmap = svg->bitmap(i, { static_cast<int>(width->value()), static_cast<int>(height->value()) });
            frame.bitmap = bitmap->clone().release_value_but_fixme_should_propagate_errors();
        }
        frame.duration = svg->frame_duration(i);
    }
}

void ImageResource::decode_image() const
{
    auto image = Platform::ImageCodecPlugin::the().decode_image(encoded_data());
    if (!image.has_value()) {
        dbgln("Could not decode image resource {}", url());
        return;
    }

    m_loop_count = image.value().loop_count;
    m_animated = image.value().is_animated;
    m_decoded_frames.resize(image.value().frames.size());
    for (size_t i = 0; i < m_decoded_frames.size(); ++i) {
        auto& frame = m_decoded_frames[i];
        frame.bitmap = image.value().frames[i].bitmap;
        frame.duration = image.value().frames[i].duration;
    }
}

Gfx::Bitmap const* ImageResource::bitmap(size_t frame_index) const
{
    decode_if_needed();
    if (frame_index >= m_decoded_frames.size())
        return nullptr;
    return m_decoded_frames[frame_index].bitmap;
}

void ImageResource::update_volatility()
{
    bool visible_in_viewport = false;
    for_each_client([&](auto& client) {
        if (static_cast<const ImageResourceClient&>(client).is_visible_in_viewport())
            visible_in_viewport = true;
    });

    if (!visible_in_viewport) {
        for (auto& frame : m_decoded_frames) {
            if (frame.bitmap)
                frame.bitmap->set_volatile();
        }
        return;
    }

    bool still_has_decoded_image = true;
    for (auto& frame : m_decoded_frames) {
        if (!frame.bitmap) {
            still_has_decoded_image = false;
        } else {
            bool was_purged = false;
            bool bitmap_has_memory = frame.bitmap->set_nonvolatile(was_purged);
            if (!bitmap_has_memory || was_purged)
                still_has_decoded_image = false;
        }
    }
    if (still_has_decoded_image)
        return;

    m_decoded_frames.clear();
    m_has_attempted_decode = false;
}

ImageResourceClient::~ImageResourceClient() = default;

}
