/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/HTMLOptionsCollection.h>

namespace Web::HTML {

HTMLOptionsCollection::HTMLOptionsCollection(DOM::ParentNode& root, Function<bool(DOM::Element const&)> filter)
    : DOM::HTMLCollection(root, move(filter))
{
}

}
