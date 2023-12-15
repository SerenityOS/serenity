/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/ScopedValueRollback.h>
#include <LibRuntime/Mutex.h>
#include <LibRuntime/System.h>
#include <bits/stdio_file_implementation.h>
#include <sys/internals.h>

static constinit Runtime::Mutex s_open_streams_lock;

// The list of open files is initialized in __stdio_init.
// We cannot rely on global constructors to initialize it, because it must
// be initialized before other global constructors run. Similarly, we cannot
// allow global destructors to destruct it.
alignas(FILE::List) static u8 s_open_streams_storage[sizeof(FILE::List)];
static FILE::List* const s_open_streams = reinterpret_cast<FILE::List*>(s_open_streams_storage);

FILE::~FILE()
{
    bool already_closed = m_fd == -1;
    VERIFY(already_closed);
}

namespace {
template<typename T>
T unwrap_and_set_errno(ErrorOr<T>&& result)
{
    if (result.is_error()) {
        errno = result.error().code();
        return -1;
    } else {
        return result.value();
    }
}

int unwrap_and_set_errno(ErrorOr<void>&& result)
{
    if (result.is_error()) {
        errno = result.error().code();
        return -1;
    } else {
        return 0;
    }
}
}

FILE* FILE::create(int fd, int mode)
{
    void* file_location = calloc(1, sizeof(FILE));
    if (file_location == nullptr)
        return nullptr;
    auto* file = new (file_location) FILE(fd, mode);
    Runtime::MutexLocker locker(s_open_streams_lock);
    s_open_streams->append(*file);
    return file;
}

static inline bool is_default_stream(FILE* stream)
{
    return stream == stdin || stream == stdout || stream == stderr;
}

int FILE::close(FILE* stream)
{
    VERIFY(stream);
    bool ok;

    {
        ScopedFileLock lock(stream);
        ok = stream->close();
    }
    ScopedValueRollback errno_restorer(errno);

    {
        Runtime::MutexLocker locker(s_open_streams_lock);
        s_open_streams->remove(*stream);
    }
    stream->~FILE();
    if (!is_default_stream(stream))
        free(stream);

    return ok ? 0 : EOF;
}

int FILE::flush_open_streams()
{
    int rc = 0;
    Runtime::MutexLocker locker(s_open_streams_lock);
    for (auto& file : *s_open_streams) {
        ScopedFileLock lock(&file);
        rc = file.flush() ? rc : EOF;
    }
    return rc;
}

bool FILE::close()
{
    bool flush_ok = flush();
    int rc = unwrap_and_set_errno(Runtime::close(m_fd));
    m_fd = -1;
    if (!flush_ok) {
        // Restore the original error from flush().
        errno = m_error;
    }
    return flush_ok && rc == 0;
}

bool FILE::flush()
{
    if (m_mode & O_WRONLY && m_buffer.may_use()) {
        // When open for writing, write out all the buffered data.
        while (m_buffer.is_not_empty()) {
            bool ok = write_from_buffer();
            if (!ok)
                return false;
        }
    }
    if (m_mode & O_RDONLY) {
        // When open for reading, just drop the buffered data.
        if constexpr (sizeof(size_t) >= sizeof(off_t))
            VERIFY(m_buffer.buffered_size() <= NumericLimits<off_t>::max());
        off_t had_buffered = static_cast<off_t>(m_buffer.buffered_size());
        m_buffer.drop();
        // Attempt to reset the underlying file position to what the user
        // expects.
        if (unwrap_and_set_errno(Runtime::lseek(m_fd, -had_buffered, Runtime::SeekWhence::Current)) < 0) {
            if (errno == ESPIPE) {
                // We can't set offset on this file; oh well, the user will just
                // have to cope.
                errno = 0;
            } else {
                return false;
            }
        }
    }

    return true;
}

void FILE::purge()
{
    m_buffer.drop();
}

size_t FILE::pending()
{
    if (m_mode & O_RDONLY) {
        return 0;
    }

    // FIXME: Check if our buffer is a write buffer, and only count those bytes.
    return m_buffer.buffered_size();
}

