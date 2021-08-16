/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import javax.management.AttributeNotFoundException;
import javax.management.BadAttributeValueExpException;
import javax.management.BadBinaryOpValueExpException;
import javax.management.BadStringOperationException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidApplicationException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.JMRuntimeException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.ServiceNotFoundException;
import javax.management.StringValueExp;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.XMLParseException;
import javax.management.monitor.MonitorSettingException;
import javax.management.openmbean.InvalidKeyException;
import javax.management.openmbean.InvalidOpenTypeException;
import javax.management.openmbean.KeyAlreadyExistsException;
import javax.management.openmbean.OpenDataException;
import javax.management.relation.InvalidRelationIdException;
import javax.management.relation.InvalidRelationServiceException;
import javax.management.relation.InvalidRelationTypeException;
import javax.management.relation.InvalidRoleInfoException;
import javax.management.relation.InvalidRoleValueException;
import javax.management.relation.RelationException;
import javax.management.relation.RelationNotFoundException;
import javax.management.relation.RelationServiceNotRegisteredException;
import javax.management.relation.RelationTypeNotFoundException;
import javax.management.relation.RoleInfoNotFoundException;
import javax.management.relation.RoleNotFoundException;
import javax.management.remote.JMXProviderException;
import javax.management.remote.JMXServerErrorException;

/**
 *  |----- Original Description Coming From Tonga Original Source Code -------|
 *  |                                                                         |
 *  | That class creates an ArrayList and fill it with an instance of each of |
 *  | the Exception class of the JMX API.                                     |
 *  | It's dedicated to use by ExceptionTest.                                 |
 *  |-------------------------------------------------------------------------|
 */
public class ExceptionFactory {

    public static final ArrayList<Exception> exceptions =
            new ArrayList<Exception>();

    static {
        String mes = "SQE";
        exceptions.add(new AttributeNotFoundException());
        exceptions.add(new BadAttributeValueExpException(mes));
        exceptions.add(new BadBinaryOpValueExpException(new StringValueExp(mes)));
        exceptions.add(new BadStringOperationException(mes));
        exceptions.add(new InstanceAlreadyExistsException());
        exceptions.add(new InstanceNotFoundException());
        exceptions.add(new IntrospectionException());
        exceptions.add(new InvalidApplicationException(mes));
        exceptions.add(new InvalidAttributeValueException());
        exceptions.add(new JMException());
        exceptions.add(new JMRuntimeException());
        exceptions.add(new ListenerNotFoundException());
        exceptions.add(new MalformedObjectNameException());
        exceptions.add(new MBeanException(new Exception(mes), mes));
        exceptions.add(new MBeanRegistrationException(new Exception(mes), mes));
        exceptions.add(new NotCompliantMBeanException());
        exceptions.add(new OperationsException());
        exceptions.add(new ReflectionException(new Exception(mes), mes));
        exceptions.add(new RuntimeErrorException(new Error(mes), mes));
        exceptions.add(new RuntimeMBeanException(new RuntimeException(mes), mes));
        exceptions.add(new RuntimeOperationsException(new RuntimeException(mes), mes));
        exceptions.add(new ServiceNotFoundException());
        exceptions.add(new InvalidTargetObjectTypeException());
        exceptions.add(new XMLParseException());
        exceptions.add(new MonitorSettingException());
        exceptions.add(new InvalidKeyException());
        exceptions.add(new InvalidOpenTypeException());
        exceptions.add(new KeyAlreadyExistsException());
        exceptions.add(new OpenDataException());
        exceptions.add(new InvalidRelationIdException());
        exceptions.add(new InvalidRelationServiceException());
        exceptions.add(new InvalidRelationTypeException());
        exceptions.add(new InvalidRoleInfoException());
        exceptions.add(new InvalidRoleValueException());
        exceptions.add(new RelationException());
        exceptions.add(new RelationNotFoundException());
        exceptions.add(new RelationServiceNotRegisteredException());
        exceptions.add(new RelationTypeNotFoundException());
        exceptions.add(new RoleInfoNotFoundException());
        exceptions.add(new RoleNotFoundException());
        exceptions.add(new JMXProviderException());
        exceptions.add(new JMXServerErrorException(mes, new Error(mes)));
        ExceptionTest.Utils.debug(ExceptionTest.Utils.DEBUG_STANDARD,
                "DataFactory::updateMap: Initialized" +
                " an ArrayList with the " +
                exceptions.size() + " exceptions of the JMX API");
    }
}
