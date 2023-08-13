/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::SVG {

// https://www.w3.org/TR/SVG11/types.html#InterfaceSVGLength
class SVGLength : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SVGLength, Bindings::PlatformObject);

public:
    [[nodiscard]] static JS::NonnullGCPtr<SVGLength> create(JS::Realm&, u8 unit_type, float value);
    virtual ~SVGLength() override;

    u8 unit_type() const { return m_unit_type; }

    float value() const { return m_value; }
    WebIDL::ExceptionOr<void> set_value(float value);

private:
    SVGLength(JS::Realm&, u8 unit_type, float value);

    virtual void initialize(JS::Realm&) override;

    u8 m_unit_type { 0 };
    float m_value { 0 };
};

}
