/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
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

#include <AK/LogStream.h>
#include <AK/PrintfImplementation.h>
#include <AK/ScopedValueRollback.h>
#include <AK/StdLibExtras.h>
#include <AK/kmalloc.h>
#include <Kernel/API/Syscall.h>
#include <assert.h>
#include <errno.h>
#include <fcntl.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/internals.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

struct FILE {
public:
    FILE(int fd, int mode)
        : m_fd(fd)
        , m_mode(mode)
    {
    }
    ~FILE();

    static FILE* create(int fd, int mode);

    void setbuf(u8* data, int mode, size_t size) { m_buffer.setbuf(data, mode, size); }

    bool flush();
    bool close();

    int fileno() const { return m_fd; }
    bool eof() const { return m_eof; }

    int error() const { return m_error; }
    void clear_err() { m_error = 0; }

    size_t read(u8*, size_t);
    size_t write(const u8*, size_t);

    bool gets(u8*, size_t);
    bool ungetc(u8 byte) { return m_buffer.enqueue_front(byte); }

    int seek(long offset, int whence);
    long tell();

    pid_t popen_child() { return m_popen_child; }
    void set_popen_child(pid_t child_pid) { m_popen_child = child_pid; }

    void reopen(int fd, int mode);

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

        bool may_use() const { return m_ungotten || m_mode != _IONBF; }
        bool is_not_empty() const { return m_ungotten || !m_empty; }
        size_t buffered_size() const;

        const u8* begin_dequeue(size_t& available_size) const;
        void did_dequeue(size_t actual_size);

        u8* begin_enqueue(size_t& available_size) const;
        void did_enqueue(size_t actual_size);

        bool enqueue_front(u8 byte);

    private:
        // Note: the fields here are arranged this way
        // to make sizeof(Buffer) smaller.
        u8* m_data { nullptr };
        size_t m_capacity { BUFSIZ };
        size_t m_begin { 0 };
        size_t m_end { 0 };

        int m_mode { -1 };
        u8 m_unget_buffer { 0 };
        bool m_ungotten : 1 { false };
        bool m_data_is_malloced : 1 { false };
        // When m_begin == m_end, we want to distinguish whether
        // the buffer is full or empty.
        bool m_empty : 1 { true };
    };

    // Read or write using the underlying fd, bypassing the buffer.
    ssize_t do_read(u8*, size_t);
    ssize_t do_write(const u8*, size_t);

    // Read some data into the buffer.
    bool read_into_buffer();
    // Flush *some* data from the buffer.
    bool write_from_buffer();

    int m_fd { -1 };
    int m_mode { 0 };
    int m_error { 0 };
    bool m_eof { false };
    pid_t m_popen_child { -1 };
    Buffer m_buffer;
};

FILE::~FILE()
{
    bool already_closed = m_fd == -1;
    ASSERT(already_closed);
}

