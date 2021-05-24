/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Function.h>
#include <LibCore/MimeData.h>
#include <LibWeb/Fetch/Response.h>
#include <LibWeb/HTML/HTMLImageElement.h>

namespace Web::Fetch {

NonnullRefPtr<Response> Response::create(Badge<ResourceLoader>, Type type)
{
    RefPtr<Response> response;
    if (type == Type::Image)
        response = adopt_ref(*new ImageResource());
    else
        response = adopt_ref(*new Response());
    response->m_type = type;
    return response.release_nonnull();
}

NonnullRefPtr<Response> Response::create_network_error(Badge<ResourceLoader>)
{
    auto response = adopt_ref(*new Response());
    response->m_new_type = NewType::Error;
    response->m_status = 0;
    return response;
}

Response::Response()
{
}

Response::~Response()
{
    dbgln("Response destroyed");
}

void Response::for_each_client(Function<void(ResourceClient&)> callback)
{
    Vector<WeakPtr<ResourceClient>, 16> clients_copy;
    clients_copy.ensure_capacity(m_clients.size());
    for (auto* client : m_clients)
        clients_copy.append(client->make_weak_ptr());
    for (auto client : clients_copy) {
        if (client)
            callback(*client);
    }
}

void Response::did_load(Badge<ResourceLoader>, ReadonlyBytes data, const HashMap<String, String, CaseInsensitiveStringTraits>& headers, Optional<u32> status_code)
{
    VERIFY(!m_loaded);
    // FIXME: Handle OOM failure.
    m_encoded_data = ByteBuffer::copy(data).release_value_but_fixme_should_propagate_errors();
    m_response_headers = headers;
    m_status_code = move(status_code);
    m_loaded = true;

    auto content_type = headers.get("Content-Type");

    if (content_type.has_value()) {
        dbgln_if(RESOURCE_DEBUG, "Content-Type header: '{}'", content_type.value());
        m_mime_type = mime_type_from_content_type(content_type.value());
        // FIXME: "The Quite OK Image Format" doesn't have an official mime type yet,
        //        and servers like nginx will send a generic octet-stream mime type instead.
        //        Let's use image/x-qoi for now, which is also what our Core::MimeData uses & would guess.
        if (m_mime_type == "application/octet-stream" && url().path().ends_with(".qoi"))
            m_mime_type = "image/x-qoi";
    } else if (url().protocol() == "data" && !url().data_mime_type().is_empty()) {
        dbgln_if(RESOURCE_DEBUG, "This is a data URL with mime-type _{}_", url().data_mime_type());
        m_mime_type = url().data_mime_type();
    } else {
        auto content_type_options = headers.get("X-Content-Type-Options");
        if (content_type_options.value_or("").equals_ignoring_case("nosniff")) {
            m_mime_type = "text/plain";
        } else {
            m_mime_type = Core::guess_mime_type_based_on_filename(url().path());
        }
    }

    m_body = data;
    for (auto& header : headers)
        m_header_list.append(header.key, header.value);
    if (!status_code.has_value())
        m_status = 0;
    else
        m_status = status_code.value();

// FIXME: where does this go, before m_body is set, or after.
//    m_encoding = {};
//    if (content_type.has_value()) {
//        auto encoding = encoding_from_content_type(content_type.value());
//        if (encoding.has_value()) {
//            dbgln_if(RESOURCE_DEBUG, "Set encoding '{}' from Content-Type", encoding.has_value());
//            m_encoding = encoding.value();
//        }
//    }

    for_each_client([](auto& client) {
        client.resource_did_load();
    });
}

void Response::did_fail(Badge<ResourceLoader>, const String& error, Optional<u32> status_code)
{
    m_status_message = error;
    if (status_code.has_value())
        m_status = 0;
    else
        m_status = status_code.value();
    m_new_type = NewType::Error;

    for_each_client([](auto& client) {
        client.resource_did_fail();
    });
}

void Response::register_client(Badge<ResourceClient>, ResourceClient& client)
{
    VERIFY(!m_clients.contains(&client));
    m_clients.set(&client);
}

void Response::unregister_client(Badge<ResourceClient>, ResourceClient& client)
{
    VERIFY(m_clients.contains(&client));
    m_clients.remove(&client);
}

// https://fetch.spec.whatwg.org/#should-response-to-request-be-blocked-due-to-nosniff? (Note: the '?' on the end is required)
// false is allowed, true is blocked.
bool Response::should_be_blocked_due_to_nosniff(const LoadRequest& request) const
{
    if (!m_header_list.determine_nosniff())
        return false;

    auto mime_type = m_header_list.extract_mime_type();

    if (request.destination_is_script_like() && (!mime_type.has_value() || !mime_type.value().is_javascript_mime_type()))
        return true;

    if (request.destination() == LoadRequest::Destination::Style && (!mime_type.has_value() || mime_type.value().essence() != "text/css"))
        return true;

    return false;
}

// https://fetch.spec.whatwg.org/#should-response-to-request-be-blocked-due-to-mime-type? (Note: the '?' on the end is required)
// false is allowed, true is blocked.
bool Response::should_be_blocked_due_to_mime_type(const LoadRequest& request) const
{
    auto mime_type = m_header_list.extract_mime_type();
    if (!mime_type.has_value())
        return false;

    auto essence = mime_type->essence();
    if (request.destination_is_script_like() && (essence.starts_with("audio/") || essence.starts_with("image/") || essence.starts_with("video/") || essence == "text/csv"))
        return true;

    return false;
}

// https://fetch.spec.whatwg.org/#concept-response-clone
NonnullRefPtr<Response> Response::clone() const
{
    // FIXME: Handle the case where this response is a filtered response.
    VERIFY(!is_filtered_response());

    auto clone = adopt_ref(*new Response());
    clone->m_type = m_type; // FIXME: Remove
    clone->m_new_type = m_new_type;
    clone->m_aborted = m_aborted;
    clone->m_url_list = m_url_list;
    clone->m_status_message = m_status_message.isolated_copy();
    clone->m_header_list = m_header_list;
    clone->m_cache_state = m_cache_state;
    clone->m_cors_exposed_header_name_list = m_cors_exposed_header_name_list;
    clone->m_range_requested = m_range_requested;
    clone->m_timing_allow_passed = m_timing_allow_passed;
    clone->m_timing_info = m_timing_info;
    clone->m_clients = m_clients; // FIXME: Probably don't need this.

    // FIXME: If body is non-null, clone it. https://fetch.spec.whatwg.org/#concept-body-clone
    //        (Body is currently not a body object from the Stream standard)
    clone->m_body = m_body; // FIXME: Not actual copy!!

    return clone;
}

// https://fetch.spec.whatwg.org/#concept-main-fetch Step 14.2
NonnullRefPtr<Response> Response::to_filtered_response(LoadRequest::ResponseTainting response_tainting) const
{
    auto filtered_response = clone();
    filtered_response->m_internal_response = this;

    switch (response_tainting) {
    case LoadRequest::ResponseTainting::Basic:
        // https://fetch.spec.whatwg.org/#concept-filtered-response-basic
        filtered_response->m_new_type = NewType::Basic;

        filtered_response->m_header_list.list().remove_all_matching([](auto& header) {
            return HTTP::is_forbidden_response_header_name(header.name);
        });
        break;
    case LoadRequest::ResponseTainting::Cors:
        // https://fetch.spec.whatwg.org/#concept-filtered-response-cors
        filtered_response->m_new_type = NewType::Cors;

        filtered_response->m_header_list.list().remove_all_matching([](auto& header) {
           return !HTTP::is_cors_safelisted_response_header_name(header.name);
        });
        break;
    case LoadRequest::ResponseTainting::Opaque:
        // https://fetch.spec.whatwg.org/#concept-filtered-response-opaque
        filtered_response->m_new_type = NewType::Opaque;
        filtered_response->m_url_list.clear();
        filtered_response->m_status = 0;
        filtered_response->m_status_message = String::empty();
        filtered_response->m_header_list.clear();
        // FIXME: Set body to null
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    return filtered_response;
}

// https://fetch.spec.whatwg.org/#concept-response-location-url
Optional<URL> Response::location_url(String const& request_fragment) const
{
    if (!has_redirect_status())
        return {};

    // FIXME: Use the "extracting header list values" algorithm
    auto location_value = m_header_list.get("Location");
    URL location;

    if (!location_value.is_null())
        location = url().value().complete_url(location_value);

    if (location.is_valid() && location.fragment().is_null())
        location.set_fragment(request_fragment);

    return location;
}

void ResourceClient::set_resource(Response* resource)
{
    if (m_resource)
        m_resource->unregister_client({}, *this);
    m_resource = resource;
    if (m_resource) {
//        VERIFY(resource->type() == client_type());

        m_resource->register_client({}, *this);

        // Make sure that reused resources also have their load callback fired.
        if (!resource->is_network_error())
            resource_did_load();

        // Make sure that reused resources also have their fail callback fired.
        else
            resource_did_fail();
    }
}

ResourceClient::~ResourceClient()
{
    if (m_resource)
        m_resource->unregister_client({}, *this);
}

}
