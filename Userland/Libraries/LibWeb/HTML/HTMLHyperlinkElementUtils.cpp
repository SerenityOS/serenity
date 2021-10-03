/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URLParser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHyperlinkElementUtils.h>

namespace Web::HTML {

HTMLHyperlinkElementUtils::~HTMLHyperlinkElementUtils()
{
}

// https://html.spec.whatwg.org/multipage/links.html#reinitialise-url
void HTMLHyperlinkElementUtils::reinitialize_url() const
{
    // 1. If element's url is non-null, its scheme is "blob", and its cannot-be-a-basFe-URL is true, terminate these steps.
    if (m_url.has_value() && m_url->scheme() == "blob"sv && m_url->cannot_be_a_base_url())
        return;

    // 2. Set the url.
    const_cast<HTMLHyperlinkElementUtils*>(this)->set_the_url();
}

// https://html.spec.whatwg.org/multipage/links.html#concept-hyperlink-url-set
void HTMLHyperlinkElementUtils::set_the_url()
{
    // 1. If this element's href content attribute is absent, set this element's url to null.
    auto href_content_attribute = hyperlink_element_utils_href();
    if (href_content_attribute.is_null()) {
        m_url = {};
        return;
    }

    // 2. Otherwise, parse this element's href content attribute value relative to this element's node document.
    //    If parsing is successful, set this element's url to the result; otherwise, set this element's url to null.
    m_url = hyperlink_element_utils_document().parse_url(href_content_attribute);
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-origin
String HTMLHyperlinkElementUtils::origin() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, return the empty string.
    if (!m_url.has_value())
        return String::empty();

    // 3. Return the serialization of this element's url's origin.
    return m_url->serialize_origin();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-protocol
String HTMLHyperlinkElementUtils::protocol() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, return ":".
    if (!m_url.has_value())
        return ":"sv;

    // 3. Return this element's url's scheme, followed by ":".
    return String::formatted("{}:", m_url->scheme());
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-protocol
void HTMLHyperlinkElementUtils::set_protocol(String protocol)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, terminate these steps.
    if (!m_url.has_value())
        return;

    // 3. Basic URL parse the given value, followed by ":", with this element's url as url and scheme start state as state override.
    auto result_url = URLParser::parse(String::formatted("{}:", protocol), nullptr, m_url, URLParser::State::SchemeStart);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 4. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-username
String HTMLHyperlinkElementUtils::username() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, return the empty string.
    if (!m_url.has_value())
        return String::empty();

    // 3. Return this element's url's username.
    return m_url->username();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-username
void HTMLHyperlinkElementUtils::set_username(String username)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url cannot have a username/password/port, then return.
    if (!url.has_value() || url->cannot_have_a_username_or_password_or_port())
        return;

    // 4. Set the username given this’s URL and the given value.
    url->set_username(AK::URL::percent_encode(username, AK::URL::PercentEncodeSet::Userinfo));

    // 5. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-password
String HTMLHyperlinkElementUtils::password() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null, then return the empty string.
    if (!url.has_value())
        return String::empty();

    // 4. Return url's password.
    return url->password();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-password
void HTMLHyperlinkElementUtils::set_password(String password)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url cannot have a username/password/port, then return.
    if (!url.has_value() || url->cannot_have_a_username_or_password_or_port())
        return;

    // 4. Set the password, given url and the given value.
    url->set_password(move(password));

    // 5. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-host
String HTMLHyperlinkElementUtils::host() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url or url's host is null, return the empty string.
    if (!url.has_value() || url->host().is_null())
        return String::empty();

    // 4. If url's port is null, return url's host, serialized.
    if (!url->port().has_value())
        return url->host();

    // 5. Return url's host, serialized, followed by ":" and url's port, serialized.
    return String::formatted("{}:{}", url->host(), url->port().value());
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-host
void HTMLHyperlinkElementUtils::set_host(String host)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url's cannot-be-a-base-URL is true, then return.
    if (!url.has_value() || url->cannot_be_a_base_url())
        return;

    // 4. Basic URL parse the given value, with url as url and host state as state override.
    auto result_url = URLParser::parse(host, nullptr, url, URLParser::State::Host);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 5. Update href.
    update_href();
}

String HTMLHyperlinkElementUtils::hostname() const
{
    // 1. Reinitialize url.
    //
    // 2. Let url be this element's url.
    //
    // 3. If url or url's host is null, return the empty string.
    //
    // 4. Return url's host, serialized.
    return AK::URL(href()).host();
}

void HTMLHyperlinkElementUtils::set_hostname(String hostname)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url's cannot-be-a-base-URL is true, then return.
    if (!url.has_value() || url->cannot_be_a_base_url())
        return;

    // 4. Basic URL parse the given value, with url as url and hostname state as state override.
    auto result_url = URLParser::parse(hostname, nullptr, m_url, URLParser::State::Hostname);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 5. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-port
String HTMLHyperlinkElementUtils::port() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url or url's port is null, return the empty string.
    if (!url.has_value() || !url->port().has_value())
        return String::empty();

