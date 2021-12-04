/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Music.h"
#include "Transport.h"

namespace LibDSP {

RollNote RollNote::at_real_time(Transport& transport) const
{
    if (transport.loop_state() == Transport::Looping::Disabled)
        return *this;

    // If we start after the loop point, we need to be offset by the time difference of fake and real time.
    if (this->on_sample >= transport.loop_start())
        return *this + transport.time_offset();
    return *this;
}
}