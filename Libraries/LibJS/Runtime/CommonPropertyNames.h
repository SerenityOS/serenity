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

#include <AK/FlyString.h>
#include <LibJS/Forward.h>

namespace JS {

#define ENUMERATE_STANDARD_PROPERTY_NAMES(P) \
    P(BigInt)                                \
    P(Boolean)                               \
    P(E)                                     \
    P(EPSILON)                               \
    P(Infinity)                              \
    P(JSON)                                  \
    P(LN10)                                  \
    P(LN2)                                   \
    P(LOG10E)                                \
    P(LOG2E)                                 \
    P(MAX_SAFE_INTEGER)                      \
    P(MIN_SAFE_INTEGER)                      \
    P(Math)                                  \
    P(NEGATIVE_INFINITY)                     \
    P(NaN)                                   \
    P(Number)                                \
    P(PI)                                    \
    P(POSITIVE_INFINITY)                     \
    P(Proxy)                                 \
    P(Reflect)                               \
    P(RegExp)                                \
    P(SQRT1_2)                               \
    P(SQRT2)                                 \
    P(String)                                \
    P(Symbol)                                \
    P(UTC)                                   \
    P(abs)                                   \
    P(acosh)                                 \
    P(apply)                                 \
    P(asIntN)                                \
    P(asUintN)                               \
    P(asinh)                                 \
    P(atanh)                                 \
    P(bind)                                  \
    P(call)                                  \
    P(cbrt)                                  \
    P(ceil)                                  \
    P(charAt)                                \
    P(charCodeAt)                            \
    P(clear)                                 \
    P(clz32)                                 \
    P(concat)                                \
    P(configurable)                          \
    P(console)                               \
    P(construct)                             \
    P(constructor)                           \
    P(cos)                                   \
    P(count)                                 \
    P(countReset)                            \
    P(debug)                                 \
    P(defineProperty)                        \
    P(deleteProperty)                        \
    P(description)                           \
    P(done)                                  \
    P(dotAll)                                \
    P(entries)                               \
    P(enumerable)                            \
    P(error)                                 \
    P(every)                                 \
    P(exec)                                  \
    P(exp)                                   \
    P(expm1)                                 \
    P(fill)                                  \
    P(filter)                                \
    P(find)                                  \
    P(findIndex)                             \
    P(flags)                                 \
    P(floor)                                 \
    P(forEach)                               \
    P(from)                                  \
    P(fromCharCode)                          \
    P(gc)                                    \
    P(get)                                   \
    P(getDate)                               \
    P(getDay)                                \
    P(getFullYear)                           \
    P(getHours)                              \
    P(getMilliseconds)                       \
    P(getMinutes)                            \
    P(getMonth)                              \
    P(getOwnPropertyDescriptor)              \
    P(getOwnPropertyNames)                   \
    P(getPrototypeOf)                        \
    P(getSeconds)                            \
    P(getTime)                               \
    P(getUTCDate)                            \
    P(getUTCDay)                             \
    P(getUTCFullYear)                        \
    P(getUTCHours)                           \
    P(getUTCMilliseconds)                    \
    P(getUTCMinutes)                         \
    P(getUTCMonth)                           \
    P(getUTCSeconds)                         \
    P(global)                                \
    P(globalThis)                            \
    P(groups)                                \
    P(has)                                   \
    P(hasOwnProperty)                        \
    P(ignoreCase)                            \
    P(includes)                              \
    P(index)                                 \
    P(indexOf)                               \
    P(info)                                  \
    P(input)                                 \
    P(is)                                    \
    P(isArray)                               \
    P(isExtensible)                          \
    P(isFinite)                              \
    P(isInteger)                             \
    P(isNaN)                                 \
    P(isSafeInteger)                         \
    P(join)                                  \
    P(keyFor)                                \
    P(keys)                                  \
    P(lastIndex)                             \
    P(lastIndexOf)                           \
    P(length)                                \
    P(log)                                   \
    P(log1p)                                 \
    P(map)                                   \
    P(max)                                   \
    P(message)                               \
    P(min)                                   \
    P(multiline)                             \
    P(name)                                  \
    P(next)                                  \
    P(now)                                   \
    P(of)                                    \
    P(ownKeys)                               \
    P(padEnd)                                \
    P(padStart)                              \
    P(parse)                                 \
    P(parseFloat)                            \
    P(pop)                                   \
    P(pow)                                   \
    P(preventExtensions)                     \
    P(prototype)                             \
    P(push)                                  \
    P(random)                                \
    P(raw)                                   \
    P(reduce)                                \
    P(reduceRight)                           \
    P(repeat)                                \
    P(reverse)                               \
    P(round)                                 \
    P(set)                                   \
    P(setPrototypeOf)                        \
    P(shift)                                 \
    P(sign)                                  \
    P(sin)                                   \
    P(slice)                                 \
    P(some)                                  \
    P(source)                                \
    P(splice)                                \
    P(sqrt)                                  \
    P(startsWith)                            \
    P(stringify)                             \
    P(sticky)                                \
    P(substring)                             \
    P(tan)                                   \
    P(test)                                  \
    P(toDateString)                          \
    P(toISOString)                           \
    P(toJSON)                                \
    P(toLocaleDateString)                    \
    P(toLocaleString)                        \
    P(toLocaleTimeString)                    \
    P(toLowerCase)                           \
    P(toString)                              \
    P(toTimeString)                          \
    P(toUpperCase)                           \
    P(trace)                                 \
    P(trim)                                  \
    P(trimEnd)                               \
    P(trimStart)                             \
    P(trunc)                                 \
    P(undefined)                             \
    P(unicode)                               \
    P(unshift)                               \
    P(value)                                 \
    P(valueOf)                               \
    P(values)                                \
    P(warn)                                  \
    P(writable)

struct CommonPropertyNames {
    FlyString for_ { "for" };
#define __ENUMERATE(x) FlyString x { #x };
    ENUMERATE_STANDARD_PROPERTY_NAMES(__ENUMERATE)
#undef __ENUMERATE
#define __JS_ENUMERATE(x, a, b, c) FlyString x { #x };
    JS_ENUMERATE_BUILTIN_TYPES
#undef __JS_ENUMERATE
#define __JS_ENUMERATE(x, a) FlyString x { #x };
    JS_ENUMERATE_WELL_KNOWN_SYMBOLS
#undef __JS_ENUMERATE
};

}
