/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.share.jdwp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import nsk.share.Failure;

/**
 * This class contains JDWP constants, types and parameters.
 */
public class JDWP {

    public static class Error {

        public static final int NONE                            = 0;
        public static final int INVALID_THREAD                  = 10;
        public static final int INVALID_THREAD_GROUP            = 11;
        public static final int INVALID_PRIORITY                = 12;
        public static final int THREAD_NOT_SUSPENDED            = 13;
        public static final int THREAD_SUSPENDED                = 14;
        public static final int INVALID_OBJECT                  = 20;
        public static final int INVALID_CLASS                   = 21;
        public static final int CLASS_NOT_PREPARED              = 22;
        public static final int INVALID_METHODID                = 23;
        public static final int INVALID_LOCATION                = 24;
        public static final int INVALID_FIELDID                 = 25;
        public static final int INVALID_FRAMEID                 = 30;
        public static final int NO_MORE_FRAMES                  = 31;
        public static final int OPAQUE_FRAME                    = 32;
        public static final int NOT_CURRENT_FRAME               = 33;
        public static final int TYPE_MISMATCH                   = 34;
        public static final int INVALID_SLOT                    = 35;
        public static final int DUPLICATE                       = 40;
        public static final int NOT_FOUND                       = 41;
        public static final int INVALID_MONITOR                 = 50;
        public static final int NOT_MONITOR_OWNER               = 51;
        public static final int INTERRUPT                       = 52;
        public static final int INVALID_CLASS_FORMAT            = 60;
        public static final int CIRCULAR_CLASS_DEFINITION       = 61;
        public static final int FAILS_VERIFICATION              = 62;
        public static final int ADD_METHOD_NOT_IMPLEMENTED      = 63;
        public static final int SCHEMA_CHANGE_NOT_IMPLEMENTED   = 64;
        public static final int INVALID_TYPESTATE               = 65;
        public static final int HIERARCHY_CHANGE_NOT_IMPLEMENTED= 66;
        public static final int DELETE_METHOD_NOT_IMPLEMENTED   = 67;
        public static final int UNSUPPORTED_VERSION             = 68;
        public static final int NAMES_DONT_MATCH                = 69;
        public static final int CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED  = 70;
        public static final int METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 71;
        public static final int NOT_IMPLEMENTED                 = 99;
        public static final int NULL_POINTER                    = 100;
        public static final int ABSENT_INFORMATION              = 101;
        public static final int INVALID_EVENT_TYPE              = 102;
        public static final int ILLEGAL_ARGUMENT                = 103;
        public static final int OUT_OF_MEMORY                   = 110;
        public static final int ACCESS_DENIED                   = 111;
        public static final int VM_DEATH                        = 112;
        public static final int INTERNAL                        = 113;
        public static final int UNATTACHED_THREAD               = 115;
        public static final int INVALID_TAG                     = 500;
        public static final int ALREADY_INVOKING                = 502;
        public static final int INVALID_INDEX                   = 503;
        public static final int INVALID_LENGTH                  = 504;
        public static final int INVALID_STRING                  = 506;
        public static final int INVALID_CLASS_LOADER            = 507;
        public static final int INVALID_ARRAY                   = 508;
        public static final int TRANSPORT_LOAD                  = 509;
        public static final int TRANSPORT_INIT                  = 510;
        public static final int NATIVE_METHOD                   = 511;
        public static final int INVALID_COUNT                   = 512;

    }

    public static class Flag {

        public static final byte NONE           = (byte)0;
        public static final byte REPLY_PACKET   = (byte)0x80;
        public static final byte EVENT_PACKET   = NONE;

    }

    public static class EventKind {

        public static final byte VM_INIT         = (byte)90;
        public static final byte VM_START        = VM_INIT;
        public static final byte VM_DISCONNECTED = (byte)100;
        public static final byte VM_DEATH        = (byte)99;

        public static final byte THREAD_START    = (byte)6;
        public static final byte THREAD_END      = (byte)7;
        public static final byte THREAD_DEATH    = THREAD_END;

