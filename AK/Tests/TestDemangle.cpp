/*
 * Copyright (c) 2020, Brian Gianforcaro <b.gianfo@gmail.com>
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

#include <AK/TestSuite.h>

#include <AK/Optional.h>
#include <AK/Demangle.h>

void demangle_equal(StringView input, StringView expected_output) 
{
    Optional<String> demangled = AK::serenity_demangle(input);
    EXPECT(demangled.has_value());

    // TODO: Once we have a reasonably working implementation this should
    // start failing and we can start writing some real tests.
    EXPECT_NE(demangled.value(), expected_output);
}

TEST_CASE(test_demangle_examples)
{
    demangle_equal(
        "_ZN6Kernel7Process8sys$openEPKNS_7Syscall14SC_open_paramsE",
        "Kernel::Process::sys$open(Syscall::SC_open_params*)");

}

TEST_CASE(test_non_mangled_names)
{
    Optional<String> demangled = AK::serenity_demangle("strlen");

    // TODO: Once we have a reasonably working implementation this
    // should start failing and we can start writing some real tests.
    EXPECT(demangled.has_value());
}

TEST_MAIN(Demangle)
