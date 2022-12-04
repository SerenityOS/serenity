/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/Traits.h>
#include <LibCore/DateTime.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Forward.h>

namespace Browser {

struct CookieStorageKey {
    bool operator==(CookieStorageKey const&) const = default;

    DeprecatedString name;
    DeprecatedString domain;
    DeprecatedString path;
};

class CookieJar {
public:
    DeprecatedString get_cookie(const URL& url, Web::Cookie::Source source);
    void set_cookie(const URL& url, Web::Cookie::ParsedCookie const& parsed_cookie, Web::Cookie::Source source);
    void update_cookie(URL const&, Web::Cookie::Cookie);
    void dump_cookies() const;
    Vector<Web::Cookie::Cookie> get_all_cookies() const;
    Vector<Web::Cookie::Cookie> get_all_cookies(URL const& url);
    Optional<Web::Cookie::Cookie> get_named_cookie(URL const& url, DeprecatedString const& name);

private:
    static Optional<DeprecatedString> canonicalize_domain(const URL& url);
    static bool domain_matches(DeprecatedString const& string, DeprecatedString const& domain_string);
    static bool path_matches(DeprecatedString const& request_path, DeprecatedString const& cookie_path);
    static DeprecatedString default_path(const URL& url);

    enum class MatchingCookiesSpecMode {
        RFC6265,
        WebDriver,
    };

    void store_cookie(Web::Cookie::ParsedCookie const& parsed_cookie, const URL& url, DeprecatedString canonicalized_domain, Web::Cookie::Source source);
    Vector<Web::Cookie::Cookie&> get_matching_cookies(const URL& url, DeprecatedString const& canonicalized_domain, Web::Cookie::Source source, MatchingCookiesSpecMode mode = MatchingCookiesSpecMode::RFC6265);
    void purge_expired_cookies();

    HashMap<CookieStorageKey, Web::Cookie::Cookie> m_cookies;
};

}

namespace AK {

template<>
struct Traits<Browser::CookieStorageKey> : public GenericTraits<Browser::CookieStorageKey> {
    static unsigned hash(Browser::CookieStorageKey const& key)
    {
        unsigned hash = 0;
        hash = pair_int_hash(hash, string_hash(key.name.characters(), key.name.length()));
        hash = pair_int_hash(hash, string_hash(key.domain.characters(), key.domain.length()));
        hash = pair_int_hash(hash, string_hash(key.path.characters(), key.path.length()));
        return hash;
    }
};

}
