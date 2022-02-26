/*
 * Copyright (c) 2021, Arne Elster <arne@elster.li>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MP3Loader.h"
#include "MP3HuffmanTables.h"
#include "MP3Tables.h"
#include <LibCore/File.h>
#include <LibCore/FileStream.h>

namespace Audio {

LibDSP::MDCT<12> MP3LoaderPlugin::s_mdct_12;
LibDSP::MDCT<36> MP3LoaderPlugin::s_mdct_36;

MP3LoaderPlugin::MP3LoaderPlugin(StringView path)
    : m_file(Core::File::construct(path))
{
    if (!m_file->open(Core::OpenMode::ReadOnly)) {
        m_error = LoaderError { LoaderError::Category::IO, String::formatted("Can't open file: {}", m_file->error_string()) };
        return;
    }

    off_t file_size = 0;
    if (!m_file->seek(0, Core::SeekMode::FromEndPosition, &file_size)) {
        m_error = LoaderError { LoaderError::Category::IO, "Could not seek in file." };
        return;
    }
    m_file_size = file_size;
    if (!m_file->seek(0, Core::SeekMode::SetPosition)) {
        m_error = LoaderError { LoaderError::Category::IO, "Could not seek in file." };
        return;
    }

    m_input_stream = make<Core::InputFileStream>(*m_file);
    if (!m_input_stream || m_input_stream->has_any_error()) {
        m_error = LoaderError { LoaderError::Category::Internal, "Could not create input stream on file." };
        return;
    }

    m_bitstream = make<InputBitStream>(*m_input_stream);
    if (!m_bitstream || m_bitstream->has_any_error()) {
        m_error = LoaderError { LoaderError::Category::Internal, "Could not create bit stream on top of input stream" };
        return;
    }
}

MP3LoaderPlugin::~MP3LoaderPlugin()
{
    if (m_bitstream)
        m_bitstream->handle_any_error();

    if (m_input_stream)
        m_input_stream->handle_any_error();
}

MaybeLoaderError MP3LoaderPlugin::initialize()
{
    if (m_error.has_value())
        return m_error.release_value();

    TRY(synchronize());

    auto header = TRY(read_header());
    if (header.id != 1 || header.layer != 3)
        return LoaderError { LoaderError::Category::Format, "Only MPEG-1 layer 3 supported." };

    m_sample_rate = header.samplerate;
    m_num_channels = header.channel_count();
    m_loaded_samples = 0;

    if (!m_file->seek(0, Core::SeekMode::SetPosition))
        return LoaderError { LoaderError::Category::IO, "Could not seek in file." };

    TRY(build_seek_table());

    if (!m_file->seek(0, Core::SeekMode::SetPosition))
        return LoaderError { LoaderError::Category::IO, "Could not seek in file." };

    m_bitstream->handle_any_error();

    return {};
}

MaybeLoaderError MP3LoaderPlugin::reset()
{
    TRY(seek(0));
    m_current_frame = {};
    m_current_frame_read = 0;
    m_synthesis_buffer = {};
    m_loaded_samples = 0;
    m_bit_reservoir.discard_or_error(m_bit_reservoir.size());
    return {};
}

MaybeLoaderError MP3LoaderPlugin::seek(int const position)
{
    for (auto const& seek_entry : m_seek_table) {
        if (seek_entry.get<1>() >= position) {
            m_file->seek(seek_entry.get<0>(), Core::SeekMode::SetPosition);
            m_loaded_samples = seek_entry.get<1>();
            break;
        }
    }
    m_current_frame = {};
    m_current_frame_read = 0;
    m_synthesis_buffer = {};
    m_bit_reservoir.discard_or_error(m_bit_reservoir.size());
    m_input_stream->handle_any_error();
    m_bitstream->handle_any_error();
    m_bit_reservoir.handle_any_error();
    m_is_first_frame = true;
    return {};
}

LoaderSamples MP3LoaderPlugin::get_more_samples(size_t max_bytes_to_read_from_input)
{
    Vector<Sample> samples;

    size_t samples_to_read = max_bytes_to_read_from_input;
    samples.resize(samples_to_read);
    while (samples_to_read > 0) {
        if (!m_current_frame.has_value()) {
            auto maybe_frame = read_next_frame();
            if (maybe_frame.is_error()) {
                if (m_input_stream->unreliable_eof()) {
                    return Buffer::create_empty();
                }
                return maybe_frame.release_error();
            }
            m_current_frame = maybe_frame.release_value();
            if (!m_current_frame.has_value())
                break;
            m_is_first_frame = false;
            m_current_frame_read = 0;
        }

        bool const is_stereo = m_current_frame->header.channel_count() == 2;
        for (; m_current_frame_read < 576 && samples_to_read > 0; m_current_frame_read++) {
            auto const left_sample = m_current_frame->channels[0].granules[0].pcm[m_current_frame_read / 32][m_current_frame_read % 32];
            auto const right_sample = is_stereo ? m_current_frame->channels[1].granules[0].pcm[m_current_frame_read / 32][m_current_frame_read % 32] : left_sample;
            samples[samples.size() - samples_to_read] = Sample { left_sample, right_sample };
            samples_to_read--;
        }
        for (; m_current_frame_read < 1152 && samples_to_read > 0; m_current_frame_read++) {
            auto const left_sample = m_current_frame->channels[0].granules[1].pcm[(m_current_frame_read - 576) / 32][(m_current_frame_read - 576) % 32];
            auto const right_sample = is_stereo ? m_current_frame->channels[1].granules[1].pcm[(m_current_frame_read - 576) / 32][(m_current_frame_read - 576) % 32] : left_sample;
            samples[samples.size() - samples_to_read] = Sample { left_sample, right_sample };
            samples_to_read--;
        }
        if (m_current_frame_read == 1152) {
            m_current_frame = {};
        }
    }

    m_loaded_samples += samples.size();
    auto maybe_buffer = Buffer::create_with_samples(move(samples));
    if (maybe_buffer.is_error())
        return LoaderError { LoaderError::Category::Internal, m_loaded_samples, "Couldn't allocate sample buffer" };
    return maybe_buffer.release_value();
}

MaybeLoaderError MP3LoaderPlugin::build_seek_table()
{
    int sample_count = 0;
    size_t frame_count = 0;
    m_seek_table.clear();

    m_bitstream->align_to_byte_boundary();

    while (!synchronize().is_error()) {
        off_t frame_pos = 0;
        if (!m_file->seek(0, Core::SeekMode::FromCurrentPosition, &frame_pos))
            return LoaderError { LoaderError::Category::IO, String::formatted("Could not get stream position at frame {}.", frame_count) };
        frame_pos -= 2;

        auto error_or_header = read_header();
        if (error_or_header.is_error() || error_or_header.value().id != 1 || error_or_header.value().layer != 3) {
            continue;
        }
        frame_count++;
        sample_count += 1152;

        if (frame_count % 10 == 0)
            m_seek_table.append({ frame_pos, sample_count });

        size_t const next_frame_position = error_or_header.value().frame_size - 6;
        if (!m_file->seek(error_or_header.value().frame_size - 6, Core::SeekMode::FromCurrentPosition))
            return LoaderError { LoaderError::Category::IO, String::formatted("Could not seek to frame {} (stream position {}).", frame_count, next_frame_position) };

        // TODO: This is just here to clear the bitstream buffer.
        // Bitstream should have a method to sync its state to the underlying stream.
        m_bitstream->align_to_byte_boundary();
    }
    m_total_samples = sample_count;
    return {};
}

ErrorOr<MP3::Header, LoaderError> MP3LoaderPlugin::read_header()
{
    MP3::Header header;
    header.id = m_bitstream->read_bit_big_endian();
    header.layer = MP3::Tables::LayerNumberLookup[m_bitstream->read_bits_big_endian(2)];
    if (header.layer <= 0)
        return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Frame header contains invalid layer number." };
    header.protection_bit = m_bitstream->read_bit_big_endian();
    header.bitrate = MP3::Tables::BitratesPerLayerLookup[header.layer - 1][m_bitstream->read_bits_big_endian(4)];
    if (header.bitrate <= 0)
        return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Frame header contains invalid bitrate." };
    header.samplerate = MP3::Tables::SampleratesLookup[m_bitstream->read_bits_big_endian(2)];
    if (header.samplerate <= 0)
        return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Frame header contains invalid samplerate." };
    header.padding_bit = m_bitstream->read_bit_big_endian();
    header.private_bit = m_bitstream->read_bit_big_endian();
    header.mode = static_cast<MP3::Mode>(m_bitstream->read_bits_big_endian(2));
    header.mode_extension = static_cast<MP3::ModeExtension>(m_bitstream->read_bits_big_endian(2));
    header.copyright_bit = m_bitstream->read_bit_big_endian();
    header.original_bit = m_bitstream->read_bit_big_endian();
    header.emphasis = static_cast<MP3::Emphasis>(m_bitstream->read_bits_big_endian(2));
    if (!header.protection_bit)
        header.crc16 = static_cast<u16>(m_bitstream->read_bits_big_endian(16));
    header.frame_size = 144 * header.bitrate * 1000 / header.samplerate + header.padding_bit;
    header.slot_count = header.frame_size - ((header.channel_count() == 2 ? 32 : 17) + (header.protection_bit ? 0 : 2) + 4);
    return header;
}

MaybeLoaderError MP3LoaderPlugin::synchronize()
{
    size_t one_counter = 0;
    while (one_counter < 12 && !m_bitstream->has_any_error()) {
        bool const bit = m_bitstream->read_bit_big_endian();
        one_counter = bit ? one_counter + 1 : 0;
        if (!bit) {
            m_bitstream->align_to_byte_boundary();
        }
    }
    if (one_counter != 12)
        return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Failed to synchronize." };
    return {};
}

ErrorOr<MP3::MP3Frame, LoaderError> MP3LoaderPlugin::read_next_frame()
{
    while (!m_bitstream->has_any_error()) {
        TRY(synchronize());
        MP3::Header header = TRY(read_header());
        if (header.id != 1 || header.layer != 3) {
            continue;
        }

        return read_frame_data(header, m_is_first_frame);
    }
    return LoaderError { LoaderError::Category::Internal, m_loaded_samples, "Could not find another frame." };
}

ErrorOr<MP3::MP3Frame, LoaderError> MP3LoaderPlugin::read_frame_data(MP3::Header const& header, bool is_first_frame)
{
    MP3::MP3Frame frame { header };

    TRY(read_side_information(frame));
    if (m_bitstream->has_any_error())
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Read error" };

    auto maybe_buffer = ByteBuffer::create_uninitialized(header.slot_count);
    if (maybe_buffer.is_error())
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Out of memory" };
    auto& buffer = maybe_buffer.value();

    size_t old_reservoir_size = m_bit_reservoir.size();
    if (m_bitstream->read(buffer) != buffer.size())
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Could not read whole frame." };
    if (m_bit_reservoir.write(buffer) != header.slot_count)
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Could not write frame into bit reservoir." };

    if (frame.main_data_begin > 0 && is_first_frame)
        return frame;
    if (!m_bit_reservoir.discard_or_error(old_reservoir_size - frame.main_data_begin))
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Could not discard old frame data." };

    InputBitStream reservoir_stream(m_bit_reservoir);
    ScopeGuard reservoir_guard([&reservoir_stream]() {
        if (reservoir_stream.has_any_error()) {
            reservoir_stream.handle_any_error();
        }
    });

    for (size_t granule_index = 0; granule_index < 2; granule_index++) {
        for (size_t channel_index = 0; channel_index < header.channel_count(); channel_index++) {
            size_t scale_factor_size = TRY(read_scale_factors(frame, reservoir_stream, granule_index, channel_index));
            TRY(read_huffman_data(frame, reservoir_stream, granule_index, channel_index, scale_factor_size));
            if (frame.channels[channel_index].granules[granule_index].block_type == MP3::BlockType::Short) {
                reorder_samples(frame.channels[channel_index].granules[granule_index], frame.header.samplerate);

                // Only reduce alias for lowest 2 bands as they're long.
                // Afaik this is not mentioned in the ISO spec, but it is addressed in the
                // changelog for the ISO compliance tests.
                if (frame.channels[channel_index].granules[granule_index].mixed_block_flag)
                    reduce_alias(frame.channels[channel_index].granules[granule_index], 36);
            } else {
                reduce_alias(frame.channels[channel_index].granules[granule_index]);
            }
        }

        if (header.mode == MP3::Mode::JointStereo) {
            process_stereo(frame, granule_index);
        }
    }

    for (size_t granule_index = 0; granule_index < 2; granule_index++) {
        for (size_t channel_index = 0; channel_index < header.channel_count(); channel_index++) {
            auto& granule = frame.channels[channel_index].granules[granule_index];

            for (size_t i = 0; i < 576; i += 18) {
                MP3::BlockType block_type = granule.block_type;
                if (i < 36 && granule.mixed_block_flag) {
                    // ISO/IEC 11172-3: if mixed_block_flag is set, the lowest two subbands are transformed with normal window.
                    block_type = MP3::BlockType::Normal;
                }

                Array<double, 36> output;
                transform_samples_to_time(granule.samples, i, output, block_type);

                int const subband_index = i / 18;
                for (size_t sample_index = 0; sample_index < 18; sample_index++) {
                    // overlap add
                    granule.filter_bank_input[subband_index][sample_index] = output[sample_index] + m_last_values[channel_index][subband_index][sample_index];
                    m_last_values[channel_index][subband_index][sample_index] = output[sample_index + 18];

                    // frequency inversion
                    if (subband_index % 2 == 1 && sample_index % 2 == 1)
                        granule.filter_bank_input[subband_index][sample_index] *= -1;
                }
            }
        }
    }

    Array<double, 32> in_samples;
    for (size_t channel_index = 0; channel_index < frame.header.channel_count(); channel_index++) {
        for (size_t granule_index = 0; granule_index < 2; granule_index++) {
            auto& granule = frame.channels[channel_index].granules[granule_index];
            for (size_t sample_index = 0; sample_index < 18; sample_index++) {
                for (size_t band_index = 0; band_index < 32; band_index++) {
                    in_samples[band_index] = granule.filter_bank_input[band_index][sample_index];
                }
                synthesis(m_synthesis_buffer[channel_index], in_samples, granule.pcm[sample_index]);
            }
        }
    }

    return frame;
}

MaybeLoaderError MP3LoaderPlugin::read_side_information(MP3::MP3Frame& frame)
{
    frame.main_data_begin = m_bitstream->read_bits_big_endian(9);

    if (frame.header.channel_count() == 1) {
        frame.private_bits = m_bitstream->read_bits_big_endian(5);
    } else {
        frame.private_bits = m_bitstream->read_bits_big_endian(3);
    }

    for (size_t channel_index = 0; channel_index < frame.header.channel_count(); channel_index++) {
        for (size_t scale_factor_selection_info_band = 0; scale_factor_selection_info_band < 4; scale_factor_selection_info_band++) {
            frame.channels[channel_index].scale_factor_selection_info[scale_factor_selection_info_band] = m_bitstream->read_bit_big_endian();
        }
    }

    for (size_t granule_index = 0; granule_index < 2; granule_index++) {
        for (size_t channel_index = 0; channel_index < frame.header.channel_count(); channel_index++) {
            auto& granule = frame.channels[channel_index].granules[granule_index];
            granule.part_2_3_length = m_bitstream->read_bits_big_endian(12);
            granule.big_values = m_bitstream->read_bits_big_endian(9);
            granule.global_gain = m_bitstream->read_bits_big_endian(8);
            granule.scalefac_compress = m_bitstream->read_bits_big_endian(4);
            granule.window_switching_flag = m_bitstream->read_bit_big_endian();
            if (granule.window_switching_flag) {
                granule.block_type = static_cast<MP3::BlockType>(m_bitstream->read_bits_big_endian(2));
                granule.mixed_block_flag = m_bitstream->read_bit_big_endian();
                for (size_t region = 0; region < 2; region++)
                    granule.table_select[region] = m_bitstream->read_bits_big_endian(5);
                for (size_t window = 0; window < 3; window++)
                    granule.sub_block_gain[window] = m_bitstream->read_bits_big_endian(3);
                granule.region0_count = (granule.block_type == MP3::BlockType::Short && !granule.mixed_block_flag) ? 8 : 7;
                granule.region1_count = 36;
            } else {
                for (size_t region = 0; region < 3; region++)
                    granule.table_select[region] = m_bitstream->read_bits_big_endian(5);
                granule.region0_count = m_bitstream->read_bits_big_endian(4);
                granule.region1_count = m_bitstream->read_bits_big_endian(3);
            }
            granule.preflag = m_bitstream->read_bit_big_endian();
            granule.scalefac_scale = m_bitstream->read_bit_big_endian();
            granule.count1table_select = m_bitstream->read_bit_big_endian();
        }
    }
    if (m_bitstream->has_any_error())
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Read error" };
    return {};
}

// From ISO/IEC 11172-3 (2.4.3.4.7.1)
Array<double, 576> MP3LoaderPlugin::calculate_frame_exponents(MP3::MP3Frame const& frame, size_t granule_index, size_t channel_index)
{
    Array<double, 576> exponents;

    auto fill_band = [&exponents](double exponent, size_t start, size_t end) {
        for (size_t j = start; j <= end; j++) {
            exponents[j] = exponent;
        }
    };

    auto const& channel = frame.channels[channel_index];
    auto const& granule = frame.channels[channel_index].granules[granule_index];

    auto const scale_factor_bands = get_scalefactor_bands(granule, frame.header.samplerate);
    double const scale_factor_multiplier = granule.scalefac_scale ? 1 : 0.5;
    int const gain = granule.global_gain - 210;

    if (granule.block_type != MP3::BlockType::Short) {
        for (size_t band_index = 0; band_index < 22; band_index++) {
            double const exponent = gain / 4.0 - (scale_factor_multiplier * (channel.scale_factors[band_index] + granule.preflag * MP3::Tables::Pretab[band_index]));
            fill_band(AK::pow(2.0, exponent), scale_factor_bands[band_index].start, scale_factor_bands[band_index].end);
        }
    } else {
        size_t band_index = 0;
        size_t sample_count = 0;

        if (granule.mixed_block_flag) {
            while (sample_count < 36) {
                double const exponent = gain / 4.0 - (scale_factor_multiplier * (channel.scale_factors[band_index] + granule.preflag * MP3::Tables::Pretab[band_index]));
                fill_band(AK::pow(2.0, exponent), scale_factor_bands[band_index].start, scale_factor_bands[band_index].end);
                sample_count += scale_factor_bands[band_index].width;
                band_index++;
            }
        }

        double const gain0 = (gain - 8 * granule.sub_block_gain[0]) / 4.0;
        double const gain1 = (gain - 8 * granule.sub_block_gain[1]) / 4.0;
        double const gain2 = (gain - 8 * granule.sub_block_gain[2]) / 4.0;

        while (sample_count < 576 && band_index < scale_factor_bands.size()) {
            double const exponent0 = gain0 - (scale_factor_multiplier * channel.scale_factors[band_index + 0]);
            double const exponent1 = gain1 - (scale_factor_multiplier * channel.scale_factors[band_index + 1]);
            double const exponent2 = gain2 - (scale_factor_multiplier * channel.scale_factors[band_index + 2]);

            fill_band(AK::pow(2.0, exponent0), scale_factor_bands[band_index + 0].start, scale_factor_bands[band_index + 0].end);
            sample_count += scale_factor_bands[band_index + 0].width;
            fill_band(AK::pow(2.0, exponent1), scale_factor_bands[band_index + 1].start, scale_factor_bands[band_index + 1].end);
            sample_count += scale_factor_bands[band_index + 1].width;
            fill_band(AK::pow(2.0, exponent2), scale_factor_bands[band_index + 2].start, scale_factor_bands[band_index + 2].end);
            sample_count += scale_factor_bands[band_index + 2].width;

            band_index += 3;
        }

        while (sample_count < 576)
            exponents[sample_count++] = 0;
    }
    return exponents;
}

ErrorOr<size_t, LoaderError> MP3LoaderPlugin::read_scale_factors(MP3::MP3Frame& frame, InputBitStream& reservoir, size_t granule_index, size_t channel_index)
{
    auto& channel = frame.channels[channel_index];
    auto const& granule = channel.granules[granule_index];
    size_t band_index = 0;
    size_t bits_read = 0;

    if (granule.window_switching_flag && granule.block_type == MP3::BlockType::Short) {
        if (granule.mixed_block_flag) {
            for (size_t i = 0; i < 8; i++) {
                auto const bits = MP3::Tables::ScalefacCompressSlen1[granule.scalefac_compress];
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                bits_read += bits;
            }
            for (size_t i = 3; i < 12; i++) {
                auto const bits = i <= 5 ? MP3::Tables::ScalefacCompressSlen1[granule.scalefac_compress] : MP3::Tables::ScalefacCompressSlen2[granule.scalefac_compress];
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                bits_read += 3 * bits;
            }
        } else {
            for (size_t i = 0; i < 12; i++) {
                auto const bits = i <= 5 ? MP3::Tables::ScalefacCompressSlen1[granule.scalefac_compress] : MP3::Tables::ScalefacCompressSlen2[granule.scalefac_compress];
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                channel.scale_factors[band_index++] = reservoir.read_bits_big_endian(bits);
                bits_read += 3 * bits;
            }
        }
        channel.scale_factors[band_index++] = 0;
        channel.scale_factors[band_index++] = 0;
        channel.scale_factors[band_index++] = 0;
    } else {
        if ((channel.scale_factor_selection_info[0] == 0) || (granule_index == 0)) {
            for (band_index = 0; band_index < 6; band_index++) {
                auto const bits = MP3::Tables::ScalefacCompressSlen1[granule.scalefac_compress];
                channel.scale_factors[band_index] = reservoir.read_bits_big_endian(bits);
                bits_read += bits;
            }
        }
        if ((channel.scale_factor_selection_info[1] == 0) || (granule_index == 0)) {
            for (band_index = 6; band_index < 11; band_index++) {
                auto const bits = MP3::Tables::ScalefacCompressSlen1[granule.scalefac_compress];
                channel.scale_factors[band_index] = reservoir.read_bits_big_endian(bits);
                bits_read += bits;
            }
        }
        if ((channel.scale_factor_selection_info[2] == 0) || (granule_index == 0)) {
            for (band_index = 11; band_index < 16; band_index++) {
                auto const bits = MP3::Tables::ScalefacCompressSlen2[granule.scalefac_compress];
                channel.scale_factors[band_index] = reservoir.read_bits_big_endian(bits);
                bits_read += bits;
            }
        }
        if ((channel.scale_factor_selection_info[3] == 0) || (granule_index == 0)) {
            for (band_index = 16; band_index < 21; band_index++) {
                auto const bits = MP3::Tables::ScalefacCompressSlen2[granule.scalefac_compress];
                channel.scale_factors[band_index] = reservoir.read_bits_big_endian(bits);
                bits_read += bits;
            }
        }
        channel.scale_factors[21] = 0;
    }

    if (reservoir.has_any_error())
        return LoaderError { LoaderError::Category::IO, m_loaded_samples, "Read error" };
    return bits_read;
}

MaybeLoaderError MP3LoaderPlugin::read_huffman_data(MP3::MP3Frame& frame, InputBitStream& reservoir, size_t granule_index, size_t channel_index, size_t granule_bits_read)
{
    auto const exponents = calculate_frame_exponents(frame, granule_index, channel_index);
    auto& granule = frame.channels[channel_index].granules[granule_index];

    auto const scale_factor_bands = get_scalefactor_bands(granule, frame.header.samplerate);
    size_t const scale_factor_band_index1 = granule.region0_count + 1;
    size_t const scale_factor_band_index2 = min(scale_factor_bands.size() - 1, scale_factor_band_index1 + granule.region1_count + 1);

    bool const is_short_granule = granule.window_switching_flag && granule.block_type == MP3::BlockType::Short;
    size_t const region1_start = is_short_granule ? 36 : scale_factor_bands[scale_factor_band_index1].start;
    size_t const region2_start = is_short_granule ? 576 : scale_factor_bands[scale_factor_band_index2].start;

    auto requantize = [](int const sample, double const exponent) -> double {
        int const sign = sample < 0 ? -1 : 1;
        int const magnitude = AK::abs(sample);
        return sign * AK::pow(static_cast<double>(magnitude), 4 / 3.0) * exponent;
    };

    size_t count = 0;

    for (; count < granule.big_values * 2; count += 2) {
        MP3::Tables::Huffman::HuffmanTreeXY const* tree = nullptr;

        if (count < region1_start) {
            tree = &MP3::Tables::Huffman::HuffmanTreesXY[granule.table_select[0]];
        } else if (count < region2_start) {
            tree = &MP3::Tables::Huffman::HuffmanTreesXY[granule.table_select[1]];
        } else {
            tree = &MP3::Tables::Huffman::HuffmanTreesXY[granule.table_select[2]];
        }

        if (!tree || tree->nodes.is_empty()) {
            return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Frame references invalid huffman table." };
        }

        // Assumption: There's enough bits to read. 32 is just a placeholder for "unlimited".
        // There are no 32 bit long huffman codes in the tables.
        auto const entry = MP3::Tables::Huffman::huffman_decode(reservoir, tree->nodes, 32);
        granule_bits_read += entry.bits_read;
        if (!entry.code.has_value())
            return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Frame contains invalid huffman data." };
        int x = entry.code->symbol.x;
        int y = entry.code->symbol.y;

        if (x == 15 && tree->linbits > 0) {
            x += reservoir.read_bits_big_endian(tree->linbits);
            granule_bits_read += tree->linbits;
        }
        if (x != 0) {
            if (reservoir.read_bit_big_endian())
                x = -x;
            granule_bits_read++;
        }

        if (y == 15 && tree->linbits > 0) {
            y += reservoir.read_bits_big_endian(tree->linbits);
            granule_bits_read += tree->linbits;
        }
        if (y != 0) {
            if (reservoir.read_bit_big_endian())
                y = -y;
            granule_bits_read++;
        }

        granule.samples[count + 0] = requantize(x, exponents[count + 0]);
        granule.samples[count + 1] = requantize(y, exponents[count + 1]);
    }

    Span<MP3::Tables::Huffman::HuffmanNode<MP3::Tables::Huffman::HuffmanVWXY> const> count1table = granule.count1table_select ? MP3::Tables::Huffman::TreeB : MP3::Tables::Huffman::TreeA;

    // count1 is not known. We have to read huffman encoded values
    // until we've exhausted the granule's bits. We know the size of
    // the granule from part2_3_length, which is the number of bits
    // used for scaleactors and huffman data (in the granule).
    while (granule_bits_read < granule.part_2_3_length && count <= 576 - 4) {
        auto const entry = MP3::Tables::Huffman::huffman_decode(reservoir, count1table, granule.part_2_3_length - granule_bits_read);
        granule_bits_read += entry.bits_read;
        if (!entry.code.has_value())
            return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Frame contains invalid huffman data." };
        int v = entry.code->symbol.v;
        if (v != 0) {
            if (granule_bits_read >= granule.part_2_3_length)
                break;
            if (reservoir.read_bit_big_endian())
                v = -v;
            granule_bits_read++;
        }
        int w = entry.code->symbol.w;
        if (w != 0) {
            if (granule_bits_read >= granule.part_2_3_length)
                break;
            if (reservoir.read_bit_big_endian())
                w = -w;
            granule_bits_read++;
        }
        int x = entry.code->symbol.x;
        if (x != 0) {
            if (granule_bits_read >= granule.part_2_3_length)
                break;
            if (reservoir.read_bit_big_endian())
                x = -x;
            granule_bits_read++;
        }
        int y = entry.code->symbol.y;
        if (y != 0) {
            if (granule_bits_read >= granule.part_2_3_length)
                break;
            if (reservoir.read_bit_big_endian())
                y = -y;
            granule_bits_read++;
        }

        granule.samples[count + 0] = requantize(v, exponents[count + 0]);
        granule.samples[count + 1] = requantize(w, exponents[count + 1]);
        granule.samples[count + 2] = requantize(x, exponents[count + 2]);
        granule.samples[count + 3] = requantize(y, exponents[count + 3]);

        count += 4;
    }

    if (granule_bits_read > granule.part_2_3_length) {
        return LoaderError { LoaderError::Category::Format, m_loaded_samples, "Read too many bits from bit reservoir." };
    }

    for (size_t i = granule_bits_read; i < granule.part_2_3_length; i++) {
        reservoir.read_bit_big_endian();
    }

    return {};
}

void MP3LoaderPlugin::reorder_samples(MP3::Granule& granule, u32 sample_rate)
{
    double tmp[576] = {};
    size_t band_index = 0;
    size_t subband_index = 0;

    auto scale_factor_bands = get_scalefactor_bands(granule, sample_rate);

    if (granule.mixed_block_flag) {
        while (subband_index < 36) {
            for (size_t frequency_line_index = 0; frequency_line_index < scale_factor_bands[band_index].width; frequency_line_index++) {
                tmp[subband_index] = granule.samples[subband_index];
                subband_index++;
            }
            band_index++;
        }
    }

    while (subband_index < 576 && band_index <= 36) {
        for (size_t frequency_line_index = 0; frequency_line_index < scale_factor_bands[band_index].width; frequency_line_index++) {
            tmp[subband_index++] = granule.samples[scale_factor_bands[band_index + 0].start + frequency_line_index];
            tmp[subband_index++] = granule.samples[scale_factor_bands[band_index + 1].start + frequency_line_index];
            tmp[subband_index++] = granule.samples[scale_factor_bands[band_index + 2].start + frequency_line_index];
        }
        band_index += 3;
    }

    for (size_t i = 0; i < 576; i++)
        granule.samples[i] = tmp[i];
}

void MP3LoaderPlugin::reduce_alias(MP3::Granule& granule, size_t max_subband_index)
{
    for (size_t subband = 0; subband < max_subband_index - 18; subband += 18) {
        for (size_t i = 0; i < 8; i++) {
            size_t const idx1 = subband + 17 - i;
            size_t const idx2 = subband + 18 + i;
            auto const d1 = granule.samples[idx1];
            auto const d2 = granule.samples[idx2];
            granule.samples[idx1] = d1 * MP3::Tables::AliasReductionCs[i] - d2 * MP3::Tables::AliasReductionCa[i];
            granule.samples[idx2] = d2 * MP3::Tables::AliasReductionCs[i] + d1 * MP3::Tables::AliasReductionCa[i];
        }
    }
}

void MP3LoaderPlugin::process_stereo(MP3::MP3Frame& frame, size_t granule_index)
{
    size_t band_index_ms_start = 0;
    size_t band_index_ms_end = 0;
    size_t band_index_intensity_start = 0;
    size_t band_index_intensity_end = 0;
    auto& granule_left = frame.channels[0].granules[granule_index];
    auto& granule_right = frame.channels[1].granules[granule_index];

    auto get_last_nonempty_band = [](Span<double> samples, Span<MP3::Tables::ScaleFactorBand const> bands) -> size_t {
        size_t last_nonempty_band = 0;

        for (size_t i = 0; i < bands.size(); i++) {
            bool is_empty = true;
            for (size_t l = bands[i].start; l < bands[i].end; l++) {
                if (samples[l] != 0) {
                    is_empty = false;
                    break;
                }
            }
            if (!is_empty)
                last_nonempty_band = i;
        }

        return last_nonempty_band;
    };

    auto process_ms_stereo = [&](MP3::Tables::ScaleFactorBand const& band) {
        double const SQRT_2 = AK::sqrt(2.0);
        for (size_t i = band.start; i <= band.end; i++) {
            double const m = granule_left.samples[i];
            double const s = granule_right.samples[i];
            granule_left.samples[i] = (m + s) / SQRT_2;
            granule_right.samples[i] = (m - s) / SQRT_2;
        }
    };

    auto process_intensity_stereo = [&](MP3::Tables::ScaleFactorBand const& band, double intensity_stereo_ratio) {
        for (size_t i = band.start; i <= band.end; i++) {
            double const sample_left = granule_left.samples[i];
            double const coeff_l = intensity_stereo_ratio / (1 + intensity_stereo_ratio);
            double const coeff_r = 1 / (1 + intensity_stereo_ratio);
            granule_left.samples[i] = sample_left * coeff_l;
            granule_right.samples[i] = sample_left * coeff_r;
        }
    };

    auto scale_factor_bands = get_scalefactor_bands(granule_right, frame.header.samplerate);

    if (has_flag(frame.header.mode_extension, MP3::ModeExtension::MsStereo)) {
        band_index_ms_start = 0;
        band_index_ms_end = scale_factor_bands.size();
    }

    if (has_flag(frame.header.mode_extension, MP3::ModeExtension::IntensityStereo)) {
        band_index_intensity_start = get_last_nonempty_band(granule_right.samples, scale_factor_bands);
        band_index_intensity_end = scale_factor_bands.size();
        band_index_ms_end = band_index_intensity_start;
    }

    for (size_t band_index = band_index_ms_start; band_index < band_index_ms_end; band_index++) {
        process_ms_stereo(scale_factor_bands[band_index]);
    }

    for (size_t band_index = band_index_intensity_start; band_index < band_index_intensity_end; band_index++) {
        auto const intensity_stereo_position = frame.channels[1].scale_factors[band_index];
        if (intensity_stereo_position == 7) {
            if (has_flag(frame.header.mode_extension, MP3::ModeExtension::MsStereo))
                process_ms_stereo(scale_factor_bands[band_index]);
            continue;
        }
        double const intensity_stereo_ratio = AK::tan(intensity_stereo_position * AK::Pi<double> / 12);
        process_intensity_stereo(scale_factor_bands[band_index], intensity_stereo_ratio);
    }
}

void MP3LoaderPlugin::transform_samples_to_time(Array<double, 576> const& input, size_t input_offset, Array<double, 36>& output, MP3::BlockType block_type)
{
    if (block_type == MP3::BlockType::Short) {
        size_t const N = 12;
        Array<double, N * 3> temp_out;
        Array<double, N / 2> temp_in;

        for (size_t k = 0; k < N / 2; k++)
            temp_in[k] = input[input_offset + 3 * k + 0];
        s_mdct_12.transform(temp_in, Span<double>(temp_out).slice(0, N));
        for (size_t i = 0; i < N; i++)
            temp_out[i + 0] *= MP3::Tables::WindowBlockTypeShort[i];

        for (size_t k = 0; k < N / 2; k++)
            temp_in[k] = input[input_offset + 3 * k + 1];
        s_mdct_12.transform(temp_in, Span<double>(temp_out).slice(12, N));
        for (size_t i = 0; i < N; i++)
            temp_out[i + 12] *= MP3::Tables::WindowBlockTypeShort[i];

        for (size_t k = 0; k < N / 2; k++)
            temp_in[k] = input[input_offset + 3 * k + 2];
        s_mdct_12.transform(temp_in, Span<double>(temp_out).slice(24, N));
        for (size_t i = 0; i < N; i++)
            temp_out[i + 24] *= MP3::Tables::WindowBlockTypeShort[i];

        Span<double> idmct1 = Span<double>(temp_out).slice(0, 12);
        Span<double> idmct2 = Span<double>(temp_out).slice(12, 12);
        Span<double> idmct3 = Span<double>(temp_out).slice(24, 12);
        for (size_t i = 0; i < 6; i++)
            output[i] = 0;
        for (size_t i = 6; i < 12; i++)
            output[i] = idmct1[i - 6];
        for (size_t i = 12; i < 18; i++)
            output[i] = idmct1[i - 6] + idmct2[i - 12];
        for (size_t i = 18; i < 24; i++)
            output[i] = idmct2[i - 12] + idmct3[i - 18];
        for (size_t i = 24; i < 30; i++)
            output[i] = idmct3[i - 18];
        for (size_t i = 30; i < 36; i++)
            output[i] = 0;

    } else {
        s_mdct_36.transform(Span<double const>(input).slice(input_offset, 18), output);
        for (size_t i = 0; i < 36; i++) {
            switch (block_type) {
            case MP3::BlockType::Normal:
                output[i] *= MP3::Tables::WindowBlockTypeNormal[i];
                break;
            case MP3::BlockType::Start:
                output[i] *= MP3::Tables::WindowBlockTypeStart[i];
                break;
            case MP3::BlockType::End:
                output[i] *= MP3::Tables::WindowBlockTypeEnd[i];
                break;
            case MP3::BlockType::Short:
                VERIFY_NOT_REACHED();
                break;
            }
        }
    }
}

// ISO/IEC 11172-3 (Figure A.2)
void MP3LoaderPlugin::synthesis(Array<double, 1024>& V, Array<double, 32>& samples, Array<double, 32>& result)
{
    for (size_t i = 1023; i >= 64; i--) {
        V[i] = V[i - 64];
    }

    for (size_t i = 0; i < 64; i++) {
        V[i] = 0;
        for (size_t k = 0; k < 32; k++) {
            double const N = MP3::Tables::SynthesisSubbandFilterCoefficients[i][k];
            V[i] += N * samples[k];
        }
    }

    Array<double, 512> U;
    for (size_t i = 0; i < 8; i++) {
        for (size_t j = 0; j < 32; j++) {
            U[i * 64 + j] = V[i * 128 + j];
            U[i * 64 + 32 + j] = V[i * 128 + 96 + j];
        }
    }

    Array<double, 512> W;
    for (size_t i = 0; i < 512; i++) {
        W[i] = U[i] * MP3::Tables::WindowSynthesis[i];
    }

    for (size_t j = 0; j < 32; j++) {
        result[j] = 0;
        for (size_t k = 0; k < 16; k++) {
            result[j] += W[j + 32 * k];
        }
    }
}

Span<MP3::Tables::ScaleFactorBand const> MP3LoaderPlugin::get_scalefactor_bands(MP3::Granule const& granule, int samplerate)
{
    switch (granule.block_type) {
    case MP3::BlockType::Short:
        switch (samplerate) {
        case 32000:
            return granule.mixed_block_flag ? MP3::Tables::ScaleFactorBandMixed32000 : MP3::Tables::ScaleFactorBandShort32000;
        case 44100:
            return granule.mixed_block_flag ? MP3::Tables::ScaleFactorBandMixed44100 : MP3::Tables::ScaleFactorBandShort44100;
        case 48000:
            return granule.mixed_block_flag ? MP3::Tables::ScaleFactorBandMixed48000 : MP3::Tables::ScaleFactorBandShort48000;
        }
        break;
    case MP3::BlockType::Normal:
        [[fallthrough]];
    case MP3::BlockType::Start:
        [[fallthrough]];
    case MP3::BlockType::End:
        switch (samplerate) {
        case 32000:
            return MP3::Tables::ScaleFactorBandLong32000;
        case 44100:
            return MP3::Tables::ScaleFactorBandLong44100;
        case 48000:
            return MP3::Tables::ScaleFactorBandLong48000;
        }
    }
    VERIFY_NOT_REACHED();
}

}
