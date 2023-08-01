/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2023, networkException <networkexception@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/IPv4Address.h>
#include <AK/IPv6Address.h>
#include <AK/URLParser.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/FileAPI/Blob.h>
#include <LibWeb/FileAPI/BlobURLStore.h>
#include <LibWeb/URL/URL.h>

namespace Web::URL {

WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> URL::create(JS::Realm& realm, AK::URL url, JS::NonnullGCPtr<URLSearchParams> query)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<URL>(realm, realm, move(url), move(query)));
}

// https://url.spec.whatwg.org/#api-url-parser
static Optional<AK::URL> parse_api_url(String const& url, Optional<String> const& base)
{
    // FIXME: We somewhat awkwardly have two failure states encapsulated in the return type (and convert between them in the steps),
    //        ideally we'd get rid of URL's valid flag

    // 1. Let parsedBase be null.
    Optional<AK::URL> parsed_base;

    // 2. If base is non-null:
    if (base.has_value()) {
        // 1. Set parsedBase to the result of running the basic URL parser on base.
        auto parsed_base_url = URLParser::basic_parse(*base);

        // 2. If parsedBase is failure, then return failure.
        if (!parsed_base_url.is_valid())
            return {};

        parsed_base = parsed_base_url;
    }

    // 3. Return the result of running the basic URL parser on url with parsedBase.
    auto parsed = URLParser::basic_parse(url, parsed_base);
    return parsed.is_valid() ? parsed : Optional<AK::URL> {};
}

