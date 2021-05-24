/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibCore/Timer.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Fetch/ImageLoader.h>
#include <LibWeb/Fetch/ResourceLoader.h>

namespace Web::Fetch {

ImageLoader::ImageLoader(DOM::Element& owner_element)
    : m_owner_element(owner_element)
    , m_timer(Core::Timer::construct())
{
}

void ImageLoader::load(const AK::URL& url)
{
    m_redirects_count = 0;
    load_without_resetting_redirect_counter(url);
}

void ImageLoader::load_without_resetting_redirect_counter(AK::URL const& url)
{
    m_loading_state = LoadingState::Loading;

//    auto request = LoadRequest::create_for_url_on_page(url, m_owner_element.document().page());
//    set_resource(ResourceLoader::the().load_resource(Response::Type::Image, request));

    // Some of this is from https://html.spec.whatwg.org/multipage/images.html#update-the-image-data
    auto request = LoadRequest::create_a_potential_cors_request(url, m_owner_element.document().page(), LoadRequest::Destination::Image /* FIXME: and the crossorigin attribute of the img element */);
    ResourceLoader::the().fetch(request, {}, {}, {}, {}, [this](auto& response) {
        set_resource(new ImageResource(response));
        dbgln("done?");
//        if (!response.is_network_error())
//            resource_did_load();
//        else
//            resource_did_fail();
    });
}

void ImageLoader::set_visible_in_viewport(bool visible_in_viewport) const
{
    if (m_visible_in_viewport == visible_in_viewport)
        return;
    m_visible_in_viewport = visible_in_viewport;

    // FIXME: Don't update volatility every time. If we're here, we're probably scanning through
    //        the whole document, updating "is visible in viewport" flags, and this could lead
    //        to the same bitmap being marked volatile back and forth unnecessarily.
    if (resource())
        const_cast<ImageResource*>(resource())->update_volatility();
}

void ImageLoader::resource_did_load()
{
    VERIFY(resource());
    dbgln("resource ptr: {:p}", resource());

    // For 3xx (Redirection) responses, the Location value refers to the preferred target resource for automatically redirecting the request.
    auto status_code = resource()->status_code();
    if (status_code.has_value() && *status_code >= 300 && *status_code <= 399) {
        auto location = resource()->response_headers().get("Location");
        if (location.has_value()) {
            if (m_redirects_count > maximum_redirects_allowed) {
                m_redirects_count = 0;
                m_loading_state = LoadingState::Failed;
                if (on_fail)
                    on_fail();
                return;
            }
            m_redirects_count++;
            load_without_resetting_redirect_counter(resource()->url().complete_url(location.value()));
            return;
        }
    }
    m_redirects_count = 0;

    if (!resource()->mime_type().starts_with("image/")) {
        m_loading_state = LoadingState::Failed;
        if (on_fail)
            on_fail();
        return;
    }
    // FIXME
//    auto resource_mime_type =
//    if (!resource()->extract_mime_type().starts_with("image/")) {
//        m_loading_state = LoadingState::Failed;
//        if (on_fail)
//            on_fail();
//        return;
//    }

    m_loading_state = LoadingState::Loaded;

    //if constexpr (IMAGE_LOADER_DEBUG) {
        if (!resource()->has_encoded_data()) {
            dbgln("ImageLoader: Resource did load, no encoded data. ");
        } else {
            dbgln("ImageLoader: Resource did load, has encoded data. ");
        }
    //}

    if (resource()->is_animated() && resource()->frame_count() > 1) {
        dbgln("resource ptr: {:p} frame_count={} url={}", resource(), resource()->frame_count(), resource()->url().value());
        m_timer->set_interval(resource()->frame_duration(0));
        m_timer->on_timeout = [this] { animate(); };
        m_timer->start();
    }

    if (on_load)
        on_load();
}

void ImageLoader::animate()
{
    if (!m_visible_in_viewport)
        return;

    m_current_frame_index = (m_current_frame_index + 1) % resource()->frame_count();
    auto current_frame_duration = resource()->frame_duration(m_current_frame_index);

    if (current_frame_duration != m_timer->interval()) {
        m_timer->restart(current_frame_duration);
    }

    if (m_current_frame_index == resource()->frame_count() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == resource()->loop_count()) {
            m_timer->stop();
        }
    }

    if (on_animate)
        on_animate();
}

void ImageLoader::resource_did_fail()
{
    // FIXME: Don't blindly take value.
    dbgln("ImageLoader: Resource did fail. URL: {}", resource()->url().value());
    m_loading_state = LoadingState::Failed;
    if (on_fail)
        on_fail();
}

bool ImageLoader::has_image() const
{
    if (!resource())
        return false;
    return bitmap(0);
}

unsigned ImageLoader::width() const
{
    if (!resource())
        return 0;
    return bitmap(0) ? bitmap(0)->width() : 0;
}

unsigned ImageLoader::height() const
{
    if (!resource())
        return 0;
    return bitmap(0) ? bitmap(0)->height() : 0;
}

const Gfx::Bitmap* ImageLoader::bitmap(size_t frame_index) const
{
    if (!resource())
        return nullptr;
    return resource()->bitmap(frame_index);
}

}
