/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Function.h>
#include <LibWeb/DOM/HTMLImageElement.h>
#include <LibWeb/Loader/Resource.h>

namespace Web {

NonnullRefPtr<Resource> Resource::create(Badge<ResourceLoader>, Type type, const LoadRequest& request)
{
    if (type == Type::Image)
        return adopt(*new ImageResource(request));
    return adopt(*new Resource(type, request));
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

String encoding_from_content_type(const String& content_type)
{
    auto offset = content_type.index_of("charset=");
    if (offset.has_value())
        return content_type.substring(offset.value() + 8, content_type.length() - offset.value() - 8).to_lowercase();

    return "utf-8";
}

String mime_type_from_content_type(const String& content_type)
{
    auto offset = content_type.index_of(";");
    if (offset.has_value())
        return content_type.substring(0, offset.value()).to_lowercase();

    return content_type;
}

static String guess_mime_type_based_on_filename(const URL& url)
{
    if (url.path().ends_with(".png"))
        return "image/png";
    if (url.path().ends_with(".gif"))
        return "image/gif";
    if (url.path().ends_with(".bmp"))
        return "image/bmp";
    if (url.path().ends_with(".md"))
        return "text/markdown";
    if (url.path().ends_with(".html") || url.path().ends_with(".htm"))
        return "text/html";
    if (url.path().ends_with("/"))
        return "text/html";
    return "text/plain";
}

void Resource::did_load(Badge<ResourceLoader>, const ByteBuffer& data, const HashMap<String, String, CaseInsensitiveStringTraits>& headers)
{
    ASSERT(!m_loaded);
    m_encoded_data = data;
    m_response_headers = headers;
    m_loaded = true;

    auto content_type = headers.get("Content-Type");
    if (content_type.has_value()) {
        dbg() << "Content-Type header: _" << content_type.value() << "_";
        m_encoding = encoding_from_content_type(content_type.value());
        m_mime_type = mime_type_from_content_type(content_type.value());
    } else if (url().protocol() == "data" && !url().data_mime_type().is_empty()) {
        dbg() << "This is a data URL with mime-type _" << url().data_mime_type() << "_";
        m_encoding = "utf-8"; // FIXME: This doesn't seem nice.
        m_mime_type = url().data_mime_type();
    } else {
        dbg() << "No Content-Type header to go on! Guessing based on filename...";
        m_encoding = "utf-8"; // FIXME: This doesn't seem nice.
        m_mime_type = guess_mime_type_based_on_filename(url());
    }

    for_each_client([](auto& client) {
        client.resource_did_load();
    });
}

void Resource::did_fail(Badge<ResourceLoader>, const String& error)
{
    m_error = error;
    m_failed = true;

    for_each_client([](auto& client) {
        client.resource_did_fail();
    });
}

void Resource::register_client(Badge<ResourceClient>, ResourceClient& client)
{
    ASSERT(!m_clients.contains(&client));
    m_clients.set(&client);
}

void Resource::unregister_client(Badge<ResourceClient>, ResourceClient& client)
{
    ASSERT(m_clients.contains(&client));
    m_clients.remove(&client);
}

void ResourceClient::set_resource(Resource* resource)
{
    if (m_resource)
        m_resource->unregister_client({}, *this);
    m_resource = resource;
    if (m_resource) {
        ASSERT(resource->type() == client_type());

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
