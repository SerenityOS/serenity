/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/SharedBuffer.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/WindowServerConnection.h>

namespace GUI {

Clipboard& Clipboard::the()
{
    static Clipboard* s_the;
    if (!s_the)
        s_the = new Clipboard;
    return *s_the;
}

Clipboard::Clipboard()
{
}

Clipboard::DataAndType Clipboard::data_and_type() const
{
    auto response = WindowServerConnection::the().send_sync<Messages::WindowServer::GetClipboardContents>();
    if (response->shared_buffer_id() < 0)
        return {};
    auto shared_buffer = SharedBuffer::create_from_shared_buffer_id(response->shared_buffer_id());
    if (!shared_buffer) {
        dbgprintf("GUI::Clipboard::data() failed to attach to the shared buffer\n");
        return {};
    }
    if (response->content_size() > shared_buffer->size()) {
        dbgprintf("GUI::Clipboard::data() clipping contents size is greater than shared buffer size\n");
        return {};
    }
    auto data = String((const char*)shared_buffer->data(), response->content_size());
    auto type = response->content_type();
    return { data, type };
}

void Clipboard::set_data(const StringView& data, const String& type)
{
    auto shared_buffer = SharedBuffer::create_with_size(data.length() + 1);
    if (!shared_buffer) {
        dbgprintf("GUI::Clipboard::set_data() failed to create a shared buffer\n");
        return;
    }
    if (!data.is_empty())
        memcpy(shared_buffer->data(), data.characters_without_null_termination(), data.length() + 1);
    else
        ((u8*)shared_buffer->data())[0] = '\0';
    shared_buffer->seal();
    shared_buffer->share_with(WindowServerConnection::the().server_pid());

    WindowServerConnection::the().send_sync<Messages::WindowServer::SetClipboardContents>(shared_buffer->shared_buffer_id(), data.length(), type);
}

void Clipboard::did_receive_clipboard_contents_changed(Badge<WindowServerConnection>, const String& data_type)
{
    if (on_content_change)
        on_content_change(data_type);
}

}
