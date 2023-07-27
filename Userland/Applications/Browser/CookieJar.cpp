/*
 * Copyright (c) 2021-2023, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CookieJar.h"
#include "Database.h"
#include <AK/IPv4Address.h>
#include <AK/StringBuilder.h>
#include <AK/StringView.h>
#include <AK/Time.h>
#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibCore/Promise.h>
#include <LibSQL/TupleDescriptor.h>
#include <LibSQL/Value.h>
#include <LibWeb/Cookie/ParsedCookie.h>

namespace Browser {

ErrorOr<CookieJar> CookieJar::create(Database& database)
{
    Statements statements {};

    statements.create_table = TRY(database.prepare_statement(R"#(
        CREATE TABLE IF NOT EXISTS Cookies (
            name TEXT,
            value TEXT,
            same_site INTEGER,
            creation_time INTEGER,
            last_access_time INTEGER,
            expiry_time INTEGER,
            domain TEXT,
            path TEXT,
            secure BOOLEAN,
            http_only BOOLEAN,
            host_only BOOLEAN,
            persistent BOOLEAN
        );)#"sv));

    statements.update_cookie = TRY(database.prepare_statement(R"#(
        UPDATE Cookies SET
            value=?,
            same_site=?,
            creation_time=?,
            last_access_time=?,
            expiry_time=?,
            secure=?,
            http_only=?,
            host_only=?,
            persistent=?
        WHERE ((name = ?) AND (domain = ?) AND (path = ?));)#"sv));

    statements.insert_cookie = TRY(database.prepare_statement("INSERT INTO Cookies VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"sv));
    statements.expire_cookie = TRY(database.prepare_statement("DELETE FROM Cookies WHERE (expiry_time < ?);"sv));
    statements.select_cookie = TRY(database.prepare_statement("SELECT * FROM Cookies WHERE ((name = ?) AND (domain = ?) AND (path = ?));"sv));
    statements.select_all_cookies = TRY(database.prepare_statement("SELECT * FROM Cookies;"sv));

    return CookieJar { PersistedStorage { database, move(statements) } };
}

CookieJar CookieJar::create()
{
    return CookieJar { TransientStorage {} };
}

CookieJar::CookieJar(PersistedStorage storage)
    : m_storage(move(storage))
{
    auto& persisted_storage = m_storage.get<PersistedStorage>();
    persisted_storage.database.execute_statement(persisted_storage.statements.create_table, {}, {}, {});
}

CookieJar::CookieJar(TransientStorage storage)
    : m_storage(move(storage))
{
}

DeprecatedString CookieJar::get_cookie(const URL& url, Web::Cookie::Source source)
{
    purge_expired_cookies();

    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return {};

    auto cookie_list = get_matching_cookies(url, domain.value(), source);
    StringBuilder builder;

    for (auto const& cookie : cookie_list) {
        // If there is an unprocessed cookie in the cookie-list, output the characters %x3B and %x20 ("; ")
        if (!builder.is_empty())
            builder.append("; "sv);

        // Output the cookie's name, the %x3D ("=") character, and the cookie's value.
        builder.appendff("{}={}", cookie.name, cookie.value);
    }

    return builder.to_deprecated_string();
}

void CookieJar::set_cookie(const URL& url, Web::Cookie::ParsedCookie const& parsed_cookie, Web::Cookie::Source source)
{
    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return;

    store_cookie(parsed_cookie, url, move(domain.value()), source);
}

// This is based on https://www.rfc-editor.org/rfc/rfc6265#section-5.3 as store_cookie() below
// however the whole ParsedCookie->Cookie conversion is skipped.
void CookieJar::update_cookie(Web::Cookie::Cookie cookie)
{
    select_cookie_from_database(
        move(cookie),

        // 11. If the cookie store contains a cookie with the same name, domain, and path as the newly created cookie:
        [this](auto& cookie, auto old_cookie) {
            // Update the creation-time of the newly created cookie to match the creation-time of the old-cookie.
            cookie.creation_time = old_cookie.creation_time;

            // Remove the old-cookie from the cookie store.
            // NOTE: Rather than deleting then re-inserting this cookie, we update it in-place.
            update_cookie_in_database(cookie);
        },

        // 12. Insert the newly created cookie into the cookie store.
        [this](auto cookie) {
            insert_cookie_into_database(cookie);
        });
}

void CookieJar::dump_cookies()
{
    static constexpr auto key_color = "\033[34;1m"sv;
    static constexpr auto attribute_color = "\033[33m"sv;
    static constexpr auto no_color = "\033[0m"sv;

    StringBuilder builder;
    size_t total_cookies { 0 };

    select_all_cookies_from_database([&](auto cookie) {
        ++total_cookies;

        builder.appendff("{}{}{} - ", key_color, cookie.name, no_color);
        builder.appendff("{}{}{} - ", key_color, cookie.domain, no_color);
        builder.appendff("{}{}{}\n", key_color, cookie.path, no_color);

        builder.appendff("\t{}Value{} = {}\n", attribute_color, no_color, cookie.value);
        builder.appendff("\t{}CreationTime{} = {}\n", attribute_color, no_color, cookie.creation_time_to_string());
        builder.appendff("\t{}LastAccessTime{} = {}\n", attribute_color, no_color, cookie.last_access_time_to_string());
        builder.appendff("\t{}ExpiryTime{} = {}\n", attribute_color, no_color, cookie.expiry_time_to_string());
        builder.appendff("\t{}Secure{} = {:s}\n", attribute_color, no_color, cookie.secure);
        builder.appendff("\t{}HttpOnly{} = {:s}\n", attribute_color, no_color, cookie.http_only);
        builder.appendff("\t{}HostOnly{} = {:s}\n", attribute_color, no_color, cookie.host_only);
        builder.appendff("\t{}Persistent{} = {:s}\n", attribute_color, no_color, cookie.persistent);
        builder.appendff("\t{}SameSite{} = {:s}\n", attribute_color, no_color, Web::Cookie::same_site_to_string(cookie.same_site));
    });

    dbgln("{} cookies stored\n{}", total_cookies, builder.to_deprecated_string());
}

Vector<Web::Cookie::Cookie> CookieJar::get_all_cookies()
{
    Vector<Web::Cookie::Cookie> cookies;

    select_all_cookies_from_database([&](auto cookie) {
        cookies.append(move(cookie));
    });

    return cookies;
}

// https://w3c.github.io/webdriver/#dfn-associated-cookies
Vector<Web::Cookie::Cookie> CookieJar::get_all_cookies(URL const& url)
{
    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return {};

    return get_matching_cookies(url, domain.value(), Web::Cookie::Source::Http, MatchingCookiesSpecMode::WebDriver);
}

Optional<Web::Cookie::Cookie> CookieJar::get_named_cookie(URL const& url, DeprecatedString const& name)
{
    auto domain = canonicalize_domain(url);
    if (!domain.has_value())
        return {};

    auto cookie_list = get_matching_cookies(url, domain.value(), Web::Cookie::Source::Http, MatchingCookiesSpecMode::WebDriver);

    for (auto const& cookie : cookie_list) {
        if (cookie.name == name)
            return cookie;
    }

    return {};
}

Optional<DeprecatedString> CookieJar::canonicalize_domain(const URL& url)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.2
    if (!url.is_valid())
        return {};

    // FIXME: Implement RFC 5890 to "Convert each label that is not a Non-Reserved LDH (NR-LDH) label to an A-label".
    if (url.host().has<Empty>())
        return {};

    return url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string().to_lowercase();
}

bool CookieJar::domain_matches(DeprecatedString const& string, DeprecatedString const& domain_string)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.3

    // A string domain-matches a given domain string if at least one of the following conditions hold:

    // The domain string and the string are identical.
    if (string == domain_string)
        return true;

    // All of the following conditions hold:
    //   - The domain string is a suffix of the string.
    //   - The last character of the string that is not included in the domain string is a %x2E (".") character.
    //   - The string is a host name (i.e., not an IP address).
    if (!string.ends_with(domain_string))
        return false;
    if (string[string.length() - domain_string.length() - 1] != '.')
        return false;
    if (AK::IPv4Address::from_string(string).has_value())
        return false;

    return true;
}

bool CookieJar::path_matches(DeprecatedString const& request_path, DeprecatedString const& cookie_path)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.4

    // A request-path path-matches a given cookie-path if at least one of the following conditions holds:

    // The cookie-path and the request-path are identical.
    if (request_path == cookie_path)
        return true;

    if (request_path.starts_with(cookie_path)) {
        // The cookie-path is a prefix of the request-path, and the last character of the cookie-path is %x2F ("/").
        if (cookie_path.ends_with('/'))
            return true;

        // The cookie-path is a prefix of the request-path, and the first character of the request-path that is not included in the cookie-path is a %x2F ("/") character.
        if (request_path[cookie_path.length()] == '/')
            return true;
    }

    return false;
}

DeprecatedString CookieJar::default_path(const URL& url)
{
    // https://tools.ietf.org/html/rfc6265#section-5.1.4

    // 1. Let uri-path be the path portion of the request-uri if such a portion exists (and empty otherwise).
    DeprecatedString uri_path = url.serialize_path();

    // 2. If the uri-path is empty or if the first character of the uri-path is not a %x2F ("/") character, output %x2F ("/") and skip the remaining steps.
    if (uri_path.is_empty() || (uri_path[0] != '/'))
        return "/";

    StringView uri_path_view = uri_path;
    size_t last_separator = uri_path_view.find_last('/').value();

    // 3. If the uri-path contains no more than one %x2F ("/") character, output %x2F ("/") and skip the remaining step.
    if (last_separator == 0)
        return "/";

    // 4. Output the characters of the uri-path from the first character up to, but not including, the right-most %x2F ("/").
    return uri_path.substring(0, last_separator);
}

void CookieJar::store_cookie(Web::Cookie::ParsedCookie const& parsed_cookie, const URL& url, DeprecatedString canonicalized_domain, Web::Cookie::Source source)
{
    // https://tools.ietf.org/html/rfc6265#section-5.3

    // 2. Create a new cookie with name cookie-name, value cookie-value. Set the creation-time and the last-access-time to the current date and time.
    Web::Cookie::Cookie cookie { parsed_cookie.name, parsed_cookie.value, parsed_cookie.same_site_attribute };
    cookie.creation_time = UnixDateTime::now();
    cookie.last_access_time = cookie.creation_time;

    if (parsed_cookie.expiry_time_from_max_age_attribute.has_value()) {
        // 3. If the cookie-attribute-list contains an attribute with an attribute-name of "Max-Age": Set the cookie's persistent-flag to true.
        // Set the cookie's expiry-time to attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Max-Age".
        cookie.persistent = true;
        cookie.expiry_time = parsed_cookie.expiry_time_from_max_age_attribute.value();
    } else if (parsed_cookie.expiry_time_from_expires_attribute.has_value()) {
        // If the cookie-attribute-list contains an attribute with an attribute-name of "Expires": Set the cookie's persistent-flag to true.
        // Set the cookie's expiry-time to attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Expires".
        cookie.persistent = true;
        cookie.expiry_time = parsed_cookie.expiry_time_from_expires_attribute.value();
    } else {
        // Set the cookie's persistent-flag to false. Set the cookie's expiry-time to the latest representable date.
        cookie.persistent = false;
        cookie.expiry_time = UnixDateTime::latest();
    }

    // 4. If the cookie-attribute-list contains an attribute with an attribute-name of "Domain":
    if (parsed_cookie.domain.has_value()) {
        // Let the domain-attribute be the attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Domain".
        cookie.domain = parsed_cookie.domain.value();
    }

    // 5. If the user agent is configured to reject "public suffixes" and the domain-attribute is a public suffix:
    // FIXME: Support rejection of public suffixes. The full list is here: https://publicsuffix.org/list/public_suffix_list.dat

    // 6. If the domain-attribute is non-empty:
    if (!cookie.domain.is_empty()) {
        // If the canonicalized request-host does not domain-match the domain-attribute: Ignore the cookie entirely and abort these steps.
        if (!domain_matches(canonicalized_domain, cookie.domain))
            return;

        // Set the cookie's host-only-flag to false. Set the cookie's domain to the domain-attribute.
        cookie.host_only = false;
    } else {
        // Set the cookie's host-only-flag to true. Set the cookie's domain to the canonicalized request-host.
        cookie.host_only = true;
        cookie.domain = move(canonicalized_domain);
    }

    // 7. If the cookie-attribute-list contains an attribute with an attribute-name of "Path":
    if (parsed_cookie.path.has_value()) {
        // Set the cookie's path to attribute-value of the last attribute in the cookie-attribute-list with an attribute-name of "Path".
        cookie.path = parsed_cookie.path.value();
    } else {
        cookie.path = default_path(url);
    }

    // 8. If the cookie-attribute-list contains an attribute with an attribute-name of "Secure", set the cookie's secure-only-flag to true.
    cookie.secure = parsed_cookie.secure_attribute_present;

    // 9. If the cookie-attribute-list contains an attribute with an attribute-name of "HttpOnly", set the cookie's http-only-flag to false.
    cookie.http_only = parsed_cookie.http_only_attribute_present;

    // 10. If the cookie was received from a "non-HTTP" API and the cookie's http-only-flag is set, abort these steps and ignore the cookie entirely.
    if (source != Web::Cookie::Source::Http && cookie.http_only)
        return;

    select_cookie_from_database(
        move(cookie),

        // 11. If the cookie store contains a cookie with the same name, domain, and path as the newly created cookie:
        [this, source](auto& cookie, auto old_cookie) {
            // If the newly created cookie was received from a "non-HTTP" API and the old-cookie's http-only-flag is set, abort these
            // steps and ignore the newly created cookie entirely.
            if (source != Web::Cookie::Source::Http && old_cookie.http_only)
                return;

            // Update the creation-time of the newly created cookie to match the creation-time of the old-cookie.
            cookie.creation_time = old_cookie.creation_time;

            // Remove the old-cookie from the cookie store.
            // NOTE: Rather than deleting then re-inserting this cookie, we update it in-place.
            update_cookie_in_database(cookie);
        },

        // 12. Insert the newly created cookie into the cookie store.
        [this](auto cookie) {
            insert_cookie_into_database(cookie);
        });
}

Vector<Web::Cookie::Cookie> CookieJar::get_matching_cookies(const URL& url, DeprecatedString const& canonicalized_domain, Web::Cookie::Source source, MatchingCookiesSpecMode mode)
{
    // https://tools.ietf.org/html/rfc6265#section-5.4

    // 1. Let cookie-list be the set of cookies from the cookie store that meets all of the following requirements:
    Vector<Web::Cookie::Cookie> cookie_list;

    select_all_cookies_from_database([&](auto cookie) {
        // Either: The cookie's host-only-flag is true and the canonicalized request-host is identical to the cookie's domain.
        // Or: The cookie's host-only-flag is false and the canonicalized request-host domain-matches the cookie's domain.
        bool is_host_only_and_has_identical_domain = cookie.host_only && (canonicalized_domain == cookie.domain);
        bool is_not_host_only_and_domain_matches = !cookie.host_only && domain_matches(canonicalized_domain, cookie.domain);
        if (!is_host_only_and_has_identical_domain && !is_not_host_only_and_domain_matches)
            return;

        // The request-uri's path path-matches the cookie's path.
        if (!path_matches(url.serialize_path(), cookie.path))
            return;

        // If the cookie's secure-only-flag is true, then the request-uri's scheme must denote a "secure" protocol.
        if (cookie.secure && (url.scheme() != "https"))
            return;

        // If the cookie's http-only-flag is true, then exclude the cookie if the cookie-string is being generated for a "non-HTTP" API.
        if (cookie.http_only && (source != Web::Cookie::Source::Http))
            return;

        // NOTE: The WebDriver spec expects only step 1 above to be executed to match cookies.
        if (mode == MatchingCookiesSpecMode::WebDriver) {
            cookie_list.append(move(cookie));
            return;
        }

        // 2.  The user agent SHOULD sort the cookie-list in the following order:
        //   - Cookies with longer paths are listed before cookies with shorter paths.
        //   - Among cookies that have equal-length path fields, cookies with earlier creation-times are listed before cookies with later creation-times.
        auto cookie_path_length = cookie.path.length();
        auto cookie_creation_time = cookie.creation_time;

        cookie_list.insert_before_matching(move(cookie), [cookie_path_length, cookie_creation_time](auto const& entry) {
            if (cookie_path_length > entry.path.length()) {
                return true;
            } else if (cookie_path_length == entry.path.length()) {
                if (cookie_creation_time < entry.creation_time)
                    return true;
            }
            return false;
        });
    });

    // 3. Update the last-access-time of each cookie in the cookie-list to the current date and time.
    auto now = UnixDateTime::now();

    for (auto& cookie : cookie_list) {
        cookie.last_access_time = now;
        update_cookie_in_database(cookie);
    }

    return cookie_list;
}

static ErrorOr<Web::Cookie::Cookie> parse_cookie(ReadonlySpan<SQL::Value> row)
{
    if (row.size() != 12)
        return Error::from_string_view("Incorrect number of columns to parse cookie"sv);

    size_t index = 0;

    auto convert_text = [&](auto& field, StringView name) -> ErrorOr<void> {
        auto const& value = row[index++];
        if (value.type() != SQL::SQLType::Text)
            return Error::from_string_view(name);

        field = value.to_deprecated_string();
        return {};
    };

    auto convert_bool = [&](auto& field, StringView name) -> ErrorOr<void> {
        auto const& value = row[index++];
        if (value.type() != SQL::SQLType::Boolean)
            return Error::from_string_view(name);

        field = value.to_bool().value();
        return {};
    };

    auto convert_time = [&](auto& field, StringView name) -> ErrorOr<void> {
        auto const& value = row[index++];
        if (value.type() != SQL::SQLType::Integer)
            return Error::from_string_view(name);

        auto time = value.to_int<i64>().value();
        field = UnixDateTime::from_seconds_since_epoch(time);
        return {};
    };

    auto convert_same_site = [&](auto& field, StringView name) -> ErrorOr<void> {
        auto const& value = row[index++];
        if (value.type() != SQL::SQLType::Integer)
            return Error::from_string_view(name);

        auto same_site = value.to_int<UnderlyingType<Web::Cookie::SameSite>>().value();
        if (same_site > to_underlying(Web::Cookie::SameSite::Lax))
            return Error::from_string_view(name);

        field = static_cast<Web::Cookie::SameSite>(same_site);
        return {};
    };

    Web::Cookie::Cookie cookie;
    TRY(convert_text(cookie.name, "name"sv));
    TRY(convert_text(cookie.value, "value"sv));
    TRY(convert_same_site(cookie.same_site, "same_site"sv));
    TRY(convert_time(cookie.creation_time, "creation_time"sv));
    TRY(convert_time(cookie.last_access_time, "last_access_time"sv));
    TRY(convert_time(cookie.expiry_time, "expiry_time"sv));
    TRY(convert_text(cookie.domain, "domain"sv));
    TRY(convert_text(cookie.path, "path"sv));
    TRY(convert_bool(cookie.secure, "secure"sv));
    TRY(convert_bool(cookie.http_only, "http_only"sv));
    TRY(convert_bool(cookie.host_only, "host_only"sv));
    TRY(convert_bool(cookie.persistent, "persistent"sv));

    return cookie;
}

void CookieJar::insert_cookie_into_database(Web::Cookie::Cookie const& cookie)
{
    m_storage.visit(
        [&](PersistedStorage& storage) {
            storage.database.execute_statement(
                storage.statements.insert_cookie, {}, [this]() { purge_expired_cookies(); }, {},
                cookie.name,
                cookie.value,
                to_underlying(cookie.same_site),
                cookie.creation_time.seconds_since_epoch(),
                cookie.last_access_time.seconds_since_epoch(),
                cookie.expiry_time.seconds_since_epoch(),
                cookie.domain,
                cookie.path,
                cookie.secure,
                cookie.http_only,
                cookie.host_only,
                cookie.persistent);
        },
        [&](TransientStorage& storage) {
            CookieStorageKey key { cookie.name, cookie.domain, cookie.path };
            storage.set(key, cookie);
        });
}

void CookieJar::update_cookie_in_database(Web::Cookie::Cookie const& cookie)
{
    m_storage.visit(
        [&](PersistedStorage& storage) {
            storage.database.execute_statement(
                storage.statements.update_cookie, {}, [this]() { purge_expired_cookies(); }, {},
                cookie.value,
                to_underlying(cookie.same_site),
                cookie.creation_time.seconds_since_epoch(),
                cookie.last_access_time.seconds_since_epoch(),
                cookie.expiry_time.seconds_since_epoch(),
                cookie.secure,
                cookie.http_only,
                cookie.host_only,
                cookie.persistent,
                cookie.name,
                cookie.domain,
                cookie.path);
        },
        [&](TransientStorage& storage) {
            CookieStorageKey key { cookie.name, cookie.domain, cookie.path };
            storage.set(key, cookie);
        });
}

struct WrappedCookie : public RefCounted<WrappedCookie> {
    explicit WrappedCookie(Web::Cookie::Cookie cookie_)
        : RefCounted()
        , cookie(move(cookie_))
    {
    }

    Web::Cookie::Cookie cookie;
    bool had_any_results { false };
};

void CookieJar::select_cookie_from_database(Web::Cookie::Cookie cookie, OnCookieFound on_result, OnCookieNotFound on_complete_without_results)
{
    m_storage.visit(
        [&](PersistedStorage& storage) {
            auto wrapped_cookie = make_ref_counted<WrappedCookie>(move(cookie));

            storage.database.execute_statement(
                storage.statements.select_cookie,
                [on_result = move(on_result), wrapped_cookie = wrapped_cookie](auto row) {
                    if (auto selected_cookie = parse_cookie(row); selected_cookie.is_error())
                        dbgln("Failed to parse cookie '{}': {}", selected_cookie.error(), row);
                    else
                        on_result(wrapped_cookie->cookie, selected_cookie.release_value());

                    wrapped_cookie->had_any_results = true;
                },
                [on_complete_without_results = move(on_complete_without_results), wrapped_cookie = wrapped_cookie]() {
                    if (!wrapped_cookie->had_any_results)
                        on_complete_without_results(move(wrapped_cookie->cookie));
                },
                {},
                wrapped_cookie->cookie.name,
                wrapped_cookie->cookie.domain,
                wrapped_cookie->cookie.path);
        },
        [&](TransientStorage& storage) {
            CookieStorageKey key { cookie.name, cookie.domain, cookie.path };

            if (auto it = storage.find(key); it != storage.end())
                on_result(cookie, it->value);
            else
                on_complete_without_results(cookie);
        });
}

void CookieJar::select_all_cookies_from_database(OnSelectAllCookiesResult on_result)
{
    // FIXME: Make surrounding APIs asynchronous.
    m_storage.visit(
        [&](PersistedStorage& storage) {
            auto promise = Core::Promise<Empty>::construct();

            storage.database.execute_statement(
                storage.statements.select_all_cookies,
                [on_result = move(on_result)](auto row) {
                    if (auto cookie = parse_cookie(row); cookie.is_error())
                        dbgln("Failed to parse cookie '{}': {}", cookie.error(), row);
                    else
                        on_result(cookie.release_value());
                },
                [&]() {
                    promise->resolve({});
                },
                [&](auto) {
                    promise->resolve({});
                });

            MUST(promise->await());
        },
        [&](TransientStorage& storage) {
            for (auto const& cookie : storage)
                on_result(cookie.value);
        });
}

void CookieJar::purge_expired_cookies()
{
    auto now = UnixDateTime::now();

    m_storage.visit(
        [&](PersistedStorage& storage) {
            storage.database.execute_statement(storage.statements.expire_cookie, {}, {}, {}, now);
        },
        [&](TransientStorage& storage) {
            Vector<CookieStorageKey> keys_to_evict;

            for (auto const& cookie : storage) {
                if (cookie.value.expiry_time < now)
                    keys_to_evict.append(cookie.key);
            }

            for (auto const& key : keys_to_evict)
                storage.remove(key);
        });
}

}
