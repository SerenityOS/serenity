/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Animations/Animation.h>
#include <LibWeb/CSS/PropertyID.h>
#include <LibWeb/CSS/StyleValue.h>

namespace Web::CSS {

// https://www.w3.org/TR/css-animations-2/#cssanimation
class CSSAnimation : public Animations::Animation {
    WEB_PLATFORM_OBJECT(CSSAnimation, Animations::Animation);
    JS_DECLARE_ALLOCATOR(CSSAnimation);

public:
    static JS::NonnullGCPtr<CSSAnimation> create(JS::Realm&);

    JS::GCPtr<DOM::Element> owning_element() const override { return m_owning_element; }
    void set_owning_element(JS::GCPtr<DOM::Element> value) { m_owning_element = value; }

    FlyString const& animation_name() const { return id(); }

    virtual Animations::AnimationClass animation_class() const override;
    virtual Optional<int> class_specific_composite_order(JS::NonnullGCPtr<Animations::Animation> other) const override;

private:
    explicit CSSAnimation(JS::Realm&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    virtual bool is_css_animation() const override { return true; }

    // https://www.w3.org/TR/css-animations-2/#owning-element-section
    JS::GCPtr<DOM::Element> m_owning_element;
};

}
