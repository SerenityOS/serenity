/*
 * Copyright (c) 2022-2023, Jakub Berkop <jakub.berkop@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Tasks/PerformanceManager.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<OpenFileDescription>> open_readable_file_description(auto& fds, int fd);

static ErrorOr<size_t> get_path_index(auto& fds, int fd, PerformanceEventBuffer* event_buffer)
{
    auto description = TRY(open_readable_file_description(fds, fd));

    if (auto path = description->original_absolute_path(); !path.is_error()) {
        return TRY(event_buffer->register_string(move(path.value())));
    } else if (auto pseudo_path = description->pseudo_path(); !pseudo_path.is_error()) {
        return TRY(event_buffer->register_string(move(pseudo_path.value())));
    } else {
        auto invalid_path_string = TRY(KString::try_create("<INVALID_FILE_PATH>"sv)); // TODO: Performance, unecessary allocations.
        return TRY(event_buffer->register_string(move(invalid_path_string)));
    }
}

// FIXME: Following functions are very similar, and could be refactored into a more generic solution.
//        However, it's not clear how to do it in a way that would be easy to read and understand.
ErrorOr<FlatPtr> Process::sys$open(Userspace<Syscall::SC_open_params const*> user_params)
{
    Optional<MonotonicTime> start_timestamp = {};

    // We have to check whether profiling is enabled at before going into the syscall implementation
    // so that we can measure the time it took to execute the syscall.
    // This approach ensures that we don't have a race condition in case profiling was enabled during
    // the execution of the syscall.
    // If profiling is disabled at the beginning, we don't want to call TimeManagement::the().monotonic_time()
    // because of the overhead it would introduce for every syscall.
    bool const profiling_enabled_at_entry = !Thread::current()->is_profiling_suppressed();
    if (profiling_enabled_at_entry) {
        start_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    }

    auto const params = TRY(copy_typed_from_user(user_params));
    auto result = open_impl(user_params);

    if (!profiling_enabled_at_entry || Thread::current()->is_profiling_suppressed())
        return result;

    auto* event_buffer = current_perf_events_buffer();
    if (event_buffer == nullptr)
        return result;

    auto const end_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    auto const duration = end_timestamp - start_timestamp.value();

    FilesystemEvent data;
    data.type = FilesystemEventType::Open;
    data.durationNs = static_cast<u64>(duration.to_nanoseconds());

    if (result.is_error()) {
        data.result.is_error = true;
        data.result.value = result.error().code();
    } else {
        data.result.is_error = false;
        data.result.value = 0;
    }

    auto path = get_syscall_path_argument(params.path);
    if (!path.is_error()) {
        auto value = event_buffer->register_string(move(path.value()));
        data.data.open.filename_index = value.value();
    }

    data.data.open.dirfd = params.dirfd;
    data.data.open.options = params.options;
    data.data.open.mode = params.mode;

    (void)event_buffer->append(PERF_EVENT_FILESYSTEM, 0, 0, {}, Thread::current(), data);

    return result;
}

ErrorOr<FlatPtr> Process::sys$close(int fd)
{
    Optional<MonotonicTime> start_timestamp = {};

    bool const profiling_enabled_at_entry = !Thread::current()->is_profiling_suppressed();
    if (profiling_enabled_at_entry) {
        start_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    }

    auto result = close_impl(fd);
    if (Thread::current()->is_profiling_suppressed())
        return result;

    if (!profiling_enabled_at_entry || Thread::current()->is_profiling_suppressed())
        return result;

    auto* event_buffer = current_perf_events_buffer();
    if (event_buffer == nullptr)
        return result;

    auto const end_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    auto const duration = end_timestamp - start_timestamp.value();

    FilesystemEvent data;
    data.type = FilesystemEventType::Close;
    data.durationNs = static_cast<u64>(duration.to_nanoseconds());
    data.data.close.fd = fd;

    if (result.is_error()) {
        data.result.is_error = true;
        data.result.value = result.error().code();
    } else {
        data.result.is_error = false;
        data.result.value = 0;
    }

    auto maybe_path_index = get_path_index(fds(), fd, event_buffer);
    if (maybe_path_index.is_error())
        return result;

    data.data.close.filename_index = maybe_path_index.value();

    (void)event_buffer->append(PERF_EVENT_FILESYSTEM, 0, 0, {}, Thread::current(), data);

    return result;
}

ErrorOr<FlatPtr> Process::sys$readv(int fd, Userspace<const struct iovec*> iov, int iov_count)
{
    Optional<MonotonicTime> start_timestamp = {};

    bool const profiling_enabled_at_entry = !Thread::current()->is_profiling_suppressed();
    if (profiling_enabled_at_entry) {
        start_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    }

    auto result = readv_impl(fd, iov, iov_count);

    if (!profiling_enabled_at_entry || Thread::current()->is_profiling_suppressed())
        return result;

    if (Thread::current()->is_profiling_suppressed())
        return result;

    auto* event_buffer = current_perf_events_buffer();
    if (event_buffer == nullptr)
        return result;

    auto const end_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    auto const duration = end_timestamp - start_timestamp.value();

    FilesystemEvent data;
    data.type = FilesystemEventType::Readv;
    data.durationNs = static_cast<u64>(duration.to_nanoseconds());
    data.data.readv.fd = fd;

    if (result.is_error()) {
        data.result.is_error = true;
        data.result.value = result.error().code();
    } else {
        data.result.is_error = false;
        data.result.value = 0;
    }

    auto maybe_path_index = get_path_index(fds(), fd, event_buffer);
    if (maybe_path_index.is_error())
        return result;

    data.data.readv.filename_index = maybe_path_index.value();

    (void)event_buffer->append(PERF_EVENT_FILESYSTEM, 0, 0, {}, Thread::current(), data);

    return result;
}

ErrorOr<FlatPtr> Process::sys$read(int fd, Userspace<u8*> buffer, size_t size)
{
    Optional<MonotonicTime> start_timestamp = {};

    bool const profiling_enabled_at_entry = !Thread::current()->is_profiling_suppressed();
    if (profiling_enabled_at_entry) {
        start_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    }

    auto result = read_impl(fd, buffer, size);

    if (!profiling_enabled_at_entry || Thread::current()->is_profiling_suppressed())
        return result;

    auto* event_buffer = current_perf_events_buffer();
    if (event_buffer == nullptr)
        return result;

    auto const end_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    auto const duration = end_timestamp - start_timestamp.value();

    FilesystemEvent data;
    data.type = FilesystemEventType::Read;
    data.durationNs = static_cast<u64>(duration.to_nanoseconds());
    data.data.read.fd = fd;

    if (result.is_error()) {
        data.result.is_error = true;
        data.result.value = result.error().code();
    } else {
        data.result.is_error = false;
        data.result.value = 0;
    }

    auto maybe_path_index = get_path_index(fds(), fd, event_buffer);
    if (maybe_path_index.is_error())
        return result;

    data.data.read.filename_index = maybe_path_index.value();

    (void)event_buffer->append(PERF_EVENT_FILESYSTEM, 0, 0, {}, Thread::current(), data);

    return result;
}

ErrorOr<FlatPtr> Process::sys$pread(int fd, Userspace<u8*> buffer, size_t size, off_t userspace_offset)
{
    Optional<MonotonicTime> start_timestamp = {};

    bool const profiling_enabled_at_entry = !Thread::current()->is_profiling_suppressed();
    if (profiling_enabled_at_entry) {
        start_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    }

    auto result = pread_impl(fd, buffer, size, userspace_offset);

    if (!profiling_enabled_at_entry || Thread::current()->is_profiling_suppressed())
        return result;

    auto* event_buffer = current_perf_events_buffer();
    if (event_buffer == nullptr)
        return result;

    auto const end_timestamp = TimeManagement::the().monotonic_time(TimePrecision::Precise);
    auto const duration = end_timestamp - start_timestamp.value();

    FilesystemEvent data;
    data.type = FilesystemEventType::Pread;
    data.durationNs = static_cast<u64>(duration.to_nanoseconds());
    data.data.pread.fd = fd;
    data.data.pread.buffer_ptr = buffer.ptr();
    data.data.pread.size = size;
    data.data.pread.offset = userspace_offset;

    if (result.is_error()) {
        data.result.is_error = true;
        data.result.value = result.error().code();
    } else {
        data.result.is_error = false;
        data.result.value = 0;
    }

    auto maybe_path_index = get_path_index(fds(), fd, event_buffer);
    if (maybe_path_index.is_error())
        return result;

    data.data.pread.filename_index = maybe_path_index.value();

    (void)event_buffer->append(PERF_EVENT_FILESYSTEM, 0, 0, {}, Thread::current(), data);

    return result;
}

}
