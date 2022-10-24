/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>

namespace Web::Fetch::Infrastructure {

Response::Response()
    : m_header_list(make_ref_counted<HeaderList>())
{
}

NonnullRefPtr<Response> Response::create()
{
    return adopt_ref(*new Response());
}

// https://fetch.spec.whatwg.org/#ref-for-concept-network-error%E2%91%A3
// A network error is a response whose status is always 0, status message is always
// the empty byte sequence, header list is always empty, and body is always null.

NonnullRefPtr<Response> Response::aborted_network_error()
{
    auto response = network_error();
    response->set_aborted(true);
    return response;
}

NonnullRefPtr<Response> Response::network_error()
{
    auto response = Response::create();
    response->set_status(0);
    response->set_type(Type::Error);
    VERIFY(!response->body().has_value());
    return response;
}

// https://fetch.spec.whatwg.org/#concept-aborted-network-error
bool Response::is_aborted_network_error() const
{
    // A response whose type is "error" and aborted flag is set is known as an aborted network error.
    return m_type == Type::Error && m_aborted;
}

// https://fetch.spec.whatwg.org/#concept-network-error
bool Response::is_network_error() const
{
    // A response whose type is "error" is known as a network error.
    return m_type == Type::Error;
}

// https://fetch.spec.whatwg.org/#concept-response-url
Optional<AK::URL const&> Response::url() const
{
    // A response has an associated URL. It is a pointer to the last URL in response’s URL list and null if response’s URL list is empty.
    if (m_url_list.is_empty())
        return {};
    return m_url_list.last();
}

// https://fetch.spec.whatwg.org/#concept-response-location-url
ErrorOr<Optional<AK::URL>> Response::location_url(Optional<String> const& request_fragment) const
{
    // The location URL of a response response, given null or an ASCII string requestFragment, is the value returned by the following steps. They return null, failure, or a URL.

    // 1. If response’s status is not a redirect status, then return null.
    if (!is_redirect_status(m_status))
        return Optional<AK::URL> {};

    // FIXME: 2. Let location be the result of extracting header list values given `Location` and response’s header list.
    auto location_value = ByteBuffer {};

    // 3. If location is a header value, then set location to the result of parsing location with response’s URL.
    auto location = AK::URL { StringView { location_value } };
    if (!location.is_valid())
        return Error::from_string_view("Invalid 'Location' header URL"sv);

    // 4. If location is a URL whose fragment is null, then set location’s fragment to requestFragment.
    if (location.fragment().is_null())
        location.set_fragment(request_fragment.value_or({}));

    // 5. Return location.
    return location;
}

// https://fetch.spec.whatwg.org/#concept-response-clone
WebIDL::ExceptionOr<NonnullRefPtr<Response>> Response::clone() const
{
    // To clone a response response, run these steps:

    auto& vm = Bindings::main_thread_vm();
    auto& realm = *vm.current_realm();

    // 1. If response is a filtered response, then return a new identical filtered response whose internal response is a clone of response’s internal response.
    if (is<FilteredResponse>(*this)) {
        auto internal_response = TRY(static_cast<FilteredResponse const&>(*this).internal_response()->clone());
        if (is<BasicFilteredResponse>(*this))
            return TRY_OR_RETURN_OOM(realm, BasicFilteredResponse::create(move(internal_response)));
        if (is<CORSFilteredResponse>(*this))
            return TRY_OR_RETURN_OOM(realm, CORSFilteredResponse::create(move(internal_response)));
        if (is<OpaqueFilteredResponse>(*this))
            return OpaqueFilteredResponse::create(move(internal_response));
        if (is<OpaqueRedirectFilteredResponse>(*this))
            return OpaqueRedirectFilteredResponse::create(move(internal_response));
        VERIFY_NOT_REACHED();
    }

    // 2. Let newResponse be a copy of response, except for its body.
    auto new_response = Infrastructure::Response::create();
    new_response->set_type(m_type);
    new_response->set_aborted(m_aborted);
    new_response->set_url_list(m_url_list);
    new_response->set_status(m_status);
    new_response->set_status_message(m_status_message);
    for (auto const& header : *m_header_list)
        MUST(new_response->header_list()->append(header));
    new_response->set_cache_state(m_cache_state);
    new_response->set_cors_exposed_header_name_list(m_cors_exposed_header_name_list);
    new_response->set_range_requested(m_range_requested);
    new_response->set_request_includes_credentials(m_request_includes_credentials);
    new_response->set_timing_allow_passed(m_timing_allow_passed);
    new_response->set_body_info(m_body_info);
    // FIXME: service worker timing info

    // 3. If response’s body is non-null, then set newResponse’s body to the result of cloning response’s body.
    if (m_body.has_value())
        new_response->set_body(TRY(m_body->clone()));

    // 4. Return newResponse.
    return new_response;
}

FilteredResponse::FilteredResponse(NonnullRefPtr<Response> internal_response)
    : m_internal_response(move(internal_response))
{
}

FilteredResponse::~FilteredResponse()
{
}

ErrorOr<NonnullRefPtr<BasicFilteredResponse>> BasicFilteredResponse::create(NonnullRefPtr<Response> internal_response)
{
    // A basic filtered response is a filtered response whose type is "basic" and header list excludes
    // any headers in internal response’s header list whose name is a forbidden response-header name.
    auto header_list = make_ref_counted<HeaderList>();
    for (auto const& header : *internal_response->header_list()) {
        if (!is_forbidden_response_header_name(header.name))
            TRY(header_list->append(header));
    }

    return adopt_ref(*new BasicFilteredResponse(move(internal_response), move(header_list)));
}

BasicFilteredResponse::BasicFilteredResponse(NonnullRefPtr<Response> internal_response, NonnullRefPtr<HeaderList> header_list)
    : FilteredResponse(move(internal_response))
    , m_header_list(move(header_list))
{
}

ErrorOr<NonnullRefPtr<CORSFilteredResponse>> CORSFilteredResponse::create(NonnullRefPtr<Response> internal_response)
{
    // A CORS filtered response is a filtered response whose type is "cors" and header list excludes
    // any headers in internal response’s header list whose name is not a CORS-safelisted response-header
    // name, given internal response’s CORS-exposed header-name list.
    Vector<ReadonlyBytes> cors_exposed_header_name_list;
    for (auto const& header_name : internal_response->cors_exposed_header_name_list())
        cors_exposed_header_name_list.append(header_name.span());

    auto header_list = make_ref_counted<HeaderList>();
    for (auto const& header : *internal_response->header_list()) {
        if (is_cors_safelisted_response_header_name(header.name, cors_exposed_header_name_list))
            TRY(header_list->append(header));
    }

    return adopt_ref(*new CORSFilteredResponse(move(internal_response), move(header_list)));
}

CORSFilteredResponse::CORSFilteredResponse(NonnullRefPtr<Response> internal_response, NonnullRefPtr<HeaderList> header_list)
    : FilteredResponse(move(internal_response))
    , m_header_list(move(header_list))
{
}

NonnullRefPtr<OpaqueFilteredResponse> OpaqueFilteredResponse::create(NonnullRefPtr<Response> internal_response)
{
    // An opaque-redirect filtered response is a filtered response whose type is "opaqueredirect",
    // status is 0, status message is the empty byte sequence, header list is empty, and body is null.
    return adopt_ref(*new OpaqueFilteredResponse(move(internal_response)));
}

OpaqueFilteredResponse::OpaqueFilteredResponse(NonnullRefPtr<Response> internal_response)
    : FilteredResponse(move(internal_response))
    , m_header_list(make_ref_counted<HeaderList>())
{
}

NonnullRefPtr<OpaqueRedirectFilteredResponse> OpaqueRedirectFilteredResponse::create(NonnullRefPtr<Response> internal_response)
{
    return adopt_ref(*new OpaqueRedirectFilteredResponse(move(internal_response)));
}

OpaqueRedirectFilteredResponse::OpaqueRedirectFilteredResponse(NonnullRefPtr<Response> internal_response)
    : FilteredResponse(move(internal_response))
    , m_header_list(make_ref_counted<HeaderList>())
{
}

}
