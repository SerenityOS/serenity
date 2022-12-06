/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::URL {

struct QueryParam {
    DeprecatedString name;
    DeprecatedString value;
};
DeprecatedString url_encode(Vector<QueryParam> const&, AK::URL::PercentEncodeSet);
Vector<QueryParam> url_decode(StringView);

class URLSearchParams : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(URLSearchParams, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<URLSearchParams> create(JS::Realm&, Vector<QueryParam> list);
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<URLSearchParams>> construct_impl(JS::Realm&, Variant<Vector<Vector<DeprecatedString>>, OrderedHashMap<DeprecatedString, DeprecatedString>, DeprecatedString> const& init);

    virtual ~URLSearchParams() override;

    void append(DeprecatedString const& name, DeprecatedString const& value);
    void delete_(DeprecatedString const& name);
    DeprecatedString get(DeprecatedString const& name);
    Vector<DeprecatedString> get_all(DeprecatedString const& name);
    bool has(DeprecatedString const& name);
    void set(DeprecatedString const& name, DeprecatedString const& value);

    void sort();

    DeprecatedString to_deprecated_string() const;

    using ForEachCallback = Function<JS::ThrowCompletionOr<void>(DeprecatedString const&, DeprecatedString const&)>;
    JS::ThrowCompletionOr<void> for_each(ForEachCallback);

private:
    friend class URL;
    friend class URLSearchParamsIterator;

    URLSearchParams(JS::Realm&, Vector<QueryParam> list);

    virtual void visit_edges(Cell::Visitor&) override;

    void update();

    Vector<QueryParam> m_list;
    JS::GCPtr<URL> m_url;
};

}
