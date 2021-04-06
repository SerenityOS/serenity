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

#include <AK/NumericLimits.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <LibAudio/Buffer.h>
#include <LibAudio/Mp3DecodingTables.h>
#include <LibAudio/Mp3Frame.h>
#include <LibAudio/Mp3Id3.h>
#include <LibAudio/Mp3Loader.h>
#include <LibCore/File.h>

namespace Audio {

Mp3LoaderPlugin::Mp3LoaderPlugin(const StringView& path)
    : m_file(Core::File::construct(path))
{
    if (!m_file->open(Core::IODevice::ReadOnly)) {
        m_error_string = String::formatted("Can't open file: {}", m_file->error_string());
        return;
    }

    m_valid = parse_header();
    if (!m_valid)
        return;

    read_at_least(1.0f);
}

Mp3LoaderPlugin::Mp3LoaderPlugin(const ByteBuffer& buffer)
{
    m_stream = make<InputMemoryStream>(buffer);
    if (!m_stream) {
        m_error_string = String::formatted("Can't open memory stream");
        return;
    }

    m_valid = parse_header();
    if (!m_valid)
        return;

    read_at_least(1.0f);
}

ALWAYS_INLINE bool Mp3LoaderPlugin::sniff()
{
    return m_valid;
}

RefPtr<Buffer> Mp3LoaderPlugin::get_more_samples([[maybe_unused]] size_t max_bytes_to_read_from_input)
{
    // FIXME: This is ugly.
    u32 ctr = m_loaded_samples;
    if (m_loaded_samples >= total_samples()) {
        ctr = total_samples() - 1;
    }

    m_loaded_samples++;
    m_loaded_samples = min(m_loaded_samples, total_samples());

    // NOTE: Experiment with this value, 1 s seems fine now.
    read_at_least(1.0f);

    return m_samples[ctr];
}

void Mp3LoaderPlugin::seek(const int position)
{
    (void)position;
    // FIXME: Implement this.
}

ALWAYS_INLINE void Mp3LoaderPlugin::seek_raw(size_t byte_position)
{
    if (m_file) {
        m_file->seek(byte_position);
    } else {
        m_stream->seek(byte_position);
    }
}

ALWAYS_INLINE void Mp3LoaderPlugin::reset()
{
    m_loaded_samples = 0;
    seek(0);
}

u8 Mp3LoaderPlugin::read_byte(bool& ok)
{
    u8 value;
    if (m_file) {
        m_file->read(&value, 1);
        ok = !m_file->has_error();
    } else {
        *m_stream >> value;
        ok = m_stream->handle_any_error() == false;
    }
    return value;
}

void Mp3LoaderPlugin::read(Bytes bytes, bool& ok)
{
    ok = true;
    if (m_file) {
        auto data = m_file->read(bytes.size());
        if (data.is_empty() || data.size() != bytes.size()) {
            ok = false;
        } else {
            // FIXME: This should be ok, but if the file is corrupted,
            //        there could be partial frames. Possible fix to report
            //        back the read size.
            memcpy(bytes.data(), data.data(), data.size());
        }
    } else {
        // FIXME: Implement this.
        TODO();
    }
}

bool Mp3LoaderPlugin::parse_header()
{
    m_valid = false;
    m_id3 = make<Mp3::Id3>(this);

    if (m_id3->has_error()) {
        m_error_string = String::format("ID3 error: %s", m_id3->error_string());
        return false;
    }

    m_valid = m_id3->is_valid();
    if (m_valid) {
        size_t header_size = 10; // ID3 header is 10 bytes.
        if (m_id3->has_footer())
            header_size += 10;
        // FIXME: Maybe the extended header needs to be considered here.
        seek_raw(m_id3->size() + header_size);
    } else {
        seek_raw(0); // Seek needs to be reseted, because at least 3 bytes were read.
    }

    // Note: MP3 file can be valid without an ID3 header.
    read_next_frame();

    if (m_current_frame->has_error()) {
        m_error_string = m_current_frame->error_string();
        return false;
    }

    m_valid = m_current_frame->is_valid();
    if (m_valid) {
        // Note: Assuming that ervery frame has the same properties.
        m_sample_rate = m_current_frame->sample_rate();
        m_num_channels = m_current_frame->num_channels();
        m_bits_per_sample = m_current_frame->bits_per_sample();

        // FIXME: Support others.
        if (m_sample_rate != 44'100) {
            m_error_string = String::format("Unsupported sampling rate: %u Hz", m_sample_rate);
        }
    }

    return m_valid;
}

ALWAYS_INLINE bool Mp3LoaderPlugin::read_next_frame()
{
    m_current_frame = make<Mp3::Frame>(this);
    m_valid = m_current_frame->is_valid();
    if (m_current_frame->has_error() == false && m_valid) {
        m_loaded_frames.append(m_current_frame->audio_frames());
    }

    return m_valid;
}

// FIXME: End this, when the file/stream is over. It's crashing now.
void Mp3LoaderPlugin::read_at_least(u32 number_of_samples)
{
    for (;;) {
        bool ok = read_next_frame();

        if (m_loaded_frames.size() >= number_of_samples || !ok) {
            m_samples.append(Buffer::create_with_samples(move(m_loaded_frames)));

            m_loaded_frames.clear();
        }
    }
}

// FIXME: End this, when the file/stream is over. It's crashing now.
void Mp3LoaderPlugin::read_at_least(float duration_in_seconds)
{
    float read_so_far = 0.0f;
    for (;;) {
        bool ok = read_next_frame();

        if (ok) {
            read_so_far += m_current_frame->duration();
        }

        if (read_so_far >= duration_in_seconds || !ok) {
            m_samples.append(Buffer::create_with_samples(move(m_loaded_frames)));

            m_loaded_frames.clear();
            return;
        }
    }
}

}
