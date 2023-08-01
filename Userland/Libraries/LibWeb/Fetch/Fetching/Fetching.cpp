/*
 * Copyright (c) 2022-2023, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2023, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Base64.h>
#include <AK/Debug.h>
#include <AK/ScopeGuard.h>
#include <LibJS/Runtime/Completion.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Fetching/Checks.h>
#include <LibWeb/Fetch/Fetching/Fetching.h>
#include <LibWeb/Fetch/Fetching/PendingResponse.h>
#include <LibWeb/Fetch/Fetching/RefCountedFlag.h>
#include <LibWeb/Fetch/Infrastructure/FetchAlgorithms.h>
#include <LibWeb/Fetch/Infrastructure/FetchController.h>
#include <LibWeb/Fetch/Infrastructure/FetchParams.h>
#include <LibWeb/Fetch/Infrastructure/FetchTimingInfo.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Methods.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Statuses.h>
#include <LibWeb/Fetch/Infrastructure/MimeTypeBlocking.h>
#include <LibWeb/Fetch/Infrastructure/NoSniffBlocking.h>
#include <LibWeb/Fetch/Infrastructure/PortBlocking.h>
#include <LibWeb/Fetch/Infrastructure/Task.h>
#include <LibWeb/Fetch/Infrastructure/URL.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/FileAPI/BlobURLStore.h>
#include <LibWeb/HTML/EventLoop/EventLoop.h>
#include <LibWeb/HTML/Scripting/Environments.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/HighResolutionTime/TimeOrigin.h>
#include <LibWeb/Loader/LoadRequest.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Platform/EventLoopPlugin.h>
#include <LibWeb/ReferrerPolicy/AbstractOperations.h>
#include <LibWeb/SRI/SRI.h>
#include <LibWeb/URL/URL.h>
#include <LibWeb/WebIDL/DOMException.h>

namespace Web::Fetch::Fetching {

#define TRY_OR_IGNORE(expression)                                                                    \
    ({                                                                                               \
        auto&& _temporary_result = (expression);                                                     \
        if (_temporary_result.is_error())                                                            \
            return;                                                                                  \
        static_assert(!::AK::Detail::IsLvalueReference<decltype(_temporary_result.release_value())>, \
            "Do not return a reference from a fallible expression");                                 \
        _temporary_result.release_value();                                                           \
    })

// https://fetch.spec.whatwg.org/#concept-fetch
WebIDL::ExceptionOr<JS::NonnullGCPtr<Infrastructure::FetchController>> fetch(JS::Realm& realm, Infrastructure::Request& request, Infrastructure::FetchAlgorithms const& algorithms, UseParallelQueue use_parallel_queue)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'fetch' with: request @ {}", &request);

    auto& vm = realm.vm();

    // 1. Assert: request’s mode is "navigate" or processEarlyHintsResponse is null.
    VERIFY(request.mode() == Infrastructure::Request::Mode::Navigate || !algorithms.process_early_hints_response().has_value());

    // 2. Let taskDestination be null.
    JS::GCPtr<JS::Object> task_destination;

    // 3. Let crossOriginIsolatedCapability be false.
    auto cross_origin_isolated_capability = HTML::CanUseCrossOriginIsolatedAPIs::No;

    // 4. If request’s client is non-null, then:
    if (request.client() != nullptr) {
        // 1. Set taskDestination to request’s client’s global object.
        task_destination = request.client()->global_object();

        // 2. Set crossOriginIsolatedCapability to request’s client’s cross-origin isolated capability.
        cross_origin_isolated_capability = request.client()->cross_origin_isolated_capability();
    }

    // FIXME: 5. If useParallelQueue is true, then set taskDestination to the result of starting a new parallel queue.
    (void)use_parallel_queue;
    (void)task_destination;

    // 6. Let timingInfo be a new fetch timing info whose start time and post-redirect start time are the coarsened
    //    shared current time given crossOriginIsolatedCapability, and render-blocking is set to request’s
    //    render-blocking.
    auto timing_info = Infrastructure::FetchTimingInfo::create(vm);
    auto now = HighResolutionTime::coarsened_shared_current_time(cross_origin_isolated_capability == HTML::CanUseCrossOriginIsolatedAPIs::Yes);
    timing_info->set_start_time(now);
    timing_info->set_post_redirect_start_time(now);
    timing_info->set_render_blocking(request.render_blocking());

    // 7. Let fetchParams be a new fetch params whose request is request, timing info is timingInfo, process request
    //    body chunk length is processRequestBodyChunkLength, process request end-of-body is processRequestEndOfBody,
    //    process early hints response is processEarlyHintsResponse, process response is processResponse, process
    //    response consume body is processResponseConsumeBody, process response end-of-body is processResponseEndOfBody,
    //    task destination is taskDestination, and cross-origin isolated capability is crossOriginIsolatedCapability.
    auto fetch_params = Infrastructure::FetchParams::create(vm, request, timing_info);
    fetch_params->set_algorithms(algorithms);
    if (task_destination)
        fetch_params->set_task_destination({ *task_destination });
    fetch_params->set_cross_origin_isolated_capability(cross_origin_isolated_capability);

    // 8. If request’s body is a byte sequence, then set request’s body to request’s body as a body.
    if (auto const* buffer = request.body().get_pointer<ByteBuffer>())
        request.set_body(TRY(Infrastructure::byte_sequence_as_body(realm, buffer->bytes())));

    // 9. If request’s window is "client", then set request’s window to request’s client, if request’s client’s global
    //    object is a Window object; otherwise "no-window".
    auto const* window = request.window().get_pointer<Infrastructure::Request::Window>();
    if (window && *window == Infrastructure::Request::Window::Client) {
        if (is<HTML::Window>(request.client()->global_object())) {
            request.set_window(request.client());
        } else {
            request.set_window(Infrastructure::Request::Window::NoWindow);
        }
    }

    // 10. If request’s origin is "client", then set request’s origin to request’s client’s origin.
    auto const* origin = request.origin().get_pointer<Infrastructure::Request::Origin>();
    if (origin && *origin == Infrastructure::Request::Origin::Client)
        request.set_origin(request.client()->origin());

    // 12. If request’s policy container is "client", then:
    auto const* policy_container = request.policy_container().get_pointer<Infrastructure::Request::PolicyContainer>();
    if (policy_container) {
        VERIFY(*policy_container == Infrastructure::Request::PolicyContainer::Client);
        // 1. If request’s client is non-null, then set request’s policy container to a clone of request’s client’s
        //    policy container.
        if (request.client() != nullptr)
            request.set_policy_container(request.client()->policy_container());
        // 2. Otherwise, set request’s policy container to a new policy container.
        else
            request.set_policy_container(HTML::PolicyContainer {});
    }

    // 13. If request’s header list does not contain `Accept`, then:
    if (!request.header_list()->contains("Accept"sv.bytes())) {
        // 1. Let value be `*/*`.
        auto value = "*/*"sv;

        // 2. A user agent should set value to the first matching statement, if any, switching on request’s
        //    destination:
        if (request.destination().has_value()) {
            switch (*request.destination()) {
            // -> "document"
            // -> "frame"
            // -> "iframe"
            case Infrastructure::Request::Destination::Document:
            case Infrastructure::Request::Destination::Frame:
            case Infrastructure::Request::Destination::IFrame:
                // `text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8`
                value = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"sv;
                break;
            // -> "image"
            case Infrastructure::Request::Destination::Image:
                // `image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5`
                value = "image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5"sv;
                break;
            // -> "style"
            case Infrastructure::Request::Destination::Style:
                // `text/css,*/*;q=0.1`
                value = "text/css,*/*;q=0.1"sv;
                break;
            default:
                break;
            }
        }

        // 3. Append (`Accept`, value) to request’s header list.
        auto header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Accept"sv, value.bytes()));
        TRY_OR_THROW_OOM(vm, request.header_list()->append(move(header)));
    }

    // 14. If request’s header list does not contain `Accept-Language`, then user agents should append
    //     (`Accept-Language, an appropriate header value) to request’s header list.
    if (!request.header_list()->contains("Accept-Language"sv.bytes())) {
        auto header = MUST(Infrastructure::Header::from_string_pair("Accept-Language"sv, "*"sv));
        TRY_OR_THROW_OOM(vm, request.header_list()->append(move(header)));
    }

    // 15. If request’s priority is null, then use request’s initiator, destination, and render-blocking appropriately
    //     in setting request’s priority to a user-agent-defined object.
    // NOTE: The user-agent-defined object could encompass stream weight and dependency for HTTP/2, and equivalent
    //       information used to prioritize dispatch and processing of HTTP/1 fetches.

    // 16. If request is a subresource request, then:
    if (request.is_subresource_request()) {
        // FIXME: 1. Let record be a new fetch record whose request is request and controller is fetchParams’s controller.
        // FIXME: 2. Append record to request’s client’s fetch group list of fetch records.
    }

    // 17. Run main fetch given fetchParams.
    (void)TRY(main_fetch(realm, fetch_params));

    // 18. Return fetchParams’s controller.
    return fetch_params->controller();
}

