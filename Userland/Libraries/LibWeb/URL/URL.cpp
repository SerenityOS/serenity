/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv4Address.h>
#include <AK/IPv6Address.h>
#include <AK/URLParser.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/URL/URL.h>

namespace Web::URL {

WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> URL::create(JS::Realm& realm, AK::URL url, JS::NonnullGCPtr<URLSearchParams> query)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<URL>(realm, realm, move(url), move(query)));
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> URL::construct_impl(JS::Realm& realm, DeprecatedString const& url, DeprecatedString const& base)
{
    // 1. Let parsedBase be null.
    Optional<AK::URL> parsed_base;
    // 2. If base is given, then:
    if (!base.is_null()) {
        // 1. Let parsedBase be the result of running the basic URL parser on base.
        parsed_base = base;
        // 2. If parsedBase is failure, then throw a TypeError.
        if (!parsed_base->is_valid())
            return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid base URL"sv };
    }
    // 3. Let parsedURL be the result of running the basic URL parser on url with parsedBase.
    AK::URL parsed_url;
    if (parsed_base.has_value())
        parsed_url = parsed_base->complete_url(url);
    else
        parsed_url = url;
    // 4. If parsedURL is failure, then throw a TypeError.
    if (!parsed_url.is_valid())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid URL"sv };
    // 5. Let query be parsedURL’s query, if that is non-null, and the empty string otherwise.
    auto& query = parsed_url.query().is_null() ? DeprecatedString::empty() : parsed_url.query();
    // 6. Set this’s URL to parsedURL.
    // 7. Set this’s query object to a new URLSearchParams object.
    auto query_object = MUST(URLSearchParams::construct_impl(realm, query));
    // 8. Initialize this’s query object with query.
    auto result_url = TRY(URL::create(realm, move(parsed_url), move(query_object)));
    // 9. Set this’s query object’s URL object to this.
    result_url->m_query->m_url = result_url;

    return result_url;
}

URL::URL(JS::Realm& realm, AK::URL url, JS::NonnullGCPtr<URLSearchParams> query)
    : PlatformObject(realm)
    , m_url(move(url))
    , m_query(move(query))
{
}

URL::~URL() = default;

JS::ThrowCompletionOr<void> URL::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::URLPrototype>(realm, "URL"));

    return {};
}

void URL::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_query.ptr());
}

DeprecatedString URL::href() const
{
    // return the serialization of this’s URL.
    return m_url.serialize();
}

DeprecatedString URL::to_json() const
{
    // return the serialization of this’s URL.
    return m_url.serialize();
}

WebIDL::ExceptionOr<void> URL::set_href(DeprecatedString const& href)
{
    // 1. Let parsedURL be the result of running the basic URL parser on the given value.
    AK::URL parsed_url = href;
    // 2. If parsedURL is failure, then throw a TypeError.
    if (!parsed_url.is_valid())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid URL"sv };
    // 3. Set this’s URL to parsedURL.
    m_url = move(parsed_url);
    // 4. Empty this’s query object’s list.
    m_query->m_list.clear();
    // 5. Let query be this’s URL’s query.
    auto& query = m_url.query();
    // 6. If query is non-null, then set this’s query object’s list to the result of parsing query.
    if (!query.is_null())
        m_query->m_list = url_decode(query);
    return {};
}

DeprecatedString URL::origin() const
{
    // return the serialization of this’s URL’s origin.
    return m_url.serialize_origin();
}

DeprecatedString URL::protocol() const
{
    // return this’s URL’s scheme, followed by U+003A (:).
    return DeprecatedString::formatted("{}:", m_url.scheme());
}

void URL::set_protocol(DeprecatedString const& protocol)
{
    // basic URL parse the given value, followed by U+003A (:), with this’s URL as url and scheme start state as state override.
    auto result_url = URLParser::parse(DeprecatedString::formatted("{}:", protocol), nullptr, m_url, URLParser::State::SchemeStart);
    if (result_url.is_valid())
        m_url = move(result_url);
}

