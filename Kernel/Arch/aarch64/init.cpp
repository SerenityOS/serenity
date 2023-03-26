/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2021, Marcin Undak <mcinek@gmail.com>
 * Copyright (c) 2021, Jesse Buhagiar <jooster669@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>

#include <Kernel/Arch/InterruptManagement.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/aarch64/ASM_wrapper.h>
#include <Kernel/Arch/aarch64/BootPPMParser.h>
#include <Kernel/Arch/aarch64/CPU.h>
#include <Kernel/Arch/aarch64/RPi/Framebuffer.h>
#include <Kernel/Arch/aarch64/RPi/Mailbox.h>
#include <Kernel/Arch/aarch64/RPi/UART.h>
#include <Kernel/Arch/aarch64/Registers.h>
#include <Kernel/Arch/aarch64/TrapFrame.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/FileSystem/VirtualFileSystem.h>
#include <Kernel/Graphics/Console/BootFramebufferConsole.h>
#include <Kernel/KSyms.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Panic.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Storage/StorageManagement.h>
#include <Kernel/TTY/VirtualConsole.h>

typedef void (*ctor_func_t)();
extern ctor_func_t start_heap_ctors[];
extern ctor_func_t end_heap_ctors[];
extern ctor_func_t start_ctors[];
extern ctor_func_t end_ctors[];

// FIXME: Share this with the Intel Prekernel.
extern uintptr_t __stack_chk_guard;
uintptr_t __stack_chk_guard;

READONLY_AFTER_INIT bool g_in_early_boot;

namespace Kernel {

static void draw_logo(u8* framebuffer_data);
static u32 query_firmware_version();

extern "C" [[noreturn]] void halt();
extern "C" [[noreturn]] void init();

ALWAYS_INLINE static Processor& bootstrap_processor()
{
    alignas(Processor) static u8 bootstrap_processor_storage[sizeof(Processor)];
    return (Processor&)bootstrap_processor_storage;
}

Atomic<Graphics::Console*> g_boot_console;

VirtualConsole* tty0;
ProcessID g_init_pid { 0 };

static void init_stage2(void*);
void init_stage2(void*)
{
    Process::register_new(Process::current());

    auto firmware_version = query_firmware_version();
    dmesgln("Firmware version: {}", firmware_version);

    VirtualFileSystem::initialize();

    StorageManagement::the().initialize(kernel_command_line().root_device(), kernel_command_line().is_force_pio(), kernel_command_line().is_nvme_polling_enabled());
    if (VirtualFileSystem::the().mount_root(StorageManagement::the().root_filesystem()).is_error()) {
        PANIC("VirtualFileSystem::mount_root failed");
    }

    // Switch out of early boot mode.
    g_in_early_boot = false;

    LockRefPtr<Thread> thread;
    auto userspace_init = kernel_command_line().userspace_init();
    auto init_args = kernel_command_line().userspace_init_args();

    auto init_or_error = Process::try_create_user_process(thread, userspace_init, UserID(0), GroupID(0), move(init_args), {}, tty0);
    if (init_or_error.is_error())
        PANIC("init_stage2: Error spawning init process: {}", init_or_error.error());

    g_init_pid = init_or_error.value()->pid();

    thread->set_priority(THREAD_PRIORITY_HIGH);

    Process::current().sys$exit(0);
    VERIFY_NOT_REACHED();
}

extern "C" [[noreturn]] void init()
{
    g_in_early_boot = true;

    // FIXME: Don't hardcode this
    multiboot_memory_map_t mmap[] = {
        { sizeof(struct multiboot_mmap_entry) - sizeof(u32),
            (u64)0x0,
            (u64)0x3F000000,
            MULTIBOOT_MEMORY_AVAILABLE }
    };

    multiboot_memory_map = mmap;
    multiboot_memory_map_count = 1;

    dbgln("Welcome to Serenity OS!");
    dbgln("Imagine this being your ideal operating system.");
    dbgln("Observed deviations from that ideal are shortcomings of your imagination.");
    dbgln();

    CommandLine::early_initialize("");

    new (&bootstrap_processor()) Processor();
    bootstrap_processor().early_initialize(0);

    // We call the constructors of kmalloc.cpp separately, because other constructors in the Kernel
    // might rely on being able to call new/kmalloc in the constructor. We do have to run the
    // kmalloc constructors, because kmalloc_init relies on that.
    for (ctor_func_t* ctor = start_heap_ctors; ctor < end_heap_ctors; ctor++)
        (*ctor)();
    kmalloc_init();

    bootstrap_processor().initialize(0);

    load_kernel_symbol_table();

    CommandLine::initialize();

    dmesgln("Starting SerenityOS...");

    Memory::MemoryManager::initialize(0);
    DeviceManagement::initialize();
    SysFSComponentRegistry::initialize();
    DeviceManagement::the().attach_null_device(*NullDevice::must_initialize());

    // Invoke all static global constructors in the kernel.
    // Note that we want to do this as early as possible.
    for (ctor_func_t* ctor = start_ctors; ctor < end_ctors; ctor++)
        (*ctor)();

    auto& framebuffer = RPi::Framebuffer::the();
    if (framebuffer.initialized()) {
        g_boot_console = &try_make_lock_ref_counted<Graphics::BootFramebufferConsole>(PhysicalAddress((PhysicalPtr)framebuffer.gpu_buffer()), framebuffer.width(), framebuffer.height(), framebuffer.pitch()).value().leak_ref();
        draw_logo(static_cast<Graphics::BootFramebufferConsole*>(g_boot_console.load())->unsafe_framebuffer_data());
    }

    initialize_interrupts();
    InterruptManagement::initialize();
    Processor::enable_interrupts();

    // Note: We have to disable interrupts otherwise Scheduler::timer_tick might be called before the scheduler is started.
    Processor::disable_interrupts();
    TimeManagement::initialize(0);

    Process::initialize();
    Scheduler::initialize();

    {
        LockRefPtr<Thread> init_stage2_thread;
        (void)Process::create_kernel_process(init_stage2_thread, KString::must_create("init_stage2"sv), init_stage2, nullptr, THREAD_AFFINITY_DEFAULT, Process::RegisterProcess::No);
        // We need to make sure we drop the reference for init_stage2_thread
        // before calling into Scheduler::start, otherwise we will have a
        // dangling Thread that never gets cleaned up
    }

    Scheduler::start();

    VERIFY_NOT_REACHED();
}

class QueryFirmwareVersionMboxMessage : RPi::Mailbox::Message {
public:
    u32 version;

