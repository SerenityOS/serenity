/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @author Sean Mullan
 * @author Steve Hanna
 *
 */

package jdk.test.lib.security;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Static utility methods useful for testing certificate/certpath APIs.
 */
public class CertUtils {

    private CertUtils() {}

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     7b:bb:a0:55:00:9d:69:16:1e:cb:e2:ad:25:d1:32:ff:fa:52:1b:05
     * Signature Algorithm: sha256WithRSAEncryption
     * Issuer: CN = localhost
     * Validity
     *     Not Before: Aug  1 11:58:25 2019 GMT
     *     Not After : Jul 29 11:58:25 2029 GMT
     * Subject: CN = localhost
     */
    public static final String RSA_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDCTCCAfGgAwIBAgIUe7ugVQCdaRYey+KtJdEy//pSGwUwDQYJKoZIhvcNAQEL\n" +
            "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE5MDgwMTExNTgyNVoXDTI5MDcy\n" +
            "OTExNTgyNVowFDESMBAGA1UEAwwJbG9jYWxob3N0MIIBIjANBgkqhkiG9w0BAQEF\n" +
            "AAOCAQ8AMIIBCgKCAQEAxDGfn+GQEErnE1ErBaYpH8+rFgUS/nhFuaKLMNsYMtAY\n" +
            "GI7XvnwzSMeYou6tDobi0WMxlnQRSlVEmmT6OPOOC9RLnt2qdU2klXVR5DCzVTrp\n" +
            "wX5TILkP+KzePRQFrpi4z6Fx15cIVhP4OdPUd4rwAffD+nYaijQezLuKwdBKBHlt\n" +
            "GBGxn978Ppcmx/6qAfFZjhtxJXBM7LzUPkDs6jHy10FK9KkqjmmB6zXM0Rvv8nN3\n" +
            "9o55H3LnbO4XSIoRUGwSISSiHEBHbOZyBblDc0yoRAnjqxSDIj5oxessfDt5gG6C\n" +
            "LqrUyfLDo7pbmQrdBoH2NEX9yScYVE1MnlRA6LusCQIDAQABo1MwUTAdBgNVHQ4E\n" +
            "FgQUbZzwnSvM67UCB3ng5fTGcL24uqUwHwYDVR0jBBgwFoAUbZzwnSvM67UCB3ng\n" +
            "5fTGcL24uqUwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAgAtI\n" +
            "feN7JySd5HdEqA0/vvCCoBJ/Z9//3OxQyW8NnkmVW3F1eMIWII/vOdYj1WJgq1uK\n" +
            "a4GKiUVgEYTVSJxwj3mBmvd9o93Im9BSI0RkGOQOTnCLJNTksAD+2qO4F984xucS\n" +
            "/R2BChlbik7+7uPZ7qnhfDmxyMJqtUuze/JkA4IrVssbFM30j70gGjNNd/waBsR2\n" +
            "upI29x7LSCdPkXmwUuzUR5/zBHaR4pZ2nQvsfxoP384BvpM1SCNrBUGvxGzDDiGA\n" +
            "pOJwIJoTEU7gGaHF8BeEUtC1YbSDWr+dN4IM7uzL6sdVs8xPVxkeptlVU7cDIyiN\n" +
            "DPm3K0U4oj/KoFfMHg==\n" +
            "-----END CERTIFICATE-----";
    public static final String RSA_KEY =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDEMZ+f4ZAQSucT\n" +
            "USsFpikfz6sWBRL+eEW5oosw2xgy0BgYjte+fDNIx5ii7q0OhuLRYzGWdBFKVUSa\n" +
            "ZPo4844L1Eue3ap1TaSVdVHkMLNVOunBflMguQ/4rN49FAWumLjPoXHXlwhWE/g5\n" +
            "09R3ivAB98P6dhqKNB7Mu4rB0EoEeW0YEbGf3vw+lybH/qoB8VmOG3ElcEzsvNQ+\n" +
            "QOzqMfLXQUr0qSqOaYHrNczRG+/yc3f2jnkfcuds7hdIihFQbBIhJKIcQEds5nIF\n" +
            "uUNzTKhECeOrFIMiPmjF6yx8O3mAboIuqtTJ8sOjuluZCt0GgfY0Rf3JJxhUTUye\n" +
            "VEDou6wJAgMBAAECggEAFwYn0HB9IpxXr9mnrdsJGnWZg1UHHJvqutVLdmlP1q67\n" +
            "FS62POGAdzsNO5m872Z++cmlSR3H5axKB8Bv6P0UH2lDkpo65dc9yFhjSt84PHlU\n" +
            "c2Oqx71QFYPb9NkDrbC4h41Pudg8nzVqvQaR+ZFxhxmXgy4XAT8KmkYsC4CxHwMY\n" +
            "FYCHsNc8kpyXc7P5bbjpdQHMwpBP3dyo42h8cim8P2c5pKM0ipSm4vD9r8NIbvG7\n" +
            "+bzLBC0aJCfL0wY2c8qRD2k5Xl/NRKovya8v6IUCyigyJV5DZMOfRqCMDeMuiaxl\n" +
            "cvKqIPO5wxE3Wt36cEPZGO6GI6H+tzXZT0+y0+OfXQKBgQD5kR2GscpFhc+/A9Qn\n" +
            "QQxeMHjDqXUjP11Czg+/K2vKjC+RHNIzOh+4jGWNb9nlMSu22IRltRzyDOrPRytB\n" +
            "RT2o5rUGSv/oZ/lEMMyHz+xPaBfegYSCkZ3h01iL1fdAUALHtzG5c6S8JXhtWzHk\n" +
            "q/dk6iXPfTtSREBkwv7c43vXTwKBgQDJQE0ZvgTSnscA+GkM1R7tH0hqKyk/xeus\n" +
            "/xu23EraiIg4qoJ7Lk2IRvOkgotuK/SK+qoWlBr3OwBRzByxBXSdjdciZ5jbOc1g\n" +
            "TA4Qwma6R9ukYdW+W95nYmsgyOi0+7tX9oVJatBJGyq3atayUANy8Lun4kSRdurn\n" +
            "WibRxuxxJwKBgQCq62vhV9pECwTguWqPB8nEqEXNGz2SSk9A9YdrUf2c1q2tIKQF\n" +
            "WYVoCx9x4mzEvwxFSbxDO+r7C0W1d/Rz20wDZR4NlUf2j170CMfLK+eX6p6OUP3D\n" +
            "vp72jpxSCNQxQ5rj1N9FN6JXCQKVQqPFDNF+V65VkFsUWJIRcErEVTf3mQKBgAiW\n" +
            "AbQTc0k5FOxprLGrdgJPz1sYNE5QN1nOGHSYuWjRYl5oh+CRfSVPQZ3HJAOzRF+Z\n" +
            "iiAkeXIKxly3BJJY1TzTjFYfbVoNAUIWfJMieBeCcVB2DBRu/vISNNfVOnheNQPv\n" +
            "tIgJUpGL4yqoGDjLSRpiQt9Ku/ooxKTSJ83TWssJAoGAflsMfkS9hdoAcWMUWkPU\n" +
            "VMTP/yHshZKJK66uAtLJYvSLXMJAN4uCIobiPM0EsdLxTh1nrL36NmWsTZlMhMsS\n" +
            "rPaBIT6f6m2M2+8ixiJoZ1ut2iyKxkkvWcECbXqjWw6ndGyAoL1/7OR5guJliePy\n" +
            "axFzqDc4QQBTtrjLYuHGi9k=";

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     3c:09:6b:31:d7:7c:00:93:b2:79:54:f9:c2:3c:d2:dd:76:56:f0:50
     * Signature Algorithm: ecdsa-with-SHA256
     * Issuer: CN = localhost
     * Validity
     *      Not Before: Aug  1 11:58:34 2019 GMT
     *      Not After : Jul 29 11:58:34 2029 GMT
     * Subject: CN = localhost
     */
    public static final String ECDSA_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBfjCCASOgAwIBAgIUPAlrMdd8AJOyeVT5wjzS3XZW8FAwCgYIKoZIzj0EAwIw\n" +
            "FDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTE5MDgwMTExNTgzNFoXDTI5MDcyOTEx\n" +
            "NTgzNFowFDESMBAGA1UEAwwJbG9jYWxob3N0MFkwEwYHKoZIzj0CAQYIKoZIzj0D\n" +
            "AQcDQgAEs8ThmP8Xi9aBkB3WPfHRflpk6u44/9NIH4IiRSmbB7jmgCH3rP50izNR\n" +
            "va4fKIZUJ0vPCS9zBr4rKVco9Z6qV6NTMFEwHQYDVR0OBBYEFFgf2AXMfO1OpBul\n" +
            "ArF1gqmVA04YMB8GA1UdIwQYMBaAFFgf2AXMfO1OpBulArF1gqmVA04YMA8GA1Ud\n" +
            "EwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSQAwRgIhAKWR1yXjBedp6hOoxvZ8n9e8\n" +
            "k2ZPdboTfyIRvCw9O4BUAiEAuHsWWs34c3xPCxsyoxbpgkBLwdZ1pZASbCMbgZ59\n" +
            "RYo=\n" +
            "-----END CERTIFICATE-----";
    public static final String ECDSA_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgajTO2cTGJdOUawrQ\n" +
            "XqGfGuX6AEevTXQY0hlVHAVx516hRANCAASzxOGY/xeL1oGQHdY98dF+WmTq7jj/\n" +
            "00gfgiJFKZsHuOaAIfes/nSLM1G9rh8ohlQnS88JL3MGvispVyj1nqpX";

