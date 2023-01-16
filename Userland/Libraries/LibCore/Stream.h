/*
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/CircularBuffer.h>
#include <AK/DeprecatedString.h>
#include <AK/EnumBits.h>
#include <AK/Function.h>
#include <AK/IPv4Address.h>
#include <AK/Noncopyable.h>
#include <AK/Result.h>
#include <AK/Span.h>
#include <AK/Time.h>
#include <AK/Variant.h>
#include <LibCore/Notifier.h>
#include <LibCore/SocketAddress.h>
#include <LibIPC/Forward.h>
#include <errno.h>
#include <netdb.h>

namespace Core::Stream {

template<DerivedFrom<Core::Stream::Stream> T>
class Handle {
public:
    template<DerivedFrom<T> U>
    Handle(NonnullOwnPtr<U> handle)
        : m_handle(adopt_own<T>(*handle.leak_ptr()))
    {
    }

    // This is made `explicit` to not accidentally create a non-owning Handle,
    // which may not always be intended.
    explicit Handle(T& handle)
        : m_handle(&handle)
    {
    }

    T* ptr()
    {
        if (m_handle.template has<T*>())
            return m_handle.template get<T*>();
        else
            return m_handle.template get<NonnullOwnPtr<T>>();
    }

    T const* ptr() const
    {
        if (m_handle.template has<T*>())
            return m_handle.template get<T*>();
        else
            return m_handle.template get<NonnullOwnPtr<T>>();
    }

    T* operator->() { return ptr(); }
    T const* operator->() const { return ptr(); }

    T& operator*() { return *ptr(); }
    T const& operator*() const { return *ptr(); }

private:
    Variant<NonnullOwnPtr<T>, T*> m_handle;
};

/// The base, abstract class for stream operations. This class defines the
/// operations one can perform on every stream in LibCore.
class Stream {
public:
    /// Reads into a buffer, with the maximum size being the size of the buffer.
    /// The amount of bytes read can be smaller than the size of the buffer.
    /// Returns either the bytes that were read, or an errno in the case of
    /// failure.
    virtual ErrorOr<Bytes> read(Bytes) = 0;
    /// Tries to fill the entire buffer through reading. Returns whether the
    /// buffer was filled without an error.
    virtual ErrorOr<void> read_entire_buffer(Bytes);
    /// Reads the stream until EOF, storing the contents into a ByteBuffer which
    /// is returned once EOF is encountered. The block size determines the size
    /// of newly allocated chunks while reading.
    virtual ErrorOr<ByteBuffer> read_until_eof(size_t block_size = 4096);
    /// Discards the given number of bytes from the stream. As this is usually used
    /// as an efficient version of `read_entire_buffer`, it returns an error
    /// if reading failed or if not all bytes could be discarded.
    /// Unless specifically overwritten, this just uses read() to read into an
    /// internal stack-based buffer.
    virtual ErrorOr<void> discard(size_t discarded_bytes);

    /// Tries to write the entire contents of the buffer. It is possible for
    /// less than the full buffer to be written. Returns either the amount of
    /// bytes written into the stream, or an errno in the case of failure.
    virtual ErrorOr<size_t> write(ReadonlyBytes) = 0;
    /// Same as write, but does not return until either the entire buffer
    /// contents are written or an error occurs.
    virtual ErrorOr<void> write_entire_buffer(ReadonlyBytes);

    // This is a wrapper around `write_entire_buffer` that is compatible with
    // `write_or_error`. This is required by some templated code in LibProtocol
    // that needs to work with either type of stream.
    // TODO: Fully port or wrap `Request::stream_into_impl` into `Core::Stream` and remove this.
    bool write_or_error(ReadonlyBytes buffer)
    {
        return !write_entire_buffer(buffer).is_error();
    }

    template<typename T>
    requires(Traits<T>::is_trivially_serializable())
    ErrorOr<T> read_value()
    {
        alignas(T) u8 buffer[sizeof(T)] = {};
        TRY(read_entire_buffer({ &buffer, sizeof(buffer) }));
        return bit_cast<T>(buffer);
    }

    template<typename T>
    requires(Traits<T>::is_trivially_serializable())
    ErrorOr<void> write_value(T const& value)
    {
        return write_entire_buffer({ &value, sizeof(value) });
    }

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

protected:
    /// Provides a default implementation of read_until_eof that works for streams
    /// that behave like POSIX file descriptors. expected_file_size can be
    /// passed as a heuristic for what the Stream subclass expects the file
    /// content size to be in order to reduce allocations (does not affect
    /// actual reading).
    ErrorOr<ByteBuffer> read_until_eof_impl(size_t block_size, size_t expected_file_size = 0);
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
    /// Shrinks or extends the stream to the given size. Returns an errno in
    /// the case of an error.
    virtual ErrorOr<void> truncate(off_t length) = 0;
    /// Seeks until after the given amount of bytes to be discarded instead of
    /// reading and discarding everything manually;
    virtual ErrorOr<void> discard(size_t discarded_bytes) override;
};

enum class PreventSIGPIPE {
    No,
    Yes,
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

    Socket(PreventSIGPIPE prevent_sigpipe = PreventSIGPIPE::No)
        : m_prevent_sigpipe(prevent_sigpipe == PreventSIGPIPE::Yes)
    {
    }

    static ErrorOr<int> create_fd(SocketDomain, SocketType);
    // FIXME: This will need to be updated when IPv6 socket arrives. Perhaps a
    //        base class for all address types is appropriate.
    static ErrorOr<IPv4Address> resolve_host(DeprecatedString const&, SocketType);

    static ErrorOr<void> connect_local(int fd, DeprecatedString const& path);
    static ErrorOr<void> connect_inet(int fd, SocketAddress const&);

    int default_flags() const
    {
        int flags = 0;
        if (m_prevent_sigpipe)
            flags |= MSG_NOSIGNAL;
        return flags;
    }

private:
    bool m_prevent_sigpipe { false };
};

/// A reusable socket maintains state about being connected in addition to
/// normal Socket capabilities, and can be reconnected once disconnected.
class ReusableSocket : public Socket {
public:
    /// Returns whether the socket is currently connected.
    virtual bool is_connected() = 0;
    /// Reconnects the socket to the given host and port. Returns EALREADY if
    /// is_connected() is true.
    virtual ErrorOr<void> reconnect(DeprecatedString const& host, u16 port) = 0;
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

enum class ShouldCloseFileDescriptor {
    Yes,
    No,
};

AK_ENUM_BITWISE_OPERATORS(OpenMode)

class File final : public SeekableStream {
    AK_MAKE_NONCOPYABLE(File);

public:
    static ErrorOr<NonnullOwnPtr<File>> open(StringView filename, OpenMode, mode_t = 0644);
    static ErrorOr<NonnullOwnPtr<File>> adopt_fd(int fd, OpenMode, ShouldCloseFileDescriptor = ShouldCloseFileDescriptor::Yes);

    static ErrorOr<NonnullOwnPtr<File>> standard_input();
    static ErrorOr<NonnullOwnPtr<File>> standard_output();
    static ErrorOr<NonnullOwnPtr<File>> standard_error();
    static ErrorOr<NonnullOwnPtr<File>> open_file_or_standard_stream(StringView filename, OpenMode mode);

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

    virtual ErrorOr<Bytes> read(Bytes) override;
    virtual ErrorOr<ByteBuffer> read_until_eof(size_t block_size = 4096) override;
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;
    virtual ErrorOr<off_t> seek(i64 offset, SeekMode) override;
    virtual ErrorOr<void> truncate(off_t length) override;

    int leak_fd(Badge<::IPC::File>)
    {
        m_should_close_file_descriptor = ShouldCloseFileDescriptor::No;
        return m_fd;
    }

    int fd() const
    {
        return m_fd;
    }

    virtual ~File() override
    {
        if (m_should_close_file_descriptor == ShouldCloseFileDescriptor::Yes)
            close();
    }

    static int open_mode_to_options(OpenMode mode);

private:
    File(OpenMode mode, ShouldCloseFileDescriptor should_close = ShouldCloseFileDescriptor::Yes)
        : m_mode(mode)
        , m_should_close_file_descriptor(should_close)
    {
    }

    ErrorOr<void> open_path(StringView filename, mode_t);

    OpenMode m_mode { OpenMode::NotOpen };
    int m_fd { -1 };
    bool m_last_read_was_eof { false };
    ShouldCloseFileDescriptor m_should_close_file_descriptor { ShouldCloseFileDescriptor::Yes };
};

class PosixSocketHelper {
    AK_MAKE_NONCOPYABLE(PosixSocketHelper);

public:
    template<typename T>
    PosixSocketHelper(Badge<T>)
    requires(IsBaseOf<Socket, T>)
    {
    }

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

    ErrorOr<Bytes> read(Bytes, int flags);
    ErrorOr<size_t> write(ReadonlyBytes, int flags);

    bool is_eof() const { return !is_open() || m_last_read_was_eof; }
    bool is_open() const { return m_fd != -1; }
    void close();

    ErrorOr<size_t> pending_bytes() const;
    ErrorOr<bool> can_read_without_blocking(int timeout) const;

    ErrorOr<void> set_blocking(bool enabled);
    ErrorOr<void> set_close_on_exec(bool enabled);
    ErrorOr<void> set_receive_timeout(Time timeout);

    void setup_notifier();
    RefPtr<Core::Notifier> notifier() { return m_notifier; }

private:
    int m_fd { -1 };
    bool m_last_read_was_eof { false };
    RefPtr<Core::Notifier> m_notifier;
};

class TCPSocket final : public Socket {
public:
    static ErrorOr<NonnullOwnPtr<TCPSocket>> connect(DeprecatedString const& host, u16 port);
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

    virtual ErrorOr<Bytes> read(Bytes buffer) override { return m_helper.read(buffer, default_flags()); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.write(buffer, default_flags()); }
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
    TCPSocket(PreventSIGPIPE prevent_sigpipe = PreventSIGPIPE::No)
        : Socket(prevent_sigpipe)
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
    static ErrorOr<NonnullOwnPtr<UDPSocket>> connect(DeprecatedString const& host, u16 port, Optional<Time> timeout = {});
    static ErrorOr<NonnullOwnPtr<UDPSocket>> connect(SocketAddress const& address, Optional<Time> timeout = {});

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

    virtual ErrorOr<Bytes> read(Bytes buffer) override
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

        return m_helper.read(buffer, default_flags());
    }

    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.write(buffer, default_flags()); }
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
    UDPSocket(PreventSIGPIPE prevent_sigpipe = PreventSIGPIPE::No)
        : Socket(prevent_sigpipe)
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

    PosixSocketHelper m_helper { Badge<UDPSocket> {} };
};

class LocalSocket final : public Socket {
public:
    static ErrorOr<NonnullOwnPtr<LocalSocket>> connect(DeprecatedString const& path, PreventSIGPIPE = PreventSIGPIPE::No);
    static ErrorOr<NonnullOwnPtr<LocalSocket>> adopt_fd(int fd, PreventSIGPIPE = PreventSIGPIPE::No);

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

    virtual ErrorOr<Bytes> read(Bytes buffer) override { return m_helper.read(buffer, default_flags()); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.write(buffer, default_flags()); }
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
    ErrorOr<Bytes> read_without_waiting(Bytes buffer);

    /// Release the fd associated with this LocalSocket. After the fd is
    /// released, the socket will be considered "closed" and all operations done
    /// on it will fail with ENOTCONN. Fails with ENOTCONN if the socket is
    /// already closed.
    ErrorOr<int> release_fd();

    Optional<int> fd() const;
    RefPtr<Core::Notifier> notifier() { return m_helper.notifier(); }

    virtual ~LocalSocket() { close(); }

private:
    LocalSocket(PreventSIGPIPE prevent_sigpipe = PreventSIGPIPE::No)
        : Socket(prevent_sigpipe)
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
    BufferedHelper(Badge<U>, NonnullOwnPtr<T> stream, CircularBuffer buffer)
        : m_stream(move(stream))
        , m_buffer(move(buffer))
    {
    }

    BufferedHelper(BufferedHelper&& other)
        : m_stream(move(other.m_stream))
        , m_buffer(move(other.m_buffer))
    {
    }

    BufferedHelper& operator=(BufferedHelper&& other)
    {
        m_stream = move(other.m_stream);
        m_buffer = move(other.m_buffer);
        return *this;
    }

    template<template<typename> typename BufferedType>
    static ErrorOr<NonnullOwnPtr<BufferedType<T>>> create_buffered(NonnullOwnPtr<T> stream, size_t buffer_size)
    {
        if (!buffer_size)
            return Error::from_errno(EINVAL);
        if (!stream->is_open())
            return Error::from_errno(ENOTCONN);

        auto buffer = TRY(CircularBuffer::create_empty(buffer_size));

        return adopt_nonnull_own_or_enomem(new BufferedType<T>(move(stream), move(buffer)));
    }

    T& stream() { return *m_stream; }
    T const& stream() const { return *m_stream; }

    ErrorOr<Bytes> read(Bytes buffer)
    {
        if (!stream().is_open())
            return Error::from_errno(ENOTCONN);
        if (buffer.is_empty())
            return buffer;

        // Fill the internal buffer if it has run dry.
        if (m_buffer.used_space() == 0)
            TRY(populate_read_buffer());

        // Let's try to take all we can from the buffer first.
        return m_buffer.read(buffer);
    }

    // Reads into the buffer until \n is encountered.
    // The size of the Bytes object is the maximum amount of bytes that will be
    // read. Returns the bytes read as a StringView.
    ErrorOr<StringView> read_line(Bytes buffer)
    {
        return StringView { TRY(read_until(buffer, "\n"sv)) };
    }

    ErrorOr<Bytes> read_until(Bytes buffer, StringView candidate)
    {
        return read_until_any_of(buffer, Array { candidate });
    }

    template<size_t N>
    ErrorOr<Bytes> read_until_any_of(Bytes buffer, Array<StringView, N> candidates)
    {
        if (!stream().is_open())
            return Error::from_errno(ENOTCONN);

        if (buffer.is_empty())
            return buffer;

        auto const candidate = TRY(find_and_populate_until_any_of(candidates, buffer.size()));

        if (stream().is_eof()) {
            if (buffer.size() < m_buffer.used_space()) {
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

        if (candidate.has_value()) {
            auto const read_bytes = m_buffer.read(buffer.trim(candidate->offset));
            TRY(m_buffer.discard(candidate->size));
            return read_bytes;
        }

        // If we still haven't found anything, then it's most likely the case
        // that the delimiter ends beyond the length of the caller-passed
        // buffer. Let's just fill the caller's buffer up.
        return m_buffer.read(buffer);
    }

    struct Match {
        size_t offset {};
        size_t size {};
    };

    template<size_t N>
    ErrorOr<Optional<Match>> find_and_populate_until_any_of(Array<StringView, N> const& candidates, Optional<size_t> max_offset = {})
    {
        Optional<size_t> longest_candidate;
        for (auto& candidate : candidates) {
            if (candidate.length() >= longest_candidate.value_or(candidate.length()))
                longest_candidate = candidate.length();
        }

        // The intention here is to try to match all the possible
        // delimiter candidates and try to find the longest one we can
        // remove from the buffer after copying up to the delimiter to the
        // user buffer.

        auto const find_candidates = [this, &candidates, &longest_candidate](size_t min_offset, Optional<size_t> max_offset = {}) -> Optional<Match> {
            auto const corrected_minimum_offset = *longest_candidate > min_offset ? 0 : min_offset - *longest_candidate;
            max_offset = max_offset.value_or(m_buffer.used_space());

            Optional<size_t> longest_match;
            size_t match_size = 0;
            for (auto& candidate : candidates) {
                auto const result = m_buffer.offset_of(candidate, corrected_minimum_offset, *max_offset);

                if (result.has_value()) {
                    auto previous_match = longest_match.value_or(*result);
                    if ((previous_match < *result) || (previous_match == *result && match_size < candidate.length())) {
                        longest_match = result;
                        match_size = candidate.length();
                    }
                }
            }

            if (longest_match.has_value())
                return Match { *longest_match, match_size };

            return {};
        };

        if (auto first_find = find_candidates(0, max_offset); first_find.has_value())
            return first_find;

        auto last_size = m_buffer.used_space();

        while (m_buffer.used_space() < max_offset.value_or(m_buffer.capacity())) {
            auto const read_bytes = TRY(populate_read_buffer());
            if (read_bytes == 0)
                break;

            if (auto first_find = find_candidates(last_size, max_offset); first_find.has_value())
                return first_find;
            last_size = m_buffer.used_space();
        }

        return Optional<Match> {};
    }

    // Returns whether a line can be read, populating the buffer in the process.
    ErrorOr<bool> can_read_line()
    {
        if (stream().is_eof())
            return m_buffer.used_space() > 0;

        return TRY(find_and_populate_until_any_of(Array<StringView, 1> { "\n"sv })).has_value();
    }

    bool is_eof() const
    {
        if (m_buffer.used_space() > 0) {
            return false;
        }

        return stream().is_eof();
    }

    size_t buffer_size() const
    {
        return m_buffer.capacity();
    }

    size_t buffered_data_size() const
    {
        return m_buffer.used_space();
    }

    void clear_buffer()
    {
        m_buffer.clear();
    }

private:
    ErrorOr<size_t> populate_read_buffer()
    {
        if (m_buffer.empty_space() == 0)
            return 0;

        // TODO: Figure out if we can do direct writes in a comfortable way.
        Array<u8, 1024> temporary_buffer;
        auto const fillable_slice = temporary_buffer.span().trim(min(temporary_buffer.size(), m_buffer.empty_space()));
        size_t nread = 0;
        do {
            auto result = stream().read(fillable_slice);
            if (result.is_error()) {
                if (!result.error().is_errno())
                    return result.error();
                if (result.error().code() == EINTR)
                    continue;
                if (result.error().code() == EAGAIN)
                    break;
                return result.error();
            }
            auto const filled_slice = result.value();
            VERIFY(m_buffer.write(filled_slice) == filled_slice.size());
            nread += filled_slice.size();
            break;
        } while (true);
        return nread;
    }

    NonnullOwnPtr<T> m_stream;
    CircularBuffer m_buffer;
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

    virtual ErrorOr<Bytes> read(Bytes buffer) override { return m_helper.read(move(buffer)); }
    virtual ErrorOr<size_t> write(ReadonlyBytes buffer) override { return m_helper.stream().write(buffer); }
    virtual bool is_eof() const override { return m_helper.is_eof(); }
    virtual bool is_open() const override { return m_helper.stream().is_open(); }
    virtual void close() override { m_helper.stream().close(); }
    virtual ErrorOr<off_t> seek(i64 offset, SeekMode mode) override
    {
        if (mode == SeekMode::FromCurrentPosition)
            offset = offset - m_helper.buffered_data_size();

        auto result = TRY(m_helper.stream().seek(offset, mode));
        m_helper.clear_buffer();

        return result;
    }
    virtual ErrorOr<void> truncate(off_t length) override
    {
        return m_helper.stream().truncate(length);
    }

    ErrorOr<StringView> read_line(Bytes buffer) { return m_helper.read_line(move(buffer)); }
    ErrorOr<Bytes> read_until(Bytes buffer, StringView candidate) { return m_helper.read_until(move(buffer), move(candidate)); }
    template<size_t N>
    ErrorOr<Bytes> read_until_any_of(Bytes buffer, Array<StringView, N> candidates) { return m_helper.read_until_any_of(move(buffer), move(candidates)); }
    ErrorOr<bool> can_read_line() { return m_helper.can_read_line(); }

    size_t buffer_size() const { return m_helper.buffer_size(); }

    virtual ~BufferedSeekable() override = default;

private:
    BufferedSeekable(NonnullOwnPtr<T> stream, CircularBuffer buffer)
        : m_helper(Badge<BufferedSeekable<T>> {}, move(stream), move(buffer))
    {
    }

    BufferedHelper<T> m_helper;
};

class BufferedSocketBase : public Socket {
public:
    virtual ErrorOr<StringView> read_line(Bytes buffer) = 0;
    virtual ErrorOr<Bytes> read_until(Bytes buffer, StringView candidate) = 0;
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

    virtual ErrorOr<Bytes> read(Bytes buffer) override { return m_helper.read(move(buffer)); }
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

    virtual ErrorOr<StringView> read_line(Bytes buffer) override { return m_helper.read_line(move(buffer)); }
    virtual ErrorOr<Bytes> read_until(Bytes buffer, StringView candidate) override { return m_helper.read_until(move(buffer), move(candidate)); }
    template<size_t N>
    ErrorOr<Bytes> read_until_any_of(Bytes buffer, Array<StringView, N> candidates) { return m_helper.read_until_any_of(move(buffer), move(candidates)); }
    virtual ErrorOr<bool> can_read_line() override { return m_helper.can_read_line(); }

    virtual size_t buffer_size() const override { return m_helper.buffer_size(); }

    virtual ~BufferedSocket() override = default;

private:
    BufferedSocket(NonnullOwnPtr<T> stream, CircularBuffer buffer)
        : m_helper(Badge<BufferedSocket<T>> {}, move(stream), move(buffer))
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
    static ErrorOr<NonnullOwnPtr<BasicReusableSocket<T>>> connect(DeprecatedString const& host, u16 port)
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

    virtual ErrorOr<void> reconnect(DeprecatedString const& host, u16 port) override
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

    virtual ErrorOr<Bytes> read(Bytes buffer) override { return m_socket.read(move(buffer)); }
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

// Note: This is only a temporary hack, to break up the task of moving away from AK::Stream into smaller parts.
class WrappedAKInputStream final : public Stream {
public:
    WrappedAKInputStream(NonnullOwnPtr<InputStream> stream);
    virtual ErrorOr<Bytes> read(Bytes) override;
    virtual ErrorOr<void> discard(size_t discarded_bytes) override;
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;

private:
    NonnullOwnPtr<InputStream> m_stream;
};

// Note: This is only a temporary hack, to break up the task of moving away from AK::Stream into smaller parts.
class WrappedAKOutputStream final : public Stream {
public:
    WrappedAKOutputStream(NonnullOwnPtr<OutputStream> stream);
    virtual ErrorOr<Bytes> read(Bytes) override;
    virtual ErrorOr<size_t> write(ReadonlyBytes) override;
    virtual bool is_eof() const override;
    virtual bool is_open() const override;
    virtual void close() override;

private:
    NonnullOwnPtr<OutputStream> m_stream;
};

// Note: This is only a temporary hack, to break up the task of moving away from AK::Stream into smaller parts.
class WrapInAKInputStream final : public InputStream {
public:
    WrapInAKInputStream(Core::Stream::Stream& stream);
    virtual size_t read(Bytes) override;
    virtual bool unreliable_eof() const override;
    virtual bool read_or_error(Bytes) override;
    virtual bool discard_or_error(size_t count) override;

private:
    Core::Stream::Stream& m_stream;
};

// Note: This is only a temporary hack, to break up the task of moving away from AK::Stream into smaller parts.
class WrapInAKOutputStream final : public OutputStream {
public:
    WrapInAKOutputStream(Core::Stream::Stream& stream);
    virtual size_t write(ReadonlyBytes) override;
    virtual bool write_or_error(ReadonlyBytes) override;

private:
    Core::Stream::Stream& m_stream;
};

}
