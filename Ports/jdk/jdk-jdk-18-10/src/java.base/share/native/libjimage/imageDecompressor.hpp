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

#ifndef LIBJIMAGE_IMAGEDECOMPRESSOR_HPP
#define LIBJIMAGE_IMAGEDECOMPRESSOR_HPP

#include <assert.h>
#include <string.h>

#include "imageFile.hpp"
#include "inttypes.hpp"
#include "jni.h"

/*
 * Compressed resources located in image have an header.
 * This header contains:
 * - _magic: A magic u4, required to retrieved the header in the compressed content
 * - _size: The size of the compressed resource.
 * - _uncompressed_size: The uncompressed size of the compressed resource.
 * - _decompressor_name_offset: The ImageDecompressor instance name StringsTable offset.
 * - _decompressor_config_offset: StringsTable offset of configuration that could be needed by
 *   the decompressor in order to decompress.
 * - _is_terminal: 1: the compressed content is terminal. Uncompressing it would
 *   create the actual resource. 0: the compressed content is not terminal. Uncompressing it
 *   will result in a compressed content to be decompressed (This occurs when a stack of compressors
 *   have been used to compress the resource.
 */
struct ResourceHeader {
    /* magic bytes that identifies a compressed resource header*/
    static const u4 resource_header_magic = 0xCAFEFAFA;
    u4 _magic; // Resource header
    u8 _size;    // Resource size
    u8 _uncompressed_size;  // Expected uncompressed size
    u4 _decompressor_name_offset;    // Strings table decompressor offset
    u4 _decompressor_config_offset; // Strings table config offset
    u1 _is_terminal; // Last decompressor 1, otherwise 0.
};

/*
 * Resources located in jimage file can be compressed. Compression occurs at
 * jimage file creation time. When compressed a resource is added an header that
 * contains the name of the compressor that compressed it.
 * Various compression strategies can be applied to compress a resource.
 * The same resource can even be compressed multiple time by a stack of compressors.
 * At runtime, a resource is decompressed in a loop until there is no more header
 * meaning that the resource is equivalent to the not compressed resource.
 * In each iteration, the name of the compressor located in the current header
 * is used to retrieve the associated instance of ImageDecompressor.
 * For example "zip" is the name of the compressor that compresses resources
 * using the zip algorithm. The ZipDecompressor class name is also "zip".
 * ImageDecompressor instances are retrieved from a static array in which
 * they are registered.
 */
class ImageDecompressor {

private:
    const char* _name;

    /*
     * Array of concrete decompressors. This array is used to retrieve the decompressor
     * that can handle resource decompression.
     */
    static ImageDecompressor** _decompressors;
    /**
     * Num of decompressors
     */
    static int _decompressors_num;
    /*
     * Identifier of a decompressor. This name is the identification key to retrieve
     * decompressor from a resource header.
     */
    inline const char* get_name() const { return _name; }

    static u8 getU8(u1* ptr, Endian *endian);
    static u4 getU4(u1* ptr, Endian *endian);

protected:
    ImageDecompressor(const char* name) : _name(name) {
    }
    virtual void decompress_resource(u1* data, u1* uncompressed,
        ResourceHeader* header, const ImageStrings* strings) = 0;

public:
    static void image_decompressor_init();
    static void image_decompressor_close();
    static ImageDecompressor* get_decompressor(const char * decompressor_name) ;
    static void decompress_resource(u1* compressed, u1* uncompressed,
        u8 uncompressed_size, const ImageStrings* strings, Endian* _endian);
};

/**
 * Zip decompressor.
 */
class ZipDecompressor : public ImageDecompressor {
public:
    ZipDecompressor(const char* sym) : ImageDecompressor(sym) { }
    void decompress_resource(u1* data, u1* uncompressed, ResourceHeader* header,
        const ImageStrings* strings);
    static jboolean decompress(void *in, u8 inSize, void *out, u8 outSize, char **pmsg);
};

/*
 * Shared Strings decompressor. This decompressor reconstruct the class
 * constant pool UTF_U entries by retrieving strings stored in jimage strings table.
 * In addition, if the UTF_8 entry is a descriptor, the descriptor has to be rebuilt,
 * all java type having been removed from the descriptor and added to the sting table.
 * eg: "(Ljava/lang/String;I)V" ==> "(L;I)V" and "java/lang", "String"
 * stored in string table. offsets to the 2 strings are compressed and stored in the
 * constantpool entry.
 */
class SharedStringDecompressor : public ImageDecompressor {
private:
    // the constant pool tag for UTF8 string located in strings table
    static const int externalized_string = 23;
    // the constant pool tag for UTF8 descriptors string located in strings table
    static const int externalized_string_descriptor = 25;
    // the constant pool tag for UTF8
    static const int constant_utf8 = 1;
    // the constant pool tag for long
    static const int constant_long = 5;
    // the constant pool tag for double
    static const int constant_double = 6;
    // array index is the constant pool tag. value is size.
    // eg: array[5]  = 8; means size of long is 8 bytes.
    static const u1 sizes[];
    // bit 5 and 6 are used to store the length of the compressed integer.
    // size can be 1 (01), 2 (10), 3 (11).
    // 0x60 ==> 0110000
    static const int compressed_index_size_mask = 0x60;
    /*
     * mask the length bits (5 and 6) and move to the right 5 bits.
     */
    inline static int get_compressed_length(char c) {
        return ((char) (c & compressed_index_size_mask) >> 5);
    }
    inline static bool is_compressed(signed char b1) { return b1 < 0; }
    static int decompress_int(unsigned char*& value);
public:
    SharedStringDecompressor(const char* sym) : ImageDecompressor(sym){}
    void decompress_resource(u1* data, u1* uncompressed, ResourceHeader* header,
    const ImageStrings* strings);
};
#endif // LIBJIMAGE_IMAGEDECOMPRESSOR_HPP
