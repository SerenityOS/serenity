/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.security.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * This class is for security permissions. A {@code SecurityPermission}
 * contains a name (also referred to as a "target name") but no actions list;
 * you either have the named permission or you don't.
 * <p>
 * The target name is the name of a security configuration parameter
 * (see below). Currently the {@code SecurityPermission} object is used to
 * guard access to the {@link AccessControlContext}, {@link Policy},
 * {@link Provider}, {@link Security}, {@link Signer}, and {@link Identity}
 * objects.
 * <p>
 * The following table lists the standard {@code SecurityPermission}
 * target names, and for each provides a description of what the permission
 * allows and a discussion of the risks of granting code the permission.
 *
 * <table class="striped">
 * <caption style="display:none">target name, what the permission allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 * <tbody>
 *
 * <tr>
 *   <th scope="row">authProvider.{provider name}</th>
 *   <td>Allow the named provider to be an AuthProvider for login and
 * logout operations. </td>
 *   <td>This allows the named provider to perform login and logout
 * operations. The named provider must extend {@code AuthProvider}
 * and care must be taken to grant to a trusted provider since
 * login operations involve sensitive authentication information
 * such as PINs and passwords. </td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">createAccessControlContext</th>
 *   <td>Creation of an AccessControlContext</td>
 *   <td>This allows someone to instantiate an AccessControlContext
 * with a {@code DomainCombiner}.  Extreme care must be taken when
 * granting this permission. Malicious code could create a DomainCombiner
 * that augments the set of permissions granted to code, and even grant the
 * code {@link java.security.AllPermission}.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getDomainCombiner</th>
 *   <td>Retrieval of an AccessControlContext's DomainCombiner</td>
 *   <td>This allows someone to retrieve an AccessControlContext's
 * {@code DomainCombiner}.  Since DomainCombiners may contain
 * sensitive information, this could potentially lead to a privacy leak.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getPolicy</th>
 *   <td>Retrieval of the system-wide security policy (specifically, of the
 * currently-installed Policy object)</td>
 *   <td>This allows someone to query the policy via the
 * {@code getPermissions} call,
 * which discloses which permissions would be granted to a given CodeSource.
 * While revealing the policy does not compromise the security of
 * the system, it does provide malicious code with additional information
 * which it may use to better aim an attack. It is wise
 * not to divulge more information than necessary.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setPolicy</th>
 *   <td>Setting of the system-wide security policy (specifically,
 * the Policy object)</td>
 *   <td>Granting this permission is extremely dangerous, as malicious
 * code may grant itself all the necessary permissions it needs
 * to successfully mount an attack on the system.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">createPolicy.{policy type}</th>
 *   <td>Getting an instance of a Policy implementation from a provider</td>
 *   <td>Granting this permission enables code to obtain a Policy object.
 * Malicious code may query the Policy object to determine what permissions
 * have been granted to code other than itself.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">getProperty.{key}</th>
 *   <td>Retrieval of the security property with the specified key</td>
 *   <td>Depending on the particular key for which access has
 * been granted, the code may have access to the list of security
 * providers, as well as the location of the system-wide and user
 * security policies.  while revealing this information does not
 * compromise the security of the system, it does provide malicious
 * code with additional information which it may use to better aim
 * an attack.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setProperty.{key}</th>
 *   <td>Setting of the security property with the specified key</td>
 *   <td>This could include setting a security provider or defining
 * the location of the system-wide security policy.  Malicious
 * code that has permission to set a new security provider may
 * set a rogue provider that steals confidential information such
 * as cryptographic private keys. In addition, malicious code with
 * permission to set the location of the system-wide security policy
 * may point it to a security policy that grants the attacker
 * all the necessary permissions it requires to successfully mount
 * an attack on the system.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">insertProvider</th>
 *   <td>Addition of a new provider</td>
 *   <td>This would allow somebody to introduce a possibly
 * malicious provider (e.g., one that discloses the private keys passed
 * to it) as the highest-priority provider. This would be possible
 * because the Security object (which manages the installed providers)
 * currently does not check the integrity or authenticity of a provider
 * before attaching it. The "insertProvider" permission subsumes the
 * "insertProvider.{provider name}" permission (see the section below for
 * more information).</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">removeProvider.{provider name}</th>
 *   <td>Removal of the specified provider</td>
 *   <td>This may change the behavior or disable execution of other
 * parts of the program. If a provider subsequently requested by the
 * program has been removed, execution may fail. Also, if the removed
 * provider is not explicitly requested by the rest of the program, but
 * it would normally be the provider chosen when a cryptography service
 * is requested (due to its previous order in the list of providers),
 * a different provider will be chosen instead, or no suitable provider
 * will be found, thereby resulting in program failure.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">clearProviderProperties.{provider name}</th>
 *   <td>"Clearing" of a Provider so that it no longer contains the properties
 * used to look up services implemented by the provider</td>
 *   <td>This disables the lookup of services implemented by the provider.
 * This may thus change the behavior or disable execution of other
 * parts of the program that would normally utilize the Provider, as
 * described under the "removeProvider.{provider name}" permission.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">putProviderProperty.{provider name}</th>
 *   <td>Setting of properties for the specified Provider</td>
 *   <td>The provider properties each specify the name and location
 * of a particular service implemented by the provider. By granting
 * this permission, you let code replace the service specification
 * with another one, thereby specifying a different implementation.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">removeProviderProperty.{provider name}</th>
 *   <td>Removal of properties from the specified Provider</td>
 *   <td>This disables the lookup of services implemented by the
 * provider. They are no longer accessible due to removal of the properties
 * specifying their names and locations. This
 * may change the behavior or disable execution of other
 * parts of the program that would normally utilize the Provider, as
 * described under the "removeProvider.{provider name}" permission.</td>
 * </tr>
 *
 * </tbody>
 * </table>
 *
 * <P>
 * The following permissions have been superseded by newer permissions or are
 * associated with classes that have been deprecated: {@link Identity},
 * {@link IdentityScope}, {@link Signer}. Use of them is discouraged. See the
 * applicable classes for more information.
 *
 * <table class="striped">
 * <caption style="display:none">target name, what the permission allows, and associated risks</caption>
 * <thead>
 * <tr>
 * <th scope="col">Permission Target Name</th>
 * <th scope="col">What the Permission Allows</th>
 * <th scope="col">Risks of Allowing this Permission</th>
 * </tr>
 * </thead>
 *
 * <tbody>
 * <tr>
 *   <th scope="row">insertProvider.{provider name}</th>
 *   <td>Addition of a new provider, with the specified name</td>
 *   <td>Use of this permission is discouraged from further use because it is
 * possible to circumvent the name restrictions by overriding the
 * {@link java.security.Provider#getName} method. Also, there is an equivalent
 * level of risk associated with granting code permission to insert a provider
 * with a specific name, or any name it chooses. Users should use the
 * "insertProvider" permission instead.
 * <p>This would allow somebody to introduce a possibly
 * malicious provider (e.g., one that discloses the private keys passed
 * to it) as the highest-priority provider. This would be possible
 * because the Security object (which manages the installed providers)
 * currently does not check the integrity or authenticity of a provider
 * before attaching it.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setSystemScope</th>
 *   <td>Setting of the system identity scope</td>
 *   <td>This would allow an attacker to configure the system identity scope with
 * certificates that should not be trusted, thereby granting applet or
 * application code signed with those certificates privileges that
 * would have been denied by the system's original identity scope.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setIdentityPublicKey</th>
 *   <td>Setting of the public key for an Identity</td>
 *   <td>If the identity is marked as "trusted", this allows an attacker to
 * introduce a different public key (e.g., its own) that is not trusted
 * by the system's identity scope, thereby granting applet or
 * application code signed with that public key privileges that
 * would have been denied otherwise.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setIdentityInfo</th>
 *   <td>Setting of a general information string for an Identity</td>
 *   <td>This allows attackers to set the general description for
 * an identity.  This may trick applications into using a different
 * identity than intended or may prevent applications from finding a
 * particular identity.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">addIdentityCertificate</th>
 *   <td>Addition of a certificate for an Identity</td>
 *   <td>This allows attackers to set a certificate for
 * an identity's public key.  This is dangerous because it affects
 * the trust relationship across the system. This public key suddenly
 * becomes trusted to a wider audience than it otherwise would be.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">removeIdentityCertificate</th>
 *   <td>Removal of a certificate for an Identity</td>
 *   <td>This allows attackers to remove a certificate for
 * an identity's public key. This is dangerous because it affects
 * the trust relationship across the system. This public key suddenly
 * becomes considered less trustworthy than it otherwise would be.</td>
 * </tr>
 *
 * <tr>
 *  <th scope="row">printIdentity</th>
 *  <td>Viewing the name of a principal
 * and optionally the scope in which it is used, and whether
 * or not it is considered "trusted" in that scope</td>
 *  <td>The scope that is printed out may be a filename, in which case
 * it may convey local system information. For example, here's a sample
 * printout of an identity named "carol", who is
 * marked not trusted in the user's identity database:<br>
 *   carol[/home/luehe/identitydb.obj][not trusted]</td>
 *</tr>
 *
 * <tr>
 *   <th scope="row">getSignerPrivateKey</th>
 *   <td>Retrieval of a Signer's private key</td>
 *   <td>It is very dangerous to allow access to a private key; private
 * keys are supposed to be kept secret. Otherwise, code can use the
 * private key to sign various files and claim the signature came from
 * the Signer.</td>
 * </tr>
 *
 * <tr>
 *   <th scope="row">setSignerKeyPair</th>
 *   <td>Setting of the key pair (public key and private key) for a Signer</td>
 *   <td>This would allow an attacker to replace somebody else's (the "target's")
 * keypair with a possibly weaker keypair (e.g., a keypair of a smaller
 * keysize).  This also would allow the attacker to listen in on encrypted
 * communication between the target and its peers. The target's peers
 * might wrap an encryption session key under the target's "new" public
 * key, which would allow the attacker (who possesses the corresponding
 * private key) to unwrap the session key and decipher the communication
 * data encrypted under that session key.</td>
 * </tr>
 *
 * </tbody>
 * </table>
 *
 * @implNote
 * Implementations may define additional target names, but should use naming
 * conventions such as reverse domain name notation to avoid name clashes.
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 * @since 1.2
 */

public final class SecurityPermission extends BasicPermission {

    @java.io.Serial
    private static final long serialVersionUID = 5236109936224050470L;

    /**
     * Creates a new SecurityPermission with the specified name.
     * The name is the symbolic name of the SecurityPermission. An asterisk
     * may appear at the end of the name, following a ".", or by itself, to
     * signify a wildcard match.
     *
     * @param name the name of the SecurityPermission
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */
    public SecurityPermission(String name)
    {
        super(name);
    }

    /**
     * Creates a new SecurityPermission object with the specified name.
     * The name is the symbolic name of the SecurityPermission, and the
     * actions String is currently unused and should be null.
     *
     * @param name the name of the SecurityPermission
     * @param actions should be null.
     *
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is empty.
     */
    public SecurityPermission(String name, String actions)
    {
        super(name, actions);
    }
}
