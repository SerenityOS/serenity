/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>

namespace Web::Fetch::Infrastructure {

Response::Response()
    : m_header_list(make_ref_counted<HeaderList>())
{
}

// https://fetch.spec.whatwg.org/#ref-for-concept-network-error%E2%91%A3
// A network error is a response whose status is always 0, status message is always
// the empty byte sequence, header list is always empty, and body is always null.

NonnullOwnPtr<Response> Response::aborted_network_error()
{
    auto response = network_error();
    response->set_aborted(true);
    return response;
}

NonnullOwnPtr<Response> Response::network_error()
{
    auto response = make<Response>();
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
WebIDL::ExceptionOr<NonnullOwnPtr<Response>> Response::clone() const
{
    // To clone a response response, run these steps:

    // FIXME: 1. If response is a filtered response, then return a new identical filtered response whose internal response is a clone of response’s internal response.

    // 2. Let newResponse be a copy of response, except for its body.
    Optional<Body> tmp_body;
    swap(tmp_body, const_cast<Optional<Body>&>(m_body));
    auto new_response = make<Infrastructure::Response>(*this);
    swap(tmp_body, const_cast<Optional<Body>&>(m_body));

    // 3. If response’s body is non-null, then set newResponse’s body to the result of cloning response’s body.
    if (m_body.has_value())
        new_response->set_body(TRY(m_body->clone()));

    // 4. Return newResponse.
    return new_response;
}

FilteredResponse::FilteredResponse(Response& internal_response)
    : m_internal_response(internal_response)
{
}

FilteredResponse::~FilteredResponse()
{
}

ErrorOr<NonnullOwnPtr<BasicFilteredResponse>> BasicFilteredResponse::create(Response& internal_response)
{
    // A basic filtered response is a filtered response whose type is "basic" and header list excludes
    // any headers in internal response’s header list whose name is a forbidden response-header name.
    auto header_list = make_ref_counted<HeaderList>();
    for (auto const& header : *internal_response.header_list()) {
        if (!is_forbidden_response_header_name(header.name))
            TRY(header_list->append(header));
    }

    return adopt_own(*new BasicFilteredResponse(internal_response, move(header_list)));
}

BasicFilteredResponse::BasicFilteredResponse(Response& internal_response, NonnullRefPtr<HeaderList> header_list)
    : FilteredResponse(internal_response)
    , m_header_list(move(header_list))
{
}

ErrorOr<NonnullOwnPtr<CORSFilteredResponse>> CORSFilteredResponse::create(Response& internal_response)
{
    // A CORS filtered response is a filtered response whose type is "cors" and header list excludes
    // any headers in internal response’s header list whose name is not a CORS-safelisted response-header
    // name, given internal response’s CORS-exposed header-name list.
    Vector<ReadonlyBytes> cors_exposed_header_name_list;
    for (auto const& header_name : internal_response.cors_exposed_header_name_list())
        cors_exposed_header_name_list.append(header_name.span());

    auto header_list = make_ref_counted<HeaderList>();
    for (auto const& header : *internal_response.header_list()) {
        if (is_cors_safelisted_response_header_name(header.name, cors_exposed_header_name_list))
            TRY(header_list->append(header));
    }

    return adopt_own(*new CORSFilteredResponse(internal_response, move(header_list)));
}

CORSFilteredResponse::CORSFilteredResponse(Response& internal_response, NonnullRefPtr<HeaderList> header_list)
    : FilteredResponse(internal_response)
    , m_header_list(move(header_list))
{
}

NonnullOwnPtr<OpaqueFilteredResponse> OpaqueFilteredResponse::create(Response& internal_response)
{
    // An opaque-redirect filtered response is a filtered response whose type is "opaqueredirect",
    // status is 0, status message is the empty byte sequence, header list is empty, and body is null.
    return adopt_own(*new OpaqueFilteredResponse(internal_response));
}

OpaqueFilteredResponse::OpaqueFilteredResponse(Response& internal_response)
    : FilteredResponse(internal_response)
    , m_header_list(make_ref_counted<HeaderList>())
{
}

NonnullOwnPtr<OpaqueRedirectFilteredResponse> OpaqueRedirectFilteredResponse::create(Response& internal_response)
{
    return adopt_own(*new OpaqueRedirectFilteredResponse(internal_response));
}

OpaqueRedirectFilteredResponse::OpaqueRedirectFilteredResponse(Response& internal_response)
    : FilteredResponse(internal_response)
    , m_header_list(make_ref_counted<HeaderList>())
{
}

}
