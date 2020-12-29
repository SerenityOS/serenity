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

#include "I386Assembly.h"
#include <AK/SourceGenerator.h>
#include <LibCpp/LibIntermediate/SIR.h>
#include <LibCpp/Option.h>

namespace BackEnd {

NonnullRefPtr<Core::File> I386Assembly::get_output_file()
{
    auto output_file = Core::File::open(m_options.output_file, Core::IODevice::WriteOnly);
    assert(!output_file.is_error());

    return output_file.value();
}

void I386Assembly::print_assembly_for_function(const SIR::Function& function)
{
    size_t param_stack = m_param_stack_start;
    StringBuilder builder;
    auto generator = SourceGenerator(builder, '{', '}');
    generator.set("function.name", function.name());

    generator.append("\t.globl {function.name}\n");
    generator.append("\t.type {function.name}, @function\n");
    generator.append("{function.name}:\n");
    generator.append("\tpushl\t%ebp\n");
    generator.append("\tmovl\t%esp, %ebp\n");

    auto variables_already_seen = HashMap<String, String>();
    Optional<const SIR::Variable*> var_in_eax;

    for (auto& operation : function.body()) {
        if (operation.is_binary_expression()) {
            auto& binop = reinterpret_cast<const SIR::BinaryExpression&>(operation);
            auto right_index = variables_already_seen.get(binop.right()->result()->name());
            auto left_index = variables_already_seen.get(binop.left()->result()->name());

            assert(right_index.has_value() && left_index.has_value());
            assert(var_in_eax.has_value());
            generator.set("right_operand.index", right_index.value());
            generator.set("left_operand.index", left_index.value());

            if (var_in_eax.value() != binop.left()->result().ptr())
                generator.append("\tmovl\t{left_operand.index}, %eax\n");

            switch (binop.binary_operation()) {
            case SIR::BinaryExpression::Kind::Addition:
                generator.append("\taddl\t{right_operand.index}, %eax\n");
                break;
            case SIR::BinaryExpression::Kind::Multiplication:
                generator.append("\timull\t{right_operand.index}, %eax\n");
                break;
            case SIR::BinaryExpression::Kind::Subtraction:
                generator.append("\tsubl\t{right_operand.index}, %eax\n");
                break;
            }
            //TODO: clear other var in eax
            variables_already_seen.set(binop.result()->name(), "%eax");
            var_in_eax = binop.result();
        } else if (operation.is_return_statement()) {
            generator.append("\tpopl\t%ebp\n\tret\n");
        } else if (operation.is_variable()) {
            auto& var = reinterpret_cast<const SIR::Variable&>(operation);
            generator.set("operand.stack_position", String::format("%zu", param_stack));
            generator.append("\tmovl\t{operand.stack_position}(%ebp), %eax\n");
            variables_already_seen.set(var.name(), String::format("%zu(%%ebp)", param_stack));
            param_stack += var.node_type()->size_in_bytes();
            var_in_eax = &var;
        } else {
            ASSERT_NOT_REACHED();
        }
    }

    generator.append("\t.size {function.name}, .-{function.name}\n");
    m_output_file->write(generator.as_string());
}

void I386Assembly::print_asm()
{
    //TODO: implement String.last_index_of
    auto input_file_name = m_options.input_file;
    while (input_file_name.contains("/")) {
        auto new_path = input_file_name.index_of("/");

        assert(new_path.has_value());
        input_file_name = input_file_name.substring(new_path.value() + 1);
    }

    StringBuilder builder;
    auto generator = SourceGenerator(builder, '{', '}');
    generator.set("input.filename", input_file_name);
    generator.append("\t.file \"{input.filename}\"\n");
    generator.append("\t.ident \"Serenity-c++ compiler V0.0.0\"\n");
    generator.append("\t.section \".note.GNU-stack\",\"\",@progbits\n");

    m_output_file->write(generator.as_string());

    for (auto& function : m_tu.functions()) {
        print_assembly_for_function(function);
    }
}

}
