/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.validator;

import java.util.*;

import java.security.AlgorithmConstraints;
import java.security.KeyStore;
import java.security.cert.*;

/**
 * Validator abstract base class. Concrete classes are instantiated by calling
 * one of the getInstance() methods. All methods defined in this class
 * must be safe for concurrent use by multiple threads.<p>
 *
 * The model is that a Validator instance is created specifying validation
 * settings, such as trust anchors or PKIX parameters. Then one or more
 * paths are validated using those parameters. In some cases, additional
 * information can be provided per path validation. This is independent of
 * the validation parameters and currently only used for TLS server validation.
 * <p>
 * Path validation is performed by calling one of the validate() methods. It
 * specifies a suggested path to be used for validation if available, or only
 * the end entity certificate otherwise. Optionally additional certificates can
 * be specified that the caller believes could be helpful. Implementations are
 * free to make use of this information or validate the path using other means.
 * validate() also checks that the end entity certificate is suitable for the
 * intended purpose as described below.
 *
 * <p>There are two orthogonal parameters to select the Validator
 * implementation: type and variant. Type selects the validation algorithm.
 * Currently supported are TYPE_SIMPLE and TYPE_PKIX. See SimpleValidator and
 * PKIXValidator for details.
 * <p>
 * Variant controls additional extension checks. Currently supported are
 * five variants:
 * <ul>
 * <li>VAR_GENERIC (no additional checks),
 * <li>VAR_TLS_CLIENT (TLS client specific checks)
 * <li>VAR_TLS_SERVER (TLS server specific checks), and
 * <li>VAR_CODE_SIGNING (code signing specific checks).
 * <li>VAR_JCE_SIGNING (JCE code signing specific checks).
 * <li>VAR_TSA_SERVER (TSA server specific checks).
 * </ul>
 * See EndEntityChecker for more information.
 * <p>
 * Examples:
 * <pre>
 *   // instantiate validator specifying type, variant, and trust anchors
 *   Validator validator = Validator.getInstance(Validator.TYPE_PKIX,
 *                                               Validator.VAR_TLS_CLIENT,
 *                                               trustedCerts);
 *   // validate one or more chains using the validator
 *   validator.validate(chain); // throws CertificateException if failed
 * </pre>
 *
 * @see SimpleValidator
 * @see PKIXValidator
 * @see EndEntityChecker
 *
 * @author Andreas Sterbenz
 */
public abstract class Validator {

    static final X509Certificate[] CHAIN0 = {};

    /**
     * Constant for a validator of type Simple.
     * @see #getInstance
     */
    public static final String TYPE_SIMPLE = "Simple";

    /**
     * Constant for a validator of type PKIX.
     * @see #getInstance
     */
    public static final String TYPE_PKIX = "PKIX";

    /**
     * Constant for a Generic variant of a validator.
     * @see #getInstance
     */
    public static final String VAR_GENERIC = "generic";

    /**
     * Constant for a Code Signing variant of a validator.
     * @see #getInstance
     */
    public static final String VAR_CODE_SIGNING = "code signing";

    /**
     * Constant for a JCE Code Signing variant of a validator.
     * @see #getInstance
     */
    public static final String VAR_JCE_SIGNING = "jce signing";

    /**
     * Constant for a TLS Client variant of a validator.
     * @see #getInstance
     */
    public static final String VAR_TLS_CLIENT = "tls client";

    /**
     * Constant for a TLS Server variant of a validator.
     * @see #getInstance
     */
    public static final String VAR_TLS_SERVER = "tls server";

    /**
     * Constant for a TSA Server variant of a validator.
     * @see #getInstance
     */
    public static final String VAR_TSA_SERVER = "tsa server";

    private final String type;
    final EndEntityChecker endEntityChecker;
    final String variant;

    /**
     * @deprecated
     * @see #setValidationDate
     */
    @Deprecated
    volatile Date validationDate;

    Validator(String type, String variant) {
        this.type = type;
        this.variant = variant;
        endEntityChecker = EndEntityChecker.getInstance(type, variant);
    }

    /**
     * Get a new Validator instance using the trusted certificates from the
     * specified KeyStore as trust anchors.
     */
    public static Validator getInstance(String type, String variant,
            KeyStore ks) {
        return getInstance(type, variant, TrustStoreUtil.getTrustedCerts(ks));
    }