        public static final byte CLASS_PREPARE   = (byte)8;
        public static final byte CLASS_LOAD      = (byte)10;
        public static final byte CLASS_UNLOAD    = (byte)9;

        public static final byte METHOD_ENTRY    = (byte)40;
        public static final byte METHOD_EXIT     = (byte)41;

        public static final byte FIELD_ACCESS    = (byte)20;
        public static final byte FIELD_MODIFICATION = (byte)21;

        public static final byte EXCEPTION       = (byte)4;
        public static final byte EXCEPTION_CATCH = (byte)30;

        public static final byte FRAME_POP       = (byte)3;

        public static final byte BREAKPOINT      = (byte)2;

        public static final byte SINGLE_STEP     = (byte)1;

        public static final byte USER_DEFINED    = (byte)5;

    }

    public static class EventModifierKind {

        public static final byte COUNT          = (byte)1;
        public static final byte CONDITIONAL    = (byte)2;
        public static final byte THREAD_ONLY    = (byte)3;
        public static final byte CLASS_ONLY     = (byte)4;
        public static final byte CLASS_MATCH    = (byte)5;
        public static final byte CLASS_EXCLUDE  = (byte)6;
        public static final byte LOCATION_ONLY  = (byte)7;
        public static final byte EXCEPTION_ONLY = (byte)8;
        public static final byte FIELD_ONLY     = (byte)9;
        public static final byte STEP           = (byte)10;
        public static final byte INSTANCE_ONLY  = (byte)11;
    };

    public static class ThreadStatus {

        public static final int ZOMBIE          = 0;
        public static final int RUNNING         = 1;
        public static final int SLEEPING        = 2;
        public static final int MONITOR         = 3;
        public static final int WAIT            = 4;

    }

    public static class SuspendStatus {

        public static final int SUSPEND_STATUS_SUSPENDED = 0x1;

    }

    public static class ClassStatus {

        public static final int PREPARED        = 2;
        public static final int VERIFIED        = 1;
        public static final int INITIALIZED     = 4;
        public static final int ERROR           = 8;

    }

    public static class TypeTag {

        public static final byte CLASS           = (byte)1;
        public static final byte INTERFACE       = (byte)2;
        public static final byte ARRAY           = (byte)3;

    }

    public static class Tag {

        public static final byte ARRAY           = (byte)91;
        public static final byte BYTE            = (byte)66;
        public static final byte CHAR            = (byte)67;
        public static final byte OBJECT          = (byte)76;
        public static final byte FLOAT           = (byte)70;
        public static final byte DOUBLE          = (byte)68;
        public static final byte INT             = (byte)73;
        public static final byte LONG            = (byte)74;
        public static final byte SHORT           = (byte)83;
        public static final byte VOID            = (byte)86;
        public static final byte BOOLEAN         = (byte)90;
        public static final byte STRING          = (byte)115;
        public static final byte THREAD          = (byte)116;
        public static final byte THREAD_GROUP    = (byte)103;
        public static final byte CLASS_LOADER    = (byte)108;
        public static final byte CLASS_OBJECT    = (byte)99;

    }

    public static class StepDepth {

        public static final int INTO            = 0;
        public static final int OVER            = 1;
        public static final int OUT             = 2;

    }

    public static class StepSize {

        public static final int MIN             = 0;
        public static final int LINE            = 1;

    }

    public static class SuspendPolicy {

        public static final byte NONE            = (byte)0;
        public static final byte EVENT_THREAD    = (byte)1;
        public static final byte ALL             = (byte)2;

    }

    public static class InvokeOptions {

        public static final int INVOKE_SINGLE_THREADED     = 0x01;
        public static final int INVOKE_NONVIRTUAL          = 0x02;

    }

    public static class TypeSize {

        // VM independent type sizes

        public static final int BYTE              = 1;
        public static final int BOOLEAN           = 1;
        public static final int CHAR              = 2;
        public static final int SHORT             = 2;
        public static final int FLOAT             = 4;
        public static final int INT               = 4;
        public static final int LONG              = 8;
        public static final int DOUBLE            = 8;

