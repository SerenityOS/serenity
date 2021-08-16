/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.jgss.spi;

import org.ietf.jgss.*;
import java.security.Provider;

/**
 * This interface is implemented by the factory class for every
 * plugin mechanism. The GSSManager locates an implementation of this
 * interface by querying the security providers installed on the
 * system. For a provider to support a mechanism defined by Oid x.y.z,
 * the provider master file would have to contain a mapping from the
 * property "GssApiMechanism.x.y.z" to an implementation class that serves
 * as the factory for that mechanism.
 * <p>
 * e.g., If a provider master file contained the a mapping from the
 * property "GssApiMechanism.1.2.840.113554.1.2.2" to the class name
 * "com.foo.krb5.Krb5GssFactory", then the GSS-API framework would assume
 * that com.foo.krb5.Krb5GssFactory implements the MechanismFactory
 * interface and that it can be used to obtain elements required by for
 * supporting this mechanism.
 *
 * @author Mayank Upadhyay
 */

public interface MechanismFactory {

    /**
     * Returns the Oid of the mechanism that this factory supports.
     * @return the Oid
     */
    public Oid getMechanismOid();

    /**
     * Returns the provider that this factory came from.
     * @return the provider
     */
    public Provider getProvider();

    /**
     * Returns the GSS-API nametypes that this mechanism can
     * support. Having this method helps the GSS-Framework decide quickly
     * if a certain mechanism can be skipped when importing a name.
     * @return an array of the Oid's corresponding to the different GSS-API
     * nametypes supported
     * @see org.ietf.jgss.GSSName
     */
    public Oid[] getNameTypes() throws GSSException;

    /**
     * Creates a credential element for this mechanism to be included as
     * part of a GSSCredential implementation. A GSSCredential is
     * conceptually a container class of several credential elements from
     * different mechanisms. A GSS-API credential can be used either for
     * initiating GSS security contexts or for accepting them. This method
     * also accepts parameters that indicate what usage is expected and how
     * long the life of the credential should be. It is not necessary that
     * the mechanism honor the request for lifetime. An application will
     * always query an acquired GSSCredential to determine what lifetime it
     * got back.<p>
     *
     * <b>Not all mechanisms support the concept of one credential element
     * that can be used for both initiating and accepting a context. In the
     * event that an application requests usage INITIATE_AND_ACCEPT for a
     * credential from such a mechanism, the GSS framework will need to
     * obtain two different credential elements from the mechanism, one
     * that will have usage INITIATE_ONLY and another that will have usage
     * ACCEPT_ONLY. The mechanism will help the GSS-API realize this by
     * returning a credential element with usage INITIATE_ONLY or
     * ACCEPT_ONLY prompting it to make another call to
     * getCredentialElement, this time with the other usage mode. The
     * mechanism indicates the missing mode by returning a 0 lifetime for
     * it.</b>
     *
     * @param name the mechanism level name element for the entity whose
     * credential is desired. A null value indicates that a mechanism
     * dependent default choice is to be made.
     * @param initLifetime indicates the lifetime (in seconds) that is
     * requested for this credential to be used at the context initiator's
     * end. This value should be ignored if the usage is
     * ACCEPT_ONLY. Predefined contants are available in the
     * org.ietf.jgss.GSSCredential interface.
     * @param acceptLifetime indicates the lifetime (in seconds) that is
     * requested for this credential to be used at the context acceptor's
     * end. This value should be ignored if the usage is
     * INITIATE_ONLY. Predefined contants are available in the
     * org.ietf.jgss.GSSCredential interface.
     * @param usage One of the values GSSCredential.INIATE_ONLY,
     * GSSCredential.ACCEPT_ONLY, and GSSCredential.INITIATE_AND_ACCEPT.
     * @see org.ietf.jgss.GSSCredential
     * @throws GSSException if one of the error situations described in RFC
     * 2743 with the GSS_Acquire_Cred or GSS_Add_Cred calls occurs.
     */
    public GSSCredentialSpi getCredentialElement(GSSNameSpi name,
      int initLifetime, int acceptLifetime, int usage) throws GSSException;

