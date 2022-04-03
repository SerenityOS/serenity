/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/SVG/SVGLength.h>

namespace Web::SVG {

// https://www.w3.org/TR/SVG11/types.html#InterfaceSVGAnimatedLength
class SVGAnimatedLength
    : public RefCounted<SVGAnimatedLength>
    , public Bindings::Wrappable
    , public Weakable<SVGAnimatedLength> {
public:
    using WrapperType = Bindings::SVGAnimatedLengthWrapper;

    static NonnullRefPtr<SVGAnimatedLength> create(NonnullRefPtr<SVGLength> base_val, NonnullRefPtr<SVGLength> anim_val);
    virtual ~SVGAnimatedLength() = default;

    NonnullRefPtr<SVGLength> const& base_val() const { return m_base_val; }
    NonnullRefPtr<SVGLength> const& anim_val() const { return m_anim_val; }

private:
    SVGAnimatedLength(NonnullRefPtr<SVGLength> base_val, NonnullRefPtr<SVGLength> anim_val);

    NonnullRefPtr<SVGLength> m_base_val;
    NonnullRefPtr<SVGLength> m_anim_val;
};

}
