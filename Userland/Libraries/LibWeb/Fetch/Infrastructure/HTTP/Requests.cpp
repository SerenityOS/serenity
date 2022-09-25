/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Requests.h>

namespace Web::Fetch::Infrastructure {

Request::Request()
    : m_header_list(make_ref_counted<HeaderList>())
{
}

// https://fetch.spec.whatwg.org/#concept-request-url
AK::URL const& Request::url() const
{
    // A request has an associated URL (a URL).
    // NOTE: Implementations are encouraged to make this a pointer to the first URL in request’s URL list. It is provided as a distinct field solely for the convenience of other standards hooking into Fetch.
    VERIFY(!m_url_list.is_empty());
    return m_url_list.first();
}

// https://fetch.spec.whatwg.org/#concept-request-current-url
AK::URL const& Request::current_url()
{
    // A request has an associated current URL. It is a pointer to the last URL in request’s URL list.
    VERIFY(!m_url_list.is_empty());
    return m_url_list.last();
}

void Request::set_url(AK::URL url)
{
    // Sometimes setting the URL and URL list are done as two distinct steps in the spec,
    // but since we know the URL is always the URL list's first item and doesn't change later
    // on, we can combine them.
    if (!m_url_list.is_empty())
        m_url_list.clear();
    m_url_list.append(move(url));
}

// https://fetch.spec.whatwg.org/#request-destination-script-like
bool Request::destination_is_script_like() const
{
    // A request’s destination is script-like if it is "audioworklet", "paintworklet", "script", "serviceworker", "sharedworker", or "worker".
    static constexpr Array script_like_destinations = {
        Destination::AudioWorklet,
        Destination::PaintWorklet,
        Destination::Script,
        Destination::ServiceWorker,
        Destination::SharedWorker,
        Destination::Worker,
    };
    return any_of(script_like_destinations, [this](auto destination) {
        return m_destination == destination;
    });
}

// https://fetch.spec.whatwg.org/#subresource-request
bool Request::is_subresource_request() const
{
    // A subresource request is a request whose destination is "audio", "audioworklet", "font", "image", "manifest", "paintworklet", "script", "style", "track", "video", "xslt", or the empty string.
    static constexpr Array subresource_request_destinations = {
        Destination::Audio,
        Destination::AudioWorklet,
        Destination::Font,
        Destination::Image,
        Destination::Manifest,
        Destination::PaintWorklet,
        Destination::Script,
        Destination::Style,
        Destination::Track,
        Destination::Video,
        Destination::XSLT,
    };
    return any_of(subresource_request_destinations, [this](auto destination) {
        return m_destination == destination;
    }) || !m_destination.has_value();
}

// https://fetch.spec.whatwg.org/#non-subresource-request
bool Request::is_non_subresource_request() const
{
    // A non-subresource request is a request whose destination is "document", "embed", "frame", "iframe", "object", "report", "serviceworker", "sharedworker", or "worker".
    static constexpr Array non_subresource_request_destinations = {
        Destination::Document,
        Destination::Embed,
        Destination::Frame,
        Destination::IFrame,
        Destination::Object,
        Destination::Report,
        Destination::ServiceWorker,
        Destination::SharedWorker,
        Destination::Worker,
    };
    return any_of(non_subresource_request_destinations, [this](auto destination) {
        return m_destination == destination;
    });
}

// https://fetch.spec.whatwg.org/#navigation-request
bool Request::is_navigation_request() const
{
    // A navigation request is a request whose destination is "document", "embed", "frame", "iframe", or "object".
    static constexpr Array navigation_request_destinations = {
        Destination::Document,
        Destination::Embed,
        Destination::Frame,
        Destination::IFrame,
        Destination::Object,
    };
    return any_of(navigation_request_destinations, [this](auto destination) {
        return m_destination == destination;
    });
}

// https://fetch.spec.whatwg.org/#concept-request-tainted-origin
bool Request::has_redirect_tainted_origin() const
{
    // A request request has a redirect-tainted origin if these steps return true:

    // 1. Let lastURL be null.
    Optional<AK::URL const&> last_url;

    // 2. For each url in request’s URL list:
    for (auto const& url : m_url_list) {
        // 1. If lastURL is null, then set lastURL to url and continue.
        if (!last_url.has_value()) {
            last_url = url;
            continue;
        }

        // 2. If url’s origin is not same origin with lastURL’s origin and request’s origin is not same origin with lastURL’s origin, then return true.
        // FIXME: Actually use the given origins once we have https://url.spec.whatwg.org/#concept-url-origin.
        if (!HTML::Origin().is_same_origin(HTML::Origin()) && HTML::Origin().is_same_origin(HTML::Origin()))
            return true;

        // 3. Set lastURL to url.
        last_url = url;
    }

    // 3. Return false.
    return false;
}

// https://fetch.spec.whatwg.org/#serializing-a-request-origin
String Request::serialize_origin() const
{
    // 1. If request has a redirect-tainted origin, then return "null".
    if (has_redirect_tainted_origin())
        return "null"sv;

    // 2. Return request’s origin, serialized.
    return m_origin.get<HTML::Origin>().serialize();
}

// https://fetch.spec.whatwg.org/#byte-serializing-a-request-origin
ErrorOr<ByteBuffer> Request::byte_serialize_origin() const
{
    // Byte-serializing a request origin, given a request request, is to return the result of serializing a request origin with request, isomorphic encoded.
    return ByteBuffer::copy(serialize_origin().bytes());
}

// https://fetch.spec.whatwg.org/#concept-request-clone
WebIDL::ExceptionOr<NonnullOwnPtr<Request>> Request::clone() const
{
    // To clone a request request, run these steps:

    // 1. Let newRequest be a copy of request, except for its body.
    BodyType tmp_body;
    swap(tmp_body, const_cast<BodyType&>(m_body));
    auto new_request = make<Infrastructure::Request>(*this);
    swap(tmp_body, const_cast<BodyType&>(m_body));

    // 2. If request’s body is non-null, set newRequest’s body to the result of cloning request’s body.
    if (auto const* body = m_body.get_pointer<Body>())
        new_request->set_body(TRY(body->clone()));

    // 3. Return newRequest.
    return new_request;
}

// https://fetch.spec.whatwg.org/#concept-request-add-range-header
ErrorOr<void> Request::add_range_reader(u64 first, Optional<u64> const& last)
{
    // To add a range header to a request request, with an integer first, and an optional integer last, run these steps:

    // 1. Assert: last is not given, or first is less than or equal to last.
    VERIFY(!last.has_value() || first <= last.value());

    // 2. Let rangeValue be `bytes=`.
    auto range_value = TRY(ByteBuffer::copy("bytes"sv.bytes()));

    // 3. Serialize and isomorphic encode first, and append the result to rangeValue.
    TRY(range_value.try_append(String::number(first).bytes()));

    // 4. Append 0x2D (-) to rangeValue.
    TRY(range_value.try_append('-'));

    // 5. If last is given, then serialize and isomorphic encode it, and append the result to rangeValue.
    if (last.has_value())
        TRY(range_value.try_append(String::number(*last).bytes()));

    // 6. Append (`Range`, rangeValue) to request’s header list.
    auto header = Header {
        .name = TRY(ByteBuffer::copy("Range"sv.bytes())),
        .value = move(range_value),
    };
    TRY(m_header_list->append(move(header)));

    return {};
}

// https://fetch.spec.whatwg.org/#cross-origin-embedder-policy-allows-credentials
bool Request::cross_origin_embedder_policy_allows_credentials() const
{
    // 1. If request’s mode is not "no-cors", then return true.
    if (m_mode != Mode::NoCORS)
        return true;

    // 2. If request’s client is null, then return true.
    if (m_client == nullptr)
        return true;

    // FIXME: 3. If request’s client’s policy container’s embedder policy’s value is not "credentialless", then return true.

    // 4. If request’s origin is same origin with request’s current URL’s origin and request does not have a redirect-tainted origin, then return true.
    // FIXME: Actually use the given origins once we have https://url.spec.whatwg.org/#concept-url-origin.
    if (HTML::Origin().is_same_origin(HTML::Origin()) && !has_redirect_tainted_origin())
        return true;

    // 5. Return false.
    return false;
}

}