// https://fetch.spec.whatwg.org/#concept-main-fetch
WebIDL::ExceptionOr<Optional<JS::NonnullGCPtr<PendingResponse>>> main_fetch(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params, Recursive recursive)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'main fetch' with: fetch_params @ {}", &fetch_params);

    auto& vm = realm.vm();

    // 1. Let request be fetchParams’s request.
    auto request = fetch_params.request();

    // 2. Let response be null.
    JS::GCPtr<Infrastructure::Response> response;

    // 3. If request’s local-URLs-only flag is set and request’s current URL is not local, then set response to a
    //    network error.
    if (request->local_urls_only() && !Infrastructure::is_local_url(request->current_url()))
        response = Infrastructure::Response::network_error(vm, "Request with 'local-URLs-only' flag must have a local URL"sv);

    // FIXME: 4. Run report Content Security Policy violations for request.
    // FIXME: 5. Upgrade request to a potentially trustworthy URL, if appropriate.
    // FIXME: 6. Upgrade a mixed content request to a potentially trustworthy URL, if appropriate.

    // 7. If should request be blocked due to a bad port, should fetching request be blocked as mixed content, or
    //    should request be blocked by Content Security Policy returns blocked, then set response to a network error.
    if (Infrastructure::block_bad_port(request) == Infrastructure::RequestOrResponseBlocking::Blocked
        || false // FIXME: "should fetching request be blocked as mixed content"
        || false // FIXME: "should request be blocked by Content Security Policy returns blocked"
    ) {
        response = Infrastructure::Response::network_error(vm, "Request was blocked"sv);
    }

    // 8. If request’s referrer policy is the empty string, then set request’s referrer policy to request’s policy
    //    container’s referrer policy.
    if (!request->referrer_policy().has_value()) {
        VERIFY(request->policy_container().has<HTML::PolicyContainer>());
        request->set_referrer_policy(request->policy_container().get<HTML::PolicyContainer>().referrer_policy);
    }

    // 9. If request’s referrer is not "no-referrer", then set request’s referrer to the result of invoking determine
    //    request’s referrer.
    // NOTE: As stated in Referrer Policy, user agents can provide the end user with options to override request’s
    //       referrer to "no-referrer" or have it expose less sensitive information.
    auto const* referrer = request->referrer().get_pointer<Infrastructure::Request::Referrer>();
    if (!referrer || *referrer != Infrastructure::Request::Referrer::NoReferrer) {
        auto determined_referrer = ReferrerPolicy::determine_requests_referrer(request);
        if (determined_referrer.has_value())
            request->set_referrer(*determined_referrer);
        else
            request->set_referrer(Infrastructure::Request::Referrer::NoReferrer);
    }

    // 10. Set request’s current URL’s scheme to "https" if all of the following conditions are true:
    if (
        // - request’s current URL’s scheme is "http"
        request->current_url().scheme() == "http"sv
        // - request’s current URL’s host is a domain
        && URL::host_is_domain(request->current_url().host())
        // FIXME: - Matching request’s current URL’s host per Known HSTS Host Domain Name Matching results in either a
        //          superdomain match with an asserted includeSubDomains directive or a congruent match (with or without an
        //          asserted includeSubDomains directive) [HSTS]; or DNS resolution for the request finds a matching HTTPS RR
        //          per section 9.5 of [SVCB].
        && false

    ) {
        request->current_url().set_scheme("https"sv);
    }

    JS::SafeFunction<WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>>()> get_response = [&realm, &vm, &fetch_params, request]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> {
        dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'main fetch' get_response() function");

        // -> fetchParams’s preloaded response candidate is not null
        if (!fetch_params.preloaded_response_candidate().has<Empty>()) {
            // 1. Wait until fetchParams’s preloaded response candidate is not "pending".
            HTML::main_thread_event_loop().spin_until([&] {
                return !fetch_params.preloaded_response_candidate().has<Infrastructure::FetchParams::PreloadedResponseCandidatePendingTag>();
            });

            // 2. Assert: fetchParams’s preloaded response candidate is a response.
            VERIFY(fetch_params.preloaded_response_candidate().has<JS::NonnullGCPtr<Infrastructure::Response>>());

            // 3. Return fetchParams’s preloaded response candidate.
            return PendingResponse::create(vm, request, fetch_params.preloaded_response_candidate().get<JS::NonnullGCPtr<Infrastructure::Response>>());
        }
        // -> request’s current URL’s origin is same origin with request’s origin, and request’s response tainting
        //    is "basic"
        // -> request’s current URL’s scheme is "data"
        // -> request’s mode is "navigate" or "websocket"
        else if (
            (request->origin().has<HTML::Origin>() && URL::url_origin(request->current_url()).is_same_origin(request->origin().get<HTML::Origin>()) && request->response_tainting() == Infrastructure::Request::ResponseTainting::Basic)
            || request->current_url().scheme() == "data"sv
            || (request->mode() == Infrastructure::Request::Mode::Navigate || request->mode() == Infrastructure::Request::Mode::WebSocket)) {
            // 1. Set request’s response tainting to "basic".
            request->set_response_tainting(Infrastructure::Request::ResponseTainting::Basic);

            // 2. Return the result of running scheme fetch given fetchParams.
            return scheme_fetch(realm, fetch_params);

            // NOTE: HTML assigns any documents and workers created from URLs whose scheme is "data" a unique
            //       opaque origin. Service workers can only be created from URLs whose scheme is an HTTP(S) scheme.
        }
        // -> request’s mode is "same-origin"
        else if (request->mode() == Infrastructure::Request::Mode::SameOrigin) {
            // Return a network error.
            return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request with 'same-origin' mode must have same URL and request origin"sv));
        }
        // -> request’s mode is "no-cors"
        else if (request->mode() == Infrastructure::Request::Mode::NoCORS) {
            // 1. If request’s redirect mode is not "follow", then return a network error.
            if (request->redirect_mode() != Infrastructure::Request::RedirectMode::Follow)
                return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request with 'no-cors' mode must have redirect mode set to 'follow'"sv));

            // 2. Set request’s response tainting to "opaque".
            request->set_response_tainting(Infrastructure::Request::ResponseTainting::Opaque);

            // 3. Return the result of running scheme fetch given fetchParams.
            return scheme_fetch(realm, fetch_params);
        }
        // -> request’s current URL’s scheme is not an HTTP(S) scheme
        else if (!Infrastructure::is_http_or_https_scheme(request->current_url().scheme())) {
            // NOTE: At this point all other request modes have been handled. Ensure we're not lying in the error message :^)
            VERIFY(request->mode() == Infrastructure::Request::Mode::CORS);

            // Return a network error.
            return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request with 'cors' mode must have URL with HTTP or HTTPS scheme"sv));
        }
        // -> request’s use-CORS-preflight flag is set
        // -> request’s unsafe-request flag is set and either request’s method is not a CORS-safelisted method or
        //    CORS-unsafe request-header names with request’s header list is not empty
        else if (
            request->use_cors_preflight()
            || (request->unsafe_request()
                && (!Infrastructure::is_cors_safelisted_method(request->method())
                    || !TRY_OR_THROW_OOM(vm, Infrastructure::get_cors_unsafe_header_names(request->header_list())).is_empty()))) {
            // 1. Set request’s response tainting to "cors".
            request->set_response_tainting(Infrastructure::Request::ResponseTainting::CORS);

            auto returned_pending_response = PendingResponse::create(vm, request);

            // 2. Let corsWithPreflightResponse be the result of running HTTP fetch given fetchParams and true.
            auto cors_with_preflight_response = TRY(http_fetch(realm, fetch_params, MakeCORSPreflight::Yes));
            cors_with_preflight_response->when_loaded([returned_pending_response](JS::NonnullGCPtr<Infrastructure::Response> cors_with_preflight_response) {
                dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'main fetch' cors_with_preflight_response load callback");
                // 3. If corsWithPreflightResponse is a network error, then clear cache entries using request.
                if (cors_with_preflight_response->is_network_error()) {
                    // FIXME: Clear cache entries
                }

                // 4. Return corsWithPreflightResponse.
                returned_pending_response->resolve(cors_with_preflight_response);
            });

            return returned_pending_response;
        }
        // -> Otherwise
        else {
            // 1. Set request’s response tainting to "cors".
            request->set_response_tainting(Infrastructure::Request::ResponseTainting::CORS);

            // 2. Return the result of running HTTP fetch given fetchParams.
            return http_fetch(realm, fetch_params);
        }
    };

    if (recursive == Recursive::Yes) {
        // 12. If response is null, then set response to the result of running the steps corresponding to the first
        //     matching statement:
        auto pending_response = !response
            ? TRY(get_response())
            : PendingResponse::create(vm, request, *response);

        // 13. If recursive is true, then return response.
        return pending_response;
    }

    // 11. If recursive is false, then run the remaining steps in parallel.
    Platform::EventLoopPlugin::the().deferred_invoke([&realm, &vm, &fetch_params, request, response, get_response = move(get_response)] {
        // 12. If response is null, then set response to the result of running the steps corresponding to the first
        //     matching statement:
        auto pending_response = PendingResponse::create(vm, request, Infrastructure::Response::create(vm));
        if (!response) {
            auto pending_response_or_error = get_response();
            if (pending_response_or_error.is_error())
                return;
            pending_response = pending_response_or_error.release_value();
        }
        pending_response->when_loaded([&realm, &vm, &fetch_params, request, response, response_was_null = !response](JS::NonnullGCPtr<Infrastructure::Response> resolved_response) mutable {
            dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'main fetch' pending_response load callback");
            if (response_was_null)
                response = resolved_response;
            // 14. If response is not a network error and response is not a filtered response, then:
            if (!response->is_network_error() && !is<Infrastructure::FilteredResponse>(*response)) {
                // 1. If request’s response tainting is "cors", then:
                if (request->response_tainting() == Infrastructure::Request::ResponseTainting::CORS) {
                    // 1. Let headerNames be the result of extracting header list values given
                    //    `Access-Control-Expose-Headers` and response’s header list.
                    auto header_names_or_failure = TRY_OR_IGNORE(Infrastructure::extract_header_list_values("Access-Control-Expose-Headers"sv.bytes(), response->header_list()));
                    auto header_names = header_names_or_failure.has<Vector<ByteBuffer>>() ? header_names_or_failure.get<Vector<ByteBuffer>>() : Vector<ByteBuffer> {};

                    // 2. If request’s credentials mode is not "include" and headerNames contains `*`, then set
                    //    response’s CORS-exposed header-name list to all unique header names in response’s header
                    //    list.
                    if (request->credentials_mode() != Infrastructure::Request::CredentialsMode::Include && header_names.contains_slow("*"sv.bytes())) {
                        auto unique_header_names = TRY_OR_IGNORE(response->header_list()->unique_names());
                        response->set_cors_exposed_header_name_list(move(unique_header_names));
                    }
                    // 3. Otherwise, if headerNames is not null or failure, then set response’s CORS-exposed
                    //    header-name list to headerNames.
                    else if (!header_names.is_empty()) {
                        response->set_cors_exposed_header_name_list(move(header_names));
                    }
                }

                // 2. Set response to the following filtered response with response as its internal response, depending
                //    on request’s response tainting:
                response = TRY_OR_IGNORE([&]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<Infrastructure::Response>> {
                    switch (request->response_tainting()) {
                    // -> "basic"
                    case Infrastructure::Request::ResponseTainting::Basic:
                        // basic filtered response
                        return TRY_OR_THROW_OOM(vm, Infrastructure::BasicFilteredResponse::create(vm, *response));
                    // -> "cors"
                    case Infrastructure::Request::ResponseTainting::CORS:
                        // CORS filtered response
                        return TRY_OR_THROW_OOM(vm, Infrastructure::CORSFilteredResponse::create(vm, *response));
                    // -> "opaque"
                    case Infrastructure::Request::ResponseTainting::Opaque:
                        // opaque filtered response
                        return Infrastructure::OpaqueFilteredResponse::create(vm, *response);
                    default:
                        VERIFY_NOT_REACHED();
                    }
                }());
            }

            // 15. Let internalResponse be response, if response is a network error, and response’s internal response
            //     otherwise.
            auto internal_response = response->is_network_error()
                ? JS::NonnullGCPtr { *response }
                : static_cast<Infrastructure::FilteredResponse&>(*response).internal_response();

            // 16. If internalResponse’s URL list is empty, then set it to a clone of request’s URL list.
            // NOTE: A response’s URL list can be empty (for example, when the response represents an about URL).
            if (internal_response->url_list().is_empty())
                internal_response->set_url_list(request->url_list());

            // 17. If request has a redirect-tainted origin, then set internalResponse’s has-cross-origin-redirects to true.
            if (request->has_redirect_tainted_origin())
                internal_response->set_has_cross_origin_redirects(true);

            // 18. If request’s timing allow failed flag is unset, then set internalResponse’s timing allow passed flag.
            if (!request->timing_allow_failed())
                internal_response->set_timing_allow_passed(true);

            // 19. If response is not a network error and any of the following returns blocked
            if (!response->is_network_error() && (
                    // FIXME: - should internalResponse to request be blocked as mixed content
                    false
                    // FIXME: - should internalResponse to request be blocked by Content Security Policy
                    || false
                    // - should internalResponse to request be blocked due to its MIME type
                    || TRY_OR_IGNORE(Infrastructure::should_response_to_request_be_blocked_due_to_its_mime_type(internal_response, request)) == Infrastructure::RequestOrResponseBlocking::Blocked
                    // - should internalResponse to request be blocked due to nosniff
                    || TRY_OR_IGNORE(Infrastructure::should_response_to_request_be_blocked_due_to_nosniff(internal_response, request)) == Infrastructure::RequestOrResponseBlocking::Blocked)) {
                // then set response and internalResponse to a network error.
                response = internal_response = Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Response was blocked"_string));
            }

            // 20. If response’s type is "opaque", internalResponse’s status is 206, internalResponse’s range-requested
            //     flag is set, and request’s header list does not contain `Range`, then set response and
            //     internalResponse to a network error.
            // NOTE: Traditionally, APIs accept a ranged response even if a range was not requested. This prevents a
            //       partial response from an earlier ranged request being provided to an API that did not make a range
            //       request.
            if (response->type() == Infrastructure::Response::Type::Opaque
                && internal_response->status() == 206
                && internal_response->range_requested()
                && !request->header_list()->contains("Range"sv.bytes())) {
                response = internal_response = Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Response has status 206 and 'range-requested' flag set, but request has no 'Range' header"_string));
            }

            // 21. If response is not a network error and either request’s method is `HEAD` or `CONNECT`, or
            //     internalResponse’s status is a null body status, set internalResponse’s body to null and disregard
            //     any enqueuing toward it (if any).
            // NOTE: This standardizes the error handling for servers that violate HTTP.
            if (!response->is_network_error() && (StringView { request->method() }.is_one_of("HEAD"sv, "CONNECT"sv) || Infrastructure::is_null_body_status(internal_response->status())))
                internal_response->set_body({});

            // 22. If request’s integrity metadata is not the empty string, then:
            if (!request->integrity_metadata().is_empty()) {
                // 1. Let processBodyError be this step: run fetch response handover given fetchParams and a network
                //    error.
                Infrastructure::Body::ProcessBodyErrorCallback process_body_error = [&realm, &vm, &fetch_params](auto) {
                    TRY_OR_IGNORE(fetch_response_handover(realm, fetch_params, Infrastructure::Response::network_error(vm, "Response body could not be processed"sv)));
                };

                // 2. If response’s body is null, then run processBodyError and abort these steps.
                if (!response->body().has_value()) {
                    process_body_error({});
                    return;
                }

                // 3. Let processBody given bytes be these steps:
                Infrastructure::Body::ProcessBodyCallback process_body = [&realm, request, response, &fetch_params, process_body_error = move(process_body_error)](ByteBuffer bytes) {
                    // 1. If bytes do not match request’s integrity metadata, then run processBodyError and abort these steps.
                    if (!TRY_OR_IGNORE(SRI::do_bytes_match_metadata_list(bytes, request->integrity_metadata()))) {
                        process_body_error({});
                        return;
                    }

                    // 2. Set response’s body to bytes as a body.
                    response->set_body(TRY_OR_IGNORE(Infrastructure::byte_sequence_as_body(realm, bytes)));

                    // 3. Run fetch response handover given fetchParams and response.
                    TRY_OR_IGNORE(fetch_response_handover(realm, fetch_params, *response));
                };

                // 4. Fully read response’s body given processBody and processBodyError.
                TRY_OR_IGNORE(response->body()->fully_read(realm, move(process_body), move(process_body_error), fetch_params.task_destination()));
            }
            // 23. Otherwise, run fetch response handover given fetchParams and response.
            else {
                TRY_OR_IGNORE(fetch_response_handover(realm, fetch_params, *response));
            }
        });
    });

    return Optional<JS::NonnullGCPtr<PendingResponse>> {};
}

