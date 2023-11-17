/*
 * Copyright (c) 2023, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/Animation.h>
#include <LibWeb/Animations/AnimationTimeline.h>
#include <LibWeb/DOM/Document.h>

namespace Web::Animations {

WebIDL::ExceptionOr<void> AnimationTimeline::set_current_time(Optional<double> value)
{
    if (value == m_current_time)
        return {};

    if (m_is_monotonically_increasing && m_current_time.has_value()) {
        if (!value.has_value() || value.value() < m_current_time.value())
            m_is_monotonically_increasing = false;
    }

    m_current_time = value;

    for (auto& animation : m_associated_animations)
        TRY(animation->set_current_time(value));

    return {};
}

void AnimationTimeline::set_associated_document(JS::GCPtr<DOM::Document> document)
{
    if (document)
        document->associate_with_timeline(*this);
    if (m_associated_document)
        m_associated_document->disassociate_with_timeline(*this);
    m_associated_document = document;
}

// https://www.w3.org/TR/web-animations-1/#inactive-timeline
bool AnimationTimeline::is_inactive() const
{
    // A timeline is considered to be inactive when its time value is unresolved.
    return !m_current_time.has_value();
}

AnimationTimeline::AnimationTimeline(JS::Realm& realm)
    : Bindings::PlatformObject(realm)
{
}

AnimationTimeline::~AnimationTimeline()
{
    if (m_associated_document)
        m_associated_document->disassociate_with_timeline(*this);
}

void AnimationTimeline::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::AnimationTimelinePrototype>(realm, "AnimationTimeline"));
}

void AnimationTimeline::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_associated_document);
    for (auto const& animation : m_associated_animations)
        visitor.visit(animation);
}

}
