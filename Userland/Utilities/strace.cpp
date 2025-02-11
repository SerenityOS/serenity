/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Format.h>
#include <AK/HashTable.h>
#include <AK/IPv4Address.h>
#include <AK/StdLibExtras.h>
#include <AK/Types.h>
#include <Kernel/API/SyscallString.h>
#include <Kernel/API/prctl_numbers.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <signal.h>
#include <stdio.h>
#include <string.h>
#include <sys/arch/regs.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/ptrace.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>

#define HANDLE(VALUE) \
    case VALUE:       \
        return #VALUE##sv;
#define VALUES_TO_NAMES(FUNC_NAME)         \
    static ByteString FUNC_NAME(int value) \
    {                                      \
        switch (value) {
#define END_VALUES_TO_NAMES()                  \
    }                                          \
    return ByteString::formatted("{}", value); \
    }

VALUES_TO_NAMES(errno_name)
HANDLE(EPERM)
HANDLE(ENOENT)
HANDLE(ESRCH)
HANDLE(EINTR)
HANDLE(EIO)
HANDLE(ENXIO)
HANDLE(E2BIG)
HANDLE(ENOEXEC)
HANDLE(EBADF)
HANDLE(ECHILD)
HANDLE(EAGAIN)
HANDLE(ENOMEM)
HANDLE(EACCES)
HANDLE(EFAULT)
HANDLE(ENOTBLK)
HANDLE(EBUSY)
HANDLE(EEXIST)
HANDLE(EXDEV)
HANDLE(ENODEV)
HANDLE(ENOTDIR)
HANDLE(EISDIR)
HANDLE(EINVAL)
HANDLE(ENFILE)
HANDLE(EMFILE)
HANDLE(ENOTTY)
HANDLE(ETXTBSY)
HANDLE(EFBIG)
HANDLE(ENOSPC)
HANDLE(ESPIPE)
HANDLE(EROFS)
HANDLE(EMLINK)
HANDLE(EPIPE)
HANDLE(ERANGE)
HANDLE(ENAMETOOLONG)
HANDLE(ELOOP)
HANDLE(EOVERFLOW)
HANDLE(EOPNOTSUPP)
HANDLE(ENOSYS)
HANDLE(ENOTIMPL)
HANDLE(EAFNOSUPPORT)
HANDLE(ENOTSOCK)
HANDLE(EADDRINUSE)
HANDLE(ENOTEMPTY)
HANDLE(EDOM)
HANDLE(ECONNREFUSED)
HANDLE(EHOSTDOWN)
HANDLE(EADDRNOTAVAIL)
HANDLE(EISCONN)
HANDLE(ECONNABORTED)
HANDLE(EALREADY)
HANDLE(ECONNRESET)
HANDLE(EDESTADDRREQ)
HANDLE(EHOSTUNREACH)
HANDLE(EILSEQ)
HANDLE(EMSGSIZE)
HANDLE(ENETDOWN)
HANDLE(ENETUNREACH)
HANDLE(ENETRESET)
HANDLE(ENOBUFS)
HANDLE(ENOLCK)
HANDLE(ENOMSG)
HANDLE(ENOPROTOOPT)
HANDLE(ENOTCONN)
HANDLE(ESHUTDOWN)
HANDLE(ETOOMANYREFS)
HANDLE(EPROTONOSUPPORT)
HANDLE(ESOCKTNOSUPPORT)
HANDLE(EDEADLK)
HANDLE(ETIMEDOUT)
HANDLE(EPROTOTYPE)
HANDLE(EINPROGRESS)
HANDLE(ENOTHREAD)
HANDLE(EPROTO)
HANDLE(ENOTSUP)
HANDLE(EPFNOSUPPORT)
HANDLE(EDIRINTOSELF)
HANDLE(EDQUOT)
HANDLE(EMAXERRNO)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(whence_name)
HANDLE(SEEK_SET)
HANDLE(SEEK_CUR)
HANDLE(SEEK_END)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(ioctl_request_name)
HANDLE(TIOCGPGRP)
HANDLE(TIOCSPGRP)
HANDLE(TCGETS)
HANDLE(TCSETS)
HANDLE(TCSETSW)
HANDLE(TCSETSF)
HANDLE(TCFLSH)
HANDLE(TIOCGWINSZ)
HANDLE(TIOCSCTTY)
HANDLE(TIOCSTI)
HANDLE(TIOCNOTTY)
HANDLE(TIOCSWINSZ)
HANDLE(GRAPHICS_IOCTL_GET_PROPERTIES)
HANDLE(GRAPHICS_IOCTL_SET_HEAD_MODE_SETTING)
HANDLE(GRAPHICS_IOCTL_GET_HEAD_MODE_SETTING)
HANDLE(GRAPHICS_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER)
HANDLE(GRAPHICS_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER)
HANDLE(GRAPHICS_IOCTL_FLUSH_HEAD_BUFFERS)
HANDLE(GRAPHICS_IOCTL_FLUSH_HEAD)
HANDLE(KEYBOARD_IOCTL_GET_NUM_LOCK)
HANDLE(KEYBOARD_IOCTL_SET_NUM_LOCK)
HANDLE(KEYBOARD_IOCTL_GET_CAPS_LOCK)
HANDLE(KEYBOARD_IOCTL_SET_CAPS_LOCK)
HANDLE(SIOCSIFADDR)
HANDLE(SIOCGIFADDR)
HANDLE(SIOCGIFHWADDR)
HANDLE(SIOCGIFNAME)
HANDLE(SIOCGIFINDEX)
HANDLE(SIOCGIFNETMASK)
HANDLE(SIOCSIFNETMASK)
HANDLE(SIOCGIFBRDADDR)
HANDLE(SIOCGIFMTU)
HANDLE(SIOCGIFFLAGS)
HANDLE(SIOCGIFCONF)
HANDLE(SIOCADDRT)
HANDLE(SIOCDELRT)
HANDLE(SIOCSARP)
HANDLE(SIOCDARP)
HANDLE(FIBMAP)
HANDLE(FIONBIO)
HANDLE(FIONREAD)
HANDLE(KCOV_SETBUFSIZE)
HANDLE(KCOV_ENABLE)
HANDLE(KCOV_DISABLE)
HANDLE(SOUNDCARD_IOCTL_SET_SAMPLE_RATE)
HANDLE(SOUNDCARD_IOCTL_GET_SAMPLE_RATE)
HANDLE(STORAGE_DEVICE_GET_SIZE)
HANDLE(STORAGE_DEVICE_GET_BLOCK_SIZE)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(domain_name)
HANDLE(AF_UNSPEC)
HANDLE(AF_UNIX)
HANDLE(AF_INET)
HANDLE(AF_INET6)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(socket_type_name)
HANDLE(SOCK_STREAM)
HANDLE(SOCK_DGRAM)
HANDLE(SOCK_RAW)
HANDLE(SOCK_RDM)
HANDLE(SOCK_SEQPACKET)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(protocol_name)
HANDLE(PF_UNSPEC)
HANDLE(PF_UNIX)
HANDLE(PF_INET)
HANDLE(PF_INET6)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(clockid_name)
HANDLE(CLOCK_REALTIME)
HANDLE(CLOCK_MONOTONIC)
HANDLE(CLOCK_REALTIME_COARSE)
HANDLE(CLOCK_MONOTONIC_COARSE)
END_VALUES_TO_NAMES()

