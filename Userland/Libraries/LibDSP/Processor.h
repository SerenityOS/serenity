/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Transport.h>

namespace DSP {

// A processor processes notes or audio into notes or audio. Processors are e.g. samplers, synthesizers, effects, arpeggiators etc.
class Processor : public RefCounted<Processor> {

public:
    virtual ~Processor() = default;
    void process(Signal const& input_signal, Signal& output_signal)
    {
        VERIFY(input_signal.type() == m_input_type);
        process_impl(input_signal, output_signal);
        VERIFY(output_signal.type() == m_output_type);
    }
    SignalType input_type() const { return m_input_type; }
    SignalType output_type() const { return m_output_type; }
    Vector<ProcessorParameter&>& parameters() { return m_parameters; }
    Vector<ProcessorParameter&> const& parameters() const { return m_parameters; }

private:
    SignalType const m_input_type;
    SignalType const m_output_type;

protected:
    Processor(NonnullRefPtr<Transport> transport, SignalType input_type, SignalType output_type)
        : m_input_type(input_type)
        , m_output_type(output_type)
        , m_transport(move(transport))
    {
    }
    virtual void process_impl(Signal const& input_signal, Signal& output_signal) = 0;

    NonnullRefPtr<Transport> m_transport;
    Vector<ProcessorParameter&> m_parameters;
};

// A common type of processor that changes audio data, i.e. applies an effect to it.
class EffectProcessor : public Processor {
protected:
    EffectProcessor(NonnullRefPtr<Transport> transport)
        : Processor(transport, SignalType::Sample, SignalType::Sample)
    {
    }
};

// A common type of processor that synthesizes audio from note data.
class SynthesizerProcessor : public Processor {
protected:
    SynthesizerProcessor(NonnullRefPtr<Transport> transport)
        : Processor(transport, SignalType::Note, SignalType::Sample)
    {
    }
};

}
