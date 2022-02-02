/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/EnumBits.h>
#include <AK/Function.h>
#include <AK/IPv4Address.h>
#include <AK/MemMem.h>
#include <AK/Noncopyable.h>
#include <AK/Result.h>
#include <AK/Span.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibCore/Notifier.h>
#include <LibCore/SocketAddress.h>
#include <errno.h>
#include <netdb.h>

namespace Core::Stream {

/// The base, abstract class for stream operations. This class defines the
/// operations one can perform on every stream in LibCore.
class Stream {
public:
    virtual bool is_readable() const { return false; }
    /// Reads into a buffer, with the maximum size being the size of the buffer.
    /// The amount of bytes read can be smaller than the size of the buffer.
    /// Returns either the amount of bytes read, or an errno in the case of
    /// failure.
    virtual ErrorOr<size_t> read(Bytes) = 0;
    /// Tries to fill the entire buffer through reading. Returns whether the
    /// buffer was filled without an error.
    virtual bool read_or_error(Bytes);

    virtual bool is_writable() const { return false; }
    /// Tries to write the entire contents of the buffer. It is possible for
    /// less than the full buffer to be written. Returns either the amount of
    /// bytes written into the stream, or an errno in the case of failure.
    virtual ErrorOr<size_t> write(ReadonlyBytes) = 0;
    /// Same as write, but does not return until either the entire buffer
    /// contents are written or an error occurs. Returns whether the entire
    /// contents were written without an error.
    virtual bool write_or_error(ReadonlyBytes);

    /// Returns whether the stream has reached the end of file. For sockets,
    /// this most likely means that the protocol has disconnected (in the case
    /// of TCP). For seekable streams, this means the end of the file. Note that
    /// is_eof will only return true _after_ a read with 0 length, so this
    /// method should be called after a read.
    virtual bool is_eof() const = 0;

    virtual bool is_open() const = 0;
    virtual void close() = 0;

    virtual ~Stream()
    {
    }
};

enum class SeekMode {
    SetPosition,
    FromCurrentPosition,
    FromEndPosition,
};

/// Adds seekability to Core::Stream. Classes inheriting from SeekableStream
/// will be seekable to any point in the stream.
class SeekableStream : public Stream {
public:
    /// Seeks to the given position in the given mode. Returns either the
    /// current position of the file, or an errno in the case of an error.
    virtual ErrorOr<off_t> seek(i64 offset, SeekMode) = 0;
    /// Returns the current position of the file, or an errno in the case of
    /// an error.
    virtual ErrorOr<off_t> tell() const;
    /// Returns the total size of the stream, or an errno in the case of an
    /// error. May not preserve the original position on the stream on failure.
    virtual ErrorOr<off_t> size();
};

/// The Socket class is the base class for all concrete BSD-style socket
/// classes. Sockets are non-seekable streams which can be read byte-wise.
class Socket : public Stream {
public:
    Socket(Socket&&) = default;
    Socket& operator=(Socket&&) = default;

    /// Checks how many bytes of data are currently available to read on the
    /// socket. For datagram-based socket, this is the size of the first
    /// datagram that can be read. Returns either the amount of bytes, or an
    /// errno in the case of failure.
    virtual ErrorOr<size_t> pending_bytes() const = 0;
    /// Returns whether there's any data that can be immediately read, or an
    /// errno on failure.
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const = 0;
    // Sets the blocking mode of the socket. If blocking mode is disabled, reads
    // will fail with EAGAIN when there's no data available to read, and writes
    // will fail with EAGAIN when the data cannot be written without blocking
    // (due to the send buffer being full, for example).
    virtual ErrorOr<void> set_blocking(bool enabled) = 0;
    // Sets the close-on-exec mode of the socket. If close-on-exec mode is
    // enabled, then the socket will be automatically closed by the kernel when
    // an exec call happens.
    virtual ErrorOr<void> set_close_on_exec(bool enabled) = 0;

    /// Disables any listening mechanisms that this socket uses.
    /// Can be called with 'false' when `on_ready_to_read` notifications are no longer needed.
    /// Conversely, set_notifications_enabled(true) will re-enable notifications.
    virtual void set_notifications_enabled(bool) { }

