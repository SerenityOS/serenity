
#include <AK/Format.h>
#include <LibMedia/Manip.h>
#include <LibMedia/readers/AVI/AVI.h>

namespace Media::Reader::AVI {

constexpr u32 generate_chunk_id(bool const video, u32 const index)
{
    // e.g. "01dc" for video, index==1
    // FIXME: identify subtitle id and handle here
    u32 result = video == true ? fourcc_to_u32("00dc") : fourcc_to_u32("00wb");
    result |= ((index & 0xFF) << 8);
    result |= ((index >> 8) & 0xFF);
    return result;
}

// FIXME: replace std::cout/cerr with logging

AVIReader::AVIReader(AK::String filePath)
    : m_file_path(move(filePath))
{
    m_reader = try_create<BitStreamReader>(m_file_path, BitStreamReader::Endian::LITTLE);
    uint32_t riff = 0;
    if (m_reader->read_fourcc(riff) == false) {
        warnln("Failed to read start of file");
        return;
    }
    if (riff != fourcc_to_u32("RIFF")) {
        warnln("not a RIFF file");
        return;
    }
    if (m_reader->read<u32>(m_file_size) == false) {
        warnln("failed to read filesize");
        return;
    }
    uint32_t file_type = 0;
    if (m_reader->read_fourcc(file_type) == false) {
        warnln("failed to read file type");
        return;
    }
    if (file_type != fourcc_to_u32("AVI ")) {
        warnln("not a RIFF AVI");
        return;
    }

    ByteBuffer buf;
    while (true) {
        u32 id = 0;
        if (m_reader->read_fourcc(id) == false) {
            if (m_reader->at_eof() == false) {
                warnln("failed to read id");
            }
            goto parse_end;
        }
        u32 list_size = 0;
        if (m_reader->read<u32>(list_size) == false) {
            warnln("read list_size failed");
            goto parse_end;
        }

        switch (id) {
        case fourcc_to_u32("LIST"): {
            u32 list_type = 0;
            if (m_reader->read_fourcc(list_type) == false) {
                warnln("read list_type failed");
                goto parse_end;
            }
            auto const list_offset = m_reader->offset();
            ByteBuffer data;
            if (list_type == fourcc_to_u32("movi")) {
                // Data could be uuuge! Do not want in memory.
                if (m_reader->seek(list_size - 4) == false) {
                    warnln("movi: seek failed");
                    goto parse_end;
                }
            } else {
                // -4 as list_type is included
                if (m_reader->read(data, list_size - 4) == false) {
                    warnln("{}: read list data failed", u32_to_fourcc(list_type));
                    goto parse_end;
                }
            }

            switch (list_type) {
            case fourcc_to_u32("hdrl"):
                m_header_list = try_make<AVI::HeaderList>(list_offset, data);
                break;
            case fourcc_to_u32("INFO"):
                m_info = try_make<AVI::INFO>(data);
                break;
            case fourcc_to_u32("movi"):
                m_movi.size = list_size;
                m_movi.offset = list_offset;
                break;
            default:
                warnln("unhandled list: {}", u32_to_fourcc(list_type));
                break;
            }
        } break;
        case fourcc_to_u32("idx1"): {
            ByteBuffer data;
            u32 const idx1_offset = m_reader->offset();
            if (m_reader->read(data, list_size) == false) {
                warnln("Failed to read idx1");
                goto parse_end;
            }
            m_index_table = try_create<IndexTable>(idx1_offset, data);
        } break;
        default:
            // seek over. probably JUNK.
            if (id != fourcc_to_u32("JUNK")) {
                // FIXME: Could be tcdl. IMPL when a sample-file with timecode discontinuity table appears
                warnln("Unhandled chunk '{}'", u32_to_fourcc(id));
            }
            if (m_reader->seek(list_size) == false) {
                goto parse_end;
            }
            break;
        }
    } // while

parse_end:
    m_open = (m_header_list.ptr() != nullptr) && (m_movi.offset != 0) && (m_index_table.ptr() != nullptr);
}

AVIReader::~AVIReader()
{
}

bool AVIReader::is_open() const
{
    return m_open;
}

String AVIReader::format() const
{
    return String("AVI");
}

u64 AVIReader::size() const
{
    return static_cast<u64>(m_file_size);
}

double AVIReader::duration() const
{
    VERIFY(m_header_list.ptr() != nullptr);
    return m_header_list->avi_header().total_frames * static_cast<double>(m_header_list->avi_header().micro_sec_per_frame) / 1'000'000;
}

u32 AVIReader::track_count() const
{
    VERIFY(m_header_list.ptr() != nullptr);
    return m_header_list->avi_header().streams;
}

u32 AVIReader::video_count() const
{
    VERIFY(m_header_list.ptr() != nullptr);
    u32 count = 0;
    for (auto const& stream : m_header_list->streams()) {
        if (stream->type() == StreamType::VIDEO) {
            count += 1;
        }
    }
    return count;
}

u32 AVIReader::audio_count() const
{
    VERIFY(m_header_list.ptr() != nullptr);
    u32 count = 0;
    for (auto const& stream : m_header_list->streams()) {
        if (stream->type() == StreamType::AUDIO) {
            count += 1;
        }
    }
    return count;
}

u32 AVIReader::subtitle_count() const
{
    VERIFY(m_header_list.ptr() != nullptr);
    u32 count = 0;
    for (auto const& stream : m_header_list->streams()) {
        if (stream->type() == StreamType::SUBTITLE) {
            count += 1;
        }
    }
    return count;
}

VideoTrackPtr AVIReader::video_track(u32 const index)
{
    VERIFY(m_header_list.ptr() != nullptr);
    VERIFY(m_index_table.ptr() != nullptr);
    u32 count = 0;

    for (size_t ix = 0; ix < m_header_list->stream_count(); ++ix) {
        auto stream = m_header_list->stream(ix);
        if (stream.type() == StreamType::VIDEO) {
            if (count == index) {
                return try_create<AVIVideoTrack>(ix, stream, m_index_table, m_reader, m_movi);
            }
            count += 1;
        }
    }
    return nullptr;
}

AudioTrackPtr AVIReader::audio_track(u32 const index)
{
    VERIFY(m_header_list.ptr() != nullptr);
    VERIFY(m_index_table.ptr() != nullptr);
    u32 count = 0;

    for (size_t ix = 0; ix < m_header_list->stream_count(); ++ix) {
        auto stream = m_header_list->stream(ix);
        if (stream.type() == StreamType::AUDIO) {
            if (count == index) {
                return try_create<AVIAudioTrack>(ix, stream, m_index_table, m_reader, m_movi);
            }
            count += 1;
        }
    }
    return nullptr;
}

SubtitleTrackPtr AVIReader::subtitle_track(u32 const index)
{
    VERIFY(m_header_list.ptr() != nullptr);
    VERIFY(m_index_table.ptr() != nullptr);
    u32 count = 0;

    for (size_t ix = 0; ix < m_header_list->stream_count(); ++ix) {
        auto stream = m_header_list->stream(ix);
        if (stream.type() == StreamType::SUBTITLE) {
            if (count == index) {
                // FIXME: return an AVISubtitleTrack
            }
            count += 1;
        }
    }
    return nullptr;
}

AVIVideoTrack::AVIVideoTrack(u32 const index, AVI::StreamList streamList, RefPtr<IndexTable> index_table,
    RefPtr<BitStreamReader> reader, Movi movi)
    : m_index(index)
    , m_stream_list(move(streamList))
    , m_chunk_id(generate_chunk_id(true, index))
    , m_index_table(move(index_table))
    , m_reader(move(reader))
    , m_movi(move(movi))
{
}

u32 AVIVideoTrack::index() const
{
    return m_index;
}

double AVIVideoTrack::duration() const
{
    return sample_count() / framerate().to_double();
}

u32 AVIVideoTrack::size() const
{
    if (m_index_table.ptr() == nullptr) {
        return 0;
    }
    u32 sum = 0;
    for (auto const& entry : m_index_table->m_entries) {
        if (entry.chunk_id == m_chunk_id) {
            sum += entry.chunk_length;
        }
    }
    return sum;
}

u32 AVIVideoTrack::bitrate() const
{
    return size() / duration();
}

u32 AVIVideoTrack::codec() const
{
    return m_stream_list.header().fcc_handler;
}

u32 AVIVideoTrack::sample_count() const
{
    return m_stream_list.header().length;
}

SamplePtr AVIVideoTrack::sample(u32 const count)
{
    VERIFY(m_index_table.ptr() != nullptr);
    VERIFY(m_reader.ptr() != nullptr);

    u32 samples = 0;
    for (auto const& entry : m_index_table->m_entries) {
        if (entry.chunk_id != m_chunk_id) {
            continue;
        }
        if (samples == count) {
            // +4 as 1st 4bytes after id are the size of chunk data.
            //  We know this already (does assume table was generated correctly...)
            u32 const position = m_movi.offset + entry.chunk_offset + 4;
            if (m_reader->seek(position, Core::SeekMode::SetPosition) == false) {
                warnln("Failed to seek AVI file to position 0x{:X}", position);
                return nullptr;
            }
            ByteBuffer data;
            if (m_reader->read(data, entry.chunk_length) == false) {
                warnln("Failed to read {} bytes in AVI file at position 0x{:X}", entry.chunk_length, position);
                return nullptr;
            }
            return try_create<AVISample>(position, count, data);
        }
        samples += 1;
    }
    return nullptr;
}

Tuple<u32, u32> AVIVideoTrack::dimensions() const
{
    return Tuple<u32, u32>(m_stream_list.format().video.width, m_stream_list.format().video.height);
}

Rational<u32> AVIVideoTrack::framerate() const
{
    return Rational(m_stream_list.header().rate, m_stream_list.header().scale);
}

Rational<u32> AVIVideoTrack::frame_aspect_ratio() const
{
    if (m_stream_list.video_properties().has_value() == true) {
        auto properties = m_stream_list.video_properties().value();
        VERIFY((properties.frame_aspect_ratio & 0xFFFF) != 0);
        return Rational(properties.frame_aspect_ratio >> 16, properties.frame_aspect_ratio & 0xFFFF);
    }

    // FIXME: guess based on calc of dims
    return Rational(0U, 1U);
}

AVIAudioTrack::AVIAudioTrack(u32 const index, AVI::StreamList streamList, RefPtr<IndexTable> index_table,
    RefPtr<BitStreamReader> reader, Movi movi)
    : m_index(index)
    , m_stream_list(move(streamList))
    , m_chunk_id(generate_chunk_id(false, index))
    , m_index_table(move(index_table))
    , m_reader(move(reader))
    , m_movi(move(movi))
{
}

u32 AVIAudioTrack::index() const
{
    return m_index;
}

double AVIAudioTrack::duration() const
{
    return static_cast<double>(m_stream_list.header().scale) * m_stream_list.header().length / m_stream_list.header().rate;
}

u32 AVIAudioTrack::size() const
{
    if (m_index_table.ptr() == nullptr) {
        return 0;
    }
    u32 sum = 0;
    for (auto const& entry : m_index_table->m_entries) {
        if (entry.chunk_id == m_chunk_id) {
            sum += entry.chunk_length;
        }
    }
    return sum;
}

u32 AVIAudioTrack::bitrate() const
{
    // FIXME: although result could be misleading (padding + container bytes) calculate using index table.
    //        AVI doesn't have a good way of identifying true bitrate. To get that the essence has to be parsed.
    return 0;
}

u32 AVIAudioTrack::codec() const
{
    // FIXME: Convert the fcc_handler enum into something meaningful.
    //        AVI sound streams use an enum to represent what it is unlike video fourcc.
    return m_stream_list.header().fcc_handler;
}

u32 AVIAudioTrack::sample_count() const
{
    return m_stream_list.header().length;
}

SamplePtr AVIAudioTrack::sample(u32 const count)
{
    VERIFY(m_index_table.ptr() != nullptr);
    VERIFY(m_reader.ptr() != nullptr);

    u32 samples = 0;
    for (auto const& entry : m_index_table->m_entries) {
        if (entry.chunk_id != m_chunk_id) {
            continue;
        }
        if (samples == count) {
            // +4 as 1st 4bytes after id are the size of chunk data.
            //  We know this already (does assume table was generated correctly...)
            u32 const position = m_movi.offset + entry.chunk_offset + 4;
            if (m_reader->seek(position, Core::SeekMode::SetPosition) == false) {
                warnln("Failed to seek AVI file to position 0x{:X}", position);
                return nullptr;
            }
            ByteBuffer data;
            if (m_reader->read(data, entry.chunk_length) == false) {
                warnln("Failed to read {} bytes in AVI file at position 0x{:X}", entry.chunk_length, position);
                return nullptr;
            }
            return try_create<AVISample>(position, count, data);
        }
        samples += 1;
    }
    return nullptr;
}

u32 AVIAudioTrack::samplerate() const
{
    return m_stream_list.format().audio.samples_per_sec;
}

u32 AVIAudioTrack::channel_count() const
{
    return m_stream_list.format().audio.channels;
}

AVISample::AVISample(u32 const offset, u32 const index, ByteBuffer data)
    : m_offset(offset)
    , m_index(index)
    , m_data(data)
{
}

u64 AVISample::offset() const
{
    return m_offset;
}

u32 AVISample::size() const
{
    return m_data.size();
}

u32 AVISample::index() const
{
    return m_index;
}

ByteBuffer AVISample::data()
{
    return m_data;
}

}
