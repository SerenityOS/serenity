/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8015081
 * @modules java.management
 *          java.security.jgss
 * @compile Subject.java
 * @compile SubjectNullTests.java
 * @build SubjectNullTests
 * @run main SubjectNullTests
 * @summary javax.security.auth.Subject.toString() throws NPE
 */

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.kerberos.KerberosPrincipal;

public class SubjectNullTests {

    // Value templates for the constructor
    private static Principal[] princVals = {
        new X500Principal("CN=Tom Sawyer, ST=Missouri, C=US"),
        new JMXPrincipal("Huckleberry Finn"),
        new KerberosPrincipal("mtwain/author@LITERATURE.US")
    };
    private static String[] pubVals = {"tsawyer", "hfinn", "mtwain"};
    private static String[] privVals = {"th3R!v3r", "oNth3R4ft", "5Cl3M3nz"};

    // Templates for collection-based modifiers for the Subject
    private static Principal[] tmplAddPrincs = {
        new X500Principal("CN=John Doe, O=Bogus Corp."),
        new KerberosPrincipal("jdoe/admin@BOGUSCORP.COM")
    };
    private static String[] tmplAddPubVals = {"jdoe", "djoe"};
    private static String[] tmplAddPrvVals = {"b4dpa55w0rd", "pass123"};

    /**
     * Byte arrays used for deserialization:
     * These byte arrays contain serialized Subjects and SecureSets,
     * either with or without nulls.  These use
     * jjjjj.security.auth.Subject, which is a modified Subject
     * implementation that allows the addition of null elements
     */
    private static final byte[] SUBJ_NO_NULL =
        jjjjj.security.auth.Subject.enc(makeSubjWithNull(false));
    private static final byte[] SUBJ_WITH_NULL =
        jjjjj.security.auth.Subject.enc(makeSubjWithNull(true));
    private static final byte[] PRIN_NO_NULL =
        jjjjj.security.auth.Subject.enc(makeSecSetWithNull(false));
    private static final byte[] PRIN_WITH_NULL =
        jjjjj.security.auth.Subject.enc(makeSecSetWithNull(true));

    /**
     * Method to allow creation of a subject that can optionally
     * insert a null reference into the principals Set.
     */
    private static jjjjj.security.auth.Subject makeSubjWithNull(
            boolean nullPrinc) {
        Set<Principal> setPrinc = new HashSet<>(Arrays.asList(princVals));
        if (nullPrinc) {
            setPrinc.add(null);
        }

        return (new jjjjj.security.auth.Subject(setPrinc));
    }

    /**
     * Method to allow creation of a SecureSet that can optionally
     * insert a null reference.
     */
    private static Set<Principal> makeSecSetWithNull(boolean nullPrinc) {
        Set<Principal> setPrinc = new HashSet<>(Arrays.asList(princVals));
        if (nullPrinc) {
            setPrinc.add(null);
        }

        jjjjj.security.auth.Subject subj =
            new jjjjj.security.auth.Subject(setPrinc);

        return subj.getPrincipals();
    }

    /**
     * Construct a subject, and optionally place a null in any one
     * of the three Sets used to initialize a Subject's values
     */
    private static Subject makeSubj(boolean nullPrinc, boolean nullPub,
                             boolean nullPriv) {
        Set<Principal> setPrinc = new HashSet<>(Arrays.asList(princVals));
        Set<String> setPubCreds = new HashSet<>(Arrays.asList(pubVals));
        Set<String> setPrvCreds = new HashSet<>(Arrays.asList(privVals));

        if (nullPrinc) {
            setPrinc.add(null);
        }

        if (nullPub) {
            setPubCreds.add(null);
        }

        if (nullPriv) {
            setPrvCreds.add(null);
        }

        return (new Subject(false, setPrinc, setPubCreds, setPrvCreds));
    }

    /**
     * Provide a simple interface for abstracting collection-on-collection
     * functions
     */
    public interface Function {
        boolean execCollection(Set<?> subjSet, Collection<?> actorData);
    }

