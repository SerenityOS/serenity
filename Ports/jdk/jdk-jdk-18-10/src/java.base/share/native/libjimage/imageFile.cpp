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

#include <assert.h>
#include <string.h>
#include <stdlib.h>

#include "endian.hpp"
#include "imageDecompressor.hpp"
#include "imageFile.hpp"
#include "inttypes.hpp"
#include "jni.h"
#include "osSupport.hpp"

// Map the full jimage, only with 64 bit addressing.
bool ImageFileReader::memory_map_image = sizeof(void *) == 8;

#ifdef WIN32
const char FileSeparator = '\\';
#else
const char FileSeparator = '/';
#endif

// Image files are an alternate file format for storing classes and resources. The
// goal is to supply file access which is faster and smaller than the jar format.
//
// (More detailed nodes in the header.)
//

// Compute the Perfect Hashing hash code for the supplied UTF-8 string.
s4 ImageStrings::hash_code(const char* string, s4 seed) {
    assert(seed > 0 && "invariant");
    // Access bytes as unsigned.
    u1* bytes = (u1*)string;
    u4 useed = (u4)seed;
    // Compute hash code.
    for (u1 byte = *bytes++; byte; byte = *bytes++) {
        useed = (useed * HASH_MULTIPLIER) ^ byte;
    }
    // Ensure the result is not signed.
    return (s4)(useed & 0x7FFFFFFF);
}

// Match up a string in a perfect hash table.
// Returns the index where the name should be.
// Result still needs validation for precise match (false positive.)
s4 ImageStrings::find(Endian* endian, const char* name, s4* redirect, u4 length) {
    // If the table is empty, then short cut.
    if (!redirect || !length) {
        return NOT_FOUND;
    }
    // Compute the basic perfect hash for name.
    s4 hash_code = ImageStrings::hash_code(name);
    // Modulo table size.
    s4 index = hash_code % length;
    // Get redirect entry.
    //   value == 0 then not found
    //   value < 0 then -1 - value is true index
    //   value > 0 then value is seed for recomputing hash.
    s4 value = endian->get(redirect[index]);
    // if recompute is required.
    if (value > 0 ) {
        // Entry collision value, need to recompute hash.
        hash_code = ImageStrings::hash_code(name, value);
        // Modulo table size.
        return hash_code % length;
    } else if (value < 0) {
        // Compute direct index.
        return -1 - value;
    }
    // No entry found.
    return NOT_FOUND;
}

// Test to see if UTF-8 string begins with the start UTF-8 string.  If so,
// return non-NULL address of remaining portion of string.  Otherwise, return
// NULL.    Used to test sections of a path without copying from image string
// table.
const char* ImageStrings::starts_with(const char* string, const char* start) {
    char ch1, ch2;
    // Match up the strings the best we can.
    while ((ch1 = *string) && (ch2 = *start)) {
        if (ch1 != ch2) {
            // Mismatch, return NULL.
            return NULL;
        }
        // Next characters.
        string++, start++;
    }
    // Return remainder of string.
    return string;
}

// Inflates the attribute stream into individual values stored in the long
// array _attributes. This allows an attribute value to be quickly accessed by
// direct indexing.  Unspecified values default to zero (from constructor.)
void ImageLocation::set_data(u1* data) {
    // Deflate the attribute stream into an array of attributes.
    u1 byte;
    // Repeat until end header is found.
    while ((data != NULL) && (byte = *data)) {
        // Extract kind from header byte.
        u1 kind = attribute_kind(byte);
        assert(kind < ATTRIBUTE_COUNT && "invalid image location attribute");
        // Extract length of data (in bytes).
        u1 n = attribute_length(byte);
        // Read value (most significant first.)
        _attributes[kind] = attribute_value(data + 1, n);
        // Position to next attribute by skipping attribute header and data bytes.
        data += n + 1;
    }
}

// Zero all attribute values.
void ImageLocation::clear_data() {
    // Set defaults to zero.
    memset(_attributes, 0, sizeof(_attributes));
}

// ImageModuleData constructor maps out sub-tables for faster access.
ImageModuleData::ImageModuleData(const ImageFileReader* image_file) :
        _image_file(image_file),
        _endian(image_file->endian()) {
}

// Release module data resource.
ImageModuleData::~ImageModuleData() {
}


