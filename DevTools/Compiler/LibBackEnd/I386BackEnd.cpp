/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "I386BackEnd.h"
#include <LibCpp/Option.h>
#include <LibMiddleEnd/SIR.h>

namespace BackEnd {

NonnullRefPtr<Core::File> I386BackEnd::get_output_file()
{
    auto output_file = Core::File::open(m_options.output_file, Core::IODevice::WriteOnly);
    assert(!output_file.is_error());

    return output_file.value();
}

void I386BackEnd::print_assembly_for_function(const SIR::Function& function)
{
    size_t param_stack = m_param_stack_start;

    m_output_file->printf("\t.globl %s\n", function.name().characters());
    m_output_file->printf("\t.type %s, @function\n", function.name().characters());
    m_output_file->printf("%s:\n", function.name().characters());

    // frame pointer
    m_output_file->printf("\tpushl\t%%ebp\n");
    m_output_file->printf("\tmovl\t%%esp, %%ebp\n");

    auto variables_already_seen = HashMap<String, String>();
    Optional<const SIR::Variable*> var_in_eax;

    for (auto& operation : function.body()) {
        if (operation.is_binary_expression()) {
            auto& binop = reinterpret_cast<const SIR::BinaryExpression&>(operation);
            auto right_index = variables_already_seen.get(binop.right()->result()->name());
            auto left_index = variables_already_seen.get(binop.left()->result()->name());

            assert(right_index.has_value() && left_index.has_value());
            assert(var_in_eax.has_value());
            if (var_in_eax.value() != binop.left()->result().ptr())
                m_output_file->printf("\tmovl\t%s, %%eax\n", left_index.value().characters());

            switch (binop.binary_operation()) {
            case SIR::BinaryExpression::Kind::Addition:
                m_output_file->printf("\taddl\t%s, %%eax\n", right_index.value().characters());
                break;
            case SIR::BinaryExpression::Kind::Multiplication:
                m_output_file->printf("\timull\t%s, %%eax\n", right_index.value().characters());
                break;
            case SIR::BinaryExpression::Kind::Subtraction:
                m_output_file->printf("\tsubl\t%s, %%eax\n", right_index.value().characters());
                break;
            }
            //TODO: clear other var in eax
            variables_already_seen.set(binop.result()->name(), "%eax");
            var_in_eax = binop.result();
        } else if (operation.is_return_statement()) {
            m_output_file->printf("\tpopl\t%%ebp\n");
            m_output_file->printf("\tret\n");
        } else if (operation.is_variable()) {
            auto& var = reinterpret_cast<const SIR::Variable&>(operation);
            m_output_file->printf("\tmovl\t%zu(%%ebp), %%eax\n", param_stack);
            variables_already_seen.set(var.name(), String::format("%zu(%%ebp)", param_stack));
            param_stack += var.node_type()->size_in_bytes();
            var_in_eax = &var;
        } else {
            ASSERT_NOT_REACHED();
        }
    }

    m_output_file->printf("\t.size %s, .-%s\n", function.name().characters(), function.name().characters());
}

void I386BackEnd::print_asm()
{
    //TODO: implement String.last_index_of
    auto input_file_name = m_options.input_file;
    while (input_file_name.contains("/")) {
        auto new_path = input_file_name.index_of("/");

        assert(new_path.has_value());
        input_file_name = input_file_name.substring(new_path.value() + 1);
    }
    m_output_file->printf("\t.file \"%s\"\n", input_file_name.characters());
    m_output_file->printf("\t.ident \"Serenity-c++ compiler V0.0.0\"\n");
    m_output_file->printf("\t.section \".note.GNU-stack\",\"\",@progbits\n");

    for (auto& function : m_tu.functions()) {
        print_assembly_for_function(function);
    }
}

}