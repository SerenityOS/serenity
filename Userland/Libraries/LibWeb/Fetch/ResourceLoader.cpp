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
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Fetch/ContentFilter.h>
#include <LibWeb/Fetch/LoadRequest.h>
#include <LibWeb/Fetch/ResourceLoader.h>
#include <LibWeb/Fetch/Response.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/DOM/Document.h>

namespace Web::Fetch {

ResourceLoader& ResourceLoader::the()
{
    static RefPtr<ResourceLoader> s_the;
    if (!s_the)
        s_the = ResourceLoader::try_create().release_value_but_fixme_should_propagate_errors();
    return *s_the;
}

ErrorOr<NonnullRefPtr<ResourceLoader>> ResourceLoader::try_create()
{

    auto protocol_client = TRY(Protocol::RequestClient::try_create());
    return adopt_nonnull_ref_or_enomem(new (nothrow) ResourceLoader(move(protocol_client)));
}

ResourceLoader::ResourceLoader(NonnullRefPtr<Protocol::RequestClient> protocol_client)
    : m_protocol_client(move(protocol_client))
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

static HashMap<NonnullRefPtr<LoadRequest>, NonnullRefPtr<Response>> s_resource_cache;

RefPtr<Response> ResourceLoader::load_resource(Response::Type type, const LoadRequest& request)
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

    // FIXME: Remove type
    auto resource = Response::create({}, type);

    if (use_cache)
        s_resource_cache.set(request, resource);