    /*
     * This EC-key certificate is singed by the above RSA CA, namely RSA_CERT.
     *
     * Version: 3 (0x2)
     * Serial Number:
     *     6a:5e:bb:97:3c:f8:0a:0d:ef:0a:ca:72:0b:6d:7f:b5:e0:af:b2:86
     * Signature Algorithm: sha256WithRSAEncryption
     * Issuer: CN = localhost
     * Validity
     *      Not Before: Apr 14 08:14:04 2020 GMT
     *      Not After : Apr 12 08:14:04 2030 GMT
     * Subject: CN = localhost
     */
    public static final String ECRSA_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIICLTCCARWgAwIBAgIUal67lzz4Cg3vCspyC21/teCvsoYwDQYJKoZIhvcNAQEL\n" +
            "BQAwFDESMBAGA1UEAwwJbG9jYWxob3N0MB4XDTIwMDQxNDA4MTQwNFoXDTMwMDQx\n" +
            "MjA4MTQwNFowFDESMBAGA1UEAwwJbG9jYWxob3N0MFkwEwYHKoZIzj0CAQYIKoZI\n" +
            "zj0DAQcDQgAEZOIGqyJHpWFhyiRbZACdNBYHvXTzWVWMC10RW8vfxiOPAZBlPzqn\n" +
            "d2X6/bGhSN1EkrMl8YlJTAKvZcGaaKFUHKNCMEAwHQYDVR0OBBYEFCl9FR9xeNjc\n" +
            "5+Zkg/Rrk7JpTKnFMB8GA1UdIwQYMBaAFG2c8J0rzOu1Agd54OX0xnC9uLqlMA0G\n" +
            "CSqGSIb3DQEBCwUAA4IBAQCPcwr88n/vjsHPByiF28P2cEZ02JdQH0FQVe+6Xw7t\n" +
            "Rn62aTAmS3kaHovXXrFpDpwgz+BMtGSNVTeR7zFttAZLyYb6w6rD8tCfZqHqOTC8\n" +
            "ctCHz7D2QnsH3tdSV1J7A8N3+P8t4cmCs1AED92yLhy9sumXBvZ2ZskpUtcA5nZB\n" +
            "djTvyJ3F74835w0s2FzWPnTULvBmit2Z94b22QyZLkFhThUpMBlu2LmXosLrdfji\n" +
            "xVcV68tpQ1nk1o9tE4V7h4/SjYVaDM1fmlaY+eM3XcbK30mVyktty5ScuOMhLpb6\n" +
            "RFP/QKvmQ/2l4+rj/epV84ImDuEAhkBGOU6vo4X4l1Du\n" +
            "-----END CERTIFICATE-----";
    public static final String ECRSA_KEY =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgldlJrkmEDVtzh4r9\n" +
            "NO8Yn/89mZuBhKPasVgpRjKQxRyhRANCAARk4garIkelYWHKJFtkAJ00Fge9dPNZ\n" +
            "VYwLXRFby9/GI48BkGU/Oqd3Zfr9saFI3USSsyXxiUlMAq9lwZpooVQc";