VALUES_TO_NAMES(prctl_option_name)
HANDLE(PR_SET_DUMPABLE)
HANDLE(PR_GET_DUMPABLE)
HANDLE(PR_SET_NO_NEW_SYSCALL_REGION_ANNOTATIONS)
HANDLE(PR_GET_NO_NEW_SYSCALL_REGION_ANNOTATIONS)
HANDLE(PR_SET_COREDUMP_METADATA_VALUE)
HANDLE(PR_SET_PROCESS_NAME)
HANDLE(PR_GET_PROCESS_NAME)
HANDLE(PR_SET_THREAD_NAME)
HANDLE(PR_GET_THREAD_NAME)
END_VALUES_TO_NAMES()

static int g_pid = -1;

using syscall_arg_t = u64;

static void handle_sigint(int)
{
    if (g_pid == -1)
        return;

    if (ptrace(PT_DETACH, g_pid, 0, 0) == -1) {
        perror("detach");
    }
}

static ErrorOr<void> copy_from_process(void const* source, Bytes target)
{
    return Core::System::ptrace_peekbuf(g_pid, const_cast<void*>(source), target);
}

static ErrorOr<ByteBuffer> copy_from_process(void const* source, size_t length)
{
    auto buffer = TRY(ByteBuffer::create_uninitialized(length));
    TRY(copy_from_process(source, buffer.bytes()));
    return buffer;
}

