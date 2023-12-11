/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/String.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Handle.h>

namespace JS::Bytecode {

struct UnwindInfo {
    JS::GCPtr<Executable const> executable;
    JS::GCPtr<Environment> lexical_environment;

    bool handler_called { false };
};

class BasicBlock {
    AK_MAKE_NONCOPYABLE(BasicBlock);

public:
    static NonnullOwnPtr<BasicBlock> create(String name);
    ~BasicBlock();

    void dump(Executable const&) const;
    ReadonlyBytes instruction_stream() const { return m_buffer.span(); }
    u8* data() { return m_buffer.data(); }
    u8 const* data() const { return m_buffer.data(); }
    size_t size() const { return m_buffer.size(); }

    void grow(size_t additional_size);

    void terminate(Badge<Generator>) { m_terminated = true; }
    bool is_terminated() const { return m_terminated; }

    String const& name() const { return m_name; }

    void set_handler(BasicBlock const& handler) { m_handler = &handler; }
    void set_finalizer(BasicBlock const& finalizer) { m_finalizer = &finalizer; }

    BasicBlock const* handler() const { return m_handler; }
    BasicBlock const* finalizer() const { return m_finalizer; }

private:
    explicit BasicBlock(String name);

    Vector<u8> m_buffer;
    BasicBlock const* m_handler { nullptr };
    BasicBlock const* m_finalizer { nullptr };
    String m_name;
    bool m_terminated { false };
};

}
