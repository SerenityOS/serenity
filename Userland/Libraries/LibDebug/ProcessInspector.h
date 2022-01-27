/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LoadedLibrary.h"
#include <AK/Types.h>
#include <LibC/sys/arch/i386/regs.h>

namespace Debug {

class ProcessInspector {
public:
    virtual ~ProcessInspector() { }
    virtual bool poke(FlatPtr address, FlatPtr data) = 0;
    virtual Optional<FlatPtr> peek(FlatPtr address) const = 0;
    virtual PtraceRegisters get_registers() const = 0;
    virtual void set_registers(PtraceRegisters const&) = 0;
    virtual void for_each_loaded_library(Function<IterationDecision(LoadedLibrary const&)>) const = 0;

    const LoadedLibrary* library_at(FlatPtr address) const;
    struct SymbolicationResult {
        String library_name;
        String symbol;
    };
    Optional<SymbolicationResult> symbolicate(FlatPtr address) const;
    Optional<DebugInfo::SourcePositionAndAddress> get_address_from_source_position(String const& file, size_t line) const;
    Optional<DebugInfo::SourcePosition> get_source_position(FlatPtr address) const;

protected:
    ProcessInspector() = default;
};

};
