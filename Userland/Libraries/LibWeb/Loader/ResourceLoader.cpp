/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonObject.h>
#include <LibCore/Directory.h>
#include <LibCore/ElapsedTimer.h>
#include <LibCore/MimeData.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/Loader/ContentFilter.h>
#include <LibWeb/Loader/GeneratedPagesLoader.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ProxyMappings.h>
#include <LibWeb/Loader/Resource.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/Platform/Timer.h>

#ifdef AK_OS_SERENITY
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
    , m_platform(default_platform)
{
}

void ResourceLoader::prefetch_dns(AK::URL const& url)
{
    if (ContentFilter::the().is_filtered(url)) {
        dbgln("ResourceLoader: Refusing to prefetch DNS for '{}': \033[31;1mURL was filtered\033[0m", url);
        return;
    }

    m_connector->prefetch_dns(url);
}

void ResourceLoader::preconnect(AK::URL const& url)
{
    if (ContentFilter::the().is_filtered(url)) {
        dbgln("ResourceLoader: Refusing to pre-connect to '{}': \033[31;1mURL was filtered\033[0m", url);
        return;
    }

    m_connector->preconnect(url);
}

static HashMap<LoadRequest, NonnullRefPtr<Resource>> s_resource_cache;

RefPtr<Resource> ResourceLoader::load_resource(Resource::Type type, LoadRequest& request)
{
    if (!request.is_valid())
        return nullptr;

    bool use_cache = request.url().scheme() != "file";

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
        [=](auto& error, auto status_code, auto, auto) {
            const_cast<Resource&>(*resource).did_fail({}, error, status_code);
        });

    return resource;
}

static DeprecatedString sanitized_url_for_logging(AK::URL const& url)
{
    if (url.scheme() == "data"sv)
        return "[data URL]"sv;
    return url.to_deprecated_string();
}

static void emit_signpost(DeprecatedString const& message, int id)
{
#ifdef AK_OS_SERENITY
    auto string_id = perf_register_string(message.characters(), message.length());
    perf_event(PERF_EVENT_SIGNPOST, string_id, id);
#else
    (void)message;
    (void)id;
#endif
}

static void store_response_cookies(Page& page, AK::URL const& url, DeprecatedString const& cookies)
{
    auto set_cookie_json_value = MUST(JsonValue::from_string(cookies));
    VERIFY(set_cookie_json_value.type() == JsonValue::Type::Array);

    for (auto const& set_cookie_entry : set_cookie_json_value.as_array().values()) {
        VERIFY(set_cookie_entry.type() == JsonValue::Type::String);

        auto cookie = Cookie::parse_cookie(set_cookie_entry.as_string());
        if (!cookie.has_value())
            continue;

        page.client().page_did_set_cookie(url, cookie.value(), Cookie::Source::Http); // FIXME: Determine cookie source correctly
    }
}

static size_t resource_id = 0;

