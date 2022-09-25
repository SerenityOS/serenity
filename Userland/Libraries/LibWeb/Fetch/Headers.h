/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/Fetch/Infrastructure/HTTP/Headers.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::Fetch {

using HeadersInit = Variant<Vector<Vector<String>>, OrderedHashMap<String, String>>;

// https://fetch.spec.whatwg.org/#headers-class
class Headers final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Headers, Bindings::PlatformObject);

public:
    enum class Guard {
        Immutable,
        Request,
        RequestNoCORS,
        Response,
        None,
    };

    static WebIDL::ExceptionOr<JS::NonnullGCPtr<Headers>> create_with_global_object(HTML::Window& window, Optional<HeadersInit> const& init);

    virtual ~Headers() override;

    [[nodiscard]] Infrastructure::HeaderList& header_list() { return m_header_list; }
    [[nodiscard]] Infrastructure::HeaderList const& header_list() const { return m_header_list; }
    void set_header_list(Infrastructure::HeaderList header_list) { m_header_list = move(header_list); }

    [[nodiscard]] Guard guard() const { return m_guard; }
    void set_guard(Guard guard) { m_guard = guard; }

    // JS API functions
    WebIDL::ExceptionOr<void> append(Infrastructure::Header);
    WebIDL::ExceptionOr<void> append(String const& name, String const& value);
    WebIDL::ExceptionOr<void> delete_(String const& name);
    WebIDL::ExceptionOr<String> get(String const& name);
    WebIDL::ExceptionOr<bool> has(String const& name);
    WebIDL::ExceptionOr<void> set(String const& name, String const& value);

    using ForEachCallback = Function<JS::ThrowCompletionOr<void>(String const&, String const&)>;
    JS::ThrowCompletionOr<void> for_each(ForEachCallback);

private:
    friend class HeadersIterator;

    explicit Headers(HTML::Window&);

    WebIDL::ExceptionOr<void> fill(HeadersInit const&);
    void remove_privileged_no_cors_headers();

    // https://fetch.spec.whatwg.org/#concept-headers-header-list
    // A Headers object has an associated header list (a header list), which is initially empty.
    Infrastructure::HeaderList m_header_list;

    // https://fetch.spec.whatwg.org/#concept-headers-guard
    // A Headers object also has an associated guard, which is a headers guard. A headers guard is "immutable", "request", "request-no-cors", "response" or "none".
    Guard m_guard { Guard::None };
};

}
