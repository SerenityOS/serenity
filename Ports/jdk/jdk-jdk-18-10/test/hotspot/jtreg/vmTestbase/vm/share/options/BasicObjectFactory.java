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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package vm.share.options;

import java.util.HashMap;
import java.util.Map;
import nsk.share.TestBug;

/**
 * This class allows user to create an {@link ObjectFactory} implementation
 * via @{@link vm.share.options.Factory} annotation. See the source code of
 * {@link vm.share.options.test.BasicObjectFactoryUsageExample} and
 * {@link vm.share.options.test.ExampleWithNonprimitiveOptions}.
 * @see Factory
 * @see FClass
 * @see ObjectFactory
 */
public class BasicObjectFactory<T> implements ObjectFactory<T>
{
    public String getPlaceholder()
    {
        return getAnnotaion().placeholder_text();
    }

    public String[] getPossibleValues()
    {
        return getTypesMap().keySet().toArray(new String[0]);
    }

    public String getDescription()
    {
        return getAnnotaion().description().equals(Factory.defDescription)? null : getAnnotaion().description();
    }

    public String getDefaultValue()
    {
        return getAnnotaion().default_value().equals(Factory.defDefaultValue)? null :
                getAnnotaion().default_value();
    }

    public String getParameterDescription(String key)
    {
        return getTypesMap().get(key).description();
    }


    // shouldn't value be named key?

    public T getObject(String value)
    {
        try
        {
            @SuppressWarnings(value="unchecked")
            T result = (T) getTypesMap().get(value).type().newInstance();
            return result;
        } catch (InstantiationException ex)
        {
            throw new TestBug("Error while trying to instantiate via " + this.getClass() + " for key " + value, ex);
        } catch (IllegalAccessException ex)
        {
            throw new TestBug("Error while trying to instantiate via " + this.getClass() + " for key " + value, ex);
        }
    }

    protected Factory getAnnotaion()
    {
        if(!this.getClass().isAnnotationPresent(Factory.class))
            throw new TestBug(" Found an unnotated BasicObjectFactory subclass.");
        Factory factoryAnn = this.getClass().getAnnotation(Factory.class);
        return factoryAnn;
    }

    protected Map<String, FClass> getTypesMap()
    {   // probably there could be some lazy initialization, but I decided not to deal with that.
        FClass[] types = getAnnotaion().classlist();
        Map<String, FClass> typesMap = new HashMap<String, FClass>(types.length);
        for (FClass type : types)
        {
            typesMap.put(type.key(), type);
        }
        return typesMap;

    }

    // see ExampleWithNonprimitiveOptions instead.
//    public void test()
//    {
//        if(!this.getClass().isAnnotationPresent(Factory.class))
//            throw new RuntimeException(" Found an unnotated BasicObjectFactory subclass.");
//        Factory factoryAnn = this.getClass().getAnnotation(Factory.class);
//        System.out.println(" placeholder_text " + factoryAnn.placeholder_text());
//        System.out.println(" default_value  " +
//                (factoryAnn.default_value().equals(Factory.defDefault_value) ? null : factoryAnn.default_value()) );
//
//    }
//
//    @Factory(placeholder_text="number", default_value="test",
//    classlist = {
//    @FClass( key="int", description="integer", type=int.class),
//    @FClass( key="boolean", description="boolean", type=boolean.class)
//    } )
//    public static class testOF extends BasicObjectFactory<BasicObjectFactory> {}
//
////    @Factory(placeholder_text="placehldr1")
////    public static class testOF1 extends BasicObjectFactory<BasicObjectFactory> {}
//
//
//    public static void main(String[] args)
//    {
//        BasicObjectFactory bof = new testOF();
//        bof.test();
////        new testOF1().test();
//
//    }

}
