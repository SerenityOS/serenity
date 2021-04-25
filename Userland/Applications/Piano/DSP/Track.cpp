/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Track.h"
#include "Processor.h"
#include <AK/Types.h>

using namespace std;

namespace LibDSP {

bool Track::add_processor(NonnullRefPtr<Processor> new_processor)
{
    m_processor_chain.append(move(new_processor));
    if (!check_processor_chain_valid()) {
        m_processor_chain.take_last();
        return false;
    }
    return true;
}

bool Track::check_processor_chain_valid_with_initial_type(SignalType initial_type) const
{
    RefPtr<Processor> prev_processor = nullptr;
    for (auto processor = m_processor_chain.begin(); !processor.is_end(); ++processor) {
        if (prev_processor.is_null() && processor->input_type() != initial_type) {
            return false;
        }
        if (prev_processor->output_type() != processor->input_type()) {
            return false;
        }
        prev_processor = *processor;
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
    Signal the_signal = current_clips_signal();
    for (auto& processor : m_processor_chain) {
        the_signal = processor.process(the_signal);
    }
    VERIFY(the_signal.type == SignalType::Sample);
    return the_signal.audio;
}

Signal NoteTrack::current_clips_signal()
{
    u32 time = m_transport->time();
    RefPtr<NoteClip> playing_clip = nullptr;
    for (auto& clip : m_clips) {
        if (clip.start() <= time && clip.end() >= time) {
            playing_clip = clip;
            break;
        }
    }
    if (playing_clip.is_null()) {
        return Signal(Vector<RollNote>());
    }

    Vector<RollNote> playing_notes;
    // FIXME: performance?
    for (auto& note_list : playing_clip->m_notes) {
        if (!note_list.is_empty())
            for (auto& note : note_list) {
                if (note.on_sample >= time && note.off_sample >= time) {
                    break;
                }
                if (note.on_sample <= time && note.off_sample >= time) {
                    // FIXME: Is this copying the note?
                    playing_notes.append(note);
                }
            }
    }
    return playing_notes;
}

Signal AudioTrack::current_clips_signal()
{
    u32 time = m_transport->time();
    RefPtr<AudioClip> playing_clip = nullptr;
    for (auto& clip : m_clips) {
        if (clip.start() <= time && clip.end() >= time) {
            playing_clip = clip;
            break;
        }
    }
    if (playing_clip.is_null()) {
        return Signal(SAMPLE_OFF);
    }

    u32 effective_sample = time - playing_clip->start();
    return Signal(playing_clip->sample_at(effective_sample));
}

}
