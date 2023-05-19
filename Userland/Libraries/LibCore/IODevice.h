/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Forward.h>
#include <AK/Stream.h>
#include <LibCore/Object.h>

namespace Core {

enum class OpenMode : unsigned {
    NotOpen = 0,
    ReadOnly = 1,
    WriteOnly = 2,
    ReadWrite = 3,
    Append = 4,
    Truncate = 8,
    MustBeNew = 16,
    KeepOnExec = 32,
};

AK_ENUM_BITWISE_OPERATORS(OpenMode)

class IODevice : public Object {
    C_OBJECT_ABSTRACT(IODevice)
public:
    virtual ~IODevice() override = default;

    int fd() const { return m_fd; }
    OpenMode mode() const { return m_mode; }
    bool eof() const { return m_eof; }

    int error() const { return m_error; }
    char const* error_string() const;

    int read(u8* buffer, int length);

    ByteBuffer read(size_t max_size);
    ByteBuffer read_all();
    DeprecatedString read_line(size_t max_size = 16384);

    bool write(u8 const*, int size);

    bool can_read_line() const;

    bool seek(i64, SeekMode = SeekMode::SetPosition, off_t* = nullptr);

    virtual bool open(OpenMode) = 0;
    virtual bool close();

protected:
    explicit IODevice(Object* parent = nullptr);

    void set_fd(int);
    void set_mode(OpenMode mode) { m_mode = mode; }
    void set_error(int error) const { m_error = error; }
    void set_eof(bool eof) const { m_eof = eof; }

private:
    bool populate_read_buffer(size_t size = 1024) const;
    bool can_read_from_fd() const;

    int m_fd { -1 };
    OpenMode m_mode { OpenMode::NotOpen };
    mutable int m_error { 0 };
    mutable bool m_eof { false };
    mutable Vector<u8> m_buffered_data;
};

}
