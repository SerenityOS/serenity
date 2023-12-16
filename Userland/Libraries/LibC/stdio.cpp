/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BuiltinWrappers.h>
#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/PrintfImplementation.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StdLibExtras.h>
#include <assert.h>
#include <bits/mutex_locker.h>
#include <bits/stdio_file_implementation.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdio_ext.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>

static constinit pthread_mutex_t s_open_streams_lock = __PTHREAD_MUTEX_INITIALIZER;

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

FILE* FILE::create(int fd, int mode)
{
    void* file_location = calloc(1, sizeof(FILE));
    if (file_location == nullptr)
        return nullptr;
    auto* file = new (file_location) FILE(fd, mode);
    LibC::MutexLocker locker(s_open_streams_lock);
    s_open_streams->append(*file);
    return file;
}

bool FILE::close()
{
    bool flush_ok = flush();
    int rc = ::close(m_fd);
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
        if (lseek(m_fd, -had_buffered, SEEK_CUR) < 0) {
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
    int nread = ::read(m_fd, data, size);

    if (nread < 0) {
        m_error = errno;
    } else if (nread == 0) {
        m_eof = true;
    }
    return nread;
}

ssize_t FILE::do_write(u8 const* data, size_t size)
{
    int nwritten = ::write(m_fd, data, size);

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

int FILE::seek(off_t offset, int whence)
{
    bool ok = flush();
    if (!ok)
        return -1;

    off_t off = lseek(m_fd, offset, whence);
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

    return lseek(m_fd, 0, SEEK_CUR);
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
    if (m_mode == -1)
        m_mode = isatty(fd) ? _IOLBF : _IOFBF;

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
    pthread_mutex_lock(&m_mutex);
}

void FILE::unlock()
{
    pthread_mutex_unlock(&m_mutex);
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setvbuf.html
int setvbuf(FILE* stream, char* buf, int mode, size_t size)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    if (mode != _IONBF && mode != _IOLBF && mode != _IOFBF) {
        errno = EINVAL;
        return -1;
    }
    stream->setbuf(reinterpret_cast<u8*>(buf), mode, size);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/setbuf.html
void setbuf(FILE* stream, char* buf)
{
    setvbuf(stream, buf, buf ? _IOFBF : _IONBF, BUFSIZ);
}

void setlinebuf(FILE* stream)
{
    setvbuf(stream, nullptr, _IOLBF, 0);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fileno.html
int fileno(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->fileno();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/feof.html
int feof(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->eof();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fflush.html
int fflush(FILE* stream)
{
    if (!stream) {
        int rc = 0;
        LibC::MutexLocker locker(s_open_streams_lock);
        for (auto& file : *s_open_streams) {
            ScopedFileLock lock(&file);
            rc = file.flush() ? rc : EOF;
        }
        return rc;
    }
    ScopedFileLock lock(stream);
    return stream->flush() ? 0 : EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgets.html
char* fgets(char* buffer, int size, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    bool ok = stream->gets(reinterpret_cast<u8*>(buffer), size);
    return ok ? buffer : nullptr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgetc.html
int fgetc(FILE* stream)
{
    VERIFY(stream);
    unsigned char ch;
    size_t nread = fread(&ch, sizeof(unsigned char), 1, stream);
    if (nread == 1) {
        return ch;
    }
    return EOF;
}

int fgetc_unlocked(FILE* stream)
{
    VERIFY(stream);
    unsigned char ch;
    size_t nread = fread_unlocked(&ch, sizeof(unsigned char), 1, stream);
    if (nread == 1)
        return ch;
    return EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getc.html
int getc(FILE* stream)
{
    return fgetc(stream);
}

int getc_unlocked(FILE* stream)
{
    return fgetc_unlocked(stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getchar.html
int getchar()
{
    return getc(stdin);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getdelim.html
ssize_t getdelim(char** lineptr, size_t* n, int delim, FILE* stream)
{
    if (!lineptr || !n) {
        errno = EINVAL;
        return -1;
    }

    if (*lineptr == nullptr || *n == 0) {
        *n = BUFSIZ;
        if ((*lineptr = static_cast<char*>(malloc(*n))) == nullptr) {
            return -1;
        }
    }

    char* ptr;
    char* eptr;
    for (ptr = *lineptr, eptr = *lineptr + *n;;) {
        int c = fgetc(stream);
        if (c == -1) {
            if (feof(stream)) {
                *ptr = '\0';
                return ptr == *lineptr ? -1 : ptr - *lineptr;
            } else {
                return -1;
            }
        }
        *ptr++ = c;
        if (c == delim) {
            *ptr = '\0';
            return ptr - *lineptr;
        }
        if (ptr + 2 >= eptr) {
            char* nbuf;
            size_t nbuf_sz = *n * 2;
            ssize_t d = ptr - *lineptr;
            if ((nbuf = static_cast<char*>(realloc(*lineptr, nbuf_sz))) == nullptr) {
                return -1;
            }
            *lineptr = nbuf;
            *n = nbuf_sz;
            eptr = nbuf + nbuf_sz;
            ptr = nbuf + d;
        }
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/getline.html
ssize_t getline(char** lineptr, size_t* n, FILE* stream)
{
    return getdelim(lineptr, n, '\n', stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ungetc.html
int ungetc(int c, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    bool ok = stream->ungetc(c);
    return ok ? c : EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fputc.html
int fputc(int ch, FILE* stream)
{
    VERIFY(stream);
    u8 byte = ch;
    ScopedFileLock lock(stream);
    size_t nwritten = stream->write(&byte, 1);
    if (nwritten == 0)
        return EOF;
    VERIFY(nwritten == 1);
    return byte;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putc.html
int putc(int ch, FILE* stream)
{
    return fputc(ch, stream);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/putchar.html
int putchar(int ch)
{
    return putc(ch, stdout);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fputs.html
int fputs(char const* s, FILE* stream)
{
    VERIFY(stream);
    size_t len = strlen(s);
    ScopedFileLock lock(stream);
    size_t nwritten = stream->write(reinterpret_cast<u8 const*>(s), len);
    if (nwritten < len)
        return EOF;
    return 1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/puts.html
int puts(char const* s)
{
    int rc = fputs(s, stdout);
    if (rc == EOF)
        return EOF;
    return fputc('\n', stdout);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/clearerr.html
void clearerr(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    stream->clear_err();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ferror.html
int ferror(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->error();
}

size_t fread_unlocked(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    VERIFY(stream);
    VERIFY(!Checked<size_t>::multiplication_would_overflow(size, nmemb));

    size_t nread = stream->read(reinterpret_cast<u8*>(ptr), size * nmemb);
    if (!nread)
        return 0;
    return nread / size;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fread.html
size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return fread_unlocked(ptr, size, nmemb, stream);
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

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fseek.html
int fseek(FILE* stream, long offset, int whence)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->seek(offset, whence);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fseeko.html
int fseeko(FILE* stream, off_t offset, int whence)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->seek(offset, whence);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftell.html
long ftell(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->tell();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftello.html
off_t ftello(FILE* stream)
{
    VERIFY(stream);
    ScopedFileLock lock(stream);
    return stream->tell();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fgetpos.html
int fgetpos(FILE* stream, fpos_t* pos)
{
    VERIFY(stream);
    VERIFY(pos);

    ScopedFileLock lock(stream);
    off_t val = stream->tell();
    if (val == -1L)
        return 1;

    *pos = val;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fsetpos.html
int fsetpos(FILE* stream, fpos_t const* pos)
{
    VERIFY(stream);
    VERIFY(pos);

    ScopedFileLock lock(stream);
    return stream->seek(*pos, SEEK_SET);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rewind.html
void rewind(FILE* stream)
{
    fseek(stream, 0, SEEK_SET);
    clearerr(stream);
}

ALWAYS_INLINE void stdout_putch(char*&, char ch)
{
    putchar(ch);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vfprintf.html
int vfprintf(FILE* stream, char const* fmt, va_list ap)
{
    return printf_internal([stream](auto, char ch) { fputc(ch, stream); }, nullptr, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fprintf.html
int fprintf(FILE* stream, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vfprintf(stream, fmt, ap);
    va_end(ap);
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vdprintf.html
int vdprintf(int fd, char const* fmt, va_list ap)
{
    // FIXME: Implement buffering so that we don't issue one write syscall for every character.
    return printf_internal([fd](auto, char ch) { write(fd, &ch, 1); }, nullptr, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/dprintf.html
int dprintf(int fd, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vdprintf(fd, fmt, ap);
    va_end(ap);
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vprintf.html
int vprintf(char const* fmt, va_list ap)
{
    return printf_internal(stdout_putch, nullptr, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/printf.html
int printf(char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vprintf(fmt, ap);
    va_end(ap);
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vasprintf.html
int vasprintf(char** strp, char const* fmt, va_list ap)
{
    StringBuilder builder;
    builder.appendvf(fmt, ap);
    VERIFY(builder.length() <= NumericLimits<int>::max());
    int length = builder.length();
    *strp = strdup(builder.to_byte_string().characters());
    return length;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/asprintf.html
int asprintf(char** strp, char const* fmt, ...)
{
    StringBuilder builder;
    va_list ap;
    va_start(ap, fmt);
    builder.appendvf(fmt, ap);
    va_end(ap);
    VERIFY(builder.length() <= NumericLimits<int>::max());
    int length = builder.length();
    *strp = strdup(builder.to_byte_string().characters());
    return length;
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vsprintf.html
int vsprintf(char* buffer, char const* fmt, va_list ap)
{
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sprintf.html
int sprintf(char* buffer, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsprintf(buffer, fmt, ap);
    va_end(ap);
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vsnprintf.html
int vsnprintf(char* buffer, size_t size, char const* fmt, va_list ap)
{
    size_t space_remaining = 0;
    if (size) {
        space_remaining = size - 1;
    } else {
        space_remaining = 0;
    }
    auto sized_buffer_putch = [&](char*& bufptr, char ch) {
        if (space_remaining) {
            *bufptr++ = ch;
            --space_remaining;
        }
    };
    int ret = printf_internal(sized_buffer_putch, buffer, fmt, ap);
    if (space_remaining) {
        buffer[ret] = '\0';
    } else if (size > 0) {
        buffer[size - 1] = '\0';
    }
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/snprintf.html
int snprintf(char* buffer, size_t size, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsnprintf(buffer, size, fmt, ap);
    va_end(ap);
    return ret;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/perror.html
void perror(char const* s)
{
    int saved_errno = errno;
    dbgln("perror(): {}: {}", s, strerror(saved_errno));
    warnln("{}: {}", s, strerror(saved_errno));
}

static int parse_mode(char const* mode)
{
    int flags = 0;

    // NOTE: rt is a non-standard mode which opens a file for read, explicitly
    // specifying that it's a text file
    for (auto* ptr = mode; *ptr; ++ptr) {
        switch (*ptr) {
        case 'r':
            flags |= O_RDONLY;
            break;
        case 'w':
            flags |= O_WRONLY | O_CREAT | O_TRUNC;
            break;
        case 'a':
            flags |= O_WRONLY | O_APPEND | O_CREAT;
            break;
        case '+':
            flags |= O_RDWR;
            break;
        case 'e':
            flags |= O_CLOEXEC;
            break;
        case 'b':
            // Ok...
            break;
        case 't':
            // Ok...
            break;
        default:
            dbgln("Potentially unsupported fopen mode _{}_ (because of '{}')", mode, *ptr);
        }
    }

    return flags;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fopen.html
FILE* fopen(char const* pathname, char const* mode)
{
    int flags = parse_mode(mode);
    int fd = open(pathname, flags, 0666);
    if (fd < 0)
        return nullptr;
    return FILE::create(fd, flags);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/freopen.html
FILE* freopen(char const* pathname, char const* mode, FILE* stream)
{
    VERIFY(stream);
    if (!pathname) {
        // FIXME: Someone should probably implement this path.
        TODO();
    }

    int flags = parse_mode(mode);
    int fd = open(pathname, flags, 0666);
    if (fd < 0)
        return nullptr;

    stream->reopen(fd, flags);
    return stream;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fdopen.html
FILE* fdopen(int fd, char const* mode)
{
    int flags = parse_mode(mode);
    // FIXME: Verify that the mode matches how fd is already open.
    if (fd < 0)
        return nullptr;
    return FILE::create(fd, flags);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fmemopen.html
FILE* fmemopen(void*, size_t, char const*)
{
    // FIXME: Implement me :^)
    TODO();
}

static inline bool is_default_stream(FILE* stream)
{
    return stream == stdin || stream == stdout || stream == stderr;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fclose.html
int fclose(FILE* stream)
{
    VERIFY(stream);
    bool ok;

    {
        ScopedFileLock lock(stream);
        ok = stream->close();
    }
    ScopedValueRollback errno_restorer(errno);

    {
        LibC::MutexLocker locker(s_open_streams_lock);
        s_open_streams->remove(*stream);
    }
    stream->~FILE();
    if (!is_default_stream(stream))
        free(stream);

    return ok ? 0 : EOF;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rename.html
int rename(char const* oldpath, char const* newpath)
{
    return renameat(AT_FDCWD, oldpath, AT_FDCWD, newpath);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/renameat.html
int renameat(int olddirfd, char const* oldpath, int newdirfd, char const* newpath)
{
    if (!oldpath || !newpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_rename_params params { olddirfd, { oldpath, strlen(oldpath) }, newdirfd, { newpath, strlen(newpath) } };
    int rc = syscall(SC_rename, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void dbgputstr(char const* characters, size_t length)
{
    syscall(SC_dbgputstr, characters, length);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tmpnam.html
char* tmpnam(char*)
{
    dbgln("FIXME: Implement tmpnam()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/popen.html
FILE* popen(char const* command, char const* type)
{
    if (!type || (*type != 'r' && *type != 'w')) {
        errno = EINVAL;
        return nullptr;
    }

    int pipe_fds[2];

    if (pipe(pipe_fds) < 0) {
        ScopedValueRollback rollback(errno);
        perror("pipe");
        return nullptr;
    }

    pid_t child_pid = fork();
    if (child_pid < 0) {
        ScopedValueRollback rollback(errno);
        perror("fork");
        close(pipe_fds[0]);
        close(pipe_fds[1]);
        return nullptr;
    } else if (child_pid == 0) {
        if (*type == 'r') {
            if (dup2(pipe_fds[1], STDOUT_FILENO) < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        } else if (*type == 'w') {
            if (dup2(pipe_fds[0], STDIN_FILENO) < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        }

        if (execl("/bin/sh", "sh", "-c", command, nullptr) < 0)
            perror("execl");
        exit(1);
    }

    FILE* file = nullptr;
    if (*type == 'r') {
        file = FILE::create(pipe_fds[0], O_RDONLY);
        close(pipe_fds[1]);
    } else if (*type == 'w') {
        file = FILE::create(pipe_fds[1], O_WRONLY);
        close(pipe_fds[0]);
    }

    file->set_popen_child(child_pid);
    return file;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/pclose.html
int pclose(FILE* stream)
{
    VERIFY(stream);
    VERIFY(stream->popen_child() != 0);

    int wstatus = 0;
    if (waitpid(stream->popen_child(), &wstatus, 0) < 0)
        return -1;

    return wstatus;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/remove.html
int remove(char const* pathname)
{
    if (unlink(pathname) < 0) {
        if (errno == EISDIR)
            return rmdir(pathname);
        return -1;
    }
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/scanf.html
int scanf(char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stdin, fmt, ap);
    va_end(ap);
    return count;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fscanf.html
int fscanf(FILE* stream, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stream, fmt, ap);
    va_end(ap);
    return count;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sscanf.html
int sscanf(char const* buffer, char const* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vsscanf(buffer, fmt, ap);
    va_end(ap);
    return count;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vfscanf.html
int vfscanf(FILE* stream, char const* fmt, va_list ap)
{
    char buffer[BUFSIZ];
    if (!fgets(buffer, sizeof(buffer) - 1, stream))
        return -1;
    return vsscanf(buffer, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/vscanf.html
int vscanf(char const* fmt, va_list ap)
{
    return vfscanf(stdin, fmt, ap);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/flockfile.html
void flockfile(FILE* filehandle)
{
    VERIFY(filehandle);
    filehandle->lock();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/funlockfile.html
void funlockfile(FILE* filehandle)
{
    VERIFY(filehandle);
    filehandle->unlock();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/tmpfile.html
FILE* tmpfile()
{
    char tmp_path[] = "/tmp/XXXXXX";
    int fd = mkstemp(tmp_path);
    if (fd < 0)
        return nullptr;
    // FIXME: instead of using this hack, implement with O_TMPFILE or similar
    unlink(tmp_path);
    return fdopen(fd, "rw");
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ctermid.html
char* ctermid(char* s)
{
    static char tty_path[L_ctermid] = "/dev/tty";
    if (s)
        return strcpy(s, tty_path);
    return tty_path;
}

size_t __fpending(FILE* stream)
{
    ScopedFileLock lock(stream);
    return stream->pending();
}

int __freading(FILE* stream)
{
    ScopedFileLock lock(stream);

    if ((stream->mode() & O_RDWR) == O_RDONLY) {
        return 1;
    }

    return (stream->flags() & FILE::Flags::LastRead);
}

int __fwriting(FILE* stream)
{
    ScopedFileLock lock(stream);

    if ((stream->mode() & O_RDWR) == O_WRONLY) {
        return 1;
    }

    return (stream->flags() & FILE::Flags::LastWrite);
}

void __fpurge(FILE* stream)
{
    ScopedFileLock lock(stream);
    stream->purge();
}

size_t __freadahead(FILE* stream)
{
    VERIFY(stream);

    ScopedFileLock lock(stream);

    size_t available_size;
    stream->readptr(available_size);
    return available_size;
}

char const* __freadptr(FILE* stream, size_t* sizep)
{
    VERIFY(stream);
    VERIFY(sizep);

    ScopedFileLock lock(stream);

    size_t available_size;
    u8 const* ptr = stream->readptr(available_size);

    if (available_size == 0)
        return nullptr;

    *sizep = available_size;
    return reinterpret_cast<char const*>(ptr);
}

void __freadptrinc(FILE* stream, size_t increment)
{
    VERIFY(stream);

    ScopedFileLock lock(stream);

    stream->readptr_increase(increment);
}

void __fseterr(FILE* stream)
{
    ScopedFileLock lock(stream);
    stream->set_err();
}
}

template bool FILE::gets<u8>(u8*, size_t);
template bool FILE::gets<u32>(u32*, size_t);
