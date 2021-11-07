/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <LibCore/ElapsedTimer.h>
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

void ResourceLoader::load_sync(LoadRequest& request, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback)
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

void ResourceLoader::prefetch_dns(AK::URL const& url)
{
    m_protocol_client->ensure_connection(url, RequestServer::CacheLevel::ResolveOnly);
}

void ResourceLoader::preconnect(AK::URL const& url)
{
    m_protocol_client->ensure_connection(url, RequestServer::CacheLevel::CreateConnection);
}

static HashMap<LoadRequest, NonnullRefPtr<Resource>> s_resource_cache;

RefPtr<Resource> ResourceLoader::load_resource(Resource::Type type, LoadRequest& request)
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

static String sanitized_url_for_logging(AK::URL const& url)
{
    if (url.protocol() == "data"sv)
        return String::formatted("[data URL, mime-type={}, size={}]", url.data_mime_type(), url.data_payload().length());
    return url.to_string();
}

void ResourceLoader::load(LoadRequest& request, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback)
{
    auto& url = request.url();
    request.start_timer();
    dbgln("ResourceLoader: Starting load of: \"{}\"", sanitized_url_for_logging(url));

    const auto log_success = [](const auto& request) {
        auto& url = request.url();
        auto load_time_ms = request.load_time().to_milliseconds();
        dbgln("ResourceLoader: Finished load of: \"{}\", Duration: {}ms", sanitized_url_for_logging(url), load_time_ms);
    };

    const auto log_failure = [](const auto& request, const auto error_message) {
        auto& url = request.url();
        auto load_time_ms = request.load_time().to_milliseconds();
        dbgln("ResourceLoader: Failed load of: \"{}\", \033[32;1mError: {}\033[0m, Duration: {}ms", sanitized_url_for_logging(url), error_message, load_time_ms);
    };

    if (is_port_blocked(url.port_or_default())) {
        log_failure(request, String::formatted("The port #{} is blocked", url.port_or_default()));
        return;
    }

    if (ContentFilter::the().is_filtered(url)) {
        auto filter_message = "URL was filtered"sv;
        log_failure(request, filter_message);
        error_callback(filter_message, {});
        return;
    }

    if (url.protocol() == "about") {
        dbgln_if(SPAM_DEBUG, "Loading about: URL {}", url);
        log_success(request);
        deferred_invoke([success_callback = move(success_callback)] {
            success_callback(String::empty().to_byte_buffer(), {}, {});
        });
        return;
    }

    if (url.protocol() == "data") {
        dbgln_if(SPAM_DEBUG, "ResourceLoader loading a data URL with mime-type: '{}', base64={}, payload='{}'",
            url.data_mime_type(),
            url.data_payload_is_base64(),
            url.data_payload());

        ByteBuffer data;
        if (url.data_payload_is_base64()) {
            auto data_maybe = decode_base64(url.data_payload());
            if (!data_maybe.has_value()) {
                auto error_message = "Base64 data contains an invalid character"sv;
                log_failure(request, error_message);
                error_callback(error_message, {});
                return;
            }
            data = data_maybe.value();
        } else {
            data = url.data_payload().to_byte_buffer();
        }

        log_success(request);
        deferred_invoke([data = move(data), success_callback = move(success_callback)] {
            success_callback(data, {}, {});
        });
        return;
    }

    if (url.protocol() == "file") {
        auto file_result = Core::File::open(url.path(), Core::OpenMode::ReadOnly);
        if (file_result.is_error()) {
            auto& error = file_result.error();
            log_failure(request, error);
            if (error_callback)
                error_callback(String::formatted("{}", error), error.code());
            return;
        }

        auto file = file_result.release_value();
        auto data = file->read_all();
        log_success(request);
        deferred_invoke([data = move(data), success_callback = move(success_callback)] {
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
            auto start_request_failure_msg = "Failed to initiate load"sv;
            log_failure(request, start_request_failure_msg);
            if (error_callback)
                error_callback(start_request_failure_msg, {});
            return;
        }
        m_active_requests.set(*protocol_request);
        protocol_request->on_buffered_request_finish = [this, success_callback = move(success_callback), error_callback = move(error_callback), log_success, log_failure, request, &protocol_request = *protocol_request](bool success, auto, auto& response_headers, auto status_code, ReadonlyBytes payload) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();
            if (!success) {
                auto http_load_failure_msg = "HTTP load failed"sv;
                log_failure(request, http_load_failure_msg);
                if (error_callback)
                    error_callback(http_load_failure_msg, {});
                return;
            }
            log_success(request);
            success_callback(payload, response_headers, status_code);
            deferred_invoke([this, &protocol_request] {
                m_active_requests.remove(protocol_request);
            });
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

    auto not_implemented_error = String::formatted("Protocol not implemented: {}", url.protocol());
    log_failure(request, not_implemented_error);
    if (error_callback)
        error_callback(not_implemented_error, {});
}

void ResourceLoader::load(const AK::URL& url, Function<void(ReadonlyBytes, const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> status_code)> success_callback, Function<void(const String&, Optional<u32> status_code)> error_callback)
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
    dbgln_if(CACHE_DEBUG, "Clearing {} items from ResourceLoader cache", s_resource_cache.size());
    s_resource_cache.clear();
}

}
