/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/VirtualAddress.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

namespace Kernel {

struct PageFaultFlags {
    enum Flags {
        NotPresent = 0x00,
        ProtectionViolation = 0x01,
        Read = 0x00,
        Write = 0x02,
        UserMode = 0x04,
        SupervisorMode = 0x00,
        ReservedBitViolation = 0x08,
        InstructionFetch = 0x10,
    };
};

class PageFault {
public:
    PageFault(u16 code, VirtualAddress vaddr)
        : m_code(code)
        , m_vaddr(vaddr)
    {
    }

    enum class Type {
        PageNotPresent = PageFaultFlags::NotPresent,
        ProtectionViolation = PageFaultFlags::ProtectionViolation,
    };

    enum class Access {
        Read = PageFaultFlags::Read,
        Write = PageFaultFlags::Write,
    };

    VirtualAddress vaddr() const { return m_vaddr; }
    u16 code() const { return m_code; }

    Type type() const { return (Type)(m_code & 1); }
    Access access() const { return (Access)(m_code & 2); }

    bool is_not_present() const { return (m_code & 1) == PageFaultFlags::NotPresent; }
    bool is_protection_violation() const { return (m_code & 1) == PageFaultFlags::ProtectionViolation; }
    bool is_read() const { return (m_code & 2) == PageFaultFlags::Read; }
    bool is_write() const { return (m_code & 2) == PageFaultFlags::Write; }
    bool is_user() const { return (m_code & 4) == PageFaultFlags::UserMode; }
    bool is_supervisor() const { return (m_code & 4) == PageFaultFlags::SupervisorMode; }
    bool is_instruction_fetch() const { return (m_code & 16) == PageFaultFlags::InstructionFetch; }

private:
    u16 m_code;
    VirtualAddress m_vaddr;
};

}
