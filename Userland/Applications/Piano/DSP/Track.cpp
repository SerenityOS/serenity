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
        if (prev_processor.is_null() &&
            processor->input_type() != initial_type) {
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

}
