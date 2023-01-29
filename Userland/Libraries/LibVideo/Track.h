/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashFunctions.h>
#include <AK/Traits.h>
#include <AK/Types.h>

namespace Video {

enum class TrackType : u32 {
    Video,
    Audio,
    Subtitles,
};

struct Track {
public:
    Track(TrackType type, size_t identifier)
        : m_type(type)
        , m_identifier(identifier)
    {
    }

    TrackType type() { return m_type; }
    size_t identifier() const { return m_identifier; }

    bool operator==(Track const& other) const
    {
        return m_type == other.m_type && m_identifier == other.m_identifier;
    }
    unsigned hash() const
    {
        return pair_int_hash(to_underlying(m_type), m_identifier);
    }

private:
    TrackType m_type;
    size_t m_identifier;
};

}

template<>
struct AK::Traits<Video::Track> : public GenericTraits<Video::Track> {
    static unsigned hash(Video::Track const& t) { return t.hash(); }
};