    Function<void()> on_ready_to_read;

protected:
    enum class SocketDomain {
        Local,
        Inet,
    };

    enum class SocketType {
        Stream,
        Datagram,
    };

    Socket()
    {
    }

    static ErrorOr<int> create_fd(SocketDomain, SocketType);
    // FIXME: This will need to be updated when IPv6 socket arrives. Perhaps a
    //        base class for all address types is appropriate.
    static ErrorOr<IPv4Address> resolve_host(String const&, SocketType);

    static ErrorOr<void> connect_local(int fd, String const& path);
    static ErrorOr<void> connect_inet(int fd, SocketAddress const&);
};

/// A reusable socket maintains state about being connected in addition to
/// normal Socket capabilities, and can be reconnected once disconnected.
class ReusableSocket : public Socket {
public:
    /// Returns whether the socket is currently connected.
    virtual bool is_connected() = 0;
    /// Reconnects the socket to the given host and port. Returns EALREADY if
    /// is_connected() is true.
    virtual ErrorOr<void> reconnect(String const& host, u16 port) = 0;
    /// Connects the socket to the given socket address (IP address + port).
    /// Returns EALREADY is_connected() is true.
    virtual ErrorOr<void> reconnect(SocketAddress const&) = 0;
};

// Concrete classes.

enum class OpenMode : unsigned {
    NotOpen = 0,
    Read = 1,
    Write = 2,
    ReadWrite = 3,
    Append = 4,
    Truncate = 8,
    MustBeNew = 16,
    KeepOnExec = 32,
    Nonblocking = 64,
};

AK_ENUM_BITWISE_OPERATORS(OpenMode)

class File final : public SeekableStream {
    AK_MAKE_NONCOPYABLE(File);

public:
    static ErrorOr<NonnullOwnPtr<File>> open(StringView filename, OpenMode, mode_t = 0644);
    static ErrorOr<NonnullOwnPtr<File>> adopt_fd(int fd, OpenMode);

    File(File&& other) { operator=(move(other)); }

    File& operator=(File&& other)
    {
        if (&other == this)
            return *this;

        m_mode = exchange(other.m_mode, OpenMode::NotOpen);
        m_fd = exchange(other.m_fd, -1);
        m_last_read_was_eof = exchange(other.m_last_read_was_eof, false);
        return *this;
    }

    virtual bool is_readable() const override;
    virtual ErrorOr<size_t> read(Bytes) override;
    virtual bool is_writable() const override;
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;
    virtual ErrorOr<off_t> seek(i64 offset, SeekMode) override;

    virtual ~File() override { close(); }

private:
    File(OpenMode mode)
        : m_mode(mode)
    {
    }

    ErrorOr<void> open_path(StringView filename, mode_t);

    OpenMode m_mode { OpenMode::NotOpen };
    int m_fd { -1 };
    bool m_last_read_was_eof { false };
};

class PosixSocketHelper {
    AK_MAKE_NONCOPYABLE(PosixSocketHelper);

public:
    template<typename T>
    PosixSocketHelper(Badge<T>) requires(IsBaseOf<Socket, T>) { }

    PosixSocketHelper(PosixSocketHelper&& other)
    {
        operator=(move(other));
    }

    PosixSocketHelper& operator=(PosixSocketHelper&& other)
    {
        m_fd = exchange(other.m_fd, -1);
        m_last_read_was_eof = exchange(other.m_last_read_was_eof, false);
        m_notifier = move(other.m_notifier);
        return *this;
    }

    int fd() const { return m_fd; }
    void set_fd(int fd) { m_fd = fd; }

    ErrorOr<size_t> read(Bytes, int flags = 0);
    ErrorOr<size_t> write(ReadonlyBytes);

    bool is_eof() const { return !is_open() || m_last_read_was_eof; }
    bool is_open() const { return m_fd != -1; }
    void close();

    ErrorOr<size_t> pending_bytes() const;
    ErrorOr<bool> can_read_without_blocking(int timeout) const;