// https://fetch.spec.whatwg.org/#fetch-finale
WebIDL::ExceptionOr<void> fetch_response_handover(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params, Infrastructure::Response& response)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'fetch response handover' with: fetch_params @ {}, response @ {}", &fetch_params, &response);

    auto& vm = realm.vm();

    // 1. Let timingInfo be fetchParams’s timing info.
    auto timing_info = fetch_params.timing_info();

    // 2. If response is not a network error and fetchParams’s request’s client is a secure context, then set
    //    timingInfo’s server-timing headers to the result of getting, decoding, and splitting `Server-Timing` from
    //    response’s header list.
    //    The user agent may decide to expose `Server-Timing` headers to non-secure contexts requests as well.
    auto client = fetch_params.request()->client();
    if (!response.is_network_error() && client != nullptr && HTML::is_secure_context(*client)) {
        auto server_timing_headers = TRY_OR_THROW_OOM(vm, response.header_list()->get_decode_and_split("Server-Timing"sv.bytes()));
        if (server_timing_headers.has_value())
            timing_info->set_server_timing_headers(server_timing_headers.release_value());
    }

    // 3. Let processResponseEndOfBody be the following steps:
    auto process_response_end_of_body = [&vm, &response, &fetch_params, timing_info] {
        // 1. Let unsafeEndTime be the unsafe shared current time.
        auto unsafe_end_time = HighResolutionTime::unsafe_shared_current_time();

        // 2. If fetchParams’s request’s destination is "document", then set fetchParams’s controller’s full timing
        //    info to fetchParams’s timing info.
        if (fetch_params.request()->destination() == Infrastructure::Request::Destination::Document)
            fetch_params.controller()->set_full_timing_info(fetch_params.timing_info());

        // 3. Set fetchParams’s controller’s report timing steps to the following steps given a global object global:
        fetch_params.controller()->set_report_timing_steps([&vm, &response, &fetch_params, timing_info, unsafe_end_time](JS::Object const& global) mutable {
            // 1. If fetchParams’s request’s URL’s scheme is not an HTTP(S) scheme, then return.
            if (!Infrastructure::is_http_or_https_scheme(fetch_params.request()->url().scheme()))
                return;

            // 2. Set timingInfo’s end time to the relative high resolution time given unsafeEndTime and global.
            timing_info->set_end_time(HighResolutionTime::relative_high_resolution_time(unsafe_end_time, global));

            // 3. Let cacheState be response’s cache state.
            auto cache_state = response.cache_state();

            // 4. Let bodyInfo be response’s body info.
            auto body_info = response.body_info();

            // 5. If response’s timing allow passed flag is not set, then set timingInfo to the result of creating an
            //    opaque timing info for timingInfo, set bodyInfo to a new response body info, and set cacheState to
            //    the empty string.
            // NOTE: This covers the case of response being a network error.
            if (!response.timing_allow_passed()) {
                timing_info = Infrastructure::create_opaque_timing_info(vm, timing_info);
                body_info = Infrastructure::Response::BodyInfo {};
                cache_state = {};
            }

            // 6. Let responseStatus be 0 if fetchParams’s request’s mode is "navigate" and response’s has-cross-origin-redirects is true; otherwise response’s status.
            auto response_status = fetch_params.request()->mode() == Infrastructure::Request::Mode::Navigate && response.has_cross_origin_redirects()
                ? 0
                : response.status();

            // FIXME: 7. If fetchParams’s request’s initiator type is not null, then mark resource timing given timingInfo,
            //           request’s URL, request’s initiator type, global, cacheState, bodyInfo, and responseStatus.
            (void)timing_info;
            (void)global;
            (void)cache_state;
            (void)body_info;
            (void)response_status;
        });

        // 4. Let processResponseEndOfBodyTask be the following steps:
        auto process_response_end_of_body_task = [&fetch_params, &response] {
            // 1. Set fetchParams’s request’s done flag.
            fetch_params.request()->set_done(true);

            // 2. If fetchParams’s process response end-of-body is non-null, then run fetchParams’s process response
            //    end-of-body given response.
            if (fetch_params.algorithms()->process_response_end_of_body().has_value())
                (*fetch_params.algorithms()->process_response_end_of_body())(response);

            // 3. If fetchParams’s request’s initiator type is non-null and fetchParams’s request’s client’s global
            //    object is fetchParams’s task destination, then run fetchParams’s controller’s report timing steps
            //    given fetchParams’s request’s client’s global object.
            auto client = fetch_params.request()->client();
            auto const* task_destination_global_object = fetch_params.task_destination().get_pointer<JS::NonnullGCPtr<JS::Object>>();
            if (client != nullptr && task_destination_global_object != nullptr) {
                if (fetch_params.request()->initiator_type().has_value() && &client->global_object() == task_destination_global_object->ptr())
                    fetch_params.controller()->report_timing(client->global_object());
            }
        };

        // FIXME: Handle 'parallel queue' task destination
        auto task_destination = fetch_params.task_destination().get<JS::NonnullGCPtr<JS::Object>>();

        // 5. Queue a fetch task to run processResponseEndOfBodyTask with fetchParams’s task destination.
        Infrastructure::queue_fetch_task(task_destination, move(process_response_end_of_body_task));
    };

    // FIXME: Handle 'parallel queue' task destination
    auto task_destination = fetch_params.task_destination().get<JS::NonnullGCPtr<JS::Object>>();

    // 4. If fetchParams’s process response is non-null, then queue a fetch task to run fetchParams’s process response
    //    given response, with fetchParams’s task destination.
    if (fetch_params.algorithms()->process_response().has_value()) {
        Infrastructure::queue_fetch_task(task_destination, [&fetch_params, &response]() {
            (*fetch_params.algorithms()->process_response())(response);
        });
    }

    // 5. Let internalResponse be response, if response is a network error; otherwise response’s internal response.
    auto internal_response = response.is_network_error() ? JS::NonnullGCPtr { response } : response.unsafe_response();

    // 6. If internalResponse’s body is null, then run processResponseEndOfBody.
    if (!internal_response->body().has_value()) {
        process_response_end_of_body();
    }
    // 7. Otherwise:
    else {
        // FIXME: 1. Let transformStream be a new TransformStream.
        // FIXME: 2. Let identityTransformAlgorithm be an algorithm which, given chunk, enqueues chunk in transformStream.
        // FIXME: 3. Set up transformStream with transformAlgorithm set to identityTransformAlgorithm and flushAlgorithm set
        //           to processResponseEndOfBody.
        // FIXME: 4. Set internalResponse’s body’s stream to the result of internalResponse’s body’s stream piped through transformStream.
    }

    // 8. If fetchParams’s process response consume body is non-null, then:
    if (fetch_params.algorithms()->process_response_consume_body().has_value()) {
        // 1. Let processBody given nullOrBytes be this step: run fetchParams’s process response consume body given
        //    response and nullOrBytes.
        auto process_body = [&fetch_params, &response](Variant<ByteBuffer, Empty> const& null_or_bytes) {
            (*fetch_params.algorithms()->process_response_consume_body())(response, null_or_bytes);
        };

        // 2. Let processBodyError be this step: run fetchParams’s process response consume body given response and
        //    failure.
        auto process_body_error = [&fetch_params, &response](auto) {
            (*fetch_params.algorithms()->process_response_consume_body())(response, Infrastructure::FetchAlgorithms::ConsumeBodyFailureTag {});
        };

        // 3. If internalResponse's body is null, then queue a fetch task to run processBody given null, with
        //    fetchParams’s task destination.
        if (!internal_response->body().has_value()) {
            Infrastructure::queue_fetch_task(task_destination, [process_body = move(process_body)]() {
                process_body({});
            });
        }
        // 4. Otherwise, fully read internalResponse body given processBody, processBodyError, and fetchParams’s task
        //    destination.
        else {
            TRY(internal_response->body()->fully_read(realm, move(process_body), move(process_body_error), fetch_params.task_destination()));
        }
    }

    return {};
}