template<typename T>
static ErrorOr<T> copy_from_process(T const* source)
{
    T value {};
    TRY(copy_from_process(source, Bytes { &value, sizeof(T) }));
    return value;
}

struct BitflagOption {
    int value;
    StringView name;
};

#define BITFLAG(NAME)   \
    BitflagOption       \
    {                   \
        NAME, #NAME##sv \
    }

struct BitflagBase {
    int flagset;
    // Derivatives must define 'options', like so:
    // static constexpr auto options = { BITFLAG(O_CREAT), BITFLAG(O_DIRECTORY) };
};

namespace AK {
template<typename BitflagDerivative>
requires(IsBaseOf<BitflagBase, BitflagDerivative>) && requires { BitflagDerivative::options; }
struct Formatter<BitflagDerivative> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder& format_builder, BitflagDerivative const& value)
    {
        bool had_any_output = false;
        int remaining = value.flagset;

        for (BitflagOption const& option : BitflagDerivative::options) {
            if ((remaining & option.value) != option.value)
                continue;
            remaining &= ~option.value;
            if (had_any_output)
                TRY(format_builder.put_literal(" | "sv));
            TRY(format_builder.put_literal(option.name));
            had_any_output = true;
        }

        if (remaining != 0) {
            // No more BitflagOptions are available. Any remaining flags are unrecognized.
            if (had_any_output)
                TRY(format_builder.put_literal(" | "sv));
            format_builder.builder().appendff("{:#x} (?)", static_cast<unsigned>(remaining));
            had_any_output = true;
        }

        if (!had_any_output) {
            if constexpr (requires { BitflagDerivative::default_; })
                TRY(format_builder.put_literal(BitflagDerivative::default_));
            else
                TRY(format_builder.put_literal("0"sv));
        }

        return {};
    }
};
}

struct PointerArgument {
    void const* value;
};

namespace AK {
template<>
struct Formatter<PointerArgument> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder& format_builder, PointerArgument const& value)
    {
        auto& builder = format_builder.builder();
        if (value.value == nullptr)
            builder.append("null"sv);
        else
            builder.appendff("{}", value.value);
        return {};
    }
};
}

struct StringArgument {
    Syscall::StringArgument argument;
    StringView trim_by {};
};

namespace AK {
template<>
struct Formatter<StringArgument> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder& format_builder, StringArgument const& string_argument)
    {
        auto& builder = format_builder.builder();
        if (string_argument.argument.characters == nullptr) {
            builder.append("null"sv);
            return {};
        }

        // TODO: Avoid trying to copy excessively long strings.
        auto string_buffer = copy_from_process(string_argument.argument.characters, string_argument.argument.length);
        if (string_buffer.is_error()) {
            builder.appendff("{}{{{:p}, {}b}}", string_buffer.error(), (void const*)string_argument.argument.characters, string_argument.argument.length);
        } else {
            auto view = StringView(string_buffer.value());
            if (!string_argument.trim_by.is_empty())
                view = view.trim(string_argument.trim_by);
            builder.appendff("\"{}\"", view);
        }

        return {};
    }
};
}

class FormattedSyscallBuilder {
public:
    FormattedSyscallBuilder(StringView syscall_name)
    {
        m_builder.append(syscall_name);
        m_builder.append('(');
    }

    template<typename T>
    void add_argument(CheckedFormatString<T> format, T&& arg)
    {
        add_argument_separator();
        m_builder.appendff(format.view(), forward<T>(arg));
    }

    template<typename T>
    void add_argument(T&& arg)
    {
        add_argument("{}", forward<T>(arg));
    }

    template<typename... Ts>
    void add_arguments(Ts&&... args)
    {
        (add_argument(forward<Ts>(args)), ...);
    }

    template<typename T>
    void format_result_no_error(T res)
    {
        m_builder.appendff(") = {}\n", res);
    }

    void format_result(Integral auto res)
    {
        m_builder.append(") = "sv);
        if (res < 0)
            m_builder.appendff("{} {}", res, errno_name(-(int)res));
        else
            m_builder.appendff("{}", res);
        m_builder.append('\n');
    }

