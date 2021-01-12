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

#include <Clipboard/Storage.h>

namespace Clipboard {

Storage& Storage::the()
{
    static Storage* s_the;
    if (!s_the)
        s_the = new Storage;
    return *s_the;
}

Storage::Storage()
{
}

Storage::~Storage()
{
}

void Storage::set_data(NonnullRefPtr<SharedBuffer> data, size_t data_size, const String& mime_type, const HashMap<String, String>& metadata)
{
    dbg() << "Storage::set_data <- [" << mime_type << "] " << data->data<void>() << " (" << data_size << " bytes)";
    for (auto& it : metadata) {
        dbg() << "  " << it.key << ": " << it.value;
    }
    m_shared_buffer = move(data);
    m_data_size = data_size;
    m_mime_type = mime_type;
    m_metadata = metadata;

    if (on_content_change)
        on_content_change();
}

}
