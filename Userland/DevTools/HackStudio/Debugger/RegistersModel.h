/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <sys/arch/i386/regs.h>

namespace HackStudio {

struct RegisterData {
    String name;
    FlatPtr value;
    bool changed { false };
};

class RegistersModel final : public GUI::Model {
public:
    static RefPtr<RegistersModel> create(const PtraceRegisters& regs)
    {
        return adopt_ref(*new RegistersModel(regs));
    }

    static RefPtr<RegistersModel> create(const PtraceRegisters& current_regs, const PtraceRegisters& previous_regs)
    {
        return adopt_ref(*new RegistersModel(current_regs, previous_regs));
    }

    enum Column {
        Register,
        Value,
        __Count
    };

    virtual ~RegistersModel() override;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual String column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    const PtraceRegisters& raw_registers() const { return m_raw_registers; }

private:
    explicit RegistersModel(const PtraceRegisters& regs);
    RegistersModel(const PtraceRegisters& current_regs, const PtraceRegisters& previous_regs);

    PtraceRegisters m_raw_registers;
    Vector<RegisterData> m_registers;
};

}
