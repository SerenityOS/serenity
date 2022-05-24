/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Arch/Processor.h>

#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Arch/aarch64/Processor.h>

#include <Kernel/TTY/TTY.h>
#include <Kernel/ThreadTracer.h>

extern "C" uintptr_t vector_table_el1;

namespace Kernel {

aarch64Processor* g_current_processor;

void aarch64Processor::initialize(u32 cpu)
{
    VERIFY(g_current_processor == nullptr);

    auto current_exception_level = static_cast<u64>(Aarch64::Asm::get_current_exception_level());
    dbgln("CPU{} started in: EL{}", cpu, current_exception_level);

    dbgln("Drop CPU{} to EL1", cpu);
    drop_to_exception_level_1();

    // Load EL1 vector table
    Aarch64::Asm::el1_vector_table_install(&vector_table_el1);

    g_current_processor = this;
}

[[noreturn]] void aarch64Processor::halt()
{
    for (;;)
        asm volatile("wfi");
}

}

// FIXME: move these to better files

#include <Kernel/Scheduler.h>
#include <Kernel/StdLib.h>

ErrorOr<void> copy_from_user(void*, void const*, unsigned long)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> copy_to_user(void*, void const*, size_t)
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/kstdio.h>
extern "C" {
void dbgputstr(char const*, size_t)
{
    VERIFY_NOT_REACHED();
}
}

namespace Kernel {
RecursiveSpinlock g_profiling_lock;

#include <Kernel/Process.h>
Time kgettimeofday()
{
    VERIFY_NOT_REACHED();
}
Process::~Process()
{
    VERIFY_NOT_REACHED();
}
void Process::delete_perf_events_buffer()
{
    VERIFY_NOT_REACHED();
}
RefPtr<Process> Process::from_pid(ProcessID)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> Process::require_no_promises() const
{
    VERIFY_NOT_REACHED();
}
bool Process::create_perf_events_buffer_if_needed()
{
    VERIFY_NOT_REACHED();
}
SpinlockProtected<Process::List>& Process::all_instances()
{
    VERIFY_NOT_REACHED();
}
void Process::set_tty(TTY*)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> Process::send_signal(u8, Process*)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> Process::require_promise(Pledge)
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/Time/TimeManagement.h>
TimeManagement& TimeManagement::the()
{
    VERIFY_NOT_REACHED();
}
bool TimeManagement::enable_profile_timer()
{
    VERIFY_NOT_REACHED();
}
bool TimeManagement::disable_profile_timer()
{
    VERIFY_NOT_REACHED();
}
bool TimeManagement::is_initialized()
{
    VERIFY_NOT_REACHED();
}
Time TimeManagement::monotonic_time(TimePrecision) const
{
    VERIFY_NOT_REACHED();
}
u64 TimeManagement::uptime_ms() const
{
    VERIFY_NOT_REACHED();
}

Process* Scheduler::colonel()
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/KString.h>
ErrorOr<NonnullOwnPtr<KString>> KString::try_clone() const
{
    VERIFY_NOT_REACHED();
}
ErrorOr<NonnullOwnPtr<KString>> KString::vformatted(StringView, AK::TypeErasedFormatParams&)
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/Devices/Device.h>
Device::Device(MajorNumber, MinorNumber)
{
    VERIFY_NOT_REACHED();
}
Device::~Device()
{
    VERIFY_NOT_REACHED();
}
void Device::after_inserting()
{
    VERIFY_NOT_REACHED();
}
ErrorOr<NonnullOwnPtr<KString>> Device::pseudo_path(OpenFileDescription const&) const
{
    VERIFY_NOT_REACHED();
}
void Device::will_be_destroyed()
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/Locking/Mutex.h>
void Mutex::lock(Mode, LockLocation const&)
{
    VERIFY_NOT_REACHED();
}
void Mutex::restore_exclusive_lock(u32, LockLocation const&)
{
    VERIFY_NOT_REACHED();
}
void Mutex::unlock()
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/FileSystem/File.h>
File::File()
{
    VERIFY_NOT_REACHED();
}
File::~File()
{
    VERIFY_NOT_REACHED();
}
ErrorOr<Memory::Region*> File::mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64, int, bool)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> File::attach(OpenFileDescription&)
{
    VERIFY_NOT_REACHED();
}
void File::detach(OpenFileDescription&)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<NonnullRefPtr<OpenFileDescription>> File::open(int)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> File::close()
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> File::ioctl(OpenFileDescription&, unsigned, Userspace<void*>)
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/FileSystem/FIFO.h>
ErrorOr<NonnullRefPtr<OpenFileDescription>> FIFO::open_direction_blocking(FIFO::Direction)
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/FileSystem/Inode.h>
ErrorOr<NonnullRefPtr<FIFO>> Inode::fifo()
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/UserOrKernelBuffer.h>
bool UserOrKernelBuffer::is_kernel_buffer() const
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/Devices/CharacterDevice.h>
CharacterDevice::~CharacterDevice() = default;

#include <Kernel/KBufferBuilder.h>
ErrorOr<void> KBufferBuilder::append(StringView)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> KBufferBuilder::append(char)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> KBufferBuilder::append_bytes(ReadonlyBytes)
{
    VERIFY_NOT_REACHED();
}
ErrorOr<void> KBufferBuilder::append_escaped_for_json(StringView)
{
    VERIFY_NOT_REACHED();
}

#include <Kernel/FileSystem/Custody.h>
ErrorOr<NonnullOwnPtr<KString>> Custody::try_serialize_absolute_path() const
{
    VERIFY_NOT_REACHED();
}

/*
#include <Kernel/Devices/DeviceManagement.h>
DeviceManagement& DeviceManagement::the()
{  VERIFY_NOT_REACHED(); }
Device* DeviceManagement::get_device(MajorNumber, MinorNumber)
{  VERIFY_NOT_REACHED(); }
*/
//{  VERIFY_NOT_REACHED(); }
//{  VERIFY_NOT_REACHED(); }
//{  VERIFY_NOT_REACHED(); }

}
