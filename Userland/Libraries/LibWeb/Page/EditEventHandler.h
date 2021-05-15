/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibWeb/Forward.h>

namespace Web {

class EditEventHandler {
public:
    explicit EditEventHandler(Frame& frame)
        : m_frame(frame)
    {
    }

    virtual ~EditEventHandler() = default;

    virtual void handle_delete(DOM::Range&);
    virtual void handle_insert(DOM::Position, u32 code_point);

private:
    Frame& m_frame;
};

}
