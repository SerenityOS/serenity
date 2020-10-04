/*
 * Copyright (c) 2020, Matthew Olsson <matthewcolsson@gmail.com>
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

#define JS_ENUMERATE_ERROR_TYPES(M)                                                                                                     \
    M(ArrayInvalidLength, "Invalid array length")                                                                                       \
    M(ArrayMaxSize, "Maximum array size exceeded")                                                                                      \
    M(ArrayPrototypeOneArg, "Array.prototype.{}() requires at least one argument")                                                      \
    M(AccessorBadField, "Accessor descriptor's '{}' field must be a function or undefined")                                             \
    M(AccessorValueOrWritable, "Accessor property descriptor cannot specify a value or writable key")                                   \
    M(BigIntBadOperator, "Cannot use {} operator with BigInt")                                                                          \
    M(BigIntBadOperatorOtherType, "Cannot use {} operator with BigInt and other type")                                                  \
    M(BigIntIntArgument, "BigInt argument must be an integer")                                                                          \
    M(BigIntInvalidValue, "Invalid value for BigInt: {}")                                                                               \
    M(ClassDoesNotExtendAConstructorOrNull, "Class extends value {} is not a constructor or null")                                      \
    M(Convert, "Cannot convert {} to {}")                                                                                               \
    M(ConvertUndefinedToObject, "Cannot convert undefined to object")                                                                   \
    M(DescChangeNonConfigurable, "Cannot change attributes of non-configurable property '{}'")                                          \
    M(FunctionArgsNotObject, "Argument array must be an object")                                                                        \
    M(InOperatorWithObject, "'in' operator must be used on an object")                                                                  \
    M(InstanceOfOperatorBadPrototype, "'prototype' property of {} is not an object")                                                    \
    M(InvalidAssignToConst, "Invalid assignment to const variable")                                                                     \
    M(InvalidLeftHandAssignment, "Invalid left-hand side in assignment")                                                                \
    M(InvalidRadix, "Radix must be an integer no less than 2, and no greater than 36")                                                  \
    M(IsNotA, "{} is not a {}")                                                                                                         \
    M(IsNotAEvaluatedFrom, "{} is not a {} (evaluated from '{}')")                                                                      \
    M(IterableNextBadReturn, "iterator.next() returned a non-object value")                                                             \
    M(IterableNextNotAFunction, "'next' property on returned object from Symbol.iterator method is "                                    \
                                "not a function")                                                                                       \
    M(JsonBigInt, "Cannot serialize BigInt value to JSON")                                                                              \
    M(JsonCircular, "Cannot stringify circular object")                                                                                 \
    M(JsonMalformed, "Malformed JSON string")                                                                                           \
    M(NotA, "Not a {} object")                                                                                                          \
    M(NotAConstructor, "{} is not a constructor")                                                                                       \
    M(NotAFunction, "{} is not a function")                                                                                             \
    M(NotAFunctionNoParam, "Not a function")                                                                                            \
    M(NotAn, "Not an {} object")                                                                                                        \
    M(NotAnObject, "{} is not an object")                                                                                               \
    M(NotASymbol, "{} is not a symbol")                                                                                                 \
    M(NotIterable, "{} is not iterable")                                                                                                \
    M(NonExtensibleDefine, "Cannot define property {} on non-extensible object")                                                        \
    M(NumberIncompatibleThis, "Number.prototype.{} method called with incompatible this target")                                        \
    M(ObjectDefinePropertyReturnedFalse, "Object's [[DefineProperty]] method returned false")                                           \
    M(ObjectSetPrototypeOfReturnedFalse, "Object's [[SetPrototypeOf]] method returned false")                                           \
    M(ObjectSetPrototypeOfTwoArgs, "Object.setPrototypeOf requires at least two arguments")                                             \
    M(ObjectPreventExtensionsReturnedFalse, "Object's [[PreventExtensions]] method returned false")                                     \
    M(ObjectPrototypeNullOrUndefinedOnSuperPropertyAccess,                                                                              \
        "Object prototype must not be {} on a super property access")                                                                   \
    M(ObjectPrototypeWrongType, "Prototype must be an object or null")                                                                  \
    M(ProxyCallWithNew, "Proxy must be called with the 'new' operator")                                                                 \
    M(ProxyConstructBadReturnType, "Proxy handler's construct trap violates invariant: must return "                                    \
                                   "an object")                                                                                         \
    M(ProxyConstructorBadType, "Expected {} argument of Proxy constructor to be object, got {}")                                        \
    M(ProxyDefinePropExistingConfigurable, "Proxy handler's defineProperty trap violates "                                              \
                                           "invariant: a property cannot be defined as non-configurable if it already exists on the "   \
                                           "target object as a configurable property")                                                  \
    M(ProxyDefinePropIncompatibleDescriptor, "Proxy handler's defineProperty trap violates "                                            \
                                             "invariant: the new descriptor is not compatible with the existing descriptor of the "     \
                                             "property on the target")                                                                  \
    M(ProxyDefinePropNonConfigurableNonExisting, "Proxy handler's defineProperty trap "                                                 \
                                                 "violates invariant: a property cannot be defined as non-configurable if it does not " \
                                                 "already exist on the target object")                                                  \
    M(ProxyDefinePropNonExtensible, "Proxy handler's defineProperty trap violates invariant: "                                          \
                                    "a property cannot be reported as being defined if the property does not exist on "                 \
                                    "the target and the target is non-extensible")                                                      \
    M(ProxyDeleteNonConfigurable, "Proxy handler's deleteProperty trap violates invariant: "                                            \
                                  "cannot report a non-configurable own property of the target as deleted")                             \
    M(ProxyGetImmutableDataProperty, "Proxy handler's get trap violates invariant: the "                                                \
                                     "returned value must match the value on the target if the property exists on the "                 \
                                     "target as a non-writable, non-configurable own data property")                                    \
    M(ProxyGetNonConfigurableAccessor, "Proxy handler's get trap violates invariant: the "                                              \
                                       "returned value must be undefined if the property exists on the target as a "                    \
                                       "non-configurable accessor property with an undefined get attribute")                            \
    M(ProxyGetOwnDescriptorExistingConfigurable, "Proxy handler's getOwnPropertyDescriptor "                                            \
                                                 "trap violates invariant: a property cannot be defined as non-configurable if it "     \
                                                 "already exists on the target object as a configurable property")                      \
    M(ProxyGetOwnDescriptorInvalidDescriptor, "Proxy handler's getOwnPropertyDescriptor trap "                                          \
                                              "violates invariant: invalid property descriptor for existing property on the target")    \
    M(ProxyGetOwnDescriptorInvalidNonConfig, "Proxy handler's getOwnPropertyDescriptor trap "                                           \
                                             "violates invariant: cannot report target's property as non-configurable if the "          \
                                             "property does not exist, or if it is configurable")                                       \
    M(ProxyGetOwnDescriptorNonConfigurable, "Proxy handler's getOwnPropertyDescriptor trap "                                            \
                                            "violates invariant: cannot return undefined for a property on the target which is "        \
                                            "a non-configurable property")                                                              \
    M(ProxyGetOwnDescriptorReturn, "Proxy handler's getOwnPropertyDescriptor trap violates "                                            \
                                   "invariant: must return an object or undefined")                                                     \
    M(ProxyGetOwnDescriptorUndefReturn, "Proxy handler's getOwnPropertyDescriptor trap "                                                \
                                        "violates invariant: cannot report a property as being undefined if it exists as an "           \
                                        "own property of the target and the target is non-extensible")                                  \
    M(ProxyGetPrototypeOfNonExtensible, "Proxy handler's getPrototypeOf trap violates "                                                 \
                                        "invariant: cannot return a different prototype object for a non-extensible target")            \
    M(ProxyGetPrototypeOfReturn, "Proxy handler's getPrototypeOf trap violates invariant: "                                             \
                                 "must return an object or null")                                                                       \
    M(ProxyHasExistingNonConfigurable, "Proxy handler's has trap violates invariant: a "                                                \
                                       "property cannot be reported as non-existent if it exists on the target as a "                   \
                                       "non-configurable property")                                                                     \
    M(ProxyHasExistingNonExtensible, "Proxy handler's has trap violates invariant: a property "                                         \
                                     "cannot be reported as non-existent if it exists on the target and the target is "                 \
                                     "non-extensible")                                                                                  \
    M(ProxyInvalidTrap, "Proxy handler's {} trap wasn't undefined, null, or callable")                                                  \
    M(ProxyIsExtensibleReturn, "Proxy handler's isExtensible trap violates invariant: "                                                 \
                               "return value must match the target's extensibility")                                                    \
    M(ProxyPreventExtensionsReturn, "Proxy handler's preventExtensions trap violates "                                                  \
                                    "invariant: cannot return true if the target object is extensible")                                 \
    M(ProxyRevoked, "An operation was performed on a revoked Proxy object")                                                             \
    M(ProxySetImmutableDataProperty, "Proxy handler's set trap violates invariant: cannot "                                             \
                                     "return true for a property on the target which is a non-configurable, non-writable "              \
                                     "own data property")                                                                               \
    M(ProxySetNonConfigurableAccessor, "Proxy handler's set trap violates invariant: cannot "                                           \
                                       "return true for a property on the target which is a non-configurable own accessor "             \
                                       "property with an undefined set attribute")                                                      \
    M(ProxySetPrototypeOfNonExtensible, "Proxy handler's setPrototypeOf trap violates "                                                 \
                                        "invariant: the argument must match the prototype of the target if the "                        \
                                        "target is non-extensible")                                                                     \
    M(ProxyTwoArguments, "Proxy constructor requires at least two arguments")                                                           \
    M(ReduceNoInitial, "Reduce of empty array with no initial value")                                                                   \
    M(ReferencePrimitiveAssignment, "Cannot assign property {} to primitive value")                                                     \
    M(ReferenceUnresolvable, "Unresolvable reference")                                                                                  \
    M(ReflectArgumentMustBeAFunction, "First argument of Reflect.{}() must be a function")                                              \
    M(ReflectArgumentMustBeAnObject, "First argument of Reflect.{}() must be an object")                                                \
    M(ReflectBadArgumentsList, "Arguments list must be an object")                                                                      \
    M(ReflectBadNewTarget, "Optional third argument of Reflect.construct() must be a constructor")                                      \
    M(ReflectBadDescriptorArgument, "Descriptor argument is not an object")                                                             \
    M(StringRawCannotConvert, "Cannot convert property 'raw' to object from {}")                                                        \
    M(StringRepeatCountMustBe, "repeat count must be a {} number")                                                                      \
    M(ThisHasNotBeenInitialized, "|this| has not been initialized")                                                                     \
    M(ThisIsAlreadyInitialized, "|this| is already initialized")                                                                        \
    M(ToObjectNullOrUndef, "ToObject on null or undefined")                                                                             \
    M(UnknownIdentifier, "'{}' is not defined")                                                                                         \
    /* LibWeb bindings */                                                                                                               \
    M(NotAByteString, "Argument to {}() must be a byte string")                                                                         \
    M(BadArgCountOne, "{}() needs one argument")                                                                                        \
    M(BadArgCountAtLeastOne, "{}() needs at least one argument")                                                                        \
    M(BadArgCountMany, "{}() needs {} arguments")

namespace JS {

class ErrorType {
public:
#define __ENUMERATE_JS_ERROR(name, message) \
    static const ErrorType name;
    JS_ENUMERATE_ERROR_TYPES(__ENUMERATE_JS_ERROR)
#undef __ENUMERATE_JS_ERROR

    const char* message() const
    {
        return m_message;
    }

private:
    explicit ErrorType(const char* message)
        : m_message(message)
    {
    }

    const char* m_message;
};

}
