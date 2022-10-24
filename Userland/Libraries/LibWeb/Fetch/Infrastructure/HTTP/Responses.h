/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Error.h>
#include <AK/Forward.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Bodies.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Headers.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Statuses.h>

namespace Web::Fetch::Infrastructure {

// https://fetch.spec.whatwg.org/#concept-response
class Response : public RefCounted<Response> {
public:
    enum class CacheState {
        Local,
        Validated,
    };

    enum class Type {
        Basic,
        CORS,
        Default,
        Error,
        Opaque,
        OpaqueRedirect,
    };

    // https://fetch.spec.whatwg.org/#response-body-info
    struct BodyInfo {
        // https://fetch.spec.whatwg.org/#fetch-timing-info-encoded-body-size
        u64 encoded_size { 0 };

        // https://fetch.spec.whatwg.org/#fetch-timing-info-decoded-body-size
        u64 decoded_size { 0 };
    };

    [[nodiscard]] static NonnullRefPtr<Response> create();
    [[nodiscard]] static NonnullRefPtr<Response> aborted_network_error();
    [[nodiscard]] static NonnullRefPtr<Response> network_error();

    virtual ~Response() = default;

    [[nodiscard]] virtual Type type() const { return m_type; }
    void set_type(Type type) { m_type = type; }

    [[nodiscard]] virtual bool aborted() const { return m_aborted; }
    void set_aborted(bool aborted) { m_aborted = aborted; }

    [[nodiscard]] virtual Vector<AK::URL> const& url_list() const { return m_url_list; }
    [[nodiscard]] virtual Vector<AK::URL>& url_list() { return m_url_list; }
    void set_url_list(Vector<AK::URL> url_list) { m_url_list = move(url_list); }

    [[nodiscard]] virtual Status status() const { return m_status; }
    void set_status(Status status) { m_status = status; }

    [[nodiscard]] virtual ReadonlyBytes status_message() const { return m_status_message; }
    void set_status_message(ByteBuffer status_message) { m_status_message = move(status_message); }

