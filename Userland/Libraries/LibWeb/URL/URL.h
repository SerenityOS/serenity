/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/URL.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/URL/URLSearchParams.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::URL {

class URL : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(URL, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<URL> create(HTML::Window&, AK::URL url, JS::NonnullGCPtr<URLSearchParams> query);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> create_with_global_object(HTML::Window&, String const& url, String const& base);

    virtual ~URL() override;

    String href() const;
    WebIDL::ExceptionOr<void> set_href(String const&);

    String origin() const;

    String protocol() const;
    void set_protocol(String const&);

    String username() const;
    void set_username(String const&);

    String password() const;
    void set_password(String const&);

    String host() const;
    void set_host(String const&);

    String hostname() const;
    void set_hostname(String const&);

    String port() const;
    void set_port(String const&);

    String pathname() const;
    void set_pathname(String const&);

    String search() const;
    void set_search(String const&);

    URLSearchParams const* search_params() const;

    String hash() const;
    void set_hash(String const&);

    String to_json() const;

    void set_query(Badge<URLSearchParams>, String query) { m_url.set_query(move(query)); }

private:
    URL(HTML::Window&, AK::URL, JS::NonnullGCPtr<URLSearchParams> query);

    virtual void visit_edges(Cell::Visitor&) override;

    AK::URL m_url;
    JS::NonnullGCPtr<URLSearchParams> m_query;
};

}
