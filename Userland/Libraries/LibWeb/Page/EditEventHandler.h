/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibWeb/Forward.h>

namespace Web {

class EditEventHandler {
public:
    explicit EditEventHandler(HTML::BrowsingContext& frame)
        : m_frame(frame)
    {
    }

    virtual ~EditEventHandler() = default;

    virtual void handle_delete_character_after(const DOM::Position&);
    virtual void handle_delete(DOM::Range&);
    virtual void handle_insert(DOM::Position, u32 code_point);

private:
    HTML::BrowsingContext& m_frame;
};

}
