/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/KeyframeEffect.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSAnimation.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

JS::NonnullGCPtr<CSSAnimation> CSSAnimation::create(JS::Realm& realm)
{
    return realm.heap().allocate<CSSAnimation>(realm, realm);
}

// https://www.w3.org/TR/css-animations-2/#animation-composite-order
Optional<int> CSSAnimation::class_specific_composite_order(JS::NonnullGCPtr<Animations::Animation> other_animation) const
{
    auto other = JS::NonnullGCPtr { verify_cast<CSSAnimation>(*other_animation) };

    // The existance of an owning element determines the animation class, so both animations should have their owning
    // element in the same state
    VERIFY(!m_owning_element == !other->m_owning_element);

    // Within the set of CSS Animations with an owning element, two animations A and B are sorted in composite order
    // (first to last) as follows:
    if (m_owning_element) {
        // 1. If the owning element of A and B differs, sort A and B by tree order of their corresponding owning elements.
        //    With regard to pseudo-elements, the sort order is as follows:
        //    - element
        //    - ::marker
        //    - ::before
        //    - any other pseudo-elements not mentioned specifically in this list, sorted in ascending order by the Unicode
        //      codepoints that make up each selector
        //    - ::after
        //    - element children
        if (m_owning_element.ptr() != other->m_owning_element.ptr()) {
            // FIXME: Sort by tree order
            return {};
        }

        // 2. Otherwise, sort A and B based on their position in the computed value of the animation-name property of the
        //    (common) owning element.
        // FIXME: Do this when animation-name supports multiple values
        return {};
    }

    // The composite order of CSS Animations without an owning element is based on their position in the global animation list.
    return global_animation_list_order() - other->global_animation_list_order();
}

Animations::AnimationClass CSSAnimation::animation_class() const
{
    if (m_owning_element)
        return Animations::AnimationClass::CSSAnimationWithOwningElement;
    return Animations::AnimationClass::CSSAnimationWithoutOwningElement;
}

CSSAnimation::CSSAnimation(JS::Realm& realm)
    : Animations::Animation(realm)
{
}

void CSSAnimation::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSAnimationPrototype>(realm, "CSSAnimation"_fly_string));
}

void CSSAnimation::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_owning_element);
}

}
