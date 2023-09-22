/*
 * Copyright (c) 2023, Martin Janiczek <martin@janiczek.cz>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibTest/Randomized/RandomRun.h>

/* RandSource provides random bits to Generators.

   If it's live, a PRNG will be used and the random values will be recorded into
   its RandomRun.

   If it's recorded, its RandomRun will be used to "mock" the PRNG. This allows
   us to replay the generation of a particular value, and to test out
   "alternative histories": "what if the PRNG generated 0 instead of 13 here?"
 */
class RandSource {
public:
    static RandSource live() { return RandSource(RandomRun(), true); }
    static RandSource recorded(RandomRun const& run) { return RandSource(run, false); }
    RandomRun& run() { return m_run; }
    bool is_live() const { return m_is_live; }

private:
    explicit RandSource(RandomRun const& run, bool is_live)
        : m_run(run)
        , m_is_live(is_live)
    {
    }
    RandomRun m_run;
    bool m_is_live;
};
