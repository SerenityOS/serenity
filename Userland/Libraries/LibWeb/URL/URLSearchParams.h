/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::URL {

struct QueryParam {
    String name;
    String value;
};
String url_encode(const Vector<QueryParam>&, AK::URL::PercentEncodeSet);
Vector<QueryParam> url_decode(StringView);

class URLSearchParams : public Bindings::Wrappable
    , public RefCounted<URLSearchParams> {
public:
    using WrapperType = Bindings::URLSearchParamsWrapper;

    static NonnullRefPtr<URLSearchParams> create(Vector<QueryParam> list)
    {
        return adopt_ref(*new URLSearchParams(move(list)));
    }

    static DOM::ExceptionOr<NonnullRefPtr<URLSearchParams>> create_with_global_object(Bindings::WindowObject&, Variant<Vector<Vector<String>>, String> const& init);

    void append(String const& name, String const& value);
    void delete_(String const& name);
    String get(String const& name);
    Vector<String> get_all(String const& name);
    bool has(String const& name);
    void set(String const& name, String const& value);

    void sort();

    String to_string();

    using ForEachCallback = Function<JS::ThrowCompletionOr<void>(String const&, String const&)>;
    JS::ThrowCompletionOr<void> for_each(ForEachCallback);

private:
    friend class URL;
    friend class URLSearchParamsIterator;

    explicit URLSearchParams(Vector<QueryParam> list)
        : m_list(move(list)) {};

    void update();

    Vector<QueryParam> m_list;
    WeakPtr<URL> m_url;
};

}

namespace Web::Bindings {

URLSearchParamsWrapper* wrap(JS::GlobalObject&, URL::URLSearchParams&);

}