    public static final Function methAdd = new Function() {
        @SuppressWarnings("unchecked")
        @Override
        public boolean execCollection(Set<?> subjSet, Collection<?> actorData) {
            return subjSet.addAll((Collection)actorData);
        }
    };

    public static final Function methContains = new Function() {
        @Override
        public boolean execCollection(Set<?> subjSet, Collection<?> actorData) {
            return subjSet.containsAll(actorData);
        }
    };

    public static final Function methRemove = new Function() {
        @Override
        public boolean execCollection(Set<?> subjSet, Collection<?> actorData) {
            return subjSet.removeAll(actorData);
        }
    };

    public static final Function methRetain = new Function() {
        @Override
        public boolean execCollection(Set<?> subjSet, Collection<?> actorData) {
            return subjSet.retainAll(actorData);
        }
    };

    /**
     * Run a test using a specified Collection method upon a Subject's
     * SecureSet fields. This method expects NullPointerExceptions
     * to be thrown, and throws RuntimeException when the operation
     * succeeds
     */
    private static void nullTestCollection(Function meth, Set<?> subjSet,
           Collection<?> actorData) {
        try {
            meth.execCollection(subjSet, actorData);
            throw new RuntimeException("Failed to throw NullPointerException");
        } catch (NullPointerException npe) {
            System.out.println("Caught expected NullPointerException [PASS]");
        }
    }

    /**
     * Run a test using a specified Collection method upon a Subject's
     * SecureSet fields. This method expects the function and arguments
     * passed in to complete without exception.  It returns false
     * if either an exception occurs or the result of the operation is
     * false.
     */
    private static boolean validTestCollection(Function meth, Set<?> subjSet,
           Collection<?> actorData) {
        boolean result = false;

        try {
            result = meth.execCollection(subjSet, actorData);
        } catch (Exception exc) {
            System.out.println("Caught exception " + exc);
        }

        return result;
    }

    /**
     * Deserialize an object from a byte array.
     *
     * @param type The {@code Class} that the serialized file is supposed
     *             to contain.
     * @param serBuffer The byte array containing the serialized object data
     *
     * @return An object of the type specified in the {@code type} parameter
     */
    private static <T> T deserializeBuffer(Class<T> type, byte[] serBuffer)
            throws IOException, ClassNotFoundException {

        ByteArrayInputStream bis = new ByteArrayInputStream(serBuffer);
        ObjectInputStream ois = new ObjectInputStream(bis);

        T newObj = type.cast(ois.readObject());
        ois.close();
        bis.close();

        return newObj;
    }

    private static void testCTOR() {
        System.out.println("------ constructor ------");

        try {
            // Case 1: Create a subject with a null principal
            // Expected result: NullPointerException
            Subject mtSubj = makeSubj(true, false, false);
            throw new RuntimeException(
                    "constructor [principal w/ null]: Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println("constructor [principal w/ null]: " +
                    "NullPointerException [PASS]");
        }

        try {
            // Case 2: Create a subject with a null public credential element
            // Expected result: NullPointerException
            Subject mtSubj = makeSubj(false, true, false);
            throw new RuntimeException(
                    "constructor [pub cred w/ null]: Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println("constructor [pub cred w/ null]: " +
                    "NullPointerException [PASS]");
        }

        try {
            // Case 3: Create a subject with a null private credential element
            // Expected result: NullPointerException
            Subject mtSubj = makeSubj(false, false, true);
            throw new RuntimeException(
                    "constructor [priv cred w/ null]: Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println("constructor [priv cred w/ null]: " +
                    "NullPointerException [PASS]");
        }

        // Case 4: Create a new subject using the principals, public
        // and private credentials from another well-formed subject
        // Expected result: Successful construction
        Subject srcSubj = makeSubj(false, false, false);
        Subject mtSubj = new Subject(false, srcSubj.getPrincipals(),
                srcSubj.getPublicCredentials(),
                srcSubj.getPrivateCredentials());
        System.out.println("Construction from another well-formed Subject's " +
                "principals/creds [PASS]");
    }

