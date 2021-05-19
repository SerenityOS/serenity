/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Function.h>
#include <LibCore/MimeData.h>
#include <LibWeb/Fetch/Response.h>
#include <LibWeb/HTML/HTMLImageElement.h>

namespace Web::Fetch {

NonnullRefPtr<Response> Response::create(Badge<ResourceLoader>, Type type, const LoadRequest& request /* FIXME: REMOVE */)
{
    if (type == Type::Image)
        return adopt_ref(*new ImageResource(request));
    return adopt_ref(*new Response(type, request));
}

NonnullRefPtr<Response> Response::create_network_error(Badge<ResourceLoader>, const LoadRequest& request /* FIXME: REMOVE */)
{
    auto response = adopt_ref(*new Response(Type::Generic, request));
    response->m_new_type = NewType::Error;
    response->m_status = 0;
    return response;
}

Response::Response(Type type, const LoadRequest& /* FIXME: REMOVE */)
    : m_type(type)
{
}

Response::~Response()
{
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

static Optional<String> encoding_from_content_type(const String& content_type)
{
    auto offset = content_type.find("charset="sv);
    if (offset.has_value()) {
        auto encoding = content_type.substring(offset.value() + 8, content_type.length() - offset.value() - 8).to_lowercase();
        if (encoding.length() >= 2 && encoding.starts_with('"') && encoding.ends_with('"'))
            return encoding.substring(1, encoding.length() - 2);
        if (encoding.length() >= 2 && encoding.starts_with('\'') && encoding.ends_with('\''))
            return encoding.substring(1, encoding.length() - 2);
        return encoding;
    }

    return {};
}

static String mime_type_from_content_type(const String& content_type)
{
    auto offset = content_type.find(';');
    if (offset.has_value())
        return content_type.substring(0, offset.value()).to_lowercase();

    return content_type;
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

    m_encoding = {};
    if (content_type.has_value()) {
        auto encoding = encoding_from_content_type(content_type.value());
        if (encoding.has_value()) {
            dbgln_if(RESOURCE_DEBUG, "Set encoding '{}' from Content-Type", encoding.has_value());
            m_encoding = encoding.value();
        }
    }

    for_each_client([](auto& client) {
        client.resource_did_load();
    });
}

void Response::did_fail(Badge<ResourceLoader>, const String& error, Optional<u32> status_code)
{
    m_error = error;
    m_status_code = move(status_code);
    m_failed = true;

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

NonnullRefPtr<Response> Response::to_filtered_response() const
{

}

void ResourceClient::set_resource(Response* resource)
{
    if (m_resource)
        m_resource->unregister_client({}, *this);
    m_resource = resource;
    if (m_resource) {
        VERIFY(resource->type() == client_type());

        m_resource->register_client({}, *this);

        // Make sure that reused resources also have their load callback fired.
        if (resource->is_loaded())
            resource_did_load();

        // Make sure that reused resources also have their fail callback fired.
        if (resource->is_failed())
            resource_did_fail();
    }
}

ResourceClient::~ResourceClient()
{
    if (m_resource)
        m_resource->unregister_client({}, *this);
}

}
