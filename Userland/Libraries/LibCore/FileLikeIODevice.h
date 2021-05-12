/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Ali Mohammad Pur <ali.mpfard@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Forward.h>
#include <LibCore/IODevice.h>
#include <LibCore/Notifier.h>
#include <LibCore/Object.h>
#include <errno.h>

namespace Core {

enum class OpenMode : unsigned {
    NotOpen = 0,
    ReadOnly = 1,
    WriteOnly = 2,
    ReadWrite = 3,
    Append = 4,
    Truncate = 8,
    MustBeNew = 16,
};

enum class SeekMode {
    SetPosition,
    FromCurrentPosition,
    FromEndPosition,
};

AK_ENUM_BITWISE_OPERATORS(OpenMode)

class FileLikeIODevice : public virtual IODevice {
    C_OBJECT_ABSTRACT(FileLikeIODevice)
public:
    // ^IODevice
    size_t read(Bytes) override;
    bool discard_or_error(size_t count) override;
    bool unreliable_eof() const override { return m_eof; }
    size_t write(ReadonlyBytes) override;
    RefPtr<AbstractNotifier> make_notifier(unsigned event_mask) override;

    virtual ~FileLikeIODevice() override;

    int fd() const { return m_fd; }
    OpenMode mode() const { return m_mode; }
    bool is_open() const { return m_mode != OpenMode::NotOpen; }
    bool eof() const { return m_eof; }

    int error() const { return m_error; }
    const char* error_string() const;

    bool has_error() const { return m_error != 0; }

    ByteBuffer read(size_t max_size);
    ByteBuffer read_all();

    bool truncate(off_t);

    bool can_read() const;

    bool seek(i64, SeekMode = SeekMode::SetPosition, off_t* = nullptr);

    virtual bool open(OpenMode) = 0;
    virtual bool close();

protected:
    explicit FileLikeIODevice(Object* parent);

    void set_fd(int);
    void set_eof(bool eof) { m_eof = eof; }
    void set_mode(OpenMode mode) { m_mode = mode; }
    void set_error(int error) const
    {
        if (error && error != EINTR && error != EAGAIN && error != EWOULDBLOCK)
            set_recoverable_error();
        m_error = error;
    }

    virtual void did_update_fd(int) { }

private:
    bool can_read_from_fd() const;

    int m_fd { -1 };
    OpenMode m_mode { OpenMode::NotOpen };
    mutable int m_error { 0 };
    mutable bool m_eof { false };
};

}
