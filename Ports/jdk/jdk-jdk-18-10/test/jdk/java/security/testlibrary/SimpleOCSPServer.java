/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.testlibrary;

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CRLReason;
import java.security.cert.X509Certificate;
import java.security.cert.Extension;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.Signature;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.math.BigInteger;

import sun.security.x509.*;
import sun.security.x509.PKIXExtensions;
import sun.security.provider.certpath.ResponderId;
import sun.security.provider.certpath.CertId;
import sun.security.provider.certpath.OCSPResponse;
import sun.security.provider.certpath.OCSPResponse.ResponseStatus;
import sun.security.util.*;


/**
 * This is a simple OCSP server designed to listen and respond to incoming
 * requests.
 */
public class SimpleOCSPServer {
    private final Debug debug = Debug.getInstance("oserv");
    private static final ObjectIdentifier OCSP_BASIC_RESPONSE_OID =
            ObjectIdentifier.of(KnownOIDs.OCSPBasicResponse);

    private static final SimpleDateFormat utcDateFmt =
            new SimpleDateFormat("MMM dd yyyy, HH:mm:ss z");

    static final int FREE_PORT = 0;

    // CertStatus values
    public static enum CertStatus {
        CERT_STATUS_GOOD,
        CERT_STATUS_REVOKED,
        CERT_STATUS_UNKNOWN,
    }

    // Fields used for the networking portion of the responder
    private ServerSocket servSocket;
    private InetAddress listenAddress;
    private int listenPort;

    // Keystore information (certs, keys, etc.)
    private KeyStore keystore;
    private X509Certificate issuerCert;
    private X509Certificate signerCert;
    private PrivateKey signerKey;

    // Fields used for the operational portions of the server
    private boolean logEnabled = false;
    private ExecutorService threadPool;
    private volatile boolean started = false;
    private volatile boolean serverReady = false;
    private volatile boolean receivedShutdown = false;
    private volatile boolean acceptConnections = true;
    private volatile long delayMsec = 0;

    // Fields used in the generation of responses
    private long nextUpdateInterval = -1;
    private Date nextUpdate = null;
    private ResponderId respId;
    private AlgorithmId sigAlgId;
    private Map<CertId, CertStatusInfo> statusDb =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * Construct a SimpleOCSPServer using keystore, password, and alias
     * parameters.
     *
     * @param ks the keystore to be used
     * @param password the password to access key material in the keystore
     * @param issuerAlias the alias of the issuer certificate
     * @param signerAlias the alias of the signer certificate and key.  A
     * value of {@code null} means that the {@code issuerAlias} will be used
     * to look up the signer key.
     *
     * @throws GeneralSecurityException if there are problems accessing the
     * keystore or finding objects within the keystore.
     * @throws IOException if a {@code ResponderId} cannot be generated from
     * the signer certificate.
     */
    public SimpleOCSPServer(KeyStore ks, String password, String issuerAlias,
            String signerAlias) throws GeneralSecurityException, IOException {
        this(null, FREE_PORT, ks, password, issuerAlias, signerAlias);
    }

    /**
     * Construct a SimpleOCSPServer using specific network parameters,
     * keystore, password, and alias.
     *
     * @param addr the address to bind the server to.  A value of {@code null}
     * means the server will bind to all interfaces.
     * @param port the port to listen on.  A value of {@code 0} will mean that
     * the server will randomly pick an open ephemeral port to bind to.
     * @param ks the keystore to be used
     * @param password the password to access key material in the keystore
     * @param issuerAlias the alias of the issuer certificate
     * @param signerAlias the alias of the signer certificate and key.  A
     * value of {@code null} means that the {@code issuerAlias} will be used
     * to look up the signer key.
     *
     * @throws GeneralSecurityException if there are problems accessing the
     * keystore or finding objects within the keystore.
     * @throws IOException if a {@code ResponderId} cannot be generated from
     * the signer certificate.
     */
    public SimpleOCSPServer(InetAddress addr, int port, KeyStore ks,
            String password, String issuerAlias, String signerAlias)
            throws GeneralSecurityException, IOException {
        Objects.requireNonNull(ks, "Null keystore provided");
        Objects.requireNonNull(issuerAlias, "Null issuerName provided");

        utcDateFmt.setTimeZone(TimeZone.getTimeZone("GMT"));

        keystore = ks;
        issuerCert = (X509Certificate)ks.getCertificate(issuerAlias);
        if (issuerCert == null) {
            throw new IllegalArgumentException("Certificate for alias " +
                    issuerAlias + " not found");
        }

        if (signerAlias != null) {
            signerCert = (X509Certificate)ks.getCertificate(signerAlias);
            if (signerCert == null) {
                throw new IllegalArgumentException("Certificate for alias " +
                    signerAlias + " not found");
            }
            signerKey = (PrivateKey)ks.getKey(signerAlias,
                    password.toCharArray());
            if (signerKey == null) {
                throw new IllegalArgumentException("PrivateKey for alias " +
                    signerAlias + " not found");
            }
        } else {
            signerCert = issuerCert;
            signerKey = (PrivateKey)ks.getKey(issuerAlias,
                    password.toCharArray());
            if (signerKey == null) {
                throw new IllegalArgumentException("PrivateKey for alias " +
                    issuerAlias + " not found");
            }
        }

        sigAlgId = AlgorithmId.get("Sha256withRSA");
        respId = new ResponderId(signerCert.getSubjectX500Principal());
        listenAddress = addr;
        listenPort = port;
    }