// https://fetch.spec.whatwg.org/#concept-scheme-fetch
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> scheme_fetch(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'scheme fetch' with: fetch_params @ {}", &fetch_params);

    auto& vm = realm.vm();

    // 1. If fetchParams is canceled, then return the appropriate network error for fetchParams.
    if (fetch_params.is_canceled())
        return PendingResponse::create(vm, fetch_params.request(), Infrastructure::Response::appropriate_network_error(vm, fetch_params));

    // 2. Let request be fetchParams’s request.
    auto request = fetch_params.request();

    // 3. Switch on request’s current URL’s scheme and run the associated steps:
    // -> "about"
    if (request->current_url().scheme() == "about"sv) {
        // If request’s current URL’s path is the string "blank", then return a new response whose status message is
        // `OK`, header list is « (`Content-Type`, `text/html;charset=utf-8`) », and body is the empty byte sequence as
        // a body.
        // NOTE: URLs such as "about:config" are handled during navigation and result in a network error in the context
        //       of fetching.
        if (request->current_url().serialize_path() == "blank"sv) {
            auto response = Infrastructure::Response::create(vm);
            response->set_status_message(MUST(ByteBuffer::copy("OK"sv.bytes())));
            auto header = MUST(Infrastructure::Header::from_string_pair("Content-Type"sv, "text/html;charset=utf-8"sv));
            TRY_OR_THROW_OOM(vm, response->header_list()->append(move(header)));
            response->set_body(MUST(Infrastructure::byte_sequence_as_body(realm, ""sv.bytes())));
            return PendingResponse::create(vm, request, response);
        }
    }
    // -> "blob"
    else if (request->current_url().scheme() == "blob"sv) {
        auto const& store = FileAPI::blob_url_store();

        // 1. Let blobURLEntry be request’s current URL’s blob URL entry.
        auto blob_url_entry = store.get(TRY_OR_THROW_OOM(vm, request->current_url().to_string()));

        // 2. If request’s method is not `GET`, blobURLEntry is null, or blobURLEntry’s object is not a Blob object,
        //    then return a network error. [FILEAPI]
        if (request->method() != "GET"sv.bytes() || !blob_url_entry.has_value()) {
            // FIXME: Handle "blobURLEntry’s object is not a Blob object". It could be a MediaSource object, but we
            //        have not yet implemented the Media Source Extensions spec.
            return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request has an invalid 'blob:' URL"sv));
        }

        // 3. Let blob be blobURLEntry’s object.
        auto const& blob = blob_url_entry->object;

        // 4. Let response be a new response.
        auto response = Infrastructure::Response::create(vm);

        // 5. Let fullLength be blob’s size.
        auto full_length = blob->size();

        // 6. Let serializedFullLength be fullLength, serialized and isomorphic encoded.
        auto serialized_full_length = TRY_OR_THROW_OOM(vm, String::number(full_length));

        // 7. Let type be blob’s type.
        auto const& type = blob->type();

        // 8. If request’s header list does not contain `Range`:
        if (!request->header_list()->contains("Range"sv.bytes())) {
            // 1. Let bodyWithType be the result of safely extracting blob.
            auto body_with_type = TRY(safely_extract_body(realm, blob));

            // 2. Set response’s status message to `OK`.
            response->set_status_message(MUST(ByteBuffer::copy("OK"sv.bytes())));

            // 3. Set response’s body to bodyWithType’s body.
            response->set_body(move(body_with_type.body));

            // 4. Set response’s header list to « (`Content-Length`, serializedFullLength), (`Content-Type`, type) ».
            auto content_length_header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Content-Length"sv, serialized_full_length));
            TRY_OR_THROW_OOM(vm, response->header_list()->append(move(content_length_header)));

            auto content_type_header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Content-Type"sv, type));
            TRY_OR_THROW_OOM(vm, response->header_list()->append(move(content_type_header)));
        }
        // FIXME: 9. Otherwise:
        else {
            // 1. Set response’s range-requested flag.
            // 2. Let rangeHeader be the result of getting `Range` from request’s header list.
            // 3. Let rangeValue be the result of parsing a single range header value given rangeHeader and true.
            // 4. If rangeValue is failure, then return a network error.
            // 5. Let (rangeStart, rangeEnd) be rangeValue.
            // 6. If rangeStart is null:
            //     1. Set rangeStart to fullLength − rangeEnd.
            //     2. Set rangeEnd to rangeStart + rangeEnd − 1.
            // 7. Otherwise:
            //     1. If rangeStart is greater than or equal to fullLength, then return a network error.
            //     2. If rangeEnd is null or rangeEnd is greater than or equal to fullLength, then set rangeEnd to fullLength − 1.
            // 8. Let slicedBlob be the result of invoking slice blob given blob, rangeStart, rangeEnd + 1, and type.
            // 9. Let slicedBodyWithType be the result of safely extracting slicedBlob.
            // 10. Set response’s body to slicedBodyWithType’s body.
            // 11. Let serializedSlicedLength be slicedBlob’s size, serialized and isomorphic encoded.
            // 12. Let contentRange be `bytes `.
            // 13. Append rangeStart, serialized and isomorphic encoded, to contentRange.
            // 14. Append 0x2D (-) to contentRange.
            // 15. Append rangeEnd, serialized and isomorphic encoded to contentRange.
            // 16. Append 0x2F (/) to contentRange.
            // 17. Append serializedFullLength to contentRange.
            // 18. Set response’s status to 206.
            // 19. Set response’s status message to `Partial Content`.
            // 20. Set response’s header list to « (`Content-Length`, serializedSlicedLength), (`Content-Type`, type), (`Content-Range`, contentRange) ».
            return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request has a 'blob:' URL with a Content-Range header, which is currently unsupported"sv));
        }

        // 10. Return response.
        return PendingResponse::create(vm, request, response);
    }
    // -> "data"
    else if (request->current_url().scheme() == "data"sv) {
        // 1. Let dataURLStruct be the result of running the data: URL processor on request’s current URL.
        auto data_url_struct = request->current_url().process_data_url();

        // 2. If dataURLStruct is failure, then return a network error.
        if (data_url_struct.is_error())
            return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Failed to process 'data:' URL"sv));

        // 3. Let mimeType be dataURLStruct’s MIME type, serialized.
        //    FIXME: Serialize MIME type.
        auto const& mime_type = data_url_struct.value().mime_type;

        // 4. Return a new response whose status message is `OK`, header list is « (`Content-Type`, mimeType) », and
        //    body is dataURLStruct’s body as a body.
        auto response = Infrastructure::Response::create(vm);
        response->set_status_message(MUST(ByteBuffer::copy("OK"sv.bytes())));
        auto header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Content-Type"sv, mime_type));
        TRY_OR_THROW_OOM(vm, response->header_list()->append(move(header)));
        response->set_body(TRY(Infrastructure::byte_sequence_as_body(realm, data_url_struct.value().body)));
        return PendingResponse::create(vm, request, response);
    }
    // -> "file"
    else if (request->current_url().scheme() == "file"sv) {
        // For now, unfortunate as it is, file: URLs are left as an exercise for the reader.
        // When in doubt, return a network error.
        return TRY(nonstandard_resource_loader_file_or_http_network_fetch(realm, fetch_params));
    }
    // -> HTTP(S) scheme
    else if (Infrastructure::is_http_or_https_scheme(request->current_url().scheme())) {
        // Return the result of running HTTP fetch given fetchParams.
        return http_fetch(realm, fetch_params);
    }

    // 4. Return a network error.
    auto message = request->current_url().scheme() == "about"sv
        ? TRY_OR_THROW_OOM(vm, "Request has invalid 'about:' URL, only 'about:blank' can be fetched"_string)
        : TRY_OR_THROW_OOM(vm, "Request URL has invalid scheme, must be one of 'about', 'blob', 'data', 'file', 'http', or 'https'"_string);
    return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, move(message)));
}

// https://fetch.spec.whatwg.org/#concept-http-fetch
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> http_fetch(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params, MakeCORSPreflight make_cors_preflight)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP fetch' with: fetch_params @ {}, make_cors_preflight = {}",
        &fetch_params, make_cors_preflight == MakeCORSPreflight::Yes ? "Yes"sv : "No"sv);

    auto& vm = realm.vm();

    // 1. Let request be fetchParams’s request.
    auto request = fetch_params.request();

    // 2. Let response be null.
    JS::GCPtr<Infrastructure::Response> response;

    // 3. Let actualResponse be null.
    JS::GCPtr<Infrastructure::Response> actual_response;

    // 4. If request’s service-workers mode is "all", then:
    if (request->service_workers_mode() == Infrastructure::Request::ServiceWorkersMode::All) {
        // 1. Let requestForServiceWorker be a clone of request.
        auto request_for_service_worker = TRY(request->clone(realm));

        // 2. If requestForServiceWorker’s body is non-null, then:
        if (!request_for_service_worker->body().has<Empty>()) {
            // FIXME: 1. Let transformStream be a new TransformStream.
            // FIXME: 2. Let transformAlgorithm given chunk be these steps:
            // FIXME: 3. Set up transformStream with transformAlgorithm set to transformAlgorithm.
            // FIXME: 4. Set requestForServiceWorker’s body’s stream to the result of requestForServiceWorker’s body’s stream
            //           piped through transformStream.
        }

        // 3. Let serviceWorkerStartTime be the coarsened shared current time given fetchParams’s cross-origin isolated
        //    capability.
        auto service_worker_start_time = HighResolutionTime::coarsened_shared_current_time(fetch_params.cross_origin_isolated_capability() == HTML::CanUseCrossOriginIsolatedAPIs::Yes);

        // FIXME: 4. Set response to the result of invoking handle fetch for requestForServiceWorker, with fetchParams’s
        //           controller and fetchParams’s cross-origin isolated capability.

        // 5. If response is not null, then:
        if (response) {
            // 1. Set fetchParams’s timing info’s final service worker start time to serviceWorkerStartTime.
            fetch_params.timing_info()->set_final_service_worker_start_time(service_worker_start_time);

            // 2. If request’s body is non-null, then cancel request’s body with undefined.
            if (!request->body().has<Empty>()) {
                // FIXME: Implement cancelling streams
            }

            // 3. Set actualResponse to response, if response is not a filtered response, and to response’s internal
            //    response otherwise.
            actual_response = !is<Infrastructure::FilteredResponse>(*response)
                ? JS::NonnullGCPtr { *response }
                : static_cast<Infrastructure::FilteredResponse const&>(*response).internal_response();

            // 4. If one of the following is true
            if (
                // - response’s type is "error"
                response->type() == Infrastructure::Response::Type::Error
                // - request’s mode is "same-origin" and response’s type is "cors"
                || (request->mode() == Infrastructure::Request::Mode::SameOrigin && response->type() == Infrastructure::Response::Type::CORS)
                // - request’s mode is not "no-cors" and response’s type is "opaque"
                || (request->mode() != Infrastructure::Request::Mode::NoCORS && response->type() == Infrastructure::Response::Type::Opaque)
                // - request’s redirect mode is not "manual" and response’s type is "opaqueredirect"
                || (request->redirect_mode() != Infrastructure::Request::RedirectMode::Manual && response->type() == Infrastructure::Response::Type::OpaqueRedirect)
                // - request’s redirect mode is not "follow" and response’s URL list has more than one item.
                || (request->redirect_mode() != Infrastructure::Request::RedirectMode::Follow && response->url_list().size() > 1)) {
                // then return a network error.
                return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Invalid request/response state combination"sv));
            }
        }
    }

    JS::GCPtr<PendingResponse> pending_actual_response;

    auto returned_pending_response = PendingResponse::create(vm, request);

    // 5. If response is null, then:
    if (!response) {
        // 1. If makeCORSPreflight is true and one of these conditions is true:
        // NOTE: This step checks the CORS-preflight cache and if there is no suitable entry it performs a
        //       CORS-preflight fetch which, if successful, populates the cache. The purpose of the CORS-preflight
        //       fetch is to ensure the fetched resource is familiar with the CORS protocol. The cache is there to
        //       minimize the number of CORS-preflight fetches.
        JS::GCPtr<PendingResponse> pending_preflight_response;
        if (make_cors_preflight == MakeCORSPreflight::Yes && (
                // - There is no method cache entry match for request’s method using request, and either request’s
                //   method is not a CORS-safelisted method or request’s use-CORS-preflight flag is set.
                //   FIXME: We currently have no cache, so there will always be no method cache entry.
                (!Infrastructure::is_cors_safelisted_method(request->method()) || request->use_cors_preflight())
                // - There is at least one item in the CORS-unsafe request-header names with request’s header list for
                //   which there is no header-name cache entry match using request.
                //   FIXME: We currently have no cache, so there will always be no header-name cache entry.
                || !TRY_OR_THROW_OOM(vm, Infrastructure::get_cors_unsafe_header_names(request->header_list())).is_empty())) {
            // 1. Let preflightResponse be the result of running CORS-preflight fetch given request.
            pending_preflight_response = TRY(cors_preflight_fetch(realm, request));

            // NOTE: Step 2 is performed in pending_preflight_response's load callback below.
        }

        auto fetch_main_content = [request = JS::make_handle(request), realm = JS::make_handle(realm), fetch_params = JS::make_handle(fetch_params)]() -> WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> {
            // 2. If request’s redirect mode is "follow", then set request’s service-workers mode to "none".
            // NOTE: Redirects coming from the network (as opposed to from a service worker) are not to be exposed to a
            //       service worker.
            if (request->redirect_mode() == Infrastructure::Request::RedirectMode::Follow)
                request->set_service_workers_mode(Infrastructure::Request::ServiceWorkersMode::None);

            // 3. Set response and actualResponse to the result of running HTTP-network-or-cache fetch given fetchParams.
            return http_network_or_cache_fetch(*realm, *fetch_params);
        };

        if (pending_preflight_response) {
            pending_actual_response = PendingResponse::create(vm, request);
            pending_preflight_response->when_loaded([returned_pending_response, pending_actual_response, fetch_main_content = move(fetch_main_content)](JS::NonnullGCPtr<Infrastructure::Response> preflight_response) {
                dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP fetch' pending_preflight_response load callback");

                // 2. If preflightResponse is a network error, then return preflightResponse.
                if (preflight_response->is_network_error()) {
                    returned_pending_response->resolve(preflight_response);
                    return;
                }

                auto pending_main_content_response = TRY_OR_IGNORE(fetch_main_content());
                pending_main_content_response->when_loaded([pending_actual_response](JS::NonnullGCPtr<Infrastructure::Response> main_content_response) {
                    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP fetch' pending_main_content_response load callback");
                    pending_actual_response->resolve(main_content_response);
                });
            });
        } else {
            pending_actual_response = TRY(fetch_main_content());
        }
    } else {
        pending_actual_response = PendingResponse::create(vm, request, Infrastructure::Response::create(vm));
    }

    pending_actual_response->when_loaded([&realm, &vm, &fetch_params, request, response, actual_response, returned_pending_response, response_was_null = !response](JS::NonnullGCPtr<Infrastructure::Response> resolved_actual_response) mutable {
        dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP fetch' pending_actual_response load callback");
        if (response_was_null) {
            response = actual_response = resolved_actual_response;
            // 4. If request’s response tainting is "cors" and a CORS check for request and response returns failure,
            //    then return a network error.
            // NOTE: As the CORS check is not to be applied to responses whose status is 304 or 407, or responses from
            //       a service worker for that matter, it is applied here.
            if (request->response_tainting() == Infrastructure::Request::ResponseTainting::CORS
                && !TRY_OR_IGNORE(cors_check(request, *response))) {
                returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Request with 'cors' response tainting failed CORS check"_string)));
                return;
            }

            // 5. If the TAO check for request and response returns failure, then set request’s timing allow failed flag.
            if (!TRY_OR_IGNORE(tao_check(request, *response)))
                request->set_timing_allow_failed(true);
        }

        // 6. If either request’s response tainting or response’s type is "opaque", and the cross-origin resource
        //    policy check with request’s origin, request’s client, request’s destination, and actualResponse returns
        //    blocked, then return a network error.
        // NOTE: The cross-origin resource policy check runs for responses coming from the network and responses coming
        //       from the service worker. This is different from the CORS check, as request’s client and the service
        //       worker can have different embedder policies.
        if ((request->response_tainting() == Infrastructure::Request::ResponseTainting::Opaque || response->type() == Infrastructure::Response::Type::Opaque)
            && false // FIXME: "and the cross-origin resource policy check with request’s origin, request’s client, request’s destination, and actualResponse returns blocked"
        ) {
            returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Response was blocked by cross-origin resource policy check"_string)));
            return;
        }

        Optional<JS::NonnullGCPtr<PendingResponse>> inner_pending_response;

        // 7. If actualResponse’s status is a redirect status, then:
        if (Infrastructure::is_redirect_status(actual_response->status())) {
            // FIXME: 1. If actualResponse’s status is not 303, request’s body is not null, and the connection uses HTTP/2,
            //           then user agents may, and are even encouraged to, transmit an RST_STREAM frame.
            // NOTE: 303 is excluded as certain communities ascribe special status to it.

            // 2. Switch on request’s redirect mode:
            switch (request->redirect_mode()) {
            // -> "error"
            case Infrastructure::Request::RedirectMode::Error:
                // Set response to a network error.
                response = Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Request with 'error' redirect mode received redirect response"_string));
                break;
            // -> "manual"
            case Infrastructure::Request::RedirectMode::Manual:
                // 1. If request’s mode is "navigate", then set fetchParams’s controller’s next manual redirect steps
                //    to run HTTP-redirect fetch given fetchParams and response.
                if (request->mode() == Infrastructure::Request::Mode::Navigate) {
                    fetch_params.controller()->set_next_manual_redirect_steps([&realm, &fetch_params, response] {
                        (void)http_redirect_fetch(realm, fetch_params, *response);
                    });
                }
                // 2. Otherwise, set response to an opaque-redirect filtered response whose internal response is
                //    actualResponse.
                else {
                    response = Infrastructure::OpaqueRedirectFilteredResponse::create(vm, *actual_response);
                }
                break;
            // -> "follow"
            case Infrastructure::Request::RedirectMode::Follow:
                // Set response to the result of running HTTP-redirect fetch given fetchParams and response.
                inner_pending_response = TRY_OR_IGNORE(http_redirect_fetch(realm, fetch_params, *response));
                break;
            default:
                VERIFY_NOT_REACHED();
            }
        }

        if (inner_pending_response.has_value()) {
            inner_pending_response.value()->when_loaded([returned_pending_response](JS::NonnullGCPtr<Infrastructure::Response> response) {
                dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP fetch' inner_pending_response load callback");
                returned_pending_response->resolve(response);
            });
        } else {
            returned_pending_response->resolve(*response);
        }
    });

    // 8. Return response.
    // NOTE: Typically actualResponse’s body’s stream is still being enqueued to after returning.
    return returned_pending_response;
}

