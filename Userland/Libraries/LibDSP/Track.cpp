/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/Types.h>
#include <LibDSP/Processor.h>
#include <LibDSP/Track.h>

using namespace std;

namespace LibDSP {

bool Track::add_processor(NonnullRefPtr<Processor> new_processor)
{
    m_processor_chain.append(move(new_processor));
    if (!check_processor_chain_valid()) {
        (void)m_processor_chain.take_last();
        return false;
    }
    return true;
}

bool Track::check_processor_chain_valid_with_initial_type(SignalType initial_type) const
{
    Processor const* previous_processor = nullptr;
    for (auto& processor : m_processor_chain) {
        // The first processor must have the given initial signal type as input.
        if (previous_processor == nullptr) {
            if (processor.input_type() != initial_type)
                return false;
        } else if (previous_processor->output_type() != processor.input_type())
            return false;
        previous_processor = &processor;
    }
    return true;
}

bool AudioTrack::check_processor_chain_valid() const
{
    return check_processor_chain_valid_with_initial_type(SignalType::Sample);
}

bool NoteTrack::check_processor_chain_valid() const
{
    return check_processor_chain_valid_with_initial_type(SignalType::Note);
}

Sample Track::current_signal()
{
    compute_current_clips_signal();
    Optional<Signal> the_signal;

    for (auto& processor : m_processor_chain) {
        the_signal = processor.process(the_signal.value_or(m_current_signal));
    }
    VERIFY(the_signal.has_value() && the_signal->type() == SignalType::Sample);
    return the_signal->get<Sample>();
}

void NoteTrack::compute_current_clips_signal()
{
    u32 time = m_transport->time();
    // Find the currently playing clip.
    NoteClip* playing_clip = nullptr;
    for (auto& clip : m_clips) {
        if (clip.start() <= time && clip.end() >= time) {
            playing_clip = &clip;
            break;
        }
    }

    auto& current_notes = m_current_signal.get<RollNotes>();
    m_current_signal.get<RollNotes>().clear_with_capacity();

    if (playing_clip == nullptr)
        return;

    // FIXME: performance?
    for (auto& note_list : playing_clip->notes()) {
        for (auto& note : note_list) {
            if (note.on_sample >= time && note.off_sample >= time)
                break;
            if (note.on_sample <= time && note.off_sample >= time)
                current_notes.set(note.pitch, note);
        }
    }
}

void AudioTrack::compute_current_clips_signal()
{
    // Find the currently playing clip.
    u32 time = m_transport->time();
    AudioClip* playing_clip = nullptr;
    for (auto& clip : m_clips) {
        if (clip.start() <= time && clip.end() >= time) {
            playing_clip = &clip;
            break;
        }
    }
    if (playing_clip == nullptr) {
        m_current_signal = Signal(static_cast<Sample const&>(SAMPLE_OFF));
    }

    // Index into the clip's samples.
    u32 effective_sample = time - playing_clip->start();
    m_current_signal = Signal(playing_clip->sample_at(effective_sample));
}

}