ssize_t FILE::do_read(u8* data, size_t size)
{
    ssize_t nread = static_cast<ssize_t>(unwrap_and_set_errno(Runtime::read(m_fd, data, size)));

    if (nread < 0) {
        m_error = errno;
    } else if (nread == 0) {
        m_eof = true;
    }
    return nread;
}

ssize_t FILE::do_write(u8 const* data, size_t size)
{
    ssize_t nwritten = static_cast<ssize_t>(unwrap_and_set_errno(Runtime::write(m_fd, data, size)));

    if (nwritten < 0)
        m_error = errno;
    return nwritten;
}

bool FILE::read_into_buffer()
{
    m_buffer.realize(m_fd);

    size_t available_size;
    u8* data = m_buffer.begin_enqueue(available_size);
    // If we want to read, the buffer must have some space!
    VERIFY(available_size);

    ssize_t nread = do_read(data, available_size);

    if (nread <= 0)
        return false;

    m_buffer.did_enqueue(nread);
    return true;
}

bool FILE::write_from_buffer()
{
    size_t size;
    u8 const* data = m_buffer.begin_dequeue(size);
    // If we want to write, the buffer must have something in it!
    VERIFY(size);

    ssize_t nwritten = do_write(data, size);

    if (nwritten < 0)
        return false;

    m_buffer.did_dequeue(nwritten);
    return true;
}

size_t FILE::read(u8* data, size_t size)
{
    size_t total_read = 0;

    m_flags |= Flags::LastRead;
    m_flags &= ~Flags::LastWrite;

    while (size > 0) {
        size_t actual_size;

        if (m_buffer.may_use()) {
            // Let's see if the buffer has something queued for us.
            size_t queued_size;
            u8 const* queued_data = m_buffer.begin_dequeue(queued_size);
            if (queued_size == 0) {
                // Nothing buffered; we're going to have to read some.
                bool read_some_more = read_into_buffer();
                if (read_some_more) {
                    // Great, now try this again.
                    continue;
                }
                return total_read;
            }
            actual_size = min(size, queued_size);
            memcpy(data, queued_data, actual_size);
            m_buffer.did_dequeue(actual_size);
        } else {
            // Read directly into the user buffer.
            ssize_t nread = do_read(data, size);
            if (nread <= 0)
                return total_read;
            actual_size = nread;
        }

        total_read += actual_size;
        data += actual_size;
        size -= actual_size;
    }

    return total_read;
}

size_t FILE::write(u8 const* data, size_t size)
{
    size_t total_written = 0;

    m_flags &= ~Flags::LastRead;
    m_flags |= Flags::LastWrite;

    while (size > 0) {
        size_t actual_size;

        if (m_buffer.may_use()) {
            m_buffer.realize(m_fd);
            // Try writing into the buffer.
            size_t available_size;
            u8* buffer_data = m_buffer.begin_enqueue(available_size);
            if (available_size == 0) {
                // There's no space in the buffer; we're going to free some.
                bool freed_some_space = write_from_buffer();
                if (freed_some_space) {
                    // Great, now try this again.
                    continue;
                }
                return total_written;
            }
            actual_size = min(size, available_size);
            memcpy(buffer_data, data, actual_size);
            m_buffer.did_enqueue(actual_size);
            // See if we have to flush it.
            if (m_buffer.mode() == _IOLBF) {
                bool includes_newline = memchr(data, '\n', actual_size);
                if (includes_newline)
                    flush();
            }
        } else {
            // Write directly from the user buffer.
            ssize_t nwritten = do_write(data, size);
            if (nwritten < 0)
                return total_written;
            actual_size = nwritten;
        }

        total_written += actual_size;
        data += actual_size;
        size -= actual_size;
    }

    return total_written;
}

