/*
 * Copyright (c) 2024, Jamie Mansfield <jmansfield@cadixdev.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/MappedFile.h>
#include <LibCore/System.h>
#include <LibJava/Parser.h>
#include <LibMain/Main.h>

StringView get_class_type(u16 access_flags);
ErrorOr<FlyString> normalise_method_name(FlyString method_name, Java::ClassFile const& class_file);
ErrorOr<FlyString> dump_class_access_flags(u16 access_flags);
ErrorOr<FlyString> dump_field_access_flags(u16 access_flags);
ErrorOr<FlyString> dump_method_access_flags(u16 access_flags);
ErrorOr<FlyString> class_access_flags_to_names(u16 access_flags);
ErrorOr<FlyString> method_access_flags_to_names(u16 access_flags);
ErrorOr<FlyString> resolve_constant_pool_info_value(Java::ConstantPool constant_pool, Java::ConstantPoolInfo info, bool verbose = true);

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));

    Core::ArgsParser args_parser;

    StringView in_path;
    args_parser.add_positional_argument(in_path, "Path to input class file", "FILE");

    bool verbose = false;
    args_parser.add_option(verbose, "Print additional information", "verbose", {});

    args_parser.parse(arguments);

    auto file = TRY(Core::MappedFile::map(in_path));
    auto parser = Java::Parser(file->bytes());
    auto class_file = TRY(parser.parse_class_file());

    auto resolve_cp_value = [class_file](Java::ConstantPoolInfo info, bool verbose = true) {
        return resolve_constant_pool_info_value(class_file.constant_pool, info, verbose);
    };

    auto class_name = TRY(resolve_cp_value(class_file.constant_pool.get(class_file.this_class).value(), false));

    if (verbose) {
        dbgln("{} {} {}", TRY(class_access_flags_to_names(class_file.access_flags)), get_class_type(class_file.access_flags), class_name);
        dbgln("  minor version: {}", class_file.minor_version);
        dbgln("  major version: {}", class_file.major_version);
        dbgln("  flags: {}", TRY(dump_class_access_flags(class_file.access_flags)));
        dbgln("  this_class: #{}    // {}", class_file.this_class, class_name);
        dbgln("  super_class: #{}   // {}", class_file.super_class, TRY(resolve_cp_value(class_file.constant_pool.get(class_file.super_class).value(), false)));
        dbgln("  interfaces: {}, fields: {}, methods: {}, attributes: {}",
            class_file.interfaces.size(), class_file.fields.size(), class_file.methods.size(), class_file.attributes.size());

        dbgln("Constant pool:");
        for (u16 i = 0; i < class_file.constant_pool.size(); i++) {
            auto info = class_file.constant_pool.get(i + 1);

            if (info->has<Java::ConstantLongInfo>() || info->has<Java::ConstantDoubleInfo>())
                i++;

            dbgln(" #{} = {}    {}", i + 1, Java::constant_pool_info_to_name(info.value()), TRY(resolve_cp_value(info.value())));
        }
    }

    if (verbose) {
        dbgln("{}", "{");
    } else {
        dbgln("{} {} {} {}", TRY(class_access_flags_to_names(class_file.access_flags)), get_class_type(class_file.access_flags), class_name, "{");
    }

    for (size_t i = 0; i < class_file.fields.size(); i++) {
        auto field = class_file.fields.at(i);
        auto field_name = TRY(resolve_cp_value(class_file.constant_pool.get(field.name_index).value()));

        dbgln("  {} {};",
            TRY(method_access_flags_to_names(field.access_flags)),
            TRY(normalise_method_name(field_name, class_file)));

        if (verbose) {
            dbgln("    descriptor: {}", TRY(resolve_cp_value(class_file.constant_pool.get(field.descriptor_index).value())));
            dbgln("    flags: {}", TRY(dump_field_access_flags(field.access_flags)));

            if (i + 1 != class_file.fields.size() || !class_file.methods.is_empty())
                dbgln("");
        }
    }

    for (size_t i = 0; i < class_file.methods.size(); i++) {
        auto method = class_file.methods.at(i);
        auto method_name = TRY(resolve_cp_value(class_file.constant_pool.get(method.name_index).value()));

        auto static_initialiser = method_name == "<clinit>";

        dbgln("  {} {}{};",
            TRY(method_access_flags_to_names(method.access_flags)),
            TRY(normalise_method_name(method_name, class_file)),
            static_initialiser ? "" : "()");

        if (verbose) {
            dbgln("    descriptor: {}", TRY(resolve_cp_value(class_file.constant_pool.get(method.descriptor_index).value())));
            dbgln("    flags: {}", TRY(dump_method_access_flags(method.access_flags)));

            if (i + 1 != class_file.methods.size())
                dbgln("");
        }
    }

    dbgln("{}", "}");

    if (verbose) {
        for (size_t i = 0; i < class_file.attributes.size(); i++) {
            auto attribute = class_file.attributes.at(i);
            auto attribute_name = TRY(resolve_cp_value(class_file.constant_pool.get(attribute.name_index).value()));

            if (attribute_name == "SourceFile") {
                auto x = attribute.info.at(0);
                auto y = attribute.info.at(1);
                u16 value_index = ((u16)x << 8) | (u16)y;

                auto source_file = TRY(resolve_cp_value(class_file.constant_pool.get(value_index).value()));

                dbgln("{}: \"{}\"", attribute_name, source_file);
            } else {
                dbgln("{}: FIXME", TRY(resolve_cp_value(class_file.constant_pool.get(attribute.name_index).value())));
            }
        }
    }

    return 0;
}

StringView get_class_type(u16 access_flags)
{
    if ((access_flags & Java::Acc_Annotation) != 0)
        return "@interface"sv;

    if ((access_flags & Java::Acc_Interface) != 0)
        return "interface"sv;

    if ((access_flags & Java::Acc_Enum) != 0)
        return "enum"sv;

    return "class"sv;
}

ErrorOr<FlyString> normalise_method_name(FlyString method_name, Java::ClassFile const& class_file)
{
    // constructor
    if (method_name == "<init>")
        return resolve_constant_pool_info_value(class_file.constant_pool, class_file.constant_pool.get(class_file.this_class).value(), false);

    // static initialiser
    if (method_name == "<clinit>") {
        StringBuilder builder;
        builder.append("{}"sv);
        return builder.to_fly_string();
    }

    return method_name;
}

ErrorOr<FlyString> dump_class_access_flags(u16 access_flags)
{
    Vector<StringView> flags;

    if ((access_flags & Java::Acc_Public) != 0)
        flags.append("ACC_PUBLIC"sv);
    if ((access_flags & Java::Acc_Final) != 0)
        flags.append("ACC_FINAL"sv);
    if ((access_flags & Java::Acc_Super) != 0)
        flags.append("ACC_SUPER"sv);
    if ((access_flags & Java::Acc_Interface) != 0)
        flags.append("ACC_INTERFACE"sv);
    if ((access_flags & Java::Acc_Abstract) != 0)
        flags.append("ACC_ABSTRACT"sv);
    if ((access_flags & Java::Acc_Synthetic) != 0)
        flags.append("ACC_SYNTHETIC"sv);
    if ((access_flags & Java::Acc_Annotation) != 0)
        flags.append("ACC_ANNOTATION"sv);
    if ((access_flags & Java::Acc_Enum) != 0)
        flags.append("ACC_ENUM"sv);
    if ((access_flags & Java::Acc_Module) != 0)
        flags.append("ACC_MODULE"sv);

    StringBuilder builder;
    builder.appendff("({:#x}) ", access_flags);
    builder.join(", "sv, flags);
    return builder.to_fly_string();
}

ErrorOr<FlyString> dump_field_access_flags(u16 access_flags)
{
    Vector<StringView> flags;

    if ((access_flags & Java::Acc_Public) != 0)
        flags.append("ACC_PUBLIC"sv);
    if ((access_flags & Java::Acc_Private) != 0)
        flags.append("ACC_PRIVATE"sv);
    if ((access_flags & Java::Acc_Protected) != 0)
        flags.append("ACC_PROTECTED"sv);
    if ((access_flags & Java::Acc_Static) != 0)
        flags.append("ACC_STATIC"sv);
    if ((access_flags & Java::Acc_Final) != 0)
        flags.append("ACC_FINAL"sv);
    if ((access_flags & Java::Acc_Volatile) != 0)
        flags.append("ACC_VOLATILE"sv);
    if ((access_flags & Java::Acc_Transient) != 0)
        flags.append("ACC_TRANSIENT"sv);
    if ((access_flags & Java::Acc_Synthetic) != 0)
        flags.append("ACC_SYNTHETIC"sv);
    if ((access_flags & Java::Acc_Enum) != 0)
        flags.append("ACC_ENUM"sv);

    StringBuilder builder;
    builder.appendff("({:#x}) ", access_flags);
    builder.join(", "sv, flags);
    return builder.to_fly_string();
}

ErrorOr<FlyString> dump_method_access_flags(u16 access_flags)
{
    Vector<StringView> flags;

    if ((access_flags & Java::Acc_Public) != 0)
        flags.append("ACC_PUBLIC"sv);
    if ((access_flags & Java::Acc_Private) != 0)
        flags.append("ACC_PRIVATE"sv);
    if ((access_flags & Java::Acc_Protected) != 0)
        flags.append("ACC_PROTECTED"sv);
    if ((access_flags & Java::Acc_Static) != 0)
        flags.append("ACC_STATIC"sv);
    if ((access_flags & Java::Acc_Final) != 0)
        flags.append("ACC_FINAL"sv);
    if ((access_flags & Java::Acc_Synchronised) != 0)
        flags.append("ACC_SYNCHRONIZED"sv);
    if ((access_flags & Java::Acc_Bridge) != 0)
        flags.append("ACC_BRIDGE"sv);
    if ((access_flags & Java::Acc_Varargs) != 0)
        flags.append("ACC_VARARGS"sv);
    if ((access_flags & Java::Acc_Native) != 0)
        flags.append("ACC_NATIVE"sv);
    if ((access_flags & Java::Acc_Abstract) != 0)
        flags.append("ACC_ABSTRACT"sv);
    if ((access_flags & Java::Acc_Strict) != 0)
        flags.append("ACC_STRICT"sv);
    if ((access_flags & Java::Acc_Synthetic) != 0)
        flags.append("ACC_SYNTHETIC"sv);

    StringBuilder builder;
    builder.appendff("({:#x}) ", access_flags);
    builder.join(", "sv, flags);
    return builder.to_fly_string();
}

ErrorOr<FlyString> class_access_flags_to_names(u16 access_flags)
{
    Vector<StringView> flags;

    if ((access_flags & Java::Acc_Public) != 0)
        flags.append("public"sv);
    if ((access_flags & Java::Acc_Final) != 0)
        flags.append("final"sv);
    if ((access_flags & Java::Acc_Abstract) != 0)
        flags.append("abstract"sv);

    StringBuilder builder;
    builder.join(" "sv, flags);
    return builder.to_fly_string();
}

ErrorOr<FlyString> method_access_flags_to_names(u16 access_flags)
{
    Vector<StringView> flags;

    if ((access_flags & Java::Acc_Public) != 0)
        flags.append("public"sv);
    if ((access_flags & Java::Acc_Private) != 0)
        flags.append("private"sv);
    if ((access_flags & Java::Acc_Protected) != 0)
        flags.append("protected"sv);
    if ((access_flags & Java::Acc_Static) != 0)
        flags.append("static"sv);
    if ((access_flags & Java::Acc_Final) != 0)
        flags.append("final"sv);
    if ((access_flags & Java::Acc_Synchronised) != 0)
        flags.append("synchronized"sv);
    if ((access_flags & Java::Acc_Native) != 0)
        flags.append("native"sv);
    if ((access_flags & Java::Acc_Abstract) != 0)
        flags.append("abstract"sv);

    StringBuilder builder;
    builder.join(" "sv, flags);
    return builder.to_fly_string();
}

ErrorOr<FlyString> resolve_constant_pool_info_value(Java::ConstantPool constant_pool, Java::ConstantPoolInfo info, bool verbose)
{
    if (info.has<Java::ConstantClassInfo>()) {
        auto value = info.get<Java::ConstantClassInfo>();

        auto name_info = constant_pool.get(value.name_index);
        auto name_value = TRY(resolve_constant_pool_info_value(constant_pool, name_info.value(), false));

        if (!verbose)
            return name_value;

        StringBuilder builder;
        builder.appendff("#{}   // {}", value.name_index, name_value);
        return builder.to_fly_string();
    }
    if (info.has<Java::ConstantFieldRefInfo>()) {
        auto value = info.get<Java::ConstantFieldRefInfo>();

        auto class_info = constant_pool.get(value.class_index);
        auto class_value = TRY(resolve_constant_pool_info_value(constant_pool, class_info.value(), false));

        auto name_and_type_info = constant_pool.get(value.name_and_type_index);
        auto name_and_type_value = TRY(resolve_constant_pool_info_value(constant_pool, name_and_type_info.value(), false));

        StringBuilder builder;
        builder.appendff("#{}.#{}   // {}.{}", value.class_index, value.name_and_type_index, class_value, name_and_type_value);
        return builder.to_fly_string();
    }
    if (info.has<Java::ConstantMethodRefInfo>()) {
        auto value = info.get<Java::ConstantMethodRefInfo>();

        auto class_info = constant_pool.get(value.class_index);
        auto class_value = TRY(resolve_constant_pool_info_value(constant_pool, class_info.value(), false));

        auto name_and_type_info = constant_pool.get(value.name_and_type_index);
        auto name_and_type_value = TRY(resolve_constant_pool_info_value(constant_pool, name_and_type_info.value(), false));

        StringBuilder builder;
        builder.appendff("#{}.#{}   // {}.{}", value.class_index, value.name_and_type_index, class_value, name_and_type_value);
        return builder.to_fly_string();
    }
    if (info.has<Java::ConstantInterfaceMethodRefInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantStringInfo>()) {
        auto value = info.get<Java::ConstantStringInfo>();

        auto name_info = constant_pool.get(value.string_index);
        auto name_value = TRY(resolve_constant_pool_info_value(constant_pool, name_info.value(), false));

        if (!verbose)
            return name_value;

        StringBuilder builder;
        builder.appendff("#{}   // {}", value.string_index, name_value);
        return builder.to_fly_string();
    }
    if (info.has<Java::ConstantIntegerInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantFloatInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantLongInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantDoubleInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantNameAndTypeInfo>()) {
        auto value = info.get<Java::ConstantNameAndTypeInfo>();

        auto name_info = constant_pool.get(value.name_index);
        auto name_value = TRY(resolve_constant_pool_info_value(constant_pool, name_info.value(), false));

        auto descriptor_info = constant_pool.get(value.descriptor_index);
        auto descriptor_value = TRY(resolve_constant_pool_info_value(constant_pool, descriptor_info.value(), false));

        if (!verbose) {
            StringBuilder builder;
            builder.appendff("{}:{}", name_value, descriptor_value);
            return builder.to_fly_string();
        }

        StringBuilder builder;
        builder.appendff("#{}.#{}   // {}:{}", value.name_index, value.descriptor_index, name_value, descriptor_value);
        return builder.to_fly_string();
    }
    if (info.has<Java::ConstantUtf8Info>()) {
        auto value = info.get<Java::ConstantUtf8Info>();
        return value.value;
    }
    if (info.has<Java::ConstantMethodHandleInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantMethodTypeInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantDynamicInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantInvokeDynamicInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantModuleInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }
    if (info.has<Java::ConstantPackageInfo>()) {
        return FlyString::from_utf8("FIXME"sv);
    }

    VERIFY_NOT_REACHED();
}
