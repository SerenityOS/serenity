/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Function.h>
#include <YAK/HashMap.h>
#include <YAK/String.h>
#include <LibCore/AnonymousBuffer.h>

namespace Clipboard {

class Storage {
public:
    static Storage& the();
    ~Storage();

    bool has_data() const { return m_buffer.is_valid(); }

    const String& mime_type() const { return m_mime_type; }
    const HashMap<String, String>& metadata() const { return m_metadata; }

    const u8* data() const
    {
        if (!has_data())
            return nullptr;
        return m_buffer.data<u8>();
    }

    size_t data_size() const
    {
        if (has_data())
            return m_data_size;
        return 0;
    }

    void set_data(Core::AnonymousBuffer, const String& mime_type, const HashMap<String, String>& metadata);

    Function<void()> on_content_change;

    const Core::AnonymousBuffer& buffer() const { return m_buffer; }

private:
    Storage();

    String m_mime_type;
    Core::AnonymousBuffer m_buffer;
    size_t m_data_size { 0 };
    HashMap<String, String> m_metadata;
};

}