        public static final int TAG               = 1;
        public static final int LOCATION_INDEX    = 8;

        // basic VM specific type sizes

        public static int OBJECT_ID         = 8;
        public static int METHOD_ID         = 4;
        public static int FIELD_ID          = 4;
        public static int FRAME_ID          = 4;

        // derivative VM specific type sizes

        public static int TAGGED_OBJECT_ID  = TAG + OBJECT_ID;

        public static int THREAD_ID         = OBJECT_ID;
        public static int THREAD_GROUP_ID   = OBJECT_ID;
        public static int STRING_ID         = OBJECT_ID;
        public static int CLASS_LOADER_ID   = OBJECT_ID;
        public static int CLASS_OBJECT_ID   = OBJECT_ID;
        public static int REFERENCE_TYPE_ID = OBJECT_ID;

        public static int CLASS_ID          = REFERENCE_TYPE_ID;
        public static int INTERFACE_ID      = REFERENCE_TYPE_ID;
        public static int ARRAY_ID          = REFERENCE_TYPE_ID;

        public static int LOCATION          = TAG + CLASS_ID + METHOD_ID + LOCATION_INDEX;

        /**
         * Calculate type sizes based on VM dependent basic type sizes.
         */
        public static void CalculateSizes() {

            TAGGED_OBJECT_ID  = TAG + OBJECT_ID;

            THREAD_ID         = OBJECT_ID;
            THREAD_GROUP_ID   = OBJECT_ID;
            STRING_ID         = OBJECT_ID;
            CLASS_LOADER_ID   = OBJECT_ID;
            CLASS_OBJECT_ID   = OBJECT_ID;
            REFERENCE_TYPE_ID = OBJECT_ID;

            CLASS_ID          = REFERENCE_TYPE_ID;
            INTERFACE_ID      = REFERENCE_TYPE_ID;
            ARRAY_ID          = REFERENCE_TYPE_ID;

            LOCATION          = TAG + CLASS_ID + METHOD_ID + LOCATION_INDEX;
        }

    }

    public static class ModifierFlag {

        public static final int PUBLIC                = 0x0001;
        public static final int PRIVATE               = 0x0002;
        public static final int PROTECTED             = 0x0004;
        public static final int STATIC                = 0x0008;
        public static final int FINAL                 = 0x0010;
        public static final int SUPER                 = 0x0020;
        public static final int VOLATILE              = 0x0040;
        public static final int TRANSIENT             = 0x0080;
        public static final int SYNCHRONIZED          = 0x0020;
        public static final int NATIVE                = 0x0100;
        public static final int INTERFACE             = 0x0200;
        public static final int ABSTRACT              = 0x0400;
        public static final int SYNTHETIC             = 0xF0000000;

        public static final int CLASS_MASK            = PUBLIC | FINAL | SUPER | INTERFACE | ABSTRACT;
        public static final int FIELD_MASK            = PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | VOLATILE | TRANSIENT;
        public static final int METHOD_MASK           = PUBLIC | PRIVATE | PROTECTED | STATIC | FINAL | SYNCHRONIZED | NATIVE | ABSTRACT;

    }

    public static class CommandSet {

        public static final byte VirtualMachine        = (byte)0x01;
        public static final byte ReferenceType         = (byte)0x02;
        public static final byte ClassType             = (byte)0x03;
        public static final byte ArrayType             = (byte)0x04;
        public static final byte InterfaceType         = (byte)0x05;
        public static final byte Method                = (byte)0x06;
        public static final byte Field                 = (byte)0x08;
        public static final byte ObjectReference       = (byte)0x09;
        public static final byte StringReference       = (byte)0x0A;
        public static final byte ThreadReference       = (byte)0x0B;
        public static final byte ThreadGroupReference  = (byte)0x0C;
        public static final byte ArrayReferemce        = (byte)0x0D;
        public static final byte ClassLoaderReference  = (byte)0x0E;
        public static final byte EventRequest          = (byte)0x0F;
        public static final byte StackFrame            = (byte)0x10;
        public static final byte ClassObjectReference  = (byte)0x11;
        public static final byte Event                 = (byte)0x40;

    }

