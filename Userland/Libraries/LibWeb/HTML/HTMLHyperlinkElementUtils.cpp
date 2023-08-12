/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/URLParser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLHyperlinkElementUtils.h>
#include <LibWeb/Loader/FrameLoader.h>

namespace Web::HTML {

HTMLHyperlinkElementUtils::~HTMLHyperlinkElementUtils() = default;

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
DeprecatedString HTMLHyperlinkElementUtils::origin() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, return the empty string.
    if (!m_url.has_value())
        return DeprecatedString::empty();

    // 3. Return the serialization of this element's url's origin.
    return m_url->serialize_origin();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-protocol
DeprecatedString HTMLHyperlinkElementUtils::protocol() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, return ":".
    if (!m_url.has_value())
        return ":"sv;

    // 3. Return this element's url's scheme, followed by ":".
    return DeprecatedString::formatted("{}:", m_url->scheme());
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-protocol
void HTMLHyperlinkElementUtils::set_protocol(DeprecatedString protocol)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, terminate these steps.
    if (!m_url.has_value())
        return;

    // 3. Basic URL parse the given value, followed by ":", with this element's url as url and scheme start state as state override.
    auto result_url = URLParser::basic_parse(DeprecatedString::formatted("{}:", protocol), {}, m_url, URLParser::State::SchemeStart);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 4. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-username
DeprecatedString HTMLHyperlinkElementUtils::username() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. If this element's url is null, return the empty string.
    if (!m_url.has_value())
        return DeprecatedString::empty();

    // 3. Return this element's url's username.
    return m_url->username().release_value().to_deprecated_string();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-username
void HTMLHyperlinkElementUtils::set_username(DeprecatedString username)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url cannot have a username/password/port, then return.
    if (!url.has_value() || url->cannot_have_a_username_or_password_or_port())
        return;

    // 4. Set the username given this’s URL and the given value.
    MUST(url->set_username(username));

    // 5. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-password
DeprecatedString HTMLHyperlinkElementUtils::password() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null, then return the empty string.
    if (!url.has_value())
        return DeprecatedString::empty();

    // 4. Return url's password.
    return url->password().release_value().to_deprecated_string();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-password
void HTMLHyperlinkElementUtils::set_password(DeprecatedString password)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url cannot have a username/password/port, then return.
    if (!url.has_value() || url->cannot_have_a_username_or_password_or_port())
        return;

    // 4. Set the password, given url and the given value.
    MUST(url->set_password(password));

    // 5. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-host
DeprecatedString HTMLHyperlinkElementUtils::host() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url or url's host is null, return the empty string.
    if (!url.has_value() || url->host().has<Empty>())
        return DeprecatedString::empty();

    // 4. If url's port is null, return url's host, serialized.
    if (!url->port().has_value())
        return url->serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string();

    // 5. Return url's host, serialized, followed by ":" and url's port, serialized.
    return DeprecatedString::formatted("{}:{}", url->serialized_host().release_value_but_fixme_should_propagate_errors(), url->port().value());
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-host
void HTMLHyperlinkElementUtils::set_host(DeprecatedString host)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url's cannot-be-a-base-URL is true, then return.
    if (!url.has_value() || url->cannot_be_a_base_url())
        return;

    // 4. Basic URL parse the given value, with url as url and host state as state override.
    auto result_url = URLParser::basic_parse(host, {}, url, URLParser::State::Host);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 5. Update href.
    update_href();
}

DeprecatedString HTMLHyperlinkElementUtils::hostname() const
{
    // 1. Reinitialize url.
    //
    // 2. Let url be this element's url.
    AK::URL url(href());

    // 3. If url or url's host is null, return the empty string.
    if (url.host().has<Empty>())
        return DeprecatedString::empty();

    // 4. Return url's host, serialized.
    return url.serialized_host().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

void HTMLHyperlinkElementUtils::set_hostname(DeprecatedString hostname)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null or url's cannot-be-a-base-URL is true, then return.
    if (!url.has_value() || url->cannot_be_a_base_url())
        return;

    // 4. Basic URL parse the given value, with url as url and hostname state as state override.
    auto result_url = URLParser::basic_parse(hostname, {}, m_url, URLParser::State::Hostname);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 5. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-port
DeprecatedString HTMLHyperlinkElementUtils::port() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url or url's port is null, return the empty string.
    if (!url.has_value() || !url->port().has_value())
        return DeprecatedString::empty();

    // 4. Return url's port, serialized.
    return DeprecatedString::number(url->port().value());
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-port
void HTMLHyperlinkElementUtils::set_port(DeprecatedString port)
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
        auto result_url = URLParser::basic_parse(port, {}, m_url, URLParser::State::Port);
        if (result_url.is_valid())
            m_url = move(result_url);
    }

    // 6. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-pathname
