/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "imageDecompressor.hpp"
#include "endian.hpp"
#ifdef WIN32
#include <windows.h>
#else
#include <dlfcn.h>
#endif

typedef jboolean (*ZipInflateFully_t)(void *inBuf, jlong inLen,
                                      void *outBuf, jlong outLen, char **pmsg);
static ZipInflateFully_t ZipInflateFully        = NULL;

#ifndef WIN32
    #define JNI_LIB_PREFIX "lib"
    #ifdef __APPLE__
        #define JNI_LIB_SUFFIX ".dylib"
    #else
        #define JNI_LIB_SUFFIX ".so"
    #endif
#endif

/**
 * Return the address of the entry point named in the zip shared library.
 * @param name - the name of the entry point
 * @return the address of the entry point or NULL
 */
static void* findEntry(const char* name) {
    void *addr = NULL;
#ifdef WIN32
    HMODULE handle = GetModuleHandle("zip.dll");
    if (handle == NULL) {
      handle = LoadLibrary("zip.dll");
    }
    if (handle == NULL) {
      return NULL;
    }
    addr = (void*) GetProcAddress(handle, name);
    return addr;
#else
    addr = dlopen(JNI_LIB_PREFIX "zip" JNI_LIB_SUFFIX, RTLD_GLOBAL|RTLD_LAZY);
    if (addr == NULL) {
        return NULL;
    }
    addr = dlsym(addr, name);
    return addr;
#endif
}

/*
 * Initialize the array of decompressors.
 */
int ImageDecompressor::_decompressors_num = 0;
ImageDecompressor** ImageDecompressor::_decompressors = NULL;
void ImageDecompressor::image_decompressor_init() {
    if (_decompressors == NULL) {
        ZipInflateFully = (ZipInflateFully_t) findEntry("ZIP_InflateFully");
     assert(ZipInflateFully != NULL && "ZIP decompressor not found.");
        _decompressors_num = 2;
        _decompressors = new ImageDecompressor*[_decompressors_num];
        _decompressors[0] = new ZipDecompressor("zip");
        _decompressors[1] = new SharedStringDecompressor("compact-cp");
    }
}

void ImageDecompressor::image_decompressor_close() {
    delete[] _decompressors;
}

/*
 * Locate decompressor.
 */
ImageDecompressor* ImageDecompressor::get_decompressor(const char * decompressor_name) {
    image_decompressor_init();
    for (int i = 0; i < _decompressors_num; i++) {
        ImageDecompressor* decompressor = _decompressors[i];
        assert(decompressor != NULL && "Decompressors not initialized.");
        if (strcmp(decompressor->get_name(), decompressor_name) == 0) {
            return decompressor;
        }
    }
    assert(false && "No decompressor found.");
    return NULL;
}

// Sparc to read unaligned content
// u8 l = (*(u8*) ptr);
// If ptr is not aligned, sparc will fail.
u8 ImageDecompressor::getU8(u1* ptr, Endian *endian) {
    u8 ret;
    if (endian->is_big_endian()) {
        ret = (u8)ptr[0] << 56 | (u8)ptr[1] << 48 | (u8)ptr[2]<<40 | (u8)ptr[3]<<32 |
                ptr[4]<<24 | ptr[5]<<16 | ptr[6]<<8 | ptr[7];
    } else {
        ret = ptr[0] | ptr[1]<<8 | ptr[2]<<16 | ptr[3]<<24 | (u8)ptr[4]<<32 |
                (u8)ptr[5]<<40 | (u8)ptr[6]<<48 | (u8)ptr[7]<<56;
    }
    return ret;
}

u4 ImageDecompressor::getU4(u1* ptr, Endian *endian) {
    u4 ret;
    if (endian->is_big_endian()) {
        ret = ptr[0] << 24 | ptr[1]<<16 | (ptr[2]<<8) | ptr[3];
    } else {
        ret = ptr[0] | ptr[1]<<8 | (ptr[2]<<16) | ptr[3]<<24;
    }
    return ret;
}

/*
 * Decompression entry point. Called from ImageFileReader::get_resource.
 */