    /*
     * Version: 3 (0x2)
        Serial Number:
            76:07:da:cb:0f:8a:89:26:72:cb:db:20:ec:df:b2:52:50:01:6a:56
        Signature Algorithm: rsassaPss
         Hash Algorithm: sha256
         Mask Algorithm: mgf1 with sha256
          Salt Length: 0xDE
         Trailer Field: 0xBC (default)
        Issuer: CN = localhost
        Validity
            Not Before: Aug  1 11:58:40 2019 GMT
            Not After : Jul 29 11:58:40 2029 GMT
        Subject: CN = localhost
     */
    public static final String RSASSAPSS_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDaTCCAiCgAwIBAgIUdgfayw+KiSZyy9sg7N+yUlABalYwPgYJKoZIhvcNAQEK\n" +
            "MDGgDTALBglghkgBZQMEAgGhGjAYBgkqhkiG9w0BAQgwCwYJYIZIAWUDBAIBogQC\n" +
            "AgDeMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0xOTA4MDExMTU4NDBaFw0yOTA3\n" +
            "MjkxMTU4NDBaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCASAwCwYJKoZIhvcNAQEK\n" +
            "A4IBDwAwggEKAoIBAQC5igqwiTdawCKIDmGVXAnYSIj5QIiMW4VzeWj87+bWqMec\n" +
            "9uiOkFBI9c1y3CMoAPu9SEBbycAMadExB0pRq93Kz7pO30nyOFwDhvnArqg0e+mn\n" +
            "6yaJeYWkQFX0HNS/vBwlVPLSkyuE80Tt1bND7ur4z31hT6H16nDBfx14b9aXW9j0\n" +
            "L2zqZbyq4jhbELeBK0DtD1tpmJsYHxXjL174fDQ0dArNjIq529veS9z+FjdpuZTm\n" +
            "e3XxOyWofA0EV4t3wN7x5RvI0pTo7Na+15TjTlhwHTuaiUPsOvMg73sI+3OxXGHI\n" +
            "GDoOgqliYqHH0SkTYWpZF9Be3Th/R90Qg7Pvzo4HAgMBAAGjUzBRMB0GA1UdDgQW\n" +
            "BBRQAfLTSK6mt9aKxrWbHUKsKwrBfDAfBgNVHSMEGDAWgBRQAfLTSK6mt9aKxrWb\n" +
            "HUKsKwrBfDAPBgNVHRMBAf8EBTADAQH/MD4GCSqGSIb3DQEBCjAxoA0wCwYJYIZI\n" +
            "AWUDBAIBoRowGAYJKoZIhvcNAQEIMAsGCWCGSAFlAwQCAaIEAgIA3gOCAQEAQPJz\n" +
            "TGugNS+wmxe6BGHmWLLsRJAQn/lr+3dJIfkfBlmkc43tSdL5R+5LfkNjE7sCUW4s\n" +
            "FFKVlQH8XzHbJH0USNp+yxJBjBv5XpXW+mrhGhCBiIoEXce78irNJLy6dJPIFo/m\n" +
            "z4Lt2YS5VassInrBvb9KyNlinpqJ5sjptLM2Nc77Rv/uFOkgTNwyuAi+LYuP1lEm\n" +
            "4AZcywjfxBv/mmuZ8oAgPj50cN0gsgQmi/bofiZsK4GrZpSncjMYZvG/C4WF2Zem\n" +
            "cd7KZtQoPrv3bSE3gyotN04wE2nFLsaR2gheuv0URitDPAzpv8QV3WjEUt6uaFZi\n" +
            "K6deQ/N/JiwhoqjM+Q==\n" +
            "-----END CERTIFICATE-----";
    public static final String RSASSAPSS_KEY =
            "MIIEuwIBADALBgkqhkiG9w0BAQoEggSnMIIEowIBAAKCAQEAuYoKsIk3WsAiiA5h\n" +
            "lVwJ2EiI+UCIjFuFc3lo/O/m1qjHnPbojpBQSPXNctwjKAD7vUhAW8nADGnRMQdK\n" +
            "Uavdys+6Tt9J8jhcA4b5wK6oNHvpp+smiXmFpEBV9BzUv7wcJVTy0pMrhPNE7dWz\n" +
            "Q+7q+M99YU+h9epwwX8deG/Wl1vY9C9s6mW8quI4WxC3gStA7Q9baZibGB8V4y9e\n" +
            "+Hw0NHQKzYyKudvb3kvc/hY3abmU5nt18TslqHwNBFeLd8De8eUbyNKU6OzWvteU\n" +
            "405YcB07molD7DrzIO97CPtzsVxhyBg6DoKpYmKhx9EpE2FqWRfQXt04f0fdEIOz\n" +
            "786OBwIDAQABAoIBAH4eO03st9fUKqTZQbPJMFf4VkM3fpoJzBjGOntUh1D6wVUM\n" +
            "8N+XcTtm+hRNVwhmQrhTWIwMA6NsemodToNdlBG8SiQ624Tukn1DTpmPH38ti5I8\n" +
            "4aEpHZKcuNCKmIMMVwV5TOWebEKfKgeQ754J1Wbzg4KWIr2KcsLUqS+otfGDsOMK\n" +
            "nuIhFQhamtNFzuWSRIYJl5jfNcnXmeTivVNywE0Q/PGD3lLn8xB3Bk6uNTAUFBdc\n" +
            "nbK7efViSfuNY+kZbHne+mcSGiBJPSzTfd25+/JhYaKFjPiQsIqPAwnZK80LBdeb\n" +
            "lxf3zSzpgbx9Jai+kULZJsrVoReZlS6fxeqzZAECgYEA4jRcR6tEQGImsIT7zBTS\n" +
            "FYTsqr0wzuUl2m3mNNQX9ZIKEVJxv9Vevyd4eQIwQRwgPM2U2JLsXPjVFc/fCAJO\n" +
            "KuLY5sXog4b0c8cHjA8nbJbmjKHkXfgCnKFGoXvUV13LgFg9DX6hzkCKMJxDO9R+\n" +
            "pE9k6HXq58yyDvRBvFOCuYECgYEA0fpxa0gwCmyMKQeFnBPd53rnPOBoW2YKnIzR\n" +
            "/X1q6YRFdeRgvcBXScPknU1nvoxAtRqHYDSI3d/sHMzZ+qb0BBoD7i2qjKsSH32u\n" +
            "jP5m5+psPebJ0UEH/bTUbETWEu9rt8sapag6Mp1QL6uYZW5OOULCpGYa9KcfX93A\n" +
            "hwgeO4cCgYBy+mptg4CNuVYxI2dZtLuUdJxXrRLCF3fGL1Z0Q9pp2HGFnIJ1H9+p\n" +
            "CkcSOyqL7d/1CApAi23ZVCH7lE2ppIJXCjd2FeK5+D8JGoGbj5haedl2YlPR795j\n" +
            "/xYHvwmP3v0xn6ho05UrYWLckpEaOEim/DQudMGSUVmwgDdpookwAQKBgQCv6RhL\n" +
            "wFY+5WEmnl6YuywUWSqQHZBPwdTyAieKLh/7MgzfD0zcqt51td84yTg4slcjYe43\n" +
            "8ssW1hmApz2Wd3fGV+UjDK7s2gR8zVYGWLrtX77+vPImlEyVh4DOk3yksF+Vwlm4\n" +
            "no7jCFe9GAy8LQTrg7p87+11OO1X6vb4KRzq0QKBgCZD8lN/qHpscBQucx60vToU\n" +
            "247vlb9LmzsMFVUeyJhg/v1+1kswIImuYC+X0nO8yF++mD8OyWIZaXZAkmEsU9qF\n" +
            "ZCdo4KHSmFTKm6mCPW+5tro3GCsavRZqFHeQF8iVRsN3V86q6wRlMvyYmKMLd0Ko\n" +
            "0CyaEnQ+kBtL6IaeVNQV";

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     49:33:8a:a8:cd:d9:14:f8:09:a1:0c:2f:67:a3:27:a6:fc:df:25:f8
     * Signature Algorithm: dsa_with_SHA256
     * Issuer: CN = localhost
     * Validity
     *     Not Before: Aug  1 12:01:30 2019 GMT
     *     Not After : Jul 27 12:01:30 2039 GMT
     * Subject: CN = localhost
     */
    public static final String DSA_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEbzCCBBSgAwIBAgIUSTOKqM3ZFPgJoQwvZ6MnpvzfJfgwCwYJYIZIAWUDBAMC\n" +
            "MBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAeFw0xOTA4MDExMjAxMzBaFw0zOTA3Mjcx\n" +
            "MjAxMzBaMBQxEjAQBgNVBAMMCWxvY2FsaG9zdDCCA0cwggI5BgcqhkjOOAQBMIIC\n" +
            "LAKCAQEAsFMaoryk333Vm0JY8QEu1y0HmQkvp5dlM/4ozMj8l6hx8HYo+LYTQD4e\n" +
            "t7b7xUf5sYc4mjxpwbV4uf8Q4G1BHfJCGdGKxKigObsbDqpRRBlubHppOX6F6mRz\n" +
            "wgaNRfWqlZbcSa+X82SfagtjMVKrH75eIs74U7EBQRun7XilrDFKuG6c98cY1JFI\n" +
            "BpAp/Sw+kEk0LYYgdGIVWhNCunECcqWtdz1AEBzHOiqEthKTzF+o1qxBFCYjOnZo\n" +
            "wkgG5fqXDc4Rb0iNyxSQXK/lTi/4r6IABY3u7f5NOhj0GmRbbCA/Ef0i6GQKJqzk\n" +
            "hfzTQDwRsvX17xLFTOeNQb26dvm23wIhAL6RNExpfF++/0Ph4mkPqxuDLHWACa/y\n" +
            "3VeNr8NjO7ovAoIBABndio/73FVBpnpbFdT1DnqY3IWUbpr40zudROuGWvSd66Ia\n" +
            "pNtRb/fcgMm3UjAq92SjbF+Rn+gf1ylm1LCtE4xeN02gxCJnR9/HKmuSTDnVOH5F\n" +
            "62yjQaEPZ7rG2cr7fP67YiW1b3nTQSL11y62MVvp+JH1BMVd4gYMop7wG8eRajFr\n" +
            "hW3AY6dz5J2w6fywvZTIXzv1cZS7be1adcdYSvkFs9V6bT+lQHKNpqM8aab61Kp9\n" +
            "aB3+p39nKYm6KPpc/wuSHs5Ez5C89mLrHB4l6xZAMAWqwkDnUmiRScwfyOIKG8VY\n" +
            "2c9GtfZOCB21dupwXGruFH1tcg5IP7wHJddOWCADggEGAAKCAQEAkrNcrwgXKVWc\n" +
            "gwv4CAJhRAH0svQChcLI1I5+6FB8KomN4xVW+obJcNag3qbTCd3V3mHu6gITxxkq\n" +
            "EoA2zCBQFMAIGW2G1PkqOlBK8K3hOut/IEbWmiMlC51P0AUHBd1NDCY6q96Y+mot\n" +
            "ogGc3lMQZK5mWseUirP6Qt43N7Ev57PXypKC5MnQKA2+NEhhiHvDruSBloj9zu+w\n" +
            "oNhXZP+0dPBb96eeHwcRj25MSuhY+Jpg2OoU+FzDvx7QDEqkq801EBdr9WOiY9hx\n" +
            "DpbUZH3mLYo9tzBwDK8RngPlcwlMpuR/A3pu6qLAGJHnVWb1c9mhNHv+8p5to74k\n" +
            "2RqOaSU26aNTMFEwHQYDVR0OBBYEFJ8MbprhtUOkVraW76QALKQnZ6yNMB8GA1Ud\n" +
            "IwQYMBaAFJ8MbprhtUOkVraW76QALKQnZ6yNMA8GA1UdEwEB/wQFMAMBAf8wCwYJ\n" +
            "YIZIAWUDBAMCA0gAMEUCIHaOTmgo0rK4EWGLruxLiTcHZs1KanLrf9FlKbmur9Ee\n" +
            "AiEAnE+fxuTBexuPj2elmnxViUj/UYo/NlC4OarhIO1SCzk=\n" +
            "-----END CERTIFICATE-----";
    public static final String DSA_KEY =
            "MIICZQIBADCCAjkGByqGSM44BAEwggIsAoIBAQCwUxqivKTffdWbQljxAS7XLQeZ\n" +
            "CS+nl2Uz/ijMyPyXqHHwdij4thNAPh63tvvFR/mxhziaPGnBtXi5/xDgbUEd8kIZ\n" +
            "0YrEqKA5uxsOqlFEGW5semk5foXqZHPCBo1F9aqVltxJr5fzZJ9qC2MxUqsfvl4i\n" +
            "zvhTsQFBG6fteKWsMUq4bpz3xxjUkUgGkCn9LD6QSTQthiB0YhVaE0K6cQJypa13\n" +
            "PUAQHMc6KoS2EpPMX6jWrEEUJiM6dmjCSAbl+pcNzhFvSI3LFJBcr+VOL/ivogAF\n" +
            "je7t/k06GPQaZFtsID8R/SLoZAomrOSF/NNAPBGy9fXvEsVM541Bvbp2+bbfAiEA\n" +
            "vpE0TGl8X77/Q+HiaQ+rG4MsdYAJr/LdV42vw2M7ui8CggEAGd2Kj/vcVUGmelsV\n" +
            "1PUOepjchZRumvjTO51E64Za9J3rohqk21Fv99yAybdSMCr3ZKNsX5Gf6B/XKWbU\n" +
            "sK0TjF43TaDEImdH38cqa5JMOdU4fkXrbKNBoQ9nusbZyvt8/rtiJbVvedNBIvXX\n" +
            "LrYxW+n4kfUExV3iBgyinvAbx5FqMWuFbcBjp3PknbDp/LC9lMhfO/VxlLtt7Vp1\n" +
            "x1hK+QWz1XptP6VAco2mozxppvrUqn1oHf6nf2cpiboo+lz/C5IezkTPkLz2Yusc\n" +
            "HiXrFkAwBarCQOdSaJFJzB/I4gobxVjZz0a19k4IHbV26nBcau4UfW1yDkg/vAcl\n" +
            "105YIAQjAiEAvP+ZQ7yzUk8rNgk65U/SF++Eyt+i+WR1UBvGxAEEKIQ=";

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     58:75:88:9a:e1:e0:da:83:da:d0:e7:3f:02:23:4f:74:ce:43:e0:3f
     * Signature Algorithm: ED25519
     * Issuer: CN = localhost
     * Validity
     *     Not Before: May 25 01:28:49 2020 GMT
     *     Not After : May 23 01:28:49 2030 GMT
     * Subject: CN = localhost
     */
    public static final String ED25519_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBezCCAS2gAwIBAgIUWHWImuHg2oPa0Oc/AiNPdM5D4D8wBQYDK2VwMBQxEjAQ\n" +
            "BgNVBAMMCWxvY2FsaG9zdDAeFw0yMDA1MjUwMTI4NDlaFw0zMDA1MjMwMTI4NDla\n" +
            "MBQxEjAQBgNVBAMMCWxvY2FsaG9zdDAqMAUGAytlcAMhADPu3xC31fcrVuWZ6sOC\n" +
            "85Wap5RqQHiVQIJ1DbQhKgjso4GQMIGNMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0O\n" +
            "BBYEFPbedM1iNhjOapOtdXXnHezJnnSTMB8GA1UdIwQYMBaAFPbedM1iNhjOapOt\n" +
            "dXXnHezJnnSTMA4GA1UdDwEB/wQEAwIBhjAqBgNVHSUBAf8EIDAeBggrBgEFBQcD\n" +
            "AwYIKwYBBQUHAwgGCCsGAQUFBwMJMAUGAytlcANBAOzu4k2pIqplPBx5k+JVcOB7\n" +
            "K325r21JCAWqME+fa2sdUR1FM8LpQkWD363YOfEFleUkl28Tk6Kccz3oc4yc5AI=\n" +
            "-----END CERTIFICATE-----";
    public static final String ED25519_KEY =
            "MC4CAQAwBQYDK2VwBCIEICyAry1Yd7O4M5ttEERs86vMixQRR71oKi4vzSEBTXag";