    // 4. Return url's port, serialized.
    return String::number(url->port().value());
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-port
void HTMLHyperlinkElementUtils::set_port(String port)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null or url cannot have a username/password/port, then return.
    if (!m_url.has_value() || m_url->cannot_have_a_username_or_password_or_port())
        return;

    // 4. If the given value is the empty string, then set url's port to null.
    if (port.is_empty()) {
        m_url->set_port({});
    } else {
        // 5. Otherwise, basic URL parse the given value, with url as url and port state as state override.
        auto result_url = URLParser::parse(port, nullptr, m_url, URLParser::State::Port);
        if (result_url.is_valid())
            m_url = move(result_url);
    }

    // 6. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-pathname
String HTMLHyperlinkElementUtils::pathname() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, return the empty string.
    if (!m_url.has_value())
        return String::empty();

    // 4. If url's cannot-be-a-base-URL is true, then return url's path[0].
    // 5. If url's path is empty, then return the empty string.
    // 6. Return "/", followed by the strings in url's path (including empty strings), separated from each other by "/".
    return m_url->path();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-pathname
void HTMLHyperlinkElementUtils::set_pathname(String pathname)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null or url's cannot-be-a-base-URL is true, then return.
    if (!m_url.has_value() || m_url->cannot_be_a_base_url())
        return;

    // 4. Set url's path to the empty list.
    auto url = m_url; // We copy the URL here to follow other browser's behaviour of reverting the path change if the parse failed.
    url->set_paths({});

    // 5. Basic URL parse the given value, with url as url and path start state as state override.
    auto result_url = URLParser::parse(pathname, nullptr, move(url), URLParser::State::PathStart);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 6. Update href.
    update_href();
}

String HTMLHyperlinkElementUtils::search() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, or url's query is either null or the empty string, return the empty string.
    if (!m_url.has_value() || m_url->query().is_null() || m_url->query().is_empty())
        return String::empty();

    // 4. Return "?", followed by url's query.
    return String::formatted("?{}", m_url->query());
}

void HTMLHyperlinkElementUtils::set_search(String search)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, terminate these steps.
    if (!m_url.has_value())
        return;

    // 4. If the given value is the empty string, set url's query to null.
    if (search.is_empty()) {
        m_url->set_query({});
    } else {
        // 5. Otherwise:
        //    1. Let input be the given value with a single leading "?" removed, if any.
        auto input = search.substring_view(search.starts_with('?'));

        //    2. Set url's query to the empty string.
        auto url_copy = m_url; // We copy the URL here to follow other browser's behaviour of reverting the search change if the parse failed.
        url_copy->set_query(String::empty());

        //    3. Basic URL parse input, with null, this element's node document's document's character encoding, url as url, and query state as state override.
        auto result_url = URLParser::parse(input, nullptr, move(url_copy), URLParser::State::Query);
        if (result_url.is_valid())
            m_url = move(result_url);
    }

    // 6. Update href.
    update_href();
}

String HTMLHyperlinkElementUtils::hash() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, or url's fragment is either null or the empty string, return the empty string.
    if (!m_url.has_value() || m_url->fragment().is_null() || m_url->fragment().is_empty())
        return String::empty();

    // 4. Return "#", followed by url's fragment.
    return String::formatted("#{}", m_url->fragment());
}

void HTMLHyperlinkElementUtils::set_hash(String hash)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, then return.
    if (!m_url.has_value())
        return;

    // 4. If the given value is the empty string, set url's fragment to null.
    if (hash.is_empty()) {
        m_url->set_fragment({});
    } else {
        // 5. Otherwise:
        //    1. Let input be the given value with a single leading "#" removed, if any.
        auto input = hash.substring_view(hash.starts_with('#'));

        //    2. Set url's fragment to the empty string.
        auto url_copy = m_url; // We copy the URL here to follow other browser's behaviour of reverting the hash change if the parse failed.
        url_copy->set_fragment(String::empty());

        //    3. Basic URL parse input, with url as url and fragment state as state override.
        auto result_url = URLParser::parse(input, nullptr, move(url_copy), URLParser::State::Fragment);
        if (result_url.is_valid())
            m_url = move(result_url);
    }

    // 6. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-href
String HTMLHyperlinkElementUtils::href() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null and this element has no href content attribute, return the empty string.
    auto href_content_attribute = hyperlink_element_utils_href();
    if (!url.has_value() && href_content_attribute.is_null())
        return String::empty();

    // 4. Otherwise, if url is null, return this element's href content attribute's value.
    if (!url->is_valid())
        return href_content_attribute;

    // 5. Return url, serialized.
    return url->serialize();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-href
void HTMLHyperlinkElementUtils::set_href(String href)
{
    // The href attribute's setter must set this element's href content attribute's value to the given value.
    set_hyperlink_element_utils_href(move(href));
}

// https://html.spec.whatwg.org/multipage/links.html#update-href
void HTMLHyperlinkElementUtils::update_href()
{
    // To update href, set the element's href content attribute's value to the element's url, serialized.
}

}