    void format_result(void* res)
    {
        if (res == MAP_FAILED)
            m_builder.append(") = MAP_FAILED\n"sv);
        else if (FlatPtr(res) > FlatPtr(-EMAXERRNO))
            m_builder.appendff(") = {} {}\n", res, errno_name(-static_cast<int>(FlatPtr(res))));
        else
            m_builder.appendff(") = {}\n", res);
    }

    void format_result()
    {
        m_builder.append(")\n"sv);
    }

    StringView string_view()
    {
        return m_builder.string_view();
    }

private:
    void add_argument_separator()
    {
        if (!m_first_arg) {
            m_builder.append(", "sv);
        }
        m_first_arg = false;
    }

    StringBuilder m_builder;
    bool m_first_arg { true };
};

static void format_getrandom(FormattedSyscallBuilder& builder, void* buffer, size_t size, unsigned flags)
{
    builder.add_arguments(buffer, size, flags);
}

static ErrorOr<void> format_realpath(FormattedSyscallBuilder& builder, Syscall::SC_realpath_params* params_p, size_t length)
{
    auto params = TRY(copy_from_process(params_p));
    builder.add_arguments(StringArgument { params.path }, StringArgument { { params.buffer.data, min(params.buffer.size, length) } });
    return {};
}

static void format_exit(FormattedSyscallBuilder& builder, int status)
{
    builder.add_argument(status);
}

struct OpenOptions : BitflagBase {
    static constexpr auto options = {
        BITFLAG(O_RDWR), BITFLAG(O_RDONLY), BITFLAG(O_WRONLY),
        BITFLAG(O_EXEC), BITFLAG(O_CREAT), BITFLAG(O_EXCL), BITFLAG(O_NOCTTY),
        BITFLAG(O_TRUNC), BITFLAG(O_APPEND), BITFLAG(O_NONBLOCK), BITFLAG(O_DIRECTORY),
        BITFLAG(O_NOFOLLOW), BITFLAG(O_CLOEXEC), BITFLAG(O_DIRECT)
    };
};

static ErrorOr<void> format_open(FormattedSyscallBuilder& builder, Syscall::SC_open_params* params_p)
{
    auto params = TRY(copy_from_process(params_p));

    if (params.dirfd == AT_FDCWD)
        builder.add_argument("AT_FDCWD");
    else
        builder.add_argument(params.dirfd);

    builder.add_arguments(StringArgument { params.path }, OpenOptions { params.options });

    if (params.options & O_CREAT)
        builder.add_argument("{:04o}", params.mode);
    return {};
}

static void format_ioctl(FormattedSyscallBuilder& builder, int fd, unsigned request, void* arg)
{
    builder.add_arguments(fd, ioctl_request_name(request));
    if (request == FIONBIO) {
        auto value = copy_from_process(reinterpret_cast<int*>(arg));
        builder.add_argument(value.release_value_but_fixme_should_propagate_errors());
    } else
        builder.add_argument(PointerArgument { arg });
}

namespace AK {
template<>
struct Formatter<struct timespec> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& format_builder, struct timespec value)
    {
        auto& builder = format_builder.builder();
        builder.appendff("{{tv_sec={}, tv_nsec={}}}", value.tv_sec, value.tv_nsec);
        return {};
    }
};

template<>
struct Formatter<struct timeval> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& format_builder, struct timeval value)
    {
        auto& builder = format_builder.builder();
        builder.appendff("{{tv_sec={}, tv_usec={}}}", value.tv_sec, value.tv_usec);
        return {};
    }
};

template<>
struct Formatter<struct stat> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& format_builder, struct stat value)
    {
        auto& builder = format_builder.builder();
        builder.appendff(
            "{{st_dev={}, st_ino={}, st_mode={}, st_nlink={}, st_uid={}, st_gid={}, st_rdev={}, "
            "st_size={}, st_blksize={}, st_blocks={}, st_atim={}, st_mtim={}, st_ctim={}}}",
            value.st_dev, value.st_ino, value.st_mode, value.st_nlink, value.st_uid, value.st_gid, value.st_rdev,
            value.st_size, value.st_blksize, value.st_blocks, value.st_atim, value.st_mtim, value.st_ctim);
        return {};
    }
};
}

