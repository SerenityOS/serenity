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

#ifndef LIBJIMAGE_IMAGEFILE_HPP
#define LIBJIMAGE_IMAGEFILE_HPP

#include <assert.h>

#include "endian.hpp"
#include "inttypes.hpp"

// Image files are an alternate file format for storing classes and resources. The
// goal is to supply file access which is faster and smaller than the jar format.
// It should be noted that unlike jars, information stored in an image is in native
// endian format. This allows the image to be mapped into memory without endian
// translation.  This also means that images are platform dependent.
//
// Image files are structured as three sections;
//
//         +-----------+
//         |  Header   |
//         +-----------+
//         |           |
//         |   Index   |
//         |           |
//         +-----------+
//         |           |
//         |           |
//         | Resources |
//         |           |
//         |           |
//         +-----------+
//
// The header contains information related to identification and description of
// contents.
//
//         +-------------------------+
//         |   Magic (0xCAFEDADA)    |
//         +------------+------------+
//         | Major Vers | Minor Vers |
//         +------------+------------+
//         |          Flags          |
//         +-------------------------+
//         |      Resource Count     |
//         +-------------------------+
//         |       Table Length      |
//         +-------------------------+
//         |      Attributes Size    |
//         +-------------------------+
//         |       Strings Size      |
//         +-------------------------+
//
// Magic - means of identifying validity of the file.  This avoids requiring a
//         special file extension.
// Major vers, minor vers - differences in version numbers indicate structural
//                          changes in the image.
// Flags - various image wide flags (future).
// Resource count - number of resources in the file.
// Table length - the length of lookup tables used in the index.
// Attributes size - number of bytes in the region used to store location attribute
//                   streams.
// Strings size - the size of the region used to store strings used by the
//                index and meta data.
//
// The index contains information related to resource lookup. The algorithm
// used for lookup is "A Practical Minimal Perfect Hashing Method"
// (http://homepages.dcc.ufmg.br/~nivio/papers/wea05.pdf). Given a path string
// in the form /<module>/<package>/<base>.<extension>  return the resource location
// information;
//
//     redirectIndex = hash(path, DEFAULT_SEED) % table_length;
//     redirect = redirectTable[redirectIndex];
//     if (redirect == 0) return not found;
//     locationIndex = redirect < 0 ? -1 - redirect : hash(path, redirect) % table_length;
//     location = locationTable[locationIndex];
//     if (!verify(location, path)) return not found;
//     return location;
//
// Note: The hash function takes an initial seed value.  A different seed value
// usually returns a different result for strings that would otherwise collide with
// other seeds. The verify function guarantees the found resource location is
// indeed the resource we are looking for.
//
// The following is the format of the index;
//
//         +-------------------+
//         |   Redirect Table  |
//         +-------------------+
//         | Attribute Offsets |
//         +-------------------+
//         |   Attribute Data  |
//         +-------------------+
//         |      Strings      |
//         +-------------------+
//
// Redirect Table - Array of 32-bit signed values representing actions that
//                  should take place for hashed strings that map to that
//                  value.  Negative values indicate no hash collision and can be
//                  quickly converted to indices into attribute offsets.  Positive
//                  values represent a new seed for hashing an index into attribute
//                  offsets.  Zero indicates not found.
// Attribute Offsets - Array of 32-bit unsigned values representing offsets into
//                     attribute data.  Attribute offsets can be iterated to do a
//                     full survey of resources in the image.  Offset of zero
//                     indicates no attributes.
// Attribute Data - Bytes representing compact attribute data for locations. (See
//                  comments in ImageLocation.)
// Strings - Collection of zero terminated UTF-8 strings used by the index and
//           image meta data.  Each string is accessed by offset.  Each string is
//           unique.  Offset zero is reserved for the empty string.
//
// Note that the memory mapped index assumes 32 bit alignment of each component
// in the index.
//
// Endianness of an image.
// An image booted by hotspot is always in native endian.  However, it is possible
// to read (by the JDK) in alternate endian format.  Primarily, this is during
// cross platform scenarios.  Ex, where javac needs to read an embedded image
// to access classes for crossing compilation.
//

class ImageFileReader; // forward declaration

// Manage image file string table.
class ImageStrings {
private:
    u1* _data; // Data bytes for strings.
    u4 _size;  // Number of bytes in the string table.
public:
    enum {
        // Not found result from find routine.
        NOT_FOUND = -1,
        // Prime used to generate hash for Perfect Hashing.
        HASH_MULTIPLIER = 0x01000193
    };

    ImageStrings(u1* data, u4 size) : _data(data), _size(size) {}

    // Return the UTF-8 string beginning at offset.
    inline const char* get(u4 offset) const {
        assert(offset < _size && "offset exceeds string table size");
        return (const char*)(_data + offset);
    }

