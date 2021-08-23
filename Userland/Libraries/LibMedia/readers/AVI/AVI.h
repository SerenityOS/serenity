#pragma once

#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <LibMedia/readers/AVI/Header.h>
#include <LibMedia/readers/AVI/Index.h>
#include <LibMedia/readers/AVI/Info.h>
#include <LibMedia/readers/BitStreamReader.h>
#include <LibMedia/readers/IReader.h>

namespace Media::Reader::AVI {

struct Movi {
    u32 size { 0 };
    u32 offset { 0 };
};

// FIXME: have a base AVITrack to reduce duplication

class AVISample : public ISample {
public:
    AVISample(u32 const offset, u32 const index, ByteBuffer data);
    virtual u64 offset() const final;
    virtual u32 size() const final;
    virtual u32 index() const final;
    virtual ByteBuffer data() final;

private:
    u32 m_offset;
    u32 m_index;
    ByteBuffer m_data;
};

class AVIAudioTrack : public IAudioTrack {
public:
    explicit AVIAudioTrack(u32 const index, AVI::StreamList, RefPtr<IndexTable>, RefPtr<BitStreamReader>, Movi);
    // ITrack
    virtual u32 index() const final;
    virtual double duration() const final;
    virtual u32 size() const final;
    virtual u32 bitrate() const final;
    virtual u32 codec() const final;
    virtual u32 sample_count() const final;
    virtual SamplePtr sample(u32 const count) final;
    // IAudioTrack
    virtual u32 samplerate() const final;
    virtual u32 channel_count() const final;

private:
    u32 m_index;
    AVI::StreamList m_stream_list;
    u32 m_chunk_id;
    // FIXME: identify a way to use these in a less hacky manner (e.g. a parent)
    RefPtr<IndexTable> m_index_table;
    RefPtr<BitStreamReader> m_reader;
    Movi m_movi;
};

class AVIVideoTrack : public IVideoTrack {
public:
    AVIVideoTrack(u32 const index, AVI::StreamList, RefPtr<IndexTable>, RefPtr<BitStreamReader>, Movi);
    // ITrack
    virtual u32 index() const final;
    virtual double duration() const final;
    virtual u32 size() const final;
    virtual u32 bitrate() const final;
    virtual u32 codec() const final;
    virtual u32 sample_count() const final;
    virtual SamplePtr sample(u32 const count) final;
    // IVideoTrack
    virtual Tuple<u32, u32> dimensions() const final;
    virtual Rational<u32> framerate() const final;
    virtual Rational<u32> frame_aspect_ratio() const final;

private:
    u32 m_index;
    StreamList m_stream_list;
    u32 m_chunk_id;
    // FIXME: identify a way to use these in a less hacky manner (e.g. a parent)
    RefPtr<IndexTable> m_index_table;
    RefPtr<BitStreamReader> m_reader;
    Movi m_movi;
};

class AVIReader : public IReader {
public:
    explicit AVIReader(AK::String filePath);
    virtual ~AVIReader();
    virtual bool is_open() const final;
    virtual String format() const final;
    virtual u64 size() const final;
    virtual double duration() const final;
    virtual u32 track_count() const final;
    virtual u32 video_count() const final;
    virtual u32 audio_count() const final;
    virtual u32 subtitle_count() const final;
    virtual VideoTrackPtr video_track(u32 const index) final;
    virtual AudioTrackPtr audio_track(u32 const index) final;
    virtual SubtitleTrackPtr subtitle_track(u32 const index) final;

private:
    // FIXME: identify if reader needs to be atomic
    RefPtr<BitStreamReader> m_reader;
    OwnPtr<HeaderList> m_header_list;
    OwnPtr<INFO> m_info;
    // FIXME: Possible to read file without index-table. Not handled atm. Maybe generate index table by parsing movi?
    RefPtr<IndexTable> m_index_table;
    Movi m_movi;
    String m_file_path;
    u32 m_file_size { 0 };
    bool m_open { false };
};

}