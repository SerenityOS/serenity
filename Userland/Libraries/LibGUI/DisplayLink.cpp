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

#include <AK/Badge.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <LibGUI/DisplayLink.h>
#include <LibGUI/WindowServerConnection.h>

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
    static HashMap<i32, RefPtr<DisplayLinkCallback>>* map;
    if (!map)
        map = new HashMap<i32, RefPtr<DisplayLinkCallback>>;
    return *map;
}

static i32 s_next_callback_id = 1;

i32 DisplayLink::register_callback(Function<void(i32)> callback)
{
    if (callbacks().is_empty())
        WindowServerConnection::the().post_message(Messages::WindowServer::EnableDisplayLink());

    i32 callback_id = s_next_callback_id++;
    callbacks().set(callback_id, adopt(*new DisplayLinkCallback(callback_id, move(callback))));

    return callback_id;
}

bool DisplayLink::unregister_callback(i32 callback_id)
{
    ASSERT(callbacks().contains(callback_id));
    callbacks().remove(callback_id);

    if (callbacks().is_empty())
        WindowServerConnection::the().post_message(Messages::WindowServer::DisableDisplayLink());

    return true;
}

void DisplayLink::notify(Badge<WindowServerConnection>)
{
    auto copy_of_callbacks = callbacks();
    for (auto& it : copy_of_callbacks)
        it.value->invoke();
}

}
