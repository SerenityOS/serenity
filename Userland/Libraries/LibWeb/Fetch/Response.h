/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <LibJS/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Fetch/Body.h>
#include <LibWeb/Fetch/BodyInit.h>
#include <LibWeb/Fetch/Headers.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Responses.h>
#include <LibWeb/Forward.h>

namespace Web::Fetch {

// https://fetch.spec.whatwg.org/#responseinit
struct ResponseInit {
    u16 status;
    String status_text;
    Optional<HeadersInit> headers;
};

// https://fetch.spec.whatwg.org/#response
class Response final
    : public Bindings::PlatformObject
    , public BodyMixin {
    WEB_PLATFORM_OBJECT(Response, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<Response> create(NonnullRefPtr<Infrastructure::Response>, Headers::Guard, JS::Realm&);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Response>> construct_impl(JS::Realm&, Optional<BodyInit> const& body = {}, ResponseInit const& init = {});

    virtual ~Response() override;

    // ^BodyMixin
    virtual Optional<MimeSniff::MimeType> mime_type_impl() const override;
    virtual Optional<Infrastructure::Body&> body_impl() override;
    virtual Optional<Infrastructure::Body const&> body_impl() const override;

    [[nodiscard]] NonnullRefPtr<Infrastructure::Response> response() const { return m_response; }

    // JS API functions
    [[nodiscard]] static JS::NonnullGCPtr<Response> error(JS::VM&);
    [[nodiscard]] static WebIDL::ExceptionOr<JS::NonnullGCPtr<Response>> redirect(JS::VM&, String const& url, u16 status);
    [[nodiscard]] static WebIDL::ExceptionOr<JS::NonnullGCPtr<Response>> json(JS::VM&, JS::Value data, ResponseInit const& init = {});
    [[nodiscard]] Bindings::ResponseType type() const;
    [[nodiscard]] String url() const;
    [[nodiscard]] bool redirected() const;
    [[nodiscard]] u16 status() const;
    [[nodiscard]] bool ok() const;
    [[nodiscard]] String status_text() const;
    [[nodiscard]] JS::NonnullGCPtr<Headers> headers() const;
    [[nodiscard]] WebIDL::ExceptionOr<JS::NonnullGCPtr<Response>> clone() const;

    // Pull in json() from the BodyMixin, which gets lost due to the static json() above
    using BodyMixin::json;

private:
    Response(JS::Realm&, NonnullRefPtr<Infrastructure::Response>);

    virtual void visit_edges(Cell::Visitor&) override;

    WebIDL::ExceptionOr<void> initialize_response(ResponseInit const&, Optional<Infrastructure::BodyWithType> const&);

    // https://fetch.spec.whatwg.org/#concept-response-response
    // A Response object has an associated response (a response).
    NonnullRefPtr<Infrastructure::Response> m_response;

    // https://fetch.spec.whatwg.org/#response-headers
    // A Response object also has an associated headers (null or a Headers object), initially null.
    JS::GCPtr<Headers> m_headers;
};

}
