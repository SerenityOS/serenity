/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FixedArray.h>
#include <AK/NoAllocationGuard.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/TypedTransfer.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>
#include <LibDSP/Processor.h>
#include <LibDSP/Track.h>
#include <unistd.h>

namespace DSP {

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
            if (processor->input_type() != initial_type)
                return false;
        } else if (previous_processor->output_type() != processor->input_type())
            return false;
        previous_processor = processor.ptr();
    }
    return true;
}

NonnullRefPtr<Synthesizers::Classic> Track::synth()
{
    return static_ptr_cast<Synthesizers::Classic>(m_processor_chain[0]);
}
NonnullRefPtr<Effects::Delay> Track::delay()
{
    return static_ptr_cast<Effects::Delay>(m_processor_chain[1]);
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
    m_secondary_sample_buffer = TRY(FixedArray<Sample>::create(buffer_size));
    FixedArray<Sample> cache = TRY(FixedArray<Sample>::create(buffer_size));
    bool false_variable = false;
    while (!m_sample_lock.compare_exchange_strong(false_variable, true))
        usleep(1);
    m_cached_sample_buffer.swap(cache);
    m_sample_lock.store(false);
    return {};
}

void Track::current_signal(FixedArray<Sample>& output_signal)
{
    // This is real-time code. We must NEVER EVER EVER allocate.
    NoAllocationGuard guard;
    VERIFY(m_secondary_sample_buffer.type() == SignalType::Sample);
    VERIFY(output_signal.size() == m_secondary_sample_buffer.get<FixedArray<Sample>>().size());

    compute_current_clips_signal();
    Signal* source_signal = &m_current_signal;
    // This provides an audio buffer of the right size. It is not allocated here, but whenever we are informed about a buffer size change.
    Signal* target_signal = &m_secondary_sample_buffer;

    for (auto& processor : m_processor_chain) {
        // Depending on what the processor needs to have as output, we need to place either a pre-allocated note hash map or a pre-allocated sample buffer in the target signal.
        if (processor->output_type() == SignalType::Note)
            target_signal = &m_secondary_note_buffer;
        else
            target_signal = &m_secondary_sample_buffer;
        processor->process(*source_signal, *target_signal);
        swap(source_signal, target_signal);
    }
    VERIFY(source_signal->type() == SignalType::Sample);
    VERIFY(output_signal.size() == source_signal->get<FixedArray<Sample>>().size());
    // The last processor is the fixed mastering processor. This can write directly to the output data. We also just trust this processor that it does the right thing :^)
    m_track_mastering->process_to_fixed_array(*source_signal, output_signal);

    bool false_variable = false;
    if (m_sample_lock.compare_exchange_strong(false_variable, true)) {
        AK::TypedTransfer<Sample>::copy(m_cached_sample_buffer.data(), output_signal.data(), m_cached_sample_buffer.size());
        m_sample_lock.store(false);
    }
}

void Track::write_cached_signal_to(Span<Sample> output_signal)
{
    bool false_variable = false;
    while (!m_sample_lock.compare_exchange_strong(false_variable, true)) {
        usleep(1);
    }
    VERIFY(output_signal.size() == m_cached_sample_buffer.size());

    AK::TypedTransfer<Sample>::copy(output_signal.data(), m_cached_sample_buffer.data(), m_cached_sample_buffer.size());

    m_sample_lock.store(false);
}

void NoteTrack::compute_current_clips_signal()
{
    // FIXME: Handle looping properly
    u32 start_time = m_transport->time();
    VERIFY(m_secondary_sample_buffer.type() == SignalType::Sample);
    size_t sample_count = m_secondary_sample_buffer.get<FixedArray<Sample>>().size();
    u32 end_time = start_time + static_cast<u32>(sample_count);

    // Find the currently playing clips.
    // We can't handle more than 32 playing clips at a time, but that is a ridiculous number.
    Array<RefPtr<NoteClip>, 32> playing_clips;
    size_t playing_clips_index = 0;
    for (auto& clip : m_clips) {
        // A clip is playing if its start time or end time fall in the current time range.
        // Or, if they both enclose the current time range.
        if ((clip->start() <= start_time && clip->end() >= end_time)
            || (clip->start() >= start_time && clip->start() < end_time)
            || (clip->end() > start_time && clip->end() <= end_time)) {
            VERIFY(playing_clips_index < playing_clips.size());
            playing_clips[playing_clips_index++] = clip;
        }
    }

    auto& current_notes = m_current_signal.get<RollNotes>();
    m_current_signal.get<RollNotes>().fill({});

    if (playing_clips_index == 0)
        return;

    for (auto const& playing_clip : playing_clips) {
        if (playing_clip.is_null())
            break;
        for (auto const& note : playing_clip->notes()) {
            if (note.is_playing_during(start_time, end_time))
                current_notes[note.pitch] = note;
        }
    }

    for (auto const& keyboard_note : m_keyboard->notes()) {
        if (!keyboard_note.has_value() || !keyboard_note->is_playing_during(start_time, end_time))
            continue;
        // Always overwrite roll notes with keyboard notes.
        current_notes[keyboard_note->pitch] = keyboard_note;
    }
}

void AudioTrack::compute_current_clips_signal()
{
    // This is quite involved as we need to look at multiple clips and take looping into account.
    TODO();
}

Optional<RollNote> NoteTrack::note_at(u32 time, u8 pitch) const
{
    for (auto& clip : m_clips) {
        if (time >= clip->start() && time <= clip->end())
            return clip->note_at(time, pitch);
    }

    return {};
}

void NoteTrack::set_note(RollNote note)
{
    for (auto& clip : m_clips) {
        if (clip->start() <= note.on_sample && clip->end() >= note.on_sample)
            clip->set_note(note);
    }
}

void NoteTrack::remove_note(RollNote note)
{
    for (auto& clip : m_clips)
        clip->remove_note(note);
}

void NoteTrack::add_clip(u32 start_time, u32 end_time)
{
    m_clips.append(AK::make_ref_counted<NoteClip>(start_time, end_time));
}

}
