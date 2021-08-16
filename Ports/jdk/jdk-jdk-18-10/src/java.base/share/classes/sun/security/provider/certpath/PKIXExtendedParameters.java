/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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


package sun.security.provider.certpath;

import java.security.InvalidAlgorithmParameterException;
import java.security.Timestamp;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.TrustAnchor;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This class is a wrapper for PKIXBuilderParameters so that a Timestamp object
 * and a string for the variant type, can be passed when doing certpath
 * checking.
 */

public class PKIXExtendedParameters extends PKIXBuilderParameters {

    private final PKIXBuilderParameters p;
    private Timestamp jarTimestamp;
    private final String variant;

    public PKIXExtendedParameters(PKIXBuilderParameters params,
            Timestamp timestamp, String variant)
            throws InvalidAlgorithmParameterException {
        super(params.getTrustAnchors(), null);
        p = params;
        jarTimestamp = timestamp;
        this.variant = variant;
    }

    public Timestamp getTimestamp() {
        return jarTimestamp;
    }
    public void setTimestamp(Timestamp t) {
        jarTimestamp = t;
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public void setDate(Date d) {
        p.setDate(d);
    }

    @Override
    public void addCertPathChecker(PKIXCertPathChecker c) {
        p.addCertPathChecker(c);
    }

    @Override
    public void setMaxPathLength(int maxPathLength) {
        p.setMaxPathLength(maxPathLength);
    }

    @Override
    public int getMaxPathLength() {
        return p.getMaxPathLength();
    }

    @Override
    public String toString() {
        return p.toString();
    }

    @Override
    public Set<TrustAnchor> getTrustAnchors() {
        return p.getTrustAnchors();
    }

    @Override
    public void setTrustAnchors(Set<TrustAnchor> trustAnchors)
            throws InvalidAlgorithmParameterException {
        // To avoid problems with PKIXBuilderParameter's constructors
        if (p == null) {
            return;
        }
        p.setTrustAnchors(trustAnchors);
    }

    @Override
    public Set<String> getInitialPolicies() {
        return p.getInitialPolicies();
    }

    @Override
    public void setInitialPolicies(Set<String> initialPolicies) {
        p.setInitialPolicies(initialPolicies);
    }

    @Override
    public void setCertStores(List<CertStore> stores) {
        p.setCertStores(stores);
    }

    @Override
    public void addCertStore(CertStore store) {
        p.addCertStore(store);
    }

    @Override
    public List<CertStore> getCertStores() {
        return p.getCertStores();
    }

    @Override
    public void setRevocationEnabled(boolean val) {
        p.setRevocationEnabled(val);
    }

    @Override
    public boolean isRevocationEnabled() {
        return p.isRevocationEnabled();
    }

    @Override
    public void setExplicitPolicyRequired(boolean val) {
        p.setExplicitPolicyRequired(val);
    }

    @Override
    public boolean isExplicitPolicyRequired() {
        return p.isExplicitPolicyRequired();
    }

    @Override
    public void setPolicyMappingInhibited(boolean val) {
        p.setPolicyMappingInhibited(val);
    }

    @Override
    public boolean isPolicyMappingInhibited() {
        return p.isPolicyMappingInhibited();
    }

    @Override
    public void setAnyPolicyInhibited(boolean val) {
        p.setAnyPolicyInhibited(val);
    }

    @Override
    public boolean isAnyPolicyInhibited() {
        return p.isAnyPolicyInhibited();
    }

    @Override
    public void setPolicyQualifiersRejected(boolean qualifiersRejected) {
        p.setPolicyQualifiersRejected(qualifiersRejected);
    }

    @Override
    public boolean getPolicyQualifiersRejected() {
        return p.getPolicyQualifiersRejected();
    }

    @Override
    public Date getDate() {
        return p.getDate();
    }

    @Override
    public void setCertPathCheckers(List<PKIXCertPathChecker> checkers) {
        p.setCertPathCheckers(checkers);
    }

    @Override
    public List<PKIXCertPathChecker> getCertPathCheckers() {
        return p.getCertPathCheckers();
    }

    @Override
    public String getSigProvider() {
        return p.getSigProvider();
    }

    @Override
    public void setSigProvider(String sigProvider) {
        p.setSigProvider(sigProvider);
    }

    @Override
    public CertSelector getTargetCertConstraints() {
        return p.getTargetCertConstraints();
    }

    @Override
    public void setTargetCertConstraints(CertSelector selector) {
        // To avoid problems with PKIXBuilderParameter's constructors
        if (p == null) {
            return;
        }
        p.setTargetCertConstraints(selector);
    }

}