// Return the module in which a package resides.    Returns NULL if not found.
const char* ImageModuleData::package_to_module(const char* package_name) {
    // replace all '/' by '.'
    char* replaced = new char[(int) strlen(package_name) + 1];
    assert(replaced != NULL && "allocation failed");
    int i;
    for (i = 0; package_name[i] != '\0'; i++) {
      replaced[i] = package_name[i] == '/' ? '.' : package_name[i];
    }
    replaced[i] = '\0';

    // build path /packages/<package_name>
    const char* radical = "/packages/";
    char* path = new char[(int) strlen(radical) + (int) strlen(package_name) + 1];
    assert(path != NULL && "allocation failed");
    strcpy(path, radical);
    strcat(path, replaced);
    delete[] replaced;

    // retrieve package location
    ImageLocation location;
    bool found = _image_file->find_location(path, location);
    delete[] path;
    if (!found) {
        return NULL;
    }

    // retrieve offsets to module name
    int size = (int)location.get_attribute(ImageLocation::ATTRIBUTE_UNCOMPRESSED);
    u1* content = new u1[size];
    assert(content != NULL && "allocation failed");
    _image_file->get_resource(location, content);
    u1* ptr = content;
    // sequence of sizeof(8) isEmpty|offset. Use the first module that is not empty.
    u4 offset = 0;
    for (i = 0; i < size; i+=8) {
        u4 isEmpty = _endian->get(*((u4*)ptr));
        ptr += 4;
        if (!isEmpty) {
            offset = _endian->get(*((u4*)ptr));
            break;
        }
        ptr += 4;
    }
    delete[] content;
    return _image_file->get_strings().get(offset);
}

// Manage a table of open image files.  This table allows multiple access points
// to share an open image.
ImageFileReaderTable::ImageFileReaderTable() : _count(0), _max(_growth) {
    _table = static_cast<ImageFileReader**>(calloc(_max, sizeof(ImageFileReader*)));
    assert(_table != NULL && "allocation failed");
}

// Add a new image entry to the table.
void ImageFileReaderTable::add(ImageFileReader* image) {
    if (_count == _max) {
        _max += _growth;
        _table = static_cast<ImageFileReader**>(realloc(_table, _max * sizeof(ImageFileReader*)));
    }
    _table[_count++] = image;
}

// Remove an image entry from the table.
void ImageFileReaderTable::remove(ImageFileReader* image) {
    for (u4 i = 0; i < _count; i++) {
        if (_table[i] == image) {
            // Swap the last element into the found slot
            _table[i] = _table[--_count];
            break;
        }
    }

    if (_count != 0 && _count == _max - _growth) {
        _max -= _growth;
        _table = static_cast<ImageFileReader**>(realloc(_table, _max * sizeof(ImageFileReader*)));
    }
}

// Determine if image entry is in table.
bool ImageFileReaderTable::contains(ImageFileReader* image) {
    for (u4 i = 0; i < _count; i++) {
        if (_table[i] == image) {
            return true;
        }
    }
    return false;
}

// Table to manage multiple opens of an image file.
ImageFileReaderTable ImageFileReader::_reader_table;

SimpleCriticalSection _reader_table_lock;

// Locate an image if file already open.
ImageFileReader* ImageFileReader::find_image(const char* name) {
    // Lock out _reader_table.
    SimpleCriticalSectionLock cs(&_reader_table_lock);
    // Search for an exist image file.
    for (u4 i = 0; i < _reader_table.count(); i++) {
        // Retrieve table entry.
        ImageFileReader* reader = _reader_table.get(i);
        // If name matches, then reuse (bump up use count.)
        assert(reader->name() != NULL && "reader->name must not be null");
        if (strcmp(reader->name(), name) == 0) {
            reader->inc_use();
            return reader;
        }
    }

    return NULL;
}

// Open an image file, reuse structure if file already open.
ImageFileReader* ImageFileReader::open(const char* name, bool big_endian) {
    ImageFileReader* reader = find_image(name);
    if (reader != NULL) {
        return reader;
    }

    // Need a new image reader.
    reader = new ImageFileReader(name, big_endian);
    if (reader == NULL || !reader->open()) {
        // Failed to open.
        delete reader;
        return NULL;
    }

    // Lock to update
    SimpleCriticalSectionLock cs(&_reader_table_lock);
    // Search for an existing image file.
    for (u4 i = 0; i < _reader_table.count(); i++) {
        // Retrieve table entry.
        ImageFileReader* existing_reader = _reader_table.get(i);
        // If name matches, then reuse (bump up use count.)
        assert(reader->name() != NULL && "reader->name still must not be null");
        if (strcmp(existing_reader->name(), name) == 0) {
            existing_reader->inc_use();
            reader->close();
            delete reader;
            return existing_reader;
        }
    }
    // Bump use count and add to table.
    reader->inc_use();
    _reader_table.add(reader);
    return reader;
}

// Close an image file if the file is not in use elsewhere.
void ImageFileReader::close(ImageFileReader *reader) {
    // Lock out _reader_table.
    SimpleCriticalSectionLock cs(&_reader_table_lock);
    // If last use then remove from table and then close.
    if (reader->dec_use()) {
        _reader_table.remove(reader);
        delete reader;
    }
}

