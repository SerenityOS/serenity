/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Clip.h"
#include "Music.h"
#include "Processor.h"
#include <LibCore/Object.h>

namespace LibDSP {

// A track is also known as a channel and serves as a container for the audio pipeline: clips -> processors -> mixing & output
class Track : public Core::Object {
    C_OBJECT_ABSTRACT(Track)
public:
    Track(NonnullRefPtr<Transport> transport)
        : m_transport(move(transport))
    {
    }
    virtual ~Track()
    {
    }

    virtual bool check_processor_chain_valid() const = 0;
    bool add_processor(NonnullRefPtr<Processor> new_processor);

    // Creates the current signal of the track by processing current note or audio data through the processing chain
    Sample current_signal();

    NonnullRefPtrVector<Processor> const& processor_chain() const { return m_processor_chain; }
    NonnullRefPtr<Transport> const transport() const { return m_transport; }

protected:
    bool check_processor_chain_valid_with_initial_type(SignalType initial_type) const;

    // Subclasses override to provide the base signal to the processing chain
    virtual Signal current_clips_signal() = 0;

    NonnullRefPtrVector<Processor> m_processor_chain;
    NonnullRefPtr<Transport> const m_transport;
};

class NoteTrack final : public Track {
public:
    virtual ~NoteTrack()
    {
    }

    bool check_processor_chain_valid() const override;
    NonnullRefPtrVector<NoteClip> const& clips() const { return m_clips; }

protected:
    virtual Signal current_clips_signal() override;

private:
    NonnullRefPtrVector<NoteClip> m_clips;
};

class AudioTrack final : public Track {
public:
    virtual ~AudioTrack()
    {
    }

    bool check_processor_chain_valid() const override;
    NonnullRefPtrVector<AudioClip> const& clips() const { return m_clips; }

protected:
    virtual Signal current_clips_signal() override;

private:
    NonnullRefPtrVector<AudioClip> m_clips;
};

}
