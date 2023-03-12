/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DisjointChunks.h>
#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Weakable.h>
#include <LibDSP/Clip.h>
#include <LibDSP/Effects.h>
#include <LibDSP/Keyboard.h>
#include <LibDSP/Music.h>
#include <LibDSP/Processor.h>
#include <LibDSP/Synthesizers.h>

namespace DSP {

// A track is also known as a channel and serves as a container for the audio pipeline: clips -> processors -> mixing & output
class Track : public RefCounted<Track>
    , public Weakable<Track> {
public:
    virtual ~Track() = default;

    virtual bool check_processor_chain_valid() const = 0;
    bool add_processor(NonnullRefPtr<Processor> new_processor);

    // Creates the current signal of the track by processing current note or audio data through the processing chain.
    void current_signal(FixedArray<Sample>& output_signal);

    void write_cached_signal_to(Span<Sample> output_signal);

    // We are informed of an audio buffer size change. This happens off-audio-thread so we can allocate.
    ErrorOr<void> resize_internal_buffers_to(size_t buffer_size);

    Vector<NonnullRefPtr<Processor>> const& processor_chain() const { return m_processor_chain; }
    NonnullRefPtr<Transport const> transport() const { return m_transport; }
    NonnullRefPtr<DSP::Effects::Mastering> track_mastering() { return m_track_mastering; }

    // FIXME: These two getters are temporary until we have dynamic processor UI
    NonnullRefPtr<Synthesizers::Classic> synth();
    NonnullRefPtr<Effects::Delay> delay();

protected:
    Track(NonnullRefPtr<Transport> transport, NonnullRefPtr<Keyboard> keyboard)
        : m_transport(move(transport))
        , m_track_mastering(make_ref_counted<Effects::Mastering>(m_transport))
        , m_keyboard(move(keyboard))
    {
    }
    bool check_processor_chain_valid_with_initial_type(SignalType initial_type) const;

    // Subclasses override to provide the base signal to the processing chain
    virtual void compute_current_clips_signal() = 0;

    Vector<NonnullRefPtr<Processor>> m_processor_chain;
    NonnullRefPtr<Transport> m_transport;
    NonnullRefPtr<Effects::Mastering> m_track_mastering;
    NonnullRefPtr<Keyboard> m_keyboard;
    // The current signal is stored here, to prevent unnecessary reallocation.
    Signal m_current_signal { FixedArray<Sample> {} };

    // These are so that we don't have to allocate a secondary buffer in current_signal().
    // A sample buffer possibly used by the processor chain.
    Signal m_secondary_sample_buffer { FixedArray<Sample> {} };
    // A note buffer possibly used by the processor chain.
    Signal m_secondary_note_buffer { RollNotes {} };

private:
    Atomic<bool> m_sample_lock;

    FixedArray<Sample> m_cached_sample_buffer = {};
};

class NoteTrack final : public Track {
public:
    virtual ~NoteTrack() override = default;

    NoteTrack(NonnullRefPtr<Transport> transport, NonnullRefPtr<Keyboard> keyboard)
        : Track(move(transport), move(keyboard))
    {
        m_current_signal = RollNotes {};
    }

    bool check_processor_chain_valid() const override;
    ReadonlySpan<NonnullRefPtr<NoteClip>> notes() const { return m_clips.span(); }

    Optional<RollNote> note_at(u32 time, u8 pitch) const;
    void set_note(RollNote note);
    void remove_note(RollNote note);

    void add_clip(u32 start_time, u32 end_time);

protected:
    void compute_current_clips_signal() override;

private:
    Vector<NonnullRefPtr<NoteClip>> m_clips;
};

class AudioTrack final : public Track {
public:
    virtual ~AudioTrack() override = default;

    AudioTrack(NonnullRefPtr<Transport> transport, NonnullRefPtr<Keyboard> keyboard)
        : Track(move(transport), move(keyboard))
    {
    }

    bool check_processor_chain_valid() const override;
    Vector<NonnullRefPtr<AudioClip>> const& clips() const { return m_clips; }

protected:
    void compute_current_clips_signal() override;

private:
    Vector<NonnullRefPtr<AudioClip>> m_clips;
};

}
