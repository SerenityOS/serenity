/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Array.h>
#include <AK/Assertions.h>
#include <AK/Optional.h>
#include <AK/TypedTransfer.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>
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

RefPtr<NoteClip> NoteTrack::current_clip() const
{
    for (auto const& clip : m_clips) {
        if (clip.start() <= m_transport->time() && clip.end() >= m_transport->time())
            return clip;
    }
    return {};
}

bool Track::check_processor_chain_valid_with_initial_type(SignalType initial_type) const
{
    Processor const* previous_processor = nullptr;
    for (auto const& processor : m_processor_chain) {
        // The first processor must have the given initial signal type as input.
        if (previous_processor == nullptr) {
            if (processor.is_valid_input_type(initial_type))
                return false;
        } else if (processor.is_valid_input_type(previous_processor->output_type()))
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

template<typename InputType>
void Track::execute_processor_chain(InputType& input_signal, FixedArray<Sample>& output_signal) requires(IsBaseOf<FixedArray<Sample>, InputType> || IsBaseOf<RollNotes, InputType>)
{
    if (m_processor_chain.is_empty()) {
        if constexpr (IsBaseOf<FixedArray<Sample>, InputType>)
            output_signal = input_signal;
        return;
    }
    // All processing is done on the output signal.
    if constexpr (IsBaseOf<FixedArray<Sample>, InputType>)
        output_signal = input_signal;

    for (auto& processor : m_processor_chain) {
        switch (processor.type()) {
        case Processor::ProcessorType::AudioEffect: {
            EffectProcessor& effect = static_cast<EffectProcessor&>(processor);
            effect.process(output_signal);
            break;
        }
        case Processor::ProcessorType::Synthesizer: {
            SynthesizerProcessor& synth = static_cast<SynthesizerProcessor&>(processor);
            // FIXME: This only works if we have a note track.
            if constexpr (IsBaseOf<RollNotes, InputType>)
                synth.process(input_signal, output_signal);
            else
                TODO();
            break;
        }
        case Processor::ProcessorType::Invalid:
        default:
            VERIFY_NOT_REACHED();
        }
    }
}
template void Track::execute_processor_chain(RollNotes&, FixedArray<Sample>&);
template void Track::execute_processor_chain(FixedArray<Sample>&, FixedArray<Sample>&);

void NoteTrack::compute_samples(FixedArray<Sample>& samples_to_fill)
{
    // For computing notes, we use the looping time.
    auto time = m_transport->looping_time();

    m_currently_playing_notes.clear_with_capacity();
    for (size_t i = 0; i < m_clips.size(); ++i) {
        auto& clip = m_clips[i];
        if (clip.start() <= time && clip.end() >= time) {
            for (auto& note_list : clip.notes()) {
                for (auto& note : note_list) {
                    if (note.is_playing(time))
                        m_currently_playing_notes.set(note.pitch, note.at_real_time(*m_transport));
                }
            }
        }
    }

    for (auto& keyboard_note : *m_keyboard->notes())
        m_currently_playing_notes.set(keyboard_note.key, keyboard_note.value);

    if (m_currently_playing_notes.size() == 0)
        return;

    execute_processor_chain(m_currently_playing_notes, samples_to_fill);
}

void AudioTrack::compute_samples(FixedArray<Sample>& samples_to_fill)
{
    u32 time = m_transport->looping_time();
    // Find the currently playing clip(s).
    // FIXME: Could we have more than 32 clips in a single sample compute?
    Array<size_t, 32> clip_indices;
    size_t clip_count = 0;
    for (size_t i = 0; i < m_clips.size(); ++i) {
        auto& clip = m_clips[i];
        if (clip.start() <= time && clip.end() >= time) {
            clip_indices[clip_count++] = i;
        }
    }
    if (clip_count == 0)
        return;

    m_temporary_audio_buffer.clear_with_capacity();
    // Progressively copy the clip sample data into the temporary audio buffer.
    size_t current_sample = 0;
    for (size_t i = 0; i < clip_count; ++i) {
        if (current_sample >= m_temporary_audio_buffer.size())
            break;
        auto& clip = m_clips[i];
        // If the clip extends past the beginning of the current area, we index somewhere into the middle of the clip.
        // If the clip starts at or after the beginning of the current area, we start at 0.
        u32 clip_copy_start;
        u32 buffer_copy_start;
        if (clip.start() < time) {
            clip_copy_start = time - clip.start();
            buffer_copy_start = 0;
        } else {
            clip_copy_start = 0;
            buffer_copy_start = clip.start() - time;
        }
        size_t amount_to_transfer = min(clip.length() - clip_copy_start, m_temporary_audio_buffer.size() - buffer_copy_start);

        AK::TypedTransfer<Sample>::copy(
            m_temporary_audio_buffer.span().offset(buffer_copy_start), clip.sample_data_at(clip_copy_start),
            amount_to_transfer);
    }

    execute_processor_chain(m_temporary_audio_buffer, samples_to_fill);
}
}
