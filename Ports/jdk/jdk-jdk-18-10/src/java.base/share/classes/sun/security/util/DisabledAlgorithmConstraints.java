/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import sun.security.validator.Validator;

import java.security.AlgorithmParameters;
import java.security.CryptoPrimitive;
import java.security.Key;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorException.BasicReason;
import java.security.interfaces.ECKey;
import java.security.interfaces.XECKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.NamedParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Algorithm constraints for disabled algorithms property
 *
 * See the "jdk.certpath.disabledAlgorithms" specification in java.security
 * for the syntax of the disabled algorithm string.
 */
public class DisabledAlgorithmConstraints extends AbstractAlgorithmConstraints {
    private static final Debug debug = Debug.getInstance("certpath");

    // Disabled algorithm security property for certificate path
    public static final String PROPERTY_CERTPATH_DISABLED_ALGS =
            "jdk.certpath.disabledAlgorithms";

    // Legacy algorithm security property for certificate path and jar
    public static final String PROPERTY_SECURITY_LEGACY_ALGS =
            "jdk.security.legacyAlgorithms";

    // Disabled algorithm security property for TLS
    public static final String PROPERTY_TLS_DISABLED_ALGS =
            "jdk.tls.disabledAlgorithms";

    // Disabled algorithm security property for jar
    public static final String PROPERTY_JAR_DISABLED_ALGS =
            "jdk.jar.disabledAlgorithms";

    // Property for disabled EC named curves
    private static final String PROPERTY_DISABLED_EC_CURVES =
            "jdk.disabled.namedCurves";

    private static final Pattern INCLUDE_PATTERN = Pattern.compile("include " +
            PROPERTY_DISABLED_EC_CURVES, Pattern.CASE_INSENSITIVE);

    private static class CertPathHolder {
        static final DisabledAlgorithmConstraints CONSTRAINTS =
            new DisabledAlgorithmConstraints(PROPERTY_CERTPATH_DISABLED_ALGS);
    }

    private static class JarHolder {
        static final DisabledAlgorithmConstraints CONSTRAINTS =
            new DisabledAlgorithmConstraints(PROPERTY_JAR_DISABLED_ALGS);
    }

    private final Set<String> disabledAlgorithms;
    private final Constraints algorithmConstraints;

    public static DisabledAlgorithmConstraints certPathConstraints() {
        return CertPathHolder.CONSTRAINTS;
    }

    public static DisabledAlgorithmConstraints jarConstraints() {
        return JarHolder.CONSTRAINTS;
    }

    /**
     * Initialize algorithm constraints with the specified security property.
     *
     * @param propertyName the security property name that define the disabled
     *        algorithm constraints
     */
    public DisabledAlgorithmConstraints(String propertyName) {
        this(propertyName, new AlgorithmDecomposer());
    }

    /**
     * Initialize algorithm constraints with the specified security property
     * for a specific usage type.
     *
     * @param propertyName the security property name that define the disabled
     *        algorithm constraints
     * @param decomposer an alternate AlgorithmDecomposer.
     */
    public DisabledAlgorithmConstraints(String propertyName,
            AlgorithmDecomposer decomposer) {
        super(decomposer);
        disabledAlgorithms = getAlgorithms(propertyName);

        // Check for alias
        for (String s : disabledAlgorithms) {
            Matcher matcher = INCLUDE_PATTERN.matcher(s);
            if (matcher.matches()) {
                disabledAlgorithms.remove(matcher.group());
                disabledAlgorithms.addAll(
                        getAlgorithms(PROPERTY_DISABLED_EC_CURVES));
                break;
            }
        }
        algorithmConstraints = new Constraints(propertyName, disabledAlgorithms);
    }

    /*
     * This only checks if the algorithm has been completely disabled.  If
     * there are keysize or other limit, this method allow the algorithm.
     */
    @Override
    public final boolean permits(Set<CryptoPrimitive> primitives,
            String algorithm, AlgorithmParameters parameters) {
        if (primitives == null || primitives.isEmpty()) {
            throw new IllegalArgumentException("The primitives cannot be null" +
                    " or empty.");
        }

        if (!checkAlgorithm(disabledAlgorithms, algorithm, decomposer)) {
            return false;
        }

        if (parameters != null) {
            return algorithmConstraints.permits(algorithm, parameters);
        }

        return true;
    }