DeprecatedString HTMLHyperlinkElementUtils::pathname() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, return the empty string.
    if (!m_url.has_value())
        return DeprecatedString::empty();

    // 4. If url's cannot-be-a-base-URL is true, then return url's path[0].
    // 5. If url's path is empty, then return the empty string.
    // 6. Return "/", followed by the strings in url's path (including empty strings), separated from each other by "/".
    return m_url->serialize_path();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-pathname
void HTMLHyperlinkElementUtils::set_pathname(DeprecatedString pathname)
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null or url's cannot-be-a-base-URL is true, then return.
    if (!m_url.has_value() || m_url->cannot_be_a_base_url())
        return;

    // 4. Set url's path to the empty list.
    auto url = m_url; // We copy the URL here to follow other browser's behavior of reverting the path change if the parse failed.
    url->set_paths({});

    // 5. Basic URL parse the given value, with url as url and path start state as state override.
    auto result_url = URLParser::basic_parse(pathname, {}, move(url), URLParser::State::PathStart);
    if (result_url.is_valid())
        m_url = move(result_url);

    // 6. Update href.
    update_href();
}

DeprecatedString HTMLHyperlinkElementUtils::search() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, or url's query is either null or the empty string, return the empty string.
    if (!m_url.has_value() || m_url->query().has_value() || m_url->query()->is_empty())
        return DeprecatedString::empty();

    // 4. Return "?", followed by url's query.
    return DeprecatedString::formatted("?{}", m_url->query());
}

void HTMLHyperlinkElementUtils::set_search(DeprecatedString search)
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
        auto url_copy = m_url; // We copy the URL here to follow other browser's behavior of reverting the search change if the parse failed.
        url_copy->set_query(String {});

        //    3. Basic URL parse input, with null, this element's node document's document's character encoding, url as url, and query state as state override.
        auto result_url = URLParser::basic_parse(input, {}, move(url_copy), URLParser::State::Query);
        if (result_url.is_valid())
            m_url = move(result_url);
    }

    // 6. Update href.
    update_href();
}

DeprecatedString HTMLHyperlinkElementUtils::hash() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.

    // 3. If url is null, or url's fragment is either null or the empty string, return the empty string.
    if (!m_url.has_value() || !m_url->fragment().has_value() || m_url->fragment()->is_empty())
        return DeprecatedString::empty();

    // 4. Return "#", followed by url's fragment.
    return DeprecatedString::formatted("#{}", *m_url->fragment());
}

void HTMLHyperlinkElementUtils::set_hash(DeprecatedString hash)
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
        auto url_copy = m_url; // We copy the URL here to follow other browser's behavior of reverting the hash change if the parse failed.
        url_copy->set_fragment(String {});

        //    3. Basic URL parse input, with url as url and fragment state as state override.
        auto result_url = URLParser::basic_parse(input, {}, move(url_copy), URLParser::State::Fragment);
        if (result_url.is_valid())
            m_url = move(result_url);
    }

    // 6. Update href.
    update_href();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-href
DeprecatedString HTMLHyperlinkElementUtils::href() const
{
    // 1. Reinitialize url.
    reinitialize_url();

    // 2. Let url be this element's url.
    auto& url = m_url;

    // 3. If url is null and this element has no href content attribute, return the empty string.
    auto href_content_attribute = hyperlink_element_utils_href();
    if (!url.has_value() && href_content_attribute.is_null())
        return DeprecatedString::empty();

    // 4. Otherwise, if url is null, return this element's href content attribute's value.
    if (!url->is_valid())
        return href_content_attribute;

    // 5. Return url, serialized.
    return url->serialize();
}

