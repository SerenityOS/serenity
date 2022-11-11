/*
 * Copyright (c) 2021, Hunter Salyer <thefalsehonesty@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>

namespace Video::Matroska {

struct EBMLHeader {
    String doc_type;
    u32 doc_type_version;
};

class SegmentInformation {
public:
    u64 timestamp_scale() const { return m_timestamp_scale; }
    void set_timestamp_scale(u64 timestamp_scale) { m_timestamp_scale = timestamp_scale; }
    Utf8View muxing_app() const { return Utf8View(m_muxing_app); }
    void set_muxing_app(String muxing_app) { m_muxing_app = move(muxing_app); }
    Utf8View writing_app() const { return Utf8View(m_writing_app); }
    void set_writing_app(String writing_app) { m_writing_app = move(writing_app); }
    Optional<double> duration() const { return m_duration; }
    void set_duration(double duration) { m_duration.emplace(duration); }

private:
    u64 m_timestamp_scale { 1'000'000 };
    String m_muxing_app;
    String m_writing_app;
    Optional<double> m_duration;
};

class TrackEntry {
public:
    enum TrackType : u8 {
        Invalid = 0,
        Video = 1,
        Audio = 2,
        Complex = 3,
        Logo = 16,
        Subtitle = 17,
        Buttons = 18,
        Control = 32,
        Metadata = 33,
    };

    enum class ColorRange : u8 {
        Unspecified = 0,
        Broadcast = 1,
        Full = 2,
        UseCICP = 3, // defined by MatrixCoefficients / TransferCharacteristics
    };

    struct ColorFormat {
        ColorPrimaries color_primaries = ColorPrimaries::Unspecified;
        TransferCharacteristics transfer_characteristics = TransferCharacteristics::Unspecified;
        MatrixCoefficients matrix_coefficients = MatrixCoefficients::Unspecified;
        u64 bits_per_channel = 0;
        ColorRange range = ColorRange::Unspecified;

        CodingIndependentCodePoints to_cicp() const
        {
            Video::ColorRange color_range;
            switch (range) {
            case ColorRange::Full:
                color_range = Video::ColorRange::Full;
                break;
            case ColorRange::Broadcast:
                color_range = Video::ColorRange::Studio;
                break;
            case ColorRange::Unspecified:
            case ColorRange::UseCICP:
                // FIXME: Figure out what UseCICP should do here. Matroska specification did not
                //        seem to explain in the 'colour' section. When this is fixed, change
                //        replace_code_points_if_specified to match.
                color_range = Video::ColorRange::Unspecified;
                break;
            }

            return { color_primaries, transfer_characteristics, matrix_coefficients, color_range };
        }
    };

    struct VideoTrack {
        u64 pixel_width;
        u64 pixel_height;

        ColorFormat color_format;
    };

    struct AudioTrack {
        u64 channels;
        u64 bit_depth;
    };

    u64 track_number() const { return m_track_number; }
    void set_track_number(u64 track_number) { m_track_number = track_number; }
    u64 track_uid() const { return m_track_uid; }
    void set_track_uid(u64 track_uid) { m_track_uid = track_uid; }
    TrackType track_type() const { return m_track_type; }
    void set_track_type(TrackType track_type) { m_track_type = track_type; }
    FlyString language() const { return m_language; }
    void set_language(FlyString const& language) { m_language = language; }
    FlyString codec_id() const { return m_codec_id; }
    void set_codec_id(FlyString const& codec_id) { m_codec_id = codec_id; }
    Optional<VideoTrack> video_track() const
    {
        if (track_type() != Video)
            return {};
        return m_video_track;
    }
    void set_video_track(VideoTrack video_track) { m_video_track = video_track; }
    Optional<AudioTrack> audio_track() const
    {
        if (track_type() != Audio)
            return {};
        return m_audio_track;
    }
    void set_audio_track(AudioTrack audio_track) { m_audio_track = audio_track; }

private:
    u64 m_track_number { 0 };
    u64 m_track_uid { 0 };
    TrackType m_track_type { Invalid };
    FlyString m_language = "eng";
    FlyString m_codec_id;

    union {
        VideoTrack m_video_track {};
        AudioTrack m_audio_track;
    };
};

class Block {
public:
    enum Lacing : u8 {
        None = 0b00,
        XIPH = 0b01,
        FixedSize = 0b10,
        EBML = 0b11,
    };

    u64 track_number() const { return m_track_number; }
    void set_track_number(u64 track_number) { m_track_number = track_number; }
    i16 timestamp() const { return m_timestamp; }
    void set_timestamp(i16 timestamp) { m_timestamp = timestamp; }
    bool only_keyframes() const { return m_only_keyframes; }
    void set_only_keyframes(bool only_keyframes) { m_only_keyframes = only_keyframes; }
    bool invisible() const { return m_invisible; }
    void set_invisible(bool invisible) { m_invisible = invisible; }
    Lacing lacing() const { return m_lacing; }
    void set_lacing(Lacing lacing) { m_lacing = lacing; }
    bool discardable() const { return m_discardable; }
    void set_discardable(bool discardable) { m_discardable = discardable; }

    void set_frames(Vector<ReadonlyBytes>&& frames) { m_frames = move(frames); }
    ReadonlyBytes const& frame(size_t index) const { return frames()[index]; }
    u64 frame_count() const { return m_frames.size(); }
    Vector<ReadonlyBytes> const& frames() const { return m_frames; }

private:
    u64 m_track_number { 0 };
    i16 m_timestamp { 0 };
    bool m_only_keyframes { false };
    bool m_invisible { false };
    Lacing m_lacing { None };
    bool m_discardable { true };
    Vector<ReadonlyBytes> m_frames;
};

class Cluster {
public:
    u64 timestamp() const { return m_timestamp; }
    void set_timestamp(u64 timestamp) { m_timestamp = timestamp; }

private:
    u64 m_timestamp;
};

}
