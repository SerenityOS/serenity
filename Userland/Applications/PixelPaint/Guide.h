/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>

namespace PixelPaint {

class Guide : public RefCounted<Guide> {
public:
    enum class Orientation {
        Unset,
        Vertical,
        Horizontal,
    };

    Guide(Orientation orientation, float offset)
        : m_orientation(orientation)
        , m_offset(offset)
    {
    }

    static NonnullRefPtr<Guide> construct(Orientation orientation, float offset)
    {
        return make_ref_counted<Guide>(orientation, offset);
    }

    Orientation orientation() const { return m_orientation; }
    float offset() const { return m_offset; }

    void set_offset(float offset) { m_offset = offset; }
    void set_orientation(Orientation orientation) { m_orientation = orientation; }

private:
    Orientation m_orientation;
    float m_offset { 0.0 };
};

};
