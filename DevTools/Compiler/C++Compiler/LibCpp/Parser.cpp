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

#include "Parser.h"
#include "AST.h"
#include "Option.h"
#include <LibCore/File.h>

#define DEBUG_LOG_SPAM
#include <AK/ScopeLogger.h>

#if defined DEBUG_LOG_SPAM && !defined DEBUG_CXX_PARSER
#    define DEBUG_CXX_PARSER
#endif

namespace Cpp {

// translation-unit:
//      - [declaration-seq]
Cpp::TranslationUnit Parser::parse_translation_unit()
{
    SCOPE_LOGGER();
    return { };
}

TranslationUnit Parser::parse(const Cpp::Option& options)
{
    Parser parser(options.input_file);

    return parser.parse_translation_unit();
}


ByteBuffer Parser::get_input_file_content(const String& filename)
{
    //TODO: maybe give the filename to the lexer.
    auto file = Core::File::open(filename, Core::IODevice::ReadOnly);
    assert(!file.is_error());
    return file.value()->read_all();
}

Parser::Parser(const String& filename)
    : m_file_content(get_input_file_content(filename))
    , m_lexer(m_file_content)
{
}


}