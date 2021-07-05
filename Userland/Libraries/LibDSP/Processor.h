/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include "ProcessorParameter.h"
#include "Transport.h"
#include <AK/Noncopyable.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <LibCore/Object.h>

namespace LibDSP {

class Processor : public Core::Object {
    C_OBJECT(Processor);
    AK_MAKE_NONCOPYABLE(Processor);
    AK_MAKE_NONMOVABLE(Processor);

public:
    virtual ~Processor()
    {
    }
    Signal process(Signal const& input_signal)
    {
        VERIFY(input_signal.type == m_input_type);
        auto processed = process_impl(input_signal);
        VERIFY(processed.type == m_output_type);
        return processed;
    }
    SignalType input_type() const { return m_input_type; }
    SignalType output_type() const { return m_output_type; }
    NonnullRefPtrVector<ProcessorParameter> parameters() const { return m_parameters; }

private:
    virtual Signal process_impl(Signal const& input_signal);
    const SignalType m_input_type;
    const SignalType m_output_type;

protected:
    Processor(NonnullRefPtr<Transport> transport, SignalType input_type, SignalType output_type)
        : m_input_type(input_type)
        , m_output_type(output_type)
        , m_transport(transport)
    {
    }
    NonnullRefPtr<Transport> m_transport;
    const NonnullRefPtrVector<ProcessorParameter> m_parameters;
};

}
