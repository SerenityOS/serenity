/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/DeprecatedString.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>

namespace JS::Bytecode {

struct UnwindInfo {
    Executable const* executable;
    BasicBlock const* handler;
    BasicBlock const* finalizer;

    JS::GCPtr<Environment> lexical_environment;

    bool handler_called { false };
};

class BasicBlock {
    AK_MAKE_NONCOPYABLE(BasicBlock);

public:
    static NonnullOwnPtr<BasicBlock> create(DeprecatedString name);
    ~BasicBlock();

    void dump(Executable const&) const;
    ReadonlyBytes instruction_stream() const { return m_buffer.span(); }
    u8* data() { return m_buffer.data(); }
    u8 const* data() const { return m_buffer.data(); }
    size_t size() const { return m_buffer.size(); }

    void grow(size_t additional_size);

    void terminate(Badge<Generator>) { m_terminated = true; }
    bool is_terminated() const { return m_terminated; }

    DeprecatedString const& name() const { return m_name; }

private:
    explicit BasicBlock(DeprecatedString name);

    Vector<u8> m_buffer;
    DeprecatedString m_name;
    bool m_terminated { false };
};

}
