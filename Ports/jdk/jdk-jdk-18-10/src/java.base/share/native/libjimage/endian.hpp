/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef LIBJIMAGE_ENDIAN_HPP
#define LIBJIMAGE_ENDIAN_HPP

#include "inttypes.hpp"

// Selectable endian handling. Endian handlers are used when accessing values
// that are of unknown (until runtime) endian.  The only requirement of the values
// accessed are that they are aligned to proper size boundaries (no misalignment.)
// To select an endian handler, one should call Endian::get_handler(big_endian);
// Where big_endian is true if big endian is required and false otherwise.  The
// native endian handler can be fetched with Endian::get_native_handler();
// To retrieve a value using the approprate endian, use one of the overloaded
// calls to get. To set a value, then use one of the overloaded set calls.
// Ex.
//          s4 value; // Imported value;
//          ...
//          Endian* endian = Endian::get_handler(true);  // Use big endian
//          s4 corrected = endian->get(value);
//          endian->set(value, 1);
//
class Endian {
public:
    virtual u2 get(u2 x) = 0;
    virtual u4 get(u4 x) = 0;
    virtual u8 get(u8 x) = 0;
    virtual s2 get(s2 x) = 0;
    virtual s4 get(s4 x) = 0;
    virtual s8 get(s8 x) = 0;

    virtual void set(u2& x, u2 y) = 0;
    virtual void set(u4& x, u4 y) = 0;
    virtual void set(u8& x, u8 y) = 0;
    virtual void set(s2& x, s2 y) = 0;
    virtual void set(s4& x, s4 y) = 0;
    virtual void set(s8& x, s8 y) = 0;

    // Quick little endian test.
    static bool is_little_endian() { u4 x = 1; return *(u1 *)&x != 0; }

    // Quick big endian test.
    static bool is_big_endian() { return !is_little_endian(); }

    // Select an appropriate endian handler.
    static Endian* get_handler(bool big_endian);

    // Return the native endian handler.
    static Endian* get_native_handler();

    // get platform u2 from Java Big endian
    static u2 get_java(u1* x);
    // set platform u2 to Java Big endian
    static void set_java(u1* p, u2 x);
};

// Normal endian handling.
class NativeEndian : public Endian {
private:
    static NativeEndian _native;

public:
    u2 get(u2 x);
    u4 get(u4 x);
    u8 get(u8 x);
    s2 get(s2 x);
    s4 get(s4 x);
    s8 get(s8 x);

    void set(u2& x, u2 y);
    void set(u4& x, u4 y);
    void set(u8& x, u8 y);
    void set(s2& x, s2 y);
    void set(s4& x, s4 y);
    void set(s8& x, s8 y);

    static Endian* get_native() { return &_native; }
};

// Swapping endian handling.
class SwappingEndian : public Endian {
private:
    static SwappingEndian _swapping;

public:
    u2 get(u2 x);
    u4 get(u4 x);
    u8 get(u8 x);
    s2 get(s2 x);
    s4 get(s4 x);
    s8 get(s8 x);

    void set(u2& x, u2 y);
    void set(u4& x, u4 y);
    void set(u8& x, u8 y);
    void set(s2& x, s2 y);
    void set(s4& x, s4 y);
    void set(s8& x, s8 y);

    static Endian* get_swapping() { return &_swapping; }
};
#endif // LIBJIMAGE_ENDIAN_HPP