// Return an id for the specifed ImageFileReader.
u8 ImageFileReader::reader_to_ID(ImageFileReader *reader) {
    // ID is just the cloaked reader address.
    return (u8)reader;
}

// Validate the image id.
bool ImageFileReader::id_check(u8 id) {
    // Make sure the ID is a managed (_reader_table) reader.
    SimpleCriticalSectionLock cs(&_reader_table_lock);
    return _reader_table.contains((ImageFileReader*)id);
}

// Return an id for the specifed ImageFileReader.
ImageFileReader* ImageFileReader::id_to_reader(u8 id) {
    assert(id_check(id) && "invalid image id");
    return (ImageFileReader*)id;
}

// Constructor intializes to a closed state.
ImageFileReader::ImageFileReader(const char* name, bool big_endian) :
    _module_data(NULL) {
    // Copy the image file name.
     int len = (int) strlen(name) + 1;
    _name = new char[len];
    assert(_name != NULL  && "allocation failed");
    strncpy(_name, name, len);
    // Initialize for a closed file.
    _fd = -1;
    _endian = Endian::get_handler(big_endian);
    _index_data = NULL;
}

// Close image and free up data structures.
ImageFileReader::~ImageFileReader() {
    // Ensure file is closed.
    close();
    // Free up name.
    if (_name) {
        delete[] _name;
        _name = NULL;
    }

    if (_module_data != NULL) {
        delete _module_data;
    }
}

// Open image file for read access.
bool ImageFileReader::open() {
    // If file exists open for reading.
    _fd = osSupport::openReadOnly(_name);
    if (_fd == -1) {
        return false;
    }
    // Retrieve the file size.
    _file_size = osSupport::size(_name);
    // Read image file header and verify it has a valid header.
    size_t header_size = sizeof(ImageHeader);
    if (_file_size < header_size ||
        !read_at((u1*)&_header, header_size, 0) ||
        _header.magic(_endian) != IMAGE_MAGIC ||
        _header.major_version(_endian) != MAJOR_VERSION ||
        _header.minor_version(_endian) != MINOR_VERSION) {
        close();
        return false;
    }
    // Size of image index.
    _index_size = index_size();
    // Make sure file is large enough to contain the index.
    if (_file_size < _index_size) {
        return false;
    }
    // Memory map image (minimally the index.)
    _index_data = (u1*)osSupport::map_memory(_fd, _name, 0, (size_t)map_size());
    assert(_index_data && "image file not memory mapped");
    // Retrieve length of index perfect hash table.
    u4 length = table_length();
    // Compute offset of the perfect hash table redirect table.
    u4 redirect_table_offset = (u4)header_size;
    // Compute offset of index attribute offsets.
    u4 offsets_table_offset = redirect_table_offset + length * (u4)sizeof(s4);
    // Compute offset of index location attribute data.
    u4 location_bytes_offset = offsets_table_offset + length * (u4)sizeof(u4);
    // Compute offset of index string table.
    u4 string_bytes_offset = location_bytes_offset + locations_size();
    // Compute address of the perfect hash table redirect table.
    _redirect_table = (s4*)(_index_data + redirect_table_offset);
    // Compute address of index attribute offsets.
    _offsets_table = (u4*)(_index_data + offsets_table_offset);
    // Compute address of index location attribute data.
    _location_bytes = _index_data + location_bytes_offset;
    // Compute address of index string table.
    _string_bytes = _index_data + string_bytes_offset;

    // Initialize the module data
    _module_data = new ImageModuleData(this);
    // Successful open (if memory allocation succeeded).
    return _module_data != NULL;
}

// Close image file.
void ImageFileReader::close() {
    // Deallocate the index.
    if (_index_data) {
        osSupport::unmap_memory((char*)_index_data, (size_t)map_size());
        _index_data = NULL;
    }
    // Close file.
    if (_fd != -1) {
        osSupport::close(_fd);
        _fd = -1;
    }

    if (_module_data != NULL) {
        delete _module_data;
        _module_data = NULL;
    }
}

// Read directly from the file.
bool ImageFileReader::read_at(u1* data, u8 size, u8 offset) const {
    return (u8)osSupport::read(_fd, (char*)data, size, offset) == size;
}

// Find the location attributes associated with the path.    Returns true if
// the location is found, false otherwise.
bool ImageFileReader::find_location(const char* path, ImageLocation& location) const {
    // Locate the entry in the index perfect hash table.
    s4 index = ImageStrings::find(_endian, path, _redirect_table, table_length());
    // If is found.
    if (index != ImageStrings::NOT_FOUND) {
        // Get address of first byte of location attribute stream.
        u1* data = get_location_data(index);
        // Expand location attributes.
        location.set_data(data);
        // Make sure result is not a false positive.
        return verify_location(location, path);
    }
    return false;
}

