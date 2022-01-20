/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/Function.h>
#include <LibCore/MimeData.h>
#include <LibWeb/HTML/HTMLImageElement.h>
#include <LibWeb/Loader/Resource.h>

namespace Web {

NonnullRefPtr<Resource> Resource::create(Badge<ResourceLoader>, Type type, const LoadRequest& request)
{
    if (type == Type::Image)
        return adopt_ref(*new ImageResource(request));
    return adopt_ref(*new Resource(type, request));
}

Resource::Resource(Type type, const LoadRequest& request)
    : m_request(request)
    , m_type(type)
{
}

Resource::~Resource()
{
}

void Resource::for_each_client(Function<void(ResourceClient&)> callback)
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

void Resource::did_load(Badge<ResourceLoader>, ReadonlyBytes data, const HashMap<String, String, CaseInsensitiveStringTraits>& headers, Optional<u32> status_code)
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

void Resource::did_fail(Badge<ResourceLoader>, const String& error, Optional<u32> status_code)
{
    m_error = error;
    m_status_code = move(status_code);
    m_failed = true;

    for_each_client([](auto& client) {
        client.resource_did_fail();
    });
}

void Resource::register_client(Badge<ResourceClient>, ResourceClient& client)
{
    VERIFY(!m_clients.contains(&client));
    m_clients.set(&client);
}

void Resource::unregister_client(Badge<ResourceClient>, ResourceClient& client)
{
    VERIFY(m_clients.contains(&client));
    m_clients.remove(&client);
}

void ResourceClient::set_resource(Resource* resource)
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
