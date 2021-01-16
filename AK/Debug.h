/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#pragma once

// FIXME: We could generate this file with CMake and configure.

#ifdef PROCESS_DEBUG
constexpr bool debug_process = true;
#else
constexpr bool debug_process = false;
#endif

#ifdef SCHEDULER_DEBUG
constexpr bool debug_scheduler = true;
#else
constexpr bool debug_scheduler = false;
#endif

#ifdef SCHEDULER_RUNNABLE_DEBUG
constexpr bool debug_scheduler_runnable = true;
#else
constexpr bool debug_scheduler_runnable = false;
#endif

#ifdef THREAD_DEBUG
constexpr bool debug_thread = true;
#else
constexpr bool debug_thread = false;
#endif

#ifdef LOCK_DEBUG
constexpr bool debug_lock = true;
#else
constexpr bool debug_lock = false;
#endif

#ifdef SIGNAL_DEBUG
constexpr bool debug_signal = true;
#else
constexpr bool debug_signal = false;
#endif

#ifdef BMP_DEBUG
constexpr bool debug_bmp = true;
#else
constexpr bool debug_bmp = false;
#endif

#ifdef WAITBLOCK_DEBUG
constexpr bool debug_waitblock = true;
#else
constexpr bool debug_waitblock = false;
#endif

#ifdef WAITQUEUE_DEBUG
constexpr bool debug_waitqueue = true;
#else
constexpr bool debug_waitqueue = false;
#endif

#ifdef MULTIPROCESSOR_DEBUG
constexpr bool debug_multiprocessor = true;
#else
constexpr bool debug_multiprocessor = false;
#endif

#ifdef ACPI_DEBUG
constexpr bool debug_acpi = true;
#else
constexpr bool debug_acpi = false;
#endif

#ifdef PAGE_FAULT_DEBUG
constexpr bool debug_page_fault = true;
#else
constexpr bool debug_page_fault = false;
#endif

#ifdef CONTEXT_SWITCH_DEBUG
constexpr bool debug_context_switch = true;
#else
constexpr bool debug_context_switch = false;
#endif

#ifdef SMP_DEBUG
constexpr bool debug_smp = true;
#else
constexpr bool debug_smp = false;
#endif

#ifdef BXVGA_DEBUG
constexpr bool debug_bxvga = true;
#else
constexpr bool debug_bxvga = false;
#endif

#ifdef PS2MOUSE_DEBUG
constexpr bool debug_ps2mouse = true;
#else
constexpr bool debug_ps2mouse = false;
#endif

#ifdef VMWAREBACKDOOR_DEBUG
constexpr bool debug_vmware_backdoor = true;
#else
constexpr bool debug_vmware_backdoor = false;
#endif

#ifdef FILEDESCRIPTION_DEBUG
constexpr bool debug_file_description = true;
#else
constexpr bool debug_file_description = false;
#endif

#ifdef PROCFS_DEBUG
constexpr bool debug_procfs = true;
#else
constexpr bool debug_procfs = false;
#endif

#ifdef VFS_DEBUG
constexpr bool debug_vfs = true;
#else
constexpr bool debug_vfs = false;
#endif

#ifdef IOAPIC_DEBUG
constexpr bool debug_ioapic = true;
#else
constexpr bool debug_ioapic = false;
#endif

#ifdef IRQ_DEBUG
constexpr bool debug_irq = true;
#else
constexpr bool debug_irq = false;
#endif

#ifdef INTERRUPT_DEBUG
constexpr bool debug_interrupt = true;
#else
constexpr bool debug_interrupt = false;
#endif

#ifdef E1000_DEBUG
constexpr bool debug_e1000 = true;
#else
constexpr bool debug_e1000 = false;
#endif

#ifdef IPV4_SOCKET_DEBUG
constexpr bool debug_ipv4_socket = true;
#else
constexpr bool debug_ipv4_socket = false;
#endif

#ifdef DEBUG_LOCAL_SOCKET
constexpr bool debug_local_socket = true;
#else
constexpr bool debug_local_socket = false;
#endif

#ifdef DEBUG_SOCKET
constexpr bool debug_socket = true;
#else
constexpr bool debug_socket = false;
#endif

