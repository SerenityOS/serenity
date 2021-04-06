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

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibAudio/Buffer.h>

#define MP3_SHOW_HEADER 0
#define MP3_SHOW_DATA 0

namespace Audio {

class Mp3LoaderPlugin;

namespace Mp3 {

class Frame {
public:
    enum class Layer : u8 {
        Reserved = 0,
        Layer_III = 1,
        Layer_II = 2,
        Layer_I = 3
    };

    enum class ChannelMode : u8 {
        Stereo = 0,
        JointStereo = 1,
        DualChannel = 2,
        Mono = 3
    };

    // Based on: http://www.mp3-tech.org/programmer/frame_header.html
    class Header : public RefCounted<Header> {
    public:
        Header(Mp3LoaderPlugin*);
        ~Header() {};

        bool is_valid() const { return m_valid; }

        bool has_error() const { return !m_error_string.is_empty(); }
        const char* error_string() const { return m_error_string.characters(); }

        const String& version() const { return m_version; };

        u32 bit_rate() const { return m_bit_rate; };
        Layer layer() const { return m_layer; };
        u32 sample_rate() const { return m_sample_rate; };
        u32 samples_per_frame() const { return m_samples_per_frame; };
        u32 bits_per_sample() const { return 0; };
        u32 num_channels() const { return m_num_channels; };
        ChannelMode channel_mode() const { return m_channel_mode; };
        bool has_crc() const { return m_crc; };
        u8 mode_extension() const { return m_mode_extension; };
        float duration() const { return m_duration; };

        // Only used in Joint stereo.
        bool intensity_stereo() const { return (m_mode_extension & 0x01) != 0; };
        bool mid_side_stereo() const { return (m_mode_extension & 0x02) != 0; };

        size_t frame_size() const { return m_frame_size; };
        size_t header_size() const { return 4; };

    private:
        void set_version(u8, bool&);
        void set_layer(u8, bool&);
        void set_crc(u8);
        void set_bit_rate(u8, bool&);
        void set_sample_rate(u8, bool&);
        void set_padding(u8);
        void set_channel_mode(u8);
        void set_mode_extension(u8);
        void set_emphasis(u8);
        void set_frame_size_and_duration();

        String m_error_string;

        String m_version;
        Layer m_layer { Layer::Reserved };
        bool m_crc { false };
        u32 m_bit_rate { 0 };
        u32 m_sample_rate { 0 };
        bool m_padding { false };
        ChannelMode m_channel_mode { ChannelMode::Stereo };
        u16 m_num_channels { 0 };
        u8 m_mode_extension { 0 };
        u8 m_emphasis { 0 };

        size_t m_samples_per_frame { 0 };
        size_t m_frame_size { 0 };
        float m_duration { 0 };

        bool m_valid { false };

        Mp3LoaderPlugin* m_loader { nullptr };
    };

    // Based on: http://www.ece.cmu.edu/~ece796/documents/MPEG-1_Audio_CD.doc
    //           https://www.diva-portal.org/smash/get/diva2:830195/FULLTEXT01.pdf
    //           https://github.com/markjeee/libmad
    //           https://github.com/FlorisCreyf/mp3-decoder
    class Data {
    public:
        struct BandWindows {
            const u32* long_win;
            const u32* short_win;
        };

        enum class BlockType : u8 {
            Reserved = 0,
            StartBlock = 1,
            ShortWindows = 2,
            EndBlock = 3
        };

        Data(Mp3LoaderPlugin*, RefPtr<Header>&);

        bool is_valid() const { return m_valid; };

        bool has_error() const { return !m_error_string.is_empty(); }
        const char* error_string() const { return m_error_string.characters(); }

        const Vector<Audio::Frame>& audio_frames() const { return m_audio_frames; };

    private:
        void decode_layer_III(bool&);

        void set_band_windows(bool&);
        void set_side_info();
        void set_main_data();

        u32 get_bits(const u8*, u32, u32);
        u32 get_bits_incremental(const u8*, u32*, u32);

        void unpack_scalefac(u8*, u8, u8, u32&);
        void unpack_samples(u8*, u8, u8, u32, u32);

        void requantize(u8, u8);
        void mid_side_stereo(u8);
        void reorder(u8, u8);
        void alias_reduction(u8, u8);
        void inverse_modified_discrete_cosine_transform(u8, u8);
        void frequency_inversion(u8, u8);
        void synth_filterbank(u8, u8);
        void set_audio_frames();

        bool m_valid { false };
        String m_error_string;

        Mp3LoaderPlugin* m_loader { nullptr };
        RefPtr<Header> m_header { nullptr };
        ByteBuffer m_data;

        size_t m_main_data_begin;
        ByteBuffer m_main_data;

        Vector<Audio::Frame> m_audio_frames;

        BandWindows m_band_index;
        BandWindows m_band_width;

        bool m_scfsi[2][4] = {};
        u16 m_part2_3_length[2][2] = {};
        u16 m_big_value[2][2] = {};
        u8 m_global_gain[2][2] = {};
        u8 m_scalefac_compress[2][2] = {};
        u8 m_slen1[2][2] = {};
        u8 m_slen2[2][2] = {};
        bool m_window_switching[2][2] = {};
        BlockType m_block_type[2][2] = {};
        bool m_mixed_block_flag[2][2] = {};
        u8 m_switch_point_long[2][2] = {};
        u8 m_switch_point_short[2][2] = {};
        u8 m_table_select[2][2][3] = {};
        u8 m_subblock_gain[2][2][3] = {};
        u8 m_region0_count[2][2] = {};
        u8 m_region1_count[2][2] = {};
        bool m_preflag[2][2] = {};
        bool m_scalefactor_scale[2][2] = {};
        bool m_count1table_select[2][2] = {};

        u8 m_scalefactor_long[2][2][22] = {};
        u8 m_scalefactor_short[2][2][3][13] = {};
    };

public:
    Frame(Mp3LoaderPlugin*);

    bool is_valid() const { return m_valid; };
    bool has_error() const { return !m_error_string.is_empty(); }
    const char* error_string() const { return m_error_string.characters(); }

    u32 sample_rate() const { return m_header->sample_rate(); };
    u32 num_channels() const { return m_header->num_channels(); };
    u32 bits_per_sample() const { return m_header->bits_per_sample(); };
    float duration() const { return m_header->duration(); };

    size_t size() const { return m_header->frame_size(); };
    size_t header_size() const { return m_header->header_size(); };

    const Vector<Audio::Frame>& audio_frames() const { return m_data->audio_frames(); };

private:
    bool m_valid { false };
    String m_error_string;

    Mp3LoaderPlugin* m_loader { nullptr };
    RefPtr<Header> m_header;
    OwnPtr<Data> m_data;
};

}
}
