# Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
#
# SPDX-License-Identifier: BSD-2-Clause

import gdb
import gdb.types
import re


def handler_class_for_type(type, re=re.compile('^([^<]+)(<.*>)?$')):
    typename = str(type.tag)

    match = re.match(typename)
    if not match:
        return None

    klass = match.group(1)

    if klass == 'YAK::Atomic':
        return YAKAtomic
    elif klass == 'YAK::DistinctNumeric':
        return YAKDistinctNumeric
    elif klass == 'YAK::HashMap':
        return YAKHashMapPrettyPrinter
    elif klass == 'YAK::RefCounted':
        return YAKRefCounted
    elif klass == 'YAK::RefPtr':
        return YAKRefPtr
    elif klass == 'YAK::OwnPtr':
        return YAKOwnPtr
    elif klass == 'YAK::NonnullRefPtr':
        return YAKRefPtr
    elif klass == 'YAK::SinglyLinkedList':
        return YAKSinglyLinkedList
    elif klass == 'YAK::String':
        return YAKString
    elif klass == 'YAK::StringView':
        return YAKStringView
    elif klass == 'YAK::StringImpl':
        return YAKStringImpl
    elif klass == 'YAK::Variant':
        return YAKVariant
    elif klass == 'YAK::Vector':
        return YAKVector
    elif klass == 'VirtualAddress':
        return VirtualAddress
    else:
        return UnhandledType


class UnhandledType:
    @classmethod
    def prettyprint_type(cls, type):
        return type.name


class YAKAtomic:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_value"]

    @classmethod
    def prettyprint_type(cls, type):
        contained_type = type.template_argument(0)
        return f'YAK::Atomic<{handler_class_for_type(contained_type).prettyprint_type(contained_type)}>'


class YAKDistinctNumeric:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_value"]

    @classmethod
    def prettyprint_type(cls, type):
        actual_name = type.template_argument(1)
        parts = actual_name.name.split("::")
        unqualified_name = re.sub(r'__(\w+)_tag', r'\1', actual_name.name)
        if unqualified_name != actual_name.name:
            qualified_name = '::'.join(parts[:-2] + [unqualified_name])
            return qualified_name
        # If the tag is malformed, just print DistinctNumeric<T>
        contained_type = type.template_argument(0)
        return f'YAK::DistinctNumeric<{handler_class_for_type(contained_type).prettyprint_type(contained_type)}>'


class YAKRefCounted:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_ref_count"]

    @classmethod
    def prettyprint_type(cls, type):
        contained_type = type.template_argument(0)
        return f'YAK::RefCounted<{handler_class_for_type(contained_type).prettyprint_type(contained_type)}>'


class YAKString:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        if int(self.val["m_impl"]["m_bits"]["m_value"]) == 0:
            return '""'
        else:
            impl = YAKRefPtr(self.val["m_impl"]).get_pointee().dereference()
            return YAKStringImpl(impl).to_string()

    @classmethod
    def prettyprint_type(cls, type):
        return 'YAK::String'


class YAKStringView:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        if int(self.val["m_length"]) == 0:
            return '""'
        else:
            characters = self.val["m_characters"]
            str_type = characters.type.target().array(self.val["m_length"]).pointer()
            return str(characters.cast(str_type).dereference())

    @classmethod
    def prettyprint_type(cls, type):
        return 'YAK::StringView'


def get_field_unalloced(val, member, type):
    # Trying to access a variable-length field seems to fail with
    # Python Exception <class 'gdb.error'> value requires 4294967296 bytes, which is more than max-value-size
    # This works around that issue.
    return gdb.parse_and_eval(f"*({type}*)(({val.type.name}*){int(val.address)})->{member}")


class YAKStringImpl:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        if int(self.val["m_length"]) == 0:
            return '""'
        else:
            str_type = gdb.lookup_type("char").array(self.val["m_length"])
            return get_field_unalloced(self.val, "m_inline_buffer", str_type)

    @classmethod
    def prettyprint_type(cls, type):
        return 'YAK::StringImpl'


class YAKOwnPtr:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return YAKOwnPtr.prettyprint_type(self.val.type)

    def children(self):
        return [('*', self.val["m_ptr"])]

    @classmethod
    def prettyprint_type(cls, type):
        contained_type = type.template_argument(0)
        return f'YAK::OwnPtr<{handler_class_for_type(contained_type).prettyprint_type(contained_type)}>'


class YAKRefPtr:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return YAKRefPtr.prettyprint_type(self.val.type)

    def get_pointee(self):
        inner_type = self.val.type.template_argument(0)
        inner_type_ptr = inner_type.pointer()
        return self.val["m_bits"]["m_value"].cast(inner_type_ptr)

    def children(self):
        return [('*', self.get_pointee())]

    @classmethod
    def prettyprint_type(cls, type):
        contained_type = type.template_argument(0)
        return f'YAK::RefPtr<{handler_class_for_type(contained_type).prettyprint_type(contained_type)}>'