    // command names, used only for debug output
    public static HashMap<Integer, String> commandNames = new HashMap<Integer, String>();

    static
    {
        commandNames.put(Command.ObjectReference.ReferringObjects, "ObjectReference.ReferringObjects");
        commandNames.put(Command.ReferenceType.Instances, "ReferenceType.Instances");
        commandNames.put(Command.ReferenceType.ClassFileVersion, "ReferenceType.ClassFileVersion");
        commandNames.put(Command.ReferenceType.ConstantPool, "ReferenceType.ConstantPool");
        commandNames.put(Command.ThreadReference.OwnedMonitorsStackDepthInfo, "ThreadReference.OwnedMonitorsStackDepthInfo");
        commandNames.put(Command.ThreadReference.ForceEarlyReturn, "ThreadReference.ForceEarlyReturn");
        commandNames.put(Command.VirtualMachine.InstanceCounts, "VirtualMachine.InstanceCounts");
    }

    public static class Command {

        public static class VirtualMachine {

            public static final int Version             = 0x0101;
            public static final int ClassesBySignature  = 0x0102;
            public static final int AllClasses          = 0x0103;
            public static final int AllThreads          = 0x0104;
            public static final int TopLevelThreadGroups  = 0x0105;
            public static final int Dispose             = 0x0106;
            public static final int IDSizes             = 0x0107;
            public static final int Suspend             = 0x0108;
            public static final int Resume              = 0x0109;
            public static final int Exit                = 0x010A;
            public static final int CreateString        = 0x010B;
            public static final int Capabilities        = 0x010C;
            public static final int ClassPaths          = 0x010D;
            public static final int DisposeObjects      = 0x010E;
            public static final int HoldEvents          = 0x010F;
            public static final int ReleaseEvents       = 0x0110;

            // since JDK-1.4
            public static final int CapabilitiesNew     = 0x0111;
            public static final int RedefineClasses     = 0x0112;
            public static final int SetDefaultStratum   = 0x0113;

            // since JDK-1.5
            public static final int AllClassesWithGeneric = 0x0114;

            // since JDK-1.6
            public static final int InstanceCounts      = 0x0115;
        }

        public static class ReferenceType {

            public static final int Signature           = 0x0201;
            public static final int ClassLoader         = 0x0202;
            public static final int Modifiers           = 0x0203;
            public static final int Fields              = 0x0204;
            public static final int Methods             = 0x0205;
            public static final int GetValues           = 0x0206;
            public static final int SourceFile          = 0x0207;
            public static final int NestedTypes         = 0x0208;
            public static final int Status              = 0x0209;
            public static final int Interfaces          = 0x020A;
            public static final int ClassObject         = 0x020B;

            // since JDK-1.4
            public static final int SourceDebugExtension = 0x020C;

            // since JDK-1.5
            public static final int SignatureWithGeneric = 0x020D;
            public static final int FieldsWithGeneric = 0x020E;
            public static final int MethodsWithGeneric = 0x020F;

            // since JDK-1.6
            public static final int Instances = 0x0210;
            public static final int ClassFileVersion = 0x0211;
            public static final int ConstantPool = 0x0212;
        }

        public static class ClassType {

            public static final int Superclass          = 0x0301;
            public static final int SetValues           = 0x0302;
            public static final int InvokeMethod        = 0x0303;
            public static final int NewInstance         = 0x0304;

        }

        public static class ArrayType {

            public static final int NewInstance         = 0x0401;

        }

        public static class InterfaceType {

        }

        public static class Method {

            public static final int LineTable           = 0x0601;
            public static final int VariableTable       = 0x0602;
            public static final int Bytecodes           = 0x0603;

            // since JDK-1.4
            public static final int IsObsolete          = 0x0604;

            // since JDK-1.5
            public static final int VariableTableWithGeneric = 0x0605;

        }

        public static class Field {

        }

        public static class ObjectReference {

