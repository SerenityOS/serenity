/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Types.h>
#include <AK/Weakable.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::SVG {

// https://www.w3.org/TR/SVG11/types.html#InterfaceSVGLength
class SVGLength
    : public RefCounted<SVGLength>
    , public Bindings::Wrappable
    , public Weakable<SVGLength> {
public:
    using WrapperType = Bindings::SVGLengthWrapper;

    static NonnullRefPtr<SVGLength> create(u8 unit_type, float value);
    virtual ~SVGLength() = default;

    u8 unit_type() const { return m_unit_type; }

    float value() const { return m_value; }
    DOM::ExceptionOr<void> set_value(float value);

private:
    SVGLength(u8 unit_type, float value);

    u8 m_unit_type { 0 };
    float m_value { 0 };
};

}
