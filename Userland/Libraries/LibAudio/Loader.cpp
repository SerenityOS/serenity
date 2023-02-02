/*
 * Copyright (c) 2018-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibAudio/FlacLoader.h>
#include <LibAudio/Loader.h>
#include <LibAudio/MP3Loader.h>
#include <LibAudio/QOALoader.h>
#include <LibAudio/WavLoader.h>

namespace Audio {

LoaderPlugin::LoaderPlugin(NonnullOwnPtr<SeekableStream> stream)
    : m_stream(move(stream))
{
}

Loader::Loader(NonnullOwnPtr<LoaderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::create_plugin(StringView path)
{
    {
        auto plugin = WavLoaderPlugin::create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = FlacLoaderPlugin::create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = MP3LoaderPlugin::create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = QOALoaderPlugin::create(path);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    return LoaderError { "No loader plugin available" };
}

Result<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::create_plugin(Bytes buffer)
{
    {
        auto plugin = WavLoaderPlugin::create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = FlacLoaderPlugin::create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = MP3LoaderPlugin::create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    {
        auto plugin = QOALoaderPlugin::create(buffer);
        if (!plugin.is_error())
            return NonnullOwnPtr<LoaderPlugin>(plugin.release_value());
    }

    return LoaderError { "No loader plugin available" };
}

}
