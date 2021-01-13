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
