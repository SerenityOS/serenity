/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/WeakPtr.h>
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>
#include <LibWeb/Page/EditEventHandler.h>

namespace Web {

class EventHandler {
public:
    explicit EventHandler(Badge<HTML::BrowsingContext>, HTML::BrowsingContext&);
    ~EventHandler();

    bool handle_mouseup(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousedown(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousemove(const Gfx::IntPoint&, unsigned buttons, unsigned modifiers);
    bool handle_mousewheel(const Gfx::IntPoint&, unsigned buttons, unsigned modifiers, int wheel_delta_x, int wheel_delta_y);

    bool handle_keydown(KeyCode, unsigned modifiers, u32 code_point);
    bool handle_keyup(KeyCode, unsigned modifiers, u32 code_point);

    void set_mouse_event_tracking_layout_node(Layout::Node*);

    void set_edit_event_handler(NonnullOwnPtr<EditEventHandler> value) { m_edit_event_handler = move(value); }

private:
    bool focus_next_element();
    bool focus_previous_element();

    Layout::InitialContainingBlock* layout_root();
    const Layout::InitialContainingBlock* layout_root() const;

    HTML::BrowsingContext& m_browsing_context;

    bool m_in_mouse_selection { false };

    WeakPtr<Layout::Node> m_mouse_event_tracking_layout_node;

    NonnullOwnPtr<EditEventHandler> m_edit_event_handler;
};

}