    /**
     * Start the server.  The server will bind to the specified network
     * address and begin listening for incoming connections.
     *
     * @throws IOException if any number of things go wonky.
     */
    public synchronized void start() throws IOException {
        // You cannot start the server twice.
        if (started) {
            log("Server has already been started");
            return;
        } else {
            started = true;
        }

        // Create and start the thread pool
        threadPool = Executors.newFixedThreadPool(32, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            }
        });

        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket sSock = new ServerSocket()) {
                    servSocket = sSock;
                    servSocket.setReuseAddress(true);
                    servSocket.setSoTimeout(500);
                    servSocket.bind(new InetSocketAddress(listenAddress,
                            listenPort), 128);
                    log("Listening on " + servSocket.getLocalSocketAddress());

                    // Singal ready
                    serverReady = true;

                    // Update the listenPort with the new port number.  If
                    // the server is restarted, it will bind to the same
                    // port rather than picking a new one.
                    listenPort = servSocket.getLocalPort();

                    // Main dispatch loop
                    while (!receivedShutdown) {
                        try {
                            Socket newConnection = servSocket.accept();
                            if (!acceptConnections) {
                                try {
                                    log("Reject connection");
                                    newConnection.close();
                                } catch (IOException e) {
                                    // ignore
                                }
                                continue;
                            }
                            threadPool.submit(new OcspHandler(newConnection));
                        } catch (SocketTimeoutException timeout) {
                            // Nothing to do here.  If receivedShutdown
                            // has changed to true then the loop will
                            // exit on its own.
                        } catch (IOException ioe) {
                            // Something bad happened, log and force a shutdown
                            log("Unexpected Exception: " + ioe);
                            stop();
                        }
                    }

                    log("Shutting down...");
                    threadPool.shutdown();
                } catch (IOException ioe) {
                    err(ioe);
                } finally {
                    // Reset state variables so the server can be restarted
                    receivedShutdown = false;
                    started = false;
                    serverReady = false;
                }
            }
        });
    }

    /**
     * Make the OCSP server reject incoming connections.
     */
    public synchronized void rejectConnections() {
        log("Reject OCSP connections");
        acceptConnections = false;
    }

    /**
     * Make the OCSP server accept incoming connections.
     */
    public synchronized void acceptConnections() {
        log("Accept OCSP connections");
        acceptConnections = true;
    }


    /**
     * Stop the OCSP server.
     */
    public synchronized void stop() {
        if (started) {
            receivedShutdown = true;
            log("Received shutdown notification");
        }
    }

    /**
     * Print {@code SimpleOCSPServer} operating parameters.
     *
     * @return the {@code SimpleOCSPServer} operating parameters in
     * {@code String} form.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OCSP Server:\n");
        sb.append("----------------------------------------------\n");
        sb.append("issuer: ").append(issuerCert.getSubjectX500Principal()).
                append("\n");
        sb.append("signer: ").append(signerCert.getSubjectX500Principal()).
                append("\n");
        sb.append("ResponderId: ").append(respId).append("\n");
        sb.append("----------------------------------------------");

        return sb.toString();
    }

    /**
     * Helpful debug routine to hex dump byte arrays.
     *
     * @param data the array of bytes to dump to stdout.
     *
     * @return the hexdump of the byte array
     */
    private static String dumpHexBytes(byte[] data) {
        return dumpHexBytes(data, 16, "\n", " ");
    }

    /**
     *
     * @param data the array of bytes to dump to stdout.
     * @param itemsPerLine the number of bytes to display per line
     * if the {@code lineDelim} character is blank then all bytes will be
     * printed on a single line.
     * @param lineDelim the delimiter between lines
     * @param itemDelim the delimiter between bytes
     *
     * @return The hexdump of the byte array
     */
    private static String dumpHexBytes(byte[] data, int itemsPerLine,
            String lineDelim, String itemDelim) {
        StringBuilder sb = new StringBuilder();
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                if (i % itemsPerLine == 0 && i != 0) {
                    sb.append(lineDelim);
                }
                sb.append(String.format("%02X", data[i])).append(itemDelim);
            }
        }

        return sb.toString();
    }

    /**
     * Enable or disable the logging feature.
     *
     * @param enable {@code true} to enable logging, {@code false} to
     * disable it.  The setting must be activated before the server calls
     * its start method.  Any calls after that have no effect.
     */
    public void enableLog(boolean enable) {
        if (!started) {
            logEnabled = enable;
        }
    }

    /**
     * Sets the nextUpdate interval.  Intervals will be calculated relative
     * to the server startup time.  When first set, the nextUpdate date is
     * calculated based on the current time plus the interval.  After that,
     * calls to getNextUpdate() will return this date if it is still
     * later than current time.  If not, the Date will be updated to the
     * next interval that is later than current time.  This value must be set
     * before the server has had its start method called.  Calls made after
     * the server has been started have no effect.
     *
     * @param interval the recurring time interval in seconds used to
     * calculate nextUpdate times.   A value less than or equal to 0 will
     * disable the nextUpdate feature.
     */
    public synchronized void setNextUpdateInterval(long interval) {
        if (!started) {
            if (interval <= 0) {
                nextUpdateInterval = -1;
                nextUpdate = null;
                log("nexUpdate support has been disabled");
            } else {
                nextUpdateInterval = interval * 1000;
                nextUpdate = new Date(System.currentTimeMillis() +
                        nextUpdateInterval);
                log("nextUpdate set to " + nextUpdate);
            }
        }
    }

    /**
     * Return the nextUpdate {@code Date} object for this server.  If the
     * nextUpdate date has already passed, set a new nextUpdate based on
     * the nextUpdate interval and return that date.
     *
     * @return a {@code Date} object set to the nextUpdate field for OCSP
     * responses.
     */
    private synchronized Date getNextUpdate() {
        if (nextUpdate != null && nextUpdate.before(new Date())) {
            long nuEpochTime = nextUpdate.getTime();
            long currentTime = System.currentTimeMillis();

            // Keep adding nextUpdate intervals until you reach a date
            // that is later than current time.
            while (currentTime >= nuEpochTime) {
                nuEpochTime += nextUpdateInterval;
            }

            // Set the nextUpdate for future threads
            nextUpdate = new Date(nuEpochTime);
            log("nextUpdate updated to new value: " + nextUpdate);
        }
        return nextUpdate;
    }

    /**
     * Add entries into the responder's status database.
     *
     * @param newEntries a map of {@code CertStatusInfo} objects, keyed on
     * their serial number (as a {@code BigInteger}).  All serial numbers
     * are assumed to have come from this responder's issuer certificate.
     *
     * @throws IOException if a CertId cannot be generated.
     */
    public void updateStatusDb(Map<BigInteger, CertStatusInfo> newEntries)
            throws IOException {
         if (newEntries != null) {
            for (BigInteger serial : newEntries.keySet()) {
                CertStatusInfo info = newEntries.get(serial);
                if (info != null) {
                    CertId cid = new CertId(issuerCert,
                            new SerialNumber(serial));
                    statusDb.put(cid, info);
                    log("Added entry for serial " + serial + "(" +
                            info.getType() + ")");
                }
            }
        }
    }

    /**
     * Check the status database for revocation information one one or more
     * certificates.
     *
     * @param reqList the list of {@code LocalSingleRequest} objects taken
     * from the incoming OCSP request.
     *
     * @return a {@code Map} of {@code CertStatusInfo} objects keyed by their
     * {@code CertId} values, for each single request passed in.  Those
     * CertIds not found in the statusDb will have returned List members with
     * a status of UNKNOWN.
     */
    private Map<CertId, CertStatusInfo> checkStatusDb(
            List<LocalOcspRequest.LocalSingleRequest> reqList) {
        // TODO figure out what, if anything to do with request extensions
        Map<CertId, CertStatusInfo> returnMap = new HashMap<>();

        for (LocalOcspRequest.LocalSingleRequest req : reqList) {
            CertId cid = req.getCertId();
            CertStatusInfo info = statusDb.get(cid);
            if (info != null) {
                log("Status for SN " + cid.getSerialNumber() + ": " +
                        info.getType());
                returnMap.put(cid, info);
            } else {
                log("Status for SN " + cid.getSerialNumber() +
                        " not found, using CERT_STATUS_UNKNOWN");
                returnMap.put(cid,
                        new CertStatusInfo(CertStatus.CERT_STATUS_UNKNOWN));
            }
        }

        return Collections.unmodifiableMap(returnMap);
    }

    /**
     * Set the digital signature algorithm used to sign OCSP responses.
     *
     * @param algName The algorithm name
     *
     * @throws NoSuchAlgorithmException if the algorithm name is invalid.
     */
    public void setSignatureAlgorithm(String algName)
            throws NoSuchAlgorithmException {
        if (!started) {
            sigAlgId = AlgorithmId.get(algName);
        }
    }

    /**
     * Get the port the OCSP server is running on.
     *
     * @return the port that the OCSP server is running on, or -1 if the
     * server has not yet been bound to a port.
     */
    public int getPort() {
        if (serverReady) {
            InetSocketAddress inetSock =
                    (InetSocketAddress)servSocket.getLocalSocketAddress();
            return inetSock.getPort();
        } else {
            return -1;
        }
    }

    /**
     * Use to check if OCSP server is ready to accept connection.
     *
     * @return true if server ready, false otherwise
     */
    public boolean isServerReady() {
        return serverReady;
    }

    /**
     * Set a delay between the reception of the request and production of
     * the response.
     *
     * @param delayMillis the number of milliseconds to wait before acting
     * on the incoming request.
     */
    public void setDelay(long delayMillis) {
        delayMsec = delayMillis > 0 ? delayMillis : 0;
        if (delayMsec > 0) {
            log("OCSP latency set to " + delayMsec + " milliseconds.");
        } else {
            log("OCSP latency disabled");
        }
    }

    /**
     * Log a message to stdout.
     *
     * @param message the message to log
     */
    private synchronized void log(String message) {
        if (logEnabled || debug != null) {
            System.out.println("[" + Thread.currentThread().getName() + "]: " +
                    message);
        }
    }

    /**
     * Log an error message on the stderr stream.
     *
     * @param message the message to log
     */
    private static synchronized void err(String message) {
        System.out.println("[" + Thread.currentThread().getName() + "]: " +
                message);
    }

    /**
     * Log exception information on the stderr stream.
     *
     * @param exc the exception to dump information about
     */
    private static synchronized void err(Throwable exc) {
        System.out.print("[" + Thread.currentThread().getName() +
                "]: Exception: ");
        exc.printStackTrace(System.out);
    }

    /**
     * The {@code CertStatusInfo} class defines an object used to return
     * information from the internal status database.  The data in this
     * object may be used to construct OCSP responses.
     */
    public static class CertStatusInfo {
        private CertStatus certStatusType;
        private CRLReason reason;
        private Date revocationTime;

        /**
         * Create a Certificate status object by providing the status only.
         * If the status is {@code REVOKED} then current time is assumed
         * for the revocation time.
         *
         * @param statType the status for this entry.
         */
        public CertStatusInfo(CertStatus statType) {
            this(statType, null, null);
        }

        /**
         * Create a CertStatusInfo providing both type and revocation date
         * (if applicable).
         *
         * @param statType the status for this entry.
         * @param revDate if applicable, the date that revocation took place.
         * A value of {@code null} indicates that current time should be used.
         * If the value of {@code statType} is not {@code CERT_STATUS_REVOKED},
         * then the {@code revDate} parameter is ignored.
         */
        public CertStatusInfo(CertStatus statType, Date revDate) {
            this(statType, revDate, null);
        }

        /**
         * Create a CertStatusInfo providing type, revocation date
         * (if applicable) and revocation reason.
         *
         * @param statType the status for this entry.
         * @param revDate if applicable, the date that revocation took place.
         * A value of {@code null} indicates that current time should be used.
         * If the value of {@code statType} is not {@code CERT_STATUS_REVOKED},
         * then the {@code revDate} parameter is ignored.
         * @param revReason the reason the certificate was revoked.  A value of
         * {@code null} means that no reason was provided.
         */
        public CertStatusInfo(CertStatus statType, Date revDate,
                CRLReason revReason) {
            Objects.requireNonNull(statType, "Cert Status must be non-null");
            certStatusType = statType;
            switch (statType) {
                case CERT_STATUS_GOOD:
                case CERT_STATUS_UNKNOWN:
                    revocationTime = null;
                    break;
                case CERT_STATUS_REVOKED:
                    revocationTime = revDate != null ? (Date)revDate.clone() :
                            new Date();
                    break;
                default:
                    throw new IllegalArgumentException("Unknown status type: " +
                            statType);
            }
        }

        /**
         * Get the cert status type
         *
         * @return the status applied to this object (e.g.
         * {@code CERT_STATUS_GOOD}, {@code CERT_STATUS_UNKNOWN}, etc.)
         */
        public CertStatus getType() {
            return certStatusType;
        }

        /**
         * Get the revocation time (if applicable).
         *
         * @return the revocation time as a {@code Date} object, or
         * {@code null} if not applicable (i.e. if the certificate hasn't been
         * revoked).
         */
        public Date getRevocationTime() {
            return (revocationTime != null ? (Date)revocationTime.clone() :
                    null);
        }

        /**
         * Get the revocation reason.
         *
         * @return the revocation reason, or {@code null} if one was not
         * provided.
         */
        public CRLReason getRevocationReason() {
            return reason;
        }
    }

    /**
     * Runnable task that handles incoming OCSP Requests and returns
     * responses.
     */
    private class OcspHandler implements Runnable {
        private final Socket sock;
        InetSocketAddress peerSockAddr;

        /**
         * Construct an {@code OcspHandler}.
         *
         * @param incomingSocket the socket the server created on accept()
         */
        private OcspHandler(Socket incomingSocket) {
            sock = incomingSocket;
        }

        /**
         * Run the OCSP Request parser and construct a response to be sent
         * back to the client.
         */
        @Override
        public void run() {
            // If we have implemented a delay to simulate network latency
            // wait out the delay here before any other processing.
            try {
                if (delayMsec > 0) {
                    Thread.sleep(delayMsec);
                }
            } catch (InterruptedException ie) {
                // Just log the interrupted sleep
                log("Delay of " + delayMsec + " milliseconds was interrupted");
            }

            try (Socket ocspSocket = sock;
                    InputStream in = ocspSocket.getInputStream();
                    OutputStream out = ocspSocket.getOutputStream()) {
                peerSockAddr =
                        (InetSocketAddress)ocspSocket.getRemoteSocketAddress();
                String[] headerTokens = readLine(in).split(" ");
                LocalOcspRequest ocspReq = null;
                LocalOcspResponse ocspResp = null;
                ResponseStatus respStat = ResponseStatus.INTERNAL_ERROR;
                try {
                    if (headerTokens[0] != null) {
                        log("Received incoming HTTP " + headerTokens[0] +
                                " from " + peerSockAddr);
                        switch (headerTokens[0]) {
                            case "POST":
                                ocspReq = parseHttpOcspPost(in);
                                break;
                            case "GET":
                                ocspReq = parseHttpOcspGet(headerTokens);
                                break;
                            default:
                                respStat = ResponseStatus.MALFORMED_REQUEST;
                                throw new IOException("Not a GET or POST");
                        }
                    } else {
                        respStat = ResponseStatus.MALFORMED_REQUEST;
                        throw new IOException("Unable to get HTTP method");
                    }

                    if (ocspReq != null) {
                        log(ocspReq.toString());
                        // Get responses for all CertIds in the request
                        Map<CertId, CertStatusInfo> statusMap =
                                checkStatusDb(ocspReq.getRequests());
                        if (statusMap.isEmpty()) {
                            respStat = ResponseStatus.UNAUTHORIZED;
                        } else {
                            ocspResp = new LocalOcspResponse(
                                    ResponseStatus.SUCCESSFUL, statusMap,
                                    ocspReq.getExtensions());
                        }
                    } else {
                        respStat = ResponseStatus.MALFORMED_REQUEST;
                        throw new IOException("Found null request");
                    }
                } catch (IOException | RuntimeException exc) {
                    err(exc);
                }
                if (ocspResp == null) {
                    ocspResp = new LocalOcspResponse(respStat);
                }
                sendResponse(out, ocspResp);
            } catch (IOException | CertificateException exc) {
                err(exc);
            }
        }

        /**
         * Send an OCSP response on an {@code OutputStream}.
         *
         * @param out the {@code OutputStream} on which to send the response.
         * @param resp the OCSP response to send.
         *
         * @throws IOException if an encoding error occurs.
         */
        public void sendResponse(OutputStream out, LocalOcspResponse resp)
                throws IOException {
            StringBuilder sb = new StringBuilder();

            byte[] respBytes;
            try {
                respBytes = resp.getBytes();
            } catch (RuntimeException re) {
                err(re);
                return;
            }

            sb.append("HTTP/1.0 200 OK\r\n");
            sb.append("Content-Type: application/ocsp-response\r\n");
            sb.append("Content-Length: ").append(respBytes.length);
            sb.append("\r\n\r\n");

            out.write(sb.toString().getBytes("UTF-8"));
            out.write(respBytes);
            log(resp.toString());
        }

        /**
         * Parse the incoming HTTP POST of an OCSP Request.
         *
         * @param inStream the input stream from the socket bound to this
         * {@code OcspHandler}.
         *
         * @return the OCSP Request as a {@code LocalOcspRequest}
         *
         * @throws IOException if there are network related issues or problems
         * occur during parsing of the OCSP request.
         * @throws CertificateException if one or more of the certificates in
         * the OCSP request cannot be read/parsed.
         */
        private LocalOcspRequest parseHttpOcspPost(InputStream inStream)
                throws IOException, CertificateException {
            boolean endOfHeader = false;
            boolean properContentType = false;
            int length = -1;

            while (!endOfHeader) {
                String[] lineTokens = readLine(inStream).split(" ");
                if (lineTokens[0].isEmpty()) {
                    endOfHeader = true;
                } else if (lineTokens[0].equalsIgnoreCase("Content-Type:")) {
                    if (lineTokens[1] == null ||
                            !lineTokens[1].equals(
                                    "application/ocsp-request")) {
                        log("Unknown Content-Type: " +
                                (lineTokens[1] != null ?
                                        lineTokens[1] : "<NULL>"));
                        return null;
                    } else {
                        properContentType = true;
                        log("Content-Type = " + lineTokens[1]);
                    }
                } else if (lineTokens[0].equalsIgnoreCase("Content-Length:")) {
                    if (lineTokens[1] != null) {
                        length = Integer.parseInt(lineTokens[1]);
                        log("Content-Length = " + length);
                    }
                }
            }

            // Okay, make sure we got what we needed from the header, then
            // read the remaining OCSP Request bytes
            if (properContentType && length >= 0) {
                byte[] ocspBytes = new byte[length];
                inStream.read(ocspBytes);
                return new LocalOcspRequest(ocspBytes);
            } else {
                return null;
            }
        }

        /**
         * Parse the incoming HTTP GET of an OCSP Request.
         *
         * @param headerTokens the individual String tokens from the first
         * line of the HTTP GET.
         *
         * @return the OCSP Request as a {@code LocalOcspRequest}
         *
         * @throws IOException if there are network related issues or problems
         * occur during parsing of the OCSP request.
         * @throws CertificateException if one or more of the certificates in
         * the OCSP request cannot be read/parsed.
         */
        private LocalOcspRequest parseHttpOcspGet(String[] headerTokens)
                throws IOException, CertificateException {
            // We have already established headerTokens[0] to be "GET".
            // We should have the URL-encoded base64 representation of the
            // OCSP request in headerTokens[1].  We need to strip any leading
            // "/" off before decoding.
            return new LocalOcspRequest(Base64.getMimeDecoder().decode(
                    URLDecoder.decode(headerTokens[1].replaceAll("/", ""),
                            "UTF-8")));
        }

        /**
         * Read a line of text that is CRLF-delimited.
         *
         * @param is the {@code InputStream} tied to the socket
         * for this {@code OcspHandler}
         *
         * @return a {@code String} consisting of the line of text
         * read from the stream with the CRLF stripped.
         *
         * @throws IOException if any I/O error occurs.
         */
        private String readLine(InputStream is) throws IOException {
            PushbackInputStream pbis = new PushbackInputStream(is);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            boolean done = false;
            while (!done) {
                byte b = (byte)pbis.read();
                if (b == '\r') {
                    byte bNext = (byte)pbis.read();
                    if (bNext == '\n' || bNext == -1) {
                        done = true;
                    } else {
                        pbis.unread(bNext);
                        bos.write(b);
                    }
                } else if (b == -1) {
                    done = true;
                } else {
                    bos.write(b);
                }
            }

            return new String(bos.toByteArray(), "UTF-8");
        }
    }


    /**
     * Simple nested class to handle OCSP requests without making
     * changes to sun.security.provider.certpath.OCSPRequest
     */
    public class LocalOcspRequest {

        private byte[] nonce;
        private byte[] signature = null;
        private AlgorithmId algId = null;
        private int version = 0;
        private GeneralName requestorName = null;
        private Map<String, Extension> extensions = Collections.emptyMap();
        private final List<LocalSingleRequest> requestList = new ArrayList<>();
        private final List<X509Certificate> certificates = new ArrayList<>();

        /**
         * Construct a {@code LocalOcspRequest} from its DER encoding.
         *
         * @param requestBytes the DER-encoded bytes
         *
         * @throws IOException if decoding errors occur
         * @throws CertificateException if certificates are found in the
         * OCSP request and they do not parse correctly.
         */
        private LocalOcspRequest(byte[] requestBytes) throws IOException,
                CertificateException {
            Objects.requireNonNull(requestBytes, "Received null input");

            DerInputStream dis = new DerInputStream(requestBytes);

            // Parse the top-level structure, it should have no more than
            // two elements.
            DerValue[] topStructs = dis.getSequence(2);
            for (DerValue dv : topStructs) {
                if (dv.tag == DerValue.tag_Sequence) {
                    parseTbsRequest(dv);
                } else if (dv.isContextSpecific((byte)0)) {
                    parseSignature(dv);
                } else {
                    throw new IOException("Unknown tag at top level: " +
                            dv.tag);
                }
            }
        }

        /**
         * Parse the signature block from an OCSP request
         *
         * @param sigSequence a {@code DerValue} containing the signature
         * block at the outer sequence datum.
         *
         * @throws IOException if any non-certificate-based parsing errors occur
         * @throws CertificateException if certificates are found in the
         * OCSP request and they do not parse correctly.
         */
        private void parseSignature(DerValue sigSequence)
                throws IOException, CertificateException {
            DerValue[] sigItems = sigSequence.data.getSequence(3);
            if (sigItems.length != 3) {
                throw new IOException("Invalid number of signature items: " +
                        "expected 3, got " + sigItems.length);
            }

            algId = AlgorithmId.parse(sigItems[0]);
            signature = sigItems[1].getBitString();

            if (sigItems[2].isContextSpecific((byte)0)) {
                DerValue[] certDerItems = sigItems[2].data.getSequence(4);
                int i = 0;
                for (DerValue dv : certDerItems) {
                    X509Certificate xc = new X509CertImpl(dv);
                    certificates.add(xc);
                }
            } else {
                throw new IOException("Invalid tag in signature block: " +
                    sigItems[2].tag);
            }
        }

        /**
         * Parse the to-be-signed request data
         *
         * @param tbsReqSeq a {@code DerValue} object containing the to-be-
         * signed OCSP request at the outermost SEQUENCE tag.
         * @throws IOException if any parsing errors occur
         */
        private void parseTbsRequest(DerValue tbsReqSeq) throws IOException {
            while (tbsReqSeq.data.available() > 0) {
                DerValue dv = tbsReqSeq.data.getDerValue();
                if (dv.isContextSpecific((byte)0)) {
                    // The version was explicitly called out
                    version = dv.data.getInteger();
                } else if (dv.isContextSpecific((byte)1)) {
                    // A GeneralName was provided
                    requestorName = new GeneralName(dv.data.getDerValue());
                } else if (dv.isContextSpecific((byte)2)) {
                    // Parse the extensions
                    DerValue[] extItems = dv.data.getSequence(2);
                    extensions = parseExtensions(extItems);
                } else if (dv.tag == DerValue.tag_Sequence) {
                    while (dv.data.available() > 0) {
                        requestList.add(new LocalSingleRequest(dv.data));
                    }
                }
            }
        }

        /**
         * Parse a SEQUENCE of extensions.  This routine is used both
         * at the overall request level and down at the singleRequest layer.
         *
         * @param extDerItems an array of {@code DerValue} items, each one
         * consisting of a DER-encoded extension.
         *
         * @return a {@code Map} of zero or more extensions,
         * keyed by its object identifier in {@code String} form.
         *
         * @throws IOException if any parsing errors occur.
         */
        private Map<String, Extension> parseExtensions(DerValue[] extDerItems)
                throws IOException {
            Map<String, Extension> extMap = new HashMap<>();

            if (extDerItems != null && extDerItems.length != 0) {
                for (DerValue extDerVal : extDerItems) {
                    sun.security.x509.Extension ext =
                            new sun.security.x509.Extension(extDerVal);
                    extMap.put(ext.getId(), ext);
                }
            }

            return extMap;
        }

        /**
         * Return the list of single request objects in this OCSP request.
         *
         * @return an unmodifiable {@code List} of zero or more requests.
         */
        private List<LocalSingleRequest> getRequests() {
            return Collections.unmodifiableList(requestList);
        }

        /**
         * Return the list of X.509 Certificates in this OCSP request.
         *
         * @return an unmodifiable {@code List} of zero or more
         * {@cpde X509Certificate} objects.
         */
        private List<X509Certificate> getCertificates() {
            return Collections.unmodifiableList(certificates);
        }

        /**
         * Return the map of OCSP request extensions.
         *
         * @return an unmodifiable {@code Map} of zero or more
         * {@code Extension} objects, keyed by their object identifiers
         * in {@code String} form.
         */
        private Map<String, Extension> getExtensions() {
            return Collections.unmodifiableMap(extensions);
        }

        /**
         * Display the {@code LocalOcspRequest} in human readable form.
         *
         * @return a {@code String} representation of the
         * {@code LocalOcspRequest}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("OCSP Request: Version %d (0x%X)",
                    version + 1, version)).append("\n");
            if (requestorName != null) {
                sb.append("Requestor Name: ").append(requestorName).
                        append("\n");
            }

            int requestCtr = 0;
            for (LocalSingleRequest lsr : requestList) {
                sb.append("Request [").append(requestCtr++).append("]\n");
                sb.append(lsr).append("\n");
            }
            if (!extensions.isEmpty()) {
                sb.append("Extensions (").append(extensions.size()).
                        append(")\n");
                for (Extension ext : extensions.values()) {
                    sb.append("\t").append(ext).append("\n");
                }
            }
            if (signature != null) {
                sb.append("Signature: ").append(algId).append("\n");
                sb.append(dumpHexBytes(signature)).append("\n");
                int certCtr = 0;
                for (X509Certificate cert : certificates) {
                    sb.append("Certificate [").append(certCtr++).append("]").
                            append("\n");
                    sb.append("\tSubject: ");
                    sb.append(cert.getSubjectX500Principal()).append("\n");
                    sb.append("\tIssuer: ");
                    sb.append(cert.getIssuerX500Principal()).append("\n");
                    sb.append("\tSerial: ").append(cert.getSerialNumber());
                }
            }

            return sb.toString();
        }

        /**
         * Inner class designed to handle the decoding/representation of
         * single requests within a {@code LocalOcspRequest} object.
         */
        public class LocalSingleRequest {
            private final CertId cid;
            private Map<String, Extension> extensions = Collections.emptyMap();

            private LocalSingleRequest(DerInputStream dis)
                    throws IOException {
                DerValue[] srItems = dis.getSequence(2);

                // There should be 1, possibly 2 DerValue items
                if (srItems.length == 1 || srItems.length == 2) {
                    // The first parsable item should be the mandatory CertId
                    cid = new CertId(srItems[0].data);
                    if (srItems.length == 2) {
                        if (srItems[1].isContextSpecific((byte)0)) {
                            DerValue[] extDerItems = srItems[1].data.getSequence(2);
                            extensions = parseExtensions(extDerItems);
                        } else {
                            throw new IOException("Illegal tag in Request " +
                                    "extensions: " + srItems[1].tag);
                        }
                    }
                } else {
                    throw new IOException("Invalid number of items in " +
                            "Request (" + srItems.length + ")");
                }
            }

            /**
             * Get the {@code CertId} for this single request.
             *
             * @return the {@code CertId} for this single request.
             */
            private CertId getCertId() {
                return cid;
            }

            /**
             * Return the map of single request extensions.
             *
             * @return an unmodifiable {@code Map} of zero or more
             * {@code Extension} objects, keyed by their object identifiers
             * in {@code String} form.
             */
            private Map<String, Extension> getExtensions() {
                return Collections.unmodifiableMap(extensions);
            }

            /**
             * Display the {@code LocalSingleRequest} in human readable form.
             *
             * @return a {@code String} representation of the
             * {@code LocalSingleRequest}
             */
            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("CertId, Algorithm = ");
                sb.append(cid.getHashAlgorithm()).append("\n");
                sb.append("\tIssuer Name Hash: ");
                sb.append(dumpHexBytes(cid.getIssuerNameHash(), 256, "", ""));
                sb.append("\n");
                sb.append("\tIssuer Key Hash: ");
                sb.append(dumpHexBytes(cid.getIssuerKeyHash(), 256, "", ""));
                sb.append("\n");
                sb.append("\tSerial Number: ").append(cid.getSerialNumber());
                if (!extensions.isEmpty()) {
                    sb.append("Extensions (").append(extensions.size()).
                            append(")\n");
                    for (Extension ext : extensions.values()) {
                        sb.append("\t").append(ext).append("\n");
                    }
                }

                return sb.toString();
            }
        }
    }

    /**
     * Simple nested class to handle OCSP requests without making
     * changes to sun.security.provider.certpath.OCSPResponse
     */
    public class LocalOcspResponse {
        private final int version = 0;
        private final OCSPResponse.ResponseStatus responseStatus;
        private final Map<CertId, CertStatusInfo> respItemMap;
        private final Date producedAtDate;
        private final List<LocalSingleResponse> singleResponseList =
                new ArrayList<>();
        private final Map<String, Extension> responseExtensions;
        private byte[] signature;
        private final List<X509Certificate> certificates;
        private final byte[] encodedResponse;

        /**
         * Constructor for the generation of non-successful responses
         *
         * @param respStat the OCSP response status.
         *
         * @throws IOException if an error happens during encoding
         * @throws NullPointerException if {@code respStat} is {@code null}
         * or {@code respStat} is successful.
         */
        public LocalOcspResponse(OCSPResponse.ResponseStatus respStat)
                throws IOException {
            this(respStat, null, null);
        }

        /**
         * Construct a response from a list of certificate
         * status objects and extensions.
         *
         * @param respStat the status of the entire response
         * @param itemMap a {@code Map} of {@code CertId} objects and their
         * respective revocation statuses from the server's response DB.
         * @param reqExtensions a {@code Map} of request extensions
         *
         * @throws IOException if an error happens during encoding
         * @throws NullPointerException if {@code respStat} is {@code null}
         * or {@code respStat} is successful, and a {@code null} {@code itemMap}
         * has been provided.
         */
        public LocalOcspResponse(OCSPResponse.ResponseStatus respStat,
                Map<CertId, CertStatusInfo> itemMap,
                Map<String, Extension> reqExtensions) throws IOException {
            responseStatus = Objects.requireNonNull(respStat,
                    "Illegal null response status");
            if (responseStatus == ResponseStatus.SUCCESSFUL) {
                respItemMap = Objects.requireNonNull(itemMap,
                        "SUCCESSFUL responses must have a response map");
                producedAtDate = new Date();

                // Turn the answerd from the response DB query into a list
                // of single responses.
                for (CertId id : itemMap.keySet()) {
                    singleResponseList.add(
                            new LocalSingleResponse(id, itemMap.get(id)));
                }

                responseExtensions = setResponseExtensions(reqExtensions);
                certificates = new ArrayList<>();
                if (signerCert != issuerCert) {
                    certificates.add(signerCert);
                }
                certificates.add(issuerCert);
            } else {
                respItemMap = null;
                producedAtDate = null;
                responseExtensions = null;
                certificates = null;
            }
            encodedResponse = this.getBytes();
        }

        /**
         * Set the response extensions based on the request extensions
         * that were received.  Right now, this is limited to the
         * OCSP nonce extension.
         *
         * @param reqExts a {@code Map} of zero or more request extensions
         *
         * @return a {@code Map} of zero or more response extensions, keyed
         * by the extension object identifier in {@code String} form.
         */
        private Map<String, Extension> setResponseExtensions(
                Map<String, Extension> reqExts) {
            Map<String, Extension> respExts = new HashMap<>();
            String ocspNonceStr = PKIXExtensions.OCSPNonce_Id.toString();

            if (reqExts != null) {
                for (String id : reqExts.keySet()) {
                    if (id.equals(ocspNonceStr)) {
                        // We found a nonce, add it into the response extensions
                        Extension ext = reqExts.get(id);
                        if (ext != null) {
                            respExts.put(id, ext);
                            log("Added OCSP Nonce to response");
                        } else {
                            log("Error: Found nonce entry, but found null " +
                                    "value.  Skipping");
                        }
                    }
                }
            }

            return respExts;
        }

        /**
         * Get the DER-encoded response bytes for this response
         *
         * @return a byte array containing the DER-encoded bytes for
         * the response
         *
         * @throws IOException if any encoding errors occur
         */
        private byte[] getBytes() throws IOException {
            DerOutputStream outerSeq = new DerOutputStream();
            DerOutputStream responseStream = new DerOutputStream();
            responseStream.putEnumerated(responseStatus.ordinal());
            if (responseStatus == ResponseStatus.SUCCESSFUL &&
                    respItemMap != null) {
                encodeResponseBytes(responseStream);
            }

            // Commit the outermost sequence bytes
            outerSeq.write(DerValue.tag_Sequence, responseStream);
            return outerSeq.toByteArray();
        }

        private void encodeResponseBytes(DerOutputStream responseStream)
                throws IOException {
            DerOutputStream explicitZero = new DerOutputStream();
            DerOutputStream respItemStream = new DerOutputStream();

            respItemStream.putOID(OCSP_BASIC_RESPONSE_OID);

            byte[] basicOcspBytes = encodeBasicOcspResponse();
            respItemStream.putOctetString(basicOcspBytes);
            explicitZero.write(DerValue.tag_Sequence, respItemStream);
            responseStream.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                    true, (byte)0), explicitZero);
        }

        private byte[] encodeBasicOcspResponse() throws IOException {
            DerOutputStream outerSeq = new DerOutputStream();
            DerOutputStream basicORItemStream = new DerOutputStream();

            // Encode the tbsResponse
            byte[] tbsResponseBytes = encodeTbsResponse();
            basicORItemStream.write(tbsResponseBytes);

            try {
                sigAlgId.derEncode(basicORItemStream);

                // Create the signature
                Signature sig = Signature.getInstance(sigAlgId.getName());
                sig.initSign(signerKey);
                sig.update(tbsResponseBytes);
                signature = sig.sign();
                basicORItemStream.putBitString(signature);
            } catch (GeneralSecurityException exc) {
                err(exc);
                throw new IOException(exc);
            }

            // Add certificates
            try {
                DerOutputStream certStream = new DerOutputStream();
                ArrayList<DerValue> certList = new ArrayList<>();
                if (signerCert != issuerCert) {
                    certList.add(new DerValue(signerCert.getEncoded()));
                }
                certList.add(new DerValue(issuerCert.getEncoded()));
                DerValue[] dvals = new DerValue[certList.size()];
                certStream.putSequence(certList.toArray(dvals));
                basicORItemStream.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                        true, (byte)0), certStream);
            } catch (CertificateEncodingException cex) {
                err(cex);
                throw new IOException(cex);
            }

            // Commit the outermost sequence bytes
            outerSeq.write(DerValue.tag_Sequence, basicORItemStream);
            return outerSeq.toByteArray();
        }

        private byte[] encodeTbsResponse() throws IOException {
            DerOutputStream outerSeq = new DerOutputStream();
            DerOutputStream tbsStream = new DerOutputStream();

            // Note: We're not going explicitly assert the version
            tbsStream.write(respId.getEncoded());
            tbsStream.putGeneralizedTime(producedAtDate);

            // Sequence of responses
            encodeSingleResponses(tbsStream);

            // TODO: add response extension support
            encodeExtensions(tbsStream);

            outerSeq.write(DerValue.tag_Sequence, tbsStream);
            return outerSeq.toByteArray();
        }

        private void encodeSingleResponses(DerOutputStream tbsStream)
                throws IOException {
            DerValue[] srDerVals = new DerValue[singleResponseList.size()];
            int srDvCtr = 0;

            for (LocalSingleResponse lsr : singleResponseList) {
                srDerVals[srDvCtr++] = new DerValue(lsr.getBytes());
            }

            tbsStream.putSequence(srDerVals);
        }

        private void encodeExtensions(DerOutputStream tbsStream)
                throws IOException {
            DerOutputStream extSequence = new DerOutputStream();
            DerOutputStream extItems = new DerOutputStream();

            for (Extension ext : responseExtensions.values()) {
                ext.encode(extItems);
            }
            extSequence.write(DerValue.tag_Sequence, extItems);
            tbsStream.write(DerValue.createTag(DerValue.TAG_CONTEXT, true,
                    (byte)1), extSequence);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("OCSP Response: ").append(responseStatus).append("\n");
            if (responseStatus == ResponseStatus.SUCCESSFUL) {
                sb.append("Response Type: ").
                        append(OCSP_BASIC_RESPONSE_OID.toString()).append("\n");
                sb.append(String.format("Version: %d (0x%X)", version + 1,
                        version)).append("\n");
                sb.append("Responder Id: ").append(respId.toString()).
                        append("\n");
                sb.append("Produced At: ").
                        append(utcDateFmt.format(producedAtDate)).append("\n");

                int srCtr = 0;
                for (LocalSingleResponse lsr : singleResponseList) {
                    sb.append("SingleResponse [").append(srCtr++).append("]\n");
                    sb.append(lsr);
                }

                if (!responseExtensions.isEmpty()) {
                    sb.append("Extensions (").append(responseExtensions.size()).
                            append(")\n");
                    for (Extension ext : responseExtensions.values()) {
                        sb.append("\t").append(ext).append("\n");
                    }
                } else {
                    sb.append("\n");
                }

                if (signature != null) {
                    sb.append("Signature: ").append(sigAlgId).append("\n");
                    sb.append(dumpHexBytes(signature)).append("\n");
                    int certCtr = 0;
                    for (X509Certificate cert : certificates) {
                        sb.append("Certificate [").append(certCtr++).append("]").
                                append("\n");
                        sb.append("\tSubject: ");
                        sb.append(cert.getSubjectX500Principal()).append("\n");
                        sb.append("\tIssuer: ");
                        sb.append(cert.getIssuerX500Principal()).append("\n");
                        sb.append("\tSerial: ").append(cert.getSerialNumber());
                        sb.append("\n");
                    }
                }
            }

            return sb.toString();
        }

        private class LocalSingleResponse {
            private final CertId certId;
            private final CertStatusInfo csInfo;
            private final Date thisUpdate;
            private final Date lsrNextUpdate;
            private final Map<String, Extension> singleExtensions;

            public LocalSingleResponse(CertId cid, CertStatusInfo info) {
                certId = Objects.requireNonNull(cid, "CertId must be non-null");
                csInfo = Objects.requireNonNull(info,
                        "CertStatusInfo must be non-null");

                // For now, we'll keep things simple and make the thisUpdate
                // field the same as the producedAt date.
                thisUpdate = producedAtDate;
                lsrNextUpdate = getNextUpdate();

                // TODO Add extensions support
                singleExtensions = Collections.emptyMap();
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("Certificate Status: ").append(csInfo.getType());
                sb.append("\n");
                if (csInfo.getType() == CertStatus.CERT_STATUS_REVOKED) {
                    sb.append("Revocation Time: ");
                    sb.append(utcDateFmt.format(csInfo.getRevocationTime()));
                    sb.append("\n");
                    if (csInfo.getRevocationReason() != null) {
                        sb.append("Revocation Reason: ");
                        sb.append(csInfo.getRevocationReason()).append("\n");
                    }
                }

                sb.append("CertId, Algorithm = ");
                sb.append(certId.getHashAlgorithm()).append("\n");
                sb.append("\tIssuer Name Hash: ");
                sb.append(dumpHexBytes(certId.getIssuerNameHash(), 256, "", ""));
                sb.append("\n");
                sb.append("\tIssuer Key Hash: ");
                sb.append(dumpHexBytes(certId.getIssuerKeyHash(), 256, "", ""));
                sb.append("\n");
                sb.append("\tSerial Number: ").append(certId.getSerialNumber());
                sb.append("\n");
                sb.append("This Update: ");
                sb.append(utcDateFmt.format(thisUpdate)).append("\n");
                if (lsrNextUpdate != null) {
                    sb.append("Next Update: ");
                    sb.append(utcDateFmt.format(lsrNextUpdate)).append("\n");
                }

                if (!singleExtensions.isEmpty()) {
                    sb.append("Extensions (").append(singleExtensions.size()).
                            append(")\n");
                    for (Extension ext : singleExtensions.values()) {
                        sb.append("\t").append(ext).append("\n");
                    }
                }

                return sb.toString();
            }

            public byte[] getBytes() throws IOException {
                byte[] nullData = { };
                DerOutputStream responseSeq = new DerOutputStream();
                DerOutputStream srStream = new DerOutputStream();

                // Encode the CertId
                certId.encode(srStream);

                // Next, encode the CertStatus field
                CertStatus csiType = csInfo.getType();
                switch (csiType) {
                    case CERT_STATUS_GOOD:
                        srStream.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                false, (byte)0), nullData);
                        break;
                    case CERT_STATUS_REVOKED:
                        DerOutputStream revInfo = new DerOutputStream();
                        revInfo.putGeneralizedTime(csInfo.getRevocationTime());
                        CRLReason revReason = csInfo.getRevocationReason();
                        if (revReason != null) {
                            byte[] revDer = new byte[3];
                            revDer[0] = DerValue.tag_Enumerated;
                            revDer[1] = 1;
                            revDer[2] = (byte)revReason.ordinal();
                            revInfo.write(DerValue.createTag(
                                    DerValue.TAG_CONTEXT, true, (byte)0),
                                    revDer);
                        }
                        srStream.write(DerValue.createTag(
                                DerValue.TAG_CONTEXT, true, (byte)1),
                                revInfo);
                        break;
                    case CERT_STATUS_UNKNOWN:
                        srStream.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                                false, (byte)2), nullData);
                        break;
                    default:
                        throw new IOException("Unknown CertStatus: " + csiType);
                }

                // Add the necessary dates
                srStream.putGeneralizedTime(thisUpdate);
                if (lsrNextUpdate != null) {
                    DerOutputStream nuStream = new DerOutputStream();
                    nuStream.putGeneralizedTime(lsrNextUpdate);
                    srStream.write(DerValue.createTag(DerValue.TAG_CONTEXT,
                            true, (byte)0), nuStream);
                }

                // TODO add singleResponse Extension support

                // Add the single response to the response output stream
                responseSeq.write(DerValue.tag_Sequence, srStream);
                return responseSeq.toByteArray();
            }
        }
    }
}