DeprecatedString URL::username() const
{
    // return this’s URL’s username.
    return m_url.username();
}

void URL::set_username(DeprecatedString const& username)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;
    // 2. Set the username given this’s URL and the given value.
    m_url.set_username(AK::URL::percent_encode(username, AK::URL::PercentEncodeSet::Userinfo));
}

DeprecatedString URL::password() const
{
    // return this’s URL’s password.
    return m_url.password();
}

void URL::set_password(DeprecatedString const& password)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;
    // 2. Set the password given this’s URL and the given value.
    m_url.set_password(AK::URL::percent_encode(password, AK::URL::PercentEncodeSet::Userinfo));
}

DeprecatedString URL::host() const
{
    // 1. Let url be this’s URL.
    auto& url = m_url;
    // 2. If url’s host is null, then return the empty string.
    if (url.host().is_null())
        return DeprecatedString::empty();
    // 3. If url’s port is null, return url’s host, serialized.
    if (!url.port().has_value())
        return url.host();
    // 4. Return url’s host, serialized, followed by U+003A (:) and url’s port, serialized.
    return DeprecatedString::formatted("{}:{}", url.host(), *url.port());
}

void URL::set_host(DeprecatedString const& host)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;
    // 2. Basic URL parse the given value with this’s URL as url and host state as state override.
    auto result_url = URLParser::parse(host, nullptr, m_url, URLParser::State::Host);
    if (result_url.is_valid())
        m_url = move(result_url);
}

DeprecatedString URL::hostname() const
{
    // 1. If this’s URL’s host is null, then return the empty string.
    if (m_url.host().is_null())
        return DeprecatedString::empty();
    // 2. Return this’s URL’s host, serialized.
    return m_url.host();
}

void URL::set_hostname(DeprecatedString const& hostname)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;
    // 2. Basic URL parse the given value with this’s URL as url and hostname state as state override.
    auto result_url = URLParser::parse(hostname, nullptr, m_url, URLParser::State::Hostname);
    if (result_url.is_valid())
        m_url = move(result_url);
}

DeprecatedString URL::port() const
{
    // 1. If this’s URL’s port is null, then return the empty string.
    if (!m_url.port().has_value())
        return {};

    // 2. Return this’s URL’s port, serialized.
    return DeprecatedString::formatted("{}", *m_url.port());
}

void URL::set_port(DeprecatedString const& port)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;

    // 2. If the given value is the empty string, then set this’s URL’s port to null.
    if (port.is_empty()) {
        m_url.set_port({});
        return;
    }

    // 3. Otherwise, basic URL parse the given value with this’s URL as url and port state as state override.
    auto result_url = URLParser::parse(port, nullptr, m_url, URLParser::State::Port);
    if (result_url.is_valid())
        m_url = move(result_url);
}

DeprecatedString URL::pathname() const
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return this’s URL’s path[0].
    // 2. If this’s URL’s path is empty, then return the empty string.
    // 3. Return U+002F (/), followed by the strings in this’s URL’s path (including empty strings), if any, separated from each other by U+002F (/).
    return m_url.path();
}

void URL::set_pathname(DeprecatedString const& pathname)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;
    // 2. Empty this’s URL’s path.
    auto url = m_url; // We copy the URL here to follow other browser's behaviour of reverting the path change if the parse failed.
    url.set_paths({});
    // 3. Basic URL parse the given value with this’s URL as url and path start state as state override.
    auto result_url = URLParser::parse(pathname, nullptr, move(url), URLParser::State::PathStart);
    if (result_url.is_valid())
        m_url = move(result_url);
}

DeprecatedString URL::search() const
{
    // 1. If this’s URL’s query is either null or the empty string, then return the empty string.
    if (m_url.query().is_null() || m_url.query().is_empty())
        return DeprecatedString::empty();
    // 2. Return U+003F (?), followed by this’s URL’s query.
    return DeprecatedString::formatted("?{}", m_url.query());
}