class YAKVariant:
    def __init__(self, val):
        self.val = val
        self.index = int(self.val["m_index"])
        self.contained_types = self.resolve_types(self.val.type)

    def to_string(self):
        return YAKVariant.prettyprint_type(self.val.type)

    def children(self):
        data = self.val["m_data"]
        ty = self.contained_types[self.index]
        return [(ty.name, data.cast(ty.pointer()).referenced_value())]

    @classmethod
    def resolve_types(cls, ty):
        contained_types = []
        type_resolved = ty.strip_typedefs()
        index = 0
        while True:
            try:
                arg = type_resolved.template_argument(index)
                index += 1
                contained_types.append(arg)
            except RuntimeError:
                break
        return contained_types

    @classmethod
    def prettyprint_type(cls, ty):
        names = ", ".join(handler_class_for_type(t).prettyprint_type(t) for t in YAKVariant.resolve_types(ty))
        return f'YAK::Variant<{names}>'


class YAKVector:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return f'{YAKVector.prettyprint_type(self.val.type)} of len {int(self.val["m_size"])}'

    def children(self):
        vec_len = int(self.val["m_size"])

        if vec_len == 0:
            return []

        outline_buf = self.val["m_outline_buffer"]

        inner_type_ptr = self.val.type.template_argument(0).pointer()

        if int(outline_buf) != 0:
            elements = outline_buf.cast(inner_type_ptr)
        else:
            elements = get_field_unalloced(self.val, "m_inline_buffer_storage", inner_type_ptr)

        return [(f"[{i}]", elements[i]) for i in range(vec_len)]

    @classmethod
    def prettyprint_type(cls, type):
        template_type = type.template_argument(0)
        return f'YAK::Vector<{handler_class_for_type(template_type).prettyprint_type(template_type)}>'


class YAKHashMapPrettyPrinter:
    def __init__(self, val):
        self.val = val

    @staticmethod
    def _iter_hashtable(val, cb):
        entry_type_ptr = val.type.template_argument(0).pointer()
        buckets = val["m_buckets"]
        for i in range(0, val["m_capacity"]):
            bucket = buckets[i]
            if bucket["used"]:
                cb(bucket["storage"].cast(entry_type_ptr))

    @staticmethod
    def _iter_hashmap(val, cb):
        table = val["m_table"]
        YAKHashMapPrettyPrinter._iter_hashtable(table, lambda entry: cb(entry["key"], entry["value"]))

    def to_string(self):
        return YAKHashMapPrettyPrinter.prettyprint_type(self.val.type)

    def children(self):
        elements = []

        def cb(key, value):
            nonlocal elements
            elements.append((f"[{key}]", value))

        YAKHashMapPrettyPrinter._iter_hashmap(self.val, cb)
        return elements

    @classmethod
    def prettyprint_type(cls, type):
        template_types = list(type.template_argument(i) for i in (0, 1))
        key, value = list(handler_class_for_type(t).prettyprint_type(t) for t in template_types)
        return f'YAK::HashMap<{key}, {value}>'


class YAKSinglyLinkedList:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return YAKSinglyLinkedList.prettyprint_type(self.val.type)

    def children(self):
        elements = []

        node = self.val["m_head"]
        while node != 0:
            elements.append(node["value"])
            node = node["next"]

        return [(f"[{i}]", elements[i]) for i in range(len(elements))]

    @classmethod
    def prettyprint_type(cls, type):
        template_type = type.template_argument(0)
        return f'YAK::SinglyLinkedList<{handler_class_for_type(template_type).prettyprint_type(template_type)}>'


class VirtualAddress:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_address"]

    @classmethod
    def prettyprint_type(cls, type):
        return 'VirtualAddress'


class SerenityPrettyPrinterLocator(gdb.printing.PrettyPrinter):
    def __init__(self):
        super(SerenityPrettyPrinterLocator, self).__init__("serenity_pretty_printers", [])

    def __call__(self, val):
        type = gdb.types.get_basic_type(val.type)
        handler = handler_class_for_type(type)
        if handler is UnhandledType:
            return None
        return handler(val)


gdb.printing.register_pretty_printer(None, SerenityPrettyPrinterLocator(), replace=True)


class FindThreadCmd(gdb.Command):
    """
    Find SerenityOS thread for the specified TID.
    find_thread TID
    """

    def __init__(self):
        super(FindThreadCmd, self).__init__(
            "find_thread", gdb.COMMAND_USER
        )

    def _find_thread(self, tid):
        threads = gdb.parse_and_eval("Kernel::Thread::g_tid_map")
        thread = None

        def cb(key, value):
            nonlocal thread
            if int(key["m_value"]) == tid:
                thread = value

        YAKHashMapPrettyPrinter._iter_hashmap(threads, cb)
        return thread

    def complete(self, text, word):
        return gdb.COMPLETE_SYMBOL

    def invoke(self, args, from_tty):
        argv = gdb.string_to_argv(args)
        if len(argv) == 0:
            gdb.write("Argument required (TID).\n")
            return
        tid = int(argv[0])
        thread = self._find_thread(tid)
        if not thread:
            gdb.write(f"No thread with TID {tid} found.\n")
        else:
            gdb.write(f"{thread}\n")


FindThreadCmd()