    /**
     * Creates a name element for this mechanism to be included as part of
     * a GSSName implementation. A GSSName is conceptually a container
     * class of several name elements from different mechanisms. A GSSName
     * can be created either with a String or with a sequence of
     * bytes. This factory method accepts the name in a String. Such a name
     * can generally be assumed to be printable and may be returned from
     * the name element's toString() method.
     *
     * @param nameStr a string containing the characters describing this
     * entity to the mechanism
     * @param nameType an Oid serving as a clue as to how the mechanism should
     * interpret the nameStr
     * @throws GSSException if any of the errors described in RFC 2743 for
     * the GSS_Import_Name or GSS_Canonicalize_Name calls occur.
     */
    public GSSNameSpi getNameElement(String nameStr, Oid nameType)
        throws GSSException;

    /**
     * This is a variation of the factory method that accepts a String for
     * the characters that make up the name. Usually the String characters
     * are assumed to be printable. The bytes passed in to this method have
     * to be converted to characters using some encoding of the mechanism's
     * choice. It is recommended that UTF-8 be used. (Note that UTF-8
     * preserves the encoding for 7-bit ASCII characters.)
     * <p>
     * An exported name will generally be passed in using this method.
     *
     * @param name the bytes describing this entity to the mechanism
     * @param nameType an Oid serving as a clue as to how the mechanism should
     * interpret the nameStr
     * @throws GSSException if any of the errors described in RFC 2743 for
     * the GSS_Import_Name or GSS_Canonicalize_Name calls occur.
     */
    public GSSNameSpi getNameElement(byte[] name, Oid nameType)
        throws GSSException;

    /**
     * Creates a security context for this mechanism so that it can be used
     * on the context initiator's side.
     *
     * @param peer the name element from this mechanism that represents the
     * peer
     * @param myInitiatorCred a credential element for the context
     * initiator obtained previously from this mechanism. The identity of
     * the context initiator can be obtained from this credential. Passing
     * a value of null here indicates that a default entity of the
     * mechanism's choice should be assumed to be the context initiator and
     * that default credentials should be applied.
     * @param lifetime the requested lifetime (in seconds) for the security
     * context. Predefined contants are available in the
     * org.ietf.jgss.GSSContext interface.
     * @throws GSSException if any of the errors described in RFC 2743 in
     * the GSS_Init_Sec_Context call occur.
     */
    public GSSContextSpi getMechanismContext(GSSNameSpi peer,
                                             GSSCredentialSpi myInitiatorCred,
                                             int lifetime) throws GSSException;

    /**
     * Creates a security context for this mechanism so thatit can be used
     * on the context acceptor's side.
     *
     * @param myAcceptorCred a credential element for the context acceptor
     * obtained previously from this mechanism. The identity of the context
     * acceptor cna be obtained from this credential. Passing a value of
     * null here indicates that tha default entity of the mechanism's
     * choice should be assumed to be the context acceptor and default
     * credentials should be applied.
     *
     * @throws GSSException if any of the errors described in RFC 2743 in
     * the GSS_Accept_Sec_Context call occur.
     */
    public GSSContextSpi getMechanismContext(GSSCredentialSpi myAcceptorCred)
        throws GSSException;

    /**
     * Creates a security context from a previously exported (serialized)
     * security context. Note that this is different from Java
     * serialization and is defined at a mechanism level to interoperate
     * over the wire with non-Java implementations. Either the initiator or
     * the acceptor can export and then import a security context.
     * Implementations of mechanism contexts are not required to implement
     * exporting and importing.
     *
     * @param exportedContext the bytes representing this security context
     * @throws GSSException is any of the errors described in RFC 2743 in
     * the GSS_Import_Sec_Context call occur.
     */
    public GSSContextSpi getMechanismContext(byte[] exportedContext)
        throws GSSException;

}
