/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Model.h>
#include <sys/arch/regs.h>

namespace HackStudio {

struct RegisterData {
    ByteString name;
    FlatPtr value;
    bool changed { false };
};

class RegistersModel final : public GUI::Model {
public:
    static RefPtr<RegistersModel> create(PtraceRegisters const& regs)
    {
        return adopt_ref(*new RegistersModel(regs));
    }

    static RefPtr<RegistersModel> create(PtraceRegisters const& current_regs, PtraceRegisters const& previous_regs)
    {
        return adopt_ref(*new RegistersModel(current_regs, previous_regs));
    }

    enum Column {
        Register,
        Value,
        __Count
    };

    virtual ~RegistersModel() override = default;

    virtual int row_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override;
    virtual int column_count(const GUI::ModelIndex& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;

    PtraceRegisters const& raw_registers() const { return m_raw_registers; }

private:
    explicit RegistersModel(PtraceRegisters const& regs);
    RegistersModel(PtraceRegisters const& current_regs, PtraceRegisters const& previous_regs);

    PtraceRegisters m_raw_registers;
    Vector<RegisterData> m_registers;
};

}