    /*
     * Checks if the key algorithm has been disabled or constraints have been
     * placed on the key.
     */
    @Override
    public final boolean permits(Set<CryptoPrimitive> primitives, Key key) {
        return checkConstraints(primitives, "", key, null);
    }

    /*
     * Checks if the key algorithm has been disabled or if constraints have
     * been placed on the key.
     */
    @Override
    public final boolean permits(Set<CryptoPrimitive> primitives,
            String algorithm, Key key, AlgorithmParameters parameters) {

        if (algorithm == null || algorithm.isEmpty()) {
            throw new IllegalArgumentException("No algorithm name specified");
        }

        return checkConstraints(primitives, algorithm, key, parameters);
    }

    public final void permits(String algorithm, AlgorithmParameters ap,
        ConstraintsParameters cp) throws CertPathValidatorException {

        permits(algorithm, cp);
        if (ap != null) {
            permits(ap, cp);
        }
    }

    private void permits(AlgorithmParameters ap, ConstraintsParameters cp)
        throws CertPathValidatorException {

        switch (ap.getAlgorithm().toUpperCase(Locale.ENGLISH)) {
            case "RSASSA-PSS":
                permitsPSSParams(ap, cp);
                break;
            default:
                // unknown algorithm, just ignore
        }
    }

    private void permitsPSSParams(AlgorithmParameters ap,
        ConstraintsParameters cp) throws CertPathValidatorException {

        try {
            PSSParameterSpec pssParams =
                ap.getParameterSpec(PSSParameterSpec.class);
            String digestAlg = pssParams.getDigestAlgorithm();
            permits(digestAlg, cp);
            AlgorithmParameterSpec mgfParams = pssParams.getMGFParameters();
            if (mgfParams instanceof MGF1ParameterSpec) {
                String mgfDigestAlg =
                    ((MGF1ParameterSpec)mgfParams).getDigestAlgorithm();
                if (!mgfDigestAlg.equalsIgnoreCase(digestAlg)) {
                    permits(mgfDigestAlg, cp);
                }
            }
        } catch (InvalidParameterSpecException ipse) {
            // ignore
        }
    }

