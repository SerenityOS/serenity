/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/CircularQueue.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Tracing/ProcessTracer.h>

class ProfileTracer : public ProcessTracer {
public:
    static NonnullRefPtr<ProfileTracer> create(Process& process) { return adopt(*new ProfileTracer(process)); }
    virtual ~ProfileTracer() override;

    virtual bool have_more_items() const override;
    virtual void read_item(SimpleBufferBuilder&) const override;
    virtual void dequeue_item() override;

    virtual bool is_profile_tracer() const override { return true; }

    constexpr static size_t max_stack_frame_count = 30;

    struct Sample {
        i32 pid;
        i32 tid;
        u64 timestamp;
        u32 frames[max_stack_frame_count];
        u32 offsets[max_stack_frame_count];
        String symbolicated_frames[max_stack_frame_count];
    };

    Sample& next_sample_slot();

private:
    virtual const char* class_name() const override { return "ProfileTracer"; }
    explicit ProfileTracer(Process&);

    void symbolicate(Sample& stack) const;

    constexpr static auto buffer_size = 1 * MB;
    constexpr static auto queue_capacity = (buffer_size - sizeof(CircularQueue<Sample, 0>)) / sizeof(Sample);

    KBuffer m_buffer;
    CircularQueue<Sample, queue_capacity>& m_queue;
};
