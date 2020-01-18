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

#include <AK/ByteBuffer.h>
#include <AK/StringView.h>
#include <LibCore/CObject.h>

class CIODevice : public CObject {
    C_OBJECT_ABSTRACT(CIODevice)
public:
    enum OpenMode {
        NotOpen = 0,
        ReadOnly = 1,
        WriteOnly = 2,
        ReadWrite = 3,
        Append = 4,
        Truncate = 8,
        MustBeNew = 16,
    };

    virtual ~CIODevice() override;

    int fd() const { return m_fd; }
    unsigned mode() const { return m_mode; }
    bool is_open() const { return m_mode != NotOpen; }
    bool eof() const { return m_eof; }

    int error() const { return m_error; }
    const char* error_string() const;

    bool has_error() const { return m_error != 0; }


    int read(u8* buffer, int length);

    ByteBuffer read(int max_size);
    ByteBuffer read_line(int max_size);
    ByteBuffer read_all();

    bool write(const u8*, int size);
    bool write(const StringView& v) { return write((const u8*)v.characters_without_null_termination(), v.length()); }

    // FIXME: I would like this to be const but currently it needs to call populate_read_buffer().
    bool can_read_line();

    bool can_read() const;

    enum class SeekMode {
        SetPosition,
        FromCurrentPosition,
        FromEndPosition,
    };

    bool seek(i64, SeekMode = SeekMode::SetPosition, off_t* = nullptr);

    virtual bool open(CIODevice::OpenMode) = 0;
    virtual bool close();

    int printf(const char*, ...);

protected:
    explicit CIODevice(CObject* parent = nullptr);

    void set_fd(int);
    void set_mode(OpenMode mode) { m_mode = mode; }
    void set_error(int error) { m_error = error; }
    void set_eof(bool eof) { m_eof = eof; }

    virtual void did_update_fd(int) {}

private:
    bool populate_read_buffer();
    bool can_read_from_fd() const;

    int m_fd { -1 };
    int m_error { 0 };
    bool m_eof { false };
    OpenMode m_mode { NotOpen };
    Vector<u8> m_buffered_data;
};