    ErrorOr<void> set_blocking(bool enabled);
    ErrorOr<void> set_close_on_exec(bool enabled);

    void setup_notifier();
    RefPtr<Core::Notifier> notifier() { return m_notifier; }

private:
    int m_fd { -1 };
    bool m_last_read_was_eof { false };
    RefPtr<Core::Notifier> m_notifier;
};

class TCPSocket final : public Socket {
public:
    static ErrorOr<NonnullOwnPtr<TCPSocket>> connect(String const& host, u16 port);
    static ErrorOr<NonnullOwnPtr<TCPSocket>> connect(SocketAddress const& address);
    static ErrorOr<NonnullOwnPtr<TCPSocket>> adopt_fd(int fd);

    TCPSocket(TCPSocket&& other)
        : Socket(static_cast<Socket&&>(other))
        , m_helper(move(other.m_helper))
    {
        if (is_open())
            setup_notifier();
    }

    TCPSocket& operator=(TCPSocket&& other)
    {
        Socket::operator=(static_cast<Socket&&>(other));
        m_helper = move(other.m_helper);
        if (is_open())
            setup_notifier();

        return *this;
    }

    virtual bool is_readable() const override { return is_open(); }
    virtual bool is_writable() const override { return is_open(); }
    virtual ErrorOr<size_t> read(Bytes buffer) override { return m_helper.read(buffer); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.write(buffer); }
    virtual bool is_eof() const override { return m_helper.is_eof(); }
    virtual bool is_open() const override { return m_helper.is_open(); };
    virtual void close() override { m_helper.close(); };
    virtual ErrorOr<size_t> pending_bytes() const override { return m_helper.pending_bytes(); }
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const override { return m_helper.can_read_without_blocking(timeout); }
    virtual void set_notifications_enabled(bool enabled) override
    {
        if (auto notifier = m_helper.notifier())
            notifier->set_enabled(enabled);
    }
    ErrorOr<void> set_blocking(bool enabled) override { return m_helper.set_blocking(enabled); }
    ErrorOr<void> set_close_on_exec(bool enabled) override { return m_helper.set_close_on_exec(enabled); }

    virtual ~TCPSocket() override { close(); }

private:
    TCPSocket()
    {
    }

    void setup_notifier()
    {
        VERIFY(is_open());

        m_helper.setup_notifier();
        m_helper.notifier()->on_ready_to_read = [this] {
            if (on_ready_to_read)
                on_ready_to_read();
        };
    }

    PosixSocketHelper m_helper { Badge<TCPSocket> {} };
};

class UDPSocket final : public Socket {
public:
    static ErrorOr<NonnullOwnPtr<UDPSocket>> connect(String const& host, u16 port);
    static ErrorOr<NonnullOwnPtr<UDPSocket>> connect(SocketAddress const& address);

    UDPSocket(UDPSocket&& other)
        : Socket(static_cast<Socket&&>(other))
        , m_helper(move(other.m_helper))
    {
        if (is_open())
            setup_notifier();
    }

    UDPSocket& operator=(UDPSocket&& other)
    {
        Socket::operator=(static_cast<Socket&&>(other));
        m_helper = move(other.m_helper);
        if (is_open())
            setup_notifier();

        return *this;
    }

    virtual ErrorOr<size_t> read(Bytes buffer) override
    {
        auto pending_bytes = TRY(this->pending_bytes());
        if (pending_bytes > buffer.size()) {
            // With UDP datagrams, reading a datagram into a buffer that's
            // smaller than the datagram's size will cause the rest of the
            // datagram to be discarded. That's not very nice, so let's bail
            // early, telling the caller that he should allocate a bigger
            // buffer.
            return Error::from_errno(EMSGSIZE);
        }

        return m_helper.read(buffer);
    }

