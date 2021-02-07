/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>
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
    : m_protocol_client(Protocol::Client::construct())
    , m_user_agent("Mozilla/4.0 (SerenityOS; x86) LibWeb+LibJS (Not KHTML, nor Gecko) LibWeb")
{
}

void ResourceLoader::load_sync(const URL& url, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback)
{
    Core::EventLoop loop;

    load(
        url,
        [&](auto data, auto& response_headers) {
            success_callback(data, response_headers);
            loop.quit(0);
        },
        [&](auto& string) {
            if (error_callback)
                error_callback(string);
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
        [=](auto data, auto& headers) {
            const_cast<Resource&>(*resource).did_load({}, data, headers);
        },
        [=](auto& error) {
            const_cast<Resource&>(*resource).did_fail({}, error);
        });

    return resource;
}

void ResourceLoader::load(const LoadRequest& request, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback)
{
    auto& url = request.url();

    if (is_port_blocked(url.port())) {
        dbgln("ResourceLoader::load: Error: blocked port {} from URL {}", url.port(), url);
        return;
    }

    if (ContentFilter::the().is_filtered(url)) {
        dbgln("\033[32;1mResourceLoader::load: URL was filtered! {}\033[0m", url);
        error_callback("URL was filtered");
        return;
    }

    if (url.protocol() == "about") {
        dbgln("Loading about: URL {}", url);
        deferred_invoke([success_callback = move(success_callback)](auto&) {
            success_callback(String::empty().to_byte_buffer(), {});
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
            success_callback(data, {});
        });
        return;
    }

    if (url.protocol() == "file") {
        auto f = Core::File::construct();
        f->set_filename(url.path());
        if (!f->open(Core::IODevice::OpenMode::ReadOnly)) {
            dbgln("ResourceLoader::load: Error: {}", f->error_string());
            if (error_callback)
                error_callback(f->error_string());
            return;
        }

        auto data = f->read_all();
        deferred_invoke([data = move(data), success_callback = move(success_callback)](auto&) {
            success_callback(data, {});
        });
        return;
    }

    if (url.protocol() == "http" || url.protocol() == "https" || url.protocol() == "gemini") {
        HashMap<String, String> headers;
        headers.set("User-Agent", m_user_agent);
        headers.set("Accept-Encoding", "gzip");

        for (auto& it : request.headers()) {
            headers.set(it.key, it.value);
        }

        auto download = protocol_client().start_download(request.method(), url.to_string(), headers, request.body());
        if (!download) {
            if (error_callback)
                error_callback("Failed to initiate load");
            return;
        }
        download->on_buffered_download_finish = [this, success_callback = move(success_callback), error_callback = move(error_callback), download](bool success, auto, auto& response_headers, auto status_code, ReadonlyBytes payload) {
            if (status_code.has_value() && status_code.value() >= 400 && status_code.value() <= 499) {
                if (error_callback)
                    error_callback(String::formatted("HTTP error ({})", status_code.value()));
                return;
            }
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                if (error_callback)
                    error_callback("HTTP load failed");
                return;
            }
            deferred_invoke([download](auto&) {
                // Clear circular reference of `download` captured by copy
                const_cast<Protocol::Download&>(*download).on_buffered_download_finish = nullptr;
            });
            success_callback(payload, response_headers);
        };
        download->set_should_buffer_all_input(true);
        download->on_certificate_requested = []() -> Protocol::Download::CertificateAndKey {
            return {};
        };
        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();
        return;
    }

    if (error_callback)
        error_callback(String::formatted("Protocol not implemented: {}", url.protocol()));
}

void ResourceLoader::load(const URL& url, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback)
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

}
