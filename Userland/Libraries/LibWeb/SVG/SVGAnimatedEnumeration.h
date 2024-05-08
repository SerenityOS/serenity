/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/types.html#InterfaceSVGAnimatedEnumeration
class SVGAnimatedEnumeration final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SVGAnimatedEnumeration, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SVGAnimatedEnumeration);

public:
    [[nodiscard]] static JS::NonnullGCPtr<SVGAnimatedEnumeration> create(JS::Realm&, WebIDL::UnsignedShort base_val, WebIDL::UnsignedShort anim_val);
    virtual ~SVGAnimatedEnumeration() override;

    WebIDL::UnsignedShort base_val() const { return m_base_val; }
    WebIDL::UnsignedShort anim_val() const { return m_anim_val; }

    WebIDL::ExceptionOr<void> set_base_val(WebIDL::UnsignedShort base_val);

private:
    SVGAnimatedEnumeration(JS::Realm&, WebIDL::UnsignedShort base_val, WebIDL::UnsignedShort anim_val);

    virtual void initialize(JS::Realm&) override;

    WebIDL::UnsignedShort m_base_val;
    WebIDL::UnsignedShort m_anim_val;
};

}