            public static final int ReferenceType       = 0x0901;
            public static final int GetValues           = 0x0902;
            public static final int SetValues           = 0x0903;
            public static final int MonitorInfo         = 0x0905;
            public static final int InvokeMethod        = 0x0906;
            public static final int DisableCollection   = 0x0907;
            public static final int EnableCollection    = 0x0908;
            public static final int IsCollected         = 0x0909;

            // since JDK-1.6
            public static final int ReferringObjects         = 0x090A;
        }

        public static class StringReference {

            public static final int Value               = 0x0A01;

        }

        public static class ThreadReference {

            public static final int Name                = 0x0B01;
            public static final int Suspend             = 0x0B02;
            public static final int Resume              = 0x0B03;
            public static final int Status              = 0x0B04;
            public static final int ThreadGroup         = 0x0B05;
            public static final int Frames              = 0x0B06;
            public static final int FrameCount          = 0x0B07;
            public static final int OwnedMonitors       = 0x0B08;
            public static final int CurrentContendedMonitor = 0x0B09;
            public static final int Stop                = 0x0B0A;
            public static final int Interrupt           = 0x0B0B;
            public static final int SuspendCount        = 0x0B0C;
            public static final int PopTopFrame         = 0x0B0D;

            // since JDK-1.6
            public static final int OwnedMonitorsStackDepthInfo = 0x0B0D;
            public static final int ForceEarlyReturn = 0x0B0E;
        }

        public static class ThreadGroupReference {

            public static final int Name                = 0x0C01;
            public static final int Parent              = 0x0C02;
            public static final int Children            = 0x0C03;

        }

        public static class ArrayReference {

            public static final int Length              = 0x0D01;
            public static final int GetValues           = 0x0D02;
            public static final int SetValues           = 0x0D03;

        }

        public static class ClassLoaderReference {

            public static final int VisibleClasses      = 0x0E01;

        }

        public static class EventRequest {

            public static final int Set                 = 0x0F01;
            public static final int Clear               = 0x0F02;
            public static final int ClearAllBreakpoints = 0x0F03;

        }

        public static class StackFrame {

            public static final int GetValues           = 0x1001;
            public static final int SetValues           = 0x1002;
            public static final int ThisObject          = 0x1003;

            // since JDK-1.4
            public static final int PopFrames           = 0x1004;

        }

        public static class ClassObjectReference {

            public static final int ReflectedType       = 0x1101;

        }

        public static class Event {

            public static final int Composite           = 0x4064;

        }

    } // end of class Command

    public static class Capability {

        // common capabilities
        public static final int CAN_WATCH_FIELD_MODIFICATION        = 0;
        public static final int CAN_WATCH_FIELD_ACCESS              = 1;
        public static final int CAN_GET_BYTECODES                   = 2;
        public static final int CAN_GET_SYNTHETIC_ATTRIBUTE         = 3;
        public static final int CAN_GET_OWNED_MONITOR_INFO          = 4;
        public static final int CAN_GET_CURRENT_CONTENDED_MONITOR   = 5;
        public static final int CAN_GET_MONITOR_INFO                = 6;

        // new capabilities (since JDWP version 1.4)
        public static final int CAN_REDEFINE_CLASSES                = 7;
        public static final int CAN_ADD_METHODR_INFO                = 8;
        public static final int CAN_UNRESTRICTEDLY_REDEFINE_CLASSES = 9;
        public static final int CAN_POP_FRAMES                      = 10;
        public static final int CAN_USE_INSTANCE_FILTER             = 11;
        public static final int CAN_GET_SOURCE_DEBUG_EXTENSION      = 12;
        public static final int CAN_REQUEST_VMDEATH_EVENT           = 13;
        public static final int CAN_SET_DEFAULT_STRATUM             = 14;
    }

    public static class Location extends ByteBuffer {

        public static int TAG_OFFSET = 0;
        public static int CLASS_ID_OFFSET = TAG_OFFSET + JDWP.TypeSize.TAG;
        public static int METHOD_ID_OFFSET = CLASS_ID_OFFSET + JDWP.TypeSize.CLASS_ID;
        public static int INDEX_OFFSET = METHOD_ID_OFFSET + JDWP.TypeSize.METHOD_ID;

