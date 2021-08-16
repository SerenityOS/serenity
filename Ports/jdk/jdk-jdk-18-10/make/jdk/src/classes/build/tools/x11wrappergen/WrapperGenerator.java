/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package build.tools.x11wrappergen;

import java.util.*;
import java.io.*;
import java.nio.charset.*;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WrapperGenerator {
    /* XLibParser converts Xlib.h to a Java Object that encapsulates the
     * X11 API and data structures */
    // Charset and decoder for ISO-8859-15
    private final static Logger log = Logger.getLogger("WrapperGenerator");
    boolean generateLog = true;
    boolean wide;
    private static Charset charset = Charset.forName("ISO-8859-15");

    String package_name = "sun.awt.X11";
    String package_path = "sun/awt/X11";
    String sizerFileName = "sizer.c";
    String defaultBaseClass = "XWrapperBase";

    String compile_options = "-lX11";
    static Hashtable<String, BaseType> symbolTable = new Hashtable<>();
    static Hashtable<String, String> sizeTable32bit = new Hashtable<>();
    static Hashtable<String, String> sizeTable64bit = new Hashtable<>();
    static Hashtable<String, Integer> knownSizes32 = new Hashtable<>();
    static Hashtable<String, Integer> knownSizes64 = new Hashtable<>();
    static {
/*
        knownSizes64.put("", Integer.valueOf());
        knownSizes32.put("", Integer.valueOf());
*/
        knownSizes64.put("XComposeStatus", Integer.valueOf(16));
        knownSizes64.put("XTimeCoord", Integer.valueOf(16));
        knownSizes64.put("XExtData", Integer.valueOf(32));
        knownSizes64.put("XWindowChanges", Integer.valueOf(40));
        knownSizes64.put("XOMCharSetList", Integer.valueOf(16));
        knownSizes64.put("XModifierKeymap", Integer.valueOf(16));
        knownSizes32.put("XIMValuesList", Integer.valueOf(8));
        knownSizes32.put("XGCValues", Integer.valueOf(92));
//          knownSizes32.put("XIMStringConversionCallbackStruct", Integer.valueOf(16));
    }

    private static abstract class BaseType {

        String real_type;
        String name;


        public String getName() {
            return name;
        }
        public String getRealType() {
            return real_type;
        }

        public String toString() {
            return name;
        }
    }

    private static class AtomicType extends BaseType {

        private boolean alias;
        private String aliasName;

        static final int TYPE_INT=0;
        static final int TYPE_CHAR=1;
        static final int TYPE_LONG=2;
        static final int TYPE_LONG_LONG=3;
        static final int TYPE_DOUBLE=4;
        static final int TYPE_FLOAT=5;
        static final int TYPE_PTR=6;
        static final int TYPE_SHORT=7;
        static final int TYPE_BOOL = 8;
        static final int TYPE_STRUCT = 9;
        static final int TYPE_ARRAY = 10;
        static final int TYPE_BYTE=11;
        static final int TYPE_ATOM = 12;
        static final int TYPE_ULONG = 13;
        static int getTypeForString(String str) {
            int type=-1;
            if (str.equals("int"))
                type = AtomicType.TYPE_INT;
            else if (str.equals("long"))
                type = AtomicType.TYPE_LONG;
            else if (str.equals("byte"))
                type = AtomicType.TYPE_BYTE;
            else if (str.equals("char"))
                type = AtomicType.TYPE_CHAR;
            else if (str.equals("long long"))
                type = AtomicType.TYPE_LONG_LONG;
            else if (str.equals("double"))
                type = AtomicType.TYPE_DOUBLE;
            else if (str.equals("float"))
                type = AtomicType.TYPE_FLOAT;
            else if (str.equals("pointer"))
                type = AtomicType.TYPE_PTR;
            else if (str.equals("short"))
                type = AtomicType.TYPE_SHORT;
            else if (str.equals("Bool"))
                type = AtomicType.TYPE_BOOL;
            else if (str.equals("struct"))
                type = AtomicType.TYPE_STRUCT;
            else if (str.equals("Atom"))
                type = AtomicType.TYPE_ATOM;
            else if (str.equals("array"))
                type = TYPE_ARRAY;
            else if (str.equals("ulong"))
                type = TYPE_ULONG;
            else throw new IllegalArgumentException("Uknown type string: " + str);

            return type;
        }
        String getJavaType() {
            if (referencedType != null) {
                if (referencedType instanceof AtomicType) {
                    return ((AtomicType)referencedType).getJavaType();
                } else {
                    return referencedType.getName();
                }
            } else {
                return getJavaTypeForType(type);
            }
        }
        static String getJavaTypeForType(int type) {
            switch (type) {
              case TYPE_INT:
                  return "int";
              case TYPE_CHAR:
                  return "char";
              case TYPE_BYTE:
                  return "byte";
              case TYPE_LONG:
              case TYPE_LONG_LONG:
              case TYPE_PTR:
              case TYPE_ULONG:
                  return "long";
              case TYPE_DOUBLE:
                  return "double";
              case TYPE_FLOAT:
                  return "float";
              case TYPE_SHORT:
                  return "short";
              case TYPE_BOOL:
                  return "boolean";
              case TYPE_ATOM:
                  return "long";
              default:
                  throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
        String getItemSize() {
            if (referencedType != null) {
                  if (referencedType instanceof StructType) {
                      return ((StructType)referencedType).getSize();
                  } else {
                      return ((AtomicType)referencedType).getItemSize();
                  }
            } else {
                int i32 = getNativeSizeForAccess(getJavaAccess(false));
                int i64 = getNativeSizeForAccess(getJavaAccess(true));
                if (i32 != i64) {
                    return "Native.get" + getNativeAccess() + "Size()";
                } else {
                    return Integer.toString(i32);
                }
            }
        }

        String getJavaResult(String offset, String base) {
            String res = null;
            switch (type) {
              case TYPE_STRUCT:
                  res = "pData + " + offset;
                  break;
              case TYPE_PTR:
                  if (referencedType == null || referencedType instanceof StructType) {
                      res = base + "+" + offset;
                  } else if (referencedType instanceof AtomicType) {
                      res = MessageFormat.format("Native.get{0}({1})",
                                                 new Object[] {getNativeAccessForType(((AtomicType)referencedType).type),
                                                               base + "+" + offset});
                  }
                  break;
              case TYPE_ARRAY:
                  if (referencedType instanceof StructType) {
                      res = "pData + " + offset;
                  }  else if (referencedType instanceof AtomicType) {
                      res = MessageFormat.format("Native.get{0}(pData + {1})",
                                                 new Object[] {getNativeAccessForType(((AtomicType)referencedType).type),
                                                               offset});
                  }
                  break;
              default:
                res = MessageFormat.format("(Native.get{0}(pData+{1}))",
                                           new Object[] {getNativeAccess(), offset});
            }
            return getJavaResultConversion(res, base);
        }
        String getJavaResultConversion(String value, String base) {
            if (referencedType != null) {
                if (referencedType instanceof StructType) {
                    if (type == TYPE_PTR) {
                        return MessageFormat.format("({2} != 0)?(new {0}({1})):(null)", new Object[] {referencedType.getName(),value, base});
                    } else {
                        return MessageFormat.format("new {0}({1})", new Object[] {referencedType.getName(),value});
                    }
                } else {
                    return value;
                }
            } else {
                return getJavaResultConversionForType(type, value);
            }
        }
        static String getJavaResultConversionForType(int type, String value) {
            return value;
        }
        String getNativeAccess() {
            return getNativeAccessForType(type);
        }
        String getJavaAccess(boolean wide) {
            return getJavaAccessForType(type, wide);
        }
        static String getJavaAccessForType(int type, boolean wide) {
            switch (type) {
              case TYPE_INT:
                  return "Int";
              case TYPE_CHAR:
                  return "Char";
              case TYPE_BYTE:
                  return "Byte";
              case TYPE_LONG:
              case TYPE_PTR:
              case TYPE_ARRAY:
              case TYPE_STRUCT:
              case TYPE_ATOM:
                  return (wide?"Long":"Int");
              case TYPE_LONG_LONG:
                  return "Long";
              case TYPE_ULONG:
                  return (wide?"ULong":"UInt");
              case TYPE_DOUBLE:
                  return "Double";
              case TYPE_FLOAT:
                  return "Float";
              case TYPE_SHORT:
                  return "Short";
              case TYPE_BOOL:
                  return "Int";
              default:
                  throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
        static String getNativeAccessForType(int type) {
            switch (type) {
              case TYPE_INT:
                  return "Int";
              case TYPE_CHAR:
                  return "Char";
              case TYPE_BYTE:
                  return "Byte";
              case TYPE_LONG:
              case TYPE_PTR:
              case TYPE_ARRAY:
              case TYPE_STRUCT:
                  return "Long";
              case TYPE_LONG_LONG:
                  return "Long";
              case TYPE_ULONG:
                  return "ULong";
              case TYPE_DOUBLE:
                  return "Double";
              case TYPE_FLOAT:
                  return "Float";
              case TYPE_SHORT:
                  return "Short";
              case TYPE_BOOL:
                  return "Bool";
              case TYPE_ATOM:
                  return "Long";
              default:
                  throw new IllegalArgumentException("Unknown type: " + type);
            }
        }

        static int getNativeSizeForAccess(String access) {
            if (access.equals("Int")) return 4;
            else if (access.equals("Byte")) return 1;
            else if (access.equals("Long")) return 8;
            else if (access.equals("Double")) return 8;
            else if (access.equals("Float")) return 4;
            else if (access.equals("Char")) return 2;
            else if (access.equals("Short")) return 2;
            else if (access.equals("ULong")) return 8;
            else if (access.equals("UInt")) return 4;
            else throw new IllegalArgumentException("Unknow access type: " + access);
        }

        String getJavaConversion(String offset, String value) {
            if (referencedType != null) {
                if (referencedType instanceof StructType) {
                    return getJavaConversionForType(TYPE_PTR, offset, value + ".pData");
                } else {
                    if (type == TYPE_ARRAY) {
                        return getJavaConversionForType(((AtomicType)referencedType).type, offset, value);
                    } else { // TYPE_PTR
                        return getJavaConversionForType(TYPE_PTR, offset, value);
                    }
                }
            } else {
                return getJavaConversionForType(type, offset, value);
            }
        }
        static String getJavaConversionForType(int type, String offset, String value) {
            return MessageFormat.format("Native.put{0}({2}, {1})", new Object[] {getNativeAccessForType(type), value, offset});
        }


        int type;
        int offset;
        int direction;
        BaseType referencedType;
        int arrayLength = -1;
        boolean autoFree = false;
        public AtomicType(int _type,String _name, String _real_type) {
            name = _name.replaceAll("[* \t]","");
            if ((name.indexOf("[") != -1) || (name.indexOf("]") != -1))
            {
                name = name.replaceAll("\\[.*\\]","");
            }
            type = _type;
            real_type = _real_type;
            if (real_type == null)
            {
                System.out.println(" real type is null");

            }
        }
        public boolean isIn() {
            return direction == 0;
        }
        public boolean isOut() {
            return direction == 1;
        }
        public boolean isInOut() {
            return direction == 2;
        }
        public boolean isAutoFree() {
            return autoFree;
        }
        public void setAttributes(String[] attributes) {
            String mod = attributes[3];
            if ("in".equals(mod)) {
                direction = 0;
            } else if ("out".equals(mod)) {
                direction = 1;
                if (attributes.length > 4 && "free".equals(attributes[4])) {
                    autoFree = true;
                }
            } else if ("inout".equals(mod)) {
                direction = 2;
            } else if ("alias".equals(mod)) {
                alias = true;
                aliasName = attributes[4];
            } else if (type == TYPE_ARRAY || type == TYPE_PTR || type == TYPE_STRUCT) {
                referencedType = symbolTable.get(mod);
                if (referencedType == null) {
                    log.warning("Can't find type for name " + mod);
                }
                if (attributes.length > 4) { // array length
                    try {
                        arrayLength = Integer.parseInt(attributes[4]);
                    } catch (Exception e) {
                    }
                }
            }
        }
        public BaseType getReferencedType() {
            return referencedType;
        }
        public int getArrayLength() {
            return arrayLength;
        }
        public void setOffset(int o)
        {
            offset = o;
        }
        public int getType() {
            return type;
        }
        public String getTypeUpperCase() {
            switch (type) {
              case TYPE_INT:
                  return "Int";
              case TYPE_CHAR:
                  return "Char";
              case TYPE_BYTE:
                  return "Byte";
              case TYPE_LONG:
              case TYPE_LONG_LONG:
              case TYPE_PTR:
                  return "Long";
              case TYPE_DOUBLE:
                  return "Double";
              case TYPE_FLOAT:
                  return "Float";
              case TYPE_SHORT:
                  return "Short";
              case TYPE_BOOL:
                  return "Int";
              case TYPE_ATOM:
                  return "Long";
              case TYPE_ULONG:
                  return "ULong";
              default: throw new IllegalArgumentException("Uknown type");
            }
        }
        public int getOffset()
        {
            return offset;
        }
        public boolean isAlias() {
            return alias;
        }
        public String getAliasName() {
            return aliasName;
        }
    }

    private static class StructType extends BaseType {

        Vector<BaseType> members;
        String description;
        boolean packed;
        int size;
        String baseClass, interfaces;
        boolean isInterface;
        String javaClassName;

        /**
         * Construct new structured type.
         * Description is used for name and type definition and has the following format:
         * structName [ '[' base classe ']' ] [ '{' interfaces '}' ] [ '|' javaClassName ]
         */
        public StructType(String _desc)
        {
            members = new Vector<>();
            parseDescription(_desc);
        }
        public int getNumFields()
        {
            return members.size();
        }
        public void setName(String _name)
        {
            _name = _name.replaceAll("[* \t]","");
            parseDescription(_name);
        }

        public void setSize(int i)
        {
            size = i;
        }

        public String getDescription()
        {
            return description;
        }

        public Enumeration<BaseType> getMembers()
        {
            return members.elements();
        }

        public void addMember(BaseType tp)
        {
            members.add(tp);
        }
        public String getBaseClass() {
            return baseClass;
        }
        public String getInterfaces() {
            return interfaces;
        }
        public boolean getIsInterface() {
            return isInterface;
        }
        public String getJavaClassName() {
            return javaClassName;
        }
        void parseDescription(String _desc) {
            if (_desc.indexOf('[') != -1) { // Has base class
                baseClass = _desc.substring(_desc.indexOf('[')+1, _desc.indexOf(']'));
                _desc = _desc.substring(0, _desc.indexOf('[')) + _desc.substring(_desc.indexOf(']')+1);
            }
            if (_desc.indexOf('{') != -1) { // Has base class
                interfaces = _desc.substring(_desc.indexOf('{')+1, _desc.indexOf('}'));
                _desc = _desc.substring(0, _desc.indexOf('{')) + _desc.substring(_desc.indexOf('}')+1);
            }
            if (_desc.startsWith("-")) { // Interface
                isInterface = true;
                _desc = _desc.substring(1, _desc.length());
            }
            if (_desc.indexOf("|") != -1) {
                javaClassName = _desc.substring(_desc.indexOf('|')+1, _desc.length());
                _desc = _desc.substring(0, _desc.indexOf('|'));
            }
            name = _desc;
            if (javaClassName == null) {
                javaClassName = name;
            }
            description = _desc;
//              System.out.println("Struct " + name + " extends " + baseClass + " implements " + interfaces);
        }

        /**
         * Returns String containing Java code calculating size of the structure depending on the data model
         */
        public String getSize() {
            String s32 = WrapperGenerator.sizeTable32bit.get(getName());
            String s64 = WrapperGenerator.sizeTable64bit.get(getName());
            if (s32 == null || s64 == null) {
                return (s32 == null)?(s64):(s32);
            }
            if (s32.equals(s64)) {
                return s32;
            } else {
                return MessageFormat.format("((XlibWrapper.dataModel == 32)?({0}):({1}))", new Object[] {s32, s64});
            }
        }
        public String getOffset(AtomicType atp) {
            String key = getName()+"."+(atp.isAlias() ? atp.getAliasName() : atp.getName());
            String s64 = WrapperGenerator.sizeTable64bit.get(key);
            String s32 = WrapperGenerator.sizeTable32bit.get(key);
            if (s32 == null || s64 == null) {
                return (s32 == null)?(s64):(s32);
            }
            if (s32.equals(s64)) {
                return s32;
            } else {
                return MessageFormat.format("((XlibWrapper.dataModel == 32)?({0}):({1}))", new Object[]{s32, s64});
            }
        }
    }

    private static class FunctionType extends BaseType {

        Vector<BaseType> args;
        String description;
        boolean packed;
        String returnType;

        int alignment;

        public FunctionType(String _desc)
        {
            args = new Vector<>();
            description = _desc;
            setName(_desc);
        }
        boolean isVoid() {
            return (returnType == null);
        }
        String getReturnType() {
            if (returnType == null) {
                return "void";
            } else {
                return returnType;
            }
        }

        public int getNumArgs()
        {
            return args.size();
        }
        public void setName(String _name)
        {
            if (_name.startsWith("!")) {
                _name = _name.substring(1, _name.length());
            }
            if (_name.indexOf("|") != -1) {
                returnType = _name.substring(_name.indexOf("|")+1, _name.length());
                _name = _name.substring(0, _name.indexOf("|"));
            }
            name = _name.replaceAll("[* \t]","");
        }

        public String getDescription()
        {
            return description;
        }

        public Collection<BaseType> getArguments()
        {
            return args;
        }
        public void addArgument(BaseType tp)
        {
            args.add(tp);
        }
    }

    public String makeComment(String str)
    {
        StringTokenizer st = new StringTokenizer(str,"\r\n");
        String ret="";

        while (st.hasMoreTokens())
        {
            ret = ret + "//" + st.nextToken() + "\n";
        }

        return ret;
    }

    public String getJavaTypeForSize(int size) {
        switch(size) {
          case 1: return "byte";
          case 2: return "short";
          case 4: return "int";
          case 8: return "long";
          default: throw new RuntimeException("Unsupported size: " + size);
        }
    }
    public String getOffsets(StructType stp,AtomicType atp, boolean wide)
    {
        String key = stp.getName()+"."+atp.getName();
        return wide == true ? sizeTable64bit.get(key) : sizeTable32bit.get(key);
    }

    public String getStructSize(StructType stp, boolean wide)
    {
        return wide == true ? sizeTable64bit.get(stp.getName()) : sizeTable32bit.get(stp.getName());
    }

    public int getLongSize(boolean wide)
    {
        return Integer.parseInt(wide == true ? sizeTable64bit.get("long") : sizeTable32bit.get("long"));
    }

    public int getPtrSize(boolean wide)
    {
        return Integer.parseInt(wide == true ? sizeTable64bit.get("ptr") : sizeTable32bit.get("ptr"));
    }
    public int getBoolSize(boolean wide) {
        return getOrdinalSize("Bool", wide);
    }
    public int getOrdinalSize(String ordinal, boolean wide) {
        return Integer.parseInt(wide == true ? sizeTable64bit.get(ordinal) : sizeTable32bit.get(ordinal));
    }

    public void writeToString(StructType stp, PrintWriter pw) {
        int type;
        pw.println("\n\n\tString getName() {\n\t\treturn \"" + stp.getName()+ "\"; \n\t}");
        pw.println("\n\n\tString getFieldsAsString() {\n\t\tStringBuilder ret = new StringBuilder(" + stp.getNumFields() * 40 + ");\n");

        for (Enumeration<BaseType> e = stp.getMembers() ; e.hasMoreElements() ;) {
            AtomicType tp = (AtomicType) e.nextElement();

            type = tp.getType();
            String name = tp.getName().replace('.', '_');
            if ((name != null) && (name.length() > 0))
            {
                if (type == AtomicType.TYPE_ATOM) {
                    pw.println("\t\tret.append(\"" + name + " = \" ).append( XAtom.get(get_" + name + "()) ).append(\", \");");
                } else if (name.equals("type")) {
                    pw.println("\t\tret.append(\"type = \").append( XlibWrapper.eventToString[get_type()] ).append(\", \");");
                } else if (name.equals("window")){
                    pw.println("\t\tret.append(\"window = \" ).append( getWindow(get_window()) ).append(\", \");");
                } else if (type == AtomicType.TYPE_ARRAY) {
                    pw.print("\t\tret.append(\"{\")");
                    for (int i = 0; i < tp.getArrayLength(); i++) {
                        pw.print("\n\t\t.append( get_" + name + "(" + i + ") ).append(\" \")");
                    }
                    pw.println(".append( \"}\");");
                } else {
                    pw.println("\t\tret.append(\"" + name +" = \").append( get_"+ name+"() ).append(\", \");");
                }
            }

        }
        pw.println("\t\treturn ret.toString();\n\t}\n\n");
    }

    public void writeStubs(StructType stp, PrintWriter pw) {
        int type;
        String prefix = "";
        if (!stp.getIsInterface()) {
            prefix = "\t\tabstract ";
        } else {
            prefix = "\t";
        }
        for (Enumeration<BaseType> e = stp.getMembers() ; e.hasMoreElements() ;) {
            AtomicType tp = (AtomicType) e.nextElement();

            type = tp.getType();
            String name = tp.getName().replace('.','_');
            if ((name != null) && (name.length() > 0))
            {
                if (type == AtomicType.TYPE_ARRAY) {
                    // Returns pointer to the start of the array
                    pw.println(prefix + "long get_" +name +"();");

                    pw.println(prefix + tp.getJavaType() + " get_" +name +"(int index);");
                    pw.println(prefix + "void set_" +name +"(int index, " + tp.getJavaType() + " v);");
                } else {
                    pw.println(prefix + tp.getJavaType() + " get_" +name +"();");
                    if (type != AtomicType.TYPE_STRUCT) pw.println(prefix + "void set_" +name +"(" + tp.getJavaType() + " v);");
                }
            }
        }
    }

    private int padSize(int size, int wordLength) {
        int bytesPerWord = wordLength / 8;
        // Make size dividable by bytesPerWord
        return (size + bytesPerWord / 2) / bytesPerWord * bytesPerWord;
    }

    public void writeAccessorImpls(StructType stp, PrintWriter pw) {
        int type;
        int i=0;
        String s_size_32 = getStructSize(stp, false);
        String s_size_64 = getStructSize(stp, true);
        int acc_size_32 = 0;
        int acc_size_64 = 0;
        String s_log = (generateLog?"log.finest(\"\");":"");
        for (Enumeration<BaseType> e = stp.getMembers() ; e.hasMoreElements() ;) {
            AtomicType tp = (AtomicType) e.nextElement();

            type = tp.getType();
            String name = tp.getName().replace('.','_');
            String pref = "\tpublic " ;
            if ((name != null) && (name.length() > 0))
            {
                String jt = tp.getJavaType();
                String ja_32 = tp.getJavaAccess(false);
                String ja_64 = tp.getJavaAccess(true);
                String ja = ja_32;
                int elemSize_32 = AtomicType.getNativeSizeForAccess(ja_32);
                int elemSize_64 = AtomicType.getNativeSizeForAccess(ja_64);
                String elemSize = tp.getItemSize();
                if (type == AtomicType.TYPE_ARRAY) {
                    acc_size_32 += elemSize_32 * tp.getArrayLength();
                    acc_size_64 += elemSize_64 * tp.getArrayLength();
                    pw.println(pref + tp.getJavaType() + " get_" +name + "(int index) { " +s_log+"return " +
                               tp.getJavaResult(stp.getOffset(tp) + "+index*" + elemSize, null) + "; }");
                    if (tp.getReferencedType() instanceof AtomicType) { // Set for StructType is forbidden
                        pw.println(MessageFormat.format(pref + "void set_{0}(int index, {1} v) '{' {3} {2}; '}'",
                                                        new Object[] {
                                                            name, jt,
                                                            tp.getJavaConversion("pData+"+stp.getOffset(tp)+" + index*" + elemSize, "v"),
                                                            s_log}));
                    }
                    // Returns pointer to the start of the array
                    pw.println(pref + "long get_" +name+ "() { "+s_log+"return pData+"+stp.getOffset(tp)+"; }");
                } else if (type == AtomicType.TYPE_PTR) {
                    pw.println(MessageFormat.format(pref + "{0} get_{1}(int index) '{' {3} return {2}; '}'",
                                         new Object[] {
                                             jt, name,
                                             tp.getJavaResult("index*" + elemSize, "Native.getLong(pData+"+stp.getOffset(tp)+")"),
                                             s_log
                                             }));
                    pw.println(pref + "long get_" +name+ "() { "+s_log+"return Native.getLong(pData+"+stp.getOffset(tp)+"); }");
                    pw.println(MessageFormat.format(pref + "void set_{0}({1} v) '{' {3} {2}; '}'",
                                                    new Object[] {name, "long", "Native.putLong(pData + " + stp.getOffset(tp) + ", v)", s_log}));
                    acc_size_32 += elemSize_32;
                    acc_size_64 += elemSize_64;
                } else {
                    acc_size_32 += elemSize_32;
                    acc_size_64 += elemSize_64;
                    pw.println(pref + tp.getJavaType() + " get_" +name +
                               "() { "+s_log+"return " + tp.getJavaResult(stp.getOffset(tp), null) + "; }");
                    if (type != AtomicType.TYPE_STRUCT) {
                        pw.println(MessageFormat.format(pref + "void set_{0}({1} v) '{' {3} {2}; '}'",
                                                        new Object[] {name, jt, tp.getJavaConversion("pData+"+stp.getOffset(tp), "v"), s_log}));
                    }
                }
                i++;
            }
        }
        if (s_size_32 != null && !s_size_32.equals(Integer.toString(acc_size_32))) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("32 bits: The size of the structure " + stp.getName() + " " + s_size_32 +
                        " is not equal to the accumulated size " +acc_size_32 + " of the fields");
            }
        } else if (s_size_64 != null && !s_size_64.equals(Integer.toString(acc_size_64))) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("64 bits: The size of the structure " + stp.getName() + " " +s_size_64+
                        " is not equal to the accumulated size " +acc_size_64+" of the fields");
            }
        }
    }

    public void writeWrapperSubclass(StructType stp, PrintWriter pw, boolean wide) {


        pw.println("class " + stp.getJavaClassName() + "AccessorImpl"  + " extends " + stp.getJavaClassName() + "Accessor  {");
        pw.println("/*\nThis class serves as a Wrapper for the following X Struct \nsThe offsets here are calculated based on actual compiler.\n\n" +stp.getDescription() + "\n\n */");

        writeAccessorImpls(stp, pw);

        pw.println("\n\n } \n\n");
    }

    public void writeWrapper(String outputDir, StructType stp)
    {
        if (stp.getNumFields() > 0) {

            try {
                FileOutputStream fs =  new FileOutputStream(outputDir + "/"+stp.getJavaClassName()+".java");
                PrintWriter pw = new PrintWriter(fs);
                pw.println("// This file is an automatically generated file, please do not edit this file, modify the WrapperGenerator.java file instead !\n" );

                pw.println("package "+package_name+";\n");
                pw.println("import jdk.internal.misc.Unsafe;\n");
                pw.println("import sun.util.logging.PlatformLogger;");
                String baseClass = stp.getBaseClass();
                if (baseClass == null) {
                    baseClass = defaultBaseClass;
                }
                if (stp.getIsInterface()) {
                    pw.print("public interface ");
                    pw.print(stp.getJavaClassName());
                } else {
                    pw.print("public class ");
                    pw.print(stp.getJavaClassName() + " extends " + baseClass);
                }
                if (stp.getInterfaces() != null) {
                    pw.print(" implements " + stp.getInterfaces());
                }
                pw.println(" { ");
                if (!stp.getIsInterface()) {
                    pw.println("\tprivate Unsafe unsafe = XlibWrapper.unsafe; ");
                    pw.println("\tprivate final boolean should_free_memory;");
                    pw.println("\tpublic static int getSize() { return " + stp.getSize() + "; }");
                    pw.println("\tpublic int getDataSize() { return getSize(); }");
                    pw.println("\n\tlong pData;");
                    pw.println("\n\tpublic long getPData() { return pData; }");

                    pw.println("\n\n\tpublic " + stp.getJavaClassName() + "(long addr) {");
                    if (generateLog) {
                        pw.println("\t\tlog.finest(\"Creating\");");
                    }
                    pw.println("\t\tpData=addr;");
                    pw.println("\t\tshould_free_memory = false;");
                    pw.println("\t}");
                    pw.println("\n\n\tpublic " + stp.getJavaClassName() + "() {");
                    if (generateLog) {
                        pw.println("\t\tlog.finest(\"Creating\");");
                    }
                    pw.println("\t\tpData = unsafe.allocateMemory(getSize());");
                    pw.println("\t\tshould_free_memory = true;");
                    pw.println("\t}");

                    pw.println("\n\n\tpublic void dispose() {");
                    if (generateLog) {
                        pw.println("\t\tlog.finest(\"Disposing\");");
                    }
                    pw.println("\t\tif (should_free_memory) {");
                    if (generateLog) {
                        pw.println("\t\t\tlog.finest(\"freeing memory\");");
                    }
                    pw.println("\t\t\tunsafe.freeMemory(pData); \n\t}");
                    pw.println("\t\t}");
                    writeAccessorImpls(stp, pw);
                    writeToString(stp,pw);
                } else {
                    pw.println("\n\n\tvoid dispose();");
                    pw.println("\n\tlong getPData();");
                    writeStubs(stp,pw);
                }


                pw.println("}\n\n\n");
                pw.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private boolean readSizeInfo(InputStream is, boolean wide) {
        String line;
        String splits[];
        BufferedReader in  = new BufferedReader(new InputStreamReader(is));
        try {
            while ((line = in.readLine()) != null)
            {
                splits = line.split("\\p{Space}");
                if (splits.length == 2)
                {
                    if (wide) {
                        sizeTable64bit.put(splits[0],splits[1]);
                    } else {
                        sizeTable32bit.put(splits[0],splits[1]);
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void writeFunctionCallWrapper(String outputDir, FunctionType ft) {
        try {
            FileOutputStream fs =  new FileOutputStream(outputDir + "/" + ft.getName()+".java");
            PrintWriter pw = new PrintWriter(fs);
            pw.println("// This file is an automatically generated file, please do not edit this file, modify the WrapperGenerator.java file instead !\n" );

            pw.println("package "+package_name+";\n");
            pw.println("import jdk.internal.misc.Unsafe;\n");
            pw.println("class " + ft.getName() + " {");
            pw.println("\tprivate static Unsafe unsafe = XlibWrapper.unsafe;");
            pw.println("\tprivate boolean __executed = false;");
            pw.println("\tprivate boolean __disposed = false;");
            Iterator<BaseType> iter = ft.getArguments().iterator();
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                if (at.isIn()) {
                    pw.println("\t" + at.getJavaType() + " _" + at.getName() + ";");
                } else {
                    pw.println("\tlong " + at.getName() + "_ptr = unsafe.allocateMemory(Native.get" + at.getTypeUpperCase() + "Size());");
                }
            }
            pw.println("\tpublic " + ft.getName() + "(");
            iter = ft.getArguments().iterator();
            boolean first = true;
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                if (at.isIn() || at.isInOut()) {
                    if (!first) {
                        pw.println(",");
                    }
                    first = false;
                    pw.print("\t\t" + at.getJavaType() + " " + at.getName());
                }
            }
            pw.println("\t)");
            pw.println("\t{");
            iter = ft.getArguments().iterator();
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                if (at.isIn() || at.isInOut()) {
                    pw.println("\t\tset_" + at.getName() + "(" + at.getName() + ");");
                }
            }
            pw.println("\t}");

            pw.println("\tpublic " + ft.getReturnType() + " execute() {");
            if (ft.isVoid()) {
                pw.println("\t\texecute(null);");
            } else {
                pw.println("\t\treturn execute(null);");
            }
            pw.println("\t}");

            pw.println("\tpublic " + ft.getReturnType() + " execute(XToolkit.XErrorHandler errorHandler) {");
            pw.println("\t\tif (__disposed) {");
            pw.println("\t\t    throw new IllegalStateException(\"Disposed\");");
            pw.println("\t\t}");
            pw.println("\t\tXToolkit.awtLock();");
            pw.println("\t\ttry {");
            pw.println("\t\t\tif (__executed) {");
            pw.println("\t\t\t    throw new IllegalStateException(\"Already executed\");");
            pw.println("\t\t\t}");
            pw.println("\t\t\t__executed = true;");
            pw.println("\t\t\tif (errorHandler != null) {");
            pw.println("\t\t\t    XErrorHandlerUtil.WITH_XERROR_HANDLER(errorHandler);");
            pw.println("\t\t\t}");
            iter = ft.getArguments().iterator();
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                if (!at.isIn() && at.isAutoFree()) {
                    pw.println("\t\t\tNative.put" + at.getTypeUpperCase() + "(" +at.getName() + "_ptr, 0);");
                }
            }
            if (!ft.isVoid()) {
                pw.println("\t\t\t" + ft.getReturnType() + " status = ");
            }
            pw.println("\t\t\tXlibWrapper." + ft.getName() + "(XToolkit.getDisplay(), ");
            iter = ft.getArguments().iterator();
            first = true;
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                if (!first) {
                    pw.println(",");
                }
                first = false;
                if (at.isIn()) {
                    pw.print("\t\t\t\tget_" + at.getName() + "()");
                } else {
                    pw.print("\t\t\t\t" + at.getName() + "_ptr");
                }
            }
            pw.println("\t\t\t);");
            pw.println("\t\t\tif (errorHandler != null) {");
            pw.println("\t\t\t    XErrorHandlerUtil.RESTORE_XERROR_HANDLER();");
            pw.println("\t\t\t}");
            if (!ft.isVoid()) {
                pw.println("\t\t\treturn status;");
            }
            pw.println("\t\t} finally {");
            pw.println("\t\t    XToolkit.awtUnlock();");
            pw.println("\t\t}");
            pw.println("\t}");

            pw.println("\tpublic boolean isExecuted() {");
            pw.println("\t    return __executed;");
            pw.println("\t}");
            pw.println("\t");
            pw.println("\tpublic boolean isDisposed() {");
            pw.println("\t    return __disposed;");
            pw.println("\t}");
            pw.println("\tpublic void finalize() {");
            pw.println("\t    dispose();");
            pw.println("\t}");

            pw.println("\tpublic void dispose() {");
            pw.println("\t\tXToolkit.awtLock();");
            pw.println("\t\ttry {");
            pw.println("\t\tif (__disposed || !__executed) {");
            pw.println("\t\t    return;");
            pw.println("\t\t} finally {");
            pw.println("\t\t    XToolkit.awtUnlock();");
            pw.println("\t\t}");
            pw.println("\t\t}");

            iter = ft.getArguments().iterator();
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                if (!at.isIn()) {
                    if (at.isAutoFree()) {
                        pw.println("\t\tif (__executed && get_" + at.getName() + "()!= 0) {");
                        pw.println("\t\t\tXlibWrapper.XFree(get_" + at.getName() + "());");
                        pw.println("\t\t}");
                    }
                    pw.println("\t\tunsafe.freeMemory(" + at.getName() + "_ptr);");
                }
            }
            pw.println("\t\t__disposed = true;");
            pw.println("\t\t}");
            pw.println("\t}");

            iter = ft.getArguments().iterator();
            while (iter.hasNext()) {
                AtomicType at = (AtomicType)iter.next();
                pw.println("\tpublic " + at.getJavaType() + " get_" + at.getName() + "() {");

                pw.println("\t\tif (__disposed) {");
                pw.println("\t\t    throw new IllegalStateException(\"Disposed\");");
                pw.println("\t\t}");
                pw.println("\t\tif (!__executed) {");
                pw.println("\t\t    throw new IllegalStateException(\"Not executed\");");
                pw.println("\t\t}");

                if (at.isIn()) {
                    pw.println("\t\treturn _" + at.getName() + ";");
                } else {
                    pw.println("\t\treturn Native.get" + at.getTypeUpperCase() + "(" + at.getName() + "_ptr);");
                }
                pw.println("\t}");

                pw.println("\tpublic void set_" + at.getName() + "(" + at.getJavaType() + " data) {");
                if (at.isIn()) {
                    pw.println("\t\t_" + at.getName() + " = data;");
                } else {
                    pw.println("\t\tNative.put" + at.getTypeUpperCase() + "(" + at.getName() + "_ptr, data);");
                }
                pw.println("\t}");
            }
            pw.println("}");
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeJavaWrapperClass(String outputDir) {
        try {
            for (Enumeration<BaseType> e = symbolTable.elements() ; e.hasMoreElements() ;) {
                BaseType tp = e.nextElement();
                if (tp instanceof StructType) {
                    StructType st = (StructType) tp;
                    writeWrapper(outputDir, st);
                } else if (tp instanceof FunctionType) {
                    writeFunctionCallWrapper(outputDir, (FunctionType)tp);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeNativeSizer(String file)
    {
        int type;
        int i=0;
        int j=0;
        BaseType tp;
        StructType stp;
        Enumeration<BaseType> eo;

        try {

            FileOutputStream fs =  new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fs);

            pw.println("/* This file is an automatically generated file, please do not edit this file, modify the XlibParser.java file instead !*/\n" );
            pw.println("#include <X11/Xlib.h>\n#include <X11/Xutil.h>\n#include <X11/Xos.h>\n#include <X11/Xatom.h>\n#include <stdio.h>\n");
            pw.println("#include <X11/extensions/Xdbe.h>");
            pw.println("#include <X11/XKBlib.h>");
            pw.println("#include \"awt_p.h\"");
            pw.println("#include \"color.h\"");
            pw.println("#include \"colordata.h\"");
            pw.println("\ntypedef struct\n");
            pw.println("{\n");
            pw.println("    unsigned long flags;\n");
            pw.println("    unsigned long functions;\n");
            pw.println("    unsigned long decorations;\n");
            pw.println("    long inputMode;\n");
            pw.println("    unsigned long status;\n");
            pw.println("} PropMwmHints;\n");

            pw.println("\n\nint main(){");
            j=0;
            for ( eo = symbolTable.elements() ; eo.hasMoreElements() ;) {
                tp = eo.nextElement();
                if (tp instanceof StructType)
                {
                    stp = (StructType) tp;
                    if (!stp.getIsInterface()) {
                        pw.println(stp.getName()+"  temp"+ j + ";\n");
                        j++;
                    }
                }
            }
            j=0;

            pw.println("printf(\"long\t%d\\n\",(int)sizeof(long));");
            pw.println("printf(\"int\t%d\\n\",(int)sizeof(int));");
            pw.println("printf(\"short\t%d\\n\",(int)sizeof(short));");
            pw.println("printf(\"ptr\t%d\\n\",(int)sizeof(void *));");
            pw.println("printf(\"Bool\t%d\\n\",(int)sizeof(Bool));");
            pw.println("printf(\"Atom\t%d\\n\",(int)sizeof(Atom));");
            pw.println("printf(\"Window\t%d\\n\",(int)sizeof(Window));");

            for (eo = symbolTable.elements() ; eo.hasMoreElements() ;) {


                tp = eo.nextElement();
                if (tp instanceof StructType)
                {
                    stp = (StructType) tp;
                    if (stp.getIsInterface()) {
                        continue;
                    }
                    for (Enumeration<BaseType> e = stp.getMembers() ; e.hasMoreElements() ;) {
                        AtomicType atp = (AtomicType) e.nextElement();
                        if (atp.isAlias()) continue;
                        pw.println("printf(\""+ stp.getName() + "." + atp.getName() + "\t%d\\n\""+
                                   ",(int)((unsigned long ) &temp"+j+"."+atp.getName()+"- (unsigned long ) &temp" + j + ")  );");

                        i++;


                    }
                    pw.println("printf(\""+ stp.getName() + "\t%d\\n\"" + ",(int)sizeof(temp"+j+"));");

                    j++;
                }

            }
            pw.println("return 0;");
            pw.println("}");
            pw.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void initTypes() {
        symbolTable.put("int", new AtomicType(AtomicType.TYPE_INT, "", "int"));
        symbolTable.put("short", new AtomicType(AtomicType.TYPE_SHORT, "", "short"));
        symbolTable.put("long", new AtomicType(AtomicType.TYPE_LONG, "", "long"));
        symbolTable.put("float", new AtomicType(AtomicType.TYPE_FLOAT, "", "float"));
        symbolTable.put("double", new AtomicType(AtomicType.TYPE_DOUBLE, "", "double"));
        symbolTable.put("Bool", new AtomicType(AtomicType.TYPE_BOOL, "", "Bool"));
        symbolTable.put("char", new AtomicType(AtomicType.TYPE_CHAR, "", "char"));
        symbolTable.put("byte", new AtomicType(AtomicType.TYPE_BYTE, "", "byte"));
        symbolTable.put("pointer", new AtomicType(AtomicType.TYPE_PTR, "", "pointer"));
        symbolTable.put("longlong", new AtomicType(AtomicType.TYPE_LONG_LONG, "", "longlong"));
        symbolTable.put("Atom", new AtomicType(AtomicType.TYPE_ATOM, "", "Atom"));
        symbolTable.put("ulong", new AtomicType(AtomicType.TYPE_ULONG, "", "ulong"));
    }

    public WrapperGenerator(String xlibFilename) {
        initTypes();
        try {
            BufferedReader in  = new BufferedReader(new FileReader(xlibFilename));
            String line;
            String splits[];
            BaseType curType = null;
            while ((line = in.readLine()) != null)
            {
                int commentStart = line.indexOf("//");
                if (commentStart >= 0) {
                    // remove comment
                    line = line.substring(0, commentStart);
                }

                if ("".equals(line)) {
                    // skip empty line
                    continue;
                }

                splits = line.split("\\p{Space}+");
                if (splits.length >= 2)
                {
                    String struct_name = curType.getName();
                    String field_name = splits[1];
                    String s_type = splits[2];
                    BaseType bt = curType;
                    int type = AtomicType.getTypeForString(s_type);
                    AtomicType atp = null;
                    if (bt != null && type != -1) {
                        atp = new AtomicType(type,field_name,s_type);
                        if (splits.length > 3) {
                            atp.setAttributes(splits);
                        }
                        if (bt instanceof StructType) {
                            StructType  stp = (StructType) bt;
                            stp.addMember(atp);
                        } else if (bt instanceof FunctionType) {
                            ((FunctionType)bt).addArgument(atp);
                        }
                    }
                    else if (bt == null) {
                        System.out.println("Cannot find " + struct_name);
                    }

                }
                else  if (line != null) {
                    BaseType bt = symbolTable.get(line);
                    if (bt == null) {
                        if (line.startsWith("!")) {
                            FunctionType ft = new FunctionType(line);
                            ft.setName(line);
                            symbolTable.put(ft.getName(),ft);
                            curType = ft;
                        } else {
                            StructType stp = new StructType(line);
                            stp.setName(line);
                            curType = stp;
                            symbolTable.put(stp.getName(),stp);
                        }
                    }
                }

            }
            in.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void makeSizer(String sizerFileName) {
        File fp = new File(sizerFileName);
        writeNativeSizer(fp.getAbsolutePath());
    }

    private boolean readFileSizeInfo(String filename, boolean wide) {
        try {
            boolean res = true;
            FileInputStream fis = new FileInputStream(filename);
            res = readSizeInfo(fis, wide);
            fis.close();
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startGeneration(String outputDir, String filename, boolean wide) {
        if (readFileSizeInfo(filename, wide))
        {
            writeJavaWrapperClass(outputDir);
        }
        else {
            System.out.println("Error calculating offsets");
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage:\nWrapperGenerator gen_java <output_dir> <xlibtypes.txt> <sizes-*.txt> <platform>");
            System.out.println("      or");
            System.out.println("WrapperGenerator gen_c_source <output_file> <xlibtypes.txt> <platform>");
            System.out.println("Where <platform>: 32, 64");

            System.exit(1);
        }

        WrapperGenerator xparser = new WrapperGenerator(args[2]);
        if (args[0].equals("gen_c_source")) {
            xparser.wide = args[3].equals("64");
            xparser.makeSizer(args[1]);
        } else if (args[0].equals("gen_java")) {
            boolean wide = args[4].equals("64");
            xparser.startGeneration(args[1], args[3], wide);
        }
    }
}
