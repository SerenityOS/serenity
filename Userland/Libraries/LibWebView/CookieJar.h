/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/Traits.h>
#include <LibCore/DateTime.h>
#include <LibSQL/Type.h>
#include <LibWeb/Cookie/Cookie.h>
#include <LibWeb/Forward.h>
#include <LibWebView/Forward.h>

namespace WebView {

struct CookieStorageKey {
    bool operator==(CookieStorageKey const&) const = default;

    DeprecatedString name;
    DeprecatedString domain;
    DeprecatedString path;
};

class CookieJar {
    struct Statements {
        SQL::StatementID create_table { 0 };
        SQL::StatementID insert_cookie { 0 };
        SQL::StatementID update_cookie { 0 };
        SQL::StatementID expire_cookie { 0 };
        SQL::StatementID select_cookie { 0 };
        SQL::StatementID select_all_cookies { 0 };
    };

    struct PersistedStorage {
        Database& database;
        Statements statements;
    };

    using TransientStorage = HashMap<CookieStorageKey, Web::Cookie::Cookie>;

public:
    static ErrorOr<CookieJar> create(Database&);
    static CookieJar create();

    DeprecatedString get_cookie(const URL& url, Web::Cookie::Source source);
    void set_cookie(const URL& url, Web::Cookie::ParsedCookie const& parsed_cookie, Web::Cookie::Source source);
    void update_cookie(Web::Cookie::Cookie);
    void dump_cookies();
    Vector<Web::Cookie::Cookie> get_all_cookies();
    Vector<Web::Cookie::Cookie> get_all_cookies(URL const& url);
    Optional<Web::Cookie::Cookie> get_named_cookie(URL const& url, DeprecatedString const& name);

private:
    explicit CookieJar(PersistedStorage);
    explicit CookieJar(TransientStorage);

    static Optional<DeprecatedString> canonicalize_domain(const URL& url);
    static bool domain_matches(DeprecatedString const& string, DeprecatedString const& domain_string);
    static bool path_matches(DeprecatedString const& request_path, DeprecatedString const& cookie_path);
    static DeprecatedString default_path(const URL& url);

    enum class MatchingCookiesSpecMode {
        RFC6265,
        WebDriver,
    };

    void store_cookie(Web::Cookie::ParsedCookie const& parsed_cookie, const URL& url, DeprecatedString canonicalized_domain, Web::Cookie::Source source);
    Vector<Web::Cookie::Cookie> get_matching_cookies(const URL& url, DeprecatedString const& canonicalized_domain, Web::Cookie::Source source, MatchingCookiesSpecMode mode = MatchingCookiesSpecMode::RFC6265);

    void insert_cookie_into_database(Web::Cookie::Cookie const& cookie);
    void update_cookie_in_database(Web::Cookie::Cookie const& cookie);

    using OnCookieFound = Function<void(Web::Cookie::Cookie&, Web::Cookie::Cookie)>;
    using OnCookieNotFound = Function<void(Web::Cookie::Cookie)>;
    void select_cookie_from_database(Web::Cookie::Cookie cookie, OnCookieFound on_result, OnCookieNotFound on_complete_without_results);

    using OnSelectAllCookiesResult = Function<void(Web::Cookie::Cookie)>;
    void select_all_cookies_from_database(OnSelectAllCookiesResult on_result);

    void purge_expired_cookies();

    Variant<PersistedStorage, TransientStorage> m_storage;
};

}

template<>
struct AK::Traits<WebView::CookieStorageKey> : public AK::GenericTraits<WebView::CookieStorageKey> {
    static unsigned hash(WebView::CookieStorageKey const& key)
    {
        unsigned hash = 0;
        hash = pair_int_hash(hash, string_hash(key.name.characters(), key.name.length()));
        hash = pair_int_hash(hash, string_hash(key.domain.characters(), key.domain.length()));
        hash = pair_int_hash(hash, string_hash(key.path.characters(), key.path.length()));
        return hash;
    }
};
