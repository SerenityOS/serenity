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
#include <LibCore/MappedFile.h>

namespace Audio {

LoaderPlugin::LoaderPlugin(NonnullOwnPtr<SeekableStream> stream)
    : m_stream(move(stream))
{
}

Loader::Loader(NonnullOwnPtr<LoaderPlugin> plugin)
    : m_plugin(move(plugin))
{
}

struct LoaderPluginInitializer {
    bool (*sniff)(SeekableStream&);
    ErrorOr<NonnullOwnPtr<LoaderPlugin>, LoaderError> (*create)(NonnullOwnPtr<SeekableStream>);
};

#define ENUMERATE_LOADER_PLUGINS    \
    __ENUMERATE_LOADER_PLUGIN(Wav)  \
    __ENUMERATE_LOADER_PLUGIN(Flac) \
    __ENUMERATE_LOADER_PLUGIN(QOA)  \
    __ENUMERATE_LOADER_PLUGIN(MP3)

static constexpr LoaderPluginInitializer s_initializers[] = {
#define __ENUMERATE_LOADER_PLUGIN(Type) \
    { Type##LoaderPlugin::sniff, Type##LoaderPlugin::create },
    ENUMERATE_LOADER_PLUGINS
#undef __ENUMERATE_LOADER_PLUGIN
};

ErrorOr<NonnullRefPtr<Loader>, LoaderError> Loader::create(StringView path)
{
    auto stream = TRY(Core::MappedFile::map(path, Core::MappedFile::Mode::ReadOnly));
    return adopt_ref(*new (nothrow) Loader(TRY(Loader::create_plugin(move(stream)))));
}

ErrorOr<NonnullRefPtr<Loader>, LoaderError> Loader::create(ReadonlyBytes buffer)
{
    auto stream = TRY(try_make<FixedMemoryStream>(buffer));
    return adopt_ref(*new (nothrow) Loader(TRY(Loader::create_plugin(move(stream)))));
}

ErrorOr<NonnullOwnPtr<LoaderPlugin>, LoaderError> Loader::create_plugin(NonnullOwnPtr<SeekableStream> stream)
{
    for (auto const& loader : s_initializers) {
        if (loader.sniff(*stream)) {
            TRY(stream->seek(0, SeekMode::SetPosition));
            return loader.create(move(stream));
        }
        TRY(stream->seek(0, SeekMode::SetPosition));
    }

    return LoaderError { "No loader plugin available" };
}

LoaderSamples Loader::get_more_samples(size_t samples_to_read_from_input)
{
    if (m_plugin_at_end_of_stream && m_buffer.is_empty())
        return FixedArray<Sample> {};

    size_t remaining_samples = total_samples() - loaded_samples();
    size_t samples_to_read = min(remaining_samples, samples_to_read_from_input);
    auto samples = TRY(FixedArray<Sample>::create(samples_to_read));

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
        if (chunk_data.is_empty()) {
            m_plugin_at_end_of_stream = true;
            break;
        }
        for (auto& chunk : chunk_data) {
            if (sample_index < samples_to_read) {
                auto count = min(samples_to_read - sample_index, chunk.size());
                AK::TypedTransfer<Sample>::move(samples.span().offset(sample_index), chunk.data(), count);
                // We didn't read all of the chunk; transfer the rest into the buffer.
                if (count < chunk.size()) {
                    auto remaining_samples_count = chunk.size() - count;
                    // We will always have an empty buffer at this point!
                    TRY(m_buffer.try_append(chunk.span().offset(count), remaining_samples_count));
                }
            } else {
                // We're now past what the user requested. Transfer the entirety of the data into the buffer.
                TRY(m_buffer.try_append(chunk.data(), chunk.size()));
            }
            sample_index += chunk.size();
        }
    }

    return samples;
}

}