template<typename T>
bool FILE::gets(T* data, size_t size)
{
    // gets() is a lot like read(), but it is different enough in how it
    // processes newlines and null-terminates the buffer that it deserves a
    // separate implementation.
    size_t total_read = 0;

    if (size == 0)
        return false;

    m_flags |= Flags::LastRead;
    m_flags &= ~Flags::LastWrite;

    while (size > 1) {
        if (m_buffer.may_use()) {
            // Let's see if the buffer has something queued for us.
            size_t queued_size;
            T const* queued_data = bit_cast<T const*>(m_buffer.begin_dequeue(queued_size));
            queued_size /= sizeof(T);
            if (queued_size == 0) {
                // Nothing buffered; we're going to have to read some.
                bool read_some_more = read_into_buffer();
                if (read_some_more) {
                    // Great, now try this again.
                    continue;
                }
                *data = 0;
                return total_read > 0;
            }
            size_t actual_size = min(size - 1, queued_size);
            T const* newline = nullptr;
            for (size_t i = 0; i < actual_size; ++i) {
                if (queued_data[i] != '\n')
                    continue;

                newline = &queued_data[i];
                actual_size = i + 1;
                break;
            }
            memcpy(data, queued_data, actual_size * sizeof(T));
            m_buffer.did_dequeue(actual_size * sizeof(T));
            total_read += actual_size;
            data += actual_size;
            size -= actual_size;
            if (newline)
                break;
        } else {
            // Sadly, we have to actually read these characters one by one.
            T value;
            ssize_t nread = do_read(bit_cast<u8*>(&value), sizeof(T));
            if (nread <= 0) {
                *data = 0;
                return total_read > 0;
            }
            VERIFY(nread == sizeof(T));
            *data = value;
            total_read++;
            data++;
            size--;
            if (value == '\n')
                break;
        }
    }

    *data = 0;
    return total_read > 0;
}

template bool FILE::gets<u8>(u8*, size_t);
template bool FILE::gets<u32>(u32*, size_t);

int FILE::seek(off_t offset, int whence)
{
    bool ok = flush();
    if (!ok)
        return -1;

    off_t off = unwrap_and_set_errno(Runtime::lseek(m_fd, offset, static_cast<Runtime::SeekWhence>(whence)));
    if (off < 0) {
        // Note: do not set m_error.
        return off;
    }

    m_eof = false;
    return 0;
}

off_t FILE::tell()
{
    bool ok = flush();
    if (!ok)
        return -1;

    return unwrap_and_set_errno(Runtime::lseek(m_fd, 0, Runtime::SeekWhence::Current));
}

void FILE::reopen(int fd, int mode)
{
    // Dr. POSIX says: "Failure to flush or close the file descriptor
    //                  successfully shall be ignored"
    // and so we ignore any failures these two might have.
    flush();
    close();

    // Just in case flush() and close() didn't drop the buffer.
    m_buffer.drop();

    m_fd = fd;
    m_mode = mode;
    m_error = 0;
    m_eof = false;
}

u8 const* FILE::readptr(size_t& available_size)
{
    return m_buffer.begin_dequeue(available_size);
}

void FILE::readptr_increase(size_t increment)
{
    m_buffer.did_dequeue(increment);
}

FILE::Buffer::~Buffer()
{
    if (m_data_is_malloced)
        free(m_data);
}

bool FILE::Buffer::may_use() const
{
    return m_ungotten != 0u || m_mode != _IONBF;
}

void FILE::Buffer::realize(int fd)
{
    if (m_mode == -1) {
        auto isatty_result = Runtime::isatty(fd);
        m_mode = !isatty_result.is_error() && isatty_result.value() ? _IOLBF : _IOFBF;
    }

    if (m_mode != _IONBF && m_data == nullptr) {
        m_data = reinterpret_cast<u8*>(malloc(m_capacity));
        m_data_is_malloced = true;
    }
}

void FILE::Buffer::setbuf(u8* data, int mode, size_t size)
{
    drop();
    m_mode = mode;
    if (data != nullptr) {
        m_data = data;
        m_capacity = size;
    }
}

void FILE::Buffer::drop()
{
    if (m_data_is_malloced) {
        free(m_data);
        m_data = nullptr;
        m_data_is_malloced = false;
    }
    m_begin = m_end = 0;
    m_empty = true;
    m_ungotten = 0u;
}

size_t FILE::Buffer::buffered_size() const
{
    // Note: does not include the ungetc() buffer.

    if (m_empty)
        return 0;

    if (m_begin < m_end)
        return m_end - m_begin;
    else
        return m_capacity - (m_begin - m_end);
}

