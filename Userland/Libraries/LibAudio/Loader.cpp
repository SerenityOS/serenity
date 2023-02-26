/*
 * Copyright (c) 2018-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypedTransfer.h>
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

LoaderSamples Loader::get_more_samples(size_t samples_to_read_from_input)
{
    size_t remaining_samples = total_samples() - loaded_samples();
    size_t samples_to_read = min(remaining_samples, samples_to_read_from_input);
    auto samples = LOADER_TRY(FixedArray<Sample>::create(samples_to_read));

    size_t sample_index = 0;

    if (m_buffer.size() > 0) {
        size_t to_transfer = min(m_buffer.size(), samples_to_read);
        AK::TypedTransfer<Sample>::move(samples.data(), m_buffer.data(), to_transfer);
        if (to_transfer < m_buffer.size())
            m_buffer.remove(0, to_transfer);
        else
            m_buffer.clear_with_capacity();

        sample_index += to_transfer;
    }

    while (sample_index < samples_to_read) {
        auto chunk_data = TRY(m_plugin->load_chunks(samples_to_read - sample_index));
        chunk_data.remove_all_matching([](auto& chunk) { return chunk.is_empty(); });
        if (chunk_data.is_empty())
            break;
        for (auto& chunk : chunk_data) {
            if (sample_index < samples_to_read) {
                auto count = min(samples_to_read - sample_index, chunk.size());
                AK::TypedTransfer<Sample>::move(samples.span().offset(sample_index), chunk.data(), count);
                // We didn't read all of the chunk; transfer the rest into the buffer.
                if (count < chunk.size()) {
                    auto remaining_samples_count = chunk.size() - count;
                    // We will always have an empty buffer at this point!
                    LOADER_TRY(m_buffer.try_append(chunk.span().offset(count), remaining_samples_count));
                }
            } else {
                // We're now past what the user requested. Transfer the entirety of the data into the buffer.
                LOADER_TRY(m_buffer.try_append(chunk.data(), chunk.size()));
            }
            sample_index += chunk.size();
        }
    }

    return samples;
}

}