void ImageDecompressor::decompress_resource(u1* compressed, u1* uncompressed,
                u8 uncompressed_size, const ImageStrings* strings, Endian *endian) {
    bool has_header = false;
    u1* decompressed_resource = compressed;
    u1* compressed_resource = compressed;
    // Resource could have been transformed by a stack of decompressors.
    // Iterate and decompress resources until there is no more header.
    do {
        ResourceHeader _header;
        u1* compressed_resource_base = compressed_resource;
        _header._magic = getU4(compressed_resource, endian);
        compressed_resource += 4;
        _header._size = getU8(compressed_resource, endian);
        compressed_resource += 8;
        _header._uncompressed_size = getU8(compressed_resource, endian);
        compressed_resource += 8;
        _header._decompressor_name_offset = getU4(compressed_resource, endian);
        compressed_resource += 4;
        _header._decompressor_config_offset = getU4(compressed_resource, endian);
        compressed_resource += 4;
        _header._is_terminal = *compressed_resource;
        compressed_resource += 1;
        has_header = _header._magic == ResourceHeader::resource_header_magic;
        if (has_header) {
            // decompressed_resource array contains the result of decompression
            decompressed_resource = new u1[(size_t) _header._uncompressed_size];
            // Retrieve the decompressor name
            const char* decompressor_name = strings->get(_header._decompressor_name_offset);
            assert(decompressor_name && "image decompressor not found");
            // Retrieve the decompressor instance
            ImageDecompressor* decompressor = get_decompressor(decompressor_name);
            assert(decompressor && "image decompressor not found");
            // Ask the decompressor to decompress the compressed content
            decompressor->decompress_resource(compressed_resource, decompressed_resource,
                &_header, strings);
            if (compressed_resource_base != compressed) {
                delete[] compressed_resource_base;
            }
            compressed_resource = decompressed_resource;
        }
    } while (has_header);
    memcpy(uncompressed, decompressed_resource, (size_t) uncompressed_size);
    delete[] decompressed_resource;
}

// Zip decompressor

void ZipDecompressor::decompress_resource(u1* data, u1* uncompressed,
                ResourceHeader* header, const ImageStrings* strings) {
    char* msg = NULL;
    jboolean res = ZipDecompressor::decompress(data, header->_size, uncompressed,
                    header->_uncompressed_size, &msg);
    assert(res && "decompression failed");
}

jboolean ZipDecompressor::decompress(void *in, u8 inSize, void *out, u8 outSize, char **pmsg) {
    return (*ZipInflateFully)(in, inSize, out, outSize, pmsg);
}

// END Zip Decompressor

// Shared String decompressor

// array index is the constant pool tag. value is size.
// eg: array[5]  = 8; means size of long is 8 bytes.
const u1 SharedStringDecompressor::sizes[] = {
    0, 0, 0, 4, 4, 8, 8, 2, 2, 4, 4, 4, 4, 0, 0, 3, 2, 0, 4
};
/**
 * Recreate the class by reconstructing the constant pool.
 */
