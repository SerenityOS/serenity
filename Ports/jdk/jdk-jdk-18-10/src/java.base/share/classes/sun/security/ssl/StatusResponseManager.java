/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.ssl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.cert.Extension;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import sun.security.action.GetBooleanAction;
import sun.security.action.GetIntegerAction;
import sun.security.action.GetPropertyAction;
import sun.security.provider.certpath.CertId;
import sun.security.provider.certpath.OCSP;
import sun.security.provider.certpath.OCSPResponse;
import sun.security.provider.certpath.ResponderId;
import sun.security.util.Cache;
import sun.security.x509.PKIXExtensions;
import sun.security.x509.SerialNumber;
import sun.security.ssl.X509Authentication.X509Possession;
import static sun.security.ssl.CertStatusExtension.*;

final class StatusResponseManager {
    private static final int DEFAULT_CORE_THREADS = 8;
    private static final int DEFAULT_CACHE_SIZE = 256;
    private static final int DEFAULT_CACHE_LIFETIME = 3600;     // seconds

    private final ScheduledThreadPoolExecutor threadMgr;
    private final Cache<CertId, ResponseCacheEntry> responseCache;
    private final URI defaultResponder;
    private final boolean respOverride;
    private final int cacheCapacity;
    private final int cacheLifetime;
    private final boolean ignoreExtensions;

