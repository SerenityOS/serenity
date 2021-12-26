/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URLParser.h>
#include <LibWeb/URL/URL.h>

namespace Web::URL {

DOM::ExceptionOr<NonnullRefPtr<URL>> URL::create_with_global_object(Bindings::WindowObject& window_object, String const& url, String const& base)
{
    // 1. Let parsedBase be null.
    Optional<AK::URL> parsed_base;
    // 2. If base is given, then:
    if (!base.is_null()) {
        // 1. Let parsedBase be the result of running the basic URL parser on base.
        parsed_base = base;
        // 2. If parsedBase is failure, then throw a TypeError.
        if (!parsed_base->is_valid())
            return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, "Invalid base URL" };
    }
    // 3. Let parsedURL be the result of running the basic URL parser on url with parsedBase.
    AK::URL parsed_url;
    if (parsed_base.has_value())
        parsed_url = parsed_base->complete_url(url);
    else
        parsed_url = url;
    // 4. If parsedURL is failure, then throw a TypeError.
    if (!parsed_url.is_valid())
        return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, "Invalid URL" };
    // 5. Let query be parsedURL’s query, if that is non-null, and the empty string otherwise.
    auto& query = parsed_url.query().is_null() ? String::empty() : parsed_url.query();
    // 6. Set this’s URL to parsedURL.
    // 7. Set this’s query object to a new URLSearchParams object.
    auto query_object = URLSearchParams::create_with_global_object(window_object, query);
    VERIFY(!query_object.is_exception()); // The string variant of the constructor can't throw.
    // 8. Initialize this’s query object with query.
    auto result_url = URL::create(move(parsed_url), query_object.release_value());
    // 9. Set this’s query object’s URL object to this.
    result_url->m_query->m_url = result_url;

    return result_url;
}

String URL::href() const
{
    // return the serialization of this’s URL.
    return m_url.serialize();
}

String URL::to_json() const
{
    // return the serialization of this’s URL.
    return m_url.serialize();
}

DOM::ExceptionOr<void> URL::set_href(String const& href)
{
    // 1. Let parsedURL be the result of running the basic URL parser on the given value.
    AK::URL parsed_url = href;
    // 2. If parsedURL is failure, then throw a TypeError.
    if (!parsed_url.is_valid())
        return DOM::SimpleException { DOM::SimpleExceptionType::TypeError, "Invalid URL" };
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

String URL::origin() const
{
    // return the serialization of this’s URL’s origin.
    return m_url.serialize_origin();
}

String URL::username() const
{
    // return this’s URL’s username.
    return m_url.username();
}

void URL::set_username(const String& username)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;
    // 2. Set the username given this’s URL and the given value.
    m_url.set_username(AK::URL::percent_encode(username, AK::URL::PercentEncodeSet::Userinfo));
}

String URL::password() const
{
    // return this’s URL’s password.
    return m_url.password();
}

void URL::set_password(String const& password)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;
    // 2. Set the password given this’s URL and the given value.
    m_url.set_password(AK::URL::percent_encode(password, AK::URL::PercentEncodeSet::Userinfo));
}

String URL::host() const
{
    // 1. Let url be this’s URL.
    auto& url = m_url;
    // 2. If url’s host is null, then return the empty string.
    if (url.host().is_null())
        return String::empty();
    // 3. If url’s port is null, return url’s host, serialized.
    if (!url.port().has_value())
        return url.host();
    // 4. Return url’s host, serialized, followed by U+003A (:) and url’s port, serialized.
    return String::formatted("{}:{}", url.host(), *url.port());
}

void URL::set_host(const String& host)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;
    // 2. Basic URL parse the given value with this’s URL as url and host state as state override.
    auto result_url = URLParser::parse(host, nullptr, m_url, URLParser::State::Host);
    if (result_url.is_valid())
        m_url = move(result_url);
}

String URL::hostname() const
{
    // 1. If this’s URL’s host is null, then return the empty string.
    if (m_url.host().is_null())
        return String::empty();
    // 2. Return this’s URL’s host, serialized.
    return m_url.host();
}

void URL::set_hostname(String const& hostname)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;
    // 2. Basic URL parse the given value with this’s URL as url and hostname state as state override.
    auto result_url = URLParser::parse(hostname, nullptr, m_url, URLParser::State::Hostname);
    if (result_url.is_valid())
        m_url = move(result_url);
}

String URL::port() const
{
    // 1. If this’s URL’s port is null, then return the empty string.
    if (!m_url.port().has_value())
        return {};

    // 2. Return this’s URL’s port, serialized.
    return String::formatted("{}", *m_url.port());
}

void URL::set_port(String const& port)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;
    // 2. If the given value is the empty string, then set this’s URL’s port to null.
    if (port.is_empty())
        m_url.set_port({});
    // 3. Otherwise, basic URL parse the given value with this’s URL as url and port state as state override.
    auto result_url = URLParser::parse(port, nullptr, m_url, URLParser::State::Port);
    if (result_url.is_valid())
        m_url = move(result_url);
}

URLSearchParams const* URL::search_params() const
{
    return m_query;
}

}
