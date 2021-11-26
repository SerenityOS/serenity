/*
 * Copyright (c) 2021, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Bytecode/BasicBlock.h>
#include <LibJS/Bytecode/Generator.h>
#include <sys/time.h>
#include <time.h>

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
        gettimeofday(&m_start_time, nullptr);
    }
    void finished()
    {
        struct timeval end_time {
            0, 0
        };
        gettimeofday(&end_time, nullptr);
        time_t interval_s = end_time.tv_sec - m_start_time.tv_sec;
        suseconds_t interval_us = end_time.tv_usec;
        if (interval_us < m_start_time.tv_usec) {
            interval_s -= 1;
            interval_us += 1000000;
        }
        interval_us -= m_start_time.tv_usec;
        m_time_difference = interval_s * 1000000 + interval_us;
    }

    u64 elapsed() const { return m_time_difference; }

protected:
    struct timeval m_start_time {
        0, 0
    };
    u64 m_time_difference { 0 };
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
            pass.perform(executable);
        finished();
    }

private:
    NonnullOwnPtrVector<Pass> m_passes;
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
