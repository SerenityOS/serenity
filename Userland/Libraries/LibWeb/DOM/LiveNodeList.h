/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

// FIXME: Just like HTMLCollection, LiveNodeList currently does no caching.

class LiveNodeList : public NodeList {
public:
    static NonnullRefPtr<NodeList> create(Node& root, Function<bool(Node const&)> filter)
    {
        return adopt_ref(*new LiveNodeList(root, move(filter)));
    }

    virtual u32 length() const override;
    virtual Node const* item(u32 index) const override;

    virtual bool is_supported_property_index(u32) const override;

private:
    LiveNodeList(Node& root, Function<bool(Node const&)> filter);

    NonnullRefPtrVector<Node> collection() const;

    NonnullRefPtr<Node> m_root;
    Function<bool(Node const&)> m_filter;
};

}
