/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/Resource.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {

ResourceLoader& ResourceLoader::the()
{
    static ResourceLoader* s_the;
    if (!s_the)
        s_the = &ResourceLoader::construct().leak_ref();
    return *s_the;
}

ResourceLoader::ResourceLoader()
    : m_protocol_client(Protocol::RequestClient::construct())
    , m_user_agent(default_user_agent)
{
}

void ResourceLoader::load_sync(const LoadRequest& request, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback)
{
    Core::EventLoop loop;

    load(
        request,
        [&](auto data, auto& response_headers, auto status_code) {
            success_callback(data, response_headers, status_code);
            loop.quit(0);
        },
        [&](auto& string, auto status_code) {
            if (error_callback)
                error_callback(string, status_code);
            loop.quit(0);
        });

    loop.exec();
}

static HashMap<LoadRequest, NonnullRefPtr<Resource>> s_resource_cache;

RefPtr<Resource> ResourceLoader::load_resource(Resource::Type type, const LoadRequest& request)
{
    if (!request.is_valid())
        return nullptr;

    bool use_cache = request.url().protocol() != "file";

    if (use_cache) {
        auto it = s_resource_cache.find(request);
        if (it != s_resource_cache.end()) {
            if (it->value->type() != type) {
                dbgln("FIXME: Not using cached resource for {} since there's a type mismatch.", request.url());
            } else {
                dbgln_if(CACHE_DEBUG, "Reusing cached resource for: {}", request.url());
                return it->value;
            }
        }
    }

    auto resource = Resource::create({}, type, request);

    if (use_cache)
        s_resource_cache.set(request, resource);

    load(
        request,
        [=](auto data, auto& headers, auto status_code) {
            const_cast<Resource&>(*resource).did_load({}, data, headers, status_code);
        },
        [=](auto& error, auto status_code) {
            const_cast<Resource&>(*resource).did_fail({}, error, status_code);
        });

    return resource;
}

void ResourceLoader::load(const LoadRequest& request, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback)
{
    auto& url = request.url();

    if (is_port_blocked(url.port())) {
        dbgln("ResourceLoader::load: Error: blocked port {} from URL {}", url.port(), url);
        return;
    }

    if (ContentFilter::the().is_filtered(url)) {
        dbgln("\033[32;1mResourceLoader::load: URL was filtered! {}\033[0m", url);
        error_callback("URL was filtered", {});
        return;
    }

    if (url.protocol() == "about") {
        dbgln("Loading about: URL {}", url);
        deferred_invoke([success_callback = move(success_callback)](auto&) {
            success_callback(String::empty().to_byte_buffer(), {}, {});
        });
        return;
    }

    if (url.protocol() == "data") {
        dbgln("ResourceLoader loading a data URL with mime-type: '{}', base64={}, payload='{}'",
            url.data_mime_type(),
            url.data_payload_is_base64(),
            url.data_payload());

        ByteBuffer data;
        if (url.data_payload_is_base64())
            data = decode_base64(url.data_payload());
        else
            data = url.data_payload().to_byte_buffer();

        deferred_invoke([data = move(data), success_callback = move(success_callback)](auto&) {
            success_callback(data, {}, {});
        });
        return;
    }

    if (url.protocol() == "file") {
        auto f = Core::File::construct();
        f->set_filename(url.path());
        if (!f->open(Core::OpenMode::ReadOnly)) {
            dbgln("ResourceLoader::load: Error: {}", f->error_string());
            if (error_callback)
                error_callback(f->error_string(), {});
            return;
        }

        auto data = f->read_all();
        deferred_invoke([data = move(data), success_callback = move(success_callback)](auto&) {
            success_callback(data, {}, {});
        });
        return;
    }

    if (url.protocol() == "http" || url.protocol() == "https" || url.protocol() == "gemini") {
        HashMap<String, String> headers;
        headers.set("User-Agent", m_user_agent);
        headers.set("Accept-Encoding", "gzip, deflate");

        for (auto& it : request.headers()) {
            headers.set(it.key, it.value);
        }

        auto protocol_request = protocol_client().start_request(request.method(), url, headers, request.body());
        if (!protocol_request) {
            if (error_callback)
                error_callback("Failed to initiate load", {});
            return;
        }
        protocol_request->on_buffered_request_finish = [this, success_callback = move(success_callback), error_callback = move(error_callback), protocol_request](bool success, auto, auto& response_headers, auto status_code, ReadonlyBytes payload) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                if (error_callback)
                    error_callback("HTTP load failed", {});
                return;
            }
            deferred_invoke([protocol_request](auto&) {
                // Clear circular reference of `protocol_request` captured by copy
                const_cast<Protocol::Request&>(*protocol_request).on_buffered_request_finish = nullptr;
            });
            success_callback(payload, response_headers, status_code);
        };
        protocol_request->set_should_buffer_all_input(true);
        protocol_request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey {
            return {};
        };
        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();
        return;
    }

    if (error_callback)
        error_callback(String::formatted("Protocol not implemented: {}", url.protocol()), {});
}

void ResourceLoader::load(const URL& url, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback)
{
    LoadRequest request;
    request.set_url(url);
    load(request, move(success_callback), move(error_callback));
}

bool ResourceLoader::is_port_blocked(int port)
{
    int ports[] { 1, 7, 9, 11, 13, 15, 17, 19, 20, 21, 22, 23, 25, 37, 42,
        43, 53, 77, 79, 87, 95, 101, 102, 103, 104, 109, 110, 111, 113,
        115, 117, 119, 123, 135, 139, 143, 179, 389, 465, 512, 513, 514,
        515, 526, 530, 531, 532, 540, 556, 563, 587, 601, 636, 993, 995,
        2049, 3659, 4045, 6000, 6379, 6665, 6666, 6667, 6668, 6669, 9000 };
    for (auto blocked_port : ports)
        if (port == blocked_port)
            return true;
    return false;
}

void ResourceLoader::clear_cache()
{
    dbgln("Clearing {} items from ResourceLoader cache", s_resource_cache.size());
    s_resource_cache.clear();
}

}
