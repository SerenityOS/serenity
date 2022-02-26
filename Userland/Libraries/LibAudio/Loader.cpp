/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/FlacLoader.h>
#include <LibAudio/Loader.h>
#include <LibAudio/MP3Loader.h>
#include <LibAudio/WavLoader.h>

namespace Audio {

Loader::Loader(NonnullOwnPtr<LoaderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::try_create(StringView path)
{
    NonnullOwnPtr<LoaderPlugin> plugin = adopt_own(*new WavLoaderPlugin(path));
    auto initstate0 = plugin->initialize();
    if (!initstate0.is_error())
        return plugin;

    plugin = adopt_own(*new FlacLoaderPlugin(path));
    auto initstate1 = plugin->initialize();
    if (!initstate1.is_error())
        return plugin;

    plugin = adopt_own(*new MP3LoaderPlugin(path));
    auto initstate2 = plugin->initialize();
    if (!initstate2.is_error())
        return plugin;

    return LoaderError { "No loader plugin available" };
}

Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::try_create(Bytes& buffer)
{
    NonnullOwnPtr<LoaderPlugin> plugin = adopt_own(*new WavLoaderPlugin(buffer));
    if (auto initstate = plugin->initialize(); !initstate.is_error())
        return plugin;
    plugin = adopt_own(*new FlacLoaderPlugin(buffer));
    if (auto initstate = plugin->initialize(); !initstate.is_error())
        return plugin;
    return LoaderError { "No loader plugin available" };
}

}