    virtual bool is_readable() const override { return is_open(); }
    virtual bool is_writable() const override { return is_open(); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.write(buffer); }
    virtual bool is_eof() const override { return m_helper.is_eof(); }
    virtual bool is_open() const override { return m_helper.is_open(); }
    virtual void close() override { m_helper.close(); }
    virtual ErrorOr<size_t> pending_bytes() const override { return m_helper.pending_bytes(); }
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const override { return m_helper.can_read_without_blocking(timeout); }
    virtual void set_notifications_enabled(bool enabled) override
    {
        if (auto notifier = m_helper.notifier())
            notifier->set_enabled(enabled);
    }
    ErrorOr<void> set_blocking(bool enabled) override { return m_helper.set_blocking(enabled); }
    ErrorOr<void> set_close_on_exec(bool enabled) override { return m_helper.set_close_on_exec(enabled); }

    virtual ~UDPSocket() override { close(); }

private:
    UDPSocket() { }

    void setup_notifier()
    {
        VERIFY(is_open());

        m_helper.setup_notifier();
        m_helper.notifier()->on_ready_to_read = [this] {
            if (on_ready_to_read)
                on_ready_to_read();
        };
    }

    PosixSocketHelper m_helper { Badge<UDPSocket> {} };
};

class LocalSocket final : public Socket {
public:
    static ErrorOr<NonnullOwnPtr<LocalSocket>> connect(String const& path);
    static ErrorOr<NonnullOwnPtr<LocalSocket>> adopt_fd(int fd);

    LocalSocket(LocalSocket&& other)
        : Socket(static_cast<Socket&&>(other))
        , m_helper(move(other.m_helper))
    {
        if (is_open())
            setup_notifier();
    }

    LocalSocket& operator=(LocalSocket&& other)
    {
        Socket::operator=(static_cast<Socket&&>(other));
        m_helper = move(other.m_helper);
        if (is_open())
            setup_notifier();

        return *this;
    }

    virtual bool is_readable() const override { return is_open(); }
    virtual bool is_writable() const override { return is_open(); }
    virtual ErrorOr<size_t> read(Bytes buffer) override { return m_helper.read(buffer); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.write(buffer); }
    virtual bool is_eof() const override { return m_helper.is_eof(); }
    virtual bool is_open() const override { return m_helper.is_open(); }
    virtual void close() override { m_helper.close(); }
    virtual ErrorOr<size_t> pending_bytes() const override { return m_helper.pending_bytes(); }
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const override { return m_helper.can_read_without_blocking(timeout); }
    virtual ErrorOr<void> set_blocking(bool enabled) override { return m_helper.set_blocking(enabled); }
    virtual ErrorOr<void> set_close_on_exec(bool enabled) override { return m_helper.set_close_on_exec(enabled); }
    virtual void set_notifications_enabled(bool enabled) override
    {
        if (auto notifier = m_helper.notifier())
            notifier->set_enabled(enabled);
    }

    ErrorOr<int> receive_fd(int flags);
    ErrorOr<void> send_fd(int fd);
    ErrorOr<pid_t> peer_pid() const;
    ErrorOr<size_t> read_without_waiting(Bytes buffer);

    /// Release the fd associated with this LocalSocket. After the fd is
    /// released, the socket will be considered "closed" and all operations done
    /// on it will fail with ENOTCONN. Fails with ENOTCONN if the socket is
    /// already closed.
    ErrorOr<int> release_fd();

    virtual ~LocalSocket() { close(); }

private:
    LocalSocket() { }

    void setup_notifier()
    {
        VERIFY(is_open());

        m_helper.setup_notifier();
        m_helper.notifier()->on_ready_to_read = [this] {
            if (on_ready_to_read)
                on_ready_to_read();
        };
    }

    PosixSocketHelper m_helper { Badge<LocalSocket> {} };
};

// Buffered stream wrappers

template<typename T>
concept StreamLike = IsBaseOf<Stream, T>;
template<typename T>
concept SeekableStreamLike = IsBaseOf<SeekableStream, T>;
template<typename T>
concept SocketLike = IsBaseOf<Socket, T>;

template<typename T>
class BufferedHelper {
    AK_MAKE_NONCOPYABLE(BufferedHelper);

public:
    template<StreamLike U>
    BufferedHelper(Badge<U>, NonnullOwnPtr<T> stream, ByteBuffer buffer)
        : m_stream(move(stream))
        , m_buffer(move(buffer))
    {
    }

