/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/SVG/SVGAnimatedLength.h>

namespace Web::SVG {

NonnullRefPtr<SVGAnimatedLength> SVGAnimatedLength::create(NonnullRefPtr<SVGLength> base_val, NonnullRefPtr<SVGLength> anim_val)
{
    return adopt_ref(*new SVGAnimatedLength(move(base_val), move(anim_val)));
}

SVGAnimatedLength::SVGAnimatedLength(NonnullRefPtr<SVGLength> base_val, NonnullRefPtr<SVGLength> anim_val)
    : m_base_val(move(base_val))
    , m_anim_val(move(anim_val))
{
    // The object referenced by animVal will always be distinct from the one referenced by baseVal, even when the attribute is not animated.
    VERIFY(m_base_val.ptr() != m_anim_val.ptr());
}

}