    /*
     * Version: 3 (0x2)
     * Serial Number:
     *     76:ea:9f:03:d9:af:dd:6a:d4:23:71:54:fe:9e:af:6a:c2:e3:2b:5d
     * Signature Algorithm: ED448
     * Issuer: CN = localhost
     * Validity
     *     Not Before: May 25 01:32:42 2020 GMT
     *     Not After : May 23 01:32:42 2030 GMT
     * Subject: CN = localhost
     */
    public static final String ED448_CERT =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIBxjCCAUagAwIBAgIUduqfA9mv3WrUI3FU/p6vasLjK10wBQYDK2VxMBQxEjAQ\n" +
            "BgNVBAMMCWxvY2FsaG9zdDAeFw0yMDA1MjUwMTMyNDJaFw0zMDA1MjMwMTMyNDJa\n" +
            "MBQxEjAQBgNVBAMMCWxvY2FsaG9zdDBDMAUGAytlcQM6AFVSJI1vSXDf9UMNBfNQ\n" +
            "IUA4lfGSr+7klW//faIVrRphIvD1Mq0SkYQv5b3uyyrkht9FcbVMJjVPAKOBkDCB\n" +
            "jTAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBQ61lrTCnJftliX4a3xBrWlU/XK\n" +
            "eDAfBgNVHSMEGDAWgBQ61lrTCnJftliX4a3xBrWlU/XKeDAOBgNVHQ8BAf8EBAMC\n" +
            "AYYwKgYDVR0lAQH/BCAwHgYIKwYBBQUHAwMGCCsGAQUFBwMIBggrBgEFBQcDCTAF\n" +
            "BgMrZXEDcwD95HY/4XWzrgFNsW2sFha7GWnAZMW9PXcUP243Kt6O1HUsJa/ynKQJ\n" +
            "c0DDikNF+8wl/lwF7XX4toCh9WyN+wWCAi2Eau9ATDumDsme7r+VniT0UZto8WQ2\n" +
            "I4B1bAfPIO7JR1zPfpDVu12muwfm+u4pCAA=\n" +
            "-----END CERTIFICATE-----";
    public static final String ED448_KEY =
            "MEcCAQAwBQYDK2VxBDsEOQcoJHPOIS+azOeX2MIEF4TYdfVEaZf/x1OdYpkI0J3m\n" +
            "//MKpGqR4+s6XL8EQNKKJhjNZlvs34XbQw==";

