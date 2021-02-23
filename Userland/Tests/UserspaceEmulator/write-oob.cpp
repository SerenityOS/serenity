/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
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

#include <AK/Assertions.h>
#include <LibCore/ArgsParser.h>
#include <mman.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static void write8(void* ptr) { *(volatile uint8_t*)ptr = 1; }
static void write16(void* ptr) { *(volatile uint16_t*)ptr = 1; }
static void write32(void* ptr) { *(volatile uint32_t*)ptr = 1; }
static void write64(void* ptr) { *(volatile double*)ptr = 1.0; }
// A u64 write might be translated by the compiler as a 32-then-32-bit write:
// static void write64_bad(void* ptr) { *(volatile uint64_t*)ptr = 1.0; }
// Let's hope this won't be translated like that.
// Godbolt says yes: https://godbolt.org/z/1b9WGo

static void run_test(void* region, ssize_t offset, size_t bits)
{
    void* ptr = (char*)region + offset;
    printf("Writing to %p\n", ptr);
    switch (bits) {
    case 8:
        write8(ptr);
        break;
    case 16:
        write16(ptr);
        break;
    case 32:
        write32(ptr);
        break;
    case 64:
        write64(ptr);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

int main(int argc, char** argv)
{
    bool do_static = false;
    int size = 10 * PAGE_SIZE;
    int offset = 10 * PAGE_SIZE - 1;
    int bits = 16;

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help(
        "Access out of bounds memory; a great testcase for UserEmulator.");
    args_parser.add_option(do_static, "Use a static region instead of an mmap'ed region. Fixes 'size' to 10*PAGESIZE = 40960. (Default: false)", "static", 'S');
    args_parser.add_option(size, "The size of the region to allocate. (Default: 10*PAGESIZE = 40960)", "size", 's', "size");
    args_parser.add_option(offset, "The signed offset at which to start writing. (Default: 10*PAGESIZE-1 = 40959)", "offset", 'o', "offset");
    args_parser.add_option(bits, "Amount of bits to write in a single instruction. (Default: 16)", "bits", 'b', "bits");
    args_parser.parse(argc, argv);

    if (do_static)
        size = 10 * PAGE_SIZE;

    printf("Writing %d bits to %s region of size %d at offset %d.\n",
        bits, do_static ? "static" : "MMAP", size, offset);

    if (do_static) {
        // Let's just hope the linker puts nothing after it!
        static unsigned char region[PAGE_SIZE * 10] = { 0 };

        run_test(region, offset, 64);
    } else {
        void* region = mmap(nullptr, size, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        VERIFY(region);
        run_test(region, offset, bits);
    }

    printf("FAIL (should have caused SIGSEGV)\n");
    return 1;
}
