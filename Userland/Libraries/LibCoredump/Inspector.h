/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Reader.h"
#include <AK/Noncopyable.h>
#include <LibDebug/ProcessInspector.h>

namespace Coredump {

class Inspector : public Debug::ProcessInspector {
    AK_MAKE_NONCOPYABLE(Inspector);
    AK_MAKE_NONMOVABLE(Inspector);

public:
    static OwnPtr<Inspector> create(StringView coredump_path, Function<void(float)> on_progress = {});
    virtual ~Inspector() override = default;

    // ^Debug::ProcessInspector
    virtual bool poke(FlatPtr address, FlatPtr data) override;
    virtual Optional<FlatPtr> peek(FlatPtr address) const override;
    virtual PtraceRegisters get_registers() const override;
    virtual void set_registers(PtraceRegisters const&) override;
    virtual void for_each_loaded_library(Function<IterationDecision(Debug::LoadedLibrary const&)>) const override;

private:
    Inspector(NonnullOwnPtr<Reader>&&, Function<void(float)> on_progress);

    void parse_loaded_libraries(Function<void(float)> on_progress);
    size_t number_of_libraries_in_coredump() const;

    NonnullOwnPtr<Reader> m_reader;

    Vector<NonnullOwnPtr<Debug::LoadedLibrary>> m_loaded_libraries;
};

}