// Find the location index and size associated with the path.
// Returns the location index and size if the location is found, 0 otherwise.
u4 ImageFileReader::find_location_index(const char* path, u8 *size) const {
    // Locate the entry in the index perfect hash table.
    s4 index = ImageStrings::find(_endian, path, _redirect_table, table_length());
    // If found.
    if (index != ImageStrings::NOT_FOUND) {
        // Get address of first byte of location attribute stream.
        u4 offset = get_location_offset(index);
        u1* data = get_location_offset_data(offset);
        // Expand location attributes.
        ImageLocation location(data);
        // Make sure result is not a false positive.
        if (verify_location(location, path)) {
                *size = (jlong)location.get_attribute(ImageLocation::ATTRIBUTE_UNCOMPRESSED);
                return offset;
        }
    }
    return 0;            // not found
}

// Verify that a found location matches the supplied path (without copying.)
bool ImageFileReader::verify_location(ImageLocation& location, const char* path) const {
    // Manage the image string table.
    ImageStrings strings(_string_bytes, _header.strings_size(_endian));
    // Position to first character of the path string.
    const char* next = path;
    // Get module name string.
    const char* module = location.get_attribute(ImageLocation::ATTRIBUTE_MODULE, strings);
    // If module string is not empty.
    if (*module != '\0') {
        // Compare '/module/' .
        if (*next++ != '/') return false;
        if (!(next = ImageStrings::starts_with(next, module))) return false;
        if (*next++ != '/') return false;
    }
    // Get parent (package) string
    const char* parent = location.get_attribute(ImageLocation::ATTRIBUTE_PARENT, strings);
    // If parent string is not empty string.
    if (*parent != '\0') {
        // Compare 'parent/' .
        if (!(next = ImageStrings::starts_with(next, parent))) return false;
        if (*next++ != '/') return false;
    }
    // Get base name string.
    const char* base = location.get_attribute(ImageLocation::ATTRIBUTE_BASE, strings);
    // Compare with basne name.
    if (!(next = ImageStrings::starts_with(next, base))) return false;
    // Get extension string.
    const char* extension = location.get_attribute(ImageLocation::ATTRIBUTE_EXTENSION, strings);
    // If extension is not empty.
    if (*extension != '\0') {
        // Compare '.extension' .
        if (*next++ != '.') return false;
        if (!(next = ImageStrings::starts_with(next, extension))) return false;
    }
    // True only if complete match and no more characters.
    return *next == '\0';
}

// Return the resource for the supplied location offset.
void ImageFileReader::get_resource(u4 offset, u1* uncompressed_data) const {
        // Get address of first byte of location attribute stream.
        u1* data = get_location_offset_data(offset);
        // Expand location attributes.
        ImageLocation location(data);
        // Read the data
        get_resource(location, uncompressed_data);
}

// Return the resource for the supplied location.
void ImageFileReader::get_resource(ImageLocation& location, u1* uncompressed_data) const {
    // Retrieve the byte offset and size of the resource.
    u8 offset = location.get_attribute(ImageLocation::ATTRIBUTE_OFFSET);
    u8 uncompressed_size = location.get_attribute(ImageLocation::ATTRIBUTE_UNCOMPRESSED);
    u8 compressed_size = location.get_attribute(ImageLocation::ATTRIBUTE_COMPRESSED);
    // If the resource is compressed.
    if (compressed_size != 0) {
        u1* compressed_data;
        // If not memory mapped read in bytes.
        if (!memory_map_image) {
            // Allocate buffer for compression.
            compressed_data = new u1[(size_t)compressed_size];
            assert(compressed_data != NULL && "allocation failed");
            // Read bytes from offset beyond the image index.
            bool is_read = read_at(compressed_data, compressed_size, _index_size + offset);
            assert(is_read && "error reading from image or short read");
        } else {
            compressed_data = get_data_address() + offset;
        }
        // Get image string table.
        const ImageStrings strings = get_strings();
        // Decompress resource.
        ImageDecompressor::decompress_resource(compressed_data, uncompressed_data, uncompressed_size,
                        &strings, _endian);
        // If not memory mapped then release temporary buffer.
        if (!memory_map_image) {
                delete[] compressed_data;
        }
    } else {
        // Read bytes from offset beyond the image index.
        bool is_read = read_at(uncompressed_data, uncompressed_size, _index_size + offset);
        assert(is_read && "error reading from image or short read");
    }
}

// Return the ImageModuleData for this image
ImageModuleData * ImageFileReader::get_image_module_data() {
    return _module_data;
}
