#include "Class.h"

#include <AK/Assertions.h>
#include <AK/FixedArray.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>
#include <LibCore/MappedFile.h>
#include <LibJVM/Attributes.h>
#include <LibJVM/ConstantPool.h>

namespace JVM {

int const MAGIC = 0xCAFEBABE;

//Just some bit helper functions
//These were taken from https://stackoverflow.com/questions/544928/reading-integer-size-bytes-from-a-char-array, although the code isn't that complex.
//The reason for these functions is that the .class file stores information is big-endian format, while the x86 is a little-endian machine.
//This means that before interpreting these values, the bytes need to be swaped.
inline unsigned short swap_16bit(unsigned short us) {
    return (unsigned short)(((us & 0xFF00) >> 8) |
                            ((us & 0x00FF) << 8));
}

inline unsigned int swap_32bit(unsigned int ui) {
    return (unsigned int)(((ui & 0xFF000000) >> 24) |
                           ((ui & 0x00FF0000) >>  8) |
                           ((ui & 0x0000FF00) <<  8) |
                           ((ui & 0x000000FF) << 24));
}

inline long long swap_64bit(long long ul) {
    return (long long)(((ul & 0xFF00000000000000) >> 56) |
                           ((ul & 0x00FF000000000000) >> 40)|
                           ((ul & 0x0000FF0000000000) >> 24)|
                           ((ul & 0x000000FF00000000) >>  8)|
                           ((ul & 0x00000000FF000000) <<  8)|
                           ((ul & 0x0000000000FF0000) << 24)|
                           ((ul & 0x000000000000FF00) << 40)|
                           ((ul & 0x00000000000000FF) << 56));
}
//Some helpers to facilitate easy data access
//If there's a weird bug with data access, it's probably from these functions, as I'm not great about thinking about bounds checking.
inline u8 get_byte(u8* data, size_t &offset, size_t length) {
    if (offset >= length)
        VERIFY_NOT_REACHED();
    return *((u8*)(data + (offset++)));
}


inline short get_short(u8* data, size_t &loc, size_t length) {
        if (loc += 2 > length)
            VERIFY_NOT_REACHED();
        return swap_16bit(*((short*)(data + (loc - 2))));
}

inline int get_int(u8* data, size_t &loc, size_t length) {
        if (loc += 4 > length)
            VERIFY_NOT_REACHED();
        return swap_32bit(*((int*)(data + (loc - 4))));
}

inline long long get_long(u8* data, size_t &loc, size_t length) {
        if (loc += 8 > length)
            VERIFY_NOT_REACHED();
        return swap_64bit(*((long long*)(data + (loc - 8))));
}

bool Class::load_from_file(AK::StringView path, bool check_file){
    auto class_file_result = Core::MappedFile::map(path);
    if (class_file_result.is_error()) {
        dbgln("Error loading file from path!\n");
        return false;
    }
    auto class_file = class_file_result.release_value();
    auto class_data = (u8*)class_file->data();
    size_t size = class_file->size();
    size_t loc = 0;
    int magic = get_int(class_data, loc, size);
    if (magic != MAGIC) {
        dbgln("Error when parsing file: file doesn't contain magic number 0xCAFEBABE as header!\n");
        return false;
    }
    this->m_minor_version = get_short(class_data, loc, size);
    this->m_major_version = get_short(class_data, loc, size);
    short cp_size = get_short(class_data, loc, size) - 1;
    auto result = this->constant_pool.try_create((size_t) cp_size);
    if (result.is_error()) {
        dbgln("Error when attempting to create a constant pool!\n");
        return false;
    }
    auto index = 0;
    while (cp_size > 0 ) {
        ConstantKind kind = static_cast<ConstantKind>(get_u8(class_data, loc, size));
        if (kind == ConstantKind::Class || kind == ConstantKind::String) {
            this->constant_pool[index] = CPEntry(kind,get_short(class_data, loc, size));
        }
        else if (kind == ConstantKind::FieldRef || kind == ConstantKind::MethodRef || kind == ConstantKind::InterfaceMethodRef) {
            short ref_values[2];
            ref_values[0] = get_short(class_data, loc, size);
            ref_values[1] = get_short(class_data, loc, size);
            this->constant_pool[index] = CPEntry(kind, ref_values);
        }
        else if (kind == ConstantKind::Integer || kind == ConstantKind::Float) {
            this->constant_pool[index] = CPEntry(kind, get_int(class_data, loc, size));
        }

        else if (kind == ConstantKind::Long || kind == ConstantKind::Double) {
            long long val = get_long(class_data, loc, size);
            this->constant_pool[index] = CPEntry(kind, val);
            index++;
            cp_size--;
            this->constant_pool[index] = CPEntry(ConstantKind::Unusable); //The oracle spec requires that the index after a long or a double is unused.
        }

        else if (kind == ConstantKind::NameAndType) {
            short NAT_values[2];
            NAT_values[0] = get_short(class_data, loc, size);
            NAT_values[1] = get_short(class_data, loc, size);
            this->constant_pool[index] = CPEntry(ConstantKind::NameAndType, NAT_values);
        }

        else if (kind == ConstantKind::Utf8) {
            short length = get_short(class_data, loc, size);
            this->constant_pool[index] = CPEntry(length, class_data + loc);
            loc += length;
        }

        else if (kind == ConstantKind::MethodHandle) {
            ReferenceKind refkind = static_cast<ReferenceKind>(get_u8(class_data, loc, size));
            short index = get_short(class_data, loc, size);
            MethodHandleInfo MHInfo = {refkind, index};
            this->constant_pool[index] = CPEntry(MHInfo);
        }

        else if (kind == ConstantKind::MethodType) {
            short desc_index = get_short(class_data, loc, size);
            this->constant_pool[index] = CPEntry(ConstantKind::MethodType, desc_index);
        }

        else if (kind == ConstantKind::Dynamic || kind == ConstantKind::InvokeDynamic) {
            short bootstrap_method_attr_index = get_short(class_data, loc, size);
            short name_and_type_index = get_short(class_data, loc, size);
            short dynamic[2] = {bootstrap_method_attr_index, name_and_type_index};
            this->constant_pool[index] = CPEntry(kind, dynamic);
        }

        else if (kind == ConstantKind::Module) {
            short name_index = get_short(class_data, loc, size);
            this->constant_pool[index] = CPEntry(ConstantKind::Module, name_index);
        }

        else if (kind == ConstantKind::Package) {
            short name_index = get_short(class_data, loc, size);
            this->constant_pool[index] = CPEntry(ConstantKind::Package, name_index);
        }

        else {
            dbgln("Error when parsing constant table! Found illegal constant tag {}", (int) kind);
            return false;
        }
        index++;
        cp_size--;


    }
    if (check_file)
        this->verify_const_pool();
    this->access_flags = get_short(class_data, loc, size);
}

}
