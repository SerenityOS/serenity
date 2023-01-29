/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/IntrusiveList.h>
#include <AK/Types.h>
#include <bits/FILE.h>
#include <bits/pthread_integration.h>
#include <bits/wchar.h>
#include <pthread.h>
#include <sys/types.h>

struct FILE {
public:
    FILE(int fd, int mode)
        : m_fd(fd)
        , m_mode(mode)
    {
        pthread_mutexattr_t attr = { __PTHREAD_MUTEX_RECURSIVE };
        pthread_mutex_init(&m_mutex, &attr);
    }
    ~FILE();

    static FILE* create(int fd, int mode);

    void setbuf(u8* data, int mode, size_t size) { m_buffer.setbuf(data, mode, size); }

    bool flush();
    void purge();
    size_t pending();
    bool close();

    void lock();
    void unlock();

    int fileno() const { return m_fd; }
    bool eof() const { return m_eof; }
    int mode() const { return m_mode; }
    u8 flags() const { return m_flags; }

    int error() const { return m_error; }
    void clear_err() { m_error = 0; }
    void set_err() { m_error = 1; }

    size_t read(u8*, size_t);
    size_t write(u8 const*, size_t);

    template<typename CharType>
    bool gets(CharType*, size_t);

    bool ungetc(u8 byte) { return m_buffer.enqueue_front(byte); }

    int seek(off_t offset, int whence);
    off_t tell();

    pid_t popen_child() { return m_popen_child; }
    void set_popen_child(pid_t child_pid) { m_popen_child = child_pid; }

    void reopen(int fd, int mode);

    u8 const* readptr(size_t& available_size);
    void readptr_increase(size_t increment);

    enum Flags : u8 {
        None = 0,
        LastRead = 1,
        LastWrite = 2,
    };

private:
    struct Buffer {
        // A ringbuffer that also transparently implements ungetc().
    public:
        ~Buffer();

        int mode() const { return m_mode; }
        void setbuf(u8* data, int mode, size_t size);
        // Make sure to call realize() before enqueuing any data.
        // Dequeuing can be attempted without it.
        void realize(int fd);
        void drop();

        bool may_use() const;
        bool is_not_empty() const { return m_ungotten || !m_empty; }
        size_t buffered_size() const;

        u8 const* begin_dequeue(size_t& available_size) const;
        void did_dequeue(size_t actual_size);

        u8* begin_enqueue(size_t& available_size) const;
        void did_enqueue(size_t actual_size);

        bool enqueue_front(u8 byte);

    private:
        constexpr static auto unget_buffer_size = MB_CUR_MAX;
        constexpr static u32 ungotten_mask = ((u32)0xffffffff) >> (sizeof(u32) * 8 - unget_buffer_size);

        // Note: the fields here are arranged this way
        // to make sizeof(Buffer) smaller.
        u8* m_data { nullptr };
        size_t m_capacity { BUFSIZ };
        size_t m_begin { 0 };
        size_t m_end { 0 };

        int m_mode { -1 };
        Array<u8, unget_buffer_size> m_unget_buffer { 0 };
        u32 m_ungotten : unget_buffer_size { 0 };
        bool m_data_is_malloced : 1 { false };
        // When m_begin == m_end, we want to distinguish whether
        // the buffer is full or empty.
        bool m_empty : 1 { true };
    };

    // Read or write using the underlying fd, bypassing the buffer.
    ssize_t do_read(u8*, size_t);
    ssize_t do_write(u8 const*, size_t);

    // Read some data into the buffer.
    bool read_into_buffer();
    // Flush *some* data from the buffer.
    bool write_from_buffer();

    int m_fd { -1 };
    int m_mode { 0 };
    u8 m_flags { Flags::None };
    int m_error { 0 };
    bool m_eof { false };
    pid_t m_popen_child { -1 };
    Buffer m_buffer;
    __pthread_mutex_t m_mutex;
    IntrusiveListNode<FILE> m_list_node;

public:
    using List = IntrusiveList<&FILE::m_list_node>;
};

class ScopedFileLock {
public:
    ScopedFileLock(FILE* file)
        : m_file(file)
    {
        m_file->lock();
    }

    ~ScopedFileLock()
    {
        m_file->unlock();
    }

private:
    FILE* m_file;
};
