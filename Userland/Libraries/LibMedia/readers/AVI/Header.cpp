
#include <AK/Format.h>
#include <LibMedia/Manip.h>
#include <LibMedia/readers/AVI/Header.h>


namespace Media::Reader::AVI {

constexpr auto VIDEO_TAG = fourcc_to_u32("vids");
constexpr auto AUDIO_TAG = fourcc_to_u32("auds");
constexpr auto SUBTITLE_TAG = fourcc_to_u32("txts");

// FIXME: find where this came from as it's possible to be used as enum instead
constexpr u16 WAVE_FORMAT_PCM = 0x0001;
constexpr u16 WAVE_FORMAT_IEEE_FLOAT = 0x0003;
constexpr u32 WAVE_FORMAT_ALAW = 0x0006;
constexpr u32 WAVE_FORMAT_MULAW = 0x0007;
constexpr u32 WAVE_FORMAT_EXTENSIBLE = 0xFFFE;

void data_to_stream_header(u32 const offset, ByteBuffer const& data, StreamHeader&);

void data_to_stream_format(StreamType const, u32 const offset, ByteBuffer const& data, StreamFormat&);

void data_to_video_properties(u32 const offset, ByteBuffer const& data, VideoProperties&);

void data_to_avi_header(u32 const offset, ByteBuffer const& data, AVIHeader&);

HeaderList::HeaderList(u32 const offset, ByteBuffer const& data)
    : m_offset(offset)
{
    if (bytes_u32_to_fourcc(data.slice(0, 4)) != fourcc_to_u32("avih")) {
        warnln("missing hdrl:avih -- found '{}'", u32_to_fourcc(bytes_to_u32le(data.slice(0, 4))));
        return;
    }
    auto const chk_size = bytes_to_u32le(data.slice(4, 4));
    data_to_avi_header(offset + 8, data.slice(8, chk_size), m_avi_header);

    auto data_offset = 8 + chk_size;

    // Read all stream lists in header
    while (data_offset < (data.size() - 4)) {
        auto const item_offset = m_offset + data_offset;
        auto const id = bytes_u32_to_fourcc(data.slice(data_offset, 4));
        auto const size = bytes_to_u32le(data.slice(data_offset + 4, 4));
        data_offset += 8;

        if (id == fourcc_to_u32("LIST")) {
            auto const chk_type = bytes_u32_to_fourcc(data.slice(data_offset, 4));
            if (chk_type == fourcc_to_u32("strl")) {
                m_streams.append(make<StreamList>(item_offset + 12, data.slice(data_offset + 4, size)));
            }
            data_offset += size;
        }
    }
}

StreamList::StreamList(StreamList const& original)
    : m_offset(original.m_offset)
    , m_strh(original.m_strh)
    , m_strf(original.m_strf)
    , m_vprp(original.m_vprp)
{
}

u32 HeaderList::offset() const
{
    return m_offset;
}

AVIHeader const& HeaderList::avi_header() const
{
    VERIFY(m_avi_header.offset != 0);
    return m_avi_header;
}

u32 HeaderList::stream_count() const
{
    return m_streams.size();
}

StreamList const& HeaderList::stream(u32 const index) const
{
    return *m_streams.at(index);
}

Vector<NonnullOwnPtr<StreamList>> const& HeaderList::streams() const
{
    return m_streams;
}

StreamList::StreamList(u32 const offset, ByteBuffer const& data)
    : m_offset(offset)
{
    u32 data_offset = 0;
    while (data_offset < (data.size() - 4)) {
        auto const id = bytes_u32_to_fourcc(data.slice(data_offset, 4));
        data_offset += 4;
        auto const size = bytes_u32_to_fourcc(data.slice(data_offset, 4));
        data_offset += 4;
        switch (id) {
        case fourcc_to_u32("strh"):
            data_to_stream_header(data_offset, data.slice(data_offset, size), m_strh);
            break;
        case fourcc_to_u32("strf"):
            // StreamFormat depends on StreamHeader being read first. It should be first in data.
            VERIFY(m_strh.offset != 0);
            data_to_stream_format(type(), data_offset, data.slice(data_offset, size), m_strf);
            break;
        case fourcc_to_u32("vprp"): {
            VideoProperties video_properties;
            data_to_video_properties(data_offset, data.slice(data_offset, size), video_properties);
            m_vprp = video_properties;
        } break;
        case fourcc_to_u32("JUNK"):
            // Don't care
            break;
        default:
            warnln("Unhandled strl: {}, size: {}", u32_to_fourcc(id), size);
            break;
        }
        data_offset += size;
    }

    VERIFY(m_strh.offset != 0);
}

StreamType StreamList::type() const
{
    switch (m_strh.fcc_type) {
    case VIDEO_TAG:
        return StreamType::VIDEO;
    case AUDIO_TAG:
        return StreamType::AUDIO;
    case SUBTITLE_TAG:
        return StreamType::SUBTITLE;
    default:
        return StreamType::UNKNOWN;
    }
}

StreamHeader const& StreamList::header() const
{
    return m_strh;
}

StreamFormat const& StreamList::format() const
{
    return m_strf;
}

Optional<VideoProperties> StreamList::video_properties() const
{
    return m_vprp;
}

void data_to_stream_header(u32 const offset, ByteBuffer const& data, StreamHeader& header)
{
    VERIFY(data.size() >= 48);
    header.offset = offset;
    header.fcc_type = bytes_to_u32le(data.slice(0, 4));
    header.fcc_handler = bytes_to_u32le(data.slice(4, 4));
    header.flags = bytes_to_u32le(data.slice(8, 4));
    header.priority = bytes_to_u16le(data.slice(12, 2));
    header.language = bytes_to_u16le(data.slice(14, 2));
    header.initial_frames = bytes_to_u32le(data.slice(16, 4));
    header.scale = bytes_to_u32le(data.slice(20, 4));
    header.rate = bytes_to_u32le(data.slice(24, 4));
    header.start = bytes_to_u32le(data.slice(28, 4));
    header.length = bytes_to_u32le(data.slice(32, 4));
    header.suggested_buffer_size = bytes_to_u32le(data.slice(36, 4));
    header.quality = static_cast<i32>(bytes_to_u32le(data.slice(40, 4)));
    header.sample_size = bytes_to_u32le(data.slice(44, 4));
}

void data_to_stream_format(StreamType const stream_type, u32 const offset, ByteBuffer const& data, StreamFormat& format)
{
    switch (stream_type) {
    case StreamType::AUDIO:
        VERIFY(data.size() >= 16);
        format.audio.offset = offset;
        format.audio.format_tag = bytes_to_u16le(data.slice(0, 2));
        format.audio.channels = bytes_to_u16le(data.slice(2, 2));
        format.audio.samples_per_sec = bytes_to_u32le(data.slice(4, 4));
        format.audio.avg_bytes_per_sec = bytes_to_u32le(data.slice(8, 4));
        format.audio.block_align = bytes_to_u16le(data.slice(12, 4));
        format.audio.bits_per_sample = bytes_to_u16le(data.slice(14, 2));
        if (format.audio.format_tag != WAVE_FORMAT_PCM) {
            VERIFY(data.size() >= 18);
            // Size is not in data for PCM
            format.audio.size = bytes_to_u16le(data.slice(16, 2));
        } else {
            format.audio.size = 0;
        }
        break;
    case StreamType::VIDEO:
        VERIFY(data.size() >= 40);
        format.video.offset = offset;
        format.video.size = bytes_to_u32le(data.slice(0, 4));
        format.video.width = bytes_to_u32le(data.slice(4, 4));
        format.video.height = bytes_to_u32le(data.slice(8, 4));
        format.video.planes = bytes_to_u16le(data.slice(12, 4));
        format.video.bit_count = bytes_to_u16le(data.slice(14, 4));
        format.video.compression = bytes_to_u32le(data.slice(16, 4));
        format.video.size_image = bytes_to_u32le(data.slice(20, 4)); // FIXME: check endianess of size_image
        format.video.xpels_per_meter = bytes_to_u32le(data.slice(24, 4));
        format.video.ypels_per_meter = bytes_to_u32le(data.slice(28, 4));
        format.video.clr_used = bytes_to_u32le(data.slice(32, 4));
        format.video.clr_important = bytes_to_u32le(data.slice(36, 4));
        break;
    default:
        warnln("StreamFormat type unhandled");
        break;
    }
}

void data_to_video_properties(u32 const offset, ByteBuffer const& data, VideoProperties& video_properties)
{
    VERIFY(data.size() >= 36);
    video_properties.offset = offset;
    video_properties.format_token = bytes_to_u32le(data.slice(0, 4));
    video_properties.standard = bytes_to_u32le(data.slice(4, 4));
    video_properties.vertical_refresh_rate = bytes_to_u32le(data.slice(8, 4));
    video_properties.h_total_in_t = bytes_to_u32le(data.slice(12, 4));
    video_properties.v_total_in_lines = bytes_to_u32le(data.slice(16, 4));
    video_properties.frame_aspect_ratio = bytes_to_u32le(data.slice(20, 4));
    video_properties.frame_width_in_pixels = bytes_to_u32le(data.slice(24, 4));
    video_properties.frame_height_in_lines = bytes_to_u32le(data.slice(28, 4));
    video_properties.field_per_frame = bytes_to_u32le(data.slice(32, 4));
}

void data_to_avi_header(u32 const offset, ByteBuffer const& data, AVIHeader& avi_header)
{
    VERIFY(data.size() >= 40);
    avi_header.offset = offset;
    avi_header.micro_sec_per_frame = bytes_to_u32le(data.slice(0, 4));
    avi_header.max_bytes_per_sec = bytes_to_u32le(data.slice(4, 4));
    avi_header.padding_granularity = bytes_to_u32le(data.slice(8, 4));
    avi_header.flags = bytes_to_u32le(data.slice(12, 4));
    avi_header.total_frames = bytes_to_u32le(data.slice(16, 4));
    avi_header.initial_frames = bytes_to_u32le(data.slice(20, 4));
    avi_header.streams = bytes_to_u32le(data.slice(24, 4));
    avi_header.suggested_buffer_size = bytes_to_u32le(data.slice(28, 4));
    avi_header.width = bytes_to_u32le(data.slice(32, 4));
    avi_header.height = bytes_to_u32le(data.slice(36, 4));
}

}
