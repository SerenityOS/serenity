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

#pragma once

#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/SharedBuffer.h>
#include <AK/String.h>

namespace Clipboard {

class Storage {
public:
    static Storage& the();
    ~Storage();

    bool has_data() const { return m_shared_buffer; }

    const String& mime_type() const { return m_mime_type; }
    const HashMap<String, String>& metadata() const { return m_metadata; }

    const u8* data() const
    {
        if (!has_data())
            return nullptr;
        return m_shared_buffer->data<u8>();
    }

    size_t data_size() const
    {
        if (has_data())
            return m_data_size;
        return 0;
    }

    void set_data(NonnullRefPtr<SharedBuffer>, size_t data_size, const String& mime_type, const HashMap<String, String>& metadata);

    Function<void()> on_content_change;

private:
    Storage();

    String m_mime_type;
    RefPtr<SharedBuffer> m_shared_buffer;
    size_t m_data_size { 0 };
    HashMap<String, String> m_metadata;
};

}
