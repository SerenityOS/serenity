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

#include <AK/StringBuilder.h>
#include <LibWeb/DOM/Position.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Layout/LayoutPosition.h>
#include <LibWeb/Page/Frame.h>

#include "EditEventHandler.h"

namespace Web {

void EditEventHandler::handle_delete(DOM::Position position)
{
    if (position.offset() == 0)
        TODO();

    if (is<DOM::Text>(*position.node())) {
        auto& node = downcast<DOM::Text>(*position.node());
        StringBuilder builder;
        builder.append(node.data().substring_view(0, position.offset() - 1));
        builder.append(node.data().substring_view(position.offset()));
        node.set_data(builder.to_string());

        m_frame.cursor_position().set_offset(m_frame.cursor_position().offset() - 1);
        node.invalidate_style();
    }
}

void EditEventHandler::handle_insert(DOM::Position position, u32 code_point)
{
    // FIXME: Unicode fiasco.

    if (is<DOM::Text>(*position.node())) {
        auto& node = downcast<DOM::Text>(*position.node());
        StringBuilder builder;
        builder.append(node.data().substring_view(0, position.offset()));
        builder.append_code_point(code_point);
        builder.append(node.data().substring_view(position.offset()));
        node.set_data(builder.to_string());

        m_frame.cursor_position().set_offset(m_frame.cursor_position().offset() + 1);
        node.invalidate_style();
    }
}

}