    /**
     * Create a StatusResponseManager with default parameters.
     */
    StatusResponseManager() {
        @SuppressWarnings("removal")
        int cap = AccessController.doPrivileged(
                new GetIntegerAction("jdk.tls.stapling.cacheSize",
                    DEFAULT_CACHE_SIZE));
        cacheCapacity = cap > 0 ? cap : 0;

        @SuppressWarnings("removal")
        int life = AccessController.doPrivileged(
                new GetIntegerAction("jdk.tls.stapling.cacheLifetime",
                    DEFAULT_CACHE_LIFETIME));
        cacheLifetime = life > 0 ? life : 0;

        String uriStr = GetPropertyAction
                .privilegedGetProperty("jdk.tls.stapling.responderURI");
        URI tmpURI;
        try {
            tmpURI = ((uriStr != null && !uriStr.isEmpty()) ?
                    new URI(uriStr) : null);
        } catch (URISyntaxException urise) {
            tmpURI = null;
        }
        defaultResponder = tmpURI;

        respOverride = GetBooleanAction
                .privilegedGetProperty("jdk.tls.stapling.responderOverride");
        ignoreExtensions = GetBooleanAction
                .privilegedGetProperty("jdk.tls.stapling.ignoreExtensions");

        threadMgr = new ScheduledThreadPoolExecutor(DEFAULT_CORE_THREADS,
                new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        }, new ThreadPoolExecutor.DiscardPolicy());
        threadMgr.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        threadMgr.setContinueExistingPeriodicTasksAfterShutdownPolicy(
                false);
        threadMgr.setKeepAliveTime(5000, TimeUnit.MILLISECONDS);
        threadMgr.allowCoreThreadTimeOut(true);
        responseCache = Cache.newSoftMemoryCache(
                cacheCapacity, cacheLifetime);
    }

    /**
     * Get the current cache lifetime setting
     *
     * @return the current cache lifetime value
     */
    int getCacheLifetime() {
        return cacheLifetime;
    }

    /**
     * Get the current maximum cache size.
     *
     * @return the current maximum cache size
     */
    int getCacheCapacity() {
        return cacheCapacity;
    }

    /**
     * Get the default OCSP responder URI, if previously set.
     *
     * @return the current default OCSP responder URI, or {@code null} if
     *      it has not been set.
     */
    URI getDefaultResponder() {
        return defaultResponder;
    }

    /**
     * Get the URI override setting
     *
     * @return {@code true} if URI override has been set, {@code false}
     * otherwise.
     */
    boolean getURIOverride() {
        return respOverride;
    }

    /**
     * Get the ignore extensions setting.
     *
     * @return {@code true} if the {@code StatusResponseManager} will not
     * pass OCSP Extensions in the TLS {@code status_request[_v2]}
     * extensions, {@code false} if extensions will be passed (the default).
     */
    boolean getIgnoreExtensions() {
        return ignoreExtensions;
    }

    /**
     * Clear the status response cache
     */
    void clear() {
        if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
            SSLLogger.fine("Clearing response cache");
        }
        responseCache.clear();
    }

    /**
     * Returns the number of currently valid objects in the response cache.
     *
     * @return the number of valid objects in the response cache.
     */
    int size() {
        return responseCache.size();
    }

    /**
     * Obtain the URI use by the {@code StatusResponseManager} during
     * lookups.
     *
     * This method takes into account not only the AIA extension from a
     * certificate to be checked, but also any default URI and possible
     * override settings for the response manager.
     *
     * @param cert the subject to get the responder URI from
     *
     * @return a {@code URI} containing the address to the OCSP responder,
     *      or {@code null} if no AIA extension exists in the certificate
     *      and no default responder has been configured.
     *
     * @throws NullPointerException if {@code cert} is {@code null}.
     */
    URI getURI(X509Certificate cert) {
        Objects.requireNonNull(cert);

        if (cert.getExtensionValue(
                PKIXExtensions.OCSPNoCheck_Id.toString()) != null) {
            if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                SSLLogger.fine(
                    "OCSP NoCheck extension found.  OCSP will be skipped");
            }
            return null;
        } else if (defaultResponder != null && respOverride) {
            if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
              SSLLogger.fine(
                    "Responder override: URI is " + defaultResponder);
            }
            return defaultResponder;
        } else {
            URI certURI = OCSP.getResponderURI(cert);
            return (certURI != null ? certURI : defaultResponder);
        }
    }

    /**
     * Shutdown the thread pool
     */
    void shutdown() {
        if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
            SSLLogger.fine("Shutting down " + threadMgr.getActiveCount() +
                    " active threads");
        }
        threadMgr.shutdown();
    }

    /**
     * Get a list of responses for a chain of certificates.
     *
     * This will find OCSP responses from the cache, or failing that,
     * directly contact the OCSP responder.  It is assumed that the
     * certificates in the provided chain are in their proper order
     * (from end-entity to trust anchor).
     *
     * @param type the type of request being made of the
     *      {@code StatusResponseManager}
     * @param request the {@code CertStatusRequest} from the
     *      status_request or status_request_v2 ClientHello extension.
     *      A value of {@code null} is interpreted as providing no
     *      responder IDs or extensions.
     * @param chain an array of 2 or more certificates.  Each certificate
     *      must be issued by the next certificate in the chain.
     * @param delay the number of time units to delay before returning
     *      responses.
     * @param unit the unit of time applied to the {@code delay} parameter
     *
     * @return an unmodifiable {@code Map} containing the certificate and
     *      its usually
     */
    Map<X509Certificate, byte[]> get(CertStatusRequestType type,
            CertStatusRequest request, X509Certificate[] chain, long delay,
            TimeUnit unit) {
        Map<X509Certificate, byte[]> responseMap = new HashMap<>();
        List<OCSPFetchCall> requestList = new ArrayList<>();

        if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
            SSLLogger.fine(
                "Beginning check: Type = " + type + ", Chain length = " +
                chain.length);
        }

        // It is assumed that the caller has ordered the certs in the chain
        // in the proper order (each certificate is issued by the next entry
        // in the provided chain).
        if (chain.length < 2) {
            return Collections.emptyMap();
        }

        if (type == CertStatusRequestType.OCSP) {
            try {
                // For type OCSP, we only check the end-entity certificate
                OCSPStatusRequest ocspReq = (OCSPStatusRequest)request;
                CertId cid = new CertId(chain[1],
                        new SerialNumber(chain[0].getSerialNumber()));
                ResponseCacheEntry cacheEntry = getFromCache(cid, ocspReq);
                if (cacheEntry != null) {
                    responseMap.put(chain[0], cacheEntry.ocspBytes);
                } else {
                    StatusInfo sInfo = new StatusInfo(chain[0], cid);
                    requestList.add(new OCSPFetchCall(sInfo, ocspReq));
                }
            } catch (IOException exc) {
                if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                    SSLLogger.fine(
                        "Exception during CertId creation: ", exc);
                }
            }
        } else if (type == CertStatusRequestType.OCSP_MULTI) {
            // For type OCSP_MULTI, we check every cert in the chain that
            // has a direct issuer at the next index.  We won't have an
            // issuer certificate for the last certificate in the chain
            // and will not be able to create a CertId because of that.
            OCSPStatusRequest ocspReq = (OCSPStatusRequest)request;
            int ctr;
            for (ctr = 0; ctr < chain.length - 1; ctr++) {
                try {
                    // The cert at "ctr" is the subject cert, "ctr + 1"
                    // is the issuer certificate.
                    CertId cid = new CertId(chain[ctr + 1],
                        new SerialNumber(chain[ctr].getSerialNumber()));
                    ResponseCacheEntry cacheEntry =
                        getFromCache(cid, ocspReq);
                    if (cacheEntry != null) {
                        responseMap.put(chain[ctr], cacheEntry.ocspBytes);
                    } else {
                        StatusInfo sInfo = new StatusInfo(chain[ctr], cid);
                        requestList.add(new OCSPFetchCall(sInfo, ocspReq));
                    }
                } catch (IOException exc) {
                    if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                        SSLLogger.fine(
                            "Exception during CertId creation: ", exc);
                    }
                }
            }
        } else {
            if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                SSLLogger.fine("Unsupported status request type: " + type);
            }
        }

        // If we were able to create one or more Fetches, go and run all
        // of them in separate threads.  For all the threads that completed
        // in the allotted time, put those status responses into the
        // returned Map.
        if (!requestList.isEmpty()) {
            try {
                // Set a bunch of threads to go do the fetching
                List<Future<StatusInfo>> resultList =
                        threadMgr.invokeAll(requestList, delay, unit);

                // Go through the Futures and from any non-cancelled task,
                // get the bytes and attach them to the responseMap.
                for (Future<StatusInfo> task : resultList) {
                    if (!task.isDone()) {
                        continue;
                    }

                    if (!task.isCancelled()) {
                        StatusInfo info = task.get();
                        if (info != null && info.responseData != null) {
                            responseMap.put(info.cert,
                                    info.responseData.ocspBytes);
                        } else if (SSLLogger.isOn &&
                                SSLLogger.isOn("respmgr")) {
                            SSLLogger.fine(
                                "Completed task had no response data");
                        }
                    } else {
                        if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                            SSLLogger.fine("Found cancelled task");
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException exc) {
                // Not sure what else to do here
                if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                    SSLLogger.fine("Exception when getting data: ", exc);
                }
            }
        }

        return Collections.unmodifiableMap(responseMap);
    }

    /**
     * Check the cache for a given {@code CertId}.
     *
     * @param cid the CertId of the response to look up
     * @param ocspRequest the OCSP request structure sent by the client
     *      in the TLS status_request[_v2] hello extension.
     *
     * @return the {@code ResponseCacheEntry} for a specific CertId, or
     *      {@code null} if it is not found or a nonce extension has been
     *      requested by the caller.
     */
    private ResponseCacheEntry getFromCache(CertId cid,
            OCSPStatusRequest ocspRequest) {
        // Determine if the nonce extension is present in the request.  If
        // so, then do not attempt to retrieve the response from the cache.
        for (Extension ext : ocspRequest.extensions) {
            if (ext.getId().equals(
                    PKIXExtensions.OCSPNonce_Id.toString())) {
                if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                    SSLLogger.fine(
                            "Nonce extension found, skipping cache check");
                }
                return null;
            }
        }

        ResponseCacheEntry respEntry = responseCache.get(cid);

        // If the response entry has a nextUpdate and it has expired
        // before the cache expiration, purge it from the cache
        // and do not return it as a cache hit.
        if (respEntry != null && respEntry.nextUpdate != null &&
                respEntry.nextUpdate.before(new Date())) {
            if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                SSLLogger.fine(
                    "nextUpdate threshold exceeded, purging from cache");
            }
            respEntry = null;
        }

        if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
            SSLLogger.fine(
                    "Check cache for SN" + cid.getSerialNumber() + ": " +
                    (respEntry != null ? "HIT" : "MISS"));
        }
        return respEntry;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("StatusResponseManager: ");

        sb.append("Core threads: ").append(threadMgr.getCorePoolSize());
        sb.append(", Cache timeout: ");
        if (cacheLifetime > 0) {
            sb.append(cacheLifetime).append(" seconds");
        } else {
            sb.append(" indefinite");
        }

        sb.append(", Cache MaxSize: ");
        if (cacheCapacity > 0) {
            sb.append(cacheCapacity).append(" items");
        } else {
            sb.append(" unbounded");
        }

        sb.append(", Default URI: ");
        if (defaultResponder != null) {
            sb.append(defaultResponder);
        } else {
            sb.append("NONE");
        }

        return sb.toString();
    }

    /**
     * Inner class used to group request and response data.
     */
    class StatusInfo {
        final X509Certificate cert;
        final CertId cid;
        final URI responder;
        ResponseCacheEntry responseData;

        /**
         * Create a StatusInfo object from certificate data.
         *
         * @param subjectCert the certificate to be checked for revocation
         * @param issuerCert the issuer of the {@code subjectCert}
         *
         * @throws IOException if CertId creation from the certificate fails
         */
        StatusInfo(X509Certificate subjectCert, X509Certificate issuerCert)
                throws IOException {
            this(subjectCert, new CertId(issuerCert,
                    new SerialNumber(subjectCert.getSerialNumber())));
        }

        /**
         * Create a StatusInfo object from an existing subject certificate
         * and its corresponding CertId.
         *
         * @param subjectCert the certificate to be checked for revocation
         * @param certId the CertId for {@code subjectCert}
         */
        StatusInfo(X509Certificate subjectCert, CertId certId) {
            cert = subjectCert;
            cid = certId;
            responder = getURI(cert);
            responseData = null;
        }

        /**
         * Copy constructor (used primarily for rescheduling).
         * This will do a member-wise copy with the exception of the
         * responseData and extensions fields, which should not persist
         * in a rescheduled fetch.
         *
         * @param orig the original {@code StatusInfo}
         */
        StatusInfo(StatusInfo orig) {
            this.cert = orig.cert;
            this.cid = orig.cid;
            this.responder = orig.responder;
            this.responseData = null;
        }

        /**
         * Return a String representation of the {@code StatusInfo}
         *
         * @return a {@code String} representation of this object
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("StatusInfo:");
            sb.append("\n\tCert: ").append(
                    this.cert.getSubjectX500Principal());
            sb.append("\n\tSerial: ").append(this.cert.getSerialNumber());
            sb.append("\n\tResponder: ").append(this.responder);
            sb.append("\n\tResponse data: ").append(
                    this.responseData != null ?
                        (this.responseData.ocspBytes.length + " bytes") :
                        "<NULL>");
            return sb.toString();
        }
    }

    /**
     * Static nested class used as the data kept in the response cache.
     */
    static class ResponseCacheEntry {
        final OCSPResponse.ResponseStatus status;
        final byte[] ocspBytes;
        final Date nextUpdate;
        final OCSPResponse.SingleResponse singleResp;
        final ResponderId respId;

        /**
         * Create a new cache entry from the raw bytes of the response
         *
         * @param responseBytes the DER encoding for the OCSP response
         *
         * @throws IOException if an {@code OCSPResponse} cannot be
         *         created from the encoded bytes.
         */
        ResponseCacheEntry(byte[] responseBytes, CertId cid)
                throws IOException {
            Objects.requireNonNull(responseBytes,
                    "Non-null responseBytes required");
            Objects.requireNonNull(cid, "Non-null Cert ID required");

            ocspBytes = responseBytes.clone();
            OCSPResponse oResp = new OCSPResponse(ocspBytes);
            status = oResp.getResponseStatus();
            respId = oResp.getResponderId();
            singleResp = oResp.getSingleResponse(cid);
            if (status == OCSPResponse.ResponseStatus.SUCCESSFUL) {
                if (singleResp != null) {
                    // Pull out the nextUpdate field in advance because the
                    // Date is cloned.
                    nextUpdate = singleResp.getNextUpdate();
                } else {
                    throw new IOException(
                            "Unable to find SingleResponse for SN " +
                            cid.getSerialNumber());
                }
            } else {
                nextUpdate = null;
            }
        }
    }

    /**
     * Inner Callable class that does the actual work of looking up OCSP
     * responses, first looking at the cache and doing OCSP requests if
     * a cache miss occurs.
     */
    class OCSPFetchCall implements Callable<StatusInfo> {
        StatusInfo statInfo;
        OCSPStatusRequest ocspRequest;
        List<Extension> extensions;
        List<ResponderId> responderIds;

        /**
         * A constructor that builds the OCSPFetchCall from the provided
         * StatusInfo and information from the status_request[_v2]
         * extension.
         *
         * @param info the {@code StatusInfo} containing the subject
         * certificate, CertId, and other supplemental info.
         * @param request the {@code OCSPStatusRequest} containing any
         * responder IDs and extensions.
         */
        public OCSPFetchCall(StatusInfo info, OCSPStatusRequest request) {
            statInfo = Objects.requireNonNull(info,
                    "Null StatusInfo not allowed");
            ocspRequest = Objects.requireNonNull(request,
                    "Null OCSPStatusRequest not allowed");
            extensions = ocspRequest.extensions;
            responderIds = ocspRequest.responderIds;
        }

        /**
         * Get an OCSP response, either from the cache or from a responder.
         *
         * @return The StatusInfo object passed into the
         *         {@code OCSPFetchCall} constructor, with the
         *         {@code responseData} field filled in with the response
         *         or {@code null} if no response can be obtained.
         */
        @Override
        public StatusInfo call() {
            if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                SSLLogger.fine(
                    "Starting fetch for SN " +
                    statInfo.cid.getSerialNumber());
            }
            try {
                ResponseCacheEntry cacheEntry;
                List<Extension> extsToSend;

                if (statInfo.responder == null) {
                    // If we have no URI then there's nothing to do
                    // but return.
                    if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                        SSLLogger.fine(
                            "Null URI detected, OCSP fetch aborted");
                    }
                    return statInfo;
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                        SSLLogger.fine(
                            "Attempting fetch from " + statInfo.responder);
                    }
                }

                // If the StatusResponseManager has been configured to not
                // forward extensions, then set extensions to an empty
                // list.
                //
                // We will forward the extensions unless one of two
                // conditions occur:
                // (1) The jdk.tls.stapling.ignoreExtensions property is
                //     true, or
                // (2) There is a non-empty ResponderId list.
                //
                // ResponderId selection is a feature that will be
                // supported in the future.
                extsToSend = (ignoreExtensions || !responderIds.isEmpty()) ?
                        Collections.emptyList() : extensions;

                byte[] respBytes = OCSP.getOCSPBytes(
                        Collections.singletonList(statInfo.cid),
                        statInfo.responder, extsToSend);

                if (respBytes != null) {
                    // Place the data into the response cache
                    cacheEntry = new ResponseCacheEntry(respBytes,
                            statInfo.cid);

                    // Get the response status and act on it appropriately
                    if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                        SSLLogger.fine("OCSP Status: " + cacheEntry.status +
                            " (" + respBytes.length + " bytes)");
                    }
                    if (cacheEntry.status ==
                            OCSPResponse.ResponseStatus.SUCCESSFUL) {
                        // Set the response in the returned StatusInfo
                        statInfo.responseData = cacheEntry;

                        // Add the response to the cache (if applicable)
                        addToCache(statInfo.cid, cacheEntry);
                    }
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                        SSLLogger.fine(
                            "No data returned from OCSP Responder");
                    }
                }
            } catch (IOException ioe) {
                if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                    SSLLogger.fine("Caught exception: ", ioe);
                }
            }

            return statInfo;
        }

        /**
         * Add a response to the cache.
         *
         * @param certId The {@code CertId} for the OCSP response
         * @param entry A cache entry containing the response bytes and
         *      the {@code OCSPResponse} built from those bytes.
         */
        private void addToCache(CertId certId, ResponseCacheEntry entry) {
            // If no cache lifetime has been set on entries then
            // don't cache this response if there is no nextUpdate field
            if (entry.nextUpdate == null && cacheLifetime == 0) {
                if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                    SSLLogger.fine("Not caching this OCSP response");
                }
            } else {
                responseCache.put(certId, entry);
                if (SSLLogger.isOn && SSLLogger.isOn("respmgr")) {
                    SSLLogger.fine(
                        "Added response for SN " +
                        certId.getSerialNumber() +
                        " to cache");
                }
            }
        }

        /**
         * Determine the delay to use when scheduling the task that will
         * update the OCSP response.  This is the shorter time between the
         * cache lifetime and the nextUpdate.  If no nextUpdate is present
         * in the response, then only the cache lifetime is used.
         * If cache timeouts are disabled (a zero value) and there's no
         * nextUpdate, then the entry is not cached and no rescheduling
         * will take place.
         *
         * @param nextUpdate a {@code Date} object corresponding to the
         *      next update time from a SingleResponse.
         *
         * @return the number of seconds of delay before the next fetch
         *      should be executed.  A zero value means that the fetch
         *      should happen immediately, while a value less than zero
         *      indicates no rescheduling should be done.
         */
        private long getNextTaskDelay(Date nextUpdate) {
            long delaySec;
            int lifetime = getCacheLifetime();

            if (nextUpdate != null) {
                long nuDiffSec = (nextUpdate.getTime() -
                        System.currentTimeMillis()) / 1000;
                delaySec = lifetime > 0 ? Long.min(nuDiffSec, lifetime) :
                        nuDiffSec;
            } else {
                delaySec = lifetime > 0 ? lifetime : -1;
            }

            return delaySec;
        }
    }

    static final StaplingParameters processStapling(
            ServerHandshakeContext shc) {
        StaplingParameters params = null;
        SSLExtension ext = null;
        CertStatusRequestType type = null;
        CertStatusRequest req = null;
        Map<X509Certificate, byte[]> responses;

        // If this feature has not been enabled, then no more processing
        // is necessary.  Also we will only staple if we're doing a full
        // handshake.
        if (!shc.sslContext.isStaplingEnabled(false) || shc.isResumption) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("Staping disabled or is a resumed session");
            }
            return null;
        }

        // Check if the client has asserted the status_request[_v2] extension(s)
        Map<SSLExtension, SSLExtension.SSLExtensionSpec> exts =
                shc.handshakeExtensions;
        CertStatusRequestSpec statReq = (CertStatusRequestSpec)exts.get(
                SSLExtension.CH_STATUS_REQUEST);
        CertStatusRequestV2Spec statReqV2 = (CertStatusRequestV2Spec)
                exts.get(SSLExtension.CH_STATUS_REQUEST_V2);

        // Determine which type of stapling we are doing and assert the
        // proper extension in the server hello.
        // Favor status_request_v2 over status_request and ocsp_multi
        // over ocsp.
        // If multiple ocsp or ocsp_multi types exist, select the first
        // instance of a given type.  Also since we don't support ResponderId
        // selection yet, only accept a request if the ResponderId field
        // is empty.  Finally, we'll only do this in (D)TLS 1.2 or earlier.
        if (statReqV2 != null && !shc.negotiatedProtocol.useTLS13PlusSpec()) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake,verbose")) {
                SSLLogger.fine("SH Processing status_request_v2 extension");
            }
            // RFC 6961 stapling
            ext = SSLExtension.CH_STATUS_REQUEST_V2;
            int ocspIdx = -1;
            int ocspMultiIdx = -1;
            CertStatusRequest[] reqItems = statReqV2.certStatusRequests;
            for (int pos = 0; (pos < reqItems.length &&
                    (ocspIdx == -1 || ocspMultiIdx == -1)); pos++) {
                CertStatusRequest item = reqItems[pos];
                CertStatusRequestType curType =
                        CertStatusRequestType.valueOf(item.statusType);
                if (ocspIdx < 0 && curType == CertStatusRequestType.OCSP) {
                    OCSPStatusRequest ocspReq = (OCSPStatusRequest)item;
                    // We currently only accept empty responder ID lists
                    // but may support them in the future
                    if (ocspReq.responderIds.isEmpty()) {
                        ocspIdx = pos;
                    }
                } else if (ocspMultiIdx < 0 &&
                        curType == CertStatusRequestType.OCSP_MULTI) {
                    OCSPStatusRequest ocspReq = (OCSPStatusRequest)item;
                    // We currently only accept empty responder ID lists
                    // but may support them in the future
                    if (ocspReq.responderIds.isEmpty()) {
                        ocspMultiIdx = pos;
                    }
                }
            }
            if (ocspMultiIdx >= 0) {
                req = reqItems[ocspMultiIdx];
                type = CertStatusRequestType.valueOf(req.statusType);
            } else if (ocspIdx >= 0) {
                req = reqItems[ocspIdx];
                type = CertStatusRequestType.valueOf(req.statusType);
            } else {
                if (SSLLogger.isOn &&
                        SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest("Warning: No suitable request " +
                            "found in the status_request_v2 extension.");
                }
            }
        }

        // Only attempt to process a status_request extension if:
        // * The status_request extension is set AND
        // * either the status_request_v2 extension is not present OR
        // * none of the underlying OCSPStatusRequest structures is
        // suitable for stapling.
        // If either of the latter two bullet items is true the ext,
        // type and req variables should all be null.  If any are null
        // we will try processing an asserted status_request.
        if ((statReq != null) &&
                (ext == null || type == null || req == null)) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake,verbose")) {
                SSLLogger.fine("SH Processing status_request extension");
            }
            ext = SSLExtension.CH_STATUS_REQUEST;
            type = CertStatusRequestType.valueOf(
                    statReq.statusRequest.statusType);
            if (type == CertStatusRequestType.OCSP) {
                // If the type is OCSP, then the request is guaranteed
                // to be OCSPStatusRequest
                OCSPStatusRequest ocspReq =
                        (OCSPStatusRequest)statReq.statusRequest;
                if (ocspReq.responderIds.isEmpty()) {
                    req = ocspReq;
                } else {
                    if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                        SSLLogger.finest("Warning: No suitable request " +
                            "found in the status_request extension.");
                    }
                }
            }
        }

        // If, after walking through the extensions we were unable to
        // find a suitable StatusRequest, then stapling is disabled.
        // The ext, type and req variables must have been set to continue.
        if (type == null || req == null || ext == null) {
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.fine("No suitable status_request or " +
                        "status_request_v2, stapling is disabled");
            }
            return null;
        }

        // Get the cert chain since we'll need it for OCSP checking
        X509Possession x509Possession = null;
        for (SSLPossession possession : shc.handshakePossessions) {
            if (possession instanceof X509Possession) {
                x509Possession = (X509Possession)possession;
                break;
            }
        }

        if (x509Possession == null) {       // unlikely
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.finest("Warning: no X.509 certificates found.  " +
                        "Stapling is disabled.");
            }
            return null;
        }

        // Get the OCSP responses from the StatusResponseManager
        X509Certificate[] certs = x509Possession.popCerts;
        StatusResponseManager statRespMgr =
                shc.sslContext.getStatusResponseManager();
        if (statRespMgr != null) {
            // For the purposes of the fetch from the SRM, override the
            // type when it is TLS 1.3 so it always gets responses for
            // all certs it can.  This should not change the type field
            // in the StaplingParameters though.
            CertStatusRequestType fetchType =
                    shc.negotiatedProtocol.useTLS13PlusSpec() ?
                    CertStatusRequestType.OCSP_MULTI : type;
            responses = statRespMgr.get(fetchType, req, certs,
                    shc.statusRespTimeout, TimeUnit.MILLISECONDS);
            if (!responses.isEmpty()) {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest("Response manager returned " +
                            responses.size() + " entries.");
                }
                // If this RFC 6066-style stapling (SSL cert only) then the
                // response cannot be zero length
                if (type == CertStatusRequestType.OCSP) {
                    byte[] respDER = responses.get(certs[0]);
                    if (respDER == null || respDER.length <= 0) {
                        if (SSLLogger.isOn &&
                                SSLLogger.isOn("ssl,handshake")) {
                            SSLLogger.finest("Warning: Null or zero-length " +
                                    "response found for leaf certificate. " +
                                    "Stapling is disabled.");
                        }
                        return null;
                    }
                }
                params = new StaplingParameters(ext, type, req, responses);
            } else {
                if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                    SSLLogger.finest("Warning: no OCSP responses obtained.  " +
                            "Stapling is disabled.");
                }
            }
        } else {
            // This should not happen, but if lazy initialization of the
            // StatusResponseManager doesn't occur we should turn off stapling.
            if (SSLLogger.isOn && SSLLogger.isOn("ssl,handshake")) {
                SSLLogger.finest("Warning: lazy initialization " +
                        "of the StatusResponseManager failed.  " +
                        "Stapling is disabled.");
            }
            params = null;
        }

        return params;
    }

    /**
     * Inner class used to hold stapling parameters needed by the handshaker
     * when stapling is active.
     */
    static final class StaplingParameters {
        final SSLExtension statusRespExt;
        final CertStatusRequestType statReqType;
        final CertStatusRequest statReqData;
        final Map<X509Certificate, byte[]> responseMap;

        StaplingParameters(SSLExtension ext, CertStatusRequestType type,
                CertStatusRequest req, Map<X509Certificate, byte[]> responses) {
            statusRespExt = ext;
            statReqType = type;
            statReqData = req;
            responseMap = responses;
        }
    }
}

