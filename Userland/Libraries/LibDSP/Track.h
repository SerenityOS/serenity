/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/Noncopyable.h"
#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibDSP/Clip.h>
#include <LibDSP/Keyboard.h>
#include <LibDSP/Music.h>
#include <LibDSP/Processor.h>
#include <LibDSP/Transport.h>

namespace LibDSP {

// A track is also known as a channel and serves as a container for the audio pipeline: clips -> processors -> mixing & output
class Track : public Core::Object {
    C_OBJECT_ABSTRACT(Track)
    AK_MAKE_NONCOPYABLE(Track);
    AK_MAKE_NONMOVABLE(Track);

public:
    Track(NonnullRefPtr<Transport> transport)
        : m_transport(move(transport))
    {
    }
    virtual ~Track() override = default;

    virtual bool check_processor_chain_valid() const = 0;
    bool add_processor(NonnullRefPtr<Processor> new_processor);

    // Creates a range of audio samples for an audio callback by processing current note or audio data through the processing chain.
    // As this is called on the audio thread (real-time!), we can't allocate here.
    // Therefore, this callback fills the entire allocated space, i.e. capacity(), of the given vector
    virtual void compute_samples(FixedArray<Sample>& samples_to_fill) = 0;

    NonnullRefPtrVector<Processor> const& processor_chain() const { return m_processor_chain; }
    NonnullRefPtr<Transport> transport() const { return m_transport; }

    virtual void set_buffer_size(size_t) { }

protected:
    bool check_processor_chain_valid_with_initial_type(SignalType initial_type) const;
    // Again, the output signal is allocated to the correct capacity by the caller.
    // For optimization reasons, the input signal can be mutated in multiple ways here.
    template<typename InputType>
    void execute_processor_chain(InputType& input_signal, FixedArray<Sample>& output_signal) requires(IsBaseOf<FixedArray<Sample>, InputType> || IsBaseOf<RollNotes, InputType>);
    virtual SignalType input_type() const = 0;

    NonnullRefPtrVector<Processor> m_processor_chain;
    NonnullRefPtr<Transport> m_transport;
};

class NoteTrack final : public Track {
    C_OBJECT(NoteTrack)
public:
    NoteTrack(NonnullRefPtr<Transport> transport, NonnullRefPtr<Keyboard> keyboard)
        : Track(move(transport))
        , m_keyboard(move(keyboard))
    {
    }
    ~NoteTrack() override = default;

    void compute_samples(FixedArray<Sample>& samples_to_fill) override;
    bool check_processor_chain_valid() const override;
    NonnullRefPtrVector<NoteClip> const& clips() const { return m_clips; }
    RefPtr<NoteClip> current_clip() const;

protected:
    SignalType input_type() const override { return SignalType::Note; }

private:
    NonnullRefPtr<Keyboard> m_keyboard;
    NonnullRefPtrVector<NoteClip> m_clips;

    RollNotes m_currently_playing_notes;
};

class AudioTrack final : public Track {
    C_OBJECT(AudioTrack)
public:
    ~AudioTrack() override = default;

    void compute_samples(FixedArray<Sample>& samples_to_fill) override;
    bool check_processor_chain_valid() const override;
    NonnullRefPtrVector<AudioClip> const& clips() const { return m_clips; }
    // This function is not called from the audio thread, as it necessarily allocates.
    // It will ensure that the audio track has a temporary buffer of appropriate size for sample computation.
    virtual void set_buffer_size(size_t samples) override
    {
        FixedArray<Sample> new_buffer(samples);
        m_temporary_audio_buffer = new_buffer;
    }

protected:
    SignalType input_type() const override { return SignalType::Sample; }

private:
    NonnullRefPtrVector<AudioClip> m_clips;

    FixedArray<Sample> m_temporary_audio_buffer;
};

}