    private static final String TEST_SRC = System.getProperty("test.src", ".");

    /**
     * Get a PEM-encoded PKCS8 private key from a string.
     *
     * @param keyAlgo the key algorithm
     * @param keyStr string containing the PEM-encoded PKCS8 private key
     * @return the private key
     * @throws NoSuchAlgorithmException if no Provider supports a KeyFactorySpi
     *         implementation for the specified algorithm
     * @throws InvalidKeySpecException if the given key specification is
     *         inappropriate for this key factory to produce a private key.
     */
    public static PrivateKey getKeyFromString(String keyAlgo, String keyStr)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance(keyAlgo);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(
                Base64.getMimeDecoder().decode(keyStr));
        PrivateKey key = keyFactory.generatePrivate(keySpec);
        return key;
    }

    /**
     * Get a PEM-encoded PKCS8 private key from a file.
     *
     * @param keyAlgo the key algorithm
     * @param keyPath path to file containing the PEM-encoded PKCS8 private key
     * @return the private key
     * @throws NoSuchAlgorithmException if no Provider supports a KeyFactorySpi
     *         implementation for the specified algorithm
     * @throws InvalidKeySpecException if the given key specification is
     *         inappropriate for this key factory to produce a private key.
     */
    public static PrivateKey getKeyFromFile(String keyAlgo, String keyPath)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        return getKeyFromString(
                keyAlgo,

                // Filter the below lines if any
                // -----BEGIN PRIVATE KEY-----
                // -----END PRIVATE KEY-----
                readFile(keyPath, line -> !line.startsWith("-----")));
    }

    /**
     * Get an X.509 certificate from an input stream.
     *
     * @param input an input stream with the certificate data.
     * @return the X509Certificate
     * @throws CertificateException on parsing errors.
     * @throws IOException on input stream errors.
     */
    public static X509Certificate getCertFromStream(InputStream input)
            throws CertificateException, IOException {
        try {
            CertificateFactory certFactory
                    = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certFactory.generateCertificate(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }

    /**
     * Get a PEM-encoded X.509 certificate from a string.
     *
     * @param cert string containing the PEM-encoded certificate
     * @return the X509Certificate
     * @throws CertificateException if the certificate type is not supported
     *                              or cannot be parsed
     * @throws IOException
     */
    public static X509Certificate getCertFromString(String certStr)
            throws CertificateException, IOException {
        return getCertFromStream(new ByteArrayInputStream(certStr.getBytes()));
    }

    /**
     * Get a X.509 certificate from a file.
     *
     * @param certFilePath path to file containing certificate
     * @return the X509Certificate
     * @throws CertificateException if the certificate type is not supported
     *                              or cannot be parsed
     * @throws IOException if the file cannot be opened
     */
    public static X509Certificate getCertFromFile(String certFilePath)
            throws CertificateException, IOException {
        return getCertFromStream(
                Files.newInputStream(Paths.get(TEST_SRC, certFilePath)));
    }

    /**
     * Get a DER-encoded X.509 CRL from a file.
     *
     * @param crlFilePath path to file containing DER-encoded CRL
     * @return the X509CRL
     * @throws CertificateException if the crl type is not supported
     * @throws CRLException if the crl cannot be parsed
     * @throws IOException if the file cannot be opened
     */
    public static X509CRL getCRLFromFile(String crlFilePath)
            throws CertificateException, CRLException, IOException {
        File crlFile = new File(TEST_SRC, crlFilePath);
        try (FileInputStream fis = new FileInputStream(crlFile)) {
            return (X509CRL)
                CertificateFactory.getInstance("X.509").generateCRL(fis);
        }
    }

    /**
     * Get a PEM-encoded X.509 crl from a string.
     *
     * @param crl string containing the PEM-encoded crl
     * @return the X509CRL
     * @throws CertificateException if the crl type is not supported
     * @throws CRLException if the crl cannot be parsed
     */
    public static X509CRL getCRLFromString(String crl)
            throws CertificateException, CRLException {
        byte[] crlBytes = crl.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(crlBytes);
        return (X509CRL)
            CertificateFactory.getInstance("X.509").generateCRL(bais);
    }

    /**
     * Read a bunch of certs from files and create a CertPath from them.
     *
     * @param fileNames an array of <code>String</code>s that are file names
     * @throws Exception on error
     */
    public static CertPath buildPath(String [] fileNames) throws Exception {
        return buildPath("", fileNames);
    }

    /**
     * Read a bunch of certs from files and create a CertPath from them.
     *
     * @param relPath relative path containing certs (must end in
     *    file.separator)
     * @param fileNames an array of <code>String</code>s that are file names
     * @throws Exception on error
     */
    public static CertPath buildPath(String relPath, String [] fileNames)
            throws Exception {
        List<X509Certificate> list = new ArrayList<X509Certificate>();
        for (int i = 0; i < fileNames.length; i++) {
            list.add(0, getCertFromFile(relPath + fileNames[i]));
        }
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        return(cf.generateCertPath(list));
    }


    /**
     * Read a bunch of certs from files and create a CertStore from them.
     *
     * @param fileNames an array of <code>String</code>s that are file names
     * @return the <code>CertStore</code> created
     * @throws Exception on error
     */
    public static CertStore createStore(String [] fileNames) throws Exception {
        return createStore("", fileNames);
    }

    /**
     * Read a bunch of certs from files and create a CertStore from them.
     *
     * @param relPath relative path containing certs (must end in
     *    file.separator)
     * @param fileNames an array of <code>String</code>s that are file names
     * @return the <code>CertStore</code> created
     * @throws Exception on error
     */
    public static CertStore createStore(String relPath, String [] fileNames)
            throws Exception {
        Set<X509Certificate> certs = new HashSet<X509Certificate>();
        for (int i = 0; i < fileNames.length; i++) {
            certs.add(getCertFromFile(relPath + fileNames[i]));
        }
        return CertStore.getInstance("Collection",
            new CollectionCertStoreParameters(certs));
    }

    /**
     * Read a bunch of CRLs from files and create a CertStore from them.
     *
     * @param fileNames an array of <code>String</code>s that are file names
     * @return the <code>CertStore</code> created
     * @throws Exception on error
     */
    public static CertStore createCRLStore(String [] fileNames)
            throws Exception {
        return createCRLStore("", fileNames);
    }

    /**
     * Read a bunch of CRLs from files and create a CertStore from them.
     *
     * @param relPath relative path containing CRLs (must end in file.separator)
     * @param fileNames an array of <code>String</code>s that are file names
     * @return the <code>CertStore</code> created
     * @throws Exception on error
     */
    public static CertStore createCRLStore(String relPath, String [] fileNames)
        throws Exception {
        Set<X509CRL> crls = new HashSet<X509CRL>();
        for (int i = 0; i < fileNames.length; i++) {
            crls.add(getCRLFromFile(relPath + fileNames[i]));
        }
        return CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(crls));
    }

    /**
     * Perform a PKIX path build. On failure, throw an exception.
     *
     * @param params PKIXBuilderParameters to use in validation
     * @throws Exception on error
     */
    public static PKIXCertPathBuilderResult build(PKIXBuilderParameters params)
        throws Exception {
        CertPathBuilder builder =
                CertPathBuilder.getInstance("PKIX");
        return (PKIXCertPathBuilderResult) builder.build(params);
    }

    /**
     * Perform a PKIX validation. On failure, throw an exception.
     *
     * @param path CertPath to validate
     * @param params PKIXParameters to use in validation
     * @throws Exception on error
     */
    public static PKIXCertPathValidatorResult validate
        (CertPath path, PKIXParameters params) throws Exception {
        CertPathValidator validator =
            CertPathValidator.getInstance("PKIX");
        return (PKIXCertPathValidatorResult) validator.validate(path, params);
    }

    /**
     * Get the content of a file with given filter condition.
     *
     * @param relativeFilePath path to file that relative to test.src directory.
     * @param predicate The condition for filtering file content
     * @return the file content
     */
    private static String readFile(String relativeFilePath,
            Predicate<String> predicate) {
        Path filePath = Paths.get(TEST_SRC, relativeFilePath);
        try (Stream<String> lines = Files.lines(filePath)) {
            Stream<String> interStream = null;
            if (predicate != null) {
                interStream = lines.filter(predicate);
            }
            return interStream != null
                   ? interStream.collect(Collectors.joining("\n"))
                   : lines.collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Cannot read file", e);
        }
    }

    /**
     * This class is useful for overriding one or more methods of an
     * X509Certificate for testing purposes.
     */
    public static class ForwardingX509Certificate extends X509Certificate {
        private final X509Certificate cert;
        public ForwardingX509Certificate(X509Certificate cert) {
            this.cert = cert;
        }
        public Set<String> getCriticalExtensionOIDs() {
           return cert.getCriticalExtensionOIDs();
        }
        public byte[] getExtensionValue(String oid) {
            return cert.getExtensionValue(oid);
        }
        public Set<String> getNonCriticalExtensionOIDs() {
            return cert.getNonCriticalExtensionOIDs();
        }
        public boolean hasUnsupportedCriticalExtension() {
            return cert.hasUnsupportedCriticalExtension();
        }
        public void checkValidity() throws CertificateExpiredException,
            CertificateNotYetValidException { /* always pass */ }
        public void checkValidity(Date date) throws CertificateExpiredException,
            CertificateNotYetValidException { /* always pass */ }
        public int getVersion() { return cert.getVersion(); }
        public BigInteger getSerialNumber() { return cert.getSerialNumber(); }
        public Principal getIssuerDN() { return cert.getIssuerDN(); }
        public Principal getSubjectDN() { return cert.getSubjectDN(); }
        public Date getNotBefore() { return cert.getNotBefore(); }
        public Date getNotAfter() { return cert.getNotAfter(); }
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return cert.getTBSCertificate();
        }
        public byte[] getSignature() { return cert.getSignature(); }
        public String getSigAlgName() { return cert.getSigAlgName(); }
        public String getSigAlgOID() { return cert.getSigAlgOID(); }
        public byte[] getSigAlgParams() { return cert.getSigAlgParams(); }
        public boolean[] getIssuerUniqueID() {
            return cert.getIssuerUniqueID();
        }
        public boolean[] getSubjectUniqueID() {
            return cert.getSubjectUniqueID();
        }
        public boolean[] getKeyUsage() { return cert.getKeyUsage(); }
        public int getBasicConstraints() { return cert.getBasicConstraints(); }
        public byte[] getEncoded() throws CertificateEncodingException {
            return cert.getEncoded();
        }
        public void verify(PublicKey key) throws CertificateException,
            InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {
            cert.verify(key);
        }
        public void verify(PublicKey key, String sigProvider) throws
            CertificateException, InvalidKeyException, NoSuchAlgorithmException,
            NoSuchProviderException, SignatureException {
            cert.verify(key, sigProvider);
        }
        public PublicKey getPublicKey() { return cert.getPublicKey(); }
        public String toString() { return cert.toString(); }
    }
}
