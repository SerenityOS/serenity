/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/ToggleTaskTracker.h>

namespace Web::HTML {

class HTMLDetailsElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLDetailsElement, HTMLElement);

public:
    virtual ~HTMLDetailsElement() override;

    // https://www.w3.org/TR/html-aria/#el-details
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::group; }

private:
    HTMLDetailsElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;

    virtual void attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value) override;

    void queue_a_details_toggle_event_task(String old_state, String new_state);

    // https://html.spec.whatwg.org/multipage/interactive-elements.html#details-toggle-task-tracker
    Optional<ToggleTaskTracker> m_details_toggle_task_tracker;
};

}
