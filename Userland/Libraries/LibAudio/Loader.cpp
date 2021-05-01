/*
 * Copyright (c) 2018-2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/WavLoader.h>

namespace Audio {

Loader::Loader(const StringView& path)
{
    m_plugin = make<WavLoaderPlugin>(path);
    if (m_plugin->sniff())
        return;
    m_plugin = nullptr;
}

Loader::Loader(const ByteBuffer& buffer)
{
    m_plugin = make<WavLoaderPlugin>(buffer);
    if (m_plugin->sniff())
        return;
    m_plugin = nullptr;
}

}