    @SuppressWarnings("unchecked")
    private static void testDeserialize() throws Exception {
        System.out.println("------ deserialize -----");

        Subject subj = null;
        Set<Principal> prin = null;

        // Case 1: positive deserialization test of a Subject
        // Expected result: well-formed Subject
        subj = deserializeBuffer(Subject.class, SUBJ_NO_NULL);
        System.out.println("Positive deserialization test (Subject) passed");

        // Case 2: positive deserialization test of a SecureSet
        // Expected result: well-formed Set
        prin = deserializeBuffer(Set.class, PRIN_NO_NULL);
        System.out.println("Positive deserialization test (SecureSet) passed");

        System.out.println(
                "* Testing deserialization with null-poisoned objects");
        // Case 3: deserialization test of a null-poisoned Subject
        // Expected result: NullPointerException
        try {
            subj = deserializeBuffer(Subject.class, SUBJ_WITH_NULL);
            throw new RuntimeException("Failed to throw NullPointerException");
        } catch (NullPointerException npe) {
            System.out.println("Caught expected NullPointerException [PASS]");
        }

        // Case 4: deserialization test of a null-poisoned SecureSet
        // Expected result: NullPointerException
        try {
            prin = deserializeBuffer(Set.class, PRIN_WITH_NULL);
            throw new RuntimeException("Failed to throw NullPointerException");
        } catch (NullPointerException npe) {
            System.out.println("Caught expected NullPointerException [PASS]");
        }
    }

    private static void testAdd() {
        System.out.println("------ add() ------");
        // Create a well formed subject
        Subject mtSubj = makeSubj(false, false, false);

        try {
            // Case 1: Attempt to add null values to principal
            // Expected result: NullPointerException
            mtSubj.getPrincipals().add(null);
            throw new RuntimeException(
                    "PRINCIPAL add(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PRINCIPAL add(null): NullPointerException [PASS]");
        }

        try {
            // Case 2: Attempt to add null into the public creds
            // Expected result: NullPointerException
            mtSubj.getPublicCredentials().add(null);
            throw new RuntimeException(
                    "PUB CRED add(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PUB CRED add(null): NullPointerException [PASS]");
        }

        try {
            // Case 3: Attempt to add null into the private creds
            // Expected result: NullPointerException
            mtSubj.getPrivateCredentials().add(null);
            throw new RuntimeException(
                    "PRIV CRED add(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PRIV CRED add(null): NullPointerException [PASS]");
        }
    }

    private static void testRemove() {
        System.out.println("------ remove() ------");
        // Create a well formed subject
        Subject mtSubj = makeSubj(false, false, false);

        try {
            // Case 1: Attempt to remove null values from principal
            // Expected result: NullPointerException
            mtSubj.getPrincipals().remove(null);
            throw new RuntimeException(
                    "PRINCIPAL remove(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PRINCIPAL remove(null): NullPointerException [PASS]");
        }

        try {
            // Case 2: Attempt to remove null from the public creds
            // Expected result: NullPointerException
            mtSubj.getPublicCredentials().remove(null);
            throw new RuntimeException(
                    "PUB CRED remove(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PUB CRED remove(null): NullPointerException [PASS]");
        }

        try {
            // Case 3: Attempt to remove null from the private creds
            // Expected result: NullPointerException
            mtSubj.getPrivateCredentials().remove(null);
            throw new RuntimeException(
                    "PRIV CRED remove(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PRIV CRED remove(null): NullPointerException [PASS]");
        }
    }

    private static void testContains() {
        System.out.println("------ contains() ------");
        // Create a well formed subject
        Subject mtSubj = makeSubj(false, false, false);

        try {
            // Case 1: Attempt to check for null values in principals
            // Expected result: NullPointerException
            mtSubj.getPrincipals().contains(null);
            throw new RuntimeException(
                    "PRINCIPAL contains(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PRINCIPAL contains(null): NullPointerException [PASS]");
        }

        try {
            // Case 2: Attempt to check for null in public creds
            // Expected result: NullPointerException
            mtSubj.getPublicCredentials().contains(null);
            throw new RuntimeException(
                    "PUB CRED contains(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PUB CRED contains(null): NullPointerException [PASS]");
        }

        try {
            // Case 3: Attempt to check for null in private creds
            // Expected result: NullPointerException
            mtSubj.getPrivateCredentials().contains(null);
            throw new RuntimeException(
                    "PRIV CRED contains(null): Failed to throw NPE");
        } catch (NullPointerException npe) {
            System.out.println(
                    "PRIV CRED contains(null): NullPointerException [PASS]");
        }
    }

