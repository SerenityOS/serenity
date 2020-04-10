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

#define JS_ENUMERATE_ERROR_SUBCLASSES                              \
    __JS_ENUMERATE_ERROR_SUBCLASS(EvalError, eval_error)           \
    __JS_ENUMERATE_ERROR_SUBCLASS(InternalError, internal_error)   \
    __JS_ENUMERATE_ERROR_SUBCLASS(RangeError, range_error)         \
    __JS_ENUMERATE_ERROR_SUBCLASS(ReferenceError, reference_error) \
    __JS_ENUMERATE_ERROR_SUBCLASS(SyntaxError, syntax_error)       \
    __JS_ENUMERATE_ERROR_SUBCLASS(TypeError, type_error)           \
    __JS_ENUMERATE_ERROR_SUBCLASS(URIError, uri_error)

namespace JS {

class ASTNode;
class ArrayConstructor;
class BooleanConstructor;
class Cell;
class DateConstructor;
class Error;
class ErrorConstructor;
class Exception;
class Expression;
class Function;
class FunctionConstructor;
class GlobalObject;
class HandleImpl;
class Heap;
class HeapBlock;
class Interpreter;
class NumberConstructor;
class Object;
class ObjectConstructor;
class PrimitiveString;
class ScopeNode;
class Shape;
class Statement;
class Value;
enum class DeclarationKind;

#define __JS_ENUMERATE_ERROR_SUBCLASS(TitleCase, snake_case) \
    class TitleCase;                                         \
    class TitleCase##Constructor;                            \
    class TitleCase##Prototype;
JS_ENUMERATE_ERROR_SUBCLASSES
#undef __JS_ENUMERATE_ERROR_SUBCLASS

struct Argument;

template<class T>
class Handle;

}