        private static void calculateOffsets() {
            CLASS_ID_OFFSET = TAG_OFFSET + JDWP.TypeSize.TAG;
            METHOD_ID_OFFSET = CLASS_ID_OFFSET + JDWP.TypeSize.CLASS_ID;
            INDEX_OFFSET = METHOD_ID_OFFSET + JDWP.TypeSize.METHOD_ID;
        }

        public Location(byte typeTag, long classID, long methodID, long index) {
            this();
            // 1 byte type tag
            putTag(typeTag);
            // classID
            putClassID(classID);
            // methodID
            putMethodID(methodID);
            // 8 bytes index
            putIndex(index);
        }

        public Location() {
            super(JDWP.TypeSize.LOCATION, 0);
            addBytes((byte)0, TypeSize.LOCATION);

            // calculate offsets for VM-dependent type sizes
            calculateOffsets();
        }

        public final byte getTag() {
            try {
                return getByte(TAG_OFFSET);
            } catch (BoundException e) {
                throw new Failure("Unable to get tag from location:\n\t" + e);
            }
        }

        public final long getClassID() {
            try {
                return getID(CLASS_ID_OFFSET, JDWP.TypeSize.CLASS_ID);
            } catch (BoundException e) {
                throw new Failure("Unable to get classID from location:\n\t" + e);
            }
        }

        public final long getMethodID() {
            try {
                return getID(METHOD_ID_OFFSET, JDWP.TypeSize.METHOD_ID);
            } catch (BoundException e) {
                throw new Failure("Unable to get methodID from location:\n\t" + e);
            }
        }

        public final long getIndex() {
            try {
                return getID(INDEX_OFFSET, JDWP.TypeSize.LOCATION_INDEX);
            } catch (BoundException e) {
                throw new Failure("Unable to get code index from location:\n\t" + e);
            }
        }

        public final void putTag(byte tag) {
            try {
                putByte(TAG_OFFSET, tag);
            } catch (BoundException e) {
                throw new Failure("Unable to put tag into location:\n\t" + e);
            }
        }

        public final void putClassID(long classID) {
            try {
                putID(CLASS_ID_OFFSET, classID, JDWP.TypeSize.CLASS_ID);
            } catch (BoundException e) {
                throw new Failure("Unable to put classID into location:\n\t" + e);
            }
        }

        public final void putMethodID(long methodID) {
            try {
                putID(METHOD_ID_OFFSET, methodID, JDWP.TypeSize.METHOD_ID);
            } catch (BoundException e) {
                throw new Failure("Unable to put methodID into location:\n\t" + e);
            }
        }

        public final void putIndex(long index) {
            try {
                putID(INDEX_OFFSET, index, JDWP.TypeSize.LOCATION_INDEX);
            } catch (BoundException e) {
                throw new Failure("Unable to put code index into location:\n\t" + e);
            }
        }

        public String toString() {
            return "Location("
            + "tag=" + getTag() + ", "
            + "classID=" + getClassID() + ", "
            + "methodID=" + getMethodID() + ", "
            + "index=" + getIndex()
            + ")";
        }

    } // end of class Location

    public static class UntaggedValue {

        public Object value = null;

        public UntaggedValue() {
        }

        public UntaggedValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }

