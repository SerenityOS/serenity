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

#include <WindowServer/WSClipboard.h>

WSClipboard& WSClipboard::the()
{
    static WSClipboard* s_the;
    if (!s_the)
        s_the = new WSClipboard;
    return *s_the;
}

WSClipboard::WSClipboard()
{
}

WSClipboard::~WSClipboard()
{
}

const u8* WSClipboard::data() const
{
    if (!m_shared_buffer)
        return nullptr;
    return (const u8*)m_shared_buffer->data();
}

int WSClipboard::size() const
{
    if (!m_shared_buffer)
        return 0;
    return m_contents_size;
}

void WSClipboard::clear()
{
    m_shared_buffer = nullptr;
    m_contents_size = 0;
}

void WSClipboard::set_data(NonnullRefPtr<SharedBuffer>&& data, int contents_size, const String& data_type)
{
    dbg() << "WSClipboard::set_data <- [" << data_type << "] " << data->data() << " (" << contents_size << " bytes)";
    m_shared_buffer = move(data);
    m_contents_size = contents_size;
    m_data_type = data_type;

    if (on_content_change)
        on_content_change();
}
