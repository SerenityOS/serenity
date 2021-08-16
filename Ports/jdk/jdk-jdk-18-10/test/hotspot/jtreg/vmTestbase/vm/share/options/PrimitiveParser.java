/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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


package vm.share.options;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import nsk.share.TestBug;

/**
 * A utility class used to parse arguments of various primitive types.
 */
public class PrimitiveParser
{
    /**
     * Checks if the parser can handle passed type ("primitive/wrapper" or one-dim array of those).
     * Note that one-dim arrays of primitives/strings/wrappers are also supported.
     * @param type the type to parse against
     * @return true, if can parse.
     */
    public static boolean canHandle(Class<?> type)
    {

        if(type.isArray())
        {
            Class<?> compType = type.getComponentType();
            if(compType.isArray()) return false; // Cannot handle multidimensional arrays
            return canHandle(compType);
        }
        type = convertPrimitiveTypeToWrapper(type);
        return parsers.containsKey(type);
    }

    /**
     * A simple helper method.
     * @param type the type to check for
     * @return the parser to use, null if there is none.
     */
    private static PParser getParser(Class<?> type)
    {
        return parsers.get(convertPrimitiveTypeToWrapper(type));
    }

    /**
     * The main API method of this class.
     * @param param the parameter to parse
     * @param type parameter type to parse against
     * @return returns the object of a given type.
     * @throws vm.share.options.PrimitiveParser.ParserException
     */
    public static Object parse(String param, Class<?> type) throws ParserException
    {
        if(type.isArray())
        {
            Class<?> compType = type.getComponentType();
            if(compType.isArray())
              throw new ParserException("Cannot handle multidimensional arrays");

            if(!canHandle(compType))
                throw new ParserException("Unable to parse unknown array component type " + compType);

            String[] params = param.split(",");
            Object arr = Array.newInstance(compType, params.length);
            for (int i = 0; i < params.length; i++)
            {
                String par = params[i].trim();
                Array.set(arr, i, parse(par, compType));
            }
            return arr;
        }
        else
        {
            if(!canHandle(type))
                throw new ParserException("Unable to parse unknown type " + type);
            return getParser(type).parse(param);
        }
    }

    // I'm not sure, if generics are of any use here...
    static private abstract class PParser<T>
    {
        abstract T parse(String param) throws ParserException;

//        Class<T> getClassKey()
//        {
//            return PrimitiveParser.(Class<T>) T.getClass();
//        }
    }

    /**
     * Converts primitive types to corresponding wrapper classes.
     * We could register int.class, boolean.class etc in the hashtable instead.
     * (Or Integer.TYPE, etc.)
     * @param type to convert to wrapper
     * @return wrapper class or type if it is not primitive
     */
    public static Class<?> convertPrimitiveTypeToWrapper(Class<?> type)
    {
        if(!type.isPrimitive()) return type;
         Object arr = Array.newInstance(type, 1);
         Object v = Array.get(arr, 0);
         return v.getClass();
    }


    //"kind of state" machine stuff

    private static Map<Class<?>, PParser<?>> parsers;