// https://fetch.spec.whatwg.org/#concept-http-redirect-fetch
WebIDL::ExceptionOr<Optional<JS::NonnullGCPtr<PendingResponse>>> http_redirect_fetch(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params, Infrastructure::Response& response)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP-redirect fetch' with: fetch_params @ {}, response = {}", &fetch_params, &response);

    auto& vm = realm.vm();

    // 1. Let request be fetchParams’s request.
    auto request = fetch_params.request();

    // 2. Let actualResponse be response, if response is not a filtered response, and response’s internal response
    //    otherwise.
    auto actual_response = !is<Infrastructure::FilteredResponse>(response)
        ? JS::NonnullGCPtr { response }
        : static_cast<Infrastructure::FilteredResponse const&>(response).internal_response();

    // 3. Let locationURL be actualResponse’s location URL given request’s current URL’s fragment.
    auto const& fragment = request->current_url().fragment();
    auto fragment_string = fragment.is_null() ? Optional<String> {} : TRY_OR_THROW_OOM(vm, String::from_deprecated_string(fragment));
    auto location_url_or_error = actual_response->location_url(fragment_string);

    // 4. If locationURL is null, then return response.
    if (!location_url_or_error.is_error() && !location_url_or_error.value().has_value())
        return PendingResponse::create(vm, request, response);

    // 5. If locationURL is failure, then return a network error.
    if (location_url_or_error.is_error())
        return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request redirect URL is invalid"sv));

    auto location_url = location_url_or_error.release_value().release_value();

    // 6. If locationURL’s scheme is not an HTTP(S) scheme, then return a network error.
    if (!Infrastructure::is_http_or_https_scheme(location_url.scheme()))
        return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request redirect URL must have HTTP or HTTPS scheme"sv));

    // 7. If request’s redirect count is 20, then return a network error.
    if (request->redirect_count() == 20)
        return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request has reached maximum redirect count of 20"sv));

    // 8. Increase request’s redirect count by 1.
    request->set_redirect_count(request->redirect_count() + 1);

    // 8. If request’s mode is "cors", locationURL includes credentials, and request’s origin is not same origin with
    //    locationURL’s origin, then return a network error.
    if (request->mode() == Infrastructure::Request::Mode::CORS
        && location_url.includes_credentials()
        && request->origin().has<HTML::Origin>()
        && !request->origin().get<HTML::Origin>().is_same_origin(URL::url_origin(location_url))) {
        return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request with 'cors' mode and different URL and request origin must not include credentials in redirect URL"sv));
    }

    // 10. If request’s response tainting is "cors" and locationURL includes credentials, then return a network error.
    // NOTE: This catches a cross-origin resource redirecting to a same-origin URL.
    if (request->response_tainting() == Infrastructure::Request::ResponseTainting::CORS && location_url.includes_credentials())
        return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request with 'cors' response tainting must not include credentials in redirect URL"sv));

    // 11. If actualResponse’s status is not 303, request’s body is non-null, and request’s body’s source is null, then
    //     return a network error.
    if (actual_response->status() != 303
        && !request->body().has<Empty>()
        && request->body().get<Infrastructure::Body>().source().has<Empty>()) {
        return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request has body but no body source"sv));
    }

    // 12. If one of the following is true
    if (
        // - actualResponse’s status is 301 or 302 and request’s method is `POST`
        ((actual_response->status() == 301 || actual_response->status() == 302) && request->method() == "POST"sv.bytes())
        // - actualResponse’s status is 303 and request’s method is not `GET` or `HEAD`
        || (actual_response->status() == 303 && !(request->method() == "GET"sv.bytes() || request->method() == "HEAD"sv.bytes()))
        // then:
    ) {
        // 1. Set request’s method to `GET` and request’s body to null.
        request->set_method(MUST(ByteBuffer::copy("GET"sv.bytes())));
        request->set_body({});

        static constexpr Array request_body_header_names {
            "Content-Encoding"sv,
            "Content-Language"sv,
            "Content-Location"sv,
            "Content-Type"sv
        };
        // 2. For each headerName of request-body-header name, delete headerName from request’s header list.
        for (auto header_name : request_body_header_names.span())
            request->header_list()->delete_(header_name.bytes());
    }

    // 13. If request’s current URL’s origin is not same origin with locationURL’s origin, then for each headerName of
    //     CORS non-wildcard request-header name, delete headerName from request’s header list.
    // NOTE: I.e., the moment another origin is seen after the initial request, the `Authorization` header is removed.
    if (!URL::url_origin(request->current_url()).is_same_origin(URL::url_origin(location_url))) {
        static constexpr Array cors_non_wildcard_request_header_names {
            "Authorization"sv
        };
        for (auto header_name : cors_non_wildcard_request_header_names)
            request->header_list()->delete_(header_name.bytes());
    }

    // 14. If request’s body is non-null, then set request’s body to the body of the result of safely extracting
    //     request’s body’s source.
    // NOTE: request’s body’s source’s nullity has already been checked.
    if (!request->body().has<Empty>()) {
        auto const& source = request->body().get<Infrastructure::Body>().source();
        // NOTE: BodyInitOrReadableBytes is a superset of Body::SourceType
        auto converted_source = source.has<ByteBuffer>()
            ? BodyInitOrReadableBytes { source.get<ByteBuffer>() }
            : BodyInitOrReadableBytes { source.get<JS::Handle<FileAPI::Blob>>() };
        auto [body, _] = TRY(safely_extract_body(realm, converted_source));
        request->set_body(move(body));
    }

    // 15. Let timingInfo be fetchParams’s timing info.
    auto timing_info = fetch_params.timing_info();

    // 16. Set timingInfo’s redirect end time and post-redirect start time to the coarsened shared current time given
    //     fetchParams’s cross-origin isolated capability.
    auto now = HighResolutionTime::coarsened_shared_current_time(fetch_params.cross_origin_isolated_capability() == HTML::CanUseCrossOriginIsolatedAPIs::Yes);
    timing_info->set_redirect_end_time(now);
    timing_info->set_post_redirect_start_time(now);

    // 17. If timingInfo’s redirect start time is 0, then set timingInfo’s redirect start time to timingInfo’s start
    //     time.
    if (timing_info->redirect_start_time() == 0)
        timing_info->set_redirect_start_time(timing_info->start_time());

    // 18. Append locationURL to request’s URL list.
    request->url_list().append(location_url);

    // FIXME: 19. Invoke set request’s referrer policy on redirect on request and actualResponse.

    // 20. Let recursive be true.
    auto recursive = Recursive::Yes;

    // 21. If request’s redirect mode is "manual", then:
    if (request->redirect_mode() == Infrastructure::Request::RedirectMode::Manual) {
        // 1. Assert: request’s mode is "navigate".
        VERIFY(request->mode() == Infrastructure::Request::Mode::Navigate);

        // 2. Set recursive to false.
        recursive = Recursive::No;
    }

    // 22. Return the result of running main fetch given fetchParams and recursive.
    return main_fetch(realm, fetch_params, recursive);
}

