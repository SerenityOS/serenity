#pragma once

#include <AK/Rational.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Tuple.h>
#include <AK/Types.h>

namespace Media::Reader {

// FIXME: properties system for tracks

class ISample : public RefCounted<ISample> {
public:
    virtual ~ISample() { }
    virtual u64 offset() const = 0;
    virtual u32 size() const = 0;
    virtual u32 index() const = 0;
    virtual ByteBuffer data() = 0;
};
using SamplePtr = RefPtr<ISample>;

class ITrack : public RefCounted<ITrack> {
public:
    virtual ~ITrack() { }
    virtual u32 index() const = 0;
    virtual double duration() const = 0;
    virtual u32 size() const = 0;
    virtual u32 bitrate() const = 0;
    virtual u32 codec() const = 0;
    virtual u32 sample_count() const = 0;
    // FIXME: sample retrieval based on something meaningful i.e. PTS/DTS/timecode/etc
    virtual SamplePtr sample(u32 const count) = 0;
};

class IVideoTrack : public ITrack {
public:
    virtual Tuple<u32, u32> dimensions() const = 0;
    // TODO: change doubles to rationals
    virtual Rational<u32> framerate() const = 0;
    virtual Rational<u32> frame_aspect_ratio() const = 0;
};
using VideoTrackPtr = RefPtr<IVideoTrack>;

class IAudioTrack : public ITrack {
public:
    virtual u32 samplerate() const = 0;
    virtual u32 channel_count() const = 0;
};
using AudioTrackPtr = RefPtr<IAudioTrack>;

class ISubtitleTrack : public ITrack {
};
using SubtitleTrackPtr = RefPtr<ISubtitleTrack>;

class IReader {
public:
    virtual ~IReader() { }
    virtual bool is_open() const = 0;
    virtual String format() const = 0;
    virtual u64 size() const = 0;
    virtual double duration() const = 0;
    virtual u32 track_count() const = 0;
    virtual u32 video_count() const = 0;
    virtual u32 audio_count() const = 0;
    virtual u32 subtitle_count() const = 0;
    virtual VideoTrackPtr video_track(u32 const index) = 0;
    virtual AudioTrackPtr audio_track(u32 const index) = 0;
    virtual SubtitleTrackPtr subtitle_track(u32 const index) = 0;
};

}
