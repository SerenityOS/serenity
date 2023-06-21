/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/FileSystem/File.h>
#include <Kernel/Library/DoubleBuffer.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Tasks/WaitQueue.h>
#include <Kernel/UnixTypes.h>

namespace Kernel {

class OpenFileDescription;

class FIFO final : public File {
public:
    enum class Direction : u8 {
        Neither,
        Reader,
        Writer
    };

    static ErrorOr<NonnullRefPtr<FIFO>> try_create(UserID);
    virtual ~FIFO() override;

    UserID uid() const { return m_uid; }

    ErrorOr<NonnullRefPtr<OpenFileDescription>> open_direction(Direction);
    ErrorOr<NonnullRefPtr<OpenFileDescription>> open_direction_blocking(Direction);

private:
    // ^File
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, UserOrKernelBuffer const&, size_t) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<struct stat> stat() const override;
    virtual void detach(OpenFileDescription&) override;
    virtual bool can_read(OpenFileDescription const&, u64) const override;
    virtual bool can_write(OpenFileDescription const&, u64) const override;
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(OpenFileDescription const&) const override;
    virtual StringView class_name() const override { return "FIFO"sv; }
    virtual bool is_fifo() const override { return true; }

    explicit FIFO(UserID, NonnullOwnPtr<DoubleBuffer> buffer);

    unsigned m_writers { 0 };
    unsigned m_readers { 0 };
    NonnullOwnPtr<DoubleBuffer> m_buffer;

    UserID m_uid { 0 };

    int m_fifo_id { 0 };

    WaitQueue m_read_open_queue;
    WaitQueue m_write_open_queue;
    Mutex m_open_lock;
};

}
