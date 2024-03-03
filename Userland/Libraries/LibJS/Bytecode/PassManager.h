/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/ElapsedTimer.h>
#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>

namespace JS::Bytecode {

struct PassPipelineExecutable {
    Executable& executable;
    Optional<HashMap<BasicBlock const*, HashTable<BasicBlock const*>>> cfg {};
    Optional<HashMap<BasicBlock const*, HashTable<BasicBlock const*>>> inverted_cfg {};
    Optional<HashTable<BasicBlock const*>> exported_blocks {};
};

class Pass {
public:
    Pass() = default;
    virtual ~Pass() = default;

    virtual void perform(PassPipelineExecutable&) = 0;
    void started()
    {
        m_timer.start();
    }
    void finished()
    {
        m_time_difference = m_timer.elapsed_time();
    }

    u64 elapsed() const { return m_time_difference.to_microseconds(); }

protected:
    Core::ElapsedTimer m_timer;
    Duration m_time_difference {};
};

class PassManager : public Pass {
public:
    PassManager() = default;
    ~PassManager() override = default;

    void add(NonnullOwnPtr<Pass> pass) { m_passes.append(move(pass)); }

    template<typename PassT, typename... Args>
    void add(Args&&... args) { m_passes.append(make<PassT>(forward<Args>(args)...)); }

    void perform(Executable& executable)
    {
        PassPipelineExecutable pipeline_executable { executable };
        perform(pipeline_executable);
    }

    virtual void perform(PassPipelineExecutable& executable) override
    {
        started();
        for (auto& pass : m_passes)
            pass->perform(executable);
        finished();
    }

private:
    Vector<NonnullOwnPtr<Pass>> m_passes;
};

namespace Passes {

class GenerateCFG : public Pass {
public:
    GenerateCFG() = default;
    ~GenerateCFG() override = default;

private:
    virtual void perform(PassPipelineExecutable&) override;
};

class MergeBlocks : public Pass {
public:
    MergeBlocks() = default;
    ~MergeBlocks() override = default;

private:
    virtual void perform(PassPipelineExecutable&) override;
};

class PlaceBlocks : public Pass {
public:
    PlaceBlocks() = default;
    ~PlaceBlocks() override = default;

private:
    virtual void perform(PassPipelineExecutable&) override;
};

class UnifySameBlocks : public Pass {
public:
    UnifySameBlocks() = default;
    ~UnifySameBlocks() override = default;

private:
    virtual void perform(PassPipelineExecutable&) override;
};

class Peephole : public Pass {
public:
    Peephole() = default;
    ~Peephole() override = default;

private:
    virtual void perform(PassPipelineExecutable&) override;
};

class DumpCFG : public Pass {
public:
    DumpCFG(FILE* file)
        : m_file(file)
    {
    }

    ~DumpCFG() override = default;

private:
    virtual void perform(PassPipelineExecutable&) override;

    FILE* m_file { nullptr };
};

}

}
