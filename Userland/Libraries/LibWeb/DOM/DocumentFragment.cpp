/*
 * Copyright (c) 2020, Luke Wilde <luke.wilde@live.co.uk>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/DocumentFragment.h>

namespace Web::DOM {

DocumentFragment::DocumentFragment(Document& document)
    : ParentNode(document, NodeType::DOCUMENT_FRAGMENT_NODE)
{
}

DocumentFragment::~DocumentFragment()
{
}

}
