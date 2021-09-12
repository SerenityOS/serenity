/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/URL/URL.h>

namespace Web::URL {

class URLSearchParams : public Bindings::Wrappable
    , public RefCounted<URLSearchParams> {
public:
    using WrapperType = Bindings::URLSearchParamsWrapper;

    static NonnullRefPtr<URLSearchParams> create(Vector<QueryParam> list)
    {
        return adopt_ref(*new URLSearchParams(move(list)));
    }

    static DOM::ExceptionOr<NonnullRefPtr<URLSearchParams>> create_with_global_object(Bindings::WindowObject&, const String& init);

    void append(String const& name, String const& value);
    void delete_(String const& name);
    String get(String const& name);
    bool has(String const& name);
    void set(String const& name, String const& value);

    void sort();

    String to_string();

private:
    explicit URLSearchParams(Vector<QueryParam> list)
        : m_list(move(list)) {};

    void update();

    Vector<QueryParam> m_list;
};

}
