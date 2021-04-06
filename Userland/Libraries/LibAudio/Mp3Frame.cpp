/*
 * Copyright (c) 2021, János Tóth <toth-janos@outlook.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibAudio/Mp3DecodingTables.h>
#include <LibAudio/Mp3Frame.h>
#include <LibAudio/Mp3Loader.h>
#include <math.h>

namespace Audio::Mp3 {

#define CHECK_OK_WITHOUT_MESSAGE() \
    if (!ok) {                     \
        return;                    \
    }

#define CHECK_OK(msg)         \
    if (!ok) {                \
        m_error_string = msg; \
        dbgln(msg);           \
        return;               \
    }

#define ERROR(msg)            \
    do {                      \
        ok = false;           \
        m_error_string = msg; \
        dbgln(msg);           \
        return;               \
    } while (0)

Frame::Header::Header(Mp3LoaderPlugin* loader)
    : m_loader(loader)
{
    bool ok;
    m_valid = false;

    u8 first_byte = m_loader->read_byte(ok);
    CHECK_OK("Unable to read the fist byte.");
    if (first_byte != 0xff) {
        dbgln("Invalid MP3 frame header first byte: {:02x}", first_byte);
        return;
    }

    u8 second_byte = m_loader->read_byte(ok);
    CHECK_OK("Unable to read the second byte.");
    if (second_byte < 0x0e) {
        dbgln("Invalid MP3 frame header second byte: {:02x}", second_byte);
        return;
    }

    m_valid = true;

    set_version(second_byte, ok);
    CHECK_OK_WITHOUT_MESSAGE();
    set_layer(second_byte, ok);
    CHECK_OK_WITHOUT_MESSAGE();
    set_crc(second_byte);

    u8 third_byte = m_loader->read_byte(ok);
    CHECK_OK("Unable to read the third byte.");
    set_bit_rate(third_byte, ok);
    CHECK_OK_WITHOUT_MESSAGE();
    set_sample_rate(third_byte, ok);
    CHECK_OK_WITHOUT_MESSAGE();
    set_padding(third_byte);

    u8 forth_byte = m_loader->read_byte(ok);
    CHECK_OK("Unable to read the forth byte.");
    set_channel_mode(forth_byte);
    set_mode_extension(forth_byte);
    set_emphasis(forth_byte);

    set_frame_size_and_duration();
}

ALWAYS_INLINE void Frame::Header::set_version(u8 second_byte, bool& ok)
{
    u8 version = (second_byte & 0b00011000) >> 3;
    ok = true;

    switch (version) {
    case 0:
        m_version = "2.5";
        break;
    case 1:
        m_version = "reserved";
        break;
    case 2:
        m_version = "2";
        break;
    case 3:
        m_version = "1";
        break;
    default:
        ERROR(String::format("Unknown MP3 frame header version: %02x", version));
    }

    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header audio version={:02x}={}", version, m_version);
}

ALWAYS_INLINE void Frame::Header::set_layer(u8 second_byte, bool& ok)
{
    ok = true;
    m_layer = (Layer)((second_byte & 0b00000110) >> 1);
    if (m_layer == Layer::Reserved) {
        ERROR(String::format("MP3 layer is reserved."));
    }

    Vector<String> layers = { "Reserved", "Layer III", "Layer II", "Layer I" };
    // FIXME: Support other layers.
    if (m_layer != Layer::Layer_III) {
        ERROR(String::format("Unsupported layer: %s", layers[(u8)m_layer].characters()));
    }

    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header layer description={}={}", (u8)m_layer, layers[(u8)m_layer]);
}

ALWAYS_INLINE void Frame::Header::set_crc(u8 second_byte)
{
    m_crc = second_byte & 0x01;
    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header CRC={}", m_crc);
}

ALWAYS_INLINE void Frame::Header::set_bit_rate(u8 third_byte, bool& ok)
{
    u8 index = (third_byte) >> 4;
    ok = true;

    if (index == 0) {
        ERROR("Free bitrate is not supported.");
    }

    if (index == 0b1111) {
        ERROR("Invalid bitrate value in MP3 frame header.");
    }

    if (m_version == "1") {
        if (m_layer == Layer::Layer_I) {
            m_bit_rate = index * 32;
        } else if (m_layer == Layer::Layer_II) {
            Vector<u32> rates = { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384 };
            m_bit_rate = rates[index] * 1000;
        } else if (m_layer == Layer::Layer_III) {
            Vector<u32> rates = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320 };
            m_bit_rate = rates[index] * 1000;
        } else {
            ERROR(String::format("Invalid layer: %u", (u8)m_layer));
        }
    } else {
        if (m_layer == Layer::Layer_I) {
            Vector<u32> rates = { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256 };
            m_bit_rate = rates[index] * 1000;
        } else if (m_layer == Layer::Layer_II || m_layer == Layer::Layer_III) {
            Vector<u32> rates = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160 };
            m_bit_rate = rates[index] * 1000;
        } else {
            ERROR(String::format("Invalid layer: %u", (u8)m_layer));
        }
    }

    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header bitrate={}={} bps", index, m_bit_rate);
}

ALWAYS_INLINE void Frame::Header::set_sample_rate(u8 third_byte, bool& ok)
{
    u8 index = (third_byte & 0x0F) >> 2;
    ok = true;

    if (index == 3) {
        ERROR(String::format("Invalid samplig rate: %u", index));
    }

    if (m_version == "1") {
        Vector<u32> rates = { 44'100, 48'000, 32'000 };
        m_sample_rate = rates[index];
    } else if (m_version == "2") {
        Vector<u32> rates = { 22'050, 24'000, 16'000 };
        m_sample_rate = rates[index];
    } else if (m_version == "2.5") {
        Vector<u32> rates = { 11'025, 12'000, 8'000 };
        m_sample_rate = rates[index];
    } else {
        ERROR("Invalid version.");
    }

    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header samplig rate={}={} Hz", index, m_sample_rate);
}

ALWAYS_INLINE void Frame::Header::set_padding(u8 third_byte)
{
    m_padding = third_byte & 0x02;
    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header padding={}", m_padding);
}

ALWAYS_INLINE void Frame::Header::set_channel_mode(u8 forth_byte)
{
    m_channel_mode = (ChannelMode)((forth_byte & 0b11000000) >> 6);
    m_num_channels = m_channel_mode == ChannelMode::Mono ? 1 : 2;

#if MP3_SHOW_HEADER
    Vector<String> modes = { "Stereo", "Joint stereo", "Dual channel", "Single channel" };
    dbgln("MP3 frame header channel mode={}={}", (u8)m_channel_mode, modes[(u8)m_channel_mode]);
#endif
}

ALWAYS_INLINE void Frame::Header::set_mode_extension(u8 forth_byte)
{
    m_mode_extension = (forth_byte & 0b00110000) >> 4;
    dbgln_if(MP3_SHOW_HEADER, "MP3 frame header mode_extension={}", m_mode_extension);
}

ALWAYS_INLINE void Frame::Header::set_emphasis(u8 forth_byte)
{
    m_emphasis = forth_byte & 0b00000011;
#if MP3_SHOW_HEADER
    Vector<String> emphasises = { "none", "50/15 ms", "reserved", "CCIT J.17" };
    dbgln("MP3 frame header emphasis={}={}", m_emphasis, emphasises[m_emphasis]);
#endif
}

ALWAYS_INLINE void Frame::Header::set_frame_size_and_duration()
{
    switch (m_layer) {
    case Layer::Layer_III:
        if (m_version == "1") {
            m_samples_per_frame = 1152;
        } else {
            m_samples_per_frame = 576;
        }
        break;
    case Layer::Layer_II:
        m_samples_per_frame = 1152;
        break;
    case Layer::Layer_I:
        m_samples_per_frame = 384;
        break;
    case Layer::Reserved:
        VERIFY_NOT_REACHED();
    }

    m_frame_size = (m_samples_per_frame / 8 * m_bit_rate / m_sample_rate) + (u8)m_padding;
    m_duration = (float)m_samples_per_frame / (float)m_sample_rate;

    dbgln_if(MP3_SHOW_HEADER, "MP3 frame size={}, duration={} s", m_frame_size, m_duration);
}

Frame::Data::Data(Mp3LoaderPlugin* loader, RefPtr<Header>& header)
    : m_loader(loader)
    , m_header(header)
{
    bool ok;
    m_valid = false;

    if (m_header->has_error()) {
        m_error_string = m_header->error_string();
        return;
    }

    if (m_header->is_valid() == false) {
        return;
    }

    m_valid = true;

    size_t size = m_header->frame_size() - m_header->header_size();
    m_data = ByteBuffer::create_uninitialized(size);
    ok = m_data.size() == size;
    CHECK_OK("Unable to create buffer for the MP3 frame data.");

    m_loader->read(m_data.bytes(), ok);
    CHECK_OK("Unable to read the MP3 frame data.");

    switch (m_header->layer()) {
    case Layer::Layer_I:
        ERROR("Layer I is not supported.");
    case Layer::Layer_II:
        ERROR("Layer II is not supported.");
    case Layer::Layer_III:
        decode_layer_III(ok);
        break;
    default:
        ERROR("Unknown layer.");
    }
}

ALWAYS_INLINE void Frame::Data::decode_layer_III(bool& ok)
{
    set_band_windows(ok);
    if (has_error()) {
        return;
    }

    set_side_info();
    set_main_data();

    u8 number_of_channels = m_header->num_channels();

    for (u8 gr = 0; gr < 2; gr++) {
        for (u8 ch = 0; ch < number_of_channels; ch++) {
            requantize(gr, ch);
        }

        if (m_header->channel_mode() == ChannelMode::JointStereo && m_header->mid_side_stereo()) {
            mid_side_stereo(gr);
        }

        for (u8 ch = 0; ch < number_of_channels; ch++) {
            if (m_block_type[gr][ch] == BlockType::ShortWindows || m_mixed_block_flag[gr][ch]) {
                reorder(gr, ch);
            } else {
                alias_reduction(gr, ch);
            }
            inverse_modified_discrete_cosine_transform(gr, ch);

            frequency_inversion(gr, ch);
            synth_filterbank(gr, ch);
        }
    }
    set_audio_frames();
}

u32 Frame::Data::get_bits(const u8* buffer, u32 start_bit, u32 end_bit)
{
    u32 start_byte = 0;
    u32 end_byte = 0;

    start_byte = start_bit >> 3;
    end_byte = end_bit >> 3;
    start_bit = start_bit % 8;
    end_bit = end_bit % 8;

    u32 result = ((u32)buffer[start_byte] << (32 - (8 - start_bit))) >> (32 - (8 - start_bit));

    if (start_byte != end_byte) {
        while (++start_byte != end_byte) {
            result <<= 8;
            result += buffer[start_byte];
        }
        result <<= end_bit;
        result += buffer[end_byte] >> (8 - end_bit);
    } else if (end_bit != 8) {
        result >>= (8 - end_bit);
    }

    return result;
}

ALWAYS_INLINE u32 Frame::Data::get_bits_incremental(const u8* buffer, u32* offset, u32 count)
{
    u32 result = get_bits(buffer, *offset, *offset + count);
    *offset += count;
    return result;
}

ALWAYS_INLINE void Frame::Data::set_band_windows(bool& ok)
{
    ok = true;
    switch (m_header->sample_rate()) {
    case 32'000:
        m_band_index.short_win = band_index_table.short_32;
        m_band_width.short_win = band_width_table.short_32;
        m_band_index.long_win = band_index_table.long_32;
        m_band_width.long_win = band_width_table.long_32;
        break;
    case 44'100:
        m_band_index.short_win = band_index_table.short_44;
        m_band_width.short_win = band_width_table.short_44;
        m_band_index.long_win = band_index_table.long_44;
        m_band_width.long_win = band_width_table.long_44;
        break;
    case 48'000:
        m_band_index.short_win = band_index_table.short_48;
        m_band_width.short_win = band_width_table.short_48;
        m_band_index.long_win = band_index_table.long_48;
        m_band_width.long_win = band_width_table.long_48;
        break;
    default:
        ERROR(String::format("Unsupported sampling rate: %u Hz", m_header->sample_rate()));
    }
}

void Frame::Data::set_side_info()
{
    u32 crc_offset = m_header->has_crc() == false ? 2 : 0;
    const u8* buffer = &m_data.data()[crc_offset];
    u32 offset = 0;

    m_main_data_begin = get_bits_incremental(buffer, &offset, 9);

    // Skip private bits.
    offset += m_header->channel_mode() == ChannelMode::Mono ? 5 : 3;

    u8 number_of_channels = m_header->num_channels();

    for (u8 ch = 0; ch < number_of_channels; ch++) {
        for (u8 scfsi_band = 0; scfsi_band < 4; scfsi_band++) {
            m_scfsi[ch][scfsi_band] = (bool)get_bits_incremental(buffer, &offset, 1);
        }
    }

    for (u8 gr = 0; gr < 2; gr++) {
        for (u8 ch = 0; ch < number_of_channels; ch++) {
            m_part2_3_length[gr][ch] = (u16)get_bits_incremental(buffer, &offset, 12);
            m_big_value[gr][ch] = (u16)get_bits_incremental(buffer, &offset, 9);
            m_global_gain[gr][ch] = (u8)get_bits_incremental(buffer, &offset, 8);
            m_scalefac_compress[gr][ch] = (u8)get_bits_incremental(buffer, &offset, 4);
            m_window_switching[gr][ch] = (bool)get_bits_incremental(buffer, &offset, 1) == 1;

            if (m_window_switching[gr][ch]) {
                m_block_type[gr][ch] = (BlockType)get_bits_incremental(buffer, &offset, 2);
                m_mixed_block_flag[gr][ch] = (bool)get_bits_incremental(buffer, &offset, 1);
                if (m_mixed_block_flag[gr][ch]) {
                    m_switch_point_long[gr][ch] = 8;
                    m_switch_point_short[gr][ch] = 3;
                } else {
                    m_switch_point_long[gr][ch] = 0;
                    m_switch_point_short[gr][ch] = 0;
                }

                m_region0_count[gr][ch] = m_block_type[gr][ch] == BlockType::ShortWindows ? 8 : 7;
                m_region1_count[gr][ch] = 20 - m_region0_count[gr][ch];

                for (u8 region = 0; region < 2; region++) {
                    m_table_select[gr][ch][region] = (u8)get_bits_incremental(buffer, &offset, 5);
                }

                for (u8 window = 0; window < 3; window++) {
                    m_subblock_gain[gr][ch][window] = (u8)get_bits_incremental(buffer, &offset, 3);
                }
            } else {
                m_block_type[gr][ch] = BlockType::Reserved;
                m_mixed_block_flag[gr][ch] = false;

                for (u8 region = 0; region < 3; region++) {
                    m_table_select[gr][ch][region] = (int)get_bits_incremental(buffer, &offset, 5);
                }

                m_region0_count[gr][ch] = (u8)get_bits_incremental(buffer, &offset, 4);
                m_region1_count[gr][ch] = (u8)get_bits_incremental(buffer, &offset, 3);
            }

            m_preflag[gr][ch] = (bool)get_bits_incremental(buffer, &offset, 1);
            m_scalefactor_scale[gr][ch] = (bool)get_bits_incremental(buffer, &offset, 1);
            m_count1table_select[gr][ch] = (bool)get_bits_incremental(buffer, &offset, 1);
            m_slen1[gr][ch] = slen[m_scalefac_compress[gr][ch]][0];
            m_slen2[gr][ch] = slen[m_scalefac_compress[gr][ch]][1];
        }
    }
}

void Frame::Data::set_main_data()
{
    // FIXME: Check CRC16.
    u32 offset = m_header->has_crc() == false ? 2 : 0;
    offset += m_header->channel_mode() == ChannelMode::Mono ? 17 : 32; // Size of side info.

    u32 frame_data_size = m_header->frame_size() - m_header->header_size();

    if (m_main_data_begin == 0) {
        u32 size = frame_data_size - offset;
        m_main_data = ByteBuffer::create_uninitialized(size);
        memcpy(m_main_data.data(), m_data.data() + offset, size);

        m_loader->append_data(m_main_data);
    } else {
        u32 size_of_data = 0;
        const Vector<ByteBuffer> loaded_data = m_loader->loaded_data();
        for (u32 frame = 0; frame < loaded_data.size(); frame++) {
            dbgln_if(MP3_SHOW_DATA, "MP3 frame data prev_frame_size[{}]={}", frame, loaded_data[frame].size());
            size_of_data += loaded_data[frame].size();
            if (m_main_data_begin < size_of_data) {
                // FIXME: Support case when data is across more than 2 frames.
                VERIFY(frame == 0);
                u32 size = frame_data_size - offset + m_main_data_begin;
                const u8* end_ptr = (const u8*)loaded_data[frame].end_pointer();

                m_main_data = ByteBuffer::create_uninitialized(size);

                memcpy(m_main_data.data(), end_ptr - m_main_data_begin, m_main_data_begin);
                memcpy(m_main_data.data() + m_main_data_begin, m_data.data() + offset, frame_data_size - offset);

                m_loader->append_data(m_main_data);
                break;
            }
        }
    }

    dbgln_if(MP3_SHOW_DATA, "MP3 frame data main_data_begin={}, size={}", m_main_data_begin, m_main_data.size());

    u32 bit = 0;
    for (u8 gr = 0; gr < 2; gr++) {
        for (u8 ch = 0; ch < (int)m_header->num_channels(); ch++) {
            u32 max_bit = bit + m_part2_3_length[gr][ch];
            unpack_scalefac(m_main_data.data(), gr, ch, bit);
            unpack_samples(m_main_data.data(), gr, ch, bit, max_bit);
            bit = max_bit;
        }
    }
}

void Frame::Data::unpack_scalefac(u8* main_data, u8 gr, u8 ch, u32& bit)
{
    u8 sfb = 0;
    u8 window = 0;
    u8 scalefactor_length[2] {
        slen[m_scalefac_compress[gr][ch]][0],
        slen[m_scalefac_compress[gr][ch]][1]
    };

    if (m_block_type[gr][ch] == BlockType::ShortWindows && m_window_switching[gr][ch]) {
        if (m_mixed_block_flag[gr][ch] == 1) {
            for (sfb = 0; sfb < 8; sfb++) {
                m_scalefactor_long[gr][ch][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[0]);
            }

            for (sfb = 3; sfb < 6; sfb++) {
                for (window = 0; window < 3; window++) {
                    m_scalefactor_short[gr][ch][window][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[0]);
                }
            }
        } else {
            for (sfb = 0; sfb < 6; sfb++) {
                for (window = 0; window < 3; window++) {
                    m_scalefactor_short[gr][ch][window][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[0]);
                }
            }
        }
        for (sfb = 6; sfb < 12; sfb++) {
            for (window = 0; window < 3; window++) {
                m_scalefactor_short[gr][ch][window][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[1]);
            }
        }

        for (window = 0; window < 3; window++) {
            m_scalefactor_short[gr][ch][window][12] = 0;
        }

    } else {
        if (gr == 0) {
            for (sfb = 0; sfb < 11; sfb++) {
                m_scalefactor_long[gr][ch][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[0]);
            }
            for (; sfb < 21; sfb++) {
                m_scalefactor_long[gr][ch][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[1]);
            }
        } else {
            u8 i = 0;
            for (; i < 2; i++) {
                for (; sfb < scalefac_sb[i]; sfb++) {
                    if (m_scfsi[ch][i]) {
                        m_scalefactor_long[gr][ch][sfb] = m_scalefactor_long[0][ch][sfb];
                    } else {
                        m_scalefactor_long[gr][ch][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[0]);
                    }
                }
            }
            for (i = 2; i < 4; i++) {
                for (; sfb < scalefac_sb[i]; sfb++) {
                    if (m_scfsi[ch][i]) {
                        m_scalefactor_long[gr][ch][sfb] = m_scalefactor_long[0][ch][sfb];
                    } else {
                        m_scalefactor_long[gr][ch][sfb] = (u8)get_bits_incremental(main_data, &bit, scalefactor_length[1]);
                    }
                }
            }
        }
        m_scalefactor_long[gr][ch][21] = 0;
    }
}

void Frame::Data::unpack_samples(u8* main_data, u8 gr, u8 ch, u32 bit, u32 max_bit)
{
    u32 sample = 0;
    u32 table_num;
    u32 region0;
    u32 region1;
    const u32* table;

    // NOTE: memset() is slower ??
    for (u16 i = 0; i < 576; i++) {
        m_loader->m_raw_samples[gr][ch][i] = 0;
    }

    if (m_window_switching[gr][ch] && m_block_type[gr][ch] == BlockType::ShortWindows) {
        region0 = 36;
        region1 = 576;
    } else {
        region0 = m_band_index.long_win[m_region0_count[gr][ch] + 1];
        region1 = m_band_index.long_win[m_region0_count[gr][ch] + 1 + m_region1_count[gr][ch] + 1];
    }

    for (; sample < (u32)m_big_value[gr][ch] * 2; sample += 2) {
        if (sample < region0) {
            table_num = m_table_select[gr][ch][0];
            table = big_value_table[table_num];
        } else if (sample < region1) {
            table_num = m_table_select[gr][ch][1];
            table = big_value_table[table_num];
        } else {
            table_num = m_table_select[gr][ch][2];
            table = big_value_table[table_num];
        }

        if (table_num == 0) {
            m_loader->m_raw_samples[gr][ch][sample] = 0;
            continue;
        }

        bool repeat = true;
        u32 bit_sample = get_bits(main_data, bit, bit + 32);

        for (u32 row = 0; row < big_value_max[table_num] && repeat; row++) {
            for (u32 col = 0; col < big_value_max[table_num]; col++) {
                u32 i = 2 * big_value_max[table_num] * row + 2 * col;
                u32 value = table[i];
                u32 size = table[i + 1];
                if (value >> (32 - size) == bit_sample >> (32 - size)) {
                    bit += size;

                    int values[2] = { (int)row, (int)col };
                    for (u8 i = 0; i < 2; i++) {
                        int linbit = 0;
                        if (big_value_linbit[table_num] != 0 && values[i] == (int)big_value_max[table_num] - 1) {
                            linbit = get_bits_incremental(main_data, &bit, big_value_linbit[table_num]);
                        }

                        char sign = 1;
                        if (values[i] > 0) {
                            sign = get_bits_incremental(main_data, &bit, 1) ? -1 : 1;
                        }

                        m_loader->m_raw_samples[gr][ch][sample + i] = (float)(sign * (values[i] + linbit));
                    }

                    repeat = false;
                    break;
                }
            }
        }
    }

    for (; bit < max_bit && sample + 4 < 576; sample += 4) {
        int values[4];

        if (m_count1table_select[gr][ch] == 1) {
            u32 bit_sample = get_bits_incremental(main_data, &bit, 4);
            values[0] = (bit_sample & 0x08) > 0 ? 0 : 1;
            values[1] = (bit_sample & 0x04) > 0 ? 0 : 1;
            values[2] = (bit_sample & 0x02) > 0 ? 0 : 1;
            values[3] = (bit_sample & 0x01) > 0 ? 0 : 1;
        } else {
            u32 bit_sample = get_bits(main_data, bit, bit + 32);
            for (u8 entry = 0; entry < 16; entry++) {
                u32 value = quad_table_1.hcod[entry];
                u32 size = quad_table_1.hlen[entry];

                if (value >> (32 - size) == bit_sample >> (32 - size)) {
                    bit += size;
                    for (u8 i = 0; i < 4; i++) {
                        values[i] = (int)quad_table_1.value[entry][i];
                    }
                    break;
                }
            }
        }

        u8 i = 0;
        for (; i < 4; i++) {
            if (values[i] > 0 && get_bits_incremental(main_data, &bit, 1) == 1) {
                values[i] = -values[i];
            }
        }
        for (i = 0; i < 4; i++) {
            m_loader->m_raw_samples[gr][ch][sample + i] = values[i];
        }
    }

    for (; sample < 576; sample++) {
        m_loader->m_raw_samples[gr][ch][sample] = 0;
    }
}

void Frame::Data::requantize(u8 gr, u8 ch)
{
    float exp1 = 0, exp2 = 0;
    u8 window = 0;
    u8 sfb = 0;
    float scalefac_mult = m_scalefactor_scale[gr][ch] == 0 ? 0.5 : 1;

    for (u32 sample = 0, i = 0; sample < 576; sample++, i++) {
        if (m_block_type[gr][ch] == BlockType::ShortWindows || (m_mixed_block_flag[gr][ch] && sfb >= 8)) {
            if (i == m_band_width.short_win[sfb]) {
                i = 0;
                if (window == 2) {
                    window = 0;
                    sfb++;
                } else {
                    window++;
                }
            }

            exp1 = m_global_gain[gr][ch] - 210.0 - 8.0 * m_subblock_gain[gr][ch][window];
            exp2 = scalefac_mult * m_scalefactor_short[gr][ch][window][sfb];
        } else {
            if (sample == m_band_index.long_win[sfb + 1]) {
                sfb++;
            }

            exp1 = m_global_gain[gr][ch] - 210.0;
            exp2 = scalefac_mult * (m_scalefactor_long[gr][ch][sfb] + m_preflag[gr][ch] * pretab[sfb]);
        }

        char sign = m_loader->m_raw_samples[gr][ch][sample] < 0 ? -1 : 1;
        float a = powf(fabsf(m_loader->m_raw_samples[gr][ch][sample]), 4.0f / 3.0f);
        float b = powf(2.0f, exp1 / 4.0f);
        float c = powf(2.0f, -exp2);

        m_loader->m_raw_samples[gr][ch][sample] = sign * a * b * c;
    }
}

void Frame::Data::mid_side_stereo(u8 gr)
{
    for (u16 sample = 0; sample < 576; sample++) {
        float middle = m_loader->m_raw_samples[gr][0][sample];
        float side = m_loader->m_raw_samples[gr][1][sample];
        m_loader->m_raw_samples[gr][0][sample] = (middle + side) / (float)M_SQRT2;
        m_loader->m_raw_samples[gr][1][sample] = (middle - side) / (float)M_SQRT2;
    }
}

void Frame::Data::reorder(u8 gr, u8 ch)
{
    u32 total = 0;
    u32 start = 0;
    u32 block = 0;

    for (u8 sb = 0; sb < 12; sb++) {
        u32 sb_width = m_band_width.short_win[sb];

        for (u32 ss = 0; ss < sb_width; ss++) {
            m_loader->m_temp_pcm2[start + block + 0] = m_loader->m_raw_samples[gr][ch][total + ss + sb_width * 0];
            m_loader->m_temp_pcm2[start + block + 6] = m_loader->m_raw_samples[gr][ch][total + ss + sb_width * 1];
            m_loader->m_temp_pcm2[start + block + 12] = m_loader->m_raw_samples[gr][ch][total + ss + sb_width * 2];

            if (block != 0 && block % 5 == 0) {
                start += 18;
                block = 0;
            } else {
                block++;
            }
        }
        total += sb_width * 3;
    }

    for (u32 i = 0; i < 576; i++) {
        m_loader->m_raw_samples[gr][ch][i] = m_loader->m_temp_pcm2[i];
    }
}

void Frame::Data::alias_reduction(u8 gr, u8 ch)
{
    u32 sb_max = m_mixed_block_flag[gr][ch] ? 2 : 32;

    for (u32 sb = 1; sb < sb_max; sb++) {
        for (u8 sample = 0; sample < 8; sample++) {
            u32 offset1 = 18 * sb - sample - 1;
            u32 offset2 = 18 * sb + sample;
            float s1 = m_loader->m_raw_samples[gr][ch][offset1];
            float s2 = m_loader->m_raw_samples[gr][ch][offset2];
            m_loader->m_raw_samples[gr][ch][offset1] = s1 * ar_coeff_cs[sample] - s2 * ar_coeff_ca[sample];
            m_loader->m_raw_samples[gr][ch][offset2] = s2 * ar_coeff_cs[sample] + s1 * ar_coeff_ca[sample];
        }
    }
}

void Frame::Data::inverse_modified_discrete_cosine_transform(u8 gr, u8 ch)
{
    u8 n = m_block_type[gr][ch] == BlockType::ShortWindows ? 12 : 36;
    u8 half_n = n >> 1;
    u8 max_win = m_block_type[gr][ch] == BlockType::ShortWindows ? 3 : 1;
    u32 sample = 0;

    for (u8 block = 0; block < 32; block++) {
        for (u8 win = 0; win < max_win; win++) {
            for (u8 i = 0; i < n; i++) {
                float xi = 0.0;
                for (u8 k = 0; k < half_n; k++) {
                    float s = m_loader->m_raw_samples[gr][ch][18 * block + half_n * win + k];
                    xi += s * cosf((float)M_PI / (2 * n) * (2 * i + 1 + half_n) * (2 * k + 1));
                }

                m_loader->m_imdct_sample_block[win * n + i] = xi * imdct_sine_block[(u8)m_block_type[gr][ch]][i];
            }
        }

        if (m_block_type[gr][ch] == BlockType::ShortWindows) {
            memcpy(m_loader->m_imdct_temp_block, m_loader->m_imdct_sample_block, sizeof(m_loader->m_imdct_temp_block));

            u8 i = 0;
            for (; i < 6; i++) {
                m_loader->m_imdct_sample_block[i] = 0;
            }
            for (; i < 12; i++) {
                m_loader->m_imdct_sample_block[i] = m_loader->m_imdct_temp_block[0 + i - 6];
            }
            for (; i < 18; i++) {
                m_loader->m_imdct_sample_block[i] = m_loader->m_imdct_temp_block[0 + i - 6] + m_loader->m_imdct_temp_block[12 + i - 12];
            }
            for (; i < 24; i++) {
                m_loader->m_imdct_sample_block[i] = m_loader->m_imdct_temp_block[12 + i - 12] + m_loader->m_imdct_temp_block[24 + i - 18];
            }
            for (; i < 30; i++) {
                m_loader->m_imdct_sample_block[i] = m_loader->m_imdct_temp_block[24 + i - 18];
            }
            for (; i < 36; i++) {
                m_loader->m_imdct_sample_block[i] = 0;
            }
        }

        for (u8 i = 0; i < 18; i++) {
            m_loader->m_raw_samples[gr][ch][sample + i] = m_loader->m_imdct_sample_block[i] + m_loader->m_prev_samples[ch][block][i];
            m_loader->m_prev_samples[ch][block][i] = m_loader->m_imdct_sample_block[18 + i];
        }
        sample += 18;
    }
}

void Frame::Data::frequency_inversion(u8 gr, u8 ch)
{
    for (u8 sb = 1; sb < 18; sb += 2) {
        for (u8 i = 1; i < 32; i += 2) {
            m_loader->m_raw_samples[gr][ch][i * 18 + sb] *= -1.0f;
        }
    }
}

void Frame::Data::synth_filterbank(u8 gr, u8 ch)
{
    u16 i, j;
    for (u8 sb = 0; sb < 18; sb++) {
        for (i = 0; i < 32; i++) {
            m_loader->m_synth_s[i] = m_loader->m_raw_samples[gr][ch][i * 18 + sb];
        }

        for (i = 1023; i > 63; i--) {
            m_loader->m_fifo[ch][i] = m_loader->m_fifo[ch][i - 64];
        }

        for (i = 0; i < 64; i++) {
            m_loader->m_fifo[ch][i] = 0.0;
            for (j = 0; j < 32; j++) {
                m_loader->m_fifo[ch][i] += m_loader->m_synth_s[j] * synth_n_table[i][j];
            }
        }

        for (i = 0; i < 8; i++) {
            for (j = 0; j < 32; j++) {
                m_loader->m_synth_u[i * 64 + j] = m_loader->m_fifo[ch][i * 128 + j];
                m_loader->m_synth_u[i * 64 + j + 32] = m_loader->m_fifo[ch][i * 128 + j + 96];
            }
        }

        for (i = 0; i < 512; i++)
            m_loader->m_synth_w[i] = m_loader->m_synth_u[i] * synth_window[i];

        for (i = 0; i < 32; i++) {
            float sum = 0;
            for (j = 0; j < 16; j++) {
                sum += m_loader->m_synth_w[j * 32 + i];
            }
            m_loader->m_temp_pcm[32 * sb + i] = sum;
        }
    }

    memcpy(m_loader->m_raw_samples[gr][ch], m_loader->m_temp_pcm, 576 * 4);
}

void Frame::Data::set_audio_frames()
{
    u32 i = 0;
    u8 number_of_channels = m_header->num_channels();

    m_audio_frames.resize(2 * 576);
    Audio::Frame frame;
    if (number_of_channels == 1) {
        for (u8 gr = 0; gr < 2; gr++) {
            for (u16 sample = 0; sample < 576; sample++) {
                frame.left = m_loader->m_raw_samples[gr][0][sample];
                frame.right = m_loader->m_raw_samples[gr][0][sample];
                m_audio_frames[i++] = frame;
            }
        }
    } else {
        for (u8 gr = 0; gr < 2; gr++) {
            for (u16 sample = 0; sample < 576; sample++) {
                frame.left = m_loader->m_raw_samples[gr][0][sample];
                frame.right = m_loader->m_raw_samples[gr][1][sample];
                m_audio_frames[i++] = frame;
            }
        }
    }
}

// Frame
Frame::Frame(Mp3LoaderPlugin* loader)
    : m_loader(loader)
{
    m_header = adopt(*new Header(m_loader));
    m_valid = m_header->is_valid();
    if (!m_valid) {
        return;
    }

    m_data = make<Data>(m_loader, m_header);
    m_valid = m_data->is_valid();
    if (!m_valid) {
        return;
    }
}

}

#undef ERROR
#undef CHECK_OK
#undef CHECK_OK_WITHOUT_MESSAGE