// https://fetch.spec.whatwg.org/#concept-http-network-or-cache-fetch
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> http_network_or_cache_fetch(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params, IsAuthenticationFetch is_authentication_fetch, IsNewConnectionFetch is_new_connection_fetch)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP-network-or-cache fetch' with: fetch_params @ {}, is_authentication_fetch = {}, is_new_connection_fetch = {}",
        &fetch_params, is_authentication_fetch == IsAuthenticationFetch::Yes ? "Yes"sv : "No"sv, is_new_connection_fetch == IsNewConnectionFetch::Yes ? "Yes"sv : "No"sv);

    auto& vm = realm.vm();

    // 1. Let request be fetchParams’s request.
    auto request = fetch_params.request();

    // 2. Let httpFetchParams be null.
    JS::GCPtr<Infrastructure::FetchParams const> http_fetch_params;

    // 3. Let httpRequest be null.
    JS::GCPtr<Infrastructure::Request> http_request;

    // 4. Let response be null.
    JS::GCPtr<Infrastructure::Response> response;

    // 5. Let storedResponse be null.
    JS::GCPtr<Infrastructure::Response> stored_response;

    // 6. Let httpCache be null.
    // (Typeless until we actually implement it, needed for checks below)
    void* http_cache = nullptr;

    // 7. Let the revalidatingFlag be unset.
    auto revalidating_flag = RefCountedFlag::create(false);

    auto include_credentials = IncludeCredentials::No;

    // 8. Run these steps, but abort when fetchParams is canceled:
    // NOTE: There's an 'if aborted' check after this anyway, so not doing this is fine and only incurs a small delay.
    //       For now, support for aborting fetch requests is limited anyway as ResourceLoader doesn't support it.
    auto aborted = false;
    {
        ScopeGuard set_aborted = [&] {
            if (fetch_params.is_canceled())
                aborted = true;
        };

        // 1. If request’s window is "no-window" and request’s redirect mode is "error", then set httpFetchParams to
        //    fetchParams and httpRequest to request.
        if (request->window().has<Infrastructure::Request::Window>()
            && request->window().get<Infrastructure::Request::Window>() == Infrastructure::Request::Window::NoWindow
            && request->redirect_mode() == Infrastructure::Request::RedirectMode::Error) {
            http_fetch_params = fetch_params;
            http_request = request;
        }
        // 2. Otherwise:
        else {
            // 1. Set httpRequest to a clone of request.
            // NOTE: Implementations are encouraged to avoid teeing request’s body’s stream when request’s body’s
            //       source is null as only a single body is needed in that case. E.g., when request’s body’s source
            //       is null, redirects and authentication will end up failing the fetch.
            http_request = TRY(request->clone(realm));

            // 2. Set httpFetchParams to a copy of fetchParams.
            // 3. Set httpFetchParams’s request to httpRequest.
            auto new_http_fetch_params = Infrastructure::FetchParams::create(vm, *http_request, fetch_params.timing_info());
            new_http_fetch_params->set_algorithms(fetch_params.algorithms());
            new_http_fetch_params->set_task_destination(fetch_params.task_destination());
            new_http_fetch_params->set_cross_origin_isolated_capability(fetch_params.cross_origin_isolated_capability());
            new_http_fetch_params->set_preloaded_response_candidate(fetch_params.preloaded_response_candidate());
            http_fetch_params = new_http_fetch_params;
        }

        // 3. Let includeCredentials be true if one of
        if (
            // - request’s credentials mode is "include"
            request->credentials_mode() == Infrastructure::Request::CredentialsMode::Include
            // - request’s credentials mode is "same-origin" and request’s response tainting is "basic"
            || (request->credentials_mode() == Infrastructure::Request::CredentialsMode::SameOrigin
                && request->response_tainting() == Infrastructure::Request::ResponseTainting::Basic)
            // is true; otherwise false.
        ) {
            include_credentials = IncludeCredentials::Yes;
        } else {
            include_credentials = IncludeCredentials::No;
        }

        // 4. If Cross-Origin-Embedder-Policy allows credentials with request returns false, then set
        //    includeCredentials to false.
        if (!request->cross_origin_embedder_policy_allows_credentials())
            include_credentials = IncludeCredentials::No;

        // 5. Let contentLength be httpRequest’s body’s length, if httpRequest’s body is non-null; otherwise null.
        auto content_length = http_request->body().has<Infrastructure::Body>()
            ? http_request->body().get<Infrastructure::Body>().length()
            : Optional<u64> {};

        // 6. Let contentLengthHeaderValue be null.
        auto content_length_header_value = Optional<ByteBuffer> {};

        // 7. If httpRequest’s body is null and httpRequest’s method is `POST` or `PUT`, then set
        //    contentLengthHeaderValue to `0`.
        if (http_request->body().has<Empty>() && StringView { http_request->method() }.is_one_of("POST"sv, "PUT"sv))
            content_length_header_value = MUST(ByteBuffer::copy("0"sv.bytes()));

        // 8. If contentLength is non-null, then set contentLengthHeaderValue to contentLength, serialized and
        //    isomorphic encoded.
        if (content_length.has_value())
            content_length_header_value = MUST(ByteBuffer::copy(TRY_OR_THROW_OOM(vm, String::number(*content_length)).bytes()));

        // 9. If contentLengthHeaderValue is non-null, then append (`Content-Length`, contentLengthHeaderValue) to
        //    httpRequest’s header list.
        if (content_length_header_value.has_value()) {
            auto header = Infrastructure::Header {
                .name = MUST(ByteBuffer::copy("Content-Length"sv.bytes())),
                .value = content_length_header_value.release_value(),
            };
            TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
        }

        // FIXME: 10. If contentLength is non-null and httpRequest’s keepalive is true, then:
        if (content_length.has_value() && http_request->keepalive()) {
            // FIXME: 1-5., requires 'fetch records' and 'fetch group' concepts.
            // NOTE: The above limit ensures that requests that are allowed to outlive the environment settings object
            //       and contain a body, have a bounded size and are not allowed to stay alive indefinitely.
        }

        // 11. If httpRequest’s referrer is a URL, then:
        if (http_request->referrer().has<AK::URL>()) {
            // 1. Let referrerValue be httpRequest’s referrer, serialized and isomorphic encoded.
            auto referrer_value = TRY_OR_THROW_OOM(vm, ByteBuffer::copy(http_request->referrer().get<AK::URL>().serialize().bytes()));

            // 2. Append (`Referer`, referrerValue) to httpRequest’s header list.
            auto header = Infrastructure::Header {
                .name = MUST(ByteBuffer::copy("Referer"sv.bytes())),
                .value = move(referrer_value),
            };
            TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
        }

        // 12. Append a request `Origin` header for httpRequest.
        TRY_OR_THROW_OOM(vm, http_request->add_origin_header());

        // FIXME: 13. Append the Fetch metadata headers for httpRequest.

        // 14. If httpRequest’s header list does not contain `User-Agent`, then user agents should append
        //     (`User-Agent`, default `User-Agent` value) to httpRequest’s header list.
        if (!http_request->header_list()->contains("User-Agent"sv.bytes())) {
            auto header = Infrastructure::Header {
                .name = MUST(ByteBuffer::copy("User-Agent"sv.bytes())),
                .value = TRY_OR_THROW_OOM(vm, Infrastructure::default_user_agent_value()),
            };
            TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
        }

        // 15. If httpRequest’s cache mode is "default" and httpRequest’s header list contains `If-Modified-Since`,
        //     `If-None-Match`, `If-Unmodified-Since`, `If-Match`, or `If-Range`, then set httpRequest’s cache mode to
        //     "no-store".
        if (http_request->cache_mode() == Infrastructure::Request::CacheMode::Default
            && (http_request->header_list()->contains("If-Modified-Since"sv.bytes())
                || http_request->header_list()->contains("If-None-Match"sv.bytes())
                || http_request->header_list()->contains("If-Unmodified-Since"sv.bytes())
                || http_request->header_list()->contains("If-Match"sv.bytes())
                || http_request->header_list()->contains("If-Range"sv.bytes()))) {
            http_request->set_cache_mode(Infrastructure::Request::CacheMode::NoStore);
        }

        // 16. If httpRequest’s cache mode is "no-cache", httpRequest’s prevent no-cache cache-control header
        //     modification flag is unset, and httpRequest’s header list does not contain `Cache-Control`, then append
        //     (`Cache-Control`, `max-age=0`) to httpRequest’s header list.
        if (http_request->cache_mode() == Infrastructure::Request::CacheMode::NoCache
            && !http_request->prevent_no_cache_cache_control_header_modification()
            && !http_request->header_list()->contains("Cache-Control"sv.bytes())) {
            auto header = MUST(Infrastructure::Header::from_string_pair("Cache-Control"sv, "max-age=0"sv));
            TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
        }

        // 17. If httpRequest’s cache mode is "no-store" or "reload", then:
        if (http_request->cache_mode() == Infrastructure::Request::CacheMode::NoStore
            || http_request->cache_mode() == Infrastructure::Request::CacheMode::Reload) {
            // 1. If httpRequest’s header list does not contain `Pragma`, then append (`Pragma`, `no-cache`) to
            //    httpRequest’s header list.
            if (!http_request->header_list()->contains("Pragma"sv.bytes())) {
                auto header = MUST(Infrastructure::Header::from_string_pair("Pragma"sv, "no-cache"sv));
                TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
            }

            // 2. If httpRequest’s header list does not contain `Cache-Control`, then append
            //    (`Cache-Control`, `no-cache`) to httpRequest’s header list.
            if (!http_request->header_list()->contains("Cache-Control"sv.bytes())) {
                auto header = MUST(Infrastructure::Header::from_string_pair("Cache-Control"sv, "no-cache"sv));
                TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
            }
        }

        // 18. If httpRequest’s header list contains `Range`, then append (`Accept-Encoding`, `identity`) to
        //     httpRequest’s header list.
        // NOTE: This avoids a failure when handling content codings with a part of an encoded response.
        //       Additionally, many servers mistakenly ignore `Range` headers if a non-identity encoding is accepted.
        if (http_request->header_list()->contains("Range"sv.bytes())) {
            auto header = MUST(Infrastructure::Header::from_string_pair("Accept-Encoding"sv, "identity"sv));
            TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
        }

        // 19. Modify httpRequest’s header list per HTTP. Do not append a given header if httpRequest’s header list
        //     contains that header’s name.
        // NOTE: It would be great if we could make this more normative somehow. At this point headers such as
        //       `Accept-Encoding`, `Connection`, `DNT`, and `Host`, are to be appended if necessary.
        //     `Accept`, `Accept-Charset`, and `Accept-Language` must not be included at this point.
        // NOTE: `Accept` and `Accept-Language` are already included (unless fetch() is used, which does not include
        //       the latter by default), and `Accept-Charset` is a waste of bytes. See HTTP header layer division for
        //       more details.

        // 20. If includeCredentials is true, then:
        if (include_credentials == IncludeCredentials::Yes) {
            // 1. If the user agent is not configured to block cookies for httpRequest (see section 7 of [COOKIES]),
            //    then:
            if (true) {
                // 1. Let cookies be the result of running the "cookie-string" algorithm (see section 5.4 of [COOKIES])
                //    with the user agent’s cookie store and httpRequest’s current URL.
                auto cookies = ([&] {
                    // FIXME: Getting to the page client reliably is way too complicated, and going via the document won't work in workers.
                    auto document = Bindings::host_defined_environment_settings_object(realm).responsible_document();
                    if (!document)
                        return DeprecatedString::empty();
                    auto* page = document->page();
                    if (!page)
                        return DeprecatedString::empty();
                    return page->client().page_did_request_cookie(http_request->current_url(), Cookie::Source::Http);
                })();

                // 2. If cookies is not the empty string, then append (`Cookie`, cookies) to httpRequest’s header list.
                if (!cookies.is_empty()) {
                    auto header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Cookie"sv, cookies));
                    TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
                }
            }

            // 2. If httpRequest’s header list does not contain `Authorization`, then:
            if (!http_request->header_list()->contains("Authorization"sv.bytes())) {
                // 1. Let authorizationValue be null.
                auto authorization_value = Optional<String> {};

                // 2. If there’s an authentication entry for httpRequest and either httpRequest’s use-URL-credentials
                //    flag is unset or httpRequest’s current URL does not include credentials, then set
                //    authorizationValue to authentication entry.
                if (false // FIXME: "If there’s an authentication entry for httpRequest"
                    && (!http_request->use_url_credentials() || !http_request->current_url().includes_credentials())) {
                    // FIXME: "set authorizationValue to authentication entry."
                }
                // 3. Otherwise, if httpRequest’s current URL does include credentials and isAuthenticationFetch is
                //    true, set authorizationValue to httpRequest’s current URL, converted to an `Authorization` value.
                else if (http_request->current_url().includes_credentials() && is_authentication_fetch == IsAuthenticationFetch::Yes) {
                    auto const& url = http_request->current_url();
                    auto payload = TRY_OR_THROW_OOM(vm, String::formatted("{}:{}", url.username(), url.password()));
                    authorization_value = TRY_OR_THROW_OOM(vm, encode_base64(payload.bytes()));
                }

                // 4. If authorizationValue is non-null, then append (`Authorization`, authorizationValue) to
                //    httpRequest’s header list.
                if (authorization_value.has_value()) {
                    auto header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Authorization"sv, *authorization_value));
                    TRY_OR_THROW_OOM(vm, http_request->header_list()->append(move(header)));
                }
            }
        }

        // FIXME: 21. If there’s a proxy-authentication entry, use it as appropriate.
        // NOTE: This intentionally does not depend on httpRequest’s credentials mode.

        // FIXME: 22. Set httpCache to the result of determining the HTTP cache partition, given httpRequest.

        // 23. If httpCache is null, then set httpRequest’s cache mode to "no-store".
        if (!http_cache)
            http_request->set_cache_mode(Infrastructure::Request::CacheMode::NoStore);

        // 24. If httpRequest’s cache mode is neither "no-store" nor "reload", then:
        if (http_request->cache_mode() != Infrastructure::Request::CacheMode::NoStore
            && http_request->cache_mode() != Infrastructure::Request::CacheMode::Reload) {
            // 1. Set storedResponse to the result of selecting a response from the httpCache, possibly needing
            //    validation, as per the "Constructing Responses from Caches" chapter of HTTP Caching [HTTP-CACHING],
            //    if any.
            // NOTE: As mandated by HTTP, this still takes the `Vary` header into account.
            stored_response = nullptr;

            // 2. If storedResponse is non-null, then:
            if (stored_response) {
                // FIXME: Caching is not implemented yet.
                VERIFY_NOT_REACHED();
            }
        }
    }

    // 9. If aborted, then return the appropriate network error for fetchParams.
    if (aborted)
        return PendingResponse::create(vm, request, Infrastructure::Response::appropriate_network_error(vm, fetch_params));

    JS::GCPtr<PendingResponse> pending_forward_response;

    // 10. If response is null, then:
    if (!response) {
        // 1. If httpRequest’s cache mode is "only-if-cached", then return a network error.
        if (http_request->cache_mode() == Infrastructure::Request::CacheMode::OnlyIfCached)
            return PendingResponse::create(vm, request, Infrastructure::Response::network_error(vm, "Request with 'only-if-cached' cache mode doesn't have a cached response"sv));

        // 2. Let forwardResponse be the result of running HTTP-network fetch given httpFetchParams, includeCredentials,
        //    and isNewConnectionFetch.
        pending_forward_response = TRY(nonstandard_resource_loader_file_or_http_network_fetch(realm, *http_fetch_params, include_credentials, is_new_connection_fetch));
    } else {
        pending_forward_response = PendingResponse::create(vm, request, Infrastructure::Response::create(vm));
    }

    auto returned_pending_response = PendingResponse::create(vm, request);

    pending_forward_response->when_loaded([&realm, &vm, &fetch_params, request, response, stored_response, http_request, returned_pending_response, is_authentication_fetch, is_new_connection_fetch, revalidating_flag, include_credentials, response_was_null = !response](JS::NonnullGCPtr<Infrastructure::Response> resolved_forward_response) mutable {
        dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP-network-or-cache fetch' pending_forward_response load callback");
        if (response_was_null) {
            auto forward_response = resolved_forward_response;

            // NOTE: TRACE is omitted as it is a forbidden method in Fetch.
            auto method_is_unsafe = StringView { http_request->method() }.is_one_of("GET"sv, "HEAD"sv, "OPTIONS"sv);

            // 3. If httpRequest’s method is unsafe and forwardResponse’s status is in the range 200 to 399, inclusive,
            //    invalidate appropriate stored responses in httpCache, as per the "Invalidation" chapter of HTTP
            //    Caching, and set storedResponse to null.
            if (method_is_unsafe && forward_response->status() >= 200 && forward_response->status() <= 399) {
                // FIXME: "invalidate appropriate stored responses in httpCache, as per the "Invalidation" chapter of HTTP Caching"
                stored_response = nullptr;
            }

            // 4. If the revalidatingFlag is set and forwardResponse’s status is 304, then:
            if (revalidating_flag->value() && forward_response->status() == 304) {
                // FIXME: 1. Update storedResponse’s header list using forwardResponse’s header list, as per the "Freshening
                //           Stored Responses upon Validation" chapter of HTTP Caching.
                // NOTE: This updates the stored response in cache as well.

                // 2. Set response to storedResponse.
                response = stored_response;

                // 3. Set response’s cache state to "validated".
                if (response)
                    response->set_cache_state(Infrastructure::Response::CacheState::Validated);
            }

            // 5. If response is null, then:
            if (!response) {
                // 1. Set response to forwardResponse.
                response = forward_response;

                // FIXME: 2. Store httpRequest and forwardResponse in httpCache, as per the "Storing Responses in Caches"
                //           chapter of HTTP Caching.
                // NOTE: If forwardResponse is a network error, this effectively caches the network error, which is
                //       sometimes known as "negative caching".
                // NOTE: The associated body info is stored in the cache alongside the response.
            }
        }

        // 11. Set response’s URL list to a clone of httpRequest’s URL list.
        response->set_url_list(http_request->url_list());

        // 12. If httpRequest’s header list contains `Range`, then set response’s range-requested flag.
        if (http_request->header_list()->contains("Range"sv.bytes()))
            response->set_range_requested(true);

        // 13. Set response’s request-includes-credentials to includeCredentials.
        response->set_request_includes_credentials(include_credentials == IncludeCredentials::Yes);

        auto inner_pending_response = PendingResponse::create(vm, request, *response);

        // 14. If response’s status is 401, httpRequest’s response tainting is not "cors", includeCredentials is true,
        //     and request’s window is an environment settings object, then:
        if (response->status() == 401
            && http_request->response_tainting() != Infrastructure::Request::ResponseTainting::CORS
            && include_credentials == IncludeCredentials::Yes
            && request->window().has<JS::GCPtr<HTML::EnvironmentSettingsObject>>()) {
            // 1. Needs testing: multiple `WWW-Authenticate` headers, missing, parsing issues.
            // (Red box in the spec, no-op)

            // 2. If request’s body is non-null, then:
            if (!request->body().has<Empty>()) {
                // 1. If request’s body’s source is null, then return a network error.
                if (request->body().get<Infrastructure::Body>().source().has<Empty>()) {
                    returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Request has body but no body source"_string)));
                    return;
                }

                // 2. Set request’s body to the body of the result of safely extracting request’s body’s source.
                auto const& source = request->body().get<Infrastructure::Body>().source();
                // NOTE: BodyInitOrReadableBytes is a superset of Body::SourceType
                auto converted_source = source.has<ByteBuffer>()
                    ? BodyInitOrReadableBytes { source.get<ByteBuffer>() }
                    : BodyInitOrReadableBytes { source.get<JS::Handle<FileAPI::Blob>>() };
                auto [body, _] = TRY_OR_IGNORE(safely_extract_body(realm, converted_source));
                request->set_body(move(body));
            }

            // 3. If request’s use-URL-credentials flag is unset or isAuthenticationFetch is true, then:
            if (!request->use_url_credentials() || is_authentication_fetch == IsAuthenticationFetch::Yes) {
                // 1. If fetchParams is canceled, then return the appropriate network error for fetchParams.
                if (fetch_params.is_canceled()) {
                    returned_pending_response->resolve(Infrastructure::Response::appropriate_network_error(vm, fetch_params));
                    return;
                }

                // FIXME: 2. Let username and password be the result of prompting the end user for a username and password,
                //           respectively, in request’s window.
                dbgln("Fetch: Username/password prompt is not implemented, using empty strings. This request will probably fail.");
                auto username = DeprecatedString::empty();
                auto password = DeprecatedString::empty();

                // 3. Set the username given request’s current URL and username.
                request->current_url().set_username(move(username));

                // 4. Set the password given request’s current URL and password.
                request->current_url().set_password(move(password));
            }

            // 4. Set response to the result of running HTTP-network-or-cache fetch given fetchParams and true.
            inner_pending_response = TRY_OR_IGNORE(http_network_or_cache_fetch(realm, fetch_params, IsAuthenticationFetch::Yes));
        }

        inner_pending_response->when_loaded([&realm, &vm, &fetch_params, request, returned_pending_response, is_authentication_fetch, is_new_connection_fetch](JS::NonnullGCPtr<Infrastructure::Response> response) {
            dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'HTTP network-or-cache fetch' inner_pending_response load callback");
            // 15. If response’s status is 407, then:
            if (response->status() == 407) {
                // 1. If request’s window is "no-window", then return a network error.
                if (request->window().has<Infrastructure::Request::Window>()
                    && request->window().get<Infrastructure::Request::Window>() == Infrastructure::Request::Window::NoWindow) {
                    returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("Request requires proxy authentication but has 'no-window' set"_string)));
                    return;
                }

                // 2. Needs testing: multiple `Proxy-Authenticate` headers, missing, parsing issues.
                // (Red box in the spec, no-op)

                // 3. If fetchParams is canceled, then return the appropriate network error for fetchParams.
                if (fetch_params.is_canceled()) {
                    returned_pending_response->resolve(Infrastructure::Response::appropriate_network_error(vm, fetch_params));
                    return;
                }

                // FIXME: 4. Prompt the end user as appropriate in request’s window and store the result as a
                //           proxy-authentication entry.
                // NOTE: Remaining details surrounding proxy authentication are defined by HTTP.

                // FIXME: 5. Set response to the result of running HTTP-network-or-cache fetch given fetchParams.
                // (Doing this without step 4 would potentially lead to an infinite request cycle.)
            }

            auto inner_pending_response = PendingResponse::create(vm, request, *response);

            // 16. If all of the following are true
            if (
                // - response’s status is 421
                response->status() == 421
                // - isNewConnectionFetch is false
                && is_new_connection_fetch == IsNewConnectionFetch::No
                // - request’s body is null, or request’s body is non-null and request’s body’s source is non-null
                && (request->body().has<Empty>() || !request->body().get<Infrastructure::Body>().source().has<Empty>())
                // then:
            ) {
                // 1. If fetchParams is canceled, then return the appropriate network error for fetchParams.
                if (fetch_params.is_canceled()) {
                    returned_pending_response->resolve(Infrastructure::Response::appropriate_network_error(vm, fetch_params));
                    return;
                }
                // 2. Set response to the result of running HTTP-network-or-cache fetch given fetchParams,
                //    isAuthenticationFetch, and true.
                inner_pending_response = TRY_OR_IGNORE(http_network_or_cache_fetch(realm, fetch_params, is_authentication_fetch, IsNewConnectionFetch::Yes));
            }

            inner_pending_response->when_loaded([returned_pending_response, is_authentication_fetch](JS::NonnullGCPtr<Infrastructure::Response> response) {
                // 17. If isAuthenticationFetch is true, then create an authentication entry for request and the given
                //     realm.
                if (is_authentication_fetch == IsAuthenticationFetch::Yes) {
                    // FIXME: "create an authentication entry for request and the given realm"
                }

                returned_pending_response->resolve(response);
            });
        });
    });

    // 18. Return response.
    // NOTE: Typically response’s body’s stream is still being enqueued to after returning.
    return returned_pending_response;
}

