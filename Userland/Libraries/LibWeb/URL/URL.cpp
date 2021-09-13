/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

}
