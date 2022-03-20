/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/HTMLCollection.h>

namespace Web::HTML {

class HTMLOptionsCollection final : public DOM::HTMLCollection {
public:
    using WrapperType = Bindings::HTMLOptionsCollectionWrapper;

    static NonnullRefPtr<HTMLOptionsCollection> create(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
    {
        return adopt_ref(*new HTMLOptionsCollection(root, move(filter)));
    }

protected:
    HTMLOptionsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter);
};

}

namespace Web::Bindings {

HTMLOptionsCollectionWrapper* wrap(JS::GlobalObject&, HTML::HTMLOptionsCollection&);

}
