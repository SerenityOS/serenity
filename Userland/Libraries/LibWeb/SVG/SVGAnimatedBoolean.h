/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::SVG {

// https://svgwg.org/svg2-draft/types.html#InterfaceSVGAnimatedBoolean
class SVGAnimatedBoolean final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(SVGAnimatedBoolean, Bindings::PlatformObject);
    JS_DECLARE_ALLOCATOR(SVGAnimatedBoolean);

public:
    [[nodiscard]] static JS::NonnullGCPtr<SVGAnimatedBoolean> create(JS::Realm&, bool base_val, bool anim_val);
    virtual ~SVGAnimatedBoolean() override;

    bool base_val() const { return m_base_val; }
    bool anim_val() const { return m_anim_val; }

    void set_base_val(bool base_val) { m_base_val = base_val; }

private:
    SVGAnimatedBoolean(JS::Realm&, bool base_val, bool anim_val);

    virtual void initialize(JS::Realm&) override;

    bool m_base_val;
    bool m_anim_val;
};

}
