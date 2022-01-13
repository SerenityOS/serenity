/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Noncopyable.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <LibCore/Object.h>
#include <LibDSP/Music.h>
#include <LibDSP/ProcessorParameter.h>
#include <LibDSP/Transport.h>

namespace LibDSP {

// A processor processes notes or audio into notes or audio. Processors are e.g. samplers, synthesizers, effects, arpeggiators etc.
class Processor : public Core::Object {
    C_OBJECT_ABSTRACT(Processor);

public:
    virtual ~Processor()
    {
    }
    Signal process(Signal const& input_signal)
    {
        VERIFY(input_signal.type() == m_input_type);
        auto processed = process_impl(input_signal);
        VERIFY(processed.type() == m_output_type);
        return processed;
    }
    SignalType input_type() const { return m_input_type; }
    SignalType output_type() const { return m_output_type; }
    Vector<ProcessorParameter&>& parameters() { return m_parameters; }

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
    virtual Signal process_impl(Signal const& input_signal) = 0;

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
