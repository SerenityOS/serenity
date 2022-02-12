/*
 * Copyright (c) 2022, DerpyCrabs <derpycrabs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Geometry/DOMRect.h>

namespace Web::Geometry {

// https://drafts.fxtf.org/geometry-1/#DOMRectList
class DOMRectList final
    : public RefCounted<DOMRectList>
    , public Bindings::Wrappable {
    AK_MAKE_NONCOPYABLE(DOMRectList);
    AK_MAKE_NONMOVABLE(DOMRectList);

public:
    using WrapperType = Bindings::DOMRectListWrapper;

    static NonnullRefPtr<DOMRectList> create(NonnullRefPtrVector<DOMRect>&& rects)
    {
        return adopt_ref(*new DOMRectList(move(rects)));
    }

    ~DOMRectList() = default;

    u32 length() const;
    DOMRect const* item(u32 index) const;

    bool is_supported_property_index(u32) const;

private:
    DOMRectList(NonnullRefPtrVector<DOMRect>&& rects);

    NonnullRefPtrVector<DOMRect> m_rects;
};

}