    [[nodiscard]] virtual NonnullRefPtr<HeaderList> const& header_list() const { return m_header_list; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList>& header_list() { return m_header_list; }
    void set_header_list(NonnullRefPtr<HeaderList> header_list) { m_header_list = move(header_list); }

    [[nodiscard]] virtual Optional<Body> const& body() const { return m_body; }
    [[nodiscard]] virtual Optional<Body>& body() { return m_body; }
    void set_body(Optional<Body> body) { m_body = move(body); }

    [[nodiscard]] virtual Optional<CacheState> const& cache_state() const { return m_cache_state; }
    void set_cache_state(Optional<CacheState> cache_state) { m_cache_state = move(cache_state); }

    [[nodiscard]] virtual Vector<ByteBuffer> const& cors_exposed_header_name_list() const { return m_cors_exposed_header_name_list; }
    void set_cors_exposed_header_name_list(Vector<ByteBuffer> cors_exposed_header_name_list) { m_cors_exposed_header_name_list = move(cors_exposed_header_name_list); }

    [[nodiscard]] virtual bool range_requested() const { return m_range_requested; }
    void set_range_requested(bool range_requested) { m_range_requested = range_requested; }

    [[nodiscard]] virtual bool request_includes_credentials() const { return m_request_includes_credentials; }
    void set_request_includes_credentials(bool request_includes_credentials) { m_request_includes_credentials = request_includes_credentials; }

    [[nodiscard]] virtual bool timing_allow_passed() const { return m_timing_allow_passed; }
    void set_timing_allow_passed(bool timing_allow_passed) { m_timing_allow_passed = timing_allow_passed; }

    [[nodiscard]] virtual BodyInfo const& body_info() const { return m_body_info; }
    void set_body_info(BodyInfo body_info) { m_body_info = body_info; }

    [[nodiscard]] bool is_aborted_network_error() const;
    [[nodiscard]] bool is_network_error() const;

    [[nodiscard]] Optional<AK::URL const&> url() const;
    [[nodiscard]] ErrorOr<Optional<AK::URL>> location_url(Optional<String> const& request_fragment) const;

    [[nodiscard]] WebIDL::ExceptionOr<NonnullRefPtr<Response>> clone() const;

protected:
    Response();

private:
    // https://fetch.spec.whatwg.org/#concept-response-type
    // A response has an associated type which is "basic", "cors", "default", "error", "opaque", or "opaqueredirect". Unless stated otherwise, it is "default".
    Type m_type { Type::Default };

    // https://fetch.spec.whatwg.org/#concept-response-aborted
    // A response can have an associated aborted flag, which is initially unset.
    bool m_aborted { false };

    // https://fetch.spec.whatwg.org/#concept-response-url-list
    // A response has an associated URL list (a list of zero or more URLs). Unless stated otherwise, it is the empty list.
    Vector<AK::URL> m_url_list;

    // https://fetch.spec.whatwg.org/#concept-response-status
    // A response has an associated status, which is a status. Unless stated otherwise it is 200.
    Status m_status { 200 };

    // https://fetch.spec.whatwg.org/#concept-response-status-message
    // A response has an associated status message. Unless stated otherwise it is the empty byte sequence.
    ByteBuffer m_status_message;

    // https://fetch.spec.whatwg.org/#concept-response-header-list
    // A response has an associated header list (a header list). Unless stated otherwise it is empty.
    NonnullRefPtr<HeaderList> m_header_list;

    // https://fetch.spec.whatwg.org/#concept-response-body
    // A response has an associated body (null or a body). Unless stated otherwise it is null.
    Optional<Body> m_body;

    // https://fetch.spec.whatwg.org/#concept-response-cache-state
    // A response has an associated cache state (the empty string, "local", or "validated"). Unless stated otherwise, it is the empty string.
    Optional<CacheState> m_cache_state;

    // https://fetch.spec.whatwg.org/#concept-response-cors-exposed-header-name-list
    // A response has an associated CORS-exposed header-name list (a list of zero or more header names). The list is empty unless otherwise specified.
    Vector<ByteBuffer> m_cors_exposed_header_name_list;

    // https://fetch.spec.whatwg.org/#concept-response-range-requested-flag
    // A response has an associated range-requested flag, which is initially unset.
    bool m_range_requested { false };

    // https://fetch.spec.whatwg.org/#response-request-includes-credentials
    // A response has an associated request-includes-credentials (a boolean), which is initially true.
    bool m_request_includes_credentials { true };

    // https://fetch.spec.whatwg.org/#concept-response-timing-allow-passed
    // A response has an associated timing allow passed flag, which is initially unset.
    bool m_timing_allow_passed { false };

    // https://fetch.spec.whatwg.org/#concept-response-body-info
    // A response has an associated body info (a response body info). Unless stated otherwise, it is a new response body info.
    BodyInfo m_body_info;

    // https://fetch.spec.whatwg.org/#response-service-worker-timing-info
    // FIXME: A response has an associated service worker timing info (null or a service worker timing info), which is initially null.
};

// https://fetch.spec.whatwg.org/#concept-filtered-response
class FilteredResponse : public Response {
public:
    explicit FilteredResponse(NonnullRefPtr<Response>);
    virtual ~FilteredResponse() = 0;

    [[nodiscard]] virtual Type type() const override { return m_internal_response->type(); }
    [[nodiscard]] virtual bool aborted() const override { return m_internal_response->aborted(); }
    [[nodiscard]] virtual Vector<AK::URL> const& url_list() const override { return m_internal_response->url_list(); }
    [[nodiscard]] virtual Vector<AK::URL>& url_list() override { return m_internal_response->url_list(); }
    [[nodiscard]] virtual Status status() const override { return m_internal_response->status(); }
    [[nodiscard]] virtual ReadonlyBytes status_message() const override { return m_internal_response->status_message(); }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList> const& header_list() const override { return m_internal_response->header_list(); }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList>& header_list() override { return m_internal_response->header_list(); }
    [[nodiscard]] virtual Optional<Body> const& body() const override { return m_internal_response->body(); }
    [[nodiscard]] virtual Optional<Body>& body() override { return m_internal_response->body(); }
    [[nodiscard]] virtual Optional<CacheState> const& cache_state() const override { return m_internal_response->cache_state(); }
    [[nodiscard]] virtual Vector<ByteBuffer> const& cors_exposed_header_name_list() const override { return m_internal_response->cors_exposed_header_name_list(); }
    [[nodiscard]] virtual bool range_requested() const override { return m_internal_response->range_requested(); }
    [[nodiscard]] virtual bool request_includes_credentials() const override { return m_internal_response->request_includes_credentials(); }
    [[nodiscard]] virtual bool timing_allow_passed() const override { return m_internal_response->timing_allow_passed(); }
    [[nodiscard]] virtual BodyInfo const& body_info() const override { return m_internal_response->body_info(); }

