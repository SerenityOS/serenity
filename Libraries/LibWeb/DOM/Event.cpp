/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <AK/Assertions.h>
#include <AK/TypeCasts.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/ShadowRoot.h>

namespace Web::DOM {

void Event::append_to_path(EventTarget& invocation_target, RefPtr<EventTarget> shadow_adjusted_target, RefPtr<EventTarget> related_target, TouchTargetList& touch_targets, bool slot_in_closed_tree)
{
    bool invocation_target_in_shadow_tree = false;
    bool root_of_closed_tree = false;

    if (is<Node>(invocation_target)) {
        auto& invocation_target_node = downcast<Node>(invocation_target);
        if (is<ShadowRoot>(invocation_target_node.root()))
            invocation_target_in_shadow_tree = true;
        if (is<ShadowRoot>(invocation_target_node)) {
            auto& invocation_target_shadow_root = downcast<ShadowRoot>(invocation_target_node);
            root_of_closed_tree = invocation_target_shadow_root.closed();
        }
    }

    m_path.append({ invocation_target, invocation_target_in_shadow_tree, shadow_adjusted_target, related_target, touch_targets, root_of_closed_tree, slot_in_closed_tree, m_path.size() });
}

void Event::set_cancelled_flag()
{
    if (m_cancelable && !m_in_passive_listener)
        m_cancelled = true;
}

}
