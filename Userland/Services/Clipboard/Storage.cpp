/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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

void Storage::set_data(Core::AnonymousBuffer data, String const& mime_type, const HashMap<String, String>& metadata)
{
    m_buffer = move(data);
    m_data_size = data.size();
    m_mime_type = mime_type;
    m_metadata = metadata;

    if (on_content_change)
        on_content_change();
}

}