    private static void testAddAll() {
        // Create a well formed subject and additional collections
        Subject mtSubj = makeSubj(false, false, false);
        Set<Principal> morePrincs = new HashSet<>(Arrays.asList(tmplAddPrincs));
        Set<Object> morePubVals = new HashSet<>(Arrays.asList(tmplAddPubVals));
        Set<Object> morePrvVals = new HashSet<>(Arrays.asList(tmplAddPrvVals));

        // Run one success test for each Subject family to verify the
        // overloaded method works as intended.
        Set<Principal> setPrin = mtSubj.getPrincipals();
        Set<Object> setPubCreds = mtSubj.getPublicCredentials();
        Set<Object> setPrvCreds = mtSubj.getPrivateCredentials();
        int prinOrigSize = setPrin.size();
        int pubOrigSize = setPubCreds.size();
        int prvOrigSize = setPrvCreds.size();

        System.out.println("------ addAll() -----");

        // Add the new members, then check the resulting size of the
        // Subject attributes to verify they've increased by the proper
        // amounts.
        if ((validTestCollection(methAdd, setPrin, morePrincs) != true) ||
            (setPrin.size() != prinOrigSize + morePrincs.size()))
        {
            throw new RuntimeException("Failed addAll() on principals");
        }
        if ((validTestCollection(methAdd, setPubCreds,
                morePubVals) != true) ||
            (setPubCreds.size() != pubOrigSize + morePubVals.size()))
        {
            throw new RuntimeException("Failed addAll() on public creds");
        }
        if ((validTestCollection(methAdd, setPrvCreds,
                morePrvVals) != true) ||
            (setPrvCreds.size() != prvOrigSize + morePrvVals.size()))
        {
            throw new RuntimeException("Failed addAll() on private creds");
        }
        System.out.println("Positive addAll() test passed");

        // Now add null elements into each container, then retest
        morePrincs.add(null);
        morePubVals.add(null);
        morePrvVals.add(null);

        System.out.println("* Testing addAll w/ null values on Principals");
        nullTestCollection(methAdd, mtSubj.getPrincipals(), null);
        nullTestCollection(methAdd, mtSubj.getPrincipals(), morePrincs);

        System.out.println("* Testing addAll w/ null values on Public Creds");
        nullTestCollection(methAdd, mtSubj.getPublicCredentials(), null);
        nullTestCollection(methAdd, mtSubj.getPublicCredentials(),
                morePubVals);

        System.out.println("* Testing addAll w/ null values on Private Creds");
        nullTestCollection(methAdd, mtSubj.getPrivateCredentials(), null);
        nullTestCollection(methAdd, mtSubj.getPrivateCredentials(),
                morePrvVals);
    }