    static
    {
        parsers = new HashMap<Class<?>, PrimitiveParser.PParser<?>>(16);
        parsers.put(Integer.class, new PParser<Integer>()
        {
            @Override Integer parse(String param) throws ParserException
            {
                if ( param.startsWith("0x") )
                    return Integer.parseInt(param.substring(2));
                else
                    return Integer.valueOf(param);
            }
        });
        parsers.put(Boolean.class, new PParser<Boolean>()
        {
            @Override Boolean parse(String param) throws ParserException
            {
                //special behavior for options
                if(param == null) return true;
                if(param.trim().length()==0) return true;
                return Boolean.valueOf(param);
            }
        });

        parsers.put(String.class, new PParser<String>()
        {
            @Override String parse(String param) throws ParserException
            {
                if(param == null) throw new ParserException(" Got null value string.");
                return param;
            }
        });


        parsers.put(Character.class, new PParser<Character>()
        {
            @Override Character parse(String param) throws ParserException
            {
               if(param.length()!=1)
                    throw new TestBug("Found Character type option of length != 1");
               return  Character.valueOf(param.charAt(0));
            }
        });

        parsers.put(Byte.class, new PParser<Byte>()
        {
            @Override Byte parse(String param) throws ParserException
            {
                if ( param.startsWith("0x") )
                    return Byte.parseByte(param.substring(2));
                else
                    return Byte.valueOf(param);
            }
        });

        parsers.put(Short.class, new PParser<Short>()
        {
            @Override Short parse(String param) throws ParserException
            {
                if ( param.startsWith("0x") )
                    return Short.parseShort(param.substring(2));
                else
                    return Short.valueOf(param);
            }
        });

        parsers.put(Long.class, new PParser<Long>()
        {
            @Override Long parse(String param) throws ParserException
            {
                if ( param.startsWith("0x") )
                    return Long.parseLong(param.substring(2));
                else
                    return Long.valueOf(param);
            }
        });

        parsers.put(Float.class, new PParser<Float>()
        {
            @Override Float parse(String param) throws ParserException
            {
                return Float.valueOf(param);
            }
        });

        parsers.put(Double.class, new PParser<Double>()
        {
            @Override Double parse(String param) throws ParserException
            {
                return Double.valueOf(param);
            }
        });
    }


/* Discussion
 * 1. It was proposed to use instead of the convertPrimitive the following
 *
 *   private static Map<Class<?>, Class<?>> wrapperClasses = new HashMap<Class<?>, Class<?>>();
 *
 *  so we could do    if(type.isPrimitive())
                type = wrapperClasses.get(type);
    static {
        wrapperClasses.put(boolean.class, Boolean.class);
        wrapperClasses.put(short.class, Short.class);
        wrapperClasses.put(int.class, Integer.class);
        wrapperClasses.put(Long.Type, Long.class);   // we can do it this way!
        wrapperClasses.put(float.class, Float.class);
        wrapperClasses.put(double.class, Double.class);
    }
 * The alternative is to register PParsers with corresponding Primitive type too.
 *
 * Also canHandle() could use
     return wrapperClasses.keySet().contains(type) || wrapperClasses.entrySet().contains(type);

 *  2. Parsing can be implemented via reflection
    return type.getMethod("valueOf", new Class[]{String.class}).invoke(null, string);

 *   I don't like using reflection as it prevents optimisation,
 *   also now Strings and Characters are handled in a nice fashion.
 *
 * As for convertToPrimitive trick both ways are good,
 * but current looks more generic though tricky
 */

//// some test, should it be commented out?
//    public static void main(String[] args)
//    {
//        try
//        {
//            String str = "0";
//            Object o = null;
//            str = "0";
//            o = parse(str, String.class);
//            System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
//
//            str = "0";
//            o = parse(str, int.class);
//            System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
//            str = "0";
//            o = parse(str, Integer.class);
//            System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
//
//            str = "0,1,2";
//            o = parse(str, int[].class);
//            System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + (int[]) o + "#");
//            System.out.println("DATA:" + java.util.Arrays.toString((int[])o));
//            //            System.out.println("DATA:" + java.util.Arrays.deepToString( (int[]) o));
//
//            str = "0";
//            o = parse(str, byte.class);
//            System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
//
//            str = "0";
//            o = parse(str, HashMap.class);
//            System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
////         String str = "0"; Object o = parsePrimitiveString(help_option, type); System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
//        } catch (ParserException ex)
//        {
//            System.out.println("" +ex);
//        }
////         String str = "0"; Object o = parsePrimitiveString(help_option, type); System.out.println("value:" + str + " type: " + o.getClass() + " value:#" + o + "#");
//
//    }

}