    // Compute the Perfect Hashing hash code for the supplied UTF-8 string.
    inline static u4 hash_code(const char* string) {
        return hash_code(string, HASH_MULTIPLIER);
    }

    // Compute the Perfect Hashing hash code for the supplied string, starting at seed.
    static s4 hash_code(const char* string, s4 seed);

    // Match up a string in a perfect hash table.    Result still needs validation
    // for precise match.
    static s4 find(Endian* endian, const char* name, s4* redirect, u4 length);

    // Test to see if UTF-8 string begins with the start UTF-8 string.  If so,
    // return non-NULL address of remaining portion of string.  Otherwise, return
    // NULL.    Used to test sections of a path without copying from image string
    // table.
    static const char* starts_with(const char* string, const char* start);

    // Test to see if UTF-8 string begins with start char.  If so, return non-NULL
    // address of remaining portion of string.  Otherwise, return NULL.  Used
    // to test a character of a path without copying.
    inline static const char* starts_with(const char* string, const char ch) {
        return *string == ch ? string + 1 : NULL;
    }
};

// Manage image file location attribute data.    Within an image, a location's
// attributes are compressed into a stream of bytes.    An attribute stream is
// composed of individual attribute sequences.  Each attribute sequence begins with
// a header byte containing the attribute 'kind' (upper 5 bits of header) and the
// 'length' less 1 (lower 3 bits of header) of bytes that follow containing the
// attribute value.  Attribute values present as most significant byte first.
//
// Ex. Container offset (ATTRIBUTE_OFFSET) 0x33562 would be represented as 0x22
// (kind = 4, length = 3), 0x03, 0x35, 0x62.
//
// An attribute stream is terminated with a header kind of ATTRIBUTE_END (header
// byte of zero.)
//
// ImageLocation inflates the stream into individual values stored in the long
// array _attributes. This allows an attribute value can be quickly accessed by
// direct indexing. Unspecified values default to zero.
//
// Notes:
//  - Even though ATTRIBUTE_END is used to mark the end of the attribute stream,
//      streams will contain zero byte values to represent lesser significant bits.
//      Thus, detecting a zero byte is not sufficient to detect the end of an attribute
//      stream.
//  - ATTRIBUTE_OFFSET represents the number of bytes from the beginning of the region
//      storing the resources.  Thus, in an image this represents the number of bytes
//      after the index.
//  - Currently, compressed resources are represented by having a non-zero
//      ATTRIBUTE_COMPRESSED value.  This represents the number of bytes stored in the
//      image, and the value of ATTRIBUTE_UNCOMPRESSED represents number of bytes of the
//      inflated resource in memory. If the ATTRIBUTE_COMPRESSED is zero then the value
//      of ATTRIBUTE_UNCOMPRESSED represents both the number of bytes in the image and
//      in memory.  In the future, additional compression techniques will be used and
//      represented differently.
//  - Package strings include trailing slash and extensions include prefix period.
//
class ImageLocation {
public:
    enum {
        ATTRIBUTE_END,                  // End of attribute stream marker
        ATTRIBUTE_MODULE,               // String table offset of module name
        ATTRIBUTE_PARENT,               // String table offset of resource path parent
        ATTRIBUTE_BASE,                 // String table offset of resource path base
        ATTRIBUTE_EXTENSION,        // String table offset of resource path extension
        ATTRIBUTE_OFFSET,               // Container byte offset of resource
        ATTRIBUTE_COMPRESSED,       // In image byte size of the compressed resource
        ATTRIBUTE_UNCOMPRESSED, // In memory byte size of the uncompressed resource
        ATTRIBUTE_COUNT                 // Number of attribute kinds
    };

private:
    // Values of inflated attributes.
    u8 _attributes[ATTRIBUTE_COUNT];

    // Return the attribute value number of bytes.
    inline static u1 attribute_length(u1 data) {
        return (data & 0x7) + 1;
    }

    // Return the attribute kind.
    inline static u1 attribute_kind(u1 data) {
        u1 kind = data >> 3;
        assert(kind < ATTRIBUTE_COUNT && "invalid attribute kind");
        return kind;
    }

    // Return the attribute length.
    inline static u8 attribute_value(u1* data, u1 n) {
        assert(0 < n && n <= 8 && "invalid attribute value length");
        u8 value = 0;
        // Most significant bytes first.
        for (u1 i = 0; i < n; i++) {
            value <<= 8;
            value |= data[i];
        }
        return value;
    }

public:
    ImageLocation() {
        clear_data();
    }

    ImageLocation(u1* data) {
        clear_data();
        set_data(data);
    }

    // Inflates the attribute stream into individual values stored in the long
    // array _attributes. This allows an attribute value to be quickly accessed by
    // direct indexing. Unspecified values default to zero.
    void set_data(u1* data);

