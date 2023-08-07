/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

using HTMLOptionOrOptGroupElement = Variant<JS::Handle<HTMLOptionElement>, JS::Handle<HTMLOptGroupElement>>;
using HTMLElementOrElementIndex = Variant<JS::Handle<HTMLElement>, i32>;

class HTMLOptionsCollection final : public DOM::HTMLCollection {
    WEB_PLATFORM_OBJECT(HTMLOptionsCollection, DOM::HTMLCollection);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<HTMLOptionsCollection>> create(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter);
    virtual ~HTMLOptionsCollection() override;

    WebIDL::ExceptionOr<void> add(HTMLOptionOrOptGroupElement element, Optional<HTMLElementOrElementIndex> before = {});

private:
    HTMLOptionsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter);

    virtual void initialize(JS::Realm&) override;
};

}
