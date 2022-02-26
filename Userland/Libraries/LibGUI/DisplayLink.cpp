/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Badge.h>
#include <AK/HashMap.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/DisplayLink.h>

namespace GUI {

class DisplayLinkCallback : public RefCounted<DisplayLinkCallback> {
public:
    DisplayLinkCallback(i32 link_id, Function<void(i32)> callback)
        : m_link_id(link_id)
        , m_callback(move(callback))
    {
    }

    void invoke()
    {
        m_callback(m_link_id);
    }

private:
    i32 m_link_id { 0 };
    Function<void(i32)> m_callback;
};

static HashMap<i32, RefPtr<DisplayLinkCallback>>& callbacks()
{
    static HashMap<i32, RefPtr<DisplayLinkCallback>> s_map;
    return s_map;
}

static i32 s_next_callback_id = 1;

i32 DisplayLink::register_callback(Function<void(i32)> callback)
{
    if (callbacks().is_empty())
        ConnectionToWindowServer::the().async_enable_display_link();

    i32 callback_id = s_next_callback_id++;
    callbacks().set(callback_id, adopt_ref(*new DisplayLinkCallback(callback_id, move(callback))));

    return callback_id;
}

bool DisplayLink::unregister_callback(i32 callback_id)
{
    VERIFY(callbacks().contains(callback_id));
    callbacks().remove(callback_id);

    if (callbacks().is_empty())
        ConnectionToWindowServer::the().async_disable_display_link();

    return true;
}

void DisplayLink::notify(Badge<ConnectionToWindowServer>)
{
    auto copy_of_callbacks = callbacks();
    for (auto& it : copy_of_callbacks)
        it.value->invoke();
}

}