    // Zero all attribute values.
    void clear_data();

    // Retrieve an attribute value from the inflated array.
    inline u8 get_attribute(u1 kind) const {
        assert(ATTRIBUTE_END < kind && kind < ATTRIBUTE_COUNT && "invalid attribute kind");
        return _attributes[kind];
    }

    // Retrieve an attribute string value from the inflated array.
    inline const char* get_attribute(u4 kind, const ImageStrings& strings) const {
        return strings.get((u4)get_attribute(kind));
    }
};

//
// Manage the image module meta data.
class ImageModuleData {
    const ImageFileReader* _image_file; // Source image file
    Endian* _endian;                    // Endian handler

public:
    ImageModuleData(const ImageFileReader* image_file);
    ~ImageModuleData();

    // Return the module in which a package resides.    Returns NULL if not found.
    const char* package_to_module(const char* package_name);
};

// Image file header, starting at offset 0.
class ImageHeader {
private:
    u4 _magic;          // Image file marker
    u4 _version;        // Image file major version number
    u4 _flags;          // Image file flags
    u4 _resource_count; // Number of resources in file
    u4 _table_length;   // Number of slots in index tables
    u4 _locations_size; // Number of bytes in attribute table
    u4 _strings_size;   // Number of bytes in string table

public:
    u4 magic() const { return _magic; }
    u4 magic(Endian* endian) const { return endian->get(_magic); }
    void set_magic(Endian* endian, u4 magic) { return endian->set(_magic, magic); }

    u4 major_version(Endian* endian) const { return endian->get(_version) >> 16; }
    u4 minor_version(Endian* endian) const { return endian->get(_version) & 0xFFFF; }
    void set_version(Endian* endian, u4 major_version, u4 minor_version) {
        return endian->set(_version, major_version << 16 | minor_version);
    }

    u4 flags(Endian* endian) const { return endian->get(_flags); }
    void set_flags(Endian* endian, u4 value) { return endian->set(_flags, value); }

    u4 resource_count(Endian* endian) const { return endian->get(_resource_count); }
    void set_resource_count(Endian* endian, u4 count) { return endian->set(_resource_count, count); }

    u4 table_length(Endian* endian) const { return endian->get(_table_length); }
    void set_table_length(Endian* endian, u4 count) { return endian->set(_table_length, count); }

    u4 locations_size(Endian* endian) const { return endian->get(_locations_size); }
    void set_locations_size(Endian* endian, u4 size) { return endian->set(_locations_size, size); }

    u4 strings_size(Endian* endian) const { return endian->get(_strings_size); }
    void set_strings_size(Endian* endian, u4 size) { return endian->set(_strings_size, size); }
};

// Max path length limit independent of platform.    Windows max path is 1024,
// other platforms use 4096.    The JCK fails several tests when 1024 is used.
#define IMAGE_MAX_PATH 4096

class ImageFileReader;

// Manage a table of open image files.  This table allows multiple access points
// to share an open image.
class ImageFileReaderTable {
private:
    const static u4 _growth = 8; // Growth rate of the table
    u4 _count;                   // Number of entries in the table
    u4 _max;                     // Maximum number of entries allocated
    ImageFileReader** _table;    // Growable array of entries

public:
    ImageFileReaderTable();
// ~ImageFileReaderTable()
// Bug 8166727
//
// WARNING: Should never close jimage files.
//          Threads may still be running during shutdown.
//

    // Return the number of entries.
    inline u4 count() { return _count; }

    // Return the ith entry from the table.
    inline ImageFileReader* get(u4 i) { return _table[i]; }

    // Add a new image entry to the table.
    void add(ImageFileReader* image);

    // Remove an image entry from the table.
    void remove(ImageFileReader* image);

    // Determine if image entry is in table.
    bool contains(ImageFileReader* image);
};

// Manage the image file.
// ImageFileReader manages the content of an image file.
// Initially, the header of the image file is read for validation.  If valid,
// values in the header are used calculate the size of the image index.  The
// index is then memory mapped to allow load on demand and sharing.  The
// -XX:+MemoryMapImage flag determines if the entire file is loaded (server use.)
// An image can be used by Hotspot and multiple reference points in the JDK, thus
// it is desirable to share a reader.    To accomodate sharing, a share table is
// defined (see ImageFileReaderTable in imageFile.cpp)  To track the number of
// uses, ImageFileReader keeps a use count (_use).  Use is incremented when
// 'opened' by reference point and decremented when 'closed'.    Use of zero
// leads the ImageFileReader to be actually closed and discarded.
class ImageFileReader {
friend class ImageFileReaderTable;
private:
    // Manage a number of image files such that an image can be shared across
    // multiple uses (ex. loader.)
    static ImageFileReaderTable _reader_table;

