/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Forward.h>
#include <AK/WeakPtr.h>
#include <Kernel/API/KeyCode.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>
#include <LibWeb/Forward.h>

namespace Web {

class Frame;

class EventHandler {
public:
    explicit EventHandler(Badge<Frame>, Frame&);
    ~EventHandler();

    bool handle_mouseup(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousedown(const Gfx::IntPoint&, unsigned button, unsigned modifiers);
    bool handle_mousemove(const Gfx::IntPoint&, unsigned buttons, unsigned modifiers);

    bool handle_keydown(KeyCode, unsigned modifiers, u32 code_point);

    void set_mouse_event_tracking_layout_node(LayoutNode*);

private:
    bool focus_next_element();
    bool focus_previous_element();

    LayoutDocument* layout_root();
    const LayoutDocument* layout_root() const;

    void dump_selection(const char* event_name) const;

    Frame& m_frame;

    bool m_in_mouse_selection { false };

    WeakPtr<LayoutNode> m_mouse_event_tracking_layout_node;
};

}