static void format_chdir(FormattedSyscallBuilder& builder, char const* path_p, size_t length)
{
    auto buf = copy_from_process(path_p, length);
    if (buf.is_error())
        builder.add_arguments(buf.error());
    else
        builder.add_arguments(StringView { buf.value().data(), buf.value().size() });
}

static void format_fstat(FormattedSyscallBuilder& builder, int fd, struct stat* buf_p)
{
    auto buf = copy_from_process(buf_p);
    builder.add_arguments(fd, buf.release_value_but_fixme_should_propagate_errors());
}

static ErrorOr<void> format_stat(FormattedSyscallBuilder& builder, Syscall::SC_stat_params* params_p)
{
    auto params = TRY(copy_from_process(params_p));
    if (params.dirfd == AT_FDCWD)
        builder.add_argument("AT_FDCWD");
    else
        builder.add_argument(params.dirfd);
    builder.add_arguments(StringArgument { params.path }, TRY(copy_from_process(params.statbuf)), params.follow_symlinks);
    return {};
}

static void format_lseek(FormattedSyscallBuilder& builder, int fd, off_t offset, int whence)
{
    builder.add_arguments(fd, offset, whence_name(whence));
}

static void format_read(FormattedSyscallBuilder& builder, int fd, void* buf, size_t nbyte)
{
    builder.add_arguments(fd, buf, nbyte);
}

static void format_write(FormattedSyscallBuilder& builder, int fd, void* buf, size_t nbyte)
{
    builder.add_arguments(fd, buf, nbyte);
}

static void format_close(FormattedSyscallBuilder& builder, int fd)
{
    builder.add_arguments(fd);
}

static ErrorOr<void> format_pledge(FormattedSyscallBuilder& builder, Syscall::SC_pledge_params* params_p)
{
    auto params = TRY(copy_from_process(params_p));
    builder.add_arguments(
        StringArgument { params.promises },
        StringArgument { params.execpromises });
    return {};
}

static ErrorOr<void> format_poll(FormattedSyscallBuilder& builder, Syscall::SC_poll_params* params_p)
{
    // TODO: format fds and sigmask properly
    auto params = TRY(copy_from_process(params_p));
    builder.add_arguments(
        params.nfds,
        PointerArgument { params.fds },
        TRY(copy_from_process(params.timeout)),
        PointerArgument { params.sigmask });
    return {};
}

namespace AK {
template<>
struct Formatter<struct sockaddr> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& format_builder, struct sockaddr address)
    {
        auto& builder = format_builder.builder();
        builder.append("{sa_family="sv);
        builder.append(domain_name(address.sa_family));
        if (address.sa_family == AF_INET) {
            auto* address_in = (const struct sockaddr_in*)&address;
            builder.appendff(
                ", sin_port={}, sin_addr={}",
                address_in->sin_port,
                IPv4Address(address_in->sin_addr.s_addr).to_byte_string());
        } else if (address.sa_family == AF_UNIX) {
            auto* address_un = (const struct sockaddr_un*)&address;
            builder.appendff(
                ", sun_path={}",
                address_un->sun_path);
        }
        builder.append('}');
        return {};
    }
};
}

static void format_socket(FormattedSyscallBuilder& builder, int domain, int type, int protocol)
{
    // TODO: show additional options in type
    builder.add_arguments(domain_name(domain), socket_type_name(type & SOCK_TYPE_MASK), protocol_name(protocol));
}

static void format_connect(FormattedSyscallBuilder& builder, int socket, const struct sockaddr* address_p, socklen_t address_len)
{
    builder.add_arguments(socket, copy_from_process(address_p).release_value_but_fixme_should_propagate_errors(), address_len);
}

struct MsgOptions : BitflagBase {
    static constexpr auto options = {
        BITFLAG(MSG_TRUNC), BITFLAG(MSG_CTRUNC), BITFLAG(MSG_PEEK),
        BITFLAG(MSG_OOB), BITFLAG(MSG_DONTROUTE), BITFLAG(MSG_WAITALL),
        BITFLAG(MSG_DONTWAIT)
    };
};

static void format_recvmsg(FormattedSyscallBuilder& builder, int socket, struct msghdr* message, int flags)
{
    // TODO: format message
    builder.add_arguments(socket, message, MsgOptions { flags });
}

