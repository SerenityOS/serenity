/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/URL.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/URL/URLSearchParams.h>

namespace Web::URL {

class URL : public Bindings::Wrappable
    , public RefCounted<URL>
    , public Weakable<URL> {
public:
    using WrapperType = Bindings::URLWrapper;

    static NonnullRefPtr<URL> create(AK::URL url, NonnullRefPtr<URLSearchParams> query)
    {
        return adopt_ref(*new URL(move(url), move(query)));
    }

    static DOM::ExceptionOr<NonnullRefPtr<URL>> create_with_global_object(Bindings::WindowObject&, const String& url, const String& base);

    String href() const;
    DOM::ExceptionOr<void> set_href(String const&);

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
    explicit URL(AK::URL url, NonnullRefPtr<URLSearchParams> query)
        : m_url(move(url))
        , m_query(move(query)) {};

    AK::URL m_url;
    NonnullRefPtr<URLSearchParams> m_query;
};

}
