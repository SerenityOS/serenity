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
    size_t allocated_stack = m_stack_start;

    m_output_file->printf("\t.globl %s\n", function.name().characters());
    m_output_file->printf("\t.type %s, @function\n", function.name().characters());
    m_output_file->printf("%s:\n", function.name().characters());

    // frame pointer
    m_output_file->printf("\tpushl\t%%ebp\n");
    m_output_file->printf("\tmovl\t%%esp, %%ebp\n");

    auto variables_already_seen = HashMap<String, size_t>();

    for (auto& operation : function.body()) {
        if (operation.is_binary_expression()) {
            auto& binop = reinterpret_cast<const SIR::BinaryExpression&>(operation);

            switch (binop.binary_operation()) {
            case SIR::BinaryExpression::Kind::Addition:
                m_output_file->printf("\taddl\t12(%%ebp), %%eax\n");
                break;
            case SIR::BinaryExpression::Kind::Multiplication:
                m_output_file->printf("\timull\t12(%%ebp), %%eax\n");
                break;
            case SIR::BinaryExpression::Kind::Subtraction:
                m_output_file->printf("\tsubl\t12(%%ebp), %%eax\n");
                break;
            }
        } else if (operation.is_statement()) {
            m_output_file->printf("\tpopl\t%%ebp\n");
            m_output_file->printf("\tret\n");
        } else if (operation.is_variable()) {
            auto& variable = reinterpret_cast<const SIR::Variable&>(operation);
            auto variable_already_seen = variables_already_seen.get(variable.name());

            if (!variable.name().is_empty() && variable_already_seen.has_value()) {
                m_output_file->printf("\tmovl\t%zu(%%ebp), %%eax\n", variable_already_seen.value());
            } else {
                m_output_file->printf("\tmovl\t%zu(%%ebp), %%eax\n", allocated_stack);
                variables_already_seen.set(variable.name(), allocated_stack);

                allocated_stack += variable.node_type()->size_in_bytes();
            }
        } else {
            ASSERT_NOT_REACHED();
        }
    }
    m_output_file->printf("\tpopl\t%%ebp\n");
    m_output_file->printf("\tret\n");

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