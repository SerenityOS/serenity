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
#include <LibJVM/Class.h>

namespace JVM {

struct Frame {
    AK::Vector local_variables;
    AK::Vector op_stack;
    AK::NonnullRefPtr<Class> rt_const_pool;
    AK::FixedArray<u8> current_method; //Frames are created when a method is invoked and destroyed when one returns, so we can just store the code once.
    //We need this for accessing additional data in the code.
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
    void push_operand(Value op);
    void push_local_var(StackValue var);

private:
    AK::NonnullRefPtr<long> m_pc;
    AK::NonnullPtrVector<Frame> m_stack;
};

}
