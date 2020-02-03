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

#include <Kernel/FileSystem/File.h>
#include <Kernel/UnixTypes.h>

class Process;
class SimpleBufferBuilder;

class ProcessTracer : public File {
public:
    virtual ~ProcessTracer() override;

    bool is_dead() const { return !m_process; }
    void set_dead() { m_process = nullptr; }

    virtual bool have_more_items() const = 0;
    virtual void read_item(SimpleBufferBuilder&) const = 0;
    virtual void dequeue_item() = 0;

    virtual bool can_read(const FileDescription&) const override { return have_more_items() || is_dead(); }
    virtual int read(FileDescription&, u8*, int) override;

    virtual bool can_write(const FileDescription&) const override { return true; }
    virtual int write(FileDescription&, const u8*, int) override { return -EIO; }

    virtual String absolute_path(const FileDescription&) const override;

    Process* process() const { return m_process; }

    virtual bool is_syscall_tracer() const { return false; }

protected:
    explicit ProcessTracer(Process&);

private:
    virtual const char* class_name() const override { return "ProcessTracer"; }

    Process* m_process;

    bool m_read_first_item { false };
    bool m_read_closing_bracket { false };
};