void URL::set_search(DeprecatedString const& search)
{
    // 1. Let url be this’s URL.
    auto& url = m_url;
    // If the given value is the empty string, set url’s query to null, empty this’s query object’s list, and then return.
    if (search.is_empty()) {
        url.set_query({});
        m_query->m_list.clear();
        return;
    }
    // 2. Let input be the given value with a single leading U+003F (?) removed, if any.
    auto input = search.substring_view(search.starts_with('?'));
    // 3. Set url’s query to the empty string.
    auto url_copy = url; // We copy the URL here to follow other browser's behaviour of reverting the search change if the parse failed.
    url_copy.set_query(DeprecatedString::empty());
    // 4. Basic URL parse input with url as url and query state as state override.
    auto result_url = URLParser::parse(input, nullptr, move(url_copy), URLParser::State::Query);
    if (result_url.is_valid()) {
        m_url = move(result_url);
        // 5. Set this’s query object’s list to the result of parsing input.
        m_query->m_list = url_decode(input);
    }
}

URLSearchParams const* URL::search_params() const
{
    return m_query;
}

DeprecatedString URL::hash() const
{
    // 1. If this’s URL’s fragment is either null or the empty string, then return the empty string.
    if (m_url.fragment().is_null() || m_url.fragment().is_empty())
        return DeprecatedString::empty();
    // 2. Return U+0023 (#), followed by this’s URL’s fragment.
    return DeprecatedString::formatted("#{}", m_url.fragment());
}

void URL::set_hash(DeprecatedString const& hash)
{
    // 1. If the given value is the empty string, then set this’s URL’s fragment to null and return.
    if (hash.is_empty()) {
        m_url.set_fragment({});
        return;
    }
    // 2. Let input be the given value with a single leading U+0023 (#) removed, if any.
    auto input = hash.substring_view(hash.starts_with('#'));
    // 3. Set this’s URL’s fragment to the empty string.
    auto url = m_url; // We copy the URL here to follow other browser's behaviour of reverting the hash change if the parse failed.
    url.set_fragment(DeprecatedString::empty());
    // 4. Basic URL parse input with this’s URL as url and fragment state as state override.
    auto result_url = URLParser::parse(input, nullptr, move(url), URLParser::State::Fragment);
    if (result_url.is_valid())
        m_url = move(result_url);
}

// https://url.spec.whatwg.org/#concept-url-origin
HTML::Origin url_origin(AK::URL const& url)
{
    // FIXME: We should probably have an extended version of AK::URL for LibWeb instead of standalone functions like this.

    // The origin of a URL url is the origin returned by running these steps, switching on url’s scheme:
    // "blob"
    if (url.scheme() == "blob"sv) {
        // FIXME: Support 'blob://' URLs
        return HTML::Origin {};
    }

    // "ftp"
    // "http"
    // "https"
    // "ws"
    // "wss"
    if (url.scheme().is_one_of("ftp"sv, "http"sv, "https"sv, "ws"sv, "wss"sv)) {
        // Return the tuple origin (url’s scheme, url’s host, url’s port, null).
        return HTML::Origin(url.scheme(), url.host(), url.port().value_or(0));
    }

    // "file"
    if (url.scheme() == "file"sv) {
        // Unfortunate as it is, this is left as an exercise to the reader. When in doubt, return a new opaque origin.
        // Note: We must return an origin with the `file://' protocol for `file://' iframes to work from `file://' pages.
        return HTML::Origin(url.scheme(), DeprecatedString(), 0);
    }

    // Return a new opaque origin.
    return HTML::Origin {};
}

// https://url.spec.whatwg.org/#concept-domain
bool host_is_domain(StringView host)
{
    // A domain is a non-empty ASCII string that identifies a realm within a network.
    return !host.is_empty()
        && !IPv4Address::from_string(host).has_value()
        && !IPv6Address::from_string(host).has_value();
}

}
