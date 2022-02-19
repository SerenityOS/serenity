/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/dynamic-markup-insertion.html#domparser
class DOMParser final
    : public RefCounted<DOMParser>
    , public Weakable<DOMParser>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::DOMParserWrapper;

    static DOM::ExceptionOr<NonnullRefPtr<DOMParser>> create_with_global_object(Bindings::WindowObject&)
    {
        return adopt_ref(*new DOMParser());
    }

    virtual ~DOMParser() override;

    NonnullRefPtr<DOM::Document> parse_from_string(String const&, Bindings::DOMParserSupportedType type);

private:
    DOMParser();
};

}