    private static void testRemoveAll() {
        // Create a well formed subject and additional collections
        Subject mtSubj = makeSubj(false, false, false);
        Set<Principal> remPrincs = new HashSet<>();
        Set<Object> remPubVals = new HashSet<>();
        Set<Object> remPrvVals = new HashSet<>();

        remPrincs.add(new KerberosPrincipal("mtwain/author@LITERATURE.US"));
        remPubVals.add("mtwain");
        remPrvVals.add("5Cl3M3nz");

        // Run one success test for each Subject family to verify the
        // overloaded method works as intended.
        Set<Principal> setPrin = mtSubj.getPrincipals();
        Set<Object> setPubCreds = mtSubj.getPublicCredentials();
        Set<Object> setPrvCreds = mtSubj.getPrivateCredentials();
        int prinOrigSize = setPrin.size();
        int pubOrigSize = setPubCreds.size();
        int prvOrigSize = setPrvCreds.size();

        System.out.println("------ removeAll() -----");

        // Remove the specified members, then check the resulting size of the
        // Subject attributes to verify they've decreased by the proper
        // amounts.
        if ((validTestCollection(methRemove, setPrin, remPrincs) != true) ||
            (setPrin.size() != prinOrigSize - remPrincs.size()))
        {
            throw new RuntimeException("Failed removeAll() on principals");
        }
        if ((validTestCollection(methRemove, setPubCreds,
                remPubVals) != true) ||
            (setPubCreds.size() != pubOrigSize - remPubVals.size()))
        {
            throw new RuntimeException("Failed removeAll() on public creds");
        }
        if ((validTestCollection(methRemove, setPrvCreds,
                remPrvVals) != true) ||
            (setPrvCreds.size() != prvOrigSize - remPrvVals.size()))
        {
            throw new RuntimeException("Failed removeAll() on private creds");
        }
        System.out.println("Positive removeAll() test passed");

        // Now add null elements into each container, then retest
        remPrincs.add(null);
        remPubVals.add(null);
        remPrvVals.add(null);

        System.out.println("* Testing removeAll w/ null values on Principals");
        nullTestCollection(methRemove, mtSubj.getPrincipals(), null);
        nullTestCollection(methRemove, mtSubj.getPrincipals(), remPrincs);

        System.out.println(
                "* Testing removeAll w/ null values on Public Creds");
        nullTestCollection(methRemove, mtSubj.getPublicCredentials(), null);
        nullTestCollection(methRemove, mtSubj.getPublicCredentials(),
                remPubVals);

        System.out.println(
                "* Testing removeAll w/ null values on Private Creds");
        nullTestCollection(methRemove, mtSubj.getPrivateCredentials(), null);
        nullTestCollection(methRemove, mtSubj.getPrivateCredentials(),
                remPrvVals);
    }

    private static void testContainsAll() {
        // Create a well formed subject and additional collections
        Subject mtSubj = makeSubj(false, false, false);
        Set<Principal> testPrincs = new HashSet<>(Arrays.asList(princVals));
        Set<Object> testPubVals = new HashSet<>(Arrays.asList(pubVals));
        Set<Object> testPrvVals = new HashSet<>(Arrays.asList(privVals));

        System.out.println("------ containsAll() -----");

        // Run one success test for each Subject family to verify the
        // overloaded method works as intended.
        if ((validTestCollection(methContains, mtSubj.getPrincipals(),
                 testPrincs) == false) &&
            (validTestCollection(methContains, mtSubj.getPublicCredentials(),
                 testPubVals) == false) &&
            (validTestCollection(methContains,
                 mtSubj.getPrivateCredentials(), testPrvVals) == false)) {
            throw new RuntimeException("Valid containsAll() check failed");
        }
        System.out.println("Positive containsAll() test passed");

        // Now let's add a null into each collection and watch the fireworks.
        testPrincs.add(null);
        testPubVals.add(null);
        testPrvVals.add(null);

        System.out.println(
                "* Testing containsAll w/ null values on Principals");
        nullTestCollection(methContains, mtSubj.getPrincipals(), null);
        nullTestCollection(methContains, mtSubj.getPrincipals(), testPrincs);

        System.out.println(
                "* Testing containsAll w/ null values on Public Creds");
        nullTestCollection(methContains, mtSubj.getPublicCredentials(),
                null);
        nullTestCollection(methContains, mtSubj.getPublicCredentials(),
                testPubVals);

        System.out.println(
                "* Testing containsAll w/ null values on Private Creds");
        nullTestCollection(methContains, mtSubj.getPrivateCredentials(),
                null);
        nullTestCollection(methContains, mtSubj.getPrivateCredentials(),
                testPrvVals);
    }

