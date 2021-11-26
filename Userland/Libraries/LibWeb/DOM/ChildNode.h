/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Web::DOM {

// https://dom.spec.whatwg.org/#childnode
template<typename NodeType>
class ChildNode {
public:
    // https://dom.spec.whatwg.org/#dom-childnode-remove
    void remove_binding()
    {
        auto* node = static_cast<NodeType*>(this);

        // 1. If this’s parent is null, then return.
        if (!node->parent())
            return;

        // 2. Remove this.
        node->remove();
    }

protected:
    ChildNode() = default;
};

}
