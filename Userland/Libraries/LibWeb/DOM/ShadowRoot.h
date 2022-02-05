/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/DocumentFragment.h>

namespace Web::DOM {

class ShadowRoot final : public DocumentFragment {
public:
    ShadowRoot(Document&, Element&);

    bool closed() const { return m_closed; }

    bool delegates_focus() const { return m_delegates_focus; }
    void set_delegates_focus(bool delegates_focus) { m_delegates_focus = delegates_focus; }

    bool available_to_element_internals() const { return m_available_to_element_internals; }
    void set_available_to_element_internals(bool available_to_element_internals) { m_available_to_element_internals = available_to_element_internals; }

    // ^EventTarget
    virtual EventTarget* get_parent(const Event&) override;

    // NOTE: This is intended for the JS bindings.
    String mode() const { return m_closed ? "closed" : "open"; }

    String inner_html() const;
    ExceptionOr<void> set_inner_html(String const&);

private:
    // ^Node
    virtual FlyString node_name() const override { return "#shadow-root"; }

    // NOTE: The specification doesn't seem to specify a default value for closed. Assuming false for now.
    bool m_closed { false };
    bool m_delegates_focus { false };
    bool m_available_to_element_internals { false };
};

}
