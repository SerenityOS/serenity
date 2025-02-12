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
#elif ARCH(RISCV64)
    m_registers.append({ "ra", regs.x[0] });
    m_registers.append({ "sp", regs.x[1] });
    m_registers.append({ "gp", regs.x[2] });
    m_registers.append({ "tp", regs.x[3] });
    m_registers.append({ "fp", regs.x[7] });

    m_registers.append({ "a0", regs.x[9] });
    m_registers.append({ "a1", regs.x[10] });
    m_registers.append({ "a2", regs.x[11] });
    m_registers.append({ "a3", regs.x[12] });
    m_registers.append({ "a4", regs.x[13] });
    m_registers.append({ "a5", regs.x[14] });
    m_registers.append({ "a6", regs.x[15] });
    m_registers.append({ "a7", regs.x[16] });

    m_registers.append({ "t0", regs.x[4] });
    m_registers.append({ "t1", regs.x[5] });
    m_registers.append({ "t2", regs.x[6] });
    m_registers.append({ "t3", regs.x[27] });
    m_registers.append({ "t4", regs.x[28] });
    m_registers.append({ "t5", regs.x[29] });
    m_registers.append({ "t6", regs.x[30] });

    m_registers.append({ "s1", regs.x[8] });
    m_registers.append({ "s2", regs.x[17] });
    m_registers.append({ "s3", regs.x[18] });
    m_registers.append({ "s4", regs.x[19] });
    m_registers.append({ "s5", regs.x[20] });
    m_registers.append({ "s6", regs.x[21] });
    m_registers.append({ "s7", regs.x[22] });
    m_registers.append({ "s8", regs.x[23] });
    m_registers.append({ "s9", regs.x[24] });
    m_registers.append({ "s10", regs.x[25] });
    m_registers.append({ "s11", regs.x[26] });
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
#elif ARCH(RISCV64)
    m_registers.append({ "ra", current_regs.x[0], current_regs.x[0] != previous_regs.x[0] });
    m_registers.append({ "sp", current_regs.x[1], current_regs.x[1] != previous_regs.x[1] });
    m_registers.append({ "gp", current_regs.x[2], current_regs.x[2] != previous_regs.x[2] });
    m_registers.append({ "tp", current_regs.x[3], current_regs.x[3] != previous_regs.x[3] });
    m_registers.append({ "fp", current_regs.x[7], current_regs.x[7] != previous_regs.x[7] });

    m_registers.append({ "a0", current_regs.x[9], current_regs.x[9] != previous_regs.x[9] });
    m_registers.append({ "a1", current_regs.x[10], current_regs.x[10] != previous_regs.x[10] });
    m_registers.append({ "a2", current_regs.x[11], current_regs.x[11] != previous_regs.x[11] });
    m_registers.append({ "a3", current_regs.x[12], current_regs.x[12] != previous_regs.x[12] });
    m_registers.append({ "a4", current_regs.x[13], current_regs.x[13] != previous_regs.x[13] });
    m_registers.append({ "a5", current_regs.x[14], current_regs.x[14] != previous_regs.x[14] });
    m_registers.append({ "a6", current_regs.x[15], current_regs.x[15] != previous_regs.x[15] });
    m_registers.append({ "a7", current_regs.x[16], current_regs.x[16] != previous_regs.x[16] });

    m_registers.append({ "t0", current_regs.x[4], current_regs.x[4] != previous_regs.x[4] });
    m_registers.append({ "t1", current_regs.x[5], current_regs.x[5] != previous_regs.x[5] });
    m_registers.append({ "t2", current_regs.x[6], current_regs.x[6] != previous_regs.x[6] });
    m_registers.append({ "t3", current_regs.x[27], current_regs.x[27] != previous_regs.x[27] });
    m_registers.append({ "t4", current_regs.x[28], current_regs.x[28] != previous_regs.x[28] });
    m_registers.append({ "t5", current_regs.x[29], current_regs.x[29] != previous_regs.x[29] });
    m_registers.append({ "t6", current_regs.x[30], current_regs.x[30] != previous_regs.x[30] });

    m_registers.append({ "s1", current_regs.x[8], current_regs.x[8] != previous_regs.x[8] });
    m_registers.append({ "s2", current_regs.x[17], current_regs.x[17] != previous_regs.x[17] });
    m_registers.append({ "s3", current_regs.x[18], current_regs.x[18] != previous_regs.x[18] });
    m_registers.append({ "s4", current_regs.x[19], current_regs.x[19] != previous_regs.x[19] });
    m_registers.append({ "s5", current_regs.x[20], current_regs.x[20] != previous_regs.x[20] });
    m_registers.append({ "s6", current_regs.x[21], current_regs.x[21] != previous_regs.x[21] });
    m_registers.append({ "s7", current_regs.x[22], current_regs.x[22] != previous_regs.x[22] });
    m_registers.append({ "s8", current_regs.x[23], current_regs.x[23] != previous_regs.x[23] });
    m_registers.append({ "s9", current_regs.x[24], current_regs.x[24] != previous_regs.x[24] });
    m_registers.append({ "s10", current_regs.x[25], current_regs.x[25] != previous_regs.x[25] });
    m_registers.append({ "s11", current_regs.x[26], current_regs.x[26] != previous_regs.x[26] });
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
        return "Value"_string;
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
            return ByteString::formatted("{:p}", reg.value);
        return {};
    }
    return {};
}

}
