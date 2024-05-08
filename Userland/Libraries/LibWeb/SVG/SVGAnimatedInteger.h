/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/types.html#InterfaceSVGAnimatedInteger
class SVGAnimatedInteger final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SVGAnimatedInteger, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SVGAnimatedInteger);

public:
    [[nodiscard]] static JS::NonnullGCPtr<SVGAnimatedInteger> create(JS::Realm&, u32 base_val, u32 anim_val);
    virtual ~SVGAnimatedInteger() override;

    u32 base_val() const { return m_base_val; }
    u32 anim_val() const { return m_anim_val; }

    void set_base_val(u32 base_val) { m_base_val = base_val; }

private:
    SVGAnimatedInteger(JS::Realm&, u32 base_val, u32 anim_val);

    virtual void initialize(JS::Realm&) override;

    u32 m_base_val;
    u32 m_anim_val;
};

}
