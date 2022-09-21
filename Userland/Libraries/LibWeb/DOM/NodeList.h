/*
 * Copyright (c) 2021, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/LegacyPlatformObject.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#nodelist
class NodeList : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(NodeList, Bindings::LegacyPlatformObject);

public:
    virtual ~NodeList() override;

    virtual u32 length() const = 0;
    virtual Node const* item(u32 index) const = 0;

    virtual JS::Value item_value(size_t index) const override;
    virtual bool is_supported_property_index(u32) const override;

protected:
    explicit NodeList(HTML::Window&);
};

}
