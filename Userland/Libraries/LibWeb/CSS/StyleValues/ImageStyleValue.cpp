/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022-2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageStyleValue.h"
#include <LibWeb/CSS/ComputedValues.h>
#include <LibWeb/CSS/Serialize.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Painting/PaintContext.h>
#include <LibWeb/Platform/Timer.h>

namespace Web::CSS {

ImageStyleValue::ImageStyleValue(AK::URL const& url)
    : AbstractImageStyleValue(Type::Image)
    , m_url(url)
{
}

void ImageStyleValue::load_any_resources(DOM::Document& document)
{
    if (resource())
        return;

    m_document = &document;
    auto request = LoadRequest::create_for_url_on_page(m_url, document.page());
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Image, request));
}

void ImageStyleValue::resource_did_load()
{
    if (!m_document)
        return;
    // FIXME: Do less than a full repaint if possible?
    if (m_document && m_document->browsing_context())
        m_document->browsing_context()->set_needs_display();

    if (resource()->is_animated() && resource()->frame_count() > 1) {
        m_timer = Platform::Timer::create();
        m_timer->set_interval(resource()->frame_duration(0));
        m_timer->on_timeout = [this] { animate(); };
        m_timer->start();
    }
}

void ImageStyleValue::animate()
{
    m_current_frame_index = (m_current_frame_index + 1) % resource()->frame_count();
    auto current_frame_duration = resource()->frame_duration(m_current_frame_index);

    if (current_frame_duration != m_timer->interval())
        m_timer->restart(current_frame_duration);

    if (m_current_frame_index == resource()->frame_count() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == resource()->loop_count())
            m_timer->stop();
    }

    if (on_animate)
        on_animate();
}

Gfx::Bitmap const* ImageStyleValue::bitmap(size_t frame_index) const
{
    if (!resource())
        return nullptr;
    return resource()->bitmap(frame_index);
}

ErrorOr<String> ImageStyleValue::to_string() const
{
    return serialize_a_url(m_url.to_deprecated_string());
}

bool ImageStyleValue::equals(StyleValue const& other) const
{
    if (type() != other.type())
        return false;
    return m_url == other.as_image().m_url;
}

Optional<CSSPixels> ImageStyleValue::natural_width() const
{
    if (auto* b = bitmap(0); b != nullptr)
        return b->width();
    return {};
}

Optional<CSSPixels> ImageStyleValue::natural_height() const
{
    if (auto* b = bitmap(0); b != nullptr)
        return b->height();
    return {};
}

void ImageStyleValue::paint(PaintContext& context, DevicePixelRect const& dest_rect, CSS::ImageRendering image_rendering) const
{
    if (auto* b = bitmap(m_current_frame_index); b != nullptr) {
        auto scaling_mode = to_gfx_scaling_mode(image_rendering, bitmap(0)->rect(), dest_rect.to_type<int>());
        context.painter().draw_scaled_bitmap(dest_rect.to_type<int>(), *b, bitmap(0)->rect(), 1.f, scaling_mode);
    }
}

}
