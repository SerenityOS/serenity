/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/HTMLCollection.h>

namespace Web::HTML {

using HTMLOptionOrOptGroupElement = Variant<JS::Handle<HTMLOptionElement>, JS::Handle<HTMLOptGroupElement>>;
using HTMLElementOrElementIndex = Variant<JS::Handle<HTMLElement>, i32>;

class HTMLOptionsCollection final : public DOM::HTMLCollection {
public:
    using WrapperType = Bindings::HTMLOptionsCollectionWrapper;

    static NonnullRefPtr<HTMLOptionsCollection> create(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
    {
        return adopt_ref(*new HTMLOptionsCollection(root, move(filter)));
    }

    DOM::ExceptionOr<void> add(HTMLOptionOrOptGroupElement element, Optional<HTMLElementOrElementIndex> before = {});

protected:
    HTMLOptionsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter);
};

}

namespace Web::Bindings {

HTMLOptionsCollectionWrapper* wrap(JS::Realm&, HTML::HTMLOptionsCollection&);

}
