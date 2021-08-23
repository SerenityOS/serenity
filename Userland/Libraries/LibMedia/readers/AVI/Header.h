#pragma once

#include <AK/Optional.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibMedia/readers/BitStreamReader.h>

namespace Media::Reader::AVI {

// VIDEO_FORMAT
enum class VideoFormat {
    UNKNOWN,
    PAL_SQUARE,
    PAL_CCIR_601,
    NTSC_SQUARE,
    NTSC_CCIR_601
};

// VIDEO_FIELD_DESC
struct VideoFieldDesc {
    u32 compressed_bm_height { 0 };
    u32 compressed_bm_width { 0 };
    u32 valid_bm_height { 0 };
    u32 valid_bm_width { 0 };
    u32 valid_bm_x_offset { 0 };
    u32 valid_bm_y_offset { 0 };
    u32 video_x_offset_in_t { 0 };
    u32 video_y_valid_start_line { 0 };
};

struct BitmapInfo {
    u32 offset;
    // Defined fields
    u32 size;
    u32 width;
    u32 height;
    u32 planes;
    u32 bit_count;
    u32 compression;
    u32 size_image;
    u32 xpels_per_meter;
    u32 ypels_per_meter;
    u32 clr_used;
    u32 clr_important;
};

struct WaveFormatEx {
    u32 offset;
    // Defined fields
    u16 format_tag; // NOTE: see rfc 2361
    u16 channels;
    u32 samples_per_sec;
    u32 avg_bytes_per_sec;
    u16 block_align;
    u16 bits_per_sample;
    u16 size;
};

// hdrl:avih:strl:strf
union StreamFormat {
    WaveFormatEx audio;
    BitmapInfo video;
};

// hdrl:avih:strl:vprp
struct VideoProperties {
    u32 offset { 0 };
    // Defined fields
    u32 format_token { 0 };
    u32 standard { 0 };
    u32 vertical_refresh_rate { 0 };
    u32 h_total_in_t { 0 };
    u32 v_total_in_lines { 0 };
    u32 frame_aspect_ratio { 0 };
    u32 frame_width_in_pixels { 0 };
    u32 frame_height_in_lines { 0 };
    u32 field_per_frame { 0 };
    Vector<VideoFieldDesc> field_info;
};

// hdrl:avih:strl:strh
struct StreamHeader {
    u32 offset { 0 };
    // Defined fields in std
    u32 fcc_type { 0 };
    u32 fcc_handler { 0 };
    u32 flags { 0 };
    u16 priority { 0 };
    u16 language { 0 };
    u32 initial_frames { 0 };
    u32 scale { 0 };
    u32 rate { 0 };
    u32 start { 0 };
    u32 length { 0 };
    u32 suggested_buffer_size { 0 };
    i32 quality { 0 };
    u32 sample_size { 0 };
};

enum class StreamType {
    AUDIO,
    SUBTITLE,
    VIDEO,
    UNKNOWN
};

// hdrl:avih:strl
class StreamList {
public:
    StreamList(u32 const offset, const ByteBuffer& data);
    StreamList(StreamList const&);
    StreamType type() const;
    StreamHeader const& header() const;
    StreamFormat const& format() const;
    Optional<VideoProperties> video_properties() const;

private:
    u32 m_offset;
    StreamHeader m_strh;
    StreamFormat m_strf;
    Optional<VideoProperties> m_vprp;
};

// hdrl:avih
struct AVIHeader {
    u32 offset { 0 };
    // Defined fields
    u32 micro_sec_per_frame { 0 };
    u32 max_bytes_per_sec { 0 };
    u32 padding_granularity { 0 };
    u32 flags { 0 };
    u32 total_frames { 0 };
    u32 initial_frames { 0 };
    u32 streams { 0 };
    u32 suggested_buffer_size { 0 };
    u32 width { 0 };
    u32 height { 0 };
};

// hdrl
class HeaderList {
public:
    HeaderList(u32 const offset, ByteBuffer const& reader);
    u32 offset() const;
    AVIHeader const& avi_header() const;
    u32 stream_count() const;
    StreamList const& stream(u32 const index) const;
    Vector<NonnullOwnPtr<StreamList>> const& streams() const;

private:
    u32 m_offset;
    AVIHeader m_avi_header;
    Vector<NonnullOwnPtr<StreamList>> m_streams;
};

}
