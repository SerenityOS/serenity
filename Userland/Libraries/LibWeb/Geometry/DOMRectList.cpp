/*
 * Copyright (c) 2022, DerpyCrabs <derpycrabs@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Geometry/DOMRect.h>
#include <LibWeb/Geometry/DOMRectList.h>

namespace Web::Geometry {

DOMRectList::DOMRectList(NonnullRefPtrVector<DOMRect>&& rects)
    : m_rects(move(rects))
{
}

// https://drafts.fxtf.org/geometry-1/#dom-domrectlist-length
u32 DOMRectList::length() const
{
    return m_rects.size();
}

// https://drafts.fxtf.org/geometry-1/#dom-domrectlist-item
DOMRect const* DOMRectList::item(u32 index) const
{
    // The item(index) method, when invoked, must return null when
    // index is greater than or equal to the number of DOMRect objects associated with the DOMRectList.
    // Otherwise, the DOMRect object at index must be returned. Indices are zero-based.
    if (index >= m_rects.size())
        return nullptr;
    return &m_rects[index];
}

bool DOMRectList::is_supported_property_index(u32 index) const
{
    return index < m_rects.size();
}

}
