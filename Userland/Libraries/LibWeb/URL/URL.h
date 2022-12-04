/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/URL.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/URL/URLSearchParams.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::URL {

class URL : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(URL, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<URL> create(JS::Realm&, AK::URL url, JS::NonnullGCPtr<URLSearchParams> query);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> construct_impl(JS::Realm&, DeprecatedString const& url, DeprecatedString const& base);

    virtual ~URL() override;

    DeprecatedString href() const;
    WebIDL::ExceptionOr<void> set_href(DeprecatedString const&);

    DeprecatedString origin() const;

    DeprecatedString protocol() const;
    void set_protocol(DeprecatedString const&);

    DeprecatedString username() const;
    void set_username(DeprecatedString const&);

    DeprecatedString password() const;
    void set_password(DeprecatedString const&);

    DeprecatedString host() const;
    void set_host(DeprecatedString const&);

    DeprecatedString hostname() const;
    void set_hostname(DeprecatedString const&);

    DeprecatedString port() const;
    void set_port(DeprecatedString const&);

    DeprecatedString pathname() const;
    void set_pathname(DeprecatedString const&);

    DeprecatedString search() const;
    void set_search(DeprecatedString const&);

    URLSearchParams const* search_params() const;

    DeprecatedString hash() const;
    void set_hash(DeprecatedString const&);

    DeprecatedString to_json() const;

    void set_query(Badge<URLSearchParams>, DeprecatedString query) { m_url.set_query(move(query)); }

private:
    URL(JS::Realm&, AK::URL, JS::NonnullGCPtr<URLSearchParams> query);

    virtual void visit_edges(Cell::Visitor&) override;

    AK::URL m_url;
    JS::NonnullGCPtr<URLSearchParams> m_query;
};

HTML::Origin url_origin(AK::URL const&);
bool host_is_domain(StringView host);

}
