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

LoaderPlugin::LoaderPlugin(OwnPtr<Core::Stream::SeekableStream> stream)
    : m_stream(move(stream))
{
}

Loader::Loader(NonnullOwnPtr<LoaderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::try_create(StringView path)
{
    {
        auto plugin = WavLoaderPlugin::try_create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = FlacLoaderPlugin::try_create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = MP3LoaderPlugin::try_create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    return LoaderError { "No loader plugin available" };
}

Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::try_create(Bytes buffer)
{
    {
        auto plugin = WavLoaderPlugin::try_create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = FlacLoaderPlugin::try_create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = MP3LoaderPlugin::try_create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    return LoaderError { "No loader plugin available" };
}

}
