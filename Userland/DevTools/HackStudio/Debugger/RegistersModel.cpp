/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RegistersModel.h"

namespace HackStudio {

RegistersModel::RegistersModel(PtraceRegisters const& regs)
    : m_raw_registers(regs)
{
#if ARCH(X86_64)
    m_registers.append({ "rax", regs.rax });
    m_registers.append({ "rbx", regs.rbx });
    m_registers.append({ "rcx", regs.rcx });
    m_registers.append({ "rdx", regs.rdx });
    m_registers.append({ "rsp", regs.rsp });
    m_registers.append({ "rbp", regs.rbp });
    m_registers.append({ "rsi", regs.rsi });
    m_registers.append({ "rdi", regs.rdi });
    m_registers.append({ "rip", regs.rip });
    m_registers.append({ "r8", regs.r8 });
    m_registers.append({ "r9", regs.r9 });
    m_registers.append({ "r10", regs.r10 });
    m_registers.append({ "r11", regs.r11 });
    m_registers.append({ "r12", regs.r12 });
    m_registers.append({ "r13", regs.r13 });
    m_registers.append({ "r14", regs.r14 });
    m_registers.append({ "r15", regs.r15 });
    m_registers.append({ "rflags", regs.rflags });

    m_registers.append({ "cs", regs.cs });
    m_registers.append({ "ss", regs.ss });
    m_registers.append({ "ds", regs.ds });
    m_registers.append({ "es", regs.es });
    m_registers.append({ "fs", regs.fs });
    m_registers.append({ "gs", regs.gs });
#elif ARCH(AARCH64)
    TODO_AARCH64();
#else
#    error Unknown architecture
#endif
}

RegistersModel::RegistersModel(PtraceRegisters const& current_regs, PtraceRegisters const& previous_regs)
    : m_raw_registers(current_regs)
{
#if ARCH(X86_64)
    m_registers.append({ "rax", current_regs.rax, current_regs.rax != previous_regs.rax });
    m_registers.append({ "rbx", current_regs.rbx, current_regs.rbx != previous_regs.rbx });
    m_registers.append({ "rcx", current_regs.rcx, current_regs.rcx != previous_regs.rcx });
    m_registers.append({ "rdx", current_regs.rdx, current_regs.rdx != previous_regs.rdx });
    m_registers.append({ "rsp", current_regs.rsp, current_regs.rsp != previous_regs.rsp });
    m_registers.append({ "rbp", current_regs.rbp, current_regs.rbp != previous_regs.rbp });
    m_registers.append({ "rsi", current_regs.rsi, current_regs.rsi != previous_regs.rsi });
    m_registers.append({ "rdi", current_regs.rdi, current_regs.rdi != previous_regs.rdi });
    m_registers.append({ "rip", current_regs.rip, current_regs.rip != previous_regs.rip });
    m_registers.append({ "r8", current_regs.r8, current_regs.r8 != previous_regs.r8 });
    m_registers.append({ "r9", current_regs.r9, current_regs.r9 != previous_regs.r9 });
    m_registers.append({ "r10", current_regs.r10, current_regs.r10 != previous_regs.r10 });
    m_registers.append({ "r11", current_regs.r11, current_regs.r11 != previous_regs.r11 });
    m_registers.append({ "r12", current_regs.r12, current_regs.r12 != previous_regs.r12 });
    m_registers.append({ "r13", current_regs.r13, current_regs.r13 != previous_regs.r13 });
    m_registers.append({ "r14", current_regs.r14, current_regs.r14 != previous_regs.r14 });
    m_registers.append({ "r15", current_regs.r15, current_regs.r15 != previous_regs.r15 });
    m_registers.append({ "rflags", current_regs.rflags, current_regs.rflags != previous_regs.rflags });
    m_registers.append({ "cs", current_regs.cs, current_regs.cs != previous_regs.cs });
    m_registers.append({ "ss", current_regs.ss, current_regs.ss != previous_regs.ss });
    m_registers.append({ "ds", current_regs.ds, current_regs.ds != previous_regs.ds });
    m_registers.append({ "es", current_regs.es, current_regs.es != previous_regs.es });
    m_registers.append({ "fs", current_regs.fs, current_regs.fs != previous_regs.fs });
    m_registers.append({ "gs", current_regs.gs, current_regs.gs != previous_regs.gs });
#elif ARCH(AARCH64)
    (void)previous_regs;
    TODO_AARCH64();
#else
#    error Unknown architecture
#endif
}

int RegistersModel::row_count(const GUI::ModelIndex&) const
{
    return m_registers.size();
}

ErrorOr<String> RegistersModel::column_name(int column) const
{
    switch (column) {
    case Column::Register:
        return "Register"_string;
    case Column::Value:
        return "Value"_short_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant RegistersModel::data(const GUI::ModelIndex& index, GUI::ModelRole role) const
{
    auto& reg = m_registers[index.row()];

    if (role == GUI::ModelRole::ForegroundColor) {
        if (reg.changed)
            return Color(Color::Red);
        else
            return Color(Color::Black);
    }

    if (role == GUI::ModelRole::Display) {
        if (index.column() == Column::Register)
            return reg.name;
        if (index.column() == Column::Value)
            return DeprecatedString::formatted("{:p}", reg.value);
        return {};
    }
    return {};
}

}
