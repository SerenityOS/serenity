/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <AK/Types.h>
#include <Kernel/Arch/RegisterState.h>
#include <Kernel/Memory/VirtualAddress.h>
#include <Kernel/Security/ExecutionMode.h>

namespace Kernel {

// NOTE: These flags are x86_64 specific.
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
        : m_vaddr(vaddr)
    {
        m_type = (Type)(code & PageFaultFlags::ProtectionViolation);
        m_access = (Access)(code & PageFaultFlags::Write);
        m_execution_mode = (code & PageFaultFlags::UserMode) != 0 ? ExecutionMode::User : ExecutionMode::Kernel;
        m_is_reserved_bit_violation = (code & PageFaultFlags::ReservedBitViolation) != 0;
        m_is_instruction_fetch = (code & PageFaultFlags::InstructionFetch) != 0;
    }

    explicit PageFault(VirtualAddress vaddr)
        : m_vaddr(vaddr)
    {
    }

    void handle(RegisterState& regs);

    enum class Type {
        PageNotPresent = PageFaultFlags::NotPresent,
        ProtectionViolation = PageFaultFlags::ProtectionViolation,
        Unknown,
    };

    enum class Access {
        Read = PageFaultFlags::Read,
        Write = PageFaultFlags::Write,
    };

    VirtualAddress vaddr() const { return m_vaddr; }
    u16 code() const
    {
        u16 code = 0;
        code |= (u16)m_type;
        code |= (u16)m_access;
        code |= m_execution_mode == ExecutionMode::User ? PageFaultFlags::UserMode : 0;
        code |= m_is_reserved_bit_violation ? PageFaultFlags::ReservedBitViolation : 0;
        code |= m_is_instruction_fetch ? PageFaultFlags::InstructionFetch : 0;
        return code;
    }

    void set_type(Type type) { m_type = type; }
    Type type() const { return m_type; }

    void set_access(Access access) { m_access = access; }
    Access access() const { return m_access; }

    void set_mode(ExecutionMode execution_mode) { m_execution_mode = execution_mode; }
    ExecutionMode mode() const { return m_execution_mode; }

    void set_instruction_fetch(bool b) { m_is_instruction_fetch = b; }

    bool is_not_present() const { return m_type == Type::PageNotPresent; }
    bool is_protection_violation() const { return m_type == Type::ProtectionViolation; }
    bool is_read() const { return m_access == Access::Read; }
    bool is_write() const { return m_access == Access::Write; }
    bool is_user() const { return m_execution_mode == ExecutionMode::User; }
    bool is_kernel() const { return m_execution_mode == ExecutionMode::Kernel; }
    bool is_reserved_bit_violation() const { return m_is_reserved_bit_violation; }
    bool is_instruction_fetch() const { return m_is_instruction_fetch; }

private:
    Type m_type = Type::Unknown;
    Access m_access;
    ExecutionMode m_execution_mode;
    bool m_is_reserved_bit_violation { false };
    bool m_is_instruction_fetch { false };

    VirtualAddress m_vaddr;
};

}
