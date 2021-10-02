/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#nodelist
class NodeList
    : public RefCounted<NodeList>
    , public Bindings::Wrappable {
    AK_MAKE_NONCOPYABLE(NodeList);
    AK_MAKE_NONMOVABLE(NodeList);

public:
    using WrapperType = Bindings::NodeListWrapper;

    virtual ~NodeList() override = default;

    virtual u32 length() const = 0;
    virtual Node const* item(u32 index) const = 0;

    virtual bool is_supported_property_index(u32) const = 0;

protected:
    NodeList() = default;
};

}
