/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FixedArray.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibCore/Object.h>
#include <LibDSP/Music.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/SignalRange.h>
#include <LibDSP/Transport.h>

namespace LibDSP {

// A processor processes notes or audio into notes or audio. Processors are e.g. samplers, synthesizers, effects, arpeggiators etc.
class Processor : public Core::Object {
    C_OBJECT_ABSTRACT(Processor);

public:
    enum class ProcessorType {
        Invalid,
        AudioEffect,
        Synthesizer,
    };

    virtual ~Processor()
    {
    }

    ProcessorType type() const { return m_type; }
    virtual bool is_valid_input_type(SignalType) const { return false; }
    virtual SignalType output_type() const { return SignalType::Invalid; }
    Vector<ProcessorParameter&>& parameters() { return m_parameters; }

private:
    ProcessorType const m_type;

protected:
    Processor(NonnullRefPtr<Transport> transport, ProcessorType type)
        : m_type(type)
        , m_transport(move(transport))
    {
    }

    NonnullRefPtr<Transport> m_transport;
    Vector<ProcessorParameter&> m_parameters;
};

// A common type of processor that changes audio data, i.e. applies an effect to it.
class EffectProcessor : public Processor {
public:
    // The effect manipulates signals, so it can work in-place, not reallocating.
    virtual void process(FixedArray<Sample>& signal) = 0;
    virtual bool is_valid_input_type(SignalType type) const override { return type == SignalType::Sample; }
    virtual SignalType output_type() const override { return SignalType::Sample; }

protected:
    EffectProcessor(NonnullRefPtr<Transport> transport)
        : Processor(move(transport), ProcessorType::AudioEffect)
    {
    }
};

// A common type of processor that synthesizes audio from note data.
class SynthesizerProcessor : public Processor {
public:
    // The synthesizer needs an output buffer, which is pre-allocated to the correct capacity.
    virtual void process(RollNotes& input_notes, FixedArray<Sample>& output_samples) = 0;
    virtual bool is_valid_input_type(SignalType type) const override { return type == SignalType::Note; }
    virtual SignalType output_type() const override { return SignalType::Sample; }

protected:
    SynthesizerProcessor(NonnullRefPtr<Transport> transport)
        : Processor(move(transport), ProcessorType::Synthesizer)
    {
    }
};

}