FILE* FILE::create(int fd, int mode)
{
    void* file = calloc(1, sizeof(FILE));
    new (file) FILE(fd, mode);
    return (FILE*)file;
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
        size_t had_buffered = m_buffer.buffered_size();
        m_buffer.drop();
        // Attempt to reset the underlying file position to what the user
        // expects.
        int rc = lseek(m_fd, -had_buffered, SEEK_CUR);
        if (rc < 0) {
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

ssize_t FILE::do_write(const u8* data, size_t size)
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
    ASSERT(available_size);

    ssize_t nread = do_read(data, available_size);

    if (nread <= 0)
        return false;

    m_buffer.did_enqueue(nread);
    return true;
}

bool FILE::write_from_buffer()
{
    size_t size;
    const u8* data = m_buffer.begin_dequeue(size);
    // If we want to write, the buffer must have something in it!
    ASSERT(size);

    ssize_t nwritten = do_write(data, size);

    if (nwritten < 0)
        return false;

    m_buffer.did_dequeue(nwritten);
    return true;
}

size_t FILE::read(u8* data, size_t size)
{
    size_t total_read = 0;

    while (size > 0) {
        size_t actual_size;

        if (m_buffer.may_use()) {
            // Let's see if the buffer has something queued for us.
            size_t queued_size;
            const u8* queued_data = m_buffer.begin_dequeue(queued_size);
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

size_t FILE::write(const u8* data, size_t size)
{
    size_t total_written = 0;

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

bool FILE::gets(u8* data, size_t size)
{
    // gets() is a lot like read(), but it is different enough in how it
    // processes newlines and null-terminates the buffer that it deserves a
    // separate implementation.
    size_t total_read = 0;

    if (size == 0)
        return false;

    while (size > 1) {
        if (m_buffer.may_use()) {
            // Let's see if the buffer has something queued for us.
            size_t queued_size;
            const u8* queued_data = m_buffer.begin_dequeue(queued_size);
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
            u8* newline = reinterpret_cast<u8*>(memchr(queued_data, '\n', actual_size));
            if (newline)
                actual_size = newline - queued_data + 1;
            memcpy(data, queued_data, actual_size);
            m_buffer.did_dequeue(actual_size);
            total_read += actual_size;
            data += actual_size;
            size -= actual_size;
            if (newline)
                break;
        } else {
            // Sadly, we have to actually read these characters one by one.
            u8 byte;
            ssize_t nread = do_read(&byte, 1);
            if (nread <= 0) {
                *data = 0;
                return total_read > 0;
            }
            ASSERT(nread == 1);
            *data = byte;
            total_read++;
            data++;
            size--;
            if (byte == '\n')
                break;
        }
    }

    *data = 0;
    return total_read > 0;
}

int FILE::seek(long offset, int whence)
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

long FILE::tell()
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

FILE::Buffer::~Buffer()
{
    if (m_data_is_malloced)
        free(m_data);
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
    m_ungotten = false;
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

const u8* FILE::Buffer::begin_dequeue(size_t& available_size) const
{
    if (m_ungotten) {
        available_size = 1;
        return &m_unget_buffer;
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
    ASSERT(actual_size > 0);

    if (m_ungotten) {
        ASSERT(actual_size == 1);
        m_ungotten = false;
        return;
    }

    m_begin += actual_size;

    ASSERT(m_begin <= m_capacity);
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
    ASSERT(m_data != nullptr);

    if (m_begin < m_end || m_empty)
        available_size = m_capacity - m_end;
    else
        available_size = m_begin - m_end;

    return const_cast<u8*>(&m_data[m_end]);
}

void FILE::Buffer::did_enqueue(size_t actual_size)
{
    ASSERT(m_data != nullptr);
    ASSERT(actual_size > 0);

    m_end += actual_size;

    ASSERT(m_end <= m_capacity);
    if (m_end == m_capacity) {
        // Wrap around.
        m_end = 0;
    }

    m_empty = false;
}

bool FILE::Buffer::enqueue_front(u8 byte)
{
    if (m_ungotten) {
        // Sorry, the place is already taken!
        return false;
    }

    m_ungotten = true;
    m_unget_buffer = byte;
    return true;
}

extern "C" {

static u8 default_streams[3][sizeof(FILE)];
FILE* stdin = reinterpret_cast<FILE*>(&default_streams[0]);
FILE* stdout = reinterpret_cast<FILE*>(&default_streams[1]);
FILE* stderr = reinterpret_cast<FILE*>(&default_streams[2]);

void __stdio_init()
{
    new (stdin) FILE(0, O_RDONLY);
    new (stdout) FILE(1, O_WRONLY);
    new (stderr) FILE(2, O_WRONLY);
    stderr->setbuf(nullptr, _IONBF, 0);
}

int setvbuf(FILE* stream, char* buf, int mode, size_t size)
{
    ASSERT(stream);
    if (mode != _IONBF && mode != _IOLBF && mode != _IOFBF) {
        errno = EINVAL;
        return -1;
    }
    stream->setbuf(reinterpret_cast<u8*>(buf), mode, size);
    return 0;
}

void setbuf(FILE* stream, char* buf)
{
    setvbuf(stream, buf, buf ? _IOFBF : _IONBF, BUFSIZ);
}

void setlinebuf(FILE* stream)
{
    setvbuf(stream, nullptr, _IOLBF, 0);
}

int fileno(FILE* stream)
{
    ASSERT(stream);
    return stream->fileno();
}

int feof(FILE* stream)
{
    ASSERT(stream);
    return stream->eof();
}

int fflush(FILE* stream)
{
    if (!stream) {
        dbg() << "FIXME: fflush(nullptr) should flush all open streams";
        return 0;
    }
    return stream->flush() ? 0 : EOF;
}

char* fgets(char* buffer, int size, FILE* stream)
{
    ASSERT(stream);
    bool ok = stream->gets(reinterpret_cast<u8*>(buffer), size);
    return ok ? buffer : nullptr;
}

int fgetc(FILE* stream)
{
    ASSERT(stream);
    char ch;
    size_t nread = fread(&ch, sizeof(char), 1, stream);
    if (nread == 1)
        return ch;
    return EOF;
}

int getc(FILE* stream)
{
    return fgetc(stream);
}

int getc_unlocked(FILE* stream)
{
    return fgetc(stream);
}

int getchar()
{
    return getc(stdin);
}

ssize_t getdelim(char** lineptr, size_t* n, int delim, FILE* stream)
{
    char *ptr, *eptr;
    if (*lineptr == nullptr || *n == 0) {
        *n = BUFSIZ;
        if ((*lineptr = static_cast<char*>(malloc(*n))) == nullptr) {
            return -1;
        }
    }

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

ssize_t getline(char** lineptr, size_t* n, FILE* stream)
{
    return getdelim(lineptr, n, '\n', stream);
}

int ungetc(int c, FILE* stream)
{
    ASSERT(stream);
    bool ok = stream->ungetc(c);
    return ok ? c : EOF;
}

int fputc(int ch, FILE* stream)
{
    ASSERT(stream);
    u8 byte = ch;
    size_t nwritten = stream->write(&byte, 1);
    if (nwritten == 0)
        return EOF;
    ASSERT(nwritten == 1);
    return byte;
}

int putc(int ch, FILE* stream)
{
    return fputc(ch, stream);
}

int putchar(int ch)
{
    return putc(ch, stdout);
}

int fputs(const char* s, FILE* stream)
{
    ASSERT(stream);
    size_t len = strlen(s);
    size_t nwritten = stream->write(reinterpret_cast<const u8*>(s), len);
    if (nwritten < len)
        return EOF;
    return 1;
}

int puts(const char* s)
{
    int rc = fputs(s, stdout);
    if (rc == EOF)
        return EOF;
    return fputc('\n', stdout);
}

void clearerr(FILE* stream)
{
    ASSERT(stream);
    stream->clear_err();
}

int ferror(FILE* stream)
{
    ASSERT(stream);
    return stream->error();
}

size_t fread(void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    ASSERT(stream);
    ASSERT(!Checked<size_t>::multiplication_would_overflow(size, nmemb));

    size_t nread = stream->read(reinterpret_cast<u8*>(ptr), size * nmemb);
    return nread / size;
}

size_t fwrite(const void* ptr, size_t size, size_t nmemb, FILE* stream)
{
    ASSERT(stream);
    ASSERT(!Checked<size_t>::multiplication_would_overflow(size, nmemb));

    size_t nwritten = stream->write(reinterpret_cast<const u8*>(ptr), size * nmemb);
    return nwritten / size;
}

int fseek(FILE* stream, long offset, int whence)
{
    ASSERT(stream);
    return stream->seek(offset, whence);
}

long ftell(FILE* stream)
{
    ASSERT(stream);
    return stream->tell();
}

int fgetpos(FILE* stream, fpos_t* pos)
{
    ASSERT(stream);
    ASSERT(pos);

    long val = stream->tell();
    if (val == -1L)
        return 1;

    *pos = val;
    return 0;
}

int fsetpos(FILE* stream, const fpos_t* pos)
{
    ASSERT(stream);
    ASSERT(pos);

    return stream->seek((long)*pos, SEEK_SET);
}

void rewind(FILE* stream)
{
    ASSERT(stream);
    int rc = stream->seek(0, SEEK_SET);
    ASSERT(rc == 0);
}

int vdbgprintf(const char* fmt, va_list ap)
{
    return printf_internal([](char*&, char ch) { dbgputch(ch); }, nullptr, fmt, ap);
}

int dbgprintf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = printf_internal([](char*&, char ch) { dbgputch(ch); }, nullptr, fmt, ap);
    va_end(ap);
    return ret;
}

ALWAYS_INLINE void stdout_putch(char*&, char ch)
{
    putchar(ch);
}

static FILE* __current_stream = nullptr;
ALWAYS_INLINE static void stream_putch(char*&, char ch)
{
    fputc(ch, __current_stream);
}

int vfprintf(FILE* stream, const char* fmt, va_list ap)
{
    __current_stream = stream;
    return printf_internal(stream_putch, nullptr, fmt, ap);
}

int fprintf(FILE* stream, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vfprintf(stream, fmt, ap);
    va_end(ap);
    return ret;
}

int vprintf(const char* fmt, va_list ap)
{
    return printf_internal(stdout_putch, nullptr, fmt, ap);
}

int printf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vprintf(fmt, ap);
    va_end(ap);
    return ret;
}

static void buffer_putch(char*& bufptr, char ch)
{
    *bufptr++ = ch;
}

int vsprintf(char* buffer, const char* fmt, va_list ap)
{
    int ret = printf_internal(buffer_putch, buffer, fmt, ap);
    buffer[ret] = '\0';
    return ret;
}

int sprintf(char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsprintf(buffer, fmt, ap);
    va_end(ap);
    return ret;
}

static size_t __vsnprintf_space_remaining;
ALWAYS_INLINE void sized_buffer_putch(char*& bufptr, char ch)
{
    if (__vsnprintf_space_remaining) {
        *bufptr++ = ch;
        --__vsnprintf_space_remaining;
    }
}

int vsnprintf(char* buffer, size_t size, const char* fmt, va_list ap)
{
    if (size) {
        __vsnprintf_space_remaining = size - 1;
    } else {
        __vsnprintf_space_remaining = 0;
    }
    int ret = printf_internal(sized_buffer_putch, buffer, fmt, ap);
    if (__vsnprintf_space_remaining) {
        buffer[ret] = '\0';
    } else if (size > 0) {
        buffer[size - 1] = '\0';
    }
    return ret;
}

int snprintf(char* buffer, size_t size, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int ret = vsnprintf(buffer, size, fmt, ap);
    va_end(ap);
    return ret;
}

void perror(const char* s)
{
    int saved_errno = errno;
    dbg() << "perror(): " << s << ": " << strerror(saved_errno);
    fprintf(stderr, "%s: %s\n", s, strerror(saved_errno));
}

static int parse_mode(const char* mode)
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
            dbg() << "Potentially unsupported fopen mode _" << mode << "_ (because of '" << *ptr << "')";
        }
    }

    return flags;
}

FILE* fopen(const char* pathname, const char* mode)
{
    int flags = parse_mode(mode);
    int fd = open(pathname, flags, 0666);
    if (fd < 0)
        return nullptr;
    return FILE::create(fd, flags);
}

FILE* freopen(const char* pathname, const char* mode, FILE* stream)
{
    ASSERT(stream);
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

FILE* fdopen(int fd, const char* mode)
{
    int flags = parse_mode(mode);
    // FIXME: Verify that the mode matches how fd is already open.
    if (fd < 0)
        return nullptr;
    return FILE::create(fd, flags);
}

static inline bool is_default_stream(FILE* stream)
{
    return stream == stdin || stream == stdout || stream == stderr;
}

int fclose(FILE* stream)
{
    ASSERT(stream);
    bool ok = stream->close();
    ScopedValueRollback errno_restorer(errno);

    stream->~FILE();
    if (!is_default_stream(stream))
        free(stream);

    return ok ? 0 : EOF;
}

int rename(const char* oldpath, const char* newpath)
{
    if (!oldpath || !newpath) {
        errno = EFAULT;
        return -1;
    }
    Syscall::SC_rename_params params { { oldpath, strlen(oldpath) }, { newpath, strlen(newpath) } };
    int rc = syscall(SC_rename, &params);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

void dbgputch(char ch)
{
    syscall(SC_dbgputch, ch);
}

int dbgputstr(const char* characters, ssize_t length)
{
    int rc = syscall(SC_dbgputstr, characters, length);
    __RETURN_WITH_ERRNO(rc, rc, -1);
}

char* tmpnam(char*)
{
    ASSERT_NOT_REACHED();
}

FILE* popen(const char* command, const char* type)
{
    if (!type || (*type != 'r' && *type != 'w')) {
        errno = EINVAL;
        return nullptr;
    }

    int pipe_fds[2];

    int rc = pipe(pipe_fds);
    if (rc < 0) {
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
            int rc = dup2(pipe_fds[1], STDOUT_FILENO);
            if (rc < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        } else if (*type == 'w') {
            int rc = dup2(pipe_fds[0], STDIN_FILENO);
            if (rc < 0) {
                perror("dup2");
                exit(1);
            }
            close(pipe_fds[0]);
            close(pipe_fds[1]);
        }

        int rc = execl("/bin/sh", "sh", "-c", command, nullptr);
        if (rc < 0)
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

int pclose(FILE* stream)
{
    ASSERT(stream);
    ASSERT(stream->popen_child() != 0);

    int wstatus = 0;
    int rc = waitpid(stream->popen_child(), &wstatus, 0);
    if (rc < 0)
        return rc;

    return wstatus;
}

int remove(const char* pathname)
{
    int rc = unlink(pathname);
    if (rc < 0 && errno != EISDIR)
        return -1;
    return rmdir(pathname);
}

int scanf(const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stdin, fmt, ap);
    va_end(ap);
    return count;
}

int fscanf(FILE* stream, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vfscanf(stream, fmt, ap);
    va_end(ap);
    return count;
}

int sscanf(const char* buffer, const char* fmt, ...)
{
    va_list ap;
    va_start(ap, fmt);
    int count = vsscanf(buffer, fmt, ap);
    va_end(ap);
    return count;
}

int vfscanf(FILE* stream, const char* fmt, va_list ap)
{
    char buffer[BUFSIZ];
    if (!fgets(buffer, sizeof(buffer) - 1, stream))
        return -1;
    return vsscanf(buffer, fmt, ap);
}

void flockfile(FILE* filehandle)
{
    (void)filehandle;
    dbgprintf("FIXME: Implement flockfile()\n");
}

void funlockfile(FILE* filehandle)
{
    (void)filehandle;
    dbgprintf("FIXME: Implement funlockfile()\n");
}

FILE* tmpfile()
{
    char tmp_path[] = "/tmp/XXXXXX";
    if (__generate_unique_filename(tmp_path) < 0)
        return nullptr;

    int fd = open(tmp_path, O_CREAT | O_EXCL | O_RDWR, S_IWUSR | S_IRUSR);
    if (fd < 0)
        return nullptr;

    // FIXME: instead of using this hack, implement with O_TMPFILE or similar
    unlink(tmp_path);

    return fdopen(fd, "rw");
}
}
