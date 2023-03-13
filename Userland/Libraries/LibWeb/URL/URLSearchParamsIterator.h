/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/URL/URLSearchParams.h>

namespace Web::URL {

class URLSearchParamsIterator : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(URLSearchParamsIterator, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<URLSearchParamsIterator>> create(URLSearchParams const&, JS::Object::PropertyKind iteration_kind);

    virtual ~URLSearchParamsIterator() override;

    JS::Object* next();

private:
    URLSearchParamsIterator(URLSearchParams const&, JS::Object::PropertyKind iteration_kind);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    JS::NonnullGCPtr<URLSearchParams const> m_url_search_params;
    JS::Object::PropertyKind m_iteration_kind;
    size_t m_index { 0 };
};

}