void SharedStringDecompressor::decompress_resource(u1* data,
                u1* uncompressed_resource,
                ResourceHeader* header, const ImageStrings* strings) {
    u1* uncompressed_base = uncompressed_resource;
    u1* data_base = data;
    int header_size = 8; // magic + major + minor
    memcpy(uncompressed_resource, data, header_size + 2); //+ cp count
    uncompressed_resource += header_size + 2;
    data += header_size;
    u2 cp_count = Endian::get_java(data);
    data += 2;
    for (int i = 1; i < cp_count; i++) {
        u1 tag = *data;
        data += 1;
        switch (tag) {

            case externalized_string:
            { // String in Strings table
                *uncompressed_resource = 1;
                uncompressed_resource += 1;
                int k = decompress_int(data);
                const char * string = strings->get(k);
                int str_length = (int) strlen(string);
                Endian::set_java(uncompressed_resource, str_length);
                uncompressed_resource += 2;
                memcpy(uncompressed_resource, string, str_length);
                uncompressed_resource += str_length;
                break;
            }
            // Descriptor String has been split and types added to Strings table
            case externalized_string_descriptor:
            {
                *uncompressed_resource = 1;
                uncompressed_resource += 1;
                int descriptor_index = decompress_int(data);
                int indexes_length = decompress_int(data);
                u1* length_address = uncompressed_resource;
                uncompressed_resource += 2;
                int desc_length = 0;
                const char * desc_string = strings->get(descriptor_index);
                if (indexes_length > 0) {
                    u1* indexes_base = data;
                    data += indexes_length;
                    char c = *desc_string;
                    do {
                        *uncompressed_resource = c;
                        uncompressed_resource++;
                        desc_length += 1;
                        /*
                         * Every L character is the marker we are looking at in order
                         * to reconstruct the descriptor. Each time an L is found, then
                         * we retrieve the couple token/token at the current index and
                         * add it to the descriptor.
                         * "(L;I)V" and "java/lang","String" couple of tokens,
                         * this becomes "(Ljava/lang/String;I)V"
                         */
                        if (c == 'L') {
                            int index = decompress_int(indexes_base);
                            const char * pkg = strings->get(index);
                            int str_length = (int) strlen(pkg);
                            // the case where we have a package.
                            // reconstruct the type full name
                            if (str_length > 0) {
                                int len = str_length + 1;
                                char* fullpkg = new char[len];
                                char* pkg_base = fullpkg;
                                memcpy(fullpkg, pkg, str_length);
                                fullpkg += str_length;
                                *fullpkg = '/';
                                memcpy(uncompressed_resource, pkg_base, len);
                                uncompressed_resource += len;
                                delete[] pkg_base;
                                desc_length += len;
                            } else { // Empty package
                                // Nothing to do.
                            }
                            int classIndex = decompress_int(indexes_base);
                            const char * clazz = strings->get(classIndex);
                            int clazz_length = (int) strlen(clazz);
                            memcpy(uncompressed_resource, clazz, clazz_length);
                            uncompressed_resource += clazz_length;
                            desc_length += clazz_length;
                        }
                        desc_string += 1;
                        c = *desc_string;
                    } while (c != '\0');
                } else {
                        desc_length = (int) strlen(desc_string);
                        memcpy(uncompressed_resource, desc_string, desc_length);
                        uncompressed_resource += desc_length;
                }
                Endian::set_java(length_address, desc_length);
                break;
            }

            case constant_utf8:
            { // UTF-8
                *uncompressed_resource = tag;
                uncompressed_resource += 1;
                u2 str_length = Endian::get_java(data);
                int len = str_length + 2;
                memcpy(uncompressed_resource, data, len);
                uncompressed_resource += len;
                data += len;
                break;
            }

            case constant_long:
            case constant_double:
            {
                i++;
            }
            /* fall through */
            default:
            {
                *uncompressed_resource = tag;
                uncompressed_resource += 1;
                int size = sizes[tag];
                memcpy(uncompressed_resource, data, size);
                uncompressed_resource += size;
                data += size;
            }
        }
    }
    u8 remain = header->_size - (int)(data - data_base);
    u8 computed = (u8)(uncompressed_resource - uncompressed_base) + remain;
    if (header->_uncompressed_size != computed)
        printf("Failure, expecting %llu but getting %llu\n", header->_uncompressed_size,
                computed);
    assert(header->_uncompressed_size == computed &&
                "Constant Pool reconstruction failed");
    memcpy(uncompressed_resource, data, (size_t) remain);
}

/*
 * Decompress integers. Compressed integers are negative.
 * If positive, the integer is not decompressed.
 * If negative, length extracted from the first byte, then reconstruct the integer
 * from the following bytes.
 * Example of compression: 1 is compressed on 1 byte: 10100001
 */
int SharedStringDecompressor::decompress_int(unsigned char*& value) {
    int len = 4;
    int res = 0;
    char b1 = *value;
    if (is_compressed((signed char)b1)) { // compressed
        len = get_compressed_length(b1);
        char clearedValue = b1 &= 0x1F;
        if (len == 1) {
            res = clearedValue;
        } else {
            res = (clearedValue & 0xFF) << 8 * (len - 1);
            for (int i = 1; i < len; i++) {
                res |= (value[i]&0xFF) << 8 * (len - i - 1);
            }
        }
    } else {
        res = (value[0] & 0xFF) << 24 | (value[1]&0xFF) << 16 |
                    (value[2]&0xFF) << 8 | (value[3]&0xFF);
    }
    value += len;
    return res;
}
// END Shared String decompressor
