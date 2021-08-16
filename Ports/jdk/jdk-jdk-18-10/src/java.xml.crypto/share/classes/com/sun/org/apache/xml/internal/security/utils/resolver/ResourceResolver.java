/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.utils.resolver;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.utils.JavaUtils;
import com.sun.org.apache.xml.internal.security.utils.resolver.implementations.ResolverDirectHTTP;
import com.sun.org.apache.xml.internal.security.utils.resolver.implementations.ResolverFragment;
import com.sun.org.apache.xml.internal.security.utils.resolver.implementations.ResolverLocalFilesystem;
import com.sun.org.apache.xml.internal.security.utils.resolver.implementations.ResolverXPointer;

/**
 * During reference validation, we have to retrieve resources from somewhere.
 * This is done by retrieving a Resolver. The resolver needs two arguments: The
 * URI in which the link to the new resource is defined and the baseURI of the
 * file/entity in which the URI occurs (the baseURI is the same as the SystemId).
 */
public class ResourceResolver {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(ResourceResolver.class);

    /** these are the system-wide resolvers */
    private static final List<ResourceResolverSpi> resolverList = new CopyOnWriteArrayList<>();

    private static final AtomicBoolean defaultResolversAdded = new AtomicBoolean();

    /**
     * Registers a ResourceResolverSpi class.
     *
     * @param className the name of the ResourceResolverSpi class to be registered
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register a resource resolver
     */
    @SuppressWarnings("unchecked")
    public static void register(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        JavaUtils.checkRegisterPermission();
        Class<ResourceResolverSpi> resourceResolverClass =
            (Class<ResourceResolverSpi>)
            ClassLoaderUtils.loadClass(className, ResourceResolver.class);
        register(JavaUtils.newInstanceWithEmptyConstructor(resourceResolverClass), false);
    }

    /**
     * Registers a ResourceResolverSpi class at the beginning of the provider list.
     *
     * @param className the name of the ResourceResolverSpi class to be registered
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register a resource resolver
     */
    @SuppressWarnings("unchecked")
    public static void registerAtStart(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        JavaUtils.checkRegisterPermission();
        Class<ResourceResolverSpi> resourceResolverClass =
            (Class<ResourceResolverSpi>)
            ClassLoaderUtils.loadClass(className, ResourceResolver.class);
        register(JavaUtils.newInstanceWithEmptyConstructor(resourceResolverClass), true);
    }

    /**
     * Registers a ResourceResolverSpi instance.
     * @param resourceResolverSpi
     * @param start
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register a resource resolver
     */
    public static void register(ResourceResolverSpi resourceResolverSpi, boolean start) {
        JavaUtils.checkRegisterPermission();
        if (start) {
            resolverList.add(0, resourceResolverSpi);
        } else {
            resolverList.add(resourceResolverSpi);
        }
        LOG.debug("Registered resolver: {}", resourceResolverSpi.toString());
    }

    /**
     * Registers a list of ResourceResolverSpi classes.
     *
     * @param classNames
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws SecurityException if a security manager is installed and the
     *    caller does not have permission to register the key resolver
     */
    public static void registerClassNames(List<String> classNames)
        throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        JavaUtils.checkRegisterPermission();

        List<ResourceResolverSpi> resourceResolversToAdd = new ArrayList<>(classNames.size());
        for (String className : classNames) {
            ResourceResolverSpi resourceResolverSpi = (ResourceResolverSpi)JavaUtils
                    .newInstanceWithEmptyConstructor(ClassLoaderUtils.loadClass(className, ResourceResolver.class));
            resourceResolversToAdd.add(resourceResolverSpi);
        }
        resolverList.addAll(resourceResolversToAdd);
    }

    /**
     * This method registers the default resolvers.
     */
    public static void registerDefaultResolvers() {
        // Add a guard so that we don't repeatedly add the default resolvers
        if (defaultResolversAdded.compareAndSet(false, true)) {
            List<ResourceResolverSpi> resourceResolversToAdd = new ArrayList<>();
            resourceResolversToAdd.add(new ResolverFragment());
            resourceResolversToAdd.add(new ResolverLocalFilesystem());
            resourceResolversToAdd.add(new ResolverXPointer());
            resourceResolversToAdd.add(new ResolverDirectHTTP());

            resolverList.addAll(resourceResolversToAdd);
        }
    }

    /**
     * Method resolve
     *
     * @param context
     * @return the resource
     *
     * @throws ResourceResolverException
     */
    public static XMLSignatureInput resolve(ResourceResolverContext context)
        throws ResourceResolverException {
        for (ResourceResolverSpi resolver : resolverList) {
            LOG.debug("check resolvability by class {}", resolver.getClass().getName());

            if (resolver.engineCanResolveURI(context)) {
                // Check to see whether the Resolver is allowed
                if (context.secureValidation
                    && (resolver instanceof ResolverLocalFilesystem
                        || resolver instanceof ResolverDirectHTTP)) {
                    Object[] exArgs = { resolver.getClass().getName() };
                    throw new ResourceResolverException(
                        "signature.Reference.ForbiddenResolver", exArgs, context.uriToResolve, context.baseUri
                    );
                }
                return resolver.engineResolveURI(context);
            }
        }

        Object[] exArgs = { context.uriToResolve != null
                ? context.uriToResolve : "null", context.baseUri };

        throw new ResourceResolverException("utils.resolver.noClass", exArgs, context.uriToResolve, context.baseUri);
    }

    /**
     * Method resolve
     *
     * @param individualResolvers
     * @param context
     * @return the resource
     *
     * @throws ResourceResolverException
     */
    public static XMLSignatureInput resolve(
        List<ResourceResolverSpi> individualResolvers, ResourceResolverContext context
    ) throws ResourceResolverException {
        LOG.debug(
            "I was asked to create a ResourceResolver and got {}",
            individualResolvers == null ? 0 : individualResolvers.size()
        );

        // first check the individual Resolvers
        if (individualResolvers != null) {
            for (ResourceResolverSpi resolver : individualResolvers) {
                String currentClass = resolver.getClass().getName();
                LOG.debug("check resolvability by class {}", currentClass);

                if (resolver.engineCanResolveURI(context)) {
                    return resolver.engineResolveURI(context);
                }
            }
        }

        return resolve(context);
    }
}
