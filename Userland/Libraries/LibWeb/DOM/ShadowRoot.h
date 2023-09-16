/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/ShadowRootPrototype.h>
#include <LibWeb/DOM/DocumentFragment.h>

namespace Web::DOM {

class ShadowRoot final : public DocumentFragment {
    WEB_PLATFORM_OBJECT(ShadowRoot, DocumentFragment);

public:
    Bindings::ShadowRootMode mode() const { return m_mode; }

    Bindings::SlotAssignmentMode slot_assignment() const { return m_slot_assignment; }
    void set_slot_assignment(Bindings::SlotAssignmentMode slot_assignment) { m_slot_assignment = slot_assignment; }

    bool delegates_focus() const { return m_delegates_focus; }
    void set_delegates_focus(bool delegates_focus) { m_delegates_focus = delegates_focus; }

    bool available_to_element_internals() const { return m_available_to_element_internals; }
    void set_available_to_element_internals(bool available_to_element_internals) { m_available_to_element_internals = available_to_element_internals; }

    // ^EventTarget
    virtual EventTarget* get_parent(Event const&) override;

    WebIDL::ExceptionOr<DeprecatedString> inner_html() const;
    WebIDL::ExceptionOr<void> set_inner_html(StringView);

private:
    ShadowRoot(Document&, Element& host, Bindings::ShadowRootMode);
    virtual void initialize(JS::Realm&) override;

    // ^Node
    virtual FlyString node_name() const override { return "#shadow-root"_fly_string; }
    virtual bool is_shadow_root() const final { return true; }

    // NOTE: The specification doesn't seem to specify a default value for mode. Assuming closed for now.
    Bindings::ShadowRootMode m_mode { Bindings::ShadowRootMode::Closed };
    Bindings::SlotAssignmentMode m_slot_assignment { Bindings::SlotAssignmentMode::Named };
    bool m_delegates_focus { false };
    bool m_available_to_element_internals { false };
};

template<>
inline bool Node::fast_is<ShadowRoot>() const { return is_shadow_root(); }

template<typename Callback>
inline IterationDecision Node::for_each_shadow_including_inclusive_descendant(Callback callback)
{
    if (callback(*this) == IterationDecision::Break)
        return IterationDecision::Break;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (child->is_element()) {
            if (JS::GCPtr<ShadowRoot> shadow_root = static_cast<Element*>(child)->shadow_root_internal()) {
                if (shadow_root->for_each_shadow_including_inclusive_descendant(callback) == IterationDecision::Break)
                    return IterationDecision::Break;
            }
        }
        if (child->for_each_shadow_including_inclusive_descendant(callback) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

template<typename Callback>
inline IterationDecision Node::for_each_shadow_including_descendant(Callback callback)
{
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (child->is_element()) {
            if (JS::GCPtr<ShadowRoot> shadow_root = static_cast<Element*>(child)->shadow_root()) {
                if (shadow_root->for_each_shadow_including_inclusive_descendant(callback) == IterationDecision::Break)
                    return IterationDecision::Break;
            }
        }
        if (child->for_each_shadow_including_inclusive_descendant(callback) == IterationDecision::Break)
            return IterationDecision::Break;
    }
    return IterationDecision::Continue;
}

}