struct MmapFlags : BitflagBase {
    static constexpr auto options = {
        BITFLAG(MAP_SHARED), BITFLAG(MAP_PRIVATE), BITFLAG(MAP_FIXED), BITFLAG(MAP_ANONYMOUS),
        BITFLAG(MAP_RANDOMIZED), BITFLAG(MAP_STACK), BITFLAG(MAP_NORESERVE), BITFLAG(MAP_PURGEABLE),
        BITFLAG(MAP_FIXED_NOREPLACE)
    };
    static constexpr StringView default_ = "MAP_FILE"sv;
};

struct MemoryProtectionFlags : BitflagBase {
    static constexpr auto options = {
        BITFLAG(PROT_READ), BITFLAG(PROT_WRITE), BITFLAG(PROT_EXEC)
    };
    static constexpr StringView default_ = "PROT_NONE"sv;
};

static ErrorOr<void> format_mmap(FormattedSyscallBuilder& builder, Syscall::SC_mmap_params* params_p)
{
    auto params = TRY(copy_from_process(params_p));
    builder.add_arguments(params.addr, params.size, MemoryProtectionFlags { params.prot }, MmapFlags { params.flags }, params.fd, params.offset, params.alignment, StringArgument { params.name });
    return {};
}

static void format_munmap(FormattedSyscallBuilder& builder, void* addr, size_t size)
{
    builder.add_arguments(addr, size);
}

static void format_mprotect(FormattedSyscallBuilder& builder, void* addr, size_t size, int prot)
{
    builder.add_arguments(addr, size, MemoryProtectionFlags { prot });
}

static ErrorOr<void> format_set_mmap_name(FormattedSyscallBuilder& builder, Syscall::SC_set_mmap_name_params* params_p)
{
    auto params = TRY(copy_from_process(params_p));
    builder.add_arguments(params.addr, params.size, StringArgument { params.name });
    return {};
}

static void format_clock_gettime(FormattedSyscallBuilder& builder, clockid_t clockid, struct timespec* time)
{
    builder.add_arguments(clockid_name(clockid), copy_from_process(time).release_value_but_fixme_should_propagate_errors());
}

static void format_dbgputstr(FormattedSyscallBuilder& builder, char* characters, size_t size)
{
    builder.add_argument(StringArgument { { characters, size }, "\0\n"sv });
}

static void format_kill(FormattedSyscallBuilder& builder, pid_t pid_or_pgid, int signal)
{
    builder.add_argument(pid_or_pgid);
    auto* signal_name = getsignalname(signal);
    if (signal_name)
        builder.add_argument(signal_name);
    else
        builder.add_argument(signal);
}

static void format_prctl(FormattedSyscallBuilder& builder, int option, size_t arg1, size_t arg2, size_t arg3)
{
    builder.add_argument(prctl_option_name(option));
    switch (option) {
    case PR_SET_DUMPABLE:
    case PR_SET_NO_NEW_SYSCALL_REGION_ANNOTATIONS:
        builder.add_argument((bool)arg1);
        break;
    case PR_GET_DUMPABLE:
    case PR_GET_NO_NEW_SYSCALL_REGION_ANNOTATIONS:
        break;
    default:
        builder.add_arguments(arg1, arg2, arg3);
        break;
    }
}