        public int length(byte tag) {
            int valueSize = 0;
            try {
                switch (tag) {
                    case JDWP.Tag.BYTE: {
                        valueSize = JDWP.TypeSize.BYTE;
                    } break;
                    case JDWP.Tag.CHAR: {
                        valueSize = JDWP.TypeSize.CHAR;
                    } break;
                    case JDWP.Tag.FLOAT: {
                        valueSize = JDWP.TypeSize.FLOAT;
                    } break;
                    case JDWP.Tag.DOUBLE: {
                        valueSize = JDWP.TypeSize.DOUBLE;
                    } break;
                    case JDWP.Tag.INT: {
                        valueSize = JDWP.TypeSize.INT;
                    } break;
                    case JDWP.Tag.SHORT: {
                        valueSize = JDWP.TypeSize.SHORT;
                    } break;
                    case JDWP.Tag.BOOLEAN: {
                        valueSize = JDWP.TypeSize.BYTE;
                    } break;
                    case JDWP.Tag.LONG: {
                        valueSize = JDWP.TypeSize.LONG;
                    } break;
                    case JDWP.Tag.VOID: {
                        valueSize = 0;
                    } break;
                    case JDWP.Tag.ARRAY:
                    case JDWP.Tag.OBJECT:
                    case JDWP.Tag.STRING:
                    case JDWP.Tag.THREAD:
                    case JDWP.Tag.THREAD_GROUP:
                    case JDWP.Tag.CLASS_LOADER:
                    case JDWP.Tag.CLASS_OBJECT: {
                        valueSize = JDWP.TypeSize.OBJECT_ID;
                    } break;
                    default: {
                        throw new Failure("Unknown tag found while putting value into packet: " + tag);
                    }
                }
            } catch (ClassCastException e) {
                throw new Failure("Wrong tag " + tag + " found while putting value to packet: " + value);
            }
            return JDWP.TypeSize.TAG + valueSize;
        }

        public void addValueTo(Packet packet, byte tag) {
            if (value == null) {
                throw new Failure("Unable to put null value into packet: " + this);
            }
            try {
                switch (tag) {
                    case JDWP.Tag.BYTE: {
                        byte castedValue = ((Byte)value).byteValue();
                        packet.addByte(castedValue);
                    } break;
                    case JDWP.Tag.CHAR: {
                        char castedValue = ((Character)value).charValue();
                        packet.addChar(castedValue);
                    } break;
                    case JDWP.Tag.FLOAT: {
                        float castedValue = ((Float)value).floatValue();
                        packet.addFloat(castedValue);
                    } break;
                    case JDWP.Tag.DOUBLE: {
                        double castedValue = ((Double)value).doubleValue();
                        packet.addDouble(castedValue);
                    } break;
                    case JDWP.Tag.INT: {
                        int castedValue = ((Integer)value).intValue();
                        packet.addInt(castedValue);
                    } break;
                    case JDWP.Tag.SHORT: {
                        short castedValue = ((Short)value).shortValue();
                        packet.addShort(castedValue);
                    } break;
                    case JDWP.Tag.BOOLEAN: {
                        boolean castedValue = ((Boolean)value).booleanValue();
                        packet.addByte((byte)(castedValue? 1 : 0));
                    } break;
                    case JDWP.Tag.LONG: {
                        long castedValue = ((Long)value).longValue();
                        packet.addLong(castedValue);
                    } break;
                    case JDWP.Tag.VOID: {
                    } break;
                    case JDWP.Tag.ARRAY:
                    case JDWP.Tag.OBJECT:
                    case JDWP.Tag.STRING:
                    case JDWP.Tag.THREAD:
                    case JDWP.Tag.THREAD_GROUP:
                    case JDWP.Tag.CLASS_LOADER:
                    case JDWP.Tag.CLASS_OBJECT: {
                        long castedValue = ((Long)value).longValue();
                        packet.addObjectID(castedValue);
                    } break;
                    default: {
                        throw new Failure("Unknown tag found while putting value into packet: " + tag);
                    }
                }
            } catch (ClassCastException e) {
                throw new Failure("Wrong tag " + tag + " found while putting value to packet: " + value);
            }
        }

