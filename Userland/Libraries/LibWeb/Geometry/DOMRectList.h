/*
 * Copyright (c) 2022, DerpyCrabs <derpycrabs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibWeb/Bindings/LegacyPlatformObject.h>
#include <LibWeb/Geometry/DOMRect.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry-1/#DOMRectList
class DOMRectList final : public Bindings::LegacyPlatformObject {
    WEB_PLATFORM_OBJECT(DOMRectList, Bindings::LegacyPlatformObject);

public:
    static JS::NonnullGCPtr<DOMRectList> create(HTML::Window&, Vector<JS::Handle<DOMRect>>);

    virtual ~DOMRectList() override;

    u32 length() const;
    DOMRect const* item(u32 index) const;

    virtual bool is_supported_property_index(u32) const override;
    virtual JS::Value item_value(size_t index) const override;

private:
    DOMRectList(HTML::Window&, Vector<JS::NonnullGCPtr<DOMRect>>);

    Vector<JS::NonnullGCPtr<DOMRect>> m_rects;
};

}

WRAPPER_HACK(DOMRectList, Web::Geometry)
