#include "Class.h"

#include <AK/Assertions.h>
#include <AK/FixedArray.h>
#include <AK/StringView.h>
#include <AK/Utf8View.h>
#include <LibCore/MappedFile.h>
#include <LibJVM/Attributes.h>
#include <LibJVM/ConstantPool.h>

namespace JVM {
//FIXME: replace all 'T's with 'unsigned T's.


unsigned short const SUPPORTED_MAJOR_MAX = 61;


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


inline unsigned short get_short(u8* data, size_t &loc, size_t length) {
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

inline Annotation parse_annotation(u8* data, size_t &loc, size_t length); //Forward delcaration

inline ElementValue parse_evalue(u8* data, size_t &loc, size_t length) {
    ElementValue value;
    short index = get_short(data, loc, length);
    u8 tag = get_byte(data, loc, length);
    value.tag = tag;
    if ((tag > 65 && tag < 91) || tag == 115) { //Ascii codes.
        value.value.const_value_index = get_short(data, loc, length);
    }
    else if (tag == 'e') {
        value.value.enum_const_value = {get_short(data, loc, length), get_short(data, loc, length) };
    }
    else if (tag == 'c') {
        value.value.class_info_index = get_short(data, loc, length);
    }
    else if (tag == '@') {
        Annotation new_annot = parse_annotation(data, loc, length);
        value.value.annotation_value = make<Annotation>(new_annot);
        //We should confirm that we still hold on to the value of new_annot after leaving this function.
    }
    else if (tag == '[') {
        short num_vals = get_short(data, loc, length);
        value.value.array_value->try_create((size_t) num_vals);
        for (int i = 0; i < num_vals; i++) {
            ElementValue result = parse_evalue(data, loc, length);
            (*value.value.array_value)[i] = result;
        }
    }
}

inline Annotation parse_annotation(u8* data, size_t &loc, size_t length) {
        short type_index = get_short(data, loc, length);
        short num_pairs = get_short(data, loc, length);
        Annotation annotation;
        annotation.type_index = type_index;
        annotation.element_value_pairs.try_create((size_t) num_pairs);
        for (int i = 0; i < num_pairs; i++) {
            short index = get_short(data, loc, length);
            ElementValue value = parse_evalue(data, loc, length);
            ElementValuePair result = {index, value};
            annotation.element_value_pairs[i] = result;
        }
}

inline VerificationType parse_type(u8* data, size_t &loc, size_t length) {
    VerificationKind kind = VerificationKind(get_byte(data, loc, length));
    if(int(kind) < 7) {
        return VerificationType(kind);
    }
    else if(int(kind) < 9) {
        return VerificationType(kind, get_short(data, loc, length));
    }
    else { VERIFY_NOT_REACHED(); }
}

bool Class::load_from_file(AK::StringView path, bool check_file){
    //FIXME: Add more verifications! Also, we should be consistent in when we choose to debug and return false and when we choose to warn and crash.
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
    if(this->m_major_version > SUPPORTED_MAJOR_MAX) {
        dbgln("Error when parsing file: file version is too advanced! Latest supported major: {}", SUPPORTED_MAJOR_MAX);
        return false;
    }
    unsigned short cp_size = get_short(class_data, loc, size) - 1;
    auto result = this->m_constant_pool.try_create((size_t) cp_size);
    if (result.is_error()) {
        dbgln("Error when attempting to create a constant pool!\n");
        return false;
    }
    auto index = 0;
    while (cp_size > 0 ) {
        ConstantKind kind = static_cast<ConstantKind>(get_u8(class_data, loc, size));
        if (kind == ConstantKind::Class || kind == ConstantKind::String) {
            this->m_constant_pool[index] = CPEntry(kind,get_short(class_data, loc, size));
        }
        else if (kind == ConstantKind::FieldRef || kind == ConstantKind::MethodRef || kind == ConstantKind::InterfaceMethodRef) {
            unsigned short ref_values[2];
            ref_values[0] = get_short(class_data, loc, size);
            ref_values[1] = get_short(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(kind, ref_values);
        }
        else if (kind == ConstantKind::Integer || kind == ConstantKind::Float) {
            this->m_constant_pool[index] = CPEntry(kind, get_int(class_data, loc, size));
        }

        else if (kind == ConstantKind::Long || kind == ConstantKind::Double) {
            long long val = get_long(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(kind, val);
            index++;
            cp_size--;
            this->m_constant_pool[index] = CPEntry(ConstantKind::Unusable); //The oracle spec requires that the index after a long or a double is unused.
        }

        else if (kind == ConstantKind::NameAndType) {
            unsigned short NAT_values[2];
            NAT_values[0] = get_short(class_data, loc, size);
            NAT_values[1] = get_short(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(ConstantKind::NameAndType, NAT_values);
        }

        else if (kind == ConstantKind::Utf8) {
            unsigned short length = get_short(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(length, class_data + loc);
            loc += length;
        }

        else if (kind == ConstantKind::MethodHandle) {
            ReferenceKind refkind = static_cast<ReferenceKind>(get_u8(class_data, loc, size));
            unsigned short index = get_short(class_data, loc, size);
            MethodHandleInfo MHInfo = {refkind, index};
            this->m_constant_pool[index] = CPEntry(MHInfo);
        }

        else if (kind == ConstantKind::MethodType) {
            unsigned short desc_index = get_short(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(ConstantKind::MethodType, desc_index);
        }

        else if (kind == ConstantKind::Dynamic || kind == ConstantKind::InvokeDynamic) {
            unsigned short bootstrap_method_attr_index = get_short(class_data, loc, size);
            unsigned short name_and_type_index = get_short(class_data, loc, size);
            unsigned short dynamic[2] = {bootstrap_method_attr_index, name_and_type_index};
            this->m_constant_pool[index] = CPEntry(kind, dynamic);
        }

        else if (kind == ConstantKind::Module) {
            unsigned short name_index = get_short(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(ConstantKind::Module, name_index);
        }

        else if (kind == ConstantKind::Package) {
            unsigned short name_index = get_short(class_data, loc, size);
            this->m_constant_pool[index] = CPEntry(ConstantKind::Package, name_index);
        }

        else {
            warnln("Error when parsing constant table! Found illegal constant tag {}", (int) kind);
            return false;
        }
        index++;
        cp_size--;


    }
    if (check_file)
        this->verify_const_pool();
    this->access_flags = get_short(class_data, loc, size);
    this->m_this_class_index = get_short(class_data, loc, size);
    this->m_super_class_index = get_short(class_data, loc, size);
    short num_interfaces = get_short(class_data, loc, size);
    this->m_interfaces.try_create((size_t) num_interfaces);
    for(short i = 0; i < num_interfaces; i++) {
        this->m_interfaces[i] = get_short(class_data, loc, size);
        if(check_file) {
            CPEntry entry = this->cp_entry(i);
            if (entry.kind() != ConstantKind::Class)
                warnln("Error when reading interfaces section of .class file: Interface does not point to a class structure!");
                VERIFY_NOT_REACHED();
            //FIXME: Add additional verifications as per the Oracle spec;
        }
    }
    //FIXME: Some attributes should only appear once in a field, and/or are required to appear. We should add confirmations for this.
    short num_fields = get_short(class_data, loc, size);
    this->m_fields.try_create((size_t) num_fields);
    for(short field_index = 0; field_index < num_fields; field_index++) {
        short acc_flags = get_short(class_data, loc, size);
        short name_ind = get_short(class_data, loc, size);
        short desc_ind = get_short(class_data, loc, size);
        short attrib_count = get_short(class_data, loc, size);
        FieldInfo field = {acc_flags, name_ind, desc_ind, AK::FixedArray<AttributeInfo>()};
        auto result = field.attributes.try_create((size_t) attrib_count);
        if (result.is_error()) {
            warnln("Error when attempting to create a field list!\n");
            return false;
        }
        for(short attrib_index = 0; attrib_index < attrib_count; attrib_index++) {
            short name_ind =  get_short(class_data, loc, size);
            CPEntry entry = this->cp_entry(name_ind);
            if (entry.kind() != ConstantKind::Utf8)
                warnln("Error when reading field section of .class file: Field attribute index is not Utf8!\n");
                VERIFY_NOT_REACHED();
            Utf8Info name = entry.as_utf8_info();
            if(StringView(name.bytes, name.length) == "ConstantValue"sv ) {
                ConstantValue cv = ConstantValue();
                int length = get_int(class_data, loc, size);
                if(check_file && length != 2) { //This should short-circuit in the case that we aren't verifying the file
                    warnln("Error when reading field section of .class file: Field ConstantValue attribute has length other than 2!\n");
                    VERIFY_NOT_REACHED();
                }
                cv.constant_value_index = get_short(class_data, loc, size);
                field.attributes[attrib_index] = AttributeInfo(cv);
            }
            else if(StringView(name.bytes, name.length) == "Synthetic"sv) {
                field.attributes[attrib_index] = AttributeInfo(AttributeKind::Synthetic);
                int length = get_int(class_data, loc, size);
            }
            else if (StringView(name.bytes, name.length) == "Deprecated"sv) {
                field.attributes[attrib_index] = AttributeInfo(AttributeKind::Deprecated);
                int length = get_int(class_data, loc, size);
            }
            else if (StringView(name.bytes, name.length) == "Signature"sv) {
                int length = get_int(class_data, loc, size);
                short sig_index = get_short(class_data, loc, size);
                Signature sig;
                sig.sig_index = sig_index;
                field.attributes[attrib_index] = AttributeInfo(sig);
            }
            else if (StringView(name.bytes, name.length) == "RuntimeVisibleAnnotations"sv) {
                int length = get_int(class_data, loc, size);
                short num_annotations = get_short(class_data, loc, size);
                RuntimeVisibleAnnotations rt_vis_annotations;
                rt_vis_annotations.annotations.try_create((size_t) num_annotations);
                for (int i = 0; i < num_annotations; i++) {
                    rt_vis_annotations.annotations[i] = parse_annotation(class_data, loc, size);
                }
                field.attributes[attrib_index] = AttributeInfo(rt_vis_annotations);

            }
            else if (StringView(name.bytes, name.length) == "RuntimeInvisibleAnnotations"sv) {
                int length = get_int(class_data, loc, size);
                short num_annotations = get_short(class_data, loc, size);
                RuntimeInvisibleAnnotations rt_invis_annotations;
                rt_invis_annotations.annotations.must_create_but_fixme_should_propagate_errors((size_t) num_annotations);
                for (int i = 0; i < num_annotations; i++) {
                    rt_invis_annotations.annotations[i] = parse_annotation(class_data, loc, size);
                }
                field.attributes[attrib_index] = AttributeInfo(rt_invis_annotations);

            }

            else if (StringView(name.bytes, name.length) == "RuntimeVisibleTypeAnnotations"sv) {
                int length = get_int(class_data, loc, size);
                short num_annotations = get_short(class_data, loc, size);
                RuntimeVisibleTypeAnnotations rt_vis_type_annotations;
                rt_vis_type_annotations.annotations.must_create_but_fixme_should_propagate_errors((size_t) num_annotations);
                for (int i = 0; i < num_annotations; i++) {
                    rt_vis_type_annotations.annotations[i].target_type = get_byte(class_data. loc, size);
                    if(rt_vis_type_annotations.annotations[i].target_type == 0x13) {
                        //The target is empty, so we just pass
                    }
                    else {
                        //Since we're in a field_info, the value for this type can only legally be 0x13 (see table 4.7.20-C).
                        //If it's not that, throw an error
                        warnln("Error: Target type of a RuntimeVisibleTypeAnnotation in a field_info struct is not 0x13!");
                        return false;
                    }
                    u8 path_length = get_byte(class_data. loc, size);
                    rt_vis_type_annotations.annotations[i].target_path.path_length = path_length;
                    rt_vis_type_annotations.annotations[i].target_path.path.try_create((size_t) path_length);
                    for(int i = 0; i < path_length; i++) {
                        rt_vis_type_annotations.annotations[i].target_path.path[i] = {get_byte(class_data. loc, size), get_byte(class_data. loc, size)};
                    }
                    short num_pairs = get_short(class_data, loc, size);
                    rt_vis_type_annotations.annotations[i].element_value_pairs.must_create_but_fixme_should_propagate_errors((size_t) num_pairs);
                    for(int i = 0; i < num_pairs; i++) {
                        rt_vis_type_annotations.annotations[i].element_value_pairs[i].element_name_index = get_short(class_data, loc, size);
                        rt_vis_type_annotations.annotations[i].element_value_pairs[i].value = parse_evalue(class_data, loc, size);
                    }
                }
                field.attributes[attrib_index] = AttributeInfo(rt_vis_type_annotations);

            }
            else if (StringView(name.bytes, name.length) == "RuntimeInvisibleTypeAnnotations"sv) {
                int length = get_int(class_data, loc, size);
                short num_annotations = get_short(class_data, loc, size);
                RuntimeInvisibleTypeAnnotations rt_invis_type_annotations;
                rt_invis_type_annotations.annotations.must_create_but_fixme_should_propagate_errors((size_t) num_annotations);
                for (int i = 0; i < num_annotations; i++) {
                    rt_invis_type_annotations.annotations[i].target_type = get_byte(class_data. loc, size);
                    if(rt_invis_type_annotations.annotations[i].target_type == 0x13) {
                        //The target is empty, so we just pass
                    }
                    else {
                        //Since we're in a field_info, the value for this type can only legally be 0x13 (see table 4.7.20-C).
                        //If it's not that, throw an error
                        warnln("Error: Target type of a RuntimeVisibleTypeAnnotation in a field_info struct is not 0x13!");
                        return false;
                    }
                    u8 path_length = get_byte(class_data. loc, size);
                    rt_invis_type_annotations.annotations[i].target_path.path_length = path_length;
                    rt_invis_type_annotations.annotations[i].target_path.path.must_create_but_fixme_should_propagate_errors((size_t) path_length);
                    for(int i = 0; i < path_length; i++) {
                        rt_invis_type_annotations.annotations[i].target_path.path[i] = {get_byte(class_data. loc, size), get_byte(class_data. loc, size)};
                    }
                    short num_pairs = get_short(class_data, loc, size);
                    rt_invis_type_annotations.annotations[i].element_value_pairs.must_create_but_fixme_should_propagate_errors((size_t) num_pairs);
                    for(int i = 0; i < num_pairs; i++) {
                        rt_invis_type_annotations.annotations[i].element_value_pairs[i].element_name_index = get_short(class_data, loc, size);
                        rt_invis_type_annotations.annotations[i].element_value_pairs[i].value = parse_evalue(class_data, loc, size);
                    }
                }
                field.attributes[attrib_index] = AttributeInfo(rt_invis_type_annotations);

            }
            else {
                Custom custom;
                custom.name_index = get_short(class_data, loc, size);
                unsigned int cus_len = get_int(class_data, loc, size);
                loc += cus_len; //Skip the data. We can't use it, so it doesn't matter.
                field.attributes[attrib_index] = AttributeInfo(custom);
            }

        }
    }
    short num_methods = get_short(class_data, loc, size);
    for(short method_index = 0; method_index < num_methods; method_index++) {
        MethodInfo method;
        method.access_flags = get_short(class_data, loc, size);
        method.name_index = get_short(class_data, loc, size);
        method.descriptor_index = get_short(class_data, loc, size);
        short attributes_count = get_short(class_data, loc, size);
        method.attributes.must_create_but_fixme_should_propagate_errors(attributes_count);
        for(short attrib_index = 0; attrib_index < attributes_count; attrib_index++) {
            short attrib_name_ind = get_short(class_data, loc, size);
            CPEntry name_entry = this->cp_entry(attrib_name_ind);
            Utf8Info name = name_entry.as_utf8_info();
            unsigned int length = get_int(class_data, loc, size);
            if(StringView(name.bytes, name.length) == "Exceptions"sv) {
               short num_exceptions = get_short(class_data, loc, size);
               ExceptionTable exception;
               exception.exception_index_table.must_create_but_fixme_should_propagate_errors((size_t) num_exceptions);
               for(short exception_index = 0; exception_index < num_exceptions; exception_index++) {
                   exception.exception_index_table[exception_index] = get_short(class_data, loc, size);
               }
               method.attributes[attrib_index] = AttributeInfo(exception);
            }
            else if(StringView(name.bytes, name.length) == "RuntimeVisibleParameterAnnotations"sv) {
                u8 num_params = get_byte(class_data, loc, size);
                RuntimeVisibleParameterAnnotations param_annos;
                param_annos.parameter_annotations.must_create_but_fixme_should_propagate_errors(num_params);
                for(u8 param_index = 0; param_index < num_params; param_index++) {
                    short num_annos = get_short(class_data, loc, size);
                    param_annos.parameter_annotations[param_index].annotations.must_create_but_fixme_should_propagate_errors(num_annos);
                    for (short i = 0; i < num_annos; i++) {
                        param_annos.parameter_annotations[param_index].annotations[i] = parse_annotation(class_data, loc, size);
                    }
                }
                method.attributes[attrib_index] = AttributeInfo(param_annos);
            }
            else if(StringView(name.bytes, name.length) == "RuntimeInvisibleParameterAnnotations"sv) {
                u8 num_params = get_byte(class_data, loc, size);
                RuntimeInvisibleParameterAnnotations param_annos;
                param_annos.parameter_annotations.must_create_but_fixme_should_propagate_errors(num_params);
                for(u8 param_index = 0; param_index < num_params; param_index++) {
                    short num_annos = get_short(class_data, loc, size);
                    param_annos.parameter_annotations[param_index].annotations.must_create_but_fixme_should_propagate_errors(num_annos);
                    for (short i = 0; i < num_annos; i++) {
                        param_annos.parameter_annotations[param_index].annotations[i] = parse_annotation(class_data, loc, size);
                    }
                }
                method.attributes[attrib_index] = AttributeInfo(param_annos);
            }
            else if(StringView(name.bytes, name.length) == "AnnotationDefault") {
               method.attributes[attrib_index] = AttributeInfo(AnnotationDefault(parse_evalue(class_data, loc, size)));
            }
            else if(StringView(name.bytes, name.length) == "MethodParameters") {
                u8 params_count = get_byte(class_data, loc, size);
                MethodParameters method_params;
                method_params.parameters.must_create_but_fixme_should_propagate_errors(params_count);
                for(int i = 0; i < params_count; i++) {
                    method_params.parameters[i] = {get_short(class_data, loc, size), get_short(class_data, loc, size)};
                }
                method.attributes[attrib_index] = AttributeInfo(method_params);
            }
            else if(StringView(name.bytes, name.length) == "Code"sv) {
                Code code;
                code.max_stack = get_short(class_data, loc, size);
                code.max_locals = get_short(class_data, loc, size);
                code.code.must_create_but_fixme_should_propagate_errors(get_int(class_data, loc, size));
                for(int i = 0; i < code.code.size(); i++) {
                    code.code[i] = get_byte(class_data, loc, size);
                }
                code.exception_table.must_create_but_fixme_should_propagate_errors(get_short(class_data, loc, size));
                for(int i = 0; i < code.exception_table.size(); i++) {
                    code.exception_table[i] = {get_short(class_data, loc, size), get_short(class_data, loc, size),
                                               get_short(class_data, loc, size), get_short(class_data, loc, size)};
                }
                code.attributes.must_create_but_fixme_should_propagate_errors(get_short(class_data, loc, size));
                for(int i = 0; i < code.attributes.size(); i++) {
                    CPEntry name_entry = this->cp_entry(get_short(class_data, loc, size));
                    Utf8Info name = name_entry.as_utf8_info();
                    int length = get_int(class_data, loc, size);
                    if(StringView(name.bytes, name.length) == "LineNumberTable"sv) {
                        LineNumberTable table;
                        table.line_number_table.must_create_but_fixme_should_propagate_errors(get_short(class_data, loc, size));
                        for (int j = 0; j < table.line_number_table.size(); j++) {
                            table.line_number_table[j] = {get_short(class_data, loc, size), get_short(class_data, loc, size)};
                        }
                        code.attributes[i] = AttributeInfo(table);
                    }
                    else if(StringView(name.bytes, name.length) == "LocalVariableTable"sv) {
                        LocalVariableTable table;
                        table.local_variable_table.must_create_but_fixme_should_propagate_errors(get_short(class_data, loc, size));
                        for (int j = 0; j < table.local_variable_table.size(); j++) {
                            table.local_variable_table[j] = {get_short(class_data, loc, size), get_short(class_data, loc, size),
                                                            get_short(class_data, loc, size), get_short(class_data, loc, size), get_short(class_data, loc, size)};
                        }
                        code.attributes[i] = AttributeInfo(table);
                    }
                    else if(StringView(name.bytes, name.length) == "LocalVariableTypeTable"sv) {
                        LocalVariableTypeTable table;
                        table.local_variable_type_table.must_create_but_fixme_should_propagate_errors(get_short(class_data, loc, size));
                        for (int j = 0; j < table.local_variable_type_table.size(); j++) {
                            table.local_variable_type_table[j] = {get_short(class_data, loc, size), get_short(class_data, loc, size),
                                                            get_short(class_data, loc, size), get_short(class_data, loc, size), get_short(class_data, loc, size)};
                        }
                        code.attributes[i] = AttributeInfo(table);
                    }
                    else if(StringView(name.bytes, name.length) == "StackMapTable"sv) {
                        StackMapTable table;
                        table.frames.must_create_but_fixme_should_propagate_errors(get_short(class_data, loc, size));
                        for (int j = 0; j < table.frames.size(); j++) {

                            u8 kind = get_short(class_data, loc, size);
                            if(kind < 64) {
                                table.frames[j] = SameFrame(kind);
                            }
                            else if(kind < 128) {
                                table.frames[j] = SameLocals1StackItemFrame(kind, parse_type(class_data, loc, size));
                            }
                            else if(kind == 247) {
                                table.frames[j] = SameLocals1StackItemFrameExtended(kind, get_short(class_data, loc, size), parse_type(class_data, loc, size));
                            }
                            else if(kind > 247 && kind < 251) {
                                table.frames[j] = ChopFrame(kind, get_short(class_data, loc, size));
                            }
                            else if(kind == 251) {
                                table.frames[j] = SameFrameExtended(kind, get_short(class_data, loc, size));
                            }
                            else if(kind < 255) {
                                unsigned short offset_delta = get_short(class_data, loc, size);
                                AK::FixedArray<VerificationType> types = FixedArray<VerificationType>(kind - 251, class_data + loc);
                                table.frames[j] = AppendFrame(kind, offset_delta, types.span());
                            }
                            else if(kind == 255) {
                                unsigned short offset_delta = get_short(class_data, loc, size);
                                unsigned short num_locals = get_short(class_data, loc, size);
                                AK::FixedArray<VerificationType> locals = FixedArray<VerificationType>(num_locals, class_data + loc); //FixedArray() should bounds-check, so this works.
                                unsigned short num_stack_items = get_short(class_data, loc, size);
                                AK::FixedArray<VerificationType> stack = FixedArray<VerificationType>(num_stack_items, class_data + loc);
                                table.frames[j] = FullFrame(kind, offset_delta, locals.span(), stack.span());
                            }
                            else {
                                //error
                                warnln("Error: Attribute StackMapTable has unknown frame type {}", kind);
                                return false;
                            }
                        }
                        code.attributes[i] = AttributeInfo(table);
                    }
                    method.attributes[attrib_index] = AttributeInfo(code);

                }
            }

        }

    }
}

}
