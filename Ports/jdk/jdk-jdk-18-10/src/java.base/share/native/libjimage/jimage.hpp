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

#include "jni.h"

// Opaque reference to a JImage file.
class JImageFile;
// Opaque reference to an image file resource location.
typedef jlong JImageLocationRef;

// Max path length limit independent of platform.  Windows max path is 1024,
// other platforms use 4096.
#define JIMAGE_MAX_PATH 4096

// JImage Error Codes

// Resource was not found
#define JIMAGE_NOT_FOUND (0)
// The image file is not prefixed with 0xCAFEDADA
#define JIMAGE_BAD_MAGIC (-1)
// The image file does not have a compatible (translatable) version
#define JIMAGE_BAD_VERSION (-2)
// The image file content is malformed
#define JIMAGE_CORRUPTED (-3)

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
JIMAGE_Open(const char *name, jint* error);

typedef JImageFile* (*JImageOpen_t)(const char *name, jint* error);

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
JIMAGE_Close(JImageFile* jimage);

typedef void (*JImageClose_t)(JImageFile* jimage);


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

extern "C" JNIEXPORT const char *
JIMAGE_PackageToModule(JImageFile* jimage, const char* package_name);

typedef const char* (*JImagePackageToModule_t)(JImageFile* jimage, const char* package_name);


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
 *                                "java.base", "9.0", "java/lang/String.class", &size);
 */
extern "C" JNIEXPORT JImageLocationRef JIMAGE_FindResource(JImageFile* jimage,
        const char* module_name, const char* version, const char* name,
        jlong* size);

typedef JImageLocationRef(*JImageFindResource_t)(JImageFile* jimage,
        const char* module_name, const char* version, const char* name,
        jlong* size);


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
 *  JImageLocationRef location = (*JImageFindResource)(image,
 *                               "java.base", "9.0", "java/lang/String.class", &size);
 *  char* buffer = new char[size];
 *  (*JImageGetResource)(image, location, buffer, size);
 */
extern "C" JNIEXPORT jlong
JIMAGE_GetResource(JImageFile* jimage, JImageLocationRef location,
        char* buffer, jlong size);

typedef jlong(*JImageGetResource_t)(JImageFile* jimage, JImageLocationRef location,
        char* buffer, jlong size);


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

typedef bool (*JImageResourceVisitor_t)(JImageFile* jimage,
        const char* module_name, const char* version, const char* package,
        const char* name, const char* extension, void* arg);

extern "C" JNIEXPORT void
JIMAGE_ResourceIterator(JImageFile* jimage,
        JImageResourceVisitor_t visitor, void *arg);

typedef void (*JImageResourceIterator_t)(JImageFile* jimage,
        JImageResourceVisitor_t visitor, void* arg);