// https://html.spec.whatwg.org/multipage/links.html#dom-hyperlink-href
WebIDL::ExceptionOr<void> HTMLHyperlinkElementUtils::set_href(DeprecatedString href)
{
    // The href attribute's setter must set this element's href content attribute's value to the given value.
    return set_hyperlink_element_utils_href(move(href));
}

// https://html.spec.whatwg.org/multipage/links.html#update-href
void HTMLHyperlinkElementUtils::update_href()
{
    // To update href, set the element's href content attribute's value to the element's url, serialized.
}

bool HTMLHyperlinkElementUtils::cannot_navigate() const
{
    // An element element cannot navigate if one of the following is true:

    // 1. element's node document is not fully active
    auto const& document = const_cast<HTMLHyperlinkElementUtils*>(this)->hyperlink_element_utils_document();
    if (!document.is_fully_active())
        return true;

    // 2. element is not an a element and is not connected.
    if (!hyperlink_element_utils_is_html_anchor_element() && !hyperlink_element_utils_is_connected())
        return true;

    return false;
}

// https://html.spec.whatwg.org/multipage/links.html#following-hyperlinks-2
void HTMLHyperlinkElementUtils::follow_the_hyperlink(Optional<DeprecatedString> hyperlink_suffix)
{
    // To follow the hyperlink created by an element subject, given an optional hyperlinkSuffix (default null):

    // 1. If subject cannot navigate, then return.
    if (cannot_navigate())
        return;

    // FIXME: 2. Let replace be false.

    // 3. Let source be subject's node document's browsing context.
    auto* source = hyperlink_element_utils_document().browsing_context();
    if (!source)
        return;

    // 4. Let targetAttributeValue be the empty string.
    // 5. If subject is an a or area element, then set targetAttributeValue to
    // the result of getting an element's target given subject.
    DeprecatedString target_attribute_value = hyperlink_element_utils_get_an_elements_target();

    // 6. Let noopener be the result of getting an element's noopener with subject and targetAttributeValue.
    auto noopener = hyperlink_element_utils_get_an_elements_noopener(target_attribute_value);

    // 7. Let target be the first return value of applying the rules for
    // choosing a browsing context given targetAttributeValue, source, and
    // noopener.
    auto target = source->choose_a_browsing_context(target_attribute_value, noopener).browsing_context;

    // 8. If target is null, then return.
    if (!target)
        return;

    // 9. Parse a URL given subject's href attribute, relative to subject's node
    // document.
    auto url = source->active_document()->parse_url(href());

    // 10. If that is successful, let URL be the resulting URL string.
    auto url_string = url.to_deprecated_string();

    // 11. Otherwise, if parsing the URL failed, the user agent may report the
    // error to the user in a user-agent-specific manner, may queue an element
    // task on the DOM manipulation task source given subject to navigate the
    // target browsing context to an error page to report the error, or may
    // ignore the error and do nothing. In any case, the user agent must then
    // return.

    // 12. If hyperlinkSuffix is non-null, then append it to URL.
    if (hyperlink_suffix.has_value()) {
        StringBuilder url_builder;
        url_builder.append(url_string);
        url_builder.append(*hyperlink_suffix);

        url_string = url_builder.to_deprecated_string();
    }

    // FIXME: 13. Let request be a new request whose URL is URL and whose
    // referrer policy is the current state of subject's referrerpolicy content
    // attribute.

    // FIXME: 14. If subject's link types includes the noreferrer keyword, then
    // set request's referrer to "no-referrer".

    // 15. Queue an element task on the DOM manipulation task source given
    // subject to navigate target to request with the source browsing context
    // set to source.
    // FIXME: "navigate" means implementing the navigation algorithm here:
    //        https://html.spec.whatwg.org/multipage/browsing-the-web.html#navigate
    hyperlink_element_utils_queue_an_element_task(Task::Source::DOMManipulation, [url_string, target] {
        verify_cast<BrowsingContext>(*target).loader().load(url_string, FrameLoader::Type::Navigation);
    });
}

}