    private static void testRetainAll() {
        // Create a well formed subject and additional collections
        Subject mtSubj = makeSubj(false, false, false);
        Set<Principal> remPrincs = new HashSet<>(Arrays.asList(tmplAddPrincs));
        Set<Object> remPubVals = new HashSet<>(Arrays.asList(tmplAddPubVals));
        Set<Object> remPrvVals = new HashSet<>(Arrays.asList(tmplAddPrvVals));

        // Add in values that exist within the Subject
        remPrincs.add(princVals[2]);
        remPubVals.add(pubVals[2]);
        remPrvVals.add(privVals[2]);

        // Run one success test for each Subject family to verify the
        // overloaded method works as intended.
        Set<Principal> setPrin = mtSubj.getPrincipals();
        Set<Object> setPubCreds = mtSubj.getPublicCredentials();
        Set<Object> setPrvCreds = mtSubj.getPrivateCredentials();
        int prinOrigSize = setPrin.size();
        int pubOrigSize = setPubCreds.size();
        int prvOrigSize = setPrvCreds.size();

        System.out.println("------ retainAll() -----");

        // Retain the specified members (those that exist in the Subject)
        // and validate the results.
        if (validTestCollection(methRetain, setPrin, remPrincs) == false ||
            setPrin.size() != 1 || setPrin.contains(princVals[2]) == false)
        {
            throw new RuntimeException("Failed retainAll() on principals");
        }

        if (validTestCollection(methRetain, setPubCreds,
                remPubVals) == false ||
            setPubCreds.size() != 1 ||
            setPubCreds.contains(pubVals[2]) == false)
        {
            throw new RuntimeException("Failed retainAll() on public creds");
        }
        if (validTestCollection(methRetain, setPrvCreds,
                remPrvVals) == false ||
            setPrvCreds.size() != 1 ||
            setPrvCreds.contains(privVals[2]) == false)
        {
            throw new RuntimeException("Failed retainAll() on private creds");
        }
        System.out.println("Positive retainAll() test passed");

        // Now add null elements into each container, then retest
        remPrincs.add(null);
        remPubVals.add(null);
        remPrvVals.add(null);

        System.out.println("* Testing retainAll w/ null values on Principals");
        nullTestCollection(methRetain, mtSubj.getPrincipals(), null);
        nullTestCollection(methRetain, mtSubj.getPrincipals(), remPrincs);

        System.out.println(
                "* Testing retainAll w/ null values on Public Creds");
        nullTestCollection(methRetain, mtSubj.getPublicCredentials(), null);
        nullTestCollection(methRetain, mtSubj.getPublicCredentials(),
                remPubVals);

        System.out.println(
                "* Testing retainAll w/ null values on Private Creds");
        nullTestCollection(methRetain, mtSubj.getPrivateCredentials(), null);
        nullTestCollection(methRetain, mtSubj.getPrivateCredentials(),
                remPrvVals);
    }

    private static void testIsEmpty() {
        Subject populatedSubj = makeSubj(false, false, false);
        Subject emptySubj = new Subject();

        System.out.println("------ isEmpty() -----");

        if (populatedSubj.getPrincipals().isEmpty()) {
            throw new RuntimeException(
                    "Populated Subject Principals incorrectly returned empty");
        }
        if (emptySubj.getPrincipals().isEmpty() == false) {
            throw new RuntimeException(
                    "Empty Subject Principals incorrectly returned non-empty");
        }
        System.out.println("isEmpty() test passed");
    }

    private static void testSecureSetEquals() {
        System.out.println("------ SecureSet.equals() -----");

        Subject subj = makeSubj(false, false, false);
        Subject subjComp = makeSubj(false, false, false);

        // Case 1: null comparison [expect false]
        if (subj.getPublicCredentials().equals(null) != false) {
            throw new RuntimeException(
                    "equals(null) incorrectly returned true");
        }

        // Case 2: Self-comparison [expect true]
        Set<Principal> princs = subj.getPrincipals();
        princs.equals(subj.getPrincipals());

        // Case 3: Comparison with non-Set type [expect false]
        List<Principal> listPrinc = new LinkedList<>(Arrays.asList(princVals));
        if (subj.getPublicCredentials().equals(listPrinc) != false) {
            throw new RuntimeException(
                    "equals([Non-Set]) incorrectly returned true");
        }

        // Case 4: SecureSets of differing sizes [expect false]
        Subject subj1princ = new Subject();
        Subject subj2princ = new Subject();
        subj1princ.getPrincipals().add(
                new X500Principal("CN=Tom Sawyer, ST=Missouri, C=US"));
        subj1princ.getPrincipals().add(
                new X500Principal("CN=John Doe, O=Bogus Corp."));
        subj2princ.getPrincipals().add(
                new X500Principal("CN=Tom Sawyer, ST=Missouri, C=US"));
        if (subj1princ.getPrincipals().equals(
                    subj2princ.getPrincipals()) != false) {
            throw new RuntimeException(
                    "equals([differing sizes]) incorrectly returned true");
        }

        // Case 5: Content equality test [expect true]
        Set<Principal> equalSet = new HashSet<>(Arrays.asList(princVals));
        if (subj.getPrincipals().equals(equalSet) != true) {
            throw new RuntimeException(
                    "equals([equivalent set]) incorrectly returned false");
        }

        // Case 5: Content inequality test [expect false]
        // Note: to not fall into the size inequality check the two
        // sets need to have the same number of elements.
        Set<Principal> inequalSet =
            new HashSet<Principal>(Arrays.asList(tmplAddPrincs));
        inequalSet.add(new JMXPrincipal("Samuel Clemens"));

        if (subj.getPrincipals().equals(inequalSet) != false) {
            throw new RuntimeException(
                    "equals([equivalent set]) incorrectly returned false");
        }
        System.out.println("SecureSet.equals() tests passed");
    }

