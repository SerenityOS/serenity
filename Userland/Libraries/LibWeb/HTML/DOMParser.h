/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Forward.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#domparser
class DOMParser final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(DOMParser, Bindings::PlatformObject);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMParser>> construct_impl(JS::Realm&);

    virtual ~DOMParser() override;

    JS::NonnullGCPtr<DOM::Document> parse_from_string(DeprecatedString const&, Bindings::DOMParserSupportedType type);

private:
    explicit DOMParser(JS::Realm&);
};

}