static ErrorOr<void> format_syscall(FormattedSyscallBuilder& builder, Syscall::Function syscall_function, syscall_arg_t arg1, syscall_arg_t arg2, syscall_arg_t arg3, syscall_arg_t arg4, syscall_arg_t res)
{
    enum ResultType {
        Int,
        Ssize,
        VoidP,
        Void
    };

    ResultType result_type { Int };
    switch (syscall_function) {
    case SC_clock_gettime:
        format_clock_gettime(builder, (clockid_t)arg1, (struct timespec*)arg2);
        break;
    case SC_close:
        format_close(builder, (int)arg1);
        break;
    case SC_connect:
        format_connect(builder, (int)arg1, (const struct sockaddr*)arg2, (socklen_t)arg3);
        break;
    case SC_dbgputstr:
        format_dbgputstr(builder, (char*)arg1, (size_t)arg2);
        break;
    case SC_exit:
        format_exit(builder, (int)arg1);
        result_type = Void;
        break;
    case SC_fstat:
        format_fstat(builder, (int)arg1, (struct stat*)arg2);
        result_type = Ssize;
        break;
    case SC_chdir:
        format_chdir(builder, (char const*)arg1, (size_t)arg2);
        result_type = Int;
        break;
    case SC_getrandom:
        format_getrandom(builder, (void*)arg1, (size_t)arg2, (unsigned)arg3);
        break;
    case SC_ioctl:
        format_ioctl(builder, (int)arg1, (unsigned)arg2, (void*)arg3);
        break;
    case SC_lseek:
        format_lseek(builder, (int)arg1, (off_t)arg2, (int)arg3);
        break;
    case SC_mmap:
        TRY(format_mmap(builder, (Syscall::SC_mmap_params*)arg1));
        result_type = VoidP;
        break;
    case SC_mprotect:
        format_mprotect(builder, (void*)arg1, (size_t)arg2, (int)arg3);
        break;
    case SC_munmap:
        format_munmap(builder, (void*)arg1, (size_t)arg2);
        break;
    case SC_open:
        TRY(format_open(builder, (Syscall::SC_open_params*)arg1));
        break;
    case SC_pledge:
        TRY(format_pledge(builder, (Syscall::SC_pledge_params*)arg1));
        break;
    case SC_poll:
        TRY(format_poll(builder, (Syscall::SC_poll_params*)arg1));
        break;
    case SC_read:
        format_read(builder, (int)arg1, (void*)arg2, (size_t)arg3);
        result_type = Ssize;
        break;
    case SC_realpath:
        TRY(format_realpath(builder, (Syscall::SC_realpath_params*)arg1, (size_t)res));
        break;
    case SC_recvmsg:
        format_recvmsg(builder, (int)arg1, (struct msghdr*)arg2, (int)arg3);
        result_type = Ssize;
        break;
    case SC_set_mmap_name:
        TRY(format_set_mmap_name(builder, (Syscall::SC_set_mmap_name_params*)arg1));
        break;
    case SC_socket:
        format_socket(builder, (int)arg1, (int)arg2, (int)arg3);
        break;
    case SC_stat:
        TRY(format_stat(builder, (Syscall::SC_stat_params*)arg1));
        break;
    case SC_write:
        format_write(builder, (int)arg1, (void*)arg2, (size_t)arg3);
        result_type = Ssize;
        break;
    case SC_kill:
        format_kill(builder, (pid_t)arg1, (int)arg2);
        break;
    case SC_prctl:
        format_prctl(builder, (int)arg1, (size_t)arg2, (size_t)arg3, (size_t)arg4);
        break;
    case SC_getuid:
    case SC_geteuid:
    case SC_getgid:
    case SC_getegid:
    case SC_getpid:
    case SC_getppid:
    case SC_gettid:
        break;
    default:
        builder.add_arguments((void*)arg1, (void*)arg2, (void*)arg3, (void*)arg4);
        result_type = VoidP;
    }

    switch (result_type) {
    case Int:
        builder.format_result((int)res);
        break;
    case Ssize:
        builder.format_result((ssize_t)res);
        break;
    case VoidP:
        builder.format_result((void*)res);
        break;
    case Void:
        builder.format_result();
        break;
    }
    return {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath proc exec ptrace sigaction"));

    Vector<StringView> child_argv;

    StringView output_filename;
    StringView exclude_syscalls_option;
    StringView include_syscalls_option;
    HashTable<StringView> exclude_syscalls;
    HashTable<StringView> include_syscalls;

    Core::ArgsParser parser;
    parser.set_stop_on_first_non_option(true);
    parser.set_general_help(
        "Trace all syscalls and their result.");
    parser.add_option(g_pid, "Trace the given PID", "pid", 'p', "pid");
    parser.add_option(output_filename, "Filename to write output to", "output", 'o', "output");
    parser.add_option(exclude_syscalls_option, "Comma-delimited syscalls to exclude", "exclude", 'e', "exclude");
    parser.add_option(include_syscalls_option, "Comma-delimited syscalls to include", "include", 'i', "include");
    parser.add_positional_argument(child_argv, "Arguments to exec", "argument", Core::ArgsParser::Required::No);

    parser.parse(arguments);

    auto trace_file = output_filename.is_empty()
        ? TRY(Core::File::standard_error())
        : TRY(Core::File::open(output_filename, Core::File::OpenMode::Write));

    auto parse_syscalls = [](StringView option, auto& hash_table) {
        if (!option.is_empty()) {
            for (auto syscall : option.split_view(','))
                hash_table.set(syscall);
        }
    };
    parse_syscalls(exclude_syscalls_option, exclude_syscalls);
    parse_syscalls(include_syscalls_option, include_syscalls);

    TRY(Core::System::pledge("stdio rpath proc exec ptrace sigaction"));

    int status;
    if (g_pid == -1) {
        if (child_argv.is_empty())
            return Error::from_string_literal("Expected either a pid or some arguments");

        auto pid = TRY(Core::System::fork());

        if (!pid) {
            TRY(Core::System::ptrace(PT_TRACE_ME, 0, 0, 0));
            TRY(Core::System::exec(child_argv.first(), child_argv, Core::System::SearchInPath::Yes));
            VERIFY_NOT_REACHED();
        }

        g_pid = pid;
        if (waitpid(pid, &status, WSTOPPED | WEXITED) != pid || !WIFSTOPPED(status)) {
            perror("waitpid");
            return 1;
        }
    }

    struct sigaction sa = {};
    sa.sa_handler = handle_sigint;
    TRY(Core::System::sigaction(SIGINT, &sa, nullptr));

    TRY(Core::System::ptrace(PT_ATTACH, g_pid, 0, 0));
    if (waitpid(g_pid, &status, WSTOPPED | WEXITED) != g_pid || !WIFSTOPPED(status)) {
        perror("waitpid");
        return 1;
    }

    for (;;) {
        TRY(Core::System::ptrace(PT_SYSCALL, g_pid, 0, 0));

        if (waitpid(g_pid, &status, WSTOPPED | WEXITED) != g_pid || !WIFSTOPPED(status)) {
            perror("wait_pid");
            return 1;
        }
        PtraceRegisters regs = {};
        TRY(Core::System::ptrace(PT_GETREGS, g_pid, &regs, 0));
#if ARCH(X86_64)
        syscall_arg_t syscall_index = regs.rax;
        syscall_arg_t arg1 = regs.rdx;
        syscall_arg_t arg2 = regs.rdi;
        syscall_arg_t arg3 = regs.rbx;
        syscall_arg_t arg4 = regs.rsi;
#elif ARCH(AARCH64)
        syscall_arg_t syscall_index = 0; // FIXME
        syscall_arg_t arg1 = 0;          // FIXME
        syscall_arg_t arg2 = 0;          // FIXME
        syscall_arg_t arg3 = 0;          // FIXME
        syscall_arg_t arg4 = 0;          // FIXME
        TODO_AARCH64();
#elif ARCH(RISCV64)
        syscall_arg_t syscall_index = regs.x[16];
        syscall_arg_t arg1 = regs.x[9];
        syscall_arg_t arg2 = regs.x[10];
        syscall_arg_t arg3 = regs.x[11];
        syscall_arg_t arg4 = regs.x[12];
#else
#    error Unknown architecture
#endif

        TRY(Core::System::ptrace(PT_SYSCALL, g_pid, 0, 0));
        if (waitpid(g_pid, &status, WSTOPPED | WEXITED) != g_pid || !WIFSTOPPED(status)) {
            perror("wait_pid");
            return 1;
        }

        TRY(Core::System::ptrace(PT_GETREGS, g_pid, &regs, 0));

#if ARCH(X86_64)
        u64 res = regs.rax;
#elif ARCH(AARCH64)
        u64 res = 0; // FIXME
        TODO_AARCH64();
#elif ARCH(RISCV64)
        u64 res = regs.x[9];
#else
#    error Unknown architecture
#endif

        auto syscall_function = (Syscall::Function)syscall_index;
        auto syscall_name = to_string(syscall_function);
        if (exclude_syscalls.contains(syscall_name))
            continue;
        if (!include_syscalls.is_empty() && !include_syscalls.contains(syscall_name))
            continue;

        FormattedSyscallBuilder builder(syscall_name);
        TRY(format_syscall(builder, syscall_function, arg1, arg2, arg3, arg4, res));

        TRY(trace_file->write_until_depleted(builder.string_view().bytes()));
    }
}