void ResourceLoader::load(LoadRequest& request, SuccessCallback success_callback, ErrorCallback error_callback, Optional<u32> timeout, TimeoutCallback timeout_callback)
{
    auto& url = request.url();
    request.start_timer();

    auto id = resource_id++;
    auto url_for_logging = sanitized_url_for_logging(url);
    emit_signpost(DeprecatedString::formatted("Starting load: {}", url_for_logging), id);
    dbgln("ResourceLoader: Starting load of: \"{}\"", url_for_logging);

    auto const log_success = [url_for_logging, id](auto const& request) {
        auto load_time_ms = request.load_time().to_milliseconds();
        emit_signpost(DeprecatedString::formatted("Finished load: {}", url_for_logging), id);
        dbgln("ResourceLoader: Finished load of: \"{}\", Duration: {}ms", url_for_logging, load_time_ms);
    };

    auto const log_failure = [url_for_logging, id](auto const& request, auto const& error_message) {
        auto load_time_ms = request.load_time().to_milliseconds();
        emit_signpost(DeprecatedString::formatted("Failed load: {}", url_for_logging), id);
        dbgln("ResourceLoader: Failed load of: \"{}\", \033[31;1mError: {}\033[0m, Duration: {}ms", url_for_logging, error_message, load_time_ms);
    };

    if (is_port_blocked(url.port_or_default())) {
        log_failure(request, DeprecatedString::formatted("The port #{} is blocked", url.port_or_default()));
        return;
    }

    if (ContentFilter::the().is_filtered(url)) {
        auto filter_message = "URL was filtered"sv;
        log_failure(request, filter_message);
        error_callback(filter_message, {}, {}, {});
        return;
    }

    if (url.scheme() == "about") {
        dbgln_if(SPAM_DEBUG, "Loading about: URL {}", url);
        log_success(request);

        HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> response_headers;
        response_headers.set("Content-Type", "text/html; charset=UTF-8");

        Platform::EventLoopPlugin::the().deferred_invoke([success_callback = move(success_callback), response_headers = move(response_headers)] {
            success_callback(DeprecatedString::empty().to_byte_buffer(), response_headers, {});
        });
        return;
    }

    if (url.scheme() == "data") {
        auto data_url_or_error = url.process_data_url();
        if (data_url_or_error.is_error()) {
            auto error_message = data_url_or_error.error().string_literal();
            log_failure(request, error_message);
            error_callback(error_message, {}, {}, {});
            return;
        }
        auto data_url = data_url_or_error.release_value();

        dbgln_if(SPAM_DEBUG, "ResourceLoader loading a data URL with mime-type: '{}', payload='{}'",
            data_url.mime_type,
            StringView(data_url.body.bytes()));

        HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> response_headers;
        response_headers.set("Content-Type", data_url.mime_type.to_deprecated_string());

        log_success(request);

        Platform::EventLoopPlugin::the().deferred_invoke([data = move(data_url.body), response_headers = move(response_headers), success_callback = move(success_callback)] {
            success_callback(data, response_headers, {});
        });
        return;
    }

    if (url.scheme() == "file") {
        if (request.page().has_value())
            m_page = request.page().value();

        if (!m_page.has_value()) {
            log_failure(request, "INTERNAL ERROR: No Page for request");
            return;
        }

        FileRequest file_request(url.serialize_path(), [this, success_callback = move(success_callback), error_callback = move(error_callback), log_success, log_failure, request](ErrorOr<i32> file_or_error) {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();

            if (file_or_error.is_error()) {
                log_failure(request, file_or_error.error());
                if (error_callback) {
                    auto status = file_or_error.error().code() == ENOENT ? 404u : 500u;
                    error_callback(DeprecatedString::formatted("{}", file_or_error.error()), status, {}, {});
                }
                return;
            }

            auto const fd = file_or_error.value();

            // When local file is a directory use file directory loader to generate response
            auto maybe_is_valid_directory = Core::Directory::is_valid_directory(fd);
            if (!maybe_is_valid_directory.is_error() && maybe_is_valid_directory.value()) {
                auto maybe_response = load_file_directory_page(request);
                if (maybe_response.is_error()) {
                    log_failure(request, maybe_response.error());
                    if (error_callback)
                        error_callback(DeprecatedString::formatted("{}", maybe_response.error()), 500u, {}, {});
                    return;
                }

                log_success(request);
                HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> response_headers;
                response_headers.set("Content-Type"sv, "text/html"sv);
                success_callback(maybe_response.release_value().bytes(), response_headers, {});
                return;
            }

            // Try to read file normally
            auto maybe_file = Core::File::adopt_fd(fd, Core::File::OpenMode::Read);
            if (maybe_file.is_error()) {
                log_failure(request, maybe_file.error());
                if (error_callback)
                    error_callback(DeprecatedString::formatted("{}", maybe_file.error()), 500u, {}, {});
                return;
            }

            auto file = maybe_file.release_value();
            auto maybe_data = file->read_until_eof();
            if (maybe_data.is_error()) {
                log_failure(request, maybe_data.error());
                if (error_callback)
                    error_callback(DeprecatedString::formatted("{}", maybe_data.error()), 500u, {}, {});
                return;
            }
            auto data = maybe_data.release_value();
            log_success(request);

            // NOTE: For file:// URLs, we have to guess the MIME type, since there's no HTTP header to tell us what this is.
            //       We insert a fake Content-Type header here, so that clients can use it to learn the MIME type.
            HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> response_headers;
            auto mime_type = Core::guess_mime_type_based_on_filename(request.url().serialize_path());
            response_headers.set("Content-Type"sv, mime_type);

            success_callback(data, response_headers, {});
        });

        m_page->client().request_file(move(file_request));

        ++m_pending_loads;
        if (on_load_counter_change)
            on_load_counter_change();

        return;
    }

    if (url.scheme() == "http" || url.scheme() == "https" || url.scheme() == "gemini") {
        auto proxy = ProxyMappings::the().proxy_for_url(url);

        HashMap<DeprecatedString, DeprecatedString> headers;
        headers.set("User-Agent", m_user_agent);
        headers.set("Accept-Encoding", "gzip, deflate, br");

        for (auto& it : request.headers()) {
            headers.set(it.key, it.value);
        }

        auto protocol_request = m_connector->start_request(request.method(), url, headers, request.body(), proxy);
        if (!protocol_request) {
            auto start_request_failure_msg = "Failed to initiate load"sv;
            log_failure(request, start_request_failure_msg);
            if (error_callback)
                error_callback(start_request_failure_msg, {}, {}, {});
            return;
        }

        if (timeout.has_value() && timeout.value() > 0) {
            auto timer = Platform::Timer::create_single_shot(timeout.value(), nullptr);
            timer->on_timeout = [timer, protocol_request, timeout_callback = move(timeout_callback)] {
                protocol_request->stop();
                if (timeout_callback)
                    timeout_callback();
            };
            timer->start();
        }

        m_active_requests.set(*protocol_request);

        protocol_request->on_buffered_request_finish = [this, success_callback = move(success_callback), error_callback = move(error_callback), log_success, log_failure, request, &protocol_request = *protocol_request](bool success, auto, auto& response_headers, auto status_code, ReadonlyBytes payload) mutable {
            --m_pending_loads;
            if (on_load_counter_change)
                on_load_counter_change();

            if (request.page().has_value()) {
                if (auto set_cookie = response_headers.get("Set-Cookie"); set_cookie.has_value())
                    store_response_cookies(request.page().value(), request.url(), *set_cookie);
                if (auto cache_control = response_headers.get("cache-control"); cache_control.has_value()) {
                    if (cache_control.value().contains("no-store"sv)) {
                        s_resource_cache.remove(request);
                    }
                }
            }

            if (!success || (status_code.has_value() && *status_code >= 400 && *status_code <= 599 && (payload.is_empty() || !request.is_main_resource()))) {
                StringBuilder error_builder;
                if (status_code.has_value())
                    error_builder.appendff("Load failed: {}", *status_code);
                else
                    error_builder.append("Load failed"sv);
                log_failure(request, error_builder.string_view());
                if (error_callback)
                    error_callback(error_builder.to_deprecated_string(), status_code, payload, response_headers);
                return;
            }
            log_success(request);
            success_callback(payload, response_headers, status_code);
            Platform::EventLoopPlugin::the().deferred_invoke([this, &protocol_request] {
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

    auto not_implemented_error = DeprecatedString::formatted("Protocol not implemented: {}", url.scheme());
    log_failure(request, not_implemented_error);
    if (error_callback)
        error_callback(not_implemented_error, {}, {}, {});
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