// https://url.spec.whatwg.org/#dom-url-url
WebIDL::ExceptionOr<JS::NonnullGCPtr<URL>> URL::construct_impl(JS::Realm& realm, String const& url, Optional<String> const& base)
{
    auto& vm = realm.vm();

    // 1. Let parsedURL be the result of running the API URL parser on url with base, if given.
    auto parsed_url = parse_api_url(url, base);

    // 2. If parsedURL is failure, then throw a TypeError.
    if (!parsed_url.has_value())
        return WebIDL::SimpleException { WebIDL::SimpleExceptionType::TypeError, "Invalid URL"sv };

    // 3. Let query be parsedURL’s query, if that is non-null, and the empty string otherwise.
    auto query = parsed_url->query().is_null() ? String {} : TRY_OR_THROW_OOM(vm, String::from_deprecated_string(parsed_url->query()));

    // 4. Set this’s URL to parsedURL.
    // 5. Set this’s query object to a new URLSearchParams object.
    auto query_object = MUST(URLSearchParams::construct_impl(realm, query));

    // 6. Initialize this’s query object with query.
    auto result_url = TRY(URL::create(realm, parsed_url.release_value(), move(query_object)));

    // 7. Set this’s query object’s URL object to this.
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

// https://w3c.github.io/FileAPI/#dfn-createObjectURL
WebIDL::ExceptionOr<String> URL::create_object_url(JS::VM& vm, JS::NonnullGCPtr<FileAPI::Blob> object)
{
    // The createObjectURL(obj) static method must return the result of adding an entry to the blob URL store for obj.
    return TRY_OR_THROW_OOM(vm, FileAPI::add_entry_to_blob_url_store(object));
}

// https://w3c.github.io/FileAPI/#dfn-revokeObjectURL
WebIDL::ExceptionOr<void> URL::revoke_object_url(JS::VM& vm, StringView url)
{
    // 1. Let url record be the result of parsing url.
    auto url_record = parse(url);

    // 2. If url record’s scheme is not "blob", return.
    if (url_record.scheme() != "blob"sv)
        return {};

    // 3. Let origin be the origin of url record.
    auto origin = url_origin(url_record);

    // 4. Let settings be the current settings object.
    auto& settings = HTML::current_settings_object();

    // 5. If origin is not same origin with settings’s origin, return.
    if (!origin.is_same_origin(settings.origin()))
        return {};

    // 6. Remove an entry from the Blob URL Store for url.
    TRY_OR_THROW_OOM(vm, FileAPI::remove_entry_from_blob_url_store(url));
    return {};
}

// https://url.spec.whatwg.org/#dom-url-canparse
bool URL::can_parse(JS::VM&, String const& url, Optional<String> const& base)
{
    // 1. Let parsedURL be the result of running the API URL parser on url with base, if given.
    auto parsed_url = parse_api_url(url, base);

    // 2. If parsedURL is failure, then return false.
    if (!parsed_url.has_value())
        return false;

    // 3. Return true.
    return true;
}

// https://url.spec.whatwg.org/#dom-url-href
WebIDL::ExceptionOr<String> URL::href() const
{
    auto& vm = realm().vm();

    // The href getter steps and the toJSON() method steps are to return the serialization of this’s URL.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(m_url.serialize()));
}

// https://url.spec.whatwg.org/#dom-url-tojson
WebIDL::ExceptionOr<String> URL::to_json() const
{
    auto& vm = realm().vm();

    // The href getter steps and the toJSON() method steps are to return the serialization of this’s URL.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(m_url.serialize()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-href②
WebIDL::ExceptionOr<void> URL::set_href(String const& href)
{
    auto& vm = realm().vm();

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
    auto query = m_url.query();

    // 6. If query is non-null, then set this’s query object’s list to the result of parsing query.
    if (!query.is_null())
        m_query->m_list = TRY_OR_THROW_OOM(vm, url_decode(query));
    return {};
}

// https://url.spec.whatwg.org/#dom-url-origin
WebIDL::ExceptionOr<String> URL::origin() const
{
    auto& vm = realm().vm();

    // The origin getter steps are to return the serialization of this’s URL’s origin. [HTML]
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(m_url.serialize_origin()));
}

// https://url.spec.whatwg.org/#dom-url-protocol
WebIDL::ExceptionOr<String> URL::protocol() const
{
    auto& vm = realm().vm();

    // The protocol getter steps are to return this’s URL’s scheme, followed by U+003A (:).
    return TRY_OR_THROW_OOM(vm, String::formatted("{}:", m_url.scheme()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-protocol%E2%91%A0
WebIDL::ExceptionOr<void> URL::set_protocol(String const& protocol)
{
    auto& vm = realm().vm();

    // The protocol setter steps are to basic URL parse the given value, followed by U+003A (:), with this’s URL as
    // url and scheme start state as state override.
    auto result_url = URLParser::basic_parse(TRY_OR_THROW_OOM(vm, String::formatted("{}:", protocol)), {}, m_url, URLParser::State::SchemeStart);
    if (result_url.is_valid())
        m_url = move(result_url);
    return {};
}

// https://url.spec.whatwg.org/#dom-url-username
WebIDL::ExceptionOr<String> URL::username() const
{
    auto& vm = realm().vm();

    // The username getter steps are to return this’s URL’s username.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(m_url.username()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-username%E2%91%A0
void URL::set_username(String const& username)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;

    // 2. Set the username given this’s URL and the given value.
    m_url.set_username(username.to_deprecated_string(), AK::URL::ApplyPercentEncoding::Yes);
}

// https://url.spec.whatwg.org/#dom-url-password
WebIDL::ExceptionOr<String> URL::password() const
{
    auto& vm = realm().vm();

    // The password getter steps are to return this’s URL’s password.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(m_url.password()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-password%E2%91%A0
void URL::set_password(String const& password)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;

    // 2. Set the password given this’s URL and the given value.
    m_url.set_password(password.to_deprecated_string(), AK::URL::ApplyPercentEncoding::Yes);
}

// https://url.spec.whatwg.org/#dom-url-host
WebIDL::ExceptionOr<String> URL::host() const
{
    auto& vm = realm().vm();

    // 1. Let url be this’s URL.
    auto& url = m_url;

    // 2. If url’s host is null, then return the empty string.
    if (url.host().has<Empty>())
        return String {};

    // 3. If url’s port is null, return url’s host, serialized.
    if (!url.port().has_value())
        return TRY_OR_THROW_OOM(vm, url.serialized_host());

    // 4. Return url’s host, serialized, followed by U+003A (:) and url’s port, serialized.
    return TRY_OR_THROW_OOM(vm, String::formatted("{}:{}", TRY_OR_THROW_OOM(vm, url.serialized_host()), *url.port()));
}

// https://url.spec.whatwg.org/#dom-url-hostref-for-dom-url-host%E2%91%A0
void URL::set_host(String const& host)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;

    // 2. Basic URL parse the given value with this’s URL as url and host state as state override.
    auto result_url = URLParser::basic_parse(host, {}, m_url, URLParser::State::Host);
    if (result_url.is_valid())
        m_url = move(result_url);
}

// https://url.spec.whatwg.org/#dom-url-hostname
WebIDL::ExceptionOr<String> URL::hostname() const
{
    auto& vm = realm().vm();

    // 1. If this’s URL’s host is null, then return the empty string.
    if (m_url.host().has<Empty>())
        return String {};

    // 2. Return this’s URL’s host, serialized.
    return TRY_OR_THROW_OOM(vm, m_url.serialized_host());
}

// https://url.spec.whatwg.org/#ref-for-dom-url-hostname①
void URL::set_hostname(String const& hostname)
{
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;

    // 2. Basic URL parse the given value with this’s URL as url and hostname state as state override.
    auto result_url = URLParser::basic_parse(hostname, {}, m_url, URLParser::State::Hostname);
    if (result_url.is_valid())
        m_url = move(result_url);
}

// https://url.spec.whatwg.org/#ref-for-dom-url-hostname①
WebIDL::ExceptionOr<String> URL::port() const
{
    auto& vm = realm().vm();

    // 1. If this’s URL’s port is null, then return the empty string.
    if (!m_url.port().has_value())
        return String {};

    // 2. Return this’s URL’s port, serialized.
    return TRY_OR_THROW_OOM(vm, String::formatted("{}", *m_url.port()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-port%E2%91%A0
void URL::set_port(String const& port)
{
    // 1. If this’s URL cannot have a username/password/port, then return.
    if (m_url.cannot_have_a_username_or_password_or_port())
        return;

    // 2. If the given value is the empty string, then set this’s URL’s port to null.
    if (port.is_empty()) {
        m_url.set_port({});
    }
    // 3. Otherwise, basic URL parse the given value with this’s URL as url and port state as state override.
    else {
        auto result_url = URLParser::basic_parse(port, {}, m_url, URLParser::State::Port);
        if (result_url.is_valid())
            m_url = move(result_url);
    }
}

// https://url.spec.whatwg.org/#dom-url-pathname
WebIDL::ExceptionOr<String> URL::pathname() const
{
    auto& vm = realm().vm();

    // The pathname getter steps are to return the result of URL path serializing this’s URL.
    return TRY_OR_THROW_OOM(vm, String::from_deprecated_string(m_url.serialize_path()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-pathname%E2%91%A0
void URL::set_pathname(String const& pathname)
{
    // FIXME: These steps no longer match the speci.
    // 1. If this’s URL’s cannot-be-a-base-URL is true, then return.
    if (m_url.cannot_be_a_base_url())
        return;

    // 2. Empty this’s URL’s path.
    auto url = m_url; // We copy the URL here to follow other browser's behavior of reverting the path change if the parse failed.
    url.set_paths({});

    // 3. Basic URL parse the given value with this’s URL as url and path start state as state override.
    auto result_url = URLParser::basic_parse(pathname, {}, move(url), URLParser::State::PathStart);
    if (result_url.is_valid())
        m_url = move(result_url);
}

// https://url.spec.whatwg.org/#dom-url-search
WebIDL::ExceptionOr<String> URL::search() const
{
    auto& vm = realm().vm();

    // 1. If this’s URL’s query is either null or the empty string, then return the empty string.
    if (m_url.query().is_null() || m_url.query().is_empty())
        return String {};

    // 2. Return U+003F (?), followed by this’s URL’s query.
    return TRY_OR_THROW_OOM(vm, String::formatted("?{}", m_url.query()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-search%E2%91%A0
WebIDL::ExceptionOr<void> URL::set_search(String const& search)
{
    auto& vm = realm().vm();

    // 1. Let url be this’s URL.
    auto& url = m_url;

    // 2. If the given value is the empty string:
    if (search.is_empty()) {
        // 1. Set url’s query to null.
        url.set_query({});

        // 2. Empty this’s query object’s list.
        m_query->m_list.clear();

        // FIXME: 3. Potentially strip trailing spaces from an opaque path with this.

        // 4. Return.
        return {};
    }

    // 3. Let input be the given value with a single leading U+003F (?) removed, if any.
    auto search_as_string_view = search.bytes_as_string_view();
    auto input = search_as_string_view.substring_view(search_as_string_view.starts_with('?'));

    // 4. Set url’s query to the empty string.
    auto url_copy = url; // We copy the URL here to follow other browser's behavior of reverting the search change if the parse failed.
    url_copy.set_query(DeprecatedString::empty());

    // 5. Basic URL parse input with url as url and query state as state override.
    auto result_url = URLParser::basic_parse(input, {}, move(url_copy), URLParser::State::Query);
    if (result_url.is_valid()) {
        m_url = move(result_url);

        // 6. Set this’s query object’s list to the result of parsing input.
        m_query->m_list = TRY_OR_THROW_OOM(vm, url_decode(input));
    }

    return {};
}

// https://url.spec.whatwg.org/#dom-url-searchparams
JS::NonnullGCPtr<URLSearchParams const> URL::search_params() const
{
    // The searchParams getter steps are to return this’s query object.
    return m_query;
}

// https://url.spec.whatwg.org/#dom-url-hash
WebIDL::ExceptionOr<String> URL::hash() const
{
    auto& vm = realm().vm();

    // 1. If this’s URL’s fragment is either null or the empty string, then return the empty string.
    if (m_url.fragment().is_null() || m_url.fragment().is_empty())
        return String {};

    // 2. Return U+0023 (#), followed by this’s URL’s fragment.
    return TRY_OR_THROW_OOM(vm, String::formatted("#{}", m_url.fragment()));
}

// https://url.spec.whatwg.org/#ref-for-dom-url-hash%E2%91%A0
void URL::set_hash(String const& hash)
{
    // 1. If the given value is the empty string:
    if (hash.is_empty()) {
        // 1. Set this’s URL’s fragment to null.
        m_url.set_fragment({});

        // FIXME: 2. Potentially strip trailing spaces from an opaque path with this.

        // 3. Return.
        return;
    }

    // 2. Let input be the given value with a single leading U+0023 (#) removed, if any.
    auto hash_as_string_view = hash.bytes_as_string_view();
    auto input = hash_as_string_view.substring_view(hash_as_string_view.starts_with('#'));

    // 3. Set this’s URL’s fragment to the empty string.
    auto url = m_url; // We copy the URL here to follow other browser's behavior of reverting the hash change if the parse failed.
    url.set_fragment(DeprecatedString::empty());

    // 4. Basic URL parse input with this’s URL as url and fragment state as state override.
    auto result_url = URLParser::basic_parse(input, {}, move(url), URLParser::State::Fragment);
    if (result_url.is_valid())
        m_url = move(result_url);
}

// https://url.spec.whatwg.org/#concept-url-origin
HTML::Origin url_origin(AK::URL const& url)
{
    // FIXME: We should probably have an extended version of AK::URL for LibWeb instead of standalone functions like this.

    // The origin of a URL url is the origin returned by running these steps, switching on url’s scheme:
    // -> "blob"
    if (url.scheme() == "blob"sv) {
        auto url_string = url.to_string().release_value_but_fixme_should_propagate_errors();

        // 1. If url’s blob URL entry is non-null, then return url’s blob URL entry’s environment’s origin.
        if (auto blob_url_entry = FileAPI::blob_url_store().get(url_string); blob_url_entry.has_value())
            return blob_url_entry->environment->origin();

        // 2. Let pathURL be the result of parsing the result of URL path serializing url.
        auto path_url = parse(url.serialize_path());

        // 3. If pathURL is failure, then return a new opaque origin.
        if (!path_url.is_valid())
            return HTML::Origin {};

        // 4. If pathURL’s scheme is "http", "https", or "file", then return pathURL’s origin.
        if (path_url.scheme().is_one_of("http"sv, "https"sv, "file"sv))
            return url_origin(path_url);

        // 5. Return a new opaque origin.
        return HTML::Origin {};
    }

    // -> "ftp"
    // -> "http"
    // -> "https"
    // -> "ws"
    // -> "wss"
    if (url.scheme().is_one_of("ftp"sv, "http"sv, "https"sv, "ws"sv, "wss"sv)) {
        // Return the tuple origin (url’s scheme, url’s host, url’s port, null).
        return HTML::Origin(url.scheme(), url.host(), url.port().value_or(0));
    }

    // -> "file"
    if (url.scheme() == "file"sv) {
        // Unfortunate as it is, this is left as an exercise to the reader. When in doubt, return a new opaque origin.
        // Note: We must return an origin with the `file://' protocol for `file://' iframes to work from `file://' pages.
        return HTML::Origin(url.scheme(), String {}, 0);
    }

    // -> Otherwise
    // Return a new opaque origin.
    return HTML::Origin {};
}

// https://url.spec.whatwg.org/#concept-domain
bool host_is_domain(AK::URL::Host const& host)
{
    // A domain is a non-empty ASCII string that identifies a realm within a network.
    return host.has<String>() && host.get<String>() != String {};
}

// https://url.spec.whatwg.org/#concept-url-parser
AK::URL parse(StringView input, Optional<AK::URL> const& base_url)
{
    // FIXME: We should probably have an extended version of AK::URL for LibWeb instead of standalone functions like this.

    // 1. Let url be the result of running the basic URL parser on input with base and encoding.
    auto url = URLParser::basic_parse(input, base_url);

    // 2. If url is failure, return failure.
    if (!url.is_valid())
        return {};

    // 3. If url’s scheme is not "blob",
    if (url.scheme() != "blob")
        return url;

    // FIXME: 4. Set url’s blob URL entry to the result of resolving the blob URL url,
    // FIXME: 5. if that did not return failure, and null otherwise.

    // 6. Return url
    return url;
}

}
