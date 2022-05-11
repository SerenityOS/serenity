/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibDSP/Clip.h>
#include <LibDSP/Music.h>
#include <LibDSP/Processor.h>

namespace LibDSP {

// A track is also known as a channel and serves as a container for the audio pipeline: clips -> processors -> mixing & output
class Track : public RefCounted<Track> {
public:
    virtual ~Track() = default;

    virtual bool check_processor_chain_valid() const = 0;
    bool add_processor(NonnullRefPtr<Processor> new_processor);

    // Creates the current signal of the track by processing current note or audio data through the processing chain.
    void current_signal(FixedArray<Sample>& output_signal);

    // We are informed of an audio buffer size change. This happens off-audio-thread so we can allocate.
    ErrorOr<void> resize_internal_buffers_to(size_t buffer_size);

    NonnullRefPtrVector<Processor> const& processor_chain() const { return m_processor_chain; }
    NonnullRefPtr<Transport const> transport() const { return m_transport; }

protected:
    Track(NonnullRefPtr<Transport> transport)
        : m_transport(move(transport))
    {
    }
    bool check_processor_chain_valid_with_initial_type(SignalType initial_type) const;

    // Subclasses override to provide the base signal to the processing chain
    virtual void compute_current_clips_signal() = 0;

    NonnullRefPtrVector<Processor> m_processor_chain;
    NonnullRefPtr<Transport> m_transport;
    // The current signal is stored here, to prevent unnecessary reallocation.
    Signal m_current_signal { FixedArray<Sample> {} };

    // These are so that we don't have to allocate a secondary buffer in current_signal().
    // A sample buffer possibly used by the processor chain.
    Signal m_secondary_sample_buffer { FixedArray<Sample> {} };
    // A note buffer possibly used by the processor chain.
    Signal m_secondary_note_buffer { RollNotes {} };
};

class NoteTrack final : public Track {
public:
    virtual ~NoteTrack() override = default;

    bool check_processor_chain_valid() const override;
    NonnullRefPtrVector<NoteClip> const& clips() const { return m_clips; }

protected:
    void compute_current_clips_signal() override;

private:
    NonnullRefPtrVector<NoteClip> m_clips;
};

class AudioTrack final : public Track {
public:
    virtual ~AudioTrack() override = default;

    bool check_processor_chain_valid() const override;
    NonnullRefPtrVector<AudioClip> const& clips() const { return m_clips; }

protected:
    void compute_current_clips_signal() override;

private:
    NonnullRefPtrVector<AudioClip> m_clips;
};

}
