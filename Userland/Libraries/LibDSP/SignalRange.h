/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibDSP/Music.h>

namespace LibDSP {

// A range of signals that are to be processed.
struct SignalRange {

    using Data = Variant<Vector<Sample>, RollNotes>;

    SignalRange(Vector<Sample>&& samples, u32 start)
        : start(start)
        , end(start + samples.capacity())
        , data(move(samples))
    {
    }
    SignalRange(RollNotes&& notes, u32 start, u32 end)
        : start(start)
        , end(end)
        , data(move(notes))
    {
    }
    SignalRange(u32 start, u32 end)
        : start(start)
        , end(end)
        , data(Vector<Sample>())
    {
        data.get<Vector<Sample>>().ensure_capacity(static_cast<size_t>(end - start));
    }

    u32 start;
    u32 end;
    Data data;
};

}
