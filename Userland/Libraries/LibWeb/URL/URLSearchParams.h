/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::URL {

struct QueryParam {
    String name;
    String value;
};
String url_encode(Vector<QueryParam> const&, AK::URL::PercentEncodeSet);
Vector<QueryParam> url_decode(StringView);

class URLSearchParams : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(URLSearchParams, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<URLSearchParams> create(HTML::Window&, Vector<QueryParam> list);
    static DOM::ExceptionOr<JS::NonnullGCPtr<URLSearchParams>> create_with_global_object(HTML::Window&, Variant<Vector<Vector<String>>, OrderedHashMap<String, String>, String> const& init);

    virtual ~URLSearchParams() override;

    void append(String const& name, String const& value);
    void delete_(String const& name);
    String get(String const& name);
    Vector<String> get_all(String const& name);
    bool has(String const& name);
    void set(String const& name, String const& value);

    void sort();

    String to_string() const;

    using ForEachCallback = Function<JS::ThrowCompletionOr<void>(String const&, String const&)>;
    JS::ThrowCompletionOr<void> for_each(ForEachCallback);

private:
    friend class URL;
    friend class URLSearchParamsIterator;

    URLSearchParams(HTML::Window&, Vector<QueryParam> list);

    virtual void visit_edges(Cell::Visitor&) override;

    void update();

    Vector<QueryParam> m_list;
    JS::GCPtr<URL> m_url;
};

}

WRAPPER_HACK(URLSearchParams, Web::URL)