    BufferedHelper(BufferedHelper&& other)
        : m_stream(move(other.m_stream))
        , m_buffer(move(other.m_buffer))
        , m_buffered_size(exchange(other.m_buffered_size, 0))
    {
    }

    BufferedHelper& operator=(BufferedHelper&& other)
    {
        m_stream = move(other.m_stream);
        m_buffer = move(other.m_buffer);
        m_buffered_size = exchange(other.m_buffered_size, 0);
        return *this;
    }

    template<template<typename> typename BufferedType>
    static ErrorOr<NonnullOwnPtr<BufferedType<T>>> create_buffered(NonnullOwnPtr<T> stream, size_t buffer_size)
    {
        if (!buffer_size)
            return Error::from_errno(EINVAL);
        if (!stream->is_open())
            return Error::from_errno(ENOTCONN);

        auto buffer = TRY(ByteBuffer::create_uninitialized(buffer_size));

        return adopt_nonnull_own_or_enomem(new BufferedType<T>(move(stream), move(buffer)));
    }

    T& stream() { return *m_stream; }
    T const& stream() const { return *m_stream; }

    ErrorOr<size_t> read(Bytes buffer)
    {
        if (!stream().is_open())
            return Error::from_errno(ENOTCONN);
        if (!buffer.size())
            return Error::from_errno(ENOBUFS);

        // Let's try to take all we can from the buffer first.
        size_t buffer_nread = 0;
        if (m_buffered_size > 0) {
            // FIXME: Use a circular buffer to avoid shifting the buffer
            //        contents.
            size_t amount_to_take = min(buffer.size(), m_buffered_size);
            auto slice_to_take = m_buffer.span().slice(0, amount_to_take);
            auto slice_to_shift = m_buffer.span().slice(amount_to_take);

            slice_to_take.copy_to(buffer);
            buffer_nread += amount_to_take;

            if (amount_to_take < m_buffered_size) {
                m_buffer.overwrite(0, slice_to_shift.data(), m_buffered_size - amount_to_take);
            }
            m_buffered_size -= amount_to_take;
        }

        // If the buffer satisfied the request, then we need not continue.
        if (buffer_nread == buffer.size()) {
            return buffer_nread;
        }

        // Otherwise, let's try an extra read just in case there's something
        // in our receive buffer.
        auto stream_nread = TRY(stream().read(buffer.slice(buffer_nread)));

        // Fill the internal buffer if it has run dry.
        if (m_buffered_size == 0)
            TRY(populate_read_buffer());

        return buffer_nread + stream_nread;
    }

    // Reads into the buffer until \n is encountered.
    // The size of the Bytes object is the maximum amount of bytes that will be
    // read. Returns the amount of bytes read.
    ErrorOr<size_t> read_line(Bytes buffer)
    {
        return read_until(buffer, "\n"sv);
    }

    ErrorOr<size_t> read_until(Bytes buffer, StringView candidate)
    {
        return read_until_any_of(buffer, Array { candidate });
    }