    QueryFirmwareVersionMboxMessage()
        : RPi::Mailbox::Message(0x0000'0001, 4)
    {
        version = 0;
    }
};

static u32 query_firmware_version()
{
    struct __attribute__((aligned(16))) {
        RPi::Mailbox::MessageHeader header;
        QueryFirmwareVersionMboxMessage query_firmware_version;
        RPi::Mailbox::MessageTail tail;
    } message_queue;

    if (!RPi::Mailbox::the().send_queue(&message_queue, sizeof(message_queue))) {
        return 0xffff'ffff;
    }

    return message_queue.query_firmware_version.version;
}

extern "C" const u32 serenity_boot_logo_start;
extern "C" const u32 serenity_boot_logo_size;

static void draw_logo(u8* framebuffer_data)
{
    BootPPMParser logo_parser(reinterpret_cast<u8 const*>(&serenity_boot_logo_start), serenity_boot_logo_size);
    if (!logo_parser.parse()) {
        dbgln("Failed to parse boot logo.");
        return;
    }

    dbgln("Boot logo size: {} ({} x {})", serenity_boot_logo_size, logo_parser.image.width, logo_parser.image.height);

    auto& framebuffer = RPi::Framebuffer::the();
    auto fb_ptr = framebuffer_data;
    auto image_left = (framebuffer.width() - logo_parser.image.width) / 2;
    auto image_right = image_left + logo_parser.image.width;
    auto image_top = (framebuffer.height() - logo_parser.image.height) / 2;
    auto image_bottom = image_top + logo_parser.image.height;
    auto logo_pixels = logo_parser.image.pixel_data;

    for (u32 y = 0; y < framebuffer.height(); y++) {
        for (u32 x = 0; x < framebuffer.width(); x++) {
            if (x >= image_left && x < image_right && y >= image_top && y < image_bottom) {
                switch (framebuffer.pixel_order()) {
                case RPi::Framebuffer::PixelOrder::RGB:
                    fb_ptr[0] = logo_pixels[0];
                    fb_ptr[1] = logo_pixels[1];
                    fb_ptr[2] = logo_pixels[2];
                    break;
                case RPi::Framebuffer::PixelOrder::BGR:
                    fb_ptr[0] = logo_pixels[2];
                    fb_ptr[1] = logo_pixels[1];
                    fb_ptr[2] = logo_pixels[0];
                    break;
                default:
                    dbgln("Unsupported pixel format");
                    VERIFY_NOT_REACHED();
                }

                logo_pixels += 3;
            } else {
                fb_ptr[0] = 0xBD;
                fb_ptr[1] = 0xBD;
                fb_ptr[2] = 0xBD;
            }

            fb_ptr[3] = 0xFF;
            fb_ptr += 4;
        }
        fb_ptr += framebuffer.pitch() - framebuffer.width() * 4;
    }
}

}
