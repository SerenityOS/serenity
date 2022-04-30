/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ProxyMappings.h>
#include <LibWeb/Loader/Resource.h>
#include <LibWeb/Loader/ResourceLoader.h>

#ifdef __serenity__
#    include <serenity.h>
#endif

namespace Web {

ResourceLoaderConnectorRequest::ResourceLoaderConnectorRequest() = default;

ResourceLoaderConnectorRequest::~ResourceLoaderConnectorRequest() = default;

ResourceLoaderConnector::ResourceLoaderConnector() = default;

ResourceLoaderConnector::~ResourceLoaderConnector() = default;

static RefPtr<ResourceLoader> s_resource_loader;

void ResourceLoader::initialize(RefPtr<ResourceLoaderConnector> connector)
{
    if (connector)
        s_resource_loader = ResourceLoader::try_create(connector.release_nonnull()).release_value_but_fixme_should_propagate_errors();
}

ResourceLoader& ResourceLoader::the()
{
    if (!s_resource_loader) {
        dbgln("Web::ResourceLoader was not initialized");
        VERIFY_NOT_REACHED();
    }
    return *s_resource_loader;
}

ErrorOr<NonnullRefPtr<ResourceLoader>> ResourceLoader::try_create(NonnullRefPtr<ResourceLoaderConnector> connector)
{
    return adopt_nonnull_ref_or_enomem(new (nothrow) ResourceLoader(move(connector)));
}

ResourceLoader::ResourceLoader(NonnullRefPtr<ResourceLoaderConnector> connector)
    : m_connector(move(connector))
    , m_user_agent(default_user_agent)
{
}

void ResourceLoader::prefetch_dns(AK::URL const& url)
{
    m_connector->prefetch_dns(url);
}

void ResourceLoader::preconnect(AK::URL const& url)
{
    m_connector->preconnect(url);
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

static void emit_signpost(String const& message, int id)
{
#ifdef __serenity__
    auto string_id = perf_register_string(message.characters(), message.length());
    perf_event(PERF_EVENT_SIGNPOST, string_id, id);
#else
    (void)message;
    (void)id;
#endif
}

static size_t resource_id = 0;

void ResourceLoader::load(LoadRequest& request, Function<void(ReadonlyBytes, HashMap<String, String, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> status_code)> success_callback, Function<void(String const&, Optional<u32> status_code)> error_callback)
{
    auto& url = request.url();
    request.start_timer();

    auto id = resource_id++;
    auto url_for_logging = sanitized_url_for_logging(url);
    emit_signpost(String::formatted("Starting load: {}", url_for_logging), id);
    dbgln("ResourceLoader: Starting load of: \"{}\"", url_for_logging);

    auto const log_success = [url_for_logging, id](auto const& request) {
        auto load_time_ms = request.load_time().to_milliseconds();
        emit_signpost(String::formatted("Finished load: {}", url_for_logging), id);
        dbgln("ResourceLoader: Finished load of: \"{}\", Duration: {}ms", url_for_logging, load_time_ms);
    };

    auto const log_failure = [url_for_logging, id](auto const& request, auto const error_message) {
        auto load_time_ms = request.load_time().to_milliseconds();
        emit_signpost(String::formatted("Failed load: {}", url_for_logging), id);
        dbgln("ResourceLoader: Failed load of: \"{}\", \033[31;1mError: {}\033[0m, Duration: {}ms", url_for_logging, error_message, load_time_ms);
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

        HashMap<String, String, CaseInsensitiveStringTraits> response_headers;
        response_headers.set("Content-Type", "text/html; charset=UTF-8");

        deferred_invoke([success_callback = move(success_callback), response_headers = move(response_headers)] {
            success_callback(String::empty().to_byte_buffer(), response_headers, {});
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
            if (data_maybe.is_error()) {
                auto error_message = data_maybe.error().string_literal();
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
        auto proxy = ProxyMappings::the().proxy_for_url(url);

        HashMap<String, String> headers;
        headers.set("User-Agent", m_user_agent);
        headers.set("Accept-Encoding", "gzip, deflate");

        for (auto& it : request.headers()) {
            headers.set(it.key, it.value);
        }

        auto protocol_request = m_connector->start_request(request.method(), url, headers, request.body(), proxy);
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
            if (!success || (status_code.has_value() && *status_code >= 400 && *status_code <= 599)) {
                StringBuilder error_builder;
                if (status_code.has_value())
                    error_builder.appendff("Load failed: {}", *status_code);
                else
                    error_builder.append("Load failed");
                log_failure(request, error_builder.string_view());
                if (error_callback)
                    error_callback(error_builder.to_string(), {});
                return;
            }
            log_success(request);
            success_callback(payload, response_headers, status_code);
            deferred_invoke([this, &protocol_request] {
                m_active_requests.remove(protocol_request);
            });
        };
        protocol_request->set_should_buffer_all_input(true);
        protocol_request->on_certificate_requested = []() -> ResourceLoaderConnectorRequest::CertificateAndKey {
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

void ResourceLoader::load(const AK::URL& url, Function<void(ReadonlyBytes, HashMap<String, String, CaseInsensitiveStringTraits> const& response_headers, Optional<u32> status_code)> success_callback, Function<void(String const&, Optional<u32> status_code)> error_callback)
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

void ResourceLoader::evict_from_cache(LoadRequest const& request)
{
    dbgln_if(CACHE_DEBUG, "Removing resource {} from cache", request.url());
    s_resource_cache.remove(request);
}

}