#if defined(WEB_FETCH_DEBUG)
static void log_load_request(auto const& load_request)
{
    dbgln("Fetch: Invoking ResourceLoader");
    dbgln("> {} {} HTTP/1.1", load_request.method(), load_request.url());
    for (auto const& [name, value] : load_request.headers())
        dbgln("> {}: {}", name, value);
    dbgln(">");
    for (auto line : StringView { load_request.body() }.split_view('\n', SplitBehavior::KeepEmpty))
        dbgln("> {}", line);
}

static void log_response(auto const& status_code, auto const& headers, auto const& data)
{
    dbgln("< HTTP/1.1 {}", status_code.value_or(0));
    for (auto const& [name, value] : headers)
        dbgln("< {}: {}", name, value);
    dbgln("<");
    for (auto line : StringView { data }.split_view('\n', SplitBehavior::KeepEmpty))
        dbgln("< {}", line);
}
#endif

// https://fetch.spec.whatwg.org/#concept-http-network-fetch
// Drop-in replacement for 'HTTP-network fetch', but obviously non-standard :^)
// It also handles file:// URLs since those can also go through ResourceLoader.
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> nonstandard_resource_loader_file_or_http_network_fetch(JS::Realm& realm, Infrastructure::FetchParams const& fetch_params, IncludeCredentials include_credentials, IsNewConnectionFetch is_new_connection_fetch)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'non-standard HTTP-network fetch' with: fetch_params @ {}", &fetch_params);

    auto& vm = realm.vm();

    (void)include_credentials;
    (void)is_new_connection_fetch;

    auto request = fetch_params.request();

    Page* page = nullptr;
    auto& global_object = realm.global_object();
    if (is<HTML::Window>(global_object))
        page = static_cast<HTML::Window&>(global_object).page();

    // NOTE: Using LoadRequest::create_for_url_on_page here will unconditionally add cookies as long as there's a page available.
    //       However, it is up to http_network_or_cache_fetch to determine if cookies should be added to the request.
    LoadRequest load_request;
    load_request.set_url(request->current_url());
    if (page)
        load_request.set_page(*page);
    load_request.set_method(DeprecatedString::copy(request->method()));
    for (auto const& header : *request->header_list())
        load_request.set_header(DeprecatedString::copy(header.name), DeprecatedString::copy(header.value));
    if (auto const* body = request->body().get_pointer<Infrastructure::Body>()) {
        TRY(body->source().visit(
            [&](ByteBuffer const& byte_buffer) -> WebIDL::ExceptionOr<void> {
                load_request.set_body(TRY_OR_THROW_OOM(vm, ByteBuffer::copy(byte_buffer)));
                return {};
            },
            [&](JS::Handle<FileAPI::Blob> const& blob_handle) -> WebIDL::ExceptionOr<void> {
                load_request.set_body(TRY_OR_THROW_OOM(vm, ByteBuffer::copy(blob_handle->bytes())));
                return {};
            },
            [](Empty) -> WebIDL::ExceptionOr<void> {
                return {};
            }));
    }

    auto pending_response = PendingResponse::create(vm, request);

    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Invoking ResourceLoader");
    if constexpr (WEB_FETCH_DEBUG)
        log_load_request(load_request);

    ResourceLoader::the().load(
        load_request,
        [&realm, &vm, request, pending_response](auto data, auto& response_headers, auto status_code) {
            dbgln_if(WEB_FETCH_DEBUG, "Fetch: ResourceLoader load for '{}' complete", request->url());
            if constexpr (WEB_FETCH_DEBUG)
                log_response(status_code, response_headers, data);
            auto [body, _] = TRY_OR_IGNORE(extract_body(realm, data));
            auto response = Infrastructure::Response::create(vm);
            response->set_status(status_code.value_or(200));
            response->set_body(move(body));
            for (auto const& [name, value] : response_headers) {
                auto header = TRY_OR_IGNORE(Infrastructure::Header::from_string_pair(name, value));
                TRY_OR_IGNORE(response->header_list()->append(header));
            }
            // FIXME: Set response status message
            pending_response->resolve(response);
        },
        [&vm, request, pending_response](auto& error, auto status_code) {
            dbgln_if(WEB_FETCH_DEBUG, "Fetch: ResourceLoader load for '{}' failed: {} (status {})", request->url(), error, status_code.value_or(0));
            auto response = Infrastructure::Response::create(vm);
            // FIXME: This is ugly, ResourceLoader should tell us.
            if (status_code.value_or(0) == 0) {
                response = Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("HTTP request failed"_string));
            } else {
                response->set_type(Infrastructure::Response::Type::Error);
                response->set_status(status_code.value_or(400));
                // FIXME: Set response status message and body
            }
            pending_response->resolve(response);
        });

    return pending_response;
}

