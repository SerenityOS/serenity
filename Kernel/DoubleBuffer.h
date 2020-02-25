/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Types.h>
#include <Kernel/KBuffer.h>
#include <Kernel/Lock.h>

namespace Kernel {

class DoubleBuffer {
public:
    explicit DoubleBuffer(size_t capacity = 65536);

    ssize_t write(const u8*, ssize_t);
    ssize_t read(u8*, ssize_t);

    bool is_empty() const { return m_empty; }

    size_t space_for_writing() const { return m_space_for_writing; }

private:
    void flip();
    void compute_lockfree_metadata();

    struct InnerBuffer {
        u8* data { nullptr };
        size_t size;
    };

    InnerBuffer* m_write_buffer { nullptr };
    InnerBuffer* m_read_buffer { nullptr };
    InnerBuffer m_buffer1;
    InnerBuffer m_buffer2;

    KBuffer m_storage;
    size_t m_capacity { 0 };
    size_t m_read_buffer_index { 0 };
    size_t m_space_for_writing { 0 };
    bool m_empty { true };
    mutable Lock m_lock { "DoubleBuffer" };
};

}