    // true if image should be fully memory mapped.
    static bool memory_map_image;

    char* _name;         // Name of image
    s4 _use;             // Use count
    int _fd;             // File descriptor
    Endian* _endian;     // Endian handler
    u8 _file_size;       // File size in bytes
    ImageHeader _header; // Image header
    size_t _index_size;  // Total size of index
    u1* _index_data;     // Raw index data
    s4* _redirect_table; // Perfect hash redirect table
    u4* _offsets_table;  // Location offset table
    u1* _location_bytes; // Location attributes
    u1* _string_bytes;   // String table
    ImageModuleData *_module_data;       // The ImageModuleData for this image

    ImageFileReader(const char* name, bool big_endian);
    ~ImageFileReader();

    // Compute number of bytes in image file index.
    inline size_t index_size() {
        return sizeof(ImageHeader) +
            table_length() * sizeof(u4) * 2 + locations_size() + strings_size();
    }

public:
    enum {
        // Image file marker.
        IMAGE_MAGIC = 0xCAFEDADA,
        // Endian inverted Image file marker.
        IMAGE_MAGIC_INVERT = 0xDADAFECA,
        // Image file major version number.
        MAJOR_VERSION = 1,
        // Image file minor version number.
        MINOR_VERSION = 0
    };

    // Locate an image if file already open.
    static ImageFileReader* find_image(const char* name);

    // Open an image file, reuse structure if file already open.
    static ImageFileReader* open(const char* name, bool big_endian = Endian::is_big_endian());

    // Close an image file if the file is not in use elsewhere.
    static void close(ImageFileReader *reader);

    // Return an id for the specifed ImageFileReader.
    static u8 reader_to_ID(ImageFileReader *reader);

    // Validate the image id.
    static bool id_check(u8 id);

    // Return an id for the specifed ImageFileReader.
    static ImageFileReader* id_to_reader(u8 id);

    // Open image file for read access.
    bool open();

    // Close image file.
    void close();

    // Read directly from the file.
    bool read_at(u1* data, u8 size, u8 offset) const;

    inline Endian* endian() const { return _endian; }

    // Retrieve name of image file.
    inline const char* name() const {
        return _name;
    }

    // Retrieve size of image file.
    inline u8 file_size() const {
        return _file_size;
    }

    // Retrieve the size of the mapped image.
    inline u8 map_size() const {
        return (u8)(memory_map_image ? _file_size : _index_size);
    }

    // Return first address of index data.
    inline u1* get_index_address() const {
        return _index_data;
    }

    // Return first address of resource data.
    inline u1* get_data_address() const {
        return _index_data + _index_size;
    }

    // Get the size of the index data.
    size_t get_index_size() const {
        return _index_size;
    }

    inline u4 table_length() const {
        return _header.table_length(_endian);
    }

    inline u4 locations_size() const {
        return _header.locations_size(_endian);
    }

    inline u4 strings_size()const    {
        return _header.strings_size(_endian);
    }

    inline u4* offsets_table() const {
        return _offsets_table;
    }

    // Increment use count.
    inline void inc_use() {
        _use++;
    }

    // Decrement use count.
    inline bool dec_use() {
        return --_use == 0;
    }

    // Return a string table accessor.
    inline const ImageStrings get_strings() const {
        return ImageStrings(_string_bytes, _header.strings_size(_endian));
    }

    // Return location attribute stream at offset.
    inline u1* get_location_offset_data(u4 offset) const {
        assert((u4)offset < _header.locations_size(_endian) &&
                            "offset exceeds location attributes size");
        return offset != 0 ? _location_bytes + offset : NULL;
    }

    // Return location attribute stream for location i.
    inline u1* get_location_data(u4 index) const {
        return get_location_offset_data(get_location_offset(index));
    }

    // Return the location offset for index.
    inline u4 get_location_offset(u4 index) const {
        assert((u4)index < _header.table_length(_endian) &&
                            "index exceeds location count");
        return _endian->get(_offsets_table[index]);
    }

    // Find the location attributes associated with the path.    Returns true if
    // the location is found, false otherwise.
    bool find_location(const char* path, ImageLocation& location) const;

    // Find the location index and size associated with the path.
    // Returns the location index and size if the location is found,
    // ImageFileReader::NOT_FOUND otherwise.
    u4 find_location_index(const char* path, u8 *size) const;

    // Verify that a found location matches the supplied path.
    bool verify_location(ImageLocation& location, const char* path) const;

    // Return the resource for the supplied location index.
    void get_resource(u4 index, u1* uncompressed_data) const;

    // Return the resource for the supplied path.
    void get_resource(ImageLocation& location, u1* uncompressed_data) const;

    // Return the ImageModuleData for this image
    ImageModuleData * get_image_module_data();

};
#endif // LIBJIMAGE_IMAGEFILE_HPP
