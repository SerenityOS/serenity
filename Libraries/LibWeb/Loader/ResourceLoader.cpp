/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/JsonObject.h>
#include <AK/SharedBuffer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibProtocol/Client.h>
#include <LibProtocol/Download.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/Resource.h>
#include <LibWeb/Loader/ResourceLoader.h>

//#define CACHE_DEBUG

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

void ResourceLoader::load_sync(const URL& url, Function<void(const ByteBuffer&, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback)
{
    Core::EventLoop loop;

    load(
        url,
        [&](auto& data, auto& response_headers) {
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

    auto it = s_resource_cache.find(request);
    if (it != s_resource_cache.end()) {
        if (it->value->type() != type) {
            dbg() << "FIXME: Not using cached resource for " << request.url() << " since there's a type mismatch.";
        } else {
#ifdef CACHE_DEBUG
            dbg() << "Reusing cached resource for: " << request.url();
#endif
            return it->value;
        }
    }

    auto resource = Resource::create({}, type, request);

    s_resource_cache.set(request, resource);

    load(
        request,
        [=](auto& data, auto& headers) {
            const_cast<Resource&>(*resource).did_load({}, data, headers);
        },
        [=](auto& error) {
            const_cast<Resource&>(*resource).did_fail({}, error);
        });

    return resource;
}

void ResourceLoader::load(const LoadRequest& request, Function<void(const ByteBuffer&, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback)
{
    auto& url = request.url();
    if (is_port_blocked(url.port())) {
        dbg() << "ResourceLoader::load: Error: blocked port " << url.port() << " for URL: " << url;
        return;
    }

    if (url.protocol() == "about") {
        dbg() << "Loading about: URL " << url;
        deferred_invoke([success_callback = move(success_callback)](auto&) {
            success_callback(ByteBuffer::wrap(const_cast<char*>(String::empty().characters()), 1), {});
        });
        return;
    }

    if (url.protocol() == "data") {
        dbg() << "ResourceLoader loading a data URL with mime-type: '" << url.data_mime_type() << "', base64=" << url.data_payload_is_base64() << ", payload='" << url.data_payload() << "'";

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
            dbg() << "ResourceLoader::load: Error: " << f->error_string();
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

        for (auto& it : request.headers()) {
            headers.set(it.key, it.value);
        }

        auto download = protocol_client().start_download(request.method(), url.to_string(), headers, request.body());
        if (!download) {
            if (error_callback)
                error_callback("Failed to initiate load");
            return;
        }
        download->on_finish = [this, success_callback = move(success_callback), error_callback = move(error_callback)](bool success, const ByteBuffer& payload, auto, auto& response_headers, auto status_code) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                if (error_callback)
                    error_callback("HTTP load failed");
                return;
            }
            if (status_code.has_value() && status_code.value() >= 400 && status_code.value() <= 499) {
                if (error_callback)
                    error_callback(String::format("HTTP error (%u)", status_code.value()));
                return;
            }
            success_callback(ByteBuffer::copy(payload.data(), payload.size()), response_headers);
        };
        download->on_certificate_requested = []() -> Protocol::Download::CertificateAndKey {
            return {};
        };
        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();
        return;
    }

    if (error_callback)
        error_callback(String::format("Protocol not implemented: %s", url.protocol().characters()));
}

void ResourceLoader::load(const URL& url, Function<void(const ByteBuffer&, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers)> success_callback, Function<void(const String&)> error_callback)
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
