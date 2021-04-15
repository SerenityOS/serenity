/*
 * Copyright (c) 2021, Tobias Christiansen <tobi@tobyase.de>
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

#include <AK/HashMap.h>
#include <AK/JsonObjectSerializer.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace UserspaceEmulator {

class Emulator;

class CallTracer {
public:
    explicit CallTracer(Emulator&);

    void register_call(FlatPtr callee, FlatPtr caller);

    void dump_calls();

    void to_json(JsonObjectSerializer<StringBuilder>& serializer);

private:
    Emulator& m_emulator;

    struct Call {
        FlatPtr called_address;
        size_t total_count;
        HashMap<FlatPtr, size_t> calls_from;
    };

    HashMap<FlatPtr, Call> m_calls {};

    bool m_has_been_sorted { false };

    void prepare_call_data();

    Vector<Call> m_sorted_calls {};
};
}