#ifdef TCP_SOCKET_DEBUG
constexpr bool debug_tcp_socket = true;
#else
constexpr bool debug_tcp_socket = false;
#endif

#ifdef PCI_DEBUG
constexpr bool debug_pci = true;
#else
constexpr bool debug_pci = false;
#endif

#ifdef PATA_DEBUG
constexpr bool debug_pata = true;
#else
constexpr bool debug_pata = false;
#endif

#ifdef DEBUG_IO
constexpr bool debug_io = true;
#else
constexpr bool debug_io = false;
#endif

#ifdef FORK_DEBUG
constexpr bool debug_fork = true;
#else
constexpr bool debug_fork = false;
#endif

#ifdef DEBUG_POLL_SELECT
constexpr bool debug_poll_select = true;
#else
constexpr bool debug_poll_select = false;
#endif

#ifdef HPET_DEBUG
constexpr bool debug_hpet = true;
#else
constexpr bool debug_hpet = false;
#endif

#ifdef HPET_COMPARATOR_DEBUG
constexpr bool debug_hpet_comperator = true;
#else
constexpr bool debug_hpet_comperator = false;
#endif

#ifdef MASTERPTY_DEBUG
constexpr bool debug_masterpty = true;
#else
constexpr bool debug_masterpty = false;
#endif

#ifdef SLAVEPTY_DEBUG
constexpr bool debug_slavepty = true;
#else
constexpr bool debug_slavepty = false;
#endif

#ifdef PTMX_DEBUG
constexpr bool debug_ptmx = true;
#else
constexpr bool debug_ptmx = false;
#endif

#ifdef TTY_DEBUG
constexpr bool debug_tty = true;
#else
constexpr bool debug_tty = false;
#endif

#ifdef CONTIGUOUS_VMOBJECT_DEBUG
constexpr bool debug_contiguous_vmobject = true;
#else
constexpr bool debug_contiguous_vmobject = false;
#endif

#ifdef VRA_DEBUG
constexpr bool debug_vra = true;
#else
constexpr bool debug_vra = false;
#endif

#ifdef COPY_DEBUG
constexpr bool debug_copy = true;
#else
constexpr bool debug_copy = false;
#endif

#ifdef DEBUG_CURSOR_TOOL
constexpr bool debug_cursor_tool = true;
#else
constexpr bool debug_cursor_tool = false;
#endif

#ifdef DEBUG_FILE_CONTENT
constexpr bool debug_file_content = true;
#else
constexpr bool debug_file_content = false;
#endif

#ifdef DEBUG_GZIP
constexpr bool debug_gzip = true;
#else
constexpr bool debug_gzip = false;
#endif

#ifdef CNETWORKJOB_DEBUG
constexpr bool debug_cnetworkjob = true;
#else
constexpr bool debug_cnetworkjob = false;
#endif

#ifdef CSOCKET_DEBUG
constexpr bool debug_csocket = true;
#else
constexpr bool debug_csocket = false;
#endif

#ifdef SAFE_SYSCALL_DEBUG
constexpr bool debug_safe_syscall = true;
#else
constexpr bool debug_safe_syscall = false;
#endif

#ifdef GHASH_PROCESS_DEBUG
constexpr bool debug_ghash_process = true;
#else
constexpr bool debug_ghash_process = false;
#endif

#ifdef NT_DEBUG
constexpr bool debug_nt = true;
#else
constexpr bool debug_nt = false;
#endif

#ifdef CRYPTO_DEBUG
constexpr bool debug_crypto = true;
#else
constexpr bool debug_crypto = false;
#endif

#ifdef DWARF_DEBUG
constexpr bool debug_dwarf = true;
#else
constexpr bool debug_dwarf = false;
#endif

#ifdef DEBUG_HUNKS
constexpr bool debug_hunks = true;
#else
constexpr bool debug_hunks = false;
#endif

#ifdef JOB_DEBUG
constexpr bool debug_job = true;
#else
constexpr bool debug_job = false;
#endif

#ifdef GIF_DEBUG
constexpr bool debug_gif = true;
#else
constexpr bool debug_gif = false;
#endif

#ifdef JPG_DEBUG
constexpr bool debug_jpg = true;
#else
constexpr bool debug_jpg = false;
#endif