    template<size_t N>
    ErrorOr<size_t> read_until_any_of(Bytes buffer, Array<StringView, N> candidates)
    {
        if (!stream().is_open())
            return Error::from_errno(ENOTCONN);

        if (buffer.is_empty())
            return Error::from_errno(ENOBUFS);

        // We fill the buffer through can_read_line.
        if (!TRY(can_read_line()))
            return 0;

        if (stream().is_eof()) {
            if (buffer.size() < m_buffered_size) {
                // Normally, reading from an EOFed stream and receiving bytes
                // would mean that the stream is no longer EOF. However, it's
                // possible with a buffered stream that the user is able to read
                // the buffer contents even when the underlying stream is EOF.
                // We already violate this invariant once by giving the user the
                // chance to read the remaining buffer contents, but if the user
                // doesn't give us a big enough buffer, then we would be
                // violating the invariant twice the next time the user attempts
                // to read, which is No Good. So let's give a descriptive error
                // to the caller about why it can't read.
                return Error::from_errno(EMSGSIZE);
            }
        }

        // The intention here is to try to match all of the possible
        // delimiter candidates and try to find the longest one we can
        // remove from the buffer after copying up to the delimiter to the
        // user buffer.
        Optional<size_t> longest_match;
        size_t match_size = 0;
        for (auto& candidate : candidates) {
            auto result = AK::memmem_optional(m_buffer.data(), m_buffered_size, candidate.bytes().data(), candidate.bytes().size());
            if (result.has_value()) {
                auto previous_match = longest_match.value_or(*result);
                if ((previous_match < *result) || (previous_match == *result && match_size < candidate.length())) {
                    longest_match = result;
                    match_size = candidate.length();
                }
            }
        }
        if (longest_match.has_value()) {
            auto size_written_to_user_buffer = *longest_match;
            auto buffer_to_take = m_buffer.span().slice(0, size_written_to_user_buffer);
            auto buffer_to_shift = m_buffer.span().slice(size_written_to_user_buffer + match_size);

            buffer_to_take.copy_to(buffer);
            m_buffer.overwrite(0, buffer_to_shift.data(), buffer_to_shift.size());

            m_buffered_size -= size_written_to_user_buffer + match_size;

            return size_written_to_user_buffer;
        }

        // If we still haven't found anything, then it's most likely the case
        // that the delimiter ends beyond the length of the caller-passed
        // buffer. Let's just fill the caller's buffer up.
        auto readable_size = min(m_buffered_size, buffer.size());
        auto buffer_to_take = m_buffer.span().slice(0, readable_size);
        auto buffer_to_shift = m_buffer.span().slice(readable_size);

        buffer_to_take.copy_to(buffer);
        m_buffer.overwrite(0, buffer_to_shift.data(), buffer_to_shift.size());

        m_buffered_size -= readable_size;

        return readable_size;
    }

    // Returns whether a line can be read, populating the buffer in the process.
    ErrorOr<bool> can_read_line()
    {
        if (stream().is_eof() && m_buffered_size > 0)
            return true;

        if (m_buffer.span().slice(0, m_buffered_size).contains_slow('\n'))
            return true;

        if (!stream().is_readable())
            return false;

        while (m_buffered_size < m_buffer.size()) {
            auto populated_slice = TRY(populate_read_buffer());

            if (stream().is_eof()) {
                // We give the user one last hurrah to read the remaining
                // contents as a "line".
                return m_buffered_size > 0;
            }

            if (populated_slice.contains_slow('\n'))
                return true;

            if (populated_slice.is_empty())
                break;
        }

        return false;
    }

    bool is_eof() const
    {
        if (m_buffered_size > 0) {
            return false;
        }

        return stream().is_eof();
    }

    size_t buffer_size() const
    {
        return m_buffer.size();
    }

    size_t buffered_data_size() const
    {
        return m_buffered_size;
    }

    void clear_buffer()
    {
        m_buffered_size = 0;
    }

private:
    ErrorOr<ReadonlyBytes> populate_read_buffer()
    {
        if (m_buffered_size == m_buffer.size())
            return ReadonlyBytes {};

        auto fillable_slice = m_buffer.span().slice(m_buffered_size);
        auto nread = TRY(stream().read(fillable_slice));
        m_buffered_size += nread;
        return fillable_slice.slice(0, nread);
    }

    NonnullOwnPtr<T> m_stream;
    // FIXME: Replacing this with a circular buffer would be really nice and
    //        would avoid excessive copies; however, right now
    //        AK::CircularDuplexBuffer inlines its entire contents, and that
    //        would make for a very large object on the stack.
    //
    //        The proper fix is to make a CircularQueue which uses a buffer on
    //        the heap.
    ByteBuffer m_buffer;
    size_t m_buffered_size { 0 };
};

// NOTE: A Buffered which accepts any Stream could be added here, but it is not
//       needed at the moment.

template<SeekableStreamLike T>
class BufferedSeekable final : public SeekableStream {
    friend BufferedHelper<T>;

public:
    static ErrorOr<NonnullOwnPtr<BufferedSeekable<T>>> create(NonnullOwnPtr<T> stream, size_t buffer_size = 16384)
    {
        return BufferedHelper<T>::template create_buffered<BufferedSeekable>(move(stream), buffer_size);
    }

