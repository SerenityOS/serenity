/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/Layout/BlockBox.h>

namespace Web::DOM {

ShadowRoot::ShadowRoot(Document& document, Element& host)
    : DocumentFragment(document)
{
    set_host(host);
}

// https://dom.spec.whatwg.org/#ref-for-get-the-parent%E2%91%A6
EventTarget* ShadowRoot::get_parent(const Event& event)
{
    if (!event.composed()) {
        auto& events_first_invocation_target = verify_cast<Node>(*event.path().first().invocation_target);
        if (&events_first_invocation_target.root() == this)
            return nullptr;
    }

    return host();
}

RefPtr<Layout::Node> ShadowRoot::create_layout_node()
{
    return adopt_ref(*new Layout::BlockBox(document(), this, CSS::ComputedValues {}));
}

}