    load(
        request,
        [=](auto data, auto& headers, auto status_code) {
            const_cast<Response&>(*resource).did_load({}, data, headers, status_code);
        },
        [=](auto& error, auto status_code) {
            const_cast<Response&>(*resource).did_fail({}, error, status_code);
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
        dbgln("ResourceLoader: Failed load of: \"{}\", \033[31;1mError: {}\033[0m, Duration: {}ms", sanitized_url_for_logging(url), error_message, load_time_ms);
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
        HashMap<String, String> headers;
        headers.set("User-Agent", m_user_agent);
        headers.set("Accept-Encoding", "gzip, deflate");

        for (auto& it : request.headers()) {
            headers.set(it.name, it.value);
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
    auto request = LoadRequest::create_for_url_on_page(url, nullptr);
    load(request, move(success_callback), move(error_callback));
}

// https://fetch.spec.whatwg.org/#block-bad-port
bool ResourceLoader::is_port_blocked(const URL& url)
{
    if (!url.is_http_or_https())
        return false;
    u16 ports[] { 1, 7, 9, 11, 13, 15, 17, 19, 20, 21, 22, 23, 25, 37, 42,
        43, 53, 69, 77, 79, 87, 95, 101, 102, 103, 104, 109, 110, 111, 113,
        115, 117, 119, 123, 135, 137, 139, 143, 161, 179, 389, 427, 465, 512, 513, 514,
        515, 526, 530, 531, 532, 540, 548, 554, 556, 563, 587, 601, 636, 993, 995,
        1719, 1720, 1723, 2049, 3659, 4045, 5060, 5061, 6000, 6566, 6665, 6666, 6667, 6668, 6669, 6697, 10080 };
    for (auto blocked_port : ports)
        if (url.port() == blocked_port)
            return true;
    return false;
}

void ResourceLoader::clear_cache()
{
    dbgln_if(CACHE_DEBUG, "Clearing {} items from ResourceLoader cache", s_resource_cache.size());
    s_resource_cache.clear();
}

// https://fetch.spec.whatwg.org/#concept-fetch
// FIXME: This should create an instance of the fetch algorithm. This instance can be terminated, suspended and resumed.
void ResourceLoader::fetch(LoadRequest& request, ProcessRequestBodyType process_request_body, ProcessRequestEndOfBodyType process_request_end_of_body, ProcessReponseType process_response, ProcessResponseEndOfBodyType process_response_end_of_body, ProcessResponseDoneType process_response_done, [[maybe_unused]] bool use_parallel_queue)
{
    // FIXME: Let taskDestination be null.
    dbgln("Performing fetch for URL {}", request.url());

    bool cross_origin_isolated_capability = false;

    // FIXME: If request’s client is non-null, then:
    //          Set taskDestination to request’s client’s global object.
    //          Set crossOriginIsolatedCapability to request’s client’s cross-origin isolated capability.

    // FIXME: If useParallelQueue is true, then set taskDestination to the result of starting a new parallel queue.

    // FIXME: Let timingInfo be a new fetch timing info whose start time and post-redirect start time are the coarsened shared current time given crossOriginIsolatedCapability.
    FetchTimingInfo timing_info;

    FetchParams fetch_params = {
        .request = request,
        .process_request_body = process_request_body,
        .process_request_end_of_body = process_request_end_of_body,
        .process_response = process_response,
        .process_response_end_of_body = process_response_end_of_body,
        .process_response_done = process_response_done,
        // FIXME: task destination is taskDestination
        .cross_origin_isolated_capability = cross_origin_isolated_capability,
        .timing_info = timing_info,
    };

    // FIXME: If request’s body is a byte sequence, then set request’s body to the first return value of safely extracting request’s body.

    // FIXME: If request’s window is "client", then set request’s window to request’s client, if request’s client’s global object is a Window object; otherwise "no-window".
    request.set_window(LoadRequest::Window::NoWindow);

    // FIXME: If request’s origin is "client", then set request’s origin to request’s client’s origin.
    dbgln("Fetch: Is origin 'client'? {}", request.origin().has<LoadRequest::OriginEnum>());
    if (request.origin().has<LoadRequest::OriginEnum>()) {
        VERIFY(request.client());
        // FIXME: This is complete guess work until environment settings objects are implemented.
        if (!request.is_navigation_request()) {
            VERIFY(request.client()->focused_frame().document());
            request.set_origin(request.client()->focused_frame().document()->origin());
        } else {
            request.set_origin(Origin::create_from_url(request.url()));
        }
        dbgln("Fetch: The guessed origin is {}", request.origin().get<Origin>().serialize());
    }

    // FIXME: If request’s policy container is "client", then:
    //          If request’s client is non-null, then set request’s policy container to a clone of request’s client’s policy container. [HTML]
    //          Otherwise, set request’s policy container to a new policy container.

    if (!request.headers().contains("Accept")) {
        String value = "*/*";

        switch (request.destination()) {
        case LoadRequest::Destination::Document:
        case LoadRequest::Destination::Frame:
        case LoadRequest::Destination::IFrame:
            value = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
            break;
        case LoadRequest::Destination::Image:
            value = "image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5";
            break;
        case LoadRequest::Destination::Style:
            value = "text/css,*/*;q=0.1";
            break;
        default:
            break;
        }

        dbgln("Fetch: Appending 'Accept' header with value: {}", value);

        request.append_header("Accept", value);
    }

    // FIXME: If request’s header list does not contain `Accept-Language`, then user agents should append `Accept-Language`/an appropriate value to request’s header list.
    // FIXME: If request’s priority is null, then use request’s initiator and destination appropriately in setting request’s priority to a user-agent-defined object.

    // FIXME: If request is a subresource request, then:
    //          Let record be a new fetch record consisting of request and this instance of the fetch algorithm.
    //          Append record to request’s client’s fetch group list of fetch records.

    main_fetch(fetch_params);
}

// https://fetch.spec.whatwg.org/#concept-main-fetch
RefPtr<Response> ResourceLoader::main_fetch(const FetchParams& fetch_params, bool recursive)
{
    auto& request = fetch_params.request;
    RefPtr<Response> response;
    dbgln("ptr: {}", response.ptr());

    if (request.local_urls_only() && !request.current_url().is_local()) {
        dbgln("Fetch: Local urls only, but URL is not local.");
        response = Response::create_network_error({});
    }

    // FIXME: Run report Content Security Policy violations for request.
    // FIXME: Upgrade request to a potentially trustworthy URL, if appropriate.

    if (is_port_blocked(request.current_url()) /* FIXME: or should fetching request be blocked as mixed content, or should request be blocked by Content Security Policy */) {
        dbgln("Fetch: Bad port.");
        response = Response::create_network_error({});
    }

    // FIXME: If request’s referrer policy is the empty string and request’s client is non-null, then set request’s referrer policy to request’s client’s referrer policy. [REFERRER]
    if (request.referrer_policy() == ReferrerPolicy::ReferrerPolicy::None) {
        // This is the default referrer policy. https://w3c.github.io/webappsec-referrer-policy/#default-referrer-policy
        request.set_referrer_policy(ReferrerPolicy::ReferrerPolicy::StrictOriginWhenCrossOrigin);
        dbgln("Fetch: Using default referrer policy 'strict-origin-when-cross-origin'");
    }

    // FIXME: If request’s referrer is not "no-referrer", then set request’s referrer to the result of invoking determine request’s referrer. [REFERRER]
    // FIXME: Set request’s current URL’s scheme to "https" if all of the following conditions are true:
    //          request’s current URL’s scheme is "http"
    //          request’s current URL’s host is a domain
    //          Matching request’s current URL’s host per Known HSTS Host Domain Name Matching results in either a superdomain match with an asserted includeSubDomains directive or a congruent match (with or without an asserted includeSubDomains directive). [HSTS]

    // This implements step 12 of the algorithm. It's in a lambda because it's called from two different paths,
    // one that gets queued to go into another thread and one that stays on the current thread.
    auto do_fetch = [this, &fetch_params, &request]() -> RefPtr<Response> {
        auto& current_url = request.current_url();
        auto current_url_origin = Origin::create_from_url(current_url);
        auto& request_origin_variant = request.origin();
        VERIFY(request_origin_variant.template has<Origin>());
        auto& request_origin = request_origin_variant.template get<Origin>();

        if ((current_url_origin.is_same(request_origin) && request.response_tainting() == LoadRequest::ResponseTainting::Basic)
            || current_url.protocol() == "data"
            || (request.mode() == LoadRequest::Mode::Navigate || request.mode() == LoadRequest::Mode::WebSocket)) {
            dbgln("Fetch: Doing same origin scheme fetch.");
            request.set_response_tainting({}, LoadRequest::ResponseTainting::Basic);
            return scheme_fetch(fetch_params);
        }

        if (request.mode() == LoadRequest::Mode::SameOrigin) {
            dbgln("Fetch: Mode is same origin but did not meet same origin requirements.");
            return Response::create_network_error({});
        }

        if (request.mode() == LoadRequest::Mode::NoCors) {
            if (request.redirect_mode() != LoadRequest::RedirectMode::Follow) {
                dbgln("Fetch: Must follow redirects in no cors mode.");
                return Response::create_network_error({});
            }

            dbgln("Fetch: Doing no cors scheme fetch.");
            request.set_response_tainting({}, LoadRequest::ResponseTainting::Opaque);

            // FIXME: This should be done out of process to prevent side channel attacks!
            auto no_cors_response = scheme_fetch(fetch_params);
            // FIXME: If noCorsResponse is a filtered response or the CORB check with request and noCorsResponse returns allowed, then return noCorsResponse.
            // FIXME: Return a new response whose status is noCorsResponse’s status.
            return Response::create_network_error({});
        }

        if (!current_url.is_http_or_https()) {
            dbgln("Fetch: Not http or https.");
            return Response::create_network_error({});
        }

//      FIXME  if (request.use_cors_preflight()
//            || (request.unsafe_request() && (is_cors_safelisted_method(request.method()) || )))

        dbgln("Fetch: Doing cors HTTP fetch.");
        request.set_response_tainting({}, LoadRequest::ResponseTainting::Cors);
        return http_fetch(fetch_params);
    };

    if (!recursive) {
        dbgln("Fetch: Doing non-recursive fetch.");
        // FIXME: This should be in parallel.
        //deferred_invoke([&](auto&) {
            if (!response) {
                dbgln("Fetch: No response, let's do this!");
                response = do_fetch();
            } else {
                dbgln("Fetch: Not fetching, we already have a response!");
            }

            VERIFY(response);

            if (!response->is_network_error() && !response->is_filtered_response()) {
                // FIXME: If request’s response tainting is "cors", then:
                //          Let headerNames be the result of extracting header list values given `Access-Control-Expose-Headers` and response’s header list.
                //          If request’s credentials mode is not "include" and headerNames contains `*`, then set response’s CORS-exposed header-name list to all unique header names in response’s header list.
                //          Otherwise, if headerNames is not null or failure, then set response’s CORS-exposed header-name list to headerNames.

                response = response->to_filtered_response(request.response_tainting());
            }

            // FIXME: This is a bit sad that this is a RefPtr instead of a NonnullRefPtr.
            RefPtr<Response> internal_response;

            if (response->is_network_error())
                internal_response = response;
            else
                internal_response = response->internal_response();

            VERIFY(internal_response);

            if (internal_response->header_list().is_empty())
                internal_response->set_header_list({}, request.headers());

            if (!request.timing_allow_failed())
                internal_response->set_timing_allow_passed({}, true);

            if (!response->is_network_error()) {
                // FIXME: Mixed content
                // FIXME: CSP
                // FIXME: Maybe split these up so we can have an error message for which one failed?
                if (internal_response->should_be_blocked_due_to_mime_type(request) || internal_response->should_be_blocked_due_to_nosniff(request))
                    response = internal_response = Response::create_network_error({});
            }

            if (response->new_type() == Response::NewType::Opaque && internal_response->status() == 206 && internal_response->range_requested() && !request.headers().contains("Range"))
                response = internal_response = Response::create_network_error({});

            if (!response->is_network_error() && (request.method().is_one_of("HEAD", "CONNECT") || internal_response->has_null_body_status())) {
                // FIXME:  set internalResponse’s body to null and disregard any enqueuing toward it (if any)
            }

            // FIXME: If request’s integrity metadata is not the empty string, then:
            //          Do a bunch of stuff

            fetch_finale(fetch_params, response.release_nonnull());
       // });

        // Fetch does not return response on this path. The return value is only used for recursive calls.
        return {};
    } else {
        dbgln("Fetch: Doing recursive fetch.");
        if (!response)
            response = do_fetch();
        VERIFY(response);
        return response;
    }
}

// https://fetch.spec.whatwg.org/#concept-scheme-fetch
RefPtr<Response> ResourceLoader::scheme_fetch(const FetchParams& fetch_params)
{
    auto& request = fetch_params.request;
    auto& url = request.current_url();

    if (url.protocol() == "about") {
        // FIXME:
        dbgln("Loading about: URL {}", url);
//        deferred_invoke([success_callback = move(success_callback)](auto&) {
//            success_callback(String::empty().to_byte_buffer(), {}, {});
//        });
        return Response::create_network_error({});
    }

    // FIXME: Handle blob.

    if (url.protocol() == "data") {
        dbgln("ResourceLoader loading a data URL with mime-type: '{}', base64={}, payload='{}'",
              url.data_mime_type(),
              url.data_payload_is_base64(),
              url.data_payload());

        // FIXME: This is a lot more involved.
        ByteBuffer data;
        if (url.data_payload_is_base64())
            data = decode_base64(url.data_payload());
        else
            data = url.data_payload().to_byte_buffer();

        auto response = Response::create({}, Response::Type::Generic);
        response->set_body(data);
        return response;
    }

    if (url.protocol() == "file") {
        // NOTE: This is implementation defined.
        auto f = Core::File::construct();
        f->set_filename(url.path());
        if (!f->open(Core::OpenMode::ReadOnly)) {
            dbgln("ResourceLoader::scheme_fetch: Error: {}", f->error_string());
            return Response::create_network_error({});
        }

        auto data = f->read_all();
        auto response = Response::create({}, Response::Type::Generic);
        response->set_body(data);
        return response;
    }

    // FIXME: Handle gemini.
    if (url.is_http_or_https())
        return http_fetch(fetch_params);

    return Response::create_network_error({});
}

// https://fetch.spec.whatwg.org/#concept-http-fetch
RefPtr<Response> ResourceLoader::http_fetch(const FetchParams& fetch_params, [[maybe_unused]] bool make_cors_preflight)
{
    auto& request = fetch_params.request;
    RefPtr<Response> response;
    RefPtr<Response> actual_response;
    auto& timing_info = fetch_params.timing_info;

    // FIXME: If request’s service-workers mode is "all", then:

    if (!response) {
        // FIXME: If makeCORSPreflight is true and one of these conditions is true:

        if (request.redirect_mode() == LoadRequest::RedirectMode::Follow)
            request.set_service_workers_mode(LoadRequest::ServiceWorkersMode::None);

        dbgln("Going into network or cache fetch...");
        auto fetched_response = http_network_or_cache_fetch(fetch_params);
        dbgln("That fetch returned :^)");
        VERIFY(fetched_response);
        response = fetched_response;
        actual_response = fetched_response;

        if (request.response_tainting() == LoadRequest::ResponseTainting::Cors && !cors_check(request, response))
            return Response::create_network_error({});

        if (!tao_check(request, response))
            request.set_timing_allow_failed({}, true);
    }

    // FIXME: Cross-origin resource policy
    //if ((request.response_tainting() == LoadRequest::ResponseTainting::Opaque || response->new_type() == Response::NewType::Opaque))

    if (actual_response->has_redirect_status()) {
        // FIXME: If actualResponse’s status is not 303, request’s body is not null, and the connection uses HTTP/2, then user agents may, and are even encouraged to, transmit an RST_STREAM frame.

        switch (request.redirect_mode()) {
        case LoadRequest::RedirectMode::Error:
            response = Response::create_network_error({});
            break;
        case LoadRequest::RedirectMode::Manual:
            // FIXME: Set response to an opaque-redirect filtered response whose internal response is actualResponse.
            TODO();
            break;
        case LoadRequest::RedirectMode::Follow:
            response = http_redirect_fetch(fetch_params, response);
            break;
        }
    }

    response->set_timing_info({}, timing_info);

    return response;
}

// https://fetch.spec.whatwg.org/#concept-http-network-or-cache-fetch
RefPtr<Response> ResourceLoader::http_network_or_cache_fetch(const FetchParams& fetch_params, bool is_authentication_fetch, bool is_new_connection_fetch)
{
    auto& request = fetch_params.request;
    // FIXME: Let httpFetchParams be null.
    // FIXME: Let httpRequest be null.
    RefPtr<Response> response;
    // FIXME: Let storedResponse be null.
    // FIXME: Let httpCache be null.
    //bool revalidating_flag = false; FIXME: Unused

    // FIXME: Run these steps, but abort when the ongoing fetch is terminated:
    // Here, we are setting up all the HTTP headers as per the spec and potentially returning a cached response.
    // FIXME: If request’s window is "no-window" and request’s redirect mode is "error", then set httpFetchParams to fetchParams and httpRequest to request.
    // FIXME: Otherwise:
    //          Set httpRequest to a clone of request.
    //          Set httpFetchParams to a copy of fetchParams.
    //          Set httpFetchParams’s request to httpRequest.

    bool include_credentials = request.credentials_mode() == LoadRequest::CredentialsMode::Include || (request.credentials_mode() == LoadRequest::CredentialsMode::SameOrigin && request.response_tainting() == LoadRequest::ResponseTainting::Basic);
    // FIXME: Let contentLength be httpRequest’s body’s length, if httpRequest’s body is non-null; otherwise null.
    Optional<size_t> content_length;
    String content_length_header_value; // FIXME: This should be a byte sequence.

    // FIXME: Let contentLengthHeaderValue be null.
    // FIXME: If httpRequest’s body is null and httpRequest’s method is `POST` or `PUT`, then set contentLengthHeaderValue to `0`.

    if (content_length.has_value()) {
        // FIXME: And isomorphic encoded.
        content_length_header_value = String::number(content_length.value());
    }

    if (!content_length_header_value.is_null())
        request.append_header("Content-Length", content_length_header_value);

    // FIXME: If contentLength is non-null and httpRequest’s keepalive is true, then:

    if (request.referrer().has<URL>()) {
        // FIXME: Isomorphic encode the serialized URL.
        // FIXME: Is this an encoded URL?
        request.append_header("Referer", request.referrer().get<URL>().to_string_encoded());
    }

    // https://fetch.spec.whatwg.org/#append-a-request-origin-header
    auto serialized_origin = request.serialize_origin(); // FIXME: Byte serialized.

    if (request.response_tainting() == LoadRequest::ResponseTainting::Cors || request.mode() == LoadRequest::Mode::WebSocket) {
        request.append_header("Origin", serialized_origin);
    } else if (!request.method().is_one_of("GET", "HEAD")) {
        switch (request.referrer_policy()) {
        case ReferrerPolicy::ReferrerPolicy::NoReferrer:
            serialized_origin = "null";
            break;
        case ReferrerPolicy::ReferrerPolicy::NoReferrerWhenDowngrade:
        case ReferrerPolicy::ReferrerPolicy::StrictOrigin:
        case ReferrerPolicy::ReferrerPolicy::StrictOriginWhenCrossOrigin: {
            auto& request_origin = request.origin().get<Origin>();
            if (!request_origin.is_null() && request_origin.protocol() == "https" && request.current_url().protocol() != "https")
                serialized_origin = "null";
            break;
        }
        case ReferrerPolicy::ReferrerPolicy::SameOrigin: {
            auto& request_origin = request.origin().get<Origin>();
            auto current_url_origin = Origin::create_from_url(request.current_url());
            if (!request_origin.is_same(current_url_origin))
                serialized_origin = "null";
            break;
        }
        default:
            break;
        }

        request.append_header("Origin", serialized_origin);
    }

    // === End of append origin header ===

    // FIXME: Append the Fetch metadata headers for httpRequest.

    if (!request.headers().contains("User-Agent"))
        request.append_header("User-Agent", m_user_agent);

    if (request.cache_mode() == LoadRequest::CacheMode::Default && (request.headers().contains("If-Modified-Since") || request.headers().contains("If-None-Match") || request.headers().contains("If-Unmodified-Since") || request.headers().contains("If-Match") || request.headers().contains("If-Range")))
        request.set_cache_mode(LoadRequest::CacheMode::NoStore);

    if (request.cache_mode() == LoadRequest::CacheMode::NoStore && !request.prevent_no_cache_cache_control_header_modification() && !request.headers().contains("Cache-Control"))
        request.append_header("Cache-Control", "max-age=0");

    if (request.headers().contains("Range"))
        request.append_header("Accept-Encoding", "identity");

    // Step 18: "Modify httpRequest’s header list per HTTP. Do not append a given header if httpRequest’s header list contains that header’s name."
    // This is where we start inserting headers such as Accept-Encoding, Connection, etc.
    if (!request.headers().contains("Accept-Encoding"))
        request.append_header("Accept-Encoding", "gzip, deflate");

    if (include_credentials) {
        // FIXME: If not configured to block cookies:
        if (request.client()) {
            // FIXME: Is this the correct source?
            auto cookie = request.client()->client().page_did_request_cookie(request.current_url(), Cookie::Source::Http);
            dbgln("do we have cookie for {}? {}", request.current_url().to_string(), !cookie.is_empty());
            if (!cookie.is_empty())
                request.append_header("Cookie", cookie);
        }

        if (!request.headers().contains("Authorization")) {
            // FIXME: Let authorizationValue be null.
            // FIXME: If there’s an authentication entry for httpRequest and either httpRequest’s use-URL-credentials flag is unset or httpRequest’s current URL does not include credentials, then set authorizationValue to authentication entry.
            // FIXME: Otherwise, if httpRequest’s current URL does include credentials and isAuthenticationFetch is true, set authorizationValue to httpRequest’s current URL, converted to an `Authorization` value.
            // FIXME: If authorizationValue is non-null, then append `Authorization`/authorizationValue to httpRequest’s header list.
        }
    }

    // FIXME: If there’s a proxy-authentication entry, use it as appropriate. (yes, that's what it says)
    // FIXME: Set httpCache to the result of determining the HTTP cache partition, given httpRequest.
    // FIXME: If httpCache is null, then set httpRequest’s cache mode to "no-store".
    // FIXME: If httpRequest’s cache mode is neither "no-store" nor "reload", then:

    // FIXME: If aborted, then:
    //          Let aborted be the termination’s aborted flag.
    //          If aborted is set, then return an aborted network error.
    //          Return a network error.

    // Response can still be null here as all the stuff before was setting up the request and then potentially
    // getting it from cache. No network fetching has happened yet at this point.
    if (!response) {
        if (request.cache_mode() == LoadRequest::CacheMode::OnlyIfCached)
            return Response::create_network_error({});

        dbgln("Going into network fetch...");
        auto forward_response = http_network_fetch(fetch_params /* FIXME: httpFetchParams */, include_credentials, is_new_connection_fetch);
        dbgln("Network fetch returned :^)");
        VERIFY(forward_response);

        // FIXME: If httpRequest’s method is unsafe and forwardResponse’s status is in the range 200 to 399, inclusive, invalidate appropriate stored responses in httpCache, as per the "Invalidation" chapter of HTTP Caching, and set storedResponse to null. [HTTP-CACHING]
        // FIXME: More cache stuff.....

        if (!response) {
            response = forward_response;
            // FIXME: Store httpRequest and forwardResponse in httpCache, as per the "Storing Responses in Caches" chapter of HTTP Caching.
        }
    }

    VERIFY(response);

    response->set_url_list({}, request.url_list());

    if (response->header_list().contains("Range"))
        response->set_range_requested({}, true);

    // FIXME: If response’s status is 401, httpRequest’s response tainting is not "cors", includeCredentials is true, and request’s window is an environment settings object, then:

    // Handle HTTP status 407 - Proxy Authentication Required
    if (response->status() == 407) {
        if (request.window() == LoadRequest::Window::NoWindow)
            return Response::create_network_error({});

        // FIXME: Handle proxy authentication
        TODO();
    }

    // Handle HTTP status 421 - Misdirected Request
    if (response->status() == 421 && !is_new_connection_fetch && request.body().is_empty() /* FIXME: or request’s body is non-null and request’s body’s source is non-null */ ) {
        // FIXME: If the ongoing fetch is terminated, then:
        //          Let aborted be the termination’s aborted flag.
        //          If aborted is set, then return an aborted network error.
        //          Return a network error.
        response = http_network_or_cache_fetch(fetch_params, is_authentication_fetch, true);
    }

    // FIXME: If isAuthenticationFetch is true, then create an authentication entry for request and the given realm.

    return response;
}

// https://fetch.spec.whatwg.org/#concept-http-network-fetch
RefPtr<Response> ResourceLoader::http_network_fetch(const FetchParams& fetch_params, bool include_credentials, [[maybe_unused]] bool force_new_connection)
{
    auto& request = fetch_params.request;
    RefPtr<Response> response;
    // FIXME: Let timingInfo be fetchParams’s timing info.
    // FIXME: Let httpCache be the result of determining the HTTP cache partition, given httpRequest.
    // FIXME: If httpCache is null: (which it always is currently)
    request.set_cache_mode(LoadRequest::CacheMode::NoStore);
    // Let networkPartitionKey be the result of determining the network partition key given request.

    // FIXME: If mode is websocket, obtain a WebSocket connection, given request's current URL.
    // FIXME: Otherwise: Let connection be the result of obtaining a connection, given networkPartitionKey, request’s current URL’s origin, includeCredentials, and forceNewConnection.

    // FIXME: Set timingInfo’s final connection timing info to the result of calling clamp and coarsen connection timing info with connection’s timing info, timingInfo’s post-redirect start time, and fetchParams’s cross-origin isolated capability.

    // FIXME: Run these steps, but abort when the ongoing fetch is terminated:
    //          If connection is failure, return a network error.
    //          If connection is not an HTTP/2 connection, request’s body is non-null, and request’s body’s source is null, then append `Transfer-Encoding`/`chunked` to request’s header list.
    //          Set timingInfo’s final network-request start time to the coarsened shared current time given fetchParams’s cross-origin isolated capability.

    VERIFY(!request.headers().is_empty());

    for (auto& it : request.headers().list()) {
        dbgln("Fetch: Header name: {} value: {}", it.name, it.value);
    }

    Core::EventLoop loop;

    dbgln("Fetch: Creating {} request for {}", request.method(), request.current_url().to_string_encoded());
    auto protocol_request = protocol_client().start_request(request.method(), request.current_url().to_string_encoded(), request.headers(), request.body());
    if (!protocol_request) {
        dbgln("Fetch: Failed to create request.");
        return Response::create_network_error({});
    }
//    protocol_request->on_headers_received = ([&](const HashMap<String, String, CaseInsensitiveStringTraits>& response_headers, Optional<u32> response_code) {
//        response = Response::create({}, Response::Type::Generic);
//        for (auto& header : response_headers)
//            response->append_header({}, header.key, header.value);
//        response->set_status({}, response_code.value());
//        loop.quit(0);
//    });
//    dbgln("Wowe");
//    protocol_request->on_headers_received = [&](auto& headers, auto response_code) {
//        dbgln("Fetch: Headers received, let's continue.");
//        VERIFY(response_code.has_value());
//        for (auto& header : headers)
//            response->append_header({}, header.key, header.value);
//        response->set_status({}, response_code.value());
//        loop.quit(0);
//    };
//    protocol_request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey {
//        return {};
//    };
//    protocol_request->stream_into(response->output_memory_stream({}));
//    protocol_request->on_finish = [response = move(response), this, &fetch_params](bool success, auto) {
//        RefPtr<Response> res = response;
//        VERIFY(success); // FIXME: Handle non-success
//        //res->set_body(res->output_memory_stream({}).bytes()); // FIXME: Not exactly optimal...
//        finalize_response(fetch_params, res);
//    };

    protocol_request->on_buffered_request_finish = [&](bool success, auto, auto& response_headers, auto status_code, ReadonlyBytes payload) {
        if (!success) {
            response = Response::create_network_error({});
            return;
        }
        deferred_invoke([protocol_request](auto&) {
            // Clear circular reference of `protocol_request` captured by copy
            const_cast<Protocol::Request&>(*protocol_request).on_buffered_request_finish = nullptr;
        });
        response = Response::create({}, Response::Type::Generic); // FIXME: definitely not always an image
        for (auto& header : response_headers)
            response->append_header({}, header.key, header.value);
        response->set_status({}, status_code.value());
        response->set_body(payload);
        loop.quit(0);
    };
    protocol_request->set_should_buffer_all_input(true);

    protocol_request->on_certificate_requested = []() -> Protocol::Request::CertificateAndKey {
        return {};
    };

    // Fetch mandates that we have to wait for the headers to come in before continuing.
    dbgln("Fetch: Waiting to receive headers...");
    loop.exec();

    dbgln("We there yet?");

    // FIXME: Use Streams instead of just getting all the data at once.

    // FIXME: Run these steps, but abort when the ongoing fetch is terminated:
    //          Set response’s body to a new body whose stream is stream.
    //          If response is not a network error and request’s cache mode is not "no-store", then update response in httpCache for request.
    if (include_credentials /* FIXME: and not configured to block cookies */) {
        if (request.client()) {
            for (auto& header : request.headers().list()) {
                if (header.name.equals_ignoring_case("Set-Cookie")) {
                    auto cookie = Cookie::parse_cookie(header.value);
                    if (!cookie.has_value())
                        continue;

                    // FIXME: Is this the correct source?
                    request.client()->client().page_did_set_cookie(request.current_url(), cookie.value(), Cookie::Source::Http);
                }
            }
        }
    }

    // FIXME: If aborted, then:
    //          Let aborted be the termination’s aborted flag.
    //          If aborted is set, then set response’s aborted flag.
    //          Return response.

    VERIFY(response);

    return response;
}

// https://fetch.spec.whatwg.org/#concept-http-redirect-fetch
RefPtr<Response> ResourceLoader::http_redirect_fetch(const FetchParams& fetch_params, RefPtr<Response> response)
{
    auto& request = fetch_params.request;
    // FIXME: Let actualResponse be response, if response is not a filtered response, and response’s internal response otherwise.
    //        Once fixed, replace all use of "response" below with "actualResponse".

    auto location_url_optional = response->location_url(request.current_url().fragment());
    if (!location_url_optional.has_value())
        return response; // FIXME: Except this one, don't replace this one.

    auto& location_url = location_url_optional.value();

    if (!location_url.is_valid())
        return Response::create_network_error({});

    if (!location_url.is_http_or_https())
        return Response::create_network_error({});

    if (request.redirect_count() == maximum_redirects_allowed)
        return Response::create_network_error({});

    request.increment_redirect_count({});

    auto location_url_origin = Origin::create_from_url(location_url);
    auto& request_origin = request.origin().get<Origin>();

    if (request.mode() == LoadRequest::Mode::Cors && location_url.include_credentials() && !request_origin.is_same(location_url_origin))
        return Response::create_network_error({});

    if (request.response_tainting() == LoadRequest::ResponseTainting::Cors && location_url.include_credentials())
        return Response::create_network_error({});

    // FIXME: If actualResponse’s status is not 303, request’s body is non-null, and request’s body’s source is null, then return a network error.

    auto current_url_origin = Origin::create_from_url(request.current_url());

    if (!location_url_origin.is_same(current_url_origin) && !request_origin.is_same(current_url_origin))
        request.set_tainted_origin({}, true);

    if (((response->status() == 301 || response->status() == 302) && request.method() == "POST")
        || (response->status() == 303 && !request.method().is_one_of("GET", "HEAD"))) {
        // FIXME
    }

    // FIXME: If request’s body is non-null, then set request’s body to the first return value of safely extracting request’s body’s source.

    // FIXME: Let timingInfo be fetchParams’s timing info.
    // FIXME: Set timingInfo’s redirect end time and post-redirect start time to the coarsened shared current time given fetchParams’s cross-origin isolated capability.
    // FIXME: If timingInfo’s redirect start time is 0, then set timingInfo’s redirect start time to timingInfo’s start time.

    request.append_url_to_url_list({}, location_url);

    // FIXME: Invoke set request’s referrer policy on redirect on request and actualResponse.

    return main_fetch(fetch_params, true);
}

// https://fetch.spec.whatwg.org/#fetch-finale
void ResourceLoader::fetch_finale(const FetchParams& fetch_params, NonnullRefPtr<Response> response)
{
    if (fetch_params.process_response) {
        // FIXME: Queue a fetch task to do this, with fetchParam's task destination.
        fetch_params.process_response(response);
    }

    if (fetch_params.process_response_end_of_body) {
        // FIXME
    }

    // FIXME: not done here!!
    finalize_response(fetch_params, response);
}

void ResourceLoader::finalize_response(const FetchParams& fetch_params, RefPtr<Response> response)
{
    fetch_params.request.set_done({}, true);
    if (fetch_params.process_response_done) {
        // FIXME: Queue a fetch task to do this, with fetchParam's task destination.
        fetch_params.process_response_done(*response);
    }
}

// https://fetch.spec.whatwg.org/#concept-cors-check
// Returns false for failure, true for success.
bool ResourceLoader::cors_check(const LoadRequest& request, RefPtr<Response> response)
{
    auto origin = response->header_list().get("Access-Control-Allow-Origin");
    if (origin.is_null())
        return false;

    if (request.credentials_mode() != LoadRequest::CredentialsMode::Include && origin == "*")
        return true;

    // FIXME: This should be byte serialized.
    if (request.serialize_origin() != origin)
        return false;

    if (request.credentials_mode() != LoadRequest::CredentialsMode::Include)
        return true;

    auto credentials = response->header_list().get("Access-Control-Allow-Credentials");
    if (credentials == "true")
        return true;

    return false;
}

// https://fetch.spec.whatwg.org/#concept-tao-check
// Returns false for failure, true for success.
bool ResourceLoader::tao_check(const LoadRequest& load_request, RefPtr<Response> response)
{
    if (load_request.timing_allow_failed())
        return false;

    if (load_request.response_tainting() == LoadRequest::ResponseTainting::Basic)
        return true;

    auto values = response->header_list().get_decode_and_split("Timing-Allow-Origin");

    if (values.find("*") != values.end())
        return true;

    if (values.find(load_request.serialize_origin()) != values.end())
        return true;

    return false;
}

}