        public void getValueFrom(Packet packet, byte tag) throws BoundException {
            switch (tag) {
                case JDWP.Tag.BYTE: {
                    byte castedValue = packet.getByte();
                    value = Byte.valueOf(castedValue);
                } break;
                case JDWP.Tag.CHAR: {
                    char castedValue = packet.getChar();
                    value = Character.valueOf(castedValue);
                } break;
                case JDWP.Tag.FLOAT: {
                    float castedValue = packet.getFloat();
                    value = Float.valueOf(castedValue);
                } break;
                case JDWP.Tag.DOUBLE: {
                    double castedValue = packet.getDouble();
                    value = Double.valueOf(castedValue);
                } break;
                case JDWP.Tag.INT: {
                    int castedValue = packet.getInt();
                    value = Integer.valueOf(castedValue);
                } break;
                case JDWP.Tag.SHORT: {
                    short castedValue = packet.getShort();
                    value = Short.valueOf(castedValue);
                } break;
                case JDWP.Tag.BOOLEAN: {
                    byte castedValue = packet.getByte();
                    value = Boolean.valueOf(castedValue != 0);
                } break;
                case JDWP.Tag.LONG: {
                    long castedValue = packet.getLong();
                    value = Long.valueOf(castedValue);
                } break;
                case JDWP.Tag.VOID: {
                    value = Long.valueOf(0);
                } break;
                case JDWP.Tag.ARRAY:
                case JDWP.Tag.OBJECT:
                case JDWP.Tag.STRING:
                case JDWP.Tag.THREAD:
                case JDWP.Tag.THREAD_GROUP:
                case JDWP.Tag.CLASS_LOADER:
                case JDWP.Tag.CLASS_OBJECT: {
                    long castedValue = packet.getObjectID();
                    value = Long.valueOf(castedValue);
                } break;
                default: {
                    throw new Failure("Unknown tag found while reading value from packet: " + tag);
                }
            }
        }

        public String toString(byte tag) {
            if (value == null) {
                return "null";
            }
            String type = null;
            try {
                switch (tag) {
                    case JDWP.Tag.BYTE: {
                        type = "BYTE";
                    } break;
                    case JDWP.Tag.CHAR: {
                        type = "CHAR";
                    } break;
                    case JDWP.Tag.FLOAT: {
                        type = "FLOAT";
                    } break;
                    case JDWP.Tag.DOUBLE: {
                        type = "DOUBLE";
                    } break;
                    case JDWP.Tag.INT: {
                        type = "INT";
                    } break;
                    case JDWP.Tag.SHORT: {
                        type = "SHORT";
                    } break;
                    case JDWP.Tag.BOOLEAN: {
                        type = "BOOLEAN";
                    } break;
                    case JDWP.Tag.LONG: {
                        type = "LONG";
                    } break;
                    case JDWP.Tag.VOID: {
                        type = "VOID";
                    } break;
                    case JDWP.Tag.ARRAY: {
                        type = "ARRAY_ID";
                    } break;
                    case JDWP.Tag.OBJECT: {
                        type = "OBJECT_ID";
                    } break;
                    case JDWP.Tag.STRING: {
                        type = "STRING_ID";
                    } break;
                    case JDWP.Tag.THREAD: {
                        type = "THREAD_ID";
                    } break;
                    case JDWP.Tag.THREAD_GROUP: {
                        type = "THREAD_GROUP_ID";
                    } break;
                    case JDWP.Tag.CLASS_LOADER: {
                        type = "CLASS_LOADER_ID";
                    } break;
                    case JDWP.Tag.CLASS_OBJECT: {
                        type = "CLASS_OBJECT_ID";
                    } break;
                    default: {
                        throw new Failure("Unknown tag found while converting value into string: " + tag);
                    }
                }
                return "(" + type + ")" + value;
            } catch (ClassCastException e) {
                throw new Failure("Wrong tag " + tag + " found while putting value to packet: " + value);
            }
        }

    } // end of class Value

    public static class Value extends UntaggedValue {

        public static final int TAG_OFFSET = 0;
        public static final int VALUE_OFFSET = TAG_OFFSET + TypeSize.TAG;

        public byte tag = 0;

        public Value() {
        }

        public Value(byte tag, Object value) {
            super(value);
            this.tag = tag;
        }

        public byte getTag() {
            return tag;
        }

        public int length() {
            return super.length(tag);
        }

        public void addValueTo(Packet packet) {
            if (value == null) {
                throw new Failure("Unable to put null value into packet: " + this);
            }
            packet.addByte(tag);
            super.addValueTo(packet, tag);
        }

        public void getValueFrom(Packet packet) throws BoundException {
            tag = packet.getByte();
            super.getValueFrom(packet, tag);
        }

        public String toString() {
            return super.toString(tag);
        }

    } // end of class Value

} // end of class JDWP
