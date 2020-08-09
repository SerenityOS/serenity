/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Badge.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace UserspaceEmulator {

class SoftCPU;

class MallocTracer {
public:
    MallocTracer();

    void target_did_malloc(Badge<SoftCPU>, FlatPtr address, size_t);
    void target_did_free(Badge<SoftCPU>, FlatPtr address);

    void audit_read(FlatPtr address, size_t);
    void audit_write(FlatPtr address, size_t);

    void dump_leak_report();

private:
    struct Mallocation {
        bool contains(FlatPtr a) const
        {
            return a >= address && a < (address + size);
        }

        FlatPtr address { 0 };
        size_t size { 0 };
        bool freed { false };

        Vector<FlatPtr> malloc_backtrace;
        Vector<FlatPtr> free_backtrace;
    };

    Mallocation* find_mallocation(FlatPtr);
    Mallocation* find_mallocation_before(FlatPtr);
    bool is_reachable(const Mallocation&) const;

    Vector<Mallocation> m_mallocations;

    bool m_auditing_enabled { true };
};

}