    BufferedSeekable(BufferedSeekable&& other) = default;
    BufferedSeekable& operator=(BufferedSeekable&& other) = default;

    virtual bool is_readable() const override { return m_helper.stream().is_readable(); }
    virtual ErrorOr<size_t> read(Bytes buffer) override { return m_helper.read(move(buffer)); }
    virtual bool is_writable() const override { return m_helper.stream().is_writable(); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.stream().write(buffer); }
    virtual bool is_eof() const override { return m_helper.is_eof(); }
    virtual bool is_open() const override { return m_helper.stream().is_open(); }
    virtual void close() override { m_helper.stream().close(); }
    virtual ErrorOr<off_t> seek(i64 offset, SeekMode mode) override
    {
        auto result = TRY(m_helper.stream().seek(offset, mode));
        m_helper.clear_buffer();
        return result;
    }

    ErrorOr<size_t> read_line(Bytes buffer) { return m_helper.read_line(move(buffer)); }
    ErrorOr<size_t> read_until(Bytes buffer, StringView candidate) { return m_helper.read_until(move(buffer), move(candidate)); }
    template<size_t N>
    ErrorOr<size_t> read_until_any_of(Bytes buffer, Array<StringView, N> candidates) { return m_helper.read_until_any_of(move(buffer), move(candidates)); }
    ErrorOr<bool> can_read_line() { return m_helper.can_read_line(); }

    size_t buffer_size() const { return m_helper.buffer_size(); }

    virtual ~BufferedSeekable() override { }

private:
    BufferedSeekable(NonnullOwnPtr<T> stream, ByteBuffer buffer)
        : m_helper(Badge<BufferedSeekable<T>> {}, move(stream), buffer)
    {
    }

    BufferedHelper<T> m_helper;
};

class BufferedSocketBase : public Socket {
public:
    virtual ErrorOr<size_t> read_line(Bytes buffer) = 0;
    virtual ErrorOr<size_t> read_until(Bytes buffer, StringView candidate) = 0;
    virtual ErrorOr<bool> can_read_line() = 0;
    virtual size_t buffer_size() const = 0;
};

template<SocketLike T>
class BufferedSocket final : public BufferedSocketBase {
    friend BufferedHelper<T>;

public:
    static ErrorOr<NonnullOwnPtr<BufferedSocket<T>>> create(NonnullOwnPtr<T> stream, size_t buffer_size = 16384)
    {
        return BufferedHelper<T>::template create_buffered<BufferedSocket>(move(stream), buffer_size);
    }

    BufferedSocket(BufferedSocket&& other)
        : BufferedSocketBase(static_cast<BufferedSocketBase&&>(other))
        , m_helper(move(other.m_helper))
    {
        setup_notifier();
    }

    BufferedSocket& operator=(BufferedSocket&& other)
    {
        Socket::operator=(static_cast<Socket&&>(other));
        m_helper = move(other.m_helper);

        setup_notifier();
        return *this;
    }

    virtual bool is_readable() const override { return m_helper.stream().is_readable(); }
    virtual ErrorOr<size_t> read(Bytes buffer) override { return m_helper.read(move(buffer)); }
    virtual bool is_writable() const override { return m_helper.stream().is_writable(); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.stream().write(buffer); }
    virtual bool is_eof() const override { return m_helper.is_eof(); }
    virtual bool is_open() const override { return m_helper.stream().is_open(); }
    virtual void close() override { m_helper.stream().close(); }
    virtual ErrorOr<size_t> pending_bytes() const override
    {
        return TRY(m_helper.stream().pending_bytes()) + m_helper.buffered_data_size();
    }
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const override { return m_helper.buffered_data_size() > 0 || TRY(m_helper.stream().can_read_without_blocking(timeout)); }
    virtual ErrorOr<void> set_blocking(bool enabled) override { return m_helper.stream().set_blocking(enabled); }
    virtual ErrorOr<void> set_close_on_exec(bool enabled) override { return m_helper.stream().set_close_on_exec(enabled); }
    virtual void set_notifications_enabled(bool enabled) override { m_helper.stream().set_notifications_enabled(enabled); }

