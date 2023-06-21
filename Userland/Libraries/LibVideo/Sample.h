/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Time.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>

namespace Video {

class Sample {
public:
    virtual ~Sample() = default;

    virtual bool is_video_sample() const { return false; }
};

class VideoSample : public Sample {
public:
    VideoSample(ReadonlyBytes data, CodingIndependentCodePoints container_cicp, Duration timestamp)
        : m_data(data)
        , m_container_cicp(container_cicp)
        , m_timestamp(timestamp)
    {
    }

    bool is_video_sample() const override { return true; }
    ReadonlyBytes const& data() const { return m_data; }
    CodingIndependentCodePoints container_cicp() const { return m_container_cicp; }
    Duration timestamp() const { return m_timestamp; }

private:
    ReadonlyBytes m_data;
    CodingIndependentCodePoints m_container_cicp;
    Duration m_timestamp;
};

// FIXME: Add samples for audio, subtitles, etc.

}
