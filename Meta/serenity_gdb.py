# Copyright (c) 2021, Gunnar Beutner <gunnar@beutner.name>
#
# SPDX-License-Identifier: BSD-2-Clause

import gdb
import gdb.types
import re


class AKAtomic:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_value"]


class AKDistinctNumeric:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_value"]


class AKRefCounted:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_ref_count"]


class AKString:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        if int(self.val["m_impl"]["m_bits"]["m_value"]) == 0:
            return '""'
        else:
            impl = AKRefPtr(self.val["m_impl"]).get_pointee().dereference()
            return AKStringImpl(impl).to_string()


class AKStringView:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        if int(self.val["m_length"]) == 0:
            return '""'
        else:
            characters = self.val["m_characters"]
            str_type = characters.type.target().array(self.val["m_length"]).pointer()
            return str(characters.cast(str_type).dereference())


def get_field_unalloced(val, member, type):
    # Trying to access a variable-length field seems to fail with
    # Python Exception <class 'gdb.error'> value requires 4294967296 bytes, which is more than max-value-size
    # This works around that issue.
    return gdb.parse_and_eval(f"*({type}*)(({val.type.name}*){int(val.address)})->{member}")


class AKStringImpl:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        if int(self.val["m_length"]) == 0:
            return '""'
        else:
            str_type = gdb.lookup_type("char").array(self.val["m_length"])
            return get_field_unalloced(self.val, "m_inline_buffer", str_type)


class AKOwnPtr:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val.type.name

    def children(self):
        return [('*', self.val["m_ptr"])]


class AKRefPtr:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val.type.name

    def get_pointee(self):
        inner_type = self.val.type.template_argument(0)
        inner_type_ptr = inner_type.pointer()
        return self.val["m_bits"]["m_value"].cast(inner_type_ptr)

    def children(self):
        return [('*', self.get_pointee())]


class AKVector:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return f'{self.val.type.name} of len {int(self.val["m_size"])}'

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


class AKHashMapPrettyPrinter:
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
        AKHashMapPrettyPrinter._iter_hashtable(table, lambda entry: cb(entry["key"], entry["value"]))

    def to_string(self):
        return self.val.type.name

    def children(self):
        elements = []

        def cb(key, value):
            nonlocal elements
            elements.append((f"[{key}]", value))

        AKHashMapPrettyPrinter._iter_hashmap(self.val, cb)
        return elements


class VirtualAddress:
    def __init__(self, val):
        self.val = val

    def to_string(self):
        return self.val["m_address"]


class SerenityPrettyPrinterLocator(gdb.printing.PrettyPrinter):
    def __init__(self):
        super(SerenityPrettyPrinterLocator, self).__init__("serenity_pretty_printers", [])

        self.re = re.compile('^([^<]+)(<.*>)?$')

    def __call__(self, val):
        type = gdb.types.get_basic_type(val.type)
        typename = str(type.tag)

        match = self.re.match(typename)
        if not match:
            return None

        klass = match.group(1)

        if klass == 'AK::Atomic':
            return AKAtomic(val)
        elif klass == 'AK::DistinctNumeric':
            return AKDistinctNumeric(val)
        elif klass == 'AK::HashMap':
            return AKHashMapPrettyPrinter(val)
        elif klass == 'AK::RefCounted':
            return AKRefCounted(val)
        elif klass == 'AK::RefPtr':
            return AKRefPtr(val)
        elif klass == 'AK::OwnPtr':
            return AKOwnPtr(val)
        elif klass == 'AK::NonnullRefPtr':
            return AKRefPtr(val)
        elif klass == 'AK::String':
            return AKString(val)
        elif klass == 'AK::StringView':
            return AKStringView(val)
        elif klass == 'AK::StringImpl':
            return AKStringImpl(val)
        elif klass == 'AK::Vector':
            return AKVector(val)
        elif klass == 'VirtualAddress':
            return VirtualAddress(val)


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

        AKHashMapPrettyPrinter._iter_hashmap(threads, cb)
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
