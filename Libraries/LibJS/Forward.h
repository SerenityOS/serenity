/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#define JS_ENUMERATE_NATIVE_OBJECTS                                              \
    __JS_ENUMERATE(Array, array, ArrayPrototype, ArrayConstructor)               \
    __JS_ENUMERATE(BooleanObject, boolean, BooleanPrototype, BooleanConstructor) \
    __JS_ENUMERATE(Date, date, DatePrototype, DateConstructor)                   \
    __JS_ENUMERATE(Error, error, ErrorPrototype, ErrorConstructor)               \
    __JS_ENUMERATE(Function, function, FunctionPrototype, FunctionConstructor)   \
    __JS_ENUMERATE(NumberObject, number, NumberPrototype, NumberConstructor)     \
    __JS_ENUMERATE(Object, object, ObjectPrototype, ObjectConstructor)           \
    __JS_ENUMERATE(StringObject, string, StringPrototype, StringConstructor)

#define JS_ENUMERATE_ERROR_SUBCLASSES                                                                   \
    __JS_ENUMERATE(EvalError, eval_error, EvalErrorPrototype, EvalErrorConstructor)                     \
    __JS_ENUMERATE(InternalError, internal_error, InternalErrorPrototype, InternalErrorConstructor)     \
    __JS_ENUMERATE(RangeError, range_error, RangeErrorPrototype, RangeErrorConstructor)                 \
    __JS_ENUMERATE(ReferenceError, reference_error, ReferenceErrorPrototype, ReferenceErrorConstructor) \
    __JS_ENUMERATE(SyntaxError, syntax_error, SyntaxErrorPrototype, SyntaxErrorConstructor)             \
    __JS_ENUMERATE(TypeError, type_error, TypeErrorPrototype, TypeErrorConstructor)                     \
    __JS_ENUMERATE(URIError, uri_error, URIErrorPrototype, URIErrorConstructor)

#define JS_ENUMERATE_BUILTIN_TYPES \
    JS_ENUMERATE_NATIVE_OBJECTS    \
    JS_ENUMERATE_ERROR_SUBCLASSES

namespace JS {

class ASTNode;
class Cell;
class DeferGC;
class Error;
class Exception;
class Expression;
class GlobalObject;
class HandleImpl;
class Heap;
class HeapBlock;
class Interpreter;
class LexicalEnvironment;
class PrimitiveString;
class ScopeNode;
class Shape;
class Statement;
class Value;
enum class DeclarationKind;

#define __JS_ENUMERATE(ClassName, snake_name, ConstructorName, PrototypeName) \
    class ClassName;                                                          \
    class ConstructorName;                                                    \
    class PrototypeName;
JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE

struct Argument;

template<class T>
class Handle;

}