    /**
     * Get a new Validator instance using the Set of X509Certificates as trust
     * anchors.
     */
    public static Validator getInstance(String type, String variant,
            Collection<X509Certificate> trustedCerts) {
        if (type.equals(TYPE_SIMPLE)) {
            return new SimpleValidator(variant, trustedCerts);
        } else if (type.equals(TYPE_PKIX)) {
            return new PKIXValidator(variant, trustedCerts);
        } else {
            throw new IllegalArgumentException
                ("Unknown validator type: " + type);
        }
    }

    /**
     * Get a new Validator instance using the provided PKIXBuilderParameters.
     * This method can only be used with the PKIX validator.
     */
    public static Validator getInstance(String type, String variant,
            PKIXBuilderParameters params) {
        if (type.equals(TYPE_PKIX) == false) {
            throw new IllegalArgumentException
                ("getInstance(PKIXBuilderParameters) can only be used "
                + "with PKIX validator");
        }
        return new PKIXValidator(variant, params);
    }

    /**
     * Validate the given certificate chain.
     */
    public final X509Certificate[] validate(X509Certificate[] chain)
            throws CertificateException {
        return validate(chain, null, null);
    }

    /**
     * Validate the given certificate chain. If otherCerts is non-null, it is
     * a Collection of additional X509Certificates that could be helpful for
     * path building.
     */
    public final X509Certificate[] validate(X509Certificate[] chain,
        Collection<X509Certificate> otherCerts) throws CertificateException {
        return validate(chain, otherCerts, null);
    }

    /**
     * Validate the given certificate chain. If otherCerts is non-null, it is
     * a Collection of additional X509Certificates that could be helpful for
     * path building.
     *
     * @return a non-empty chain that was used to validate the path. The
     * end entity cert is at index 0, the trust anchor at index n-1.
     */
    public final X509Certificate[] validate(X509Certificate[] chain,
            Collection<X509Certificate> otherCerts, Object parameter)
            throws CertificateException {
        return validate(chain, otherCerts, Collections.emptyList(), null,
                parameter);
    }

    /**
     * Validate the given certificate chain.
     *
     * @param chain the target certificate chain
     * @param otherCerts a Collection of additional X509Certificates that
     *        could be helpful for path building (or null)
     * @param responseList a List of zero or more byte arrays, each
     *        one being a DER-encoded OCSP response (per RFC 6960).  Entries
     *        in the List must match the order of the certificates in the
     *        chain parameter.  It is possible that fewer responses may be
     *        in the list than are elements in {@code chain} and a missing
     *        response for a matching element in {@code chain} can be
     *        represented with a zero-length byte array.
     * @param constraints algorithm constraints for certification path
     *        processing
     * @param parameter an additional parameter object to pass specific data.
     *        This parameter object maybe one of the two below:
     *        1) TLS_SERVER variant validators, where it must be non null and
     *        the name of the TLS key exchange algorithm being used
     *        (see JSSE X509TrustManager specification).
     *        2) {@code Timestamp} object from a signed JAR file.
     * @return a non-empty chain that was used to validate the path. The
     *        end entity cert is at index 0, the trust anchor at index n-1.
     */
    public final X509Certificate[] validate(X509Certificate[] chain,
                Collection<X509Certificate> otherCerts,
                List<byte[]> responseList,
                AlgorithmConstraints constraints,
                Object parameter) throws CertificateException {
        chain = engineValidate(chain, otherCerts, responseList, constraints,
                parameter);

        // omit EE extension check if EE cert is also trust anchor
        if (chain.length > 1) {
            // EndEntityChecker does not need to check unresolved critical
            // extensions when validating with a TYPE_PKIX Validator.
            // A TYPE_PKIX Validator will already have run checks on all
            // certs' extensions, including checks by any PKIXCertPathCheckers
            // included in the PKIXParameters, so the extra checks would be
            // redundant.
            boolean checkUnresolvedCritExts =
                    (type == TYPE_PKIX) ? false : true;
            endEntityChecker.check(chain, parameter,
                                   checkUnresolvedCritExts);
        }

        return chain;
    }

    abstract X509Certificate[] engineValidate(X509Certificate[] chain,
                Collection<X509Certificate> otherCerts,
                List<byte[]> responseList,
                AlgorithmConstraints constraints,
                Object parameter) throws CertificateException;

    /**
     * Returns an immutable Collection of the X509Certificates this instance
     * uses as trust anchors.
     */
    public abstract Collection<X509Certificate> getTrustedCertificates();

    /**
     * Set the date to be used for subsequent validations. NOTE that
     * this is not a supported API, it is provided to simplify
     * writing tests only.
     *
     * @deprecated
     */
    @Deprecated
    public void setValidationDate(Date validationDate) {
        this.validationDate = validationDate;
    }

}