    virtual ErrorOr<size_t> read_line(Bytes buffer) override { return m_helper.read_line(move(buffer)); }
    virtual ErrorOr<size_t> read_until(Bytes buffer, StringView candidate) override { return m_helper.read_until(move(buffer), move(candidate)); }
    template<size_t N>
    ErrorOr<size_t> read_until_any_of(Bytes buffer, Array<StringView, N> candidates) { return m_helper.read_until_any_of(move(buffer), move(candidates)); }
    virtual ErrorOr<bool> can_read_line() override { return m_helper.can_read_line(); }

    virtual size_t buffer_size() const override { return m_helper.buffer_size(); }

    virtual ~BufferedSocket() override { }

private:
    BufferedSocket(NonnullOwnPtr<T> stream, ByteBuffer buffer)
        : m_helper(Badge<BufferedSocket<T>> {}, move(stream), buffer)
    {
        setup_notifier();
    }

    void setup_notifier()
    {
        m_helper.stream().on_ready_to_read = [this] {
            if (on_ready_to_read)
                on_ready_to_read();
        };
    }

    BufferedHelper<T> m_helper;
};

using BufferedFile = BufferedSeekable<File>;
using BufferedTCPSocket = BufferedSocket<TCPSocket>;
using BufferedUDPSocket = BufferedSocket<UDPSocket>;
using BufferedLocalSocket = BufferedSocket<LocalSocket>;

/// A BasicReusableSocket allows one to use one of the base Core::Stream classes
/// as a ReusableSocket. It does not preserve any connection state or options,
/// and instead just recreates the stream when reconnecting.
template<SocketLike T>
class BasicReusableSocket final : public ReusableSocket {
public:
    static ErrorOr<NonnullOwnPtr<BasicReusableSocket<T>>> connect(String const& host, u16 port)
    {
        return make<BasicReusableSocket<T>>(TRY(T::connect(host, port)));
    }

    static ErrorOr<NonnullOwnPtr<BasicReusableSocket<T>>> connect(SocketAddress const& address)
    {
        return make<BasicReusableSocket<T>>(TRY(T::connect(address)));
    }

    virtual bool is_connected() override
    {
        return m_socket.is_open();
    }

    virtual ErrorOr<void> reconnect(String const& host, u16 port) override
    {
        if (is_connected())
            return Error::from_errno(EALREADY);

        m_socket = TRY(T::connect(host, port));
        return {};
    }

    virtual ErrorOr<void> reconnect(SocketAddress const& address) override
    {
        if (is_connected())
            return Error::from_errno(EALREADY);

        m_socket = TRY(T::connect(address));
        return {};
    }

    virtual bool is_readable() const override { return m_socket.is_readable(); }
    virtual ErrorOr<size_t> read(Bytes buffer) override { return m_socket.read(move(buffer)); }
    virtual bool is_writable() const override { return m_socket.is_writable(); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_socket.write(buffer); }
    virtual bool is_eof() const override { return m_socket.is_eof(); }
    virtual bool is_open() const override { return m_socket.is_open(); }
    virtual void close() override { m_socket.close(); }
    virtual ErrorOr<size_t> pending_bytes() const override { return m_socket.pending_bytes(); }
    virtual ErrorOr<bool> can_read_without_blocking(int timeout = 0) const override { return m_socket.can_read_without_blocking(timeout); }
    virtual ErrorOr<void> set_blocking(bool enabled) override { return m_socket.set_blocking(enabled); }
    virtual ErrorOr<void> set_close_on_exec(bool enabled) override { return m_socket.set_close_on_exec(enabled); }

private:
    BasicReusableSocket(NonnullOwnPtr<T> socket)
        : m_socket(move(socket))
    {
    }

    NonnullOwnPtr<T> m_socket;
};

using ReusableTCPSocket = BasicReusableSocket<TCPSocket>;
using ReusableUDPSocket = BasicReusableSocket<UDPSocket>;

}
