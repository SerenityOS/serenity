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

#include "CallTracer.h"
#include "Emulator.h"
#include "Report.h"
#include <AK/QuickSort.h>
#include <LibDebug/DebugInfo.h>
#include <unistd.h>

namespace UserspaceEmulator {

CallTracer::CallTracer(Emulator& emulator)
    : m_emulator(emulator)
{
}

void CallTracer::prepare_call_data()
{
    if (m_has_been_sorted)
        warn("The call data should only be prepared once!");

    m_has_been_sorted = true;

    for (auto& address : m_calls.keys()) {
        auto& call = m_calls.find(address)->value;
        m_sorted_calls.append(call);
    }

    quick_sort(m_sorted_calls, [](auto& a, auto& b) {
        return a.total_count > b.total_count;
    });
}

void CallTracer::dump_calls()
{
    if (!m_has_been_sorted)
        prepare_call_data();

    reportln("\n=={}==  \033[33;1mCalls\033[0m", getpid());

    for (auto& entry : m_sorted_calls) {
        // TODO: Symbolicate
        reportln("=={}==  {:5} {:p}", getpid(), entry.total_count, entry.called_address);
    }
}

void CallTracer::register_call(FlatPtr callee, FlatPtr caller)
{
    if (!m_calls.contains(callee))
        m_calls.set(callee, { callee, 0, {} });

    auto& call = m_calls.find(callee)->value;
    call.total_count++;

    if (!call.calls_from.contains(caller))
        call.calls_from.set(caller, 0);

    (call.calls_from.find(caller)->value)++;
}

void CallTracer::to_json(JsonObjectSerializer<StringBuilder>& serializer)
{
    if (!m_has_been_sorted)
        prepare_call_data();

    auto array = serializer.add_array("calls");
    for (auto& call : m_sorted_calls) {
        auto call_object = array.add_object();
        call_object.add("address", call.called_address);
        call_object.add("total_count", call.total_count);
        auto call_from_array = call_object.add_array("calls_to");
        for (auto& entry : call.calls_from.keys()) {
            auto& value = call.calls_from.find(entry)->value;
            auto call_entry_object = call_from_array.add_object();
            call_entry_object.add("address", entry);
            call_entry_object.add("count", value);
            call_entry_object.finish();
        }
        call_from_array.finish();
        call_object.finish();
    }
    array.finish();
}

}