u8 const* FILE::Buffer::begin_dequeue(size_t& available_size) const
{
    if (m_ungotten != 0u) {
        auto available_bytes = count_trailing_zeroes(m_ungotten) + 1;
        available_size = available_bytes;
        return &m_unget_buffer[unget_buffer_size - available_bytes];
    }

    if (m_empty) {
        available_size = 0;
        return nullptr;
    }

    if (m_begin < m_end)
        available_size = m_end - m_begin;
    else
        available_size = m_capacity - m_begin;

    return &m_data[m_begin];
}

void FILE::Buffer::did_dequeue(size_t actual_size)
{
    VERIFY(actual_size > 0);

    if (m_ungotten != 0u) {
        VERIFY(actual_size <= static_cast<size_t>(popcount(m_ungotten & ungotten_mask)));
        auto available_bytes = count_trailing_zeroes(m_ungotten);
        m_ungotten &= (0xffffffffu << (actual_size + available_bytes));
        return;
    }

    m_begin += actual_size;

    VERIFY(m_begin <= m_capacity);
    if (m_begin == m_capacity) {
        // Wrap around.
        m_begin = 0;
    }

    if (m_begin == m_end) {
        m_empty = true;
        // As an optimization, move both pointers to the beginning of the
        // buffer, so that more consecutive space is available next time.
        m_begin = m_end = 0;
    }
}

u8* FILE::Buffer::begin_enqueue(size_t& available_size) const
{
    VERIFY(m_data != nullptr);

    if (m_begin < m_end || m_empty)
        available_size = m_capacity - m_end;
    else
        available_size = m_begin - m_end;

    return const_cast<u8*>(&m_data[m_end]);
}

void FILE::Buffer::did_enqueue(size_t actual_size)
{
    VERIFY(m_data != nullptr);
    VERIFY(actual_size > 0);

    m_end += actual_size;

    VERIFY(m_end <= m_capacity);
    if (m_end == m_capacity) {
        // Wrap around.
        m_end = 0;
    }

    m_empty = false;
}

bool FILE::Buffer::enqueue_front(u8 byte)
{
    size_t placement_index;
    if (m_ungotten == 0u) {
        placement_index = 3u;
        m_ungotten = 1u;
    } else {
        auto first_zero_index = count_trailing_zeroes(bit_cast<u32>(~m_ungotten)); // Thanks C.
        if (first_zero_index >= unget_buffer_size) {
            // Sorry, the place is already taken!
            return false;
        }
        placement_index = unget_buffer_size - first_zero_index - 1;
        m_ungotten |= (1 << first_zero_index);
    }

    m_unget_buffer[placement_index] = byte;
    return true;
}

void FILE::lock()
{
    m_mutex.lock();
}

void FILE::unlock()
{
    m_mutex.unlock();
}

extern "C" {

alignas(FILE) static u8 default_streams[3][sizeof(FILE)];
FILE* stdin = reinterpret_cast<FILE*>(&default_streams[0]);
FILE* stdout = reinterpret_cast<FILE*>(&default_streams[1]);
FILE* stderr = reinterpret_cast<FILE*>(&default_streams[2]);

void __stdio_init()
{
    new (s_open_streams) FILE::List();
    new (stdin) FILE(0, O_RDONLY);
    new (stdout) FILE(1, O_WRONLY);
    new (stderr) FILE(2, O_WRONLY);
    stderr->setbuf(nullptr, _IONBF, 0);
    s_open_streams->append(*stdin);
    s_open_streams->append(*stdout);
    s_open_streams->append(*stderr);
    __stdio_is_initialized = true;
}
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ferror.html
int ferror(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->error();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fwrite.html
size_t fwrite(void const* ptr, size_t size, size_t nmemb, FILE* stream)
{
    VERIFY(stream);
    VERIFY(!Checked<size_t>::multiplication_would_overflow(size, nmemb));

    ScopedFileLock lock(stream);
    size_t nwritten = stream->write(reinterpret_cast<u8 const*>(ptr), size * nmemb);
    if (!nwritten)
        return 0;
    return nwritten / size;
}
