/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#include <string.h>

#include "jimage.hpp"

#include "imageFile.hpp"

/*
 * JImageOpen - Given the supplied full path file name, open an image file. This
 * function will also initialize tables and retrieve meta-data necessary to
 * satisfy other functions in the API. If the image file has been previously
 * open, a new open request will share memory and resources used by the previous
 * open. A call to JImageOpen should be balanced by a call to JImageClose, to
 * release memory and resources used. If the image file is not found or cannot
 * be open, then NULL is returned and error will contain a reason for the
 * failure; a positive value for a system error number, negative for a jimage
 * specific error (see JImage Error Codes.)
 *
 *  Ex.
 *   jint error;
 *   JImageFile* jimage = (*JImageOpen)(JAVA_HOME "lib/modules", &error);
 *   if (image == NULL) {
 *     tty->print_cr("JImage failed to open: %d", error);
 *     ...
 *   }
 *   ...
 */
extern "C" JNIEXPORT JImageFile*
JIMAGE_Open(const char *name, jint* error) {
    // TODO - return a meaningful error code
    *error = 0;
    ImageFileReader* jfile = ImageFileReader::open(name);
    return (JImageFile*) jfile;
}

/*
 * JImageClose - Given the supplied open image file (see JImageOpen), release
 * memory and resources used by the open file and close the file. If the image
 * file is shared by other uses, release and close is deferred until the last use
 * is also closed.
 *
 * Ex.
 *  (*JImageClose)(image);
 */
extern "C" JNIEXPORT void
JIMAGE_Close(JImageFile* image) {
    ImageFileReader::close((ImageFileReader*) image);
}

/*
 * JImagePackageToModule - Given an open image file (see JImageOpen) and the name
 * of a package, return the name of module where the package resides. If the
 * package does not exist in the image file, the function returns NULL.
 * The resulting string does/should not have to be released. All strings are
 * utf-8, zero byte terminated.
 *
 * Ex.
 *  const char* package = (*JImagePackageToModule)(image, "java/lang");
 *  tty->print_cr(package);
 *  -> java.base
 */
extern "C" JNIEXPORT const char*
JIMAGE_PackageToModule(JImageFile* image, const char* package_name) {
    return ((ImageFileReader*) image)->get_image_module_data()->package_to_module(package_name);
}

/*
 * JImageFindResource - Given an open image file (see JImageOpen), a module
 * name, a version string and the name of a class/resource, return location
 * information describing the resource and its size. If no resource is found, the
 * function returns JIMAGE_NOT_FOUND and the value of size is undefined.
 * The version number should be "9.0" and is not used in locating the resource.
 * The resulting location does/should not have to be released.
 * All strings are utf-8, zero byte terminated.
 *
 *  Ex.
 *   jlong size;
 *   JImageLocationRef location = (*JImageFindResource)(image,
 *                                 "java.base", "9.0", "java/lang/String.class", &size);
 */
extern "C" JNIEXPORT JImageLocationRef
JIMAGE_FindResource(JImageFile* image,
        const char* module_name, const char* version, const char* name,
        jlong* size) {
    // Concatenate to get full path
    char fullpath[IMAGE_MAX_PATH];
    size_t moduleNameLen = strlen(module_name);
    size_t nameLen = strlen(name);
    size_t index;

    // TBD:   assert(moduleNameLen > 0 && "module name must be non-empty");
    assert(nameLen > 0 && "name must non-empty");

    // If the concatenated string is too long for the buffer, return not found
    if (1 + moduleNameLen + 1 + nameLen + 1 > IMAGE_MAX_PATH) {
        return 0L;
    }

    index = 0;
    fullpath[index++] = '/';
    memcpy(&fullpath[index], module_name, moduleNameLen);
    index += moduleNameLen;
    fullpath[index++] = '/';
    memcpy(&fullpath[index], name, nameLen);
    index += nameLen;
    fullpath[index++] = '\0';

    JImageLocationRef loc =
            (JImageLocationRef) ((ImageFileReader*) image)->find_location_index(fullpath, (u8*) size);
    return loc;
}

/*
 * JImageGetResource - Given an open image file (see JImageOpen), a resource's
 * location information (see JImageFindResource), a buffer of appropriate
 * size and the size, retrieve the bytes associated with the
 * resource. If the size is less than the resource size then the read is truncated.
 * If the size is greater than the resource size then the remainder of the buffer
 * is zero filled.  The function will return the actual size of the resource.
 *
 * Ex.
 *  jlong size;
 *   JImageLocationRef location = (*JImageFindResource)(image,
 *                                 "java.base", "9.0", "java/lang/String.class", &size);
 *  char* buffer = new char[size];
 *  (*JImageGetResource)(image, location, buffer, size);
 */
extern "C" JNIEXPORT jlong
JIMAGE_GetResource(JImageFile* image, JImageLocationRef location,
        char* buffer, jlong size) {
    ((ImageFileReader*) image)->get_resource((u4) location, (u1*) buffer);
    return size;
}

/*
 * JImageResourceIterator - Given an open image file (see JImageOpen), a visitor
 * function and a visitor argument, iterator through each of the image's resources.
 * The visitor function is called with the image file, the module name, the
 * package name, the base name, the extension and the visitor argument. The return
 * value of the visitor function should be true, unless an early iteration exit is
 * required. All strings are utf-8, zero byte terminated.file.
 *
 * Ex.
 *   bool ctw_visitor(JImageFile* jimage, const char* module_name, const char* version,
 *                  const char* package, const char* name, const char* extension, void* arg) {
 *     if (strcmp(extension, "class") == 0) {
 *       char path[JIMAGE_MAX_PATH];
 *       Thread* THREAD = Thread::current();
 *       jio_snprintf(path, JIMAGE_MAX_PATH - 1, "/%s/%s", package, name);
 *       ClassLoader::compile_the_world_in(path, (Handle)arg, THREAD);
 *       return !HAS_PENDING_EXCEPTION;
 *     }
 *     return true;
 *   }
 *   (*JImageResourceIterator)(image, ctw_visitor, loader);
 */
extern "C" JNIEXPORT void
JIMAGE_ResourceIterator(JImageFile* image,
        JImageResourceVisitor_t visitor, void* arg) {
    ImageFileReader* imageFile = (ImageFileReader*) image;
    u4 nEntries = imageFile->table_length();
    const ImageStrings strings = imageFile->get_strings();
    for (u4 i = 0; i < nEntries; i++) {
        ImageLocation location(imageFile->get_location_data(i));

        u4 moduleOffset = (u4) location.get_attribute(ImageLocation::ATTRIBUTE_MODULE);
        if (moduleOffset == 0) {
            continue; // skip non-modules
        }
        const char *module = strings.get(moduleOffset);
        if (strcmp(module, "modules") == 0
            || strcmp(module, "packages") == 0) {
            continue; // always skip
        }

        u4 parentOffset = (u4) location.get_attribute(ImageLocation::ATTRIBUTE_PARENT);
        const char *parent = strings.get(parentOffset);
        u4 baseOffset = (u4) location.get_attribute(ImageLocation::ATTRIBUTE_BASE);
        const char *base = strings.get(baseOffset);
        u4 extOffset = (u4) location.get_attribute(ImageLocation::ATTRIBUTE_EXTENSION);
        const char *extension = strings.get(extOffset);

        if (!(*visitor)(image, module, "9", parent, base, extension, arg)) {
            break;
        }
    }
}
