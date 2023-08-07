/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/URL/URLSearchParams.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::URL {

class URL : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(URL, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> create(JS::Realm&, AK::URL url, JS::NonnullGCPtr<URLSearchParams> query);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> construct_impl(JS::Realm&, String const& url, Optional<String> const& base = {});

    virtual ~URL() override;

    static WebIDL::ExceptionOr<String> create_object_url(JS::VM&, JS::NonnullGCPtr<FileAPI::Blob> object);
    static WebIDL::ExceptionOr<void> revoke_object_url(JS::VM&, StringView url);

    static bool can_parse(JS::VM&, String const& url, Optional<String> const& base = {});

    WebIDL::ExceptionOr<String> href() const;
    WebIDL::ExceptionOr<void> set_href(String const&);

    WebIDL::ExceptionOr<String> origin() const;

    WebIDL::ExceptionOr<String> protocol() const;
    WebIDL::ExceptionOr<void> set_protocol(String const&);

    WebIDL::ExceptionOr<String> username() const;
    void set_username(String const&);

    WebIDL::ExceptionOr<String> password() const;
    void set_password(String const&);

    WebIDL::ExceptionOr<String> host() const;
    void set_host(String const&);

    WebIDL::ExceptionOr<String> hostname() const;
    void set_hostname(String const&);

    WebIDL::ExceptionOr<String> port() const;
    void set_port(String const&);

    WebIDL::ExceptionOr<String> pathname() const;
    void set_pathname(String const&);

    WebIDL::ExceptionOr<String> search() const;
    WebIDL::ExceptionOr<void> set_search(String const&);

    JS::NonnullGCPtr<URLSearchParams const> search_params() const;

    WebIDL::ExceptionOr<String> hash() const;
    void set_hash(String const&);

    WebIDL::ExceptionOr<String> to_json() const;

    void set_query(Badge<URLSearchParams>, StringView query) { m_url.set_query(query); }

private:
    URL(JS::Realm&, AK::URL, JS::NonnullGCPtr<URLSearchParams> query);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    AK::URL m_url;
    JS::NonnullGCPtr<URLSearchParams> m_query;
};

HTML::Origin url_origin(AK::URL const&);
bool host_is_domain(AK::URL::Host const&);

// https://url.spec.whatwg.org/#concept-url-parser
AK::URL parse(StringView input, Optional<AK::URL> const& base_url = {});

}