    private static void testSecureSetHashCode() {
        System.out.println("------ SecureSet.hashCode() -----");

        Subject subj = makeSubj(false, false, false);

        // Make sure two other Set types that we know are equal per
        // SecureSet.equals() and verify their hashCodes are also the same
        Set<Principal> equalHashSet = new HashSet<>(Arrays.asList(princVals));

        if (subj.getPrincipals().hashCode() != equalHashSet.hashCode()) {
            throw new RuntimeException(
                    "SecureSet and HashSet hashCodes() differ");
        }
        System.out.println("SecureSet.hashCode() tests passed");
    }

    private static void testToArray() {
        System.out.println("------ toArray() -----");

        Subject subj = makeSubj(false, false, false);

        // Case 1: no-parameter toArray with equality comparison
        // Expected result: true
        List<Object> alSubj = Arrays.asList(subj.getPrincipals().toArray());
        List<Principal> alPrincs = Arrays.asList(princVals);

        if (alSubj.size() != alPrincs.size() ||
                alSubj.containsAll(alPrincs) != true) {
            throw new RuntimeException(
                    "Unexpected inequality on returned toArray()");
        }

        // Case 2: generic-type toArray where passed array is of sufficient
        //         size.
        // Expected result: returned Array is reference-equal to input param
        // and content equal to data used to construct the originating Subject.
        Principal[] pBlock = new Principal[3];
        Principal[] pBlockRef = subj.getPrincipals().toArray(pBlock);
        alSubj = Arrays.asList((Object[])pBlockRef);

        if (pBlockRef != pBlock) {
            throw new RuntimeException(
                    "Unexpected reference-inequality on returned toArray(T[])");
        } else if (alSubj.size() != alPrincs.size() ||
                alSubj.containsAll(alPrincs) != true) {
            throw new RuntimeException(
                    "Unexpected content-inequality on returned toArray(T[])");
        }

        // Case 3: generic-type toArray where passed array is of
        //         insufficient size.
        // Expected result: returned Array is not reference-equal to
        // input param but is content equal to data used to construct the
        // originating Subject.
        pBlock = new Principal[1];
        pBlockRef = subj.getPrincipals().toArray(pBlock);
        alSubj = Arrays.asList((Object[])pBlockRef);

        if (pBlockRef == pBlock) {
            throw new RuntimeException(
                    "Unexpected reference-equality on returned toArray(T[])");
        } else if (alSubj.size() != alPrincs.size() ||
                alSubj.containsAll(alPrincs) != true) {
            throw new RuntimeException(
                    "Unexpected content-inequality on returned toArray(T[])");
        }
        System.out.println("toArray() tests passed");
    }

    public static void main(String[] args) throws Exception {

        testCTOR();

        testDeserialize();

        testAdd();

        testRemove();

        testContains();

        testAddAll();

        testRemoveAll();

        testContainsAll();

        testRetainAll();

        testIsEmpty();

        testSecureSetEquals();

        testSecureSetHashCode();

        testToArray();
    }
}
