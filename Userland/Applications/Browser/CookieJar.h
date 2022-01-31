/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Traits.h>
#include <LibCore/DateTime.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Forward.h>

namespace Browser {

struct CookieStorageKey {
    bool operator==(const CookieStorageKey&) const = default;

    String name;
    String domain;
    String path;
};

class CookieJar {
public:
    String get_cookie(const URL& url, Web::Cookie::Source source);
    void set_cookie(const URL& url, const Web::Cookie::ParsedCookie& parsed_cookie, Web::Cookie::Source source);
    void dump_cookies() const;

private:
    static Optional<String> canonicalize_domain(const URL& url);
    static bool domain_matches(const String& string, const String& domain_string);
    static bool path_matches(const String& request_path, const String& cookie_path);
    static String default_path(const URL& url);

    void store_cookie(const Web::Cookie::ParsedCookie& parsed_cookie, const URL& url, String canonicalized_domain, Web::Cookie::Source source);
    Vector<Web::Cookie::Cookie&> get_matching_cookies(const URL& url, const String& canonicalized_domain, Web::Cookie::Source source);
    void purge_expired_cookies();

    HashMap<CookieStorageKey, Web::Cookie::Cookie> m_cookies;
};

}

namespace AK {

template<>
struct Traits<Browser::CookieStorageKey> : public GenericTraits<Browser::CookieStorageKey> {
    static unsigned hash(const Browser::CookieStorageKey& key)
    {
        unsigned hash = 0;
        hash = pair_int_hash(hash, string_hash(key.name.characters(), key.name.length()));
        hash = pair_int_hash(hash, string_hash(key.domain.characters(), key.domain.length()));
        hash = pair_int_hash(hash, string_hash(key.path.characters(), key.path.length()));
        return hash;
    }
};

}
