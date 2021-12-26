/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/DoubleBuffer.h>
#include <Kernel/FileSystem/File.h>
#include <Kernel/Lock.h>
#include <Kernel/UnixTypes.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class FileDescription;

class FIFO final : public File {
public:
    enum class Direction : u8 {
        Neither,
        Reader,
        Writer
    };

    static NonnullRefPtr<FIFO> create(uid_t);
    virtual ~FIFO() override;

    uid_t uid() const { return m_uid; }

    KResultOr<NonnullRefPtr<FileDescription>> open_direction(Direction);
    KResultOr<NonnullRefPtr<FileDescription>> open_direction_blocking(Direction);

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Woverloaded-virtual"
    void attach(Direction);
    void detach(Direction);
#pragma GCC diagnostic pop

private:
    // ^File
    virtual KResultOr<size_t> write(FileDescription&, u64, const UserOrKernelBuffer&, size_t) override;
    virtual KResultOr<size_t> read(FileDescription&, u64, UserOrKernelBuffer&, size_t) override;
    virtual KResult stat(::stat&) const override;
    virtual bool can_read(const FileDescription&, size_t) const override;
    virtual bool can_write(const FileDescription&, size_t) const override;
    virtual String absolute_path(const FileDescription&) const override;
    virtual StringView class_name() const override { return "FIFO"; }
    virtual bool is_fifo() const override { return true; }

    explicit FIFO(uid_t);

    unsigned m_writers { 0 };
    unsigned m_readers { 0 };
    DoubleBuffer m_buffer;

    uid_t m_uid { 0 };

    int m_fifo_id { 0 };

    WaitQueue m_read_open_queue;
    WaitQueue m_write_open_queue;
    Lock m_open_lock;
};

}
