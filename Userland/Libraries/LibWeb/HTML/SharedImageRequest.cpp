/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashTable.h>
#include <LibGfx/Bitmap.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>
#include <LibWeb/Fetch/Infrastructure/FetchController.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/HTML/AnimatedBitmapDecodedImageData.h>
#include <LibWeb/HTML/DecodedImageData.h>
#include <LibWeb/HTML/SharedImageRequest.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>
#include <LibWeb/SVG/SVGDecodedImageData.h>

namespace Web::HTML {

static HashMap<AK::URL, SharedImageRequest*>& shared_image_requests()
{
    static HashMap<AK::URL, SharedImageRequest*> requests;
    return requests;
}

JS::NonnullGCPtr<SharedImageRequest> SharedImageRequest::get_or_create(JS::Realm& realm, Page& page, AK::URL const& url)
{
    if (auto it = shared_image_requests().find(url); it != shared_image_requests().end())
        return *it->value;
    auto request = realm.heap().allocate<SharedImageRequest>(realm, page, url);
    shared_image_requests().set(url, request);
    return request;
}

SharedImageRequest::SharedImageRequest(Page& page, AK::URL url)
    : m_page(page)
    , m_url(move(url))
{
}

SharedImageRequest::~SharedImageRequest()
{
    shared_image_requests().remove(m_url);
}

void SharedImageRequest::visit_edges(JS::Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_fetch_controller);
    for (auto& callback : m_callbacks) {
        visitor.visit(callback.on_finish);
        visitor.visit(callback.on_fail);
    }
}

RefPtr<DecodedImageData const> SharedImageRequest::image_data() const
{
    return m_image_data;
}

JS::GCPtr<Fetch::Infrastructure::FetchController> SharedImageRequest::fetch_controller()
{
    return m_fetch_controller.ptr();
}

void SharedImageRequest::set_fetch_controller(JS::GCPtr<Fetch::Infrastructure::FetchController> fetch_controller)
{
    m_fetch_controller = move(fetch_controller);
}

void SharedImageRequest::fetch_image(JS::Realm& realm, JS::NonnullGCPtr<Fetch::Infrastructure::Request> request)
{
    Fetch::Infrastructure::FetchAlgorithms::Input fetch_algorithms_input {};
    fetch_algorithms_input.process_response = [this, &realm, request](JS::NonnullGCPtr<Fetch::Infrastructure::Response> response) {
        // FIXME: If the response is CORS cross-origin, we must use its internal response to query any of its data. See:
        //        https://github.com/whatwg/html/issues/9355
        response = response->unsafe_response();

        auto process_body = [this, request, response](ByteBuffer data) {
            auto extracted_mime_type = response->header_list()->extract_mime_type().release_value_but_fixme_should_propagate_errors();
            auto mime_type = extracted_mime_type.has_value() ? extracted_mime_type.value().essence().bytes_as_string_view() : StringView {};
            handle_successful_fetch(request->url(), mime_type, move(data));
        };
        auto process_body_error = [this](auto) {
            handle_failed_fetch();
        };

        if (response->body())
            response->body()->fully_read(realm, move(process_body), move(process_body_error), JS::NonnullGCPtr { realm.global_object() }).release_value_but_fixme_should_propagate_errors();
        else
            handle_failed_fetch();
    };

    m_state = State::Fetching;

    auto fetch_controller = Fetch::Fetching::fetch(
        realm,
        request,
        Fetch::Infrastructure::FetchAlgorithms::create(realm.vm(), move(fetch_algorithms_input)))
                                .release_value_but_fixme_should_propagate_errors();

    set_fetch_controller(fetch_controller);
}

void SharedImageRequest::add_callbacks(Function<void()> on_finish, Function<void()> on_fail)
{
    if (m_state == State::Finished) {
        if (on_finish)
            on_finish();
        return;
    }

    if (m_state == State::Failed) {
        if (on_fail)
            on_fail();
        return;
    }

    Callbacks callbacks;
    if (on_finish)
        callbacks.on_finish = JS::create_heap_function(vm().heap(), move(on_finish));
    if (on_fail)
        callbacks.on_fail = JS::create_heap_function(vm().heap(), move(on_fail));

    m_callbacks.append(move(callbacks));
}

void SharedImageRequest::handle_successful_fetch(AK::URL const& url_string, StringView mime_type, ByteBuffer data)
{
    // AD-HOC: At this point, things gets very ad-hoc.
    // FIXME: Bring this closer to spec.

    bool const is_svg_image = mime_type == "image/svg+xml"sv || url_string.basename().ends_with(".svg"sv);

    RefPtr<DecodedImageData> image_data;

    auto handle_failed_decode = [&] {
        m_state = State::Failed;
        for (auto& callback : m_callbacks) {
            if (callback.on_fail)
                callback.on_fail->function()();
        }
    };

    if (is_svg_image) {
        auto result = SVG::SVGDecodedImageData::create(m_page, url_string, data);
        if (result.is_error())
            return handle_failed_decode();

        image_data = result.release_value();
    } else {
        auto result = Web::Platform::ImageCodecPlugin::the().decode_image(data.bytes());
        if (!result.has_value())
            return handle_failed_decode();

        Vector<AnimatedBitmapDecodedImageData::Frame> frames;
        for (auto& frame : result.value().frames) {
            frames.append(AnimatedBitmapDecodedImageData::Frame {
                .bitmap = frame.bitmap,
                .duration = static_cast<int>(frame.duration),
            });
        }
        image_data = AnimatedBitmapDecodedImageData::create(move(frames), result.value().loop_count, result.value().is_animated).release_value_but_fixme_should_propagate_errors();
    }

    m_image_data = move(image_data);

    m_state = State::Finished;

    for (auto& callback : m_callbacks) {
        if (callback.on_finish)
            callback.on_finish->function()();
    }
    m_callbacks.clear();
}

void SharedImageRequest::handle_failed_fetch()
{
    m_state = State::Failed;
    for (auto& callback : m_callbacks) {
        if (callback.on_fail)
            callback.on_fail->function()();
    }
    m_callbacks.clear();
}

bool SharedImageRequest::needs_fetching() const
{
    return m_state == State::New;
}

bool SharedImageRequest::is_fetching() const
{
    return m_state == State::Fetching;
}

}