    public final void permits(String algorithm, ConstraintsParameters cp)
            throws CertPathValidatorException {

        // Check if named curves in the key are disabled.
        for (Key key : cp.getKeys()) {
            for (String curve : getNamedCurveFromKey(key)) {
                if (!checkAlgorithm(disabledAlgorithms, curve, decomposer)) {
                    throw new CertPathValidatorException(
                            "Algorithm constraints check failed on disabled " +
                                    "algorithm: " + curve,
                            null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
                }
            }
        }

        algorithmConstraints.permits(algorithm, cp);
    }

    private static List<String> getNamedCurveFromKey(Key key) {
        if (key instanceof ECKey) {
            NamedCurve nc = CurveDB.lookup(((ECKey)key).getParams());
            return (nc == null ? List.of()
                               : Arrays.asList(nc.getNameAndAliases()));
        } else if (key instanceof XECKey) {
            return List.of(
                ((NamedParameterSpec)((XECKey)key).getParams()).getName());
        } else {
            return List.of();
        }
    }

    // Check algorithm constraints with key and algorithm
    private boolean checkConstraints(Set<CryptoPrimitive> primitives,
            String algorithm, Key key, AlgorithmParameters parameters) {

        if (primitives == null || primitives.isEmpty()) {
            throw new IllegalArgumentException("The primitives cannot be null" +
                    " or empty.");
        }

        if (key == null) {
            throw new IllegalArgumentException("The key cannot be null");
        }

        // check the signature algorithm with parameters
        if (algorithm != null && !algorithm.isEmpty()) {
            if (!permits(primitives, algorithm, parameters)) {
                return false;
            }
        }

        // check the key algorithm
        if (!permits(primitives, key.getAlgorithm(), null)) {
            return false;
        }

        // If this is an elliptic curve, check if it is disabled
        for (String curve : getNamedCurveFromKey(key)) {
            if (!permits(primitives, curve, null)) {
                return false;
            }
        }

        // check the key constraints
        return algorithmConstraints.permits(key);
    }


    /**
     * Key and Certificate Constraints
     *
     * The complete disabling of an algorithm is not handled by Constraints or
     * Constraint classes.  That is addressed with
     *   permit(Set<CryptoPrimitive>, String, AlgorithmParameters)
     *
     * When passing a Key to permit(), the boolean return values follow the
     * same as the interface class AlgorithmConstraints.permit().  This is to
     * maintain compatibility:
     * 'true' means the operation is allowed.
     * 'false' means it failed the constraints and is disallowed.
     *
     * When passing ConstraintsParameters through permit(), an exception
     * will be thrown on a failure to better identify why the operation was
     * disallowed.
     */

    private static class Constraints {
        private Map<String, List<Constraint>> constraintsMap = new HashMap<>();

        private static class Holder {
            private static final Pattern DENY_AFTER_PATTERN = Pattern.compile(
                    "denyAfter\\s+(\\d{4})-(\\d{2})-(\\d{2})");
        }

        public Constraints(String propertyName, Set<String> constraintSet) {
            for (String constraintEntry : constraintSet) {
                if (constraintEntry == null || constraintEntry.isEmpty()) {
                    continue;
                }

                constraintEntry = constraintEntry.trim();
                if (debug != null) {
                    debug.println("Constraints: " + constraintEntry);
                }

                // Check if constraint is a complete disabling of an
                // algorithm or has conditions.
                int space = constraintEntry.indexOf(' ');
                String algorithm = AlgorithmDecomposer.hashName(
                        ((space > 0 ? constraintEntry.substring(0, space) :
                                constraintEntry)));
                List<Constraint> constraintList =
                        constraintsMap.getOrDefault(
                                algorithm.toUpperCase(Locale.ENGLISH),
                                new ArrayList<>(1));

                // Consider the impact of algorithm aliases.
                for (String alias : AlgorithmDecomposer.getAliases(algorithm)) {
                    constraintsMap.putIfAbsent(
                            alias.toUpperCase(Locale.ENGLISH), constraintList);
                }

                // If there is no whitespace, it is a algorithm name; however,
                // if there is a whitespace, could be a multi-word EC curve too.
                if (space <= 0 || CurveDB.lookup(constraintEntry) != null) {
                    constraintList.add(new DisabledConstraint(algorithm));
                    continue;
                }

                String policy = constraintEntry.substring(space + 1);

                // Convert constraint conditions into Constraint classes
                Constraint c, lastConstraint = null;
                // Allow only one jdkCA entry per constraint entry
                boolean jdkCALimit = false;
                // Allow only one denyAfter entry per constraint entry
                boolean denyAfterLimit = false;

                for (String entry : policy.split("&")) {
                    entry = entry.trim();

                    Matcher matcher;
                    if (entry.startsWith("keySize")) {
                        if (debug != null) {
                            debug.println("Constraints set to keySize: " +
                                    entry);
                        }
                        StringTokenizer tokens = new StringTokenizer(entry);
                        if (!"keySize".equals(tokens.nextToken())) {
                            throw new IllegalArgumentException("Error in " +
                                    "security property. Constraint unknown: " +
                                    entry);
                        }
                        c = new KeySizeConstraint(algorithm,
                                KeySizeConstraint.Operator.of(tokens.nextToken()),
                                Integer.parseInt(tokens.nextToken()));

                    } else if (entry.equalsIgnoreCase("jdkCA")) {
                        if (debug != null) {
                            debug.println("Constraints set to jdkCA.");
                        }
                        if (jdkCALimit) {
                            throw new IllegalArgumentException("Only one " +
                                    "jdkCA entry allowed in property. " +
                                    "Constraint: " + constraintEntry);
                        }
                        c = new jdkCAConstraint(algorithm);
                        jdkCALimit = true;

                    } else if (entry.startsWith("denyAfter") &&
                            (matcher = Holder.DENY_AFTER_PATTERN.matcher(entry))
                                    .matches()) {
                        if (debug != null) {
                            debug.println("Constraints set to denyAfter");
                        }
                        if (denyAfterLimit) {
                            throw new IllegalArgumentException("Only one " +
                                    "denyAfter entry allowed in property. " +
                                    "Constraint: " + constraintEntry);
                        }
                        int year = Integer.parseInt(matcher.group(1));
                        int month = Integer.parseInt(matcher.group(2));
                        int day = Integer.parseInt(matcher.group(3));
                        c = new DenyAfterConstraint(algorithm, year, month,
                                day);
                        denyAfterLimit = true;
                    } else if (entry.startsWith("usage")) {
                        String s[] = (entry.substring(5)).trim().split(" ");
                        c = new UsageConstraint(algorithm, s);
                        if (debug != null) {
                            debug.println("Constraints usage length is " + s.length);
                        }
                    } else {
                        throw new IllegalArgumentException("Error in security" +
                                " property. Constraint unknown: " + entry);
                    }

                    // Link multiple conditions for a single constraint
                    // into a linked list.
                    if (lastConstraint == null) {
                        constraintList.add(c);
                    } else {
                        lastConstraint.nextConstraint = c;
                    }
                    lastConstraint = c;
                }
            }
        }

        // Get applicable constraints based off the algorithm
        private List<Constraint> getConstraints(String algorithm) {
            return constraintsMap.get(algorithm.toUpperCase(Locale.ENGLISH));
        }

        // Check if KeySizeConstraints permit the specified key
        public boolean permits(Key key) {
            List<Constraint> list = getConstraints(key.getAlgorithm());
            if (list == null) {
                return true;
            }
            for (Constraint constraint : list) {
                if (!constraint.permits(key)) {
                    if (debug != null) {
                        debug.println("Constraints: failed key size" +
                                "constraint check " + KeyUtil.getKeySize(key));
                    }
                    return false;
                }
            }
            return true;
        }

        // Check if constraints permit this AlgorithmParameters.
        public boolean permits(String algorithm, AlgorithmParameters aps) {
            List<Constraint> list = getConstraints(algorithm);
            if (list == null) {
                return true;
            }

            for (Constraint constraint : list) {
                if (!constraint.permits(aps)) {
                    if (debug != null) {
                        debug.println("Constraints: failed algorithm " +
                                "parameters constraint check " + aps);
                    }

                    return false;
                }
            }

            return true;
        }

        public void permits(String algorithm, ConstraintsParameters cp)
                throws CertPathValidatorException {

            if (debug != null) {
                debug.println("Constraints.permits(): " + algorithm + ", "
                              + cp.toString());
            }

            // Get all signature algorithms to check for constraints
            Set<String> algorithms = new HashSet<>();
            if (algorithm != null) {
                algorithms.addAll(AlgorithmDecomposer.decomposeOneHash(algorithm));
                algorithms.add(algorithm);
            }

            for (Key key : cp.getKeys()) {
                algorithms.add(key.getAlgorithm());
            }

            // Check all applicable constraints
            for (String alg : algorithms) {
                List<Constraint> list = getConstraints(alg);
                if (list == null) {
                    continue;
                }
                for (Constraint constraint : list) {
                    constraint.permits(cp);
                }
            }
        }
    }

    /**
     * This abstract Constraint class for algorithm-based checking
     * may contain one or more constraints.  If the '&' on the {@Security}
     * property is used, multiple constraints have been grouped together
     * requiring all the constraints to fail for the check to be disallowed.
     *
     * If the class contains multiple constraints, the next constraint
     * is stored in {@code nextConstraint} in linked-list fashion.
     */
    private abstract static class Constraint {
        String algorithm;
        Constraint nextConstraint = null;

        // operator
        enum Operator {
            EQ,         // "=="
            NE,         // "!="
            LT,         // "<"
            LE,         // "<="
            GT,         // ">"
            GE;         // ">="

            static Operator of(String s) {
                switch (s) {
                    case "==":
                        return EQ;
                    case "!=":
                        return NE;
                    case "<":
                        return LT;
                    case "<=":
                        return LE;
                    case ">":
                        return GT;
                    case ">=":
                        return GE;
                }

                throw new IllegalArgumentException("Error in security " +
                        "property. " + s + " is not a legal Operator");
            }
        }

        /**
         * Check if an algorithm constraint is permitted with a given key.
         *
         * If the check inside of {@code permit()} fails, it must call
         * {@code next()} with the same {@code Key} parameter passed if
         * multiple constraints need to be checked.
         *
         * @param key Public key
         * @return 'true' if constraint is allowed, 'false' if disallowed.
         */
        public boolean permits(Key key) {
            return true;
        }

        /**
         * Check if the algorithm constraint permits a given cryptographic
         * parameters.
         *
         * @param parameters the cryptographic parameters
         * @return 'true' if the cryptographic parameters is allowed,
         *         'false' ortherwise.
         */
        public boolean permits(AlgorithmParameters parameters) {
            return true;
        }

        /**
         * Check if an algorithm constraint is permitted with a given
         * ConstraintsParameters.
         *
         * If the check inside of {@code permits()} fails, it must call
         * {@code next()} with the same {@code ConstraintsParameters}
         * parameter passed if multiple constraints need to be checked.
         *
         * @param cp ConstraintsParameter containing certificate info
         * @throws CertPathValidatorException if constraint disallows.
         *
         */
        public abstract void permits(ConstraintsParameters cp)
                throws CertPathValidatorException;

        /**
         * Recursively check if the constraints are allowed.
         *
         * If {@code nextConstraint} is non-null, this method will
         * call {@code nextConstraint}'s {@code permits()} to check if the
         * constraint is allowed or denied.  If the constraint's
         * {@code permits()} is allowed, this method will exit this and any
         * recursive next() calls, returning 'true'.  If the constraints called
         * were disallowed, the last constraint will throw
         * {@code CertPathValidatorException}.
         *
         * @param cp ConstraintsParameters
         * @return 'true' if constraint allows the operation, 'false' if
         * we are at the end of the constraint list or,
         * {@code nextConstraint} is null.
         */
        boolean next(ConstraintsParameters cp)
                throws CertPathValidatorException {
            if (nextConstraint != null) {
                nextConstraint.permits(cp);
                return true;
            }
            return false;
        }

        /**
         * Recursively check if this constraint is allowed,
         *
         * If {@code nextConstraint} is non-null, this method will
         * call {@code nextConstraint}'s {@code permit()} to check if the
         * constraint is allowed or denied.  If the constraint's
         * {@code permit()} is allowed, this method will exit this and any
         * recursive next() calls, returning 'true'.  If the constraints
         * called were disallowed the check will exit with 'false'.
         *
         * @param key Public key
         * @return 'true' if constraint allows the operation, 'false' if
         * the constraint denies the operation.
         */
        boolean next(Key key) {
            return nextConstraint != null && nextConstraint.permits(key);
        }
    }

    /*
     * This class contains constraints dealing with the certificate chain
     * of the certificate.
     */
    private static class jdkCAConstraint extends Constraint {
        jdkCAConstraint(String algo) {
            algorithm = algo;
        }

        /*
         * Check if ConstraintsParameters has a trusted match, if it does
         * call next() for any following constraints. If it does not, exit
         * as this constraint(s) does not restrict the operation.
         */
        @Override
        public void permits(ConstraintsParameters cp)
                throws CertPathValidatorException {
            if (debug != null) {
                debug.println("jdkCAConstraints.permits(): " + algorithm);
            }

            // Check if any certs chain back to at least one trust anchor in
            // cacerts
            if (cp.anchorIsJdkCA()) {
                if (next(cp)) {
                    return;
                }
                throw new CertPathValidatorException(
                        "Algorithm constraints check failed on certificate " +
                        "anchor limits. " + algorithm + cp.extendedExceptionMsg(),
                        null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
            }
        }
    }

    /*
     * This class handles the denyAfter constraint.  The date is in the UTC/GMT
     * timezone.
     */
    private static class DenyAfterConstraint extends Constraint {
        private Date denyAfterDate;
        private static final SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE, MMM d HH:mm:ss z yyyy");

        DenyAfterConstraint(String algo, int year, int month, int day) {
            Calendar c;

            algorithm = algo;

            if (debug != null) {
                debug.println("DenyAfterConstraint read in as:  year " +
                        year + ", month = " + month + ", day = " + day);
            }

            c = new Calendar.Builder().setTimeZone(TimeZone.getTimeZone("GMT"))
                    .setDate(year, month - 1, day).build();

            if (year > c.getActualMaximum(Calendar.YEAR) ||
                    year < c.getActualMinimum(Calendar.YEAR)) {
                throw new IllegalArgumentException(
                        "Invalid year given in constraint: " + year);
            }
            if ((month - 1) > c.getActualMaximum(Calendar.MONTH) ||
                    (month - 1) < c.getActualMinimum(Calendar.MONTH)) {
                throw new IllegalArgumentException(
                        "Invalid month given in constraint: " + month);
            }
            if (day > c.getActualMaximum(Calendar.DAY_OF_MONTH) ||
                    day < c.getActualMinimum(Calendar.DAY_OF_MONTH)) {
                throw new IllegalArgumentException(
                        "Invalid Day of Month given in constraint: " + day);
            }

            denyAfterDate = c.getTime();
            if (debug != null) {
                debug.println("DenyAfterConstraint date set to: " +
                        dateFormat.format(denyAfterDate));
            }
        }

        /*
         * Checking that the provided date is not beyond the constraint date.
         * The provided date can be the PKIXParameter date if given,
         * otherwise it is the current date.
         *
         * If the constraint disallows, call next() for any following
         * constraints. Throw an exception if this is the last constraint.
         */
        @Override
        public void permits(ConstraintsParameters cp)
                throws CertPathValidatorException {
            Date currentDate;
            String errmsg;

            if (cp.getDate() != null) {
                currentDate = cp.getDate();
            } else {
                currentDate = new Date();
            }

            if (!denyAfterDate.after(currentDate)) {
                if (next(cp)) {
                    return;
                }
                throw new CertPathValidatorException(
                        "denyAfter constraint check failed: " + algorithm +
                        " used with Constraint date: " +
                        dateFormat.format(denyAfterDate) + "; params date: " +
                        dateFormat.format(currentDate) + cp.extendedExceptionMsg(),
                        null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
            }
        }

        /*
         * Return result if the constraint's date is beyond the current date
         * in UTC timezone.
         */
        @Override
        public boolean permits(Key key) {
            if (next(key)) {
                return true;
            }
            if (debug != null) {
                debug.println("DenyAfterConstraints.permits(): " + algorithm);
            }

            return denyAfterDate.after(new Date());
        }
    }

    /*
     * The usage constraint is for the "usage" keyword.  It checks against the
     * variant value in ConstraintsParameters.
     */
    private static class UsageConstraint extends Constraint {
        String[] usages;

        UsageConstraint(String algorithm, String[] usages) {
            this.algorithm = algorithm;
            this.usages = usages;
        }

        @Override
        public void permits(ConstraintsParameters cp)
                throws CertPathValidatorException {
            String variant = cp.getVariant();
            for (String usage : usages) {

                boolean match = false;
                switch (usage.toLowerCase()) {
                    case "tlsserver":
                        match = variant.equals(Validator.VAR_TLS_SERVER);
                        break;
                    case "tlsclient":
                        match = variant.equals(Validator.VAR_TLS_CLIENT);
                        break;
                    case "signedjar":
                        match =
                            variant.equals(Validator.VAR_CODE_SIGNING) ||
                            variant.equals(Validator.VAR_TSA_SERVER);
                        break;
                }

                if (debug != null) {
                    debug.println("Checking if usage constraint \"" + usage +
                            "\" matches \"" + cp.getVariant() + "\"");
                    if (Debug.isVerbose()) {
                        // Because usage checking can come from many places
                        // a stack trace is very helpful.
                        (new Exception()).printStackTrace(debug.getPrintStream());
                    }
                }
                if (match) {
                    if (next(cp)) {
                        return;
                    }
                    throw new CertPathValidatorException("Usage constraint " +
                            usage + " check failed: " + algorithm +
                            cp.extendedExceptionMsg(),
                            null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
                }
            }
        }
    }

    /*
     * This class contains constraints dealing with the key size
     * support limits per algorithm.   e.g.  "keySize <= 1024"
     */
    private static class KeySizeConstraint extends Constraint {

        private int minSize;            // the minimal available key size
        private int maxSize;            // the maximal available key size
        private int prohibitedSize = -1;    // unavailable key sizes

        public KeySizeConstraint(String algo, Operator operator, int length) {
            algorithm = algo;
            switch (operator) {
                case EQ:      // an unavailable key size
                    this.minSize = 0;
                    this.maxSize = Integer.MAX_VALUE;
                    prohibitedSize = length;
                    break;
                case NE:
                    this.minSize = length;
                    this.maxSize = length;
                    break;
                case LT:
                    this.minSize = length;
                    this.maxSize = Integer.MAX_VALUE;
                    break;
                case LE:
                    this.minSize = length + 1;
                    this.maxSize = Integer.MAX_VALUE;
                    break;
                case GT:
                    this.minSize = 0;
                    this.maxSize = length;
                    break;
                case GE:
                    this.minSize = 0;
                    this.maxSize = length > 1 ? (length - 1) : 0;
                    break;
                default:
                    // unlikely to happen
                    this.minSize = Integer.MAX_VALUE;
                    this.maxSize = -1;
            }
        }

        /*
         * For each key, check if each constraint fails and check if there is
         * a linked constraint. Any permitted constraint will exit the linked
         * list to allow the operation.
         */
        @Override
        public void permits(ConstraintsParameters cp)
                throws CertPathValidatorException {
            for (Key key : cp.getKeys()) {
                if (!permitsImpl(key)) {
                    if (nextConstraint != null) {
                        nextConstraint.permits(cp);
                        continue;
                    }
                    throw new CertPathValidatorException(
                        "Algorithm constraints check failed on keysize limits: " +
                        algorithm + " " + KeyUtil.getKeySize(key) + " bit key" +
                        cp.extendedExceptionMsg(),
                        null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
                }
            }
        }


        // Check if key constraint disable the specified key
        // Uses old style permit()
        @Override
        public boolean permits(Key key) {
            // If we recursively find a constraint that permits us to use
            // this key, return true and skip any other constraint checks.
            if (nextConstraint != null && nextConstraint.permits(key)) {
                return true;
            }
            if (debug != null) {
                debug.println("KeySizeConstraints.permits(): " + algorithm);
            }

            return permitsImpl(key);
        }

        @Override
        public boolean permits(AlgorithmParameters parameters) {
            String paramAlg = parameters.getAlgorithm();
            if (!algorithm.equalsIgnoreCase(parameters.getAlgorithm())) {
                // Consider the impact of the algorithm aliases.
                Collection<String> aliases =
                        AlgorithmDecomposer.getAliases(algorithm);
                if (!aliases.contains(paramAlg)) {
                    return true;
                }
            }

            int keySize = KeyUtil.getKeySize(parameters);
            if (keySize == 0) {
                return false;
            } else if (keySize > 0) {
                return !((keySize < minSize) || (keySize > maxSize) ||
                    (prohibitedSize == keySize));
            }   // Otherwise, the key size is not accessible or determined.
                // Conservatively, please don't disable such keys.

            return true;
        }

        private boolean permitsImpl(Key key) {
            // Verify this constraint is for this public key algorithm
            if (algorithm.compareToIgnoreCase(key.getAlgorithm()) != 0) {
                return true;
            }

            int size = KeyUtil.getKeySize(key);
            if (size == 0) {
                return false;    // we don't allow any key of size 0.
            } else if (size > 0) {
                return !((size < minSize) || (size > maxSize) ||
                    (prohibitedSize == size));
            }   // Otherwise, the key size is not accessible. Conservatively,
                // please don't disable such keys.

            return true;
        }
    }

    /*
     * This constraint is used for the complete disabling of the algorithm.
     */
    private static class DisabledConstraint extends Constraint {
        DisabledConstraint(String algo) {
            algorithm = algo;
        }

        @Override
        public void permits(ConstraintsParameters cp)
                throws CertPathValidatorException {
            throw new CertPathValidatorException(
                    "Algorithm constraints check failed on disabled " +
                            "algorithm: " + algorithm + cp.extendedExceptionMsg(),
                    null, null, -1, BasicReason.ALGORITHM_CONSTRAINED);
        }

        @Override
        public boolean permits(Key key) {
            return false;
        }
    }
}
