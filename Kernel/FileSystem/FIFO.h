/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/DoubleBuffer.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/WaitQueue.h>

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

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Woverloaded-virtual"
    void attach(Direction);
    void detach(Direction);
#pragma GCC diagnostic pop

private:
    // ^File
    virtual ErrorOr<size_t> write(OpenFileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<size_t> read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual ErrorOr<void> stat(::stat&) const override;
    virtual bool can_read(const OpenFileDescription&, size_t) const override;
    virtual bool can_write(const OpenFileDescription&, size_t) const override;
    virtual ErrorOr<NonnullOwnPtr<KString>> pseudo_path(const OpenFileDescription&) const override;
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
