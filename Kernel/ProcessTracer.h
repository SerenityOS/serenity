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

#include <AK/CircularQueue.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class ProcessTracer : public File {
public:
    static NonnullRefPtr<ProcessTracer> create(pid_t pid) { return adopt(*new ProcessTracer(pid)); }
    virtual ~ProcessTracer() override;

    bool is_dead() const { return m_dead; }
    void set_dead() { m_dead = true; }

    virtual bool can_read(const FileDescription&) const override { return !m_calls.is_empty() || m_dead; }
    virtual int read(FileDescription&, u8*, int) override;

    virtual bool can_write(const FileDescription&) const override { return true; }
    virtual int write(FileDescription&, const u8*, int) override { return -EIO; }

    virtual String absolute_path(const FileDescription&) const override;

    void did_syscall(u32 function, u32 arg1, u32 arg2, u32 arg3, u32 result);
    pid_t pid() const { return m_pid; }

private:
    virtual const char* class_name() const override { return "ProcessTracer"; }
    explicit ProcessTracer(pid_t);

    struct CallData {
        u32 function;
        u32 arg1;
        u32 arg2;
        u32 arg3;
        u32 result;
    };

    pid_t m_pid;
    bool m_dead { false };
    CircularQueue<CallData, 200> m_calls;
};

}
