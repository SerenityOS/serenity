/*
 * Copyright (c) 2022, May Neelon <mayflower@gmail.com>
 *
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once
#include <AK/NonnullPtrVector.h>
#include <AK/Vector.h>
#include <AK/RefPtr.h>
#include <LibJVM/Forward.h>
#include <LibJVM/Value.h>

namespace JVM {

struct Frame {
    AK::Vector<StackValue> local_variables;
    AK::Vector<StackValue> op_stack;
    AK::NonnullRefPtr<ConstantPool> rt_const_pool;
};

class Thread {

public:
    Frame current_frame();
    Frame pop_frame();
    void push_frame(Frame frame);
    void remove_frame();
    void replace_frame(Frame frame);
    long pc();
    void set_pc(long pc);
    void inc_pc(long pc);

private:
    AK::NonnullRefPtr<long> m_pc;
    AK::NonnullPtrVector<Frame> m_stack;
};

}
