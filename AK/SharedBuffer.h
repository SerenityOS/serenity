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

#ifdef __serenity__

#include <AK/RefCounted.h>
#include <AK/RefPtr.h>

namespace AK {

class SharedBuffer : public RefCounted<SharedBuffer> {
public:
    static RefPtr<SharedBuffer> create_with_size(int);
    static RefPtr<SharedBuffer> create_from_shared_buffer_id(int);
    ~SharedBuffer();

    bool share_globally();
    bool share_with(pid_t);
    int shared_buffer_id() const { return m_shared_buffer_id; }
    void seal();
    int size() const { return m_size; }
    void* data() { return m_data; }
    const void* data() const { return m_data; }
    void set_volatile();
    [[nodiscard]] bool set_nonvolatile();

private:
    SharedBuffer(int shared_buffer_id, int size, void*);

    int m_shared_buffer_id { -1 };
    int m_size { 0 };
    void* m_data;
};

}

using AK::SharedBuffer;

#endif
