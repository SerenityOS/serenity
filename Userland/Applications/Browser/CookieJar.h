/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@pm.me>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Traits.h>
#include <LibCore/DateTime.h>

namespace Browser {

struct Cookie {
    String name;
    String value;
    Core::DateTime creation_time {};
    Core::DateTime last_access_time {};
    Core::DateTime expiry_time {};
    String domain {};
    String path {};
    bool secure { false };
    bool http_only { false };
    bool host_only { false };
    bool persistent { false };
};

struct ParsedCookie;

struct CookieStorageKey {
    bool operator==(const CookieStorageKey&) const = default;

    String name;
    String domain;
    String path;
};

class CookieJar {
public:
    String get_cookie(const URL& url);
    void set_cookie(const URL& url, const String& cookie);
    void dump_cookies() const;

private:
    static Optional<String> canonicalize_domain(const URL& url);
    static String default_path(const URL& url);
    static Optional<ParsedCookie> parse_cookie(const String& cookie_string);
    static void parse_attributes(ParsedCookie& parsed_cookie, StringView unparsed_attributes);
    static void process_attribute(ParsedCookie& parsed_cookie, StringView attribute_name, StringView attribute_value);
    static void on_expires_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
    static void on_max_age_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
    static void on_domain_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
    static void on_path_attribute(ParsedCookie& parsed_cookie, StringView attribute_value);
    static void on_secure_attribute(ParsedCookie& parsed_cookie);
    static void on_http_only_attribute(ParsedCookie& parsed_cookie);
    static Optional<Core::DateTime> parse_date_time(StringView date_string);
    static bool domain_matches(const String& string, const String& domain_string);

    void store_cookie(ParsedCookie& parsed_cookie, const URL& url, String canonicalized_domain);
    void purge_expired_cookies();

    HashMap<CookieStorageKey, Cookie> m_cookies;
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