    [[nodiscard]] NonnullRefPtr<Response> internal_response() const { return m_internal_response; }

protected:
    // https://fetch.spec.whatwg.org/#concept-internal-response
    NonnullRefPtr<Response> m_internal_response;
};

// https://fetch.spec.whatwg.org/#concept-filtered-response-basic
class BasicFilteredResponse final : public FilteredResponse {
public:
    static ErrorOr<NonnullRefPtr<BasicFilteredResponse>> create(NonnullRefPtr<Response>);

    [[nodiscard]] virtual Type type() const override { return Type::Basic; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList> const& header_list() const override { return m_header_list; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList>& header_list() override { return m_header_list; }

private:
    BasicFilteredResponse(NonnullRefPtr<Response>, NonnullRefPtr<HeaderList>);

    NonnullRefPtr<HeaderList> m_header_list;
};

// https://fetch.spec.whatwg.org/#concept-filtered-response-cors
class CORSFilteredResponse final : public FilteredResponse {
public:
    static ErrorOr<NonnullRefPtr<CORSFilteredResponse>> create(NonnullRefPtr<Response>);

    [[nodiscard]] virtual Type type() const override { return Type::CORS; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList> const& header_list() const override { return m_header_list; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList>& header_list() override { return m_header_list; }

private:
    CORSFilteredResponse(NonnullRefPtr<Response>, NonnullRefPtr<HeaderList>);

    NonnullRefPtr<HeaderList> m_header_list;
};

// https://fetch.spec.whatwg.org/#concept-filtered-response-opaque
class OpaqueFilteredResponse final : public FilteredResponse {
public:
    static NonnullRefPtr<OpaqueFilteredResponse> create(NonnullRefPtr<Response>);

    [[nodiscard]] virtual Type type() const override { return Type::Opaque; }
    [[nodiscard]] virtual Vector<AK::URL> const& url_list() const override { return m_url_list; }
    [[nodiscard]] virtual Vector<AK::URL>& url_list() override { return m_url_list; }
    [[nodiscard]] virtual Status status() const override { return 0; }
    [[nodiscard]] virtual ReadonlyBytes status_message() const override { return {}; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList> const& header_list() const override { return m_header_list; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList>& header_list() override { return m_header_list; }
    [[nodiscard]] virtual Optional<Body> const& body() const override { return m_body; }
    [[nodiscard]] virtual Optional<Body>& body() override { return m_body; }

private:
    explicit OpaqueFilteredResponse(NonnullRefPtr<Response>);

    Vector<AK::URL> m_url_list;
    NonnullRefPtr<HeaderList> m_header_list;
    Optional<Body> m_body;
};

// https://fetch.spec.whatwg.org/#concept-filtered-response-opaque-redirect
class OpaqueRedirectFilteredResponse final : public FilteredResponse {
public:
    static NonnullRefPtr<OpaqueRedirectFilteredResponse> create(NonnullRefPtr<Response>);

    [[nodiscard]] virtual Type type() const override { return Type::OpaqueRedirect; }
    [[nodiscard]] virtual Status status() const override { return 0; }
    [[nodiscard]] virtual ReadonlyBytes status_message() const override { return {}; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList> const& header_list() const override { return m_header_list; }
    [[nodiscard]] virtual NonnullRefPtr<HeaderList>& header_list() override { return m_header_list; }
    [[nodiscard]] virtual Optional<Body> const& body() const override { return m_body; }
    [[nodiscard]] virtual Optional<Body>& body() override { return m_body; }

private:
    explicit OpaqueRedirectFilteredResponse(NonnullRefPtr<Response>);

    NonnullRefPtr<HeaderList> m_header_list;
    Optional<Body> m_body;
};

}
