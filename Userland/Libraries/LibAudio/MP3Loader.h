/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Buffer.h"
#include "Loader.h"
#include "MP3Types.h"
#include <AK/Tuple.h>
#include <LibCore/FileStream.h>
#include <LibDSP/MDCT.h>

namespace Audio {

namespace MP3::Tables {
struct ScaleFactorBand;
}

class MP3LoaderPlugin : public LoaderPlugin {
public:
    explicit MP3LoaderPlugin(StringView path);
    ~MP3LoaderPlugin();

    virtual MaybeLoaderError initialize() override;
    virtual LoaderSamples get_more_samples(size_t max_bytes_to_read_from_input = 128 * KiB) override;

    virtual MaybeLoaderError reset() override;
    virtual MaybeLoaderError seek(int const position) override;

    virtual int loaded_samples() override { return m_loaded_samples; }
    virtual int total_samples() override { return m_total_samples; }
    virtual u32 sample_rate() override { return m_sample_rate; }
    virtual u16 num_channels() override { return m_num_channels; }
    virtual PcmSampleFormat pcm_format() override { return m_sample_format; }
    virtual RefPtr<Core::File> file() override { return m_file; }
    virtual String format_name() override { return "MP3 (.mp3)"; }

private:
    MaybeLoaderError synchronize();
    MaybeLoaderError build_seek_table();
    ErrorOr<MP3::Header, LoaderError> read_header();
    ErrorOr<MP3::MP3Frame, LoaderError> read_next_frame();
    ErrorOr<MP3::MP3Frame, LoaderError> read_frame_data(MP3::Header const&, bool is_first_frame);
    MaybeLoaderError read_side_information(MP3::MP3Frame&);
    ErrorOr<size_t, LoaderError> read_scale_factors(MP3::MP3Frame&, InputBitStream& reservoir, size_t granule_index, size_t channel_index);
    MaybeLoaderError read_huffman_data(MP3::MP3Frame&, InputBitStream& reservoir, size_t granule_index, size_t channel_index, size_t granule_bits_read);
    static AK::Array<double, 576> calculate_frame_exponents(MP3::MP3Frame const&, size_t granule_index, size_t channel_index);
    static void reorder_samples(MP3::Granule&, u32 sample_rate);
    static void reduce_alias(MP3::Granule&, size_t max_subband_index = 576);
    static void process_stereo(MP3::MP3Frame&, size_t granule_index);
    static void transform_samples_to_time(Array<double, 576> const& input, size_t input_offset, Array<double, 36>& output, MP3::BlockType block_type);
    static void synthesis(Array<double, 1024>& V, Array<double, 32>& samples, Array<double, 32>& result);
    static Span<MP3::Tables::ScaleFactorBand const> get_scalefactor_bands(MP3::Granule const&, int samplerate);

    AK::Vector<AK::Tuple<size_t, int>> m_seek_table;
    AK::Array<AK::Array<AK::Array<double, 18>, 32>, 2> m_last_values {};
    AK::Array<AK::Array<double, 1024>, 2> m_synthesis_buffer {};
    static LibDSP::MDCT<36> s_mdct_36;
    static LibDSP::MDCT<12> s_mdct_12;

    u32 m_sample_rate { 0 };
    u8 m_num_channels { 0 };
    PcmSampleFormat m_sample_format { PcmSampleFormat::Int16 };
    size_t m_file_size { 0 };
    int m_total_samples { 0 };
    size_t m_loaded_samples { 0 };
    bool m_is_first_frame { true };

    AK::Optional<MP3::MP3Frame> m_current_frame;
    u32 m_current_frame_read;
    RefPtr<Core::File> m_file;
    OwnPtr<Core::InputFileStream> m_input_stream;
    OwnPtr<InputBitStream> m_bitstream;
    DuplexMemoryStream m_bit_reservoir;
    Optional<LoaderError> m_error {};
};

}