// https://fetch.spec.whatwg.org/#cors-preflight-fetch-0
WebIDL::ExceptionOr<JS::NonnullGCPtr<PendingResponse>> cors_preflight_fetch(JS::Realm& realm, Infrastructure::Request& request)
{
    dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'CORS-preflight fetch' with request @ {}", &request);

    auto& vm = realm.vm();

    // 1. Let preflight be a new request whose method is `OPTIONS`, URL list is a clone of request’s URL list, initiator is
    //    request’s initiator, destination is request’s destination, origin is request’s origin, referrer is request’s referrer,
    //    referrer policy is request’s referrer policy, mode is "cors", and response tainting is "cors".
    auto preflight = Fetch::Infrastructure::Request::create(vm);
    preflight->set_method(TRY_OR_THROW_OOM(vm, ByteBuffer::copy("OPTIONS"sv.bytes())));
    preflight->set_url_list(request.url_list());
    preflight->set_initiator(request.initiator());
    preflight->set_destination(request.destination());
    preflight->set_origin(request.origin());
    preflight->set_referrer(request.referrer());
    preflight->set_referrer_policy(request.referrer_policy());
    preflight->set_mode(Infrastructure::Request::Mode::CORS);
    preflight->set_response_tainting(Infrastructure::Request::ResponseTainting::CORS);

    // 2. Append (`Accept`, `*/*`) to preflight’s header list.
    auto temp_header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Accept"sv, "*/*"sv));
    TRY_OR_THROW_OOM(vm, preflight->header_list()->append(move(temp_header)));

    // 3. Append (`Access-Control-Request-Method`, request’s method) to preflight’s header list.
    temp_header = TRY_OR_THROW_OOM(vm, Infrastructure::Header::from_string_pair("Access-Control-Request-Method"sv, request.method()));
    TRY_OR_THROW_OOM(vm, preflight->header_list()->append(move(temp_header)));

    // 4. Let headers be the CORS-unsafe request-header names with request’s header list.
    auto headers = TRY_OR_THROW_OOM(vm, Infrastructure::get_cors_unsafe_header_names(request.header_list()));

    // 5. If headers is not empty, then:
    if (!headers.is_empty()) {
        // 1. Let value be the items in headers separated from each other by `,`.
        // NOTE: This intentionally does not use combine, as 0x20 following 0x2C is not the way this was implemented,
        //       for better or worse.
        ByteBuffer value;

        bool first = true;
        for (auto const& header : headers) {
            if (!first)
                TRY_OR_THROW_OOM(vm, value.try_append(','));
            TRY_OR_THROW_OOM(vm, value.try_append(header));
            first = false;
        }

        // 2. Append (`Access-Control-Request-Headers`, value) to preflight’s header list.
        temp_header = Infrastructure::Header {
            .name = TRY_OR_THROW_OOM(vm, ByteBuffer::copy("Access-Control-Request-Headers"sv.bytes())),
            .value = move(value),
        };
        TRY_OR_THROW_OOM(vm, preflight->header_list()->append(move(temp_header)));
    }

    // 6. Let response be the result of running HTTP-network-or-cache fetch given a new fetch params whose request is preflight.
    // FIXME: The spec doesn't say anything about timing_info here, but FetchParams requires a non-null FetchTimingInfo object.
    auto timing_info = Infrastructure::FetchTimingInfo::create(vm);
    auto fetch_params = Infrastructure::FetchParams::create(vm, preflight, timing_info);

    auto returned_pending_response = PendingResponse::create(vm, request);

    auto preflight_response = TRY(http_network_or_cache_fetch(realm, fetch_params));

    preflight_response->when_loaded([&vm, &request, returned_pending_response](JS::NonnullGCPtr<Infrastructure::Response> response) {
        dbgln_if(WEB_FETCH_DEBUG, "Fetch: Running 'CORS-preflight fetch' preflight_response load callback");

        // 7. If a CORS check for request and response returns success and response’s status is an ok status, then:
        // NOTE: The CORS check is done on request rather than preflight to ensure the correct credentials mode is used.
        if (TRY_OR_IGNORE(cors_check(request, response)) && Infrastructure::is_ok_status(response->status())) {
            // 1. Let methods be the result of extracting header list values given `Access-Control-Allow-Methods` and response’s header list.
            auto methods_or_failure = TRY_OR_IGNORE(Infrastructure::extract_header_list_values("Access-Control-Allow-Methods"sv.bytes(), response->header_list()));

            // 2. Let headerNames be the result of extracting header list values given `Access-Control-Allow-Headers` and
            //    response’s header list.
            auto header_names_or_failure = TRY_OR_IGNORE(Infrastructure::extract_header_list_values("Access-Control-Allow-Headers"sv.bytes(), response->header_list()));

            // 3. If either methods or headerNames is failure, return a network error.
            if (methods_or_failure.has<Infrastructure::ExtractHeaderParseFailure>()) {
                returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("The Access-Control-Allow-Methods in the CORS-preflight response is syntactically invalid"_string)));
                return;
            }

            if (header_names_or_failure.has<Infrastructure::ExtractHeaderParseFailure>()) {
                returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("The Access-Control-Allow-Headers in the CORS-preflight response is syntactically invalid"_string)));
                return;
            }

            // NOTE: We treat "methods_or_failure" being `Empty` as empty Vector here.
            auto methods = methods_or_failure.has<Vector<ByteBuffer>>() ? methods_or_failure.get<Vector<ByteBuffer>>() : Vector<ByteBuffer> {};

            // NOTE: We treat "header_names_or_failure" being `Empty` as empty Vector here.
            auto header_names = header_names_or_failure.has<Vector<ByteBuffer>>() ? header_names_or_failure.get<Vector<ByteBuffer>>() : Vector<ByteBuffer> {};

            // 4. If methods is null and request’s use-CORS-preflight flag is set, then set methods to a new list containing request’s method.
            // NOTE: This ensures that a CORS-preflight fetch that happened due to request’s use-CORS-preflight flag being set is cached.
            if (methods.is_empty() && request.use_cors_preflight())
                methods = Vector { TRY_OR_IGNORE(ByteBuffer::copy(request.method())) };

            // 5. If request’s method is not in methods, request’s method is not a CORS-safelisted method, and request’s credentials mode
            //    is "include" or methods does not contain `*`, then return a network error.
            if (!methods.contains_slow(request.method()) && !Infrastructure::is_cors_safelisted_method(request.method())) {
                if (request.credentials_mode() == Infrastructure::Request::CredentialsMode::Include) {
                    returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE(String::formatted("Non-CORS-safelisted method '{}' not found in the CORS-preflight response's Access-Control-Allow-Methods header (the header may be missing). '*' is not allowed as the main request includes credentials."sv, StringView { request.method() }))));
                    return;
                }

                if (!methods.contains_slow("*"sv.bytes())) {
                    returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE(String::formatted("Non-CORS-safelisted method '{}' not found in the CORS-preflight response's Access-Control-Allow-Methods header and there was no '*' entry. The header may be missing."sv, StringView { request.method() }))));
                    return;
                }
            }

            // 6. If one of request’s header list’s names is a CORS non-wildcard request-header name and is not a byte-case-insensitive match
            //    for an item in headerNames, then return a network error.
            for (auto const& header : *request.header_list()) {
                if (Infrastructure::is_cors_non_wildcard_request_header_name(header.name)) {
                    bool is_in_header_names = false;

                    for (auto const& allowed_header_name : header_names) {
                        if (StringView { allowed_header_name }.equals_ignoring_ascii_case(header.name)) {
                            is_in_header_names = true;
                            break;
                        }
                    }

                    if (!is_in_header_names) {
                        returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE(String::formatted("Main request contains the header '{}' that is not specified in the CORS-preflight response's Access-Control-Allow-Headers header (the header may be missing). '*' does not capture this header."sv, StringView { header.name }))));
                        return;
                    }
                }
            }

            // 7. For each unsafeName of the CORS-unsafe request-header names with request’s header list, if unsafeName is not a
            //    byte-case-insensitive match for an item in headerNames and request’s credentials mode is "include" or headerNames
            //    does not contain `*`, return a network error.
            auto unsafe_names = TRY_OR_IGNORE(Infrastructure::get_cors_unsafe_header_names(request.header_list()));
            for (auto const& unsafe_name : unsafe_names) {
                bool is_in_header_names = false;

                for (auto const& header_name : header_names) {
                    if (StringView { unsafe_name }.equals_ignoring_ascii_case(header_name)) {
                        is_in_header_names = true;
                        break;
                    }
                }

                if (!is_in_header_names) {
                    if (request.credentials_mode() == Infrastructure::Request::CredentialsMode::Include) {
                        returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE(String::formatted("CORS-unsafe request-header '{}' not found in the CORS-preflight response's Access-Control-Allow-Headers header (the header may be missing). '*' is not allowed as the main request includes credentials."sv, StringView { unsafe_name }))));
                        return;
                    }

                    if (!header_names.contains_slow("*"sv.bytes())) {
                        returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE(String::formatted("CORS-unsafe request-header '{}' not found in the CORS-preflight response's Access-Control-Allow-Headers header and there was no '*' entry. The header may be missing."sv, StringView { unsafe_name }))));
                        return;
                    }
                }
            }

            // FIXME: 8. Let max-age be the result of extracting header list values given `Access-Control-Max-Age` and response’s header list.
            // FIXME: 9. If max-age is failure or null, then set max-age to 5.
            // FIXME: 10. If max-age is greater than an imposed limit on max-age, then set max-age to the imposed limit.

            // 11. If the user agent does not provide for a cache, then return response.
            // NOTE: Since we don't currently have a cache, this is always true.
            returned_pending_response->resolve(response);
            return;

            // FIXME: 12. For each method in methods for which there is a method cache entry match using request, set matching entry’s max-age
            //            to max-age.
            // FIXME: 13. For each method in methods for which there is no method cache entry match using request, create a new cache entry
            //            with request, max-age, method, and null.
            // FIXME: 14. For each headerName in headerNames for which there is a header-name cache entry match using request, set matching
            //            entry’s max-age to max-age.
            // FIXME: 15. For each headerName in headerNames for which there is no header-name cache entry match using request, create a
            //            new cache entry with request, max-age, null, and headerName.
            // FIXME: 16. Return response.
        }

        // 8. Otherwise, return a network error.
        returned_pending_response->resolve(Infrastructure::Response::network_error(vm, TRY_OR_IGNORE("CORS-preflight check failed"_string)));
    });

    return returned_pending_response;
}

}
