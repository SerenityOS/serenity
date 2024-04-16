/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibWeb/Forward.h>

namespace Web {

class EditEventHandler {
public:
    explicit EditEventHandler(HTML::BrowsingContext& browsing_context)
        : m_browsing_context(browsing_context)
    {
    }

    virtual ~EditEventHandler() = default;

    virtual void handle_delete_character_after(JS::NonnullGCPtr<DOM::Position>);
    virtual void handle_delete(DOM::Range&);
    virtual void handle_insert(JS::NonnullGCPtr<DOM::Position>, u32 code_point);
    virtual void handle_insert(JS::NonnullGCPtr<DOM::Position>, String);

private:
    JS::NonnullGCPtr<HTML::BrowsingContext> m_browsing_context;
};

}
