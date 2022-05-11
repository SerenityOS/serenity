/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/NoAllocationGuard.h>
#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/TypedTransfer.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>
#include <LibDSP/Processor.h>
#include <LibDSP/Track.h>

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

ErrorOr<void> Track::resize_internal_buffers_to(size_t buffer_size)
{
    m_secondary_sample_buffer = TRY(FixedArray<Sample>::try_create(buffer_size));
    return {};
}

void Track::current_signal(FixedArray<Sample>& output_signal)
{
    // This is real-time code. We must NEVER EVER EVER allocate.
    NoAllocationGuard guard;
    VERIFY(output_signal.size() == m_secondary_sample_buffer.get<FixedArray<Sample>>().size());

    compute_current_clips_signal();
    Signal* source_signal = &m_current_signal;
    // This provides an audio buffer of the right size. It is not allocated here, but whenever we are informed about a buffer size change.
    Signal* target_signal = &m_secondary_sample_buffer;

    for (auto& processor : m_processor_chain) {
        // Depending on what the processor needs to have as output, we need to place either a pre-allocated note hash map or a pre-allocated sample buffer in the target signal.
        if (processor.output_type() == SignalType::Note)
            target_signal = &m_secondary_note_buffer;
        else
            target_signal = &m_secondary_sample_buffer;
        processor.process(*source_signal, *target_signal);
        swap(source_signal, target_signal);
    }
    VERIFY(source_signal->type() == SignalType::Sample);
    VERIFY(output_signal.size() == source_signal->get<FixedArray<Sample>>().size());
    // This is one final unavoidable memcopy. Otherwise we need to special-case the last processor or
    AK::TypedTransfer<Sample>::copy(output_signal.data(), source_signal->get<FixedArray<Sample>>().data(), output_signal.size());
}

void NoteTrack::compute_current_clips_signal()
{
    // Consider the entire time duration.
    TODO();

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
    for (auto const& note_list : playing_clip->notes()) {
        for (auto const& note : note_list) {
            if (note.on_sample >= time && note.off_sample >= time)
                break;
            if (note.on_sample <= time && note.off_sample >= time)
                current_notes.set(note.pitch, note);
        }
    }
}

void AudioTrack::compute_current_clips_signal()
{
    // This is quite involved as we need to look at multiple clips and take looping into account.
    TODO();
}

}
