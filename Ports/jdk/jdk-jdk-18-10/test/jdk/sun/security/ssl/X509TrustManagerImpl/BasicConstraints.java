/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

//
// SunJSSE does not support dynamic system properties, no way to re-use
// system properties in samevm/agentvm mode.
//

/*
 * @test
 * @bug 7166570
 * @summary JSSE certificate validation has started to fail for
 *     certificate chains
 * @run main/othervm BasicConstraints PKIX
 * @run main/othervm BasicConstraints SunX509
 */

import java.util.*;
import java.io.*;
import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.*;
import java.security.spec.*;
import java.security.interfaces.*;

import java.util.Base64;

public class BasicConstraints {

    /*
     * =============================================================
     * Set the various variables needed for the tests, then
     * specify what tests to run on each side.
     */

    /*
     * Should we run the client or server in a separate thread?
     * Both sides can throw exceptions, but do you have a preference
     * as to which side should be the main thread.
     */
    static boolean separateServerThread = true;

    /*
     * Where do we find the keystores?
     */
    // Certificate information:
    // Issuer: C=US, O=Java, OU=SunJSSE Test Serivce
    // Validity
    //     Not Before: Dec 20 13:13:44 2019 GMT
    //     Not After : Dec 17 13:13:44 2029 GMT
    // Subject: C=US, O=Java, OU=SunJSSE Test Serivce
    // X509v3 Subject Key Identifier:
    //     88:A7:8D:A1:4F:85:3C:9B:32:47:88:E8:74:81:65:45:00:DE:DD:45
    // X509v3 Authority Key Identifier:
    //     keyid:88:A7:8D:A1:4F:85:3C:9B:32:47:88:E8:74:81:65:45:00:DE:DD:45
    static String trusedCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDZDCCAkygAwIBAgIUSXd4x4/VUhfEFGgfxEt/BG2n8RIwDQYJKoZIhvcNAQEL\n" +
        "BQAwOzELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpT\n" +
        "U0UgVGVzdCBTZXJpdmNlMB4XDTE5MTIyMDEzMTM0NFoXDTI5MTIxNzEzMTM0NFow\n" +
        "OzELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0Ug\n" +
        "VGVzdCBTZXJpdmNlMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1Cd4\n" +
        "U//Y2P4vIu9BBGi+pm64YXYP2LNRNK/e5/nWWmNKJapCAYYda/FJClrbzpI/FgRU\n" +
        "NLM9B4Uo065FRIrBi1vu8zyYgwT7UK0WsLwg6Z81KH50PfM0ClEx44tTqocYDc7C\n" +
        "gsvbyIeTIbV9AnRlEnBA15WFJAJMTCglaNleXUZ9+A/tazRhHlsRp0Ob8j4tCMJa\n" +
        "RDpGMYTy1XbG+WqC8wXP63a63cwjPrL5uzt/C4W1bgNBfTRwIHSUShNhfdc7ZJNS\n" +
        "r2NFPcwodd7uVle5JePNag7oyhjOFFEaBGq21dl6/ozVRkqSWWAi1P7MRay9eYj3\n" +
        "mLZiZaL6NlWxXnfzVwIDAQABo2AwXjAdBgNVHQ4EFgQUiKeNoU+FPJsyR4jodIFl\n" +
        "RQDe3UUwHwYDVR0jBBgwFoAUiKeNoU+FPJsyR4jodIFlRQDe3UUwDwYDVR0TAQH/\n" +
        "BAUwAwEB/zALBgNVHQ8EBAMCAQYwDQYJKoZIhvcNAQELBQADggEBAI1Lgf1Sd/iR\n" +
        "pXBW6OKE9Oa6WkZx/hKrtm3tw+m5OTU4veQijMPIIgnXw0QYXFMieWSjSz+OGq+v\n" +
        "t5NJWj7afCOADrhswrfAY3q3XY9+HnoXv1OvANFhokos25w6fB9t0lrm5KR+3d8l\n" +
        "RwQbxhr8I6tDn2pDExVXRe8k2PYqkabgG6IqPnLzt4iLhPx4ivzo4Zc+zfQZc672\n" +
        "oyNJw2/iNufHRsoRa8QqHJM9vziYfChZqdSSlTiqaoyijT0Br6/2yyIKfjjt5Abt\n" +
        "cwIDUWqQda62xV7ChkTh7ia3uvBXob2iiB0aI3gVTTqDfK9F5XXtW4BXfqx0hvwB\n" +
        "6JzgmNyDQos=\n" +
        "-----END CERTIFICATE-----";
    static String trustedPrivateKey = // Private key in the format of PKCS#8
        "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDUJ3hT/9jY/i8i\n" +
        "70EEaL6mbrhhdg/Ys1E0r97n+dZaY0olqkIBhh1r8UkKWtvOkj8WBFQ0sz0HhSjT\n" +
        "rkVEisGLW+7zPJiDBPtQrRawvCDpnzUofnQ98zQKUTHji1OqhxgNzsKCy9vIh5Mh\n" +
        "tX0CdGUScEDXlYUkAkxMKCVo2V5dRn34D+1rNGEeWxGnQ5vyPi0IwlpEOkYxhPLV\n" +
        "dsb5aoLzBc/rdrrdzCM+svm7O38LhbVuA0F9NHAgdJRKE2F91ztkk1KvY0U9zCh1\n" +
        "3u5WV7kl481qDujKGM4UURoEarbV2Xr+jNVGSpJZYCLU/sxFrL15iPeYtmJlovo2\n" +
        "VbFed/NXAgMBAAECggEAUZvlQ5q1VbNhenTCc+m+/NK2hncd3WQNJtFIU7/dXuO2\n" +
        "0ApQXbmzc6RbTmppB2tmbRe5NJSGM3BbpiHxb05Y6TyyDEsQ98Vgz0Xl5pJXrsaZ\n" +
        "cjxChtoY+KcHI9qikoRpElaoqBu3LcpJJLxlnB4eCxu3NbbEgneH1fvTeCO1kvcp\n" +
        "i3DDdyfY7WB9RW1yWAveiuqvtnbsPfJJLKEhFvZL2ArYCRTm/oIw64yukNe/QLR5\n" +
        "bGzEJMT2ZNQMld1f+CW9tOrUKrnnPCGfMa351T5we+8B6sujWfftPutgEVx5TmHs\n" +
        "AOW1SntMapbgg46K9EC/C5YQa5D1aNOH9ZTEMkgUMQKBgQDrpPQIHFozeeyZ0iiq\n" +
        "HtReLPcqpkwr/9ELc3SjgUypSvpu0l/m++um0yLinlXMn25km/BP6Mv3t/+1uzAc\n" +
        "qpopkcyek8X1hzNRhDkWuMv4KDOKk5c6qLx8FGSm6q8PYm5KbsiyeCM7CJoeoqJ5\n" +
        "74IZjOIw7UrYLckCb6W8xGQLIwKBgQDmew3vGRR3JmCCSumtJQOqhF6bBYrNb6Qc\n" +
        "r4vrng+QhNIquwGqHKPorAI1J8J1jOS+dkDWTxSz2xQKQ83nsOspzVPskpDh5mWL\n" +
        "gGk5QCkX87jFsXfhvZFLksZMbIdpWze997Zs2fe/PWfPaH6o3erqo2zAhQV0eA9q\n" +
        "C7tfImREPQKBgQDi2Xq/8CN52M9IScQx+dnyC5Gqckt0NCKXxn8sBIa7l129oDMI\n" +
        "187FXA8CYPEyOu14V5KiKvdos66s0daAUlB04lI8+v+g3ZYuzH50/FQHwxPTPUBi\n" +
        "DRzeyncXJWiAA/8vErWM8hDgfOh5w5Fsl4EEfdcmyNm7gWA4Qyknr1ysRwKBgQDC\n" +
        "JSPepUy09VHUTxA59nT5HRmoEeoTFRizxTfi2LkZrphuwCotxoRXiRUu+3f1lyJU\n" +
        "Qb5qCCFTQ5bE8squgTwGcVxhajC66V3ePePlAuPatkWN2ek28X1DoLaDR+Rk3h69\n" +
        "Wb2EQbNMl4grkUUoMA8jaVhBb4vhyQSK+qjyAUFerQKBgQDXZPuflfsjH/d/O2yw\n" +
        "qZbssKe9AKORjv795teblAc3vmsSlNwwVnPdS2aq1LHyoNbetc/OaZV151hTQ/9z\n" +
        "bsA48oOojgrDD07Ovg3uDcNEIufxR0aGeSSvqhElp1r7wAYj8bAr6W/RH6MS16WW\n" +
        "dRd+PH6hsap8BD2RlVCnrT3vIQ==";

    // Certificate information:
    // Issuer: C=US, O=Java, OU=SunJSSE Test Serivce
    // Validity
    //     Not Before: Dec 20 13:13:44 2019 GMT
    //     Not After : Dec 17 13:13:44 2029 GMT
    // Subject: C=US, O=Java, OU=SunJSSE Test Serivce, CN=casigner
    // X509v3 Subject Key Identifier:
    //     4B:6D:B0:B0:E6:EF:45:15:35:B5:FC:6B:E2:C7:FC:A6:E6:C4:EC:95
    // X509v3 Authority Key Identifier:
    //     keyid:88:A7:8D:A1:4F:85:3C:9B:32:47:88:E8:74:81:65:45:00:DE:DD:45
    static String caSignerStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDdzCCAl+gAwIBAgIUDYDCpVXk72hlpeNam094GPxl9Z0wDQYJKoZIhvcNAQEL\n" +
        "BQAwOzELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpT\n" +
        "U0UgVGVzdCBTZXJpdmNlMB4XDTE5MTIyMDEzMTM0NFoXDTI5MTIxNzEzMTM0NFow\n" +
        "TjELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpTU0Ug\n" +
        "VGVzdCBTZXJpdmNlMREwDwYDVQQDDAhjYXNpZ25lcjCCASIwDQYJKoZIhvcNAQEB\n" +
        "BQADggEPADCCAQoCggEBAMC8Z4sqVbWWNp567w28MKN9bkE0rZzQLivLsiz7WYzg\n" +
        "8LsUDhtGkxpAcoiMuxnkPWGgD3Xzdy/enVo/vn9lgw7LHWJ3+FeZt3eOnwFHTBu+\n" +
        "srFrnf7iU7RLkAvl06lTYBWFx15Dv4PCgvqIC4eo1wAGDcKKOshwV5kdw8zBpkx3\n" +
        "1jEkbpiuc0cxaNtdMYqmZrTY0wHVSdHGx02mGp9G3aCRSzXyXrr3uxInt5uW9JYR\n" +
        "bDUGa2uD02jbxRSyIXyrSb2L8bRDNg6tLq+CG6blukcCLHF8D1n+jMes3yB/yA0N\n" +
        "NGcbqmEPBVvVSP2c7Z/3JMCvHsrPkS1E2YPH1I0xL2sCAwEAAaNgMF4wHQYDVR0O\n" +
        "BBYEFEttsLDm70UVNbX8a+LH/KbmxOyVMB8GA1UdIwQYMBaAFIinjaFPhTybMkeI\n" +
        "6HSBZUUA3t1FMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQDAgEGMA0GCSqGSIb3\n" +
        "DQEBCwUAA4IBAQBpwrPMDlCvxRvv91w4oFYhYTV2zj9BecsYQPhbqG9zRiHrJoNE\n" +
        "dDPxZQnjb3P5u2LAe7Cp+Nah1ZSvjnF1oVk7ct+Usz02InojHxN72xDsZOMLWuAN\n" +
        "3CJhjGp6WyYUstRWybpiJzPehZdYfk+FaMxwM54REAiipDTFO07PZrj1h/aDQ0Tl\n" +
        "7D6w2v1pz1IR/ctuij7sFReFvjFEE4JoTNjfqzNWO4ML1vDHVi5MHeBgUckujOrI\n" +
        "P0QqaqP+xJIY+sRrzdckxSfS9AOOrJk2VXY8qEoxCN4wCvHJWuHEAF/Lm65d/hq3\n" +
        "2Uh8P+QHLeuEwF8RoTpjiGM9dXvaqcQz7w5G\n" +
        "-----END CERTIFICATE-----";
    static String caSignerPrivateKey = // Private key in the format of PKCS#8
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDAvGeLKlW1ljae\n" +
        "eu8NvDCjfW5BNK2c0C4ry7Is+1mM4PC7FA4bRpMaQHKIjLsZ5D1hoA9183cv3p1a\n" +
        "P75/ZYMOyx1id/hXmbd3jp8BR0wbvrKxa53+4lO0S5AL5dOpU2AVhcdeQ7+DwoL6\n" +
        "iAuHqNcABg3CijrIcFeZHcPMwaZMd9YxJG6YrnNHMWjbXTGKpma02NMB1UnRxsdN\n" +
        "phqfRt2gkUs18l6697sSJ7eblvSWEWw1Bmtrg9No28UUsiF8q0m9i/G0QzYOrS6v\n" +
        "ghum5bpHAixxfA9Z/ozHrN8gf8gNDTRnG6phDwVb1Uj9nO2f9yTArx7Kz5EtRNmD\n" +
        "x9SNMS9rAgMBAAECggEAZk6cF/8s5+sIqy9OXdgbaW1XbT1tOuQ23gCOX9o8Os/c\n" +
        "eTG4GzpnM3QqV9l8J85D1uKD0nSeO8bLd/CGSlG0M9IVkwNjy/xIqyoFtUQHXmLn\n" +
        "r84UXAv/qqDBoc8pf6RGSKZuodcMfgBuTlaQ6D3zgou0GiQN9//KP/jQyouwnr3A\n" +
        "LyXQekxriwPuSYAPak8s5XLfugOebbSRm2UdGEgX3yrT9FVu9rtgeMKdRaCOU8T4\n" +
        "G2UdpGaiDfm5yrR+2XEIv4oaH3WFxmmfQCxVcOFJ1iRvfKBbLb1UCgtJuCBD067y\n" +
        "dq5PrwUTeAvd7hwZd0lxCSnWY7VvYFNr7iJfyElowQKBgQD8eosot+Th03hpkYDs\n" +
        "BIVsw7oqhJmcrPV1bSZ+aQwqqrOGypNmb7nLGTC8Cj1sT+EzfGs7GqxiLOEn4NXr\n" +
        "TYV//RUPBSEXVp2y+2dot1a9oq0BJ8FwGTYL0qSwJrIXJfkQFrYhVVz3JLIWJbwV\n" +
        "cy4YCQr094BhXTS7joJOUDRsYwKBgQDDbI3Lv+bBK8lLfIBll1RY1k5Gqy/H+qxp\n" +
        "sMN8FmadmIGzHhe9xml6b5EfAZphAUF4vZJhQXloT5Wm+NNIAf6X6dRjvzyw7N9B\n" +
        "d48EFJF4ChqNGBocsQRNr2wPRzQ+k2caw9YyYMIjbhktDzO1U/FJGYW6/Vgr2v4K\n" +
        "siROnXfLWQKBgBOVAZQP5z2opC8z7NbhZuPPrnG7xRpEw+jupUyqoxnwEWqD7bjF\n" +
        "M5jQBFqhRLBQ5buTi9GSuQoIRxJLuuu8IH2TyH1YvX9M5YBLRXL2vVCJ/HcZeURT\n" +
        "gECcfs92wNtQw6d+y3N8ZnB4tSNIm/Th8RJGKUZkp91lWECvxeWDDP3XAoGASfNq\n" +
        "NRAJYlAPfGFAtTDu2i8+r79X9XUGiXg6gVp4umpbqkxY75eFkq9lWzZgFRVEkUwr\n" +
        "eGIubyquluDSEw2uKg5yMMzNSqZYVY3IsOKXqbUpFvtn5jOWTU90tNNdEdD100sI\n" +
        "Y0f6Ly4amNKH3rZFOERQNtJn6zCTsbh3xMgR7QECgYBhQTqxLU5eIu38MKobzRue\n" +
        "RoUkMcoY3DePkKPSYjilFhkUDozIXf/xUGnB8kERZKO+44wUkuPGljiFL1/P/RO9\n" +
        "zhHAV94Kw2ddtfxy05GVtUZ99miBmsMb2m8vumGJqfR8h2xpfc1Ra0zfrsPgLNru\n" +
        "xDTDW+bNbM7XyPvg9mOf7Q==";

    // Certificate information:
    // Issuer: C=US, O=Java, OU=SunJSSE Test Serivce, CN=casigner
    // Validity
    //     Not Before: May  5 02:40:57 2012 GMT
    //     Not After : Jan 21 02:40:57 2032 GMT
    // Subject: C=US, O=Java, OU=SunJSSE Test Serivce, CN=certissuer
    // X509v3 Subject Key Identifier:
    //     B4:E8:EA:80:A9:2B:F5:62:B5:2C:A6:F8:FF:65:BC:CF:51:40:9C:15
    // X509v3 Authority Key Identifier:
    //     keyid:4B:6D:B0:B0:E6:EF:45:15:35:B5:FC:6B:E2:C7:FC:A6:E6:C4:EC:95
    static String certIssuerStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDjDCCAnSgAwIBAgIUJWLHjJR9tY2/5DX3iOcZ2JRKY8cwDQYJKoZIhvcNAQEL\n" +
        "BQAwTjELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpT\n" +
        "U0UgVGVzdCBTZXJpdmNlMREwDwYDVQQDDAhjYXNpZ25lcjAeFw0xOTEyMjAxMzEz\n" +
        "NDVaFw0yOTEyMTcxMzEzNDVaMFAxCzAJBgNVBAYTAlVTMQ0wCwYDVQQKDARKYXZh\n" +
        "MR0wGwYDVQQLDBRTdW5KU1NFIFRlc3QgU2VyaXZjZTETMBEGA1UEAwwKY2VydGlz\n" +
        "c3VlcjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBALWUNWnObPBso4vI\n" +
        "VaSM+Oq1f3EsyrtJWqhu+EG/5UKEwYaNBs1A9u1zM5xc05y4wXJfFj755djtzfsz\n" +
        "OFt1ke/hjhpYSf4DcSJfb99MBvHHXrmrEqIdsPYSaUqT9DrIi+L0z0Rdev++IQJj\n" +
        "j9J213gpi18RNrQWl8Xn9mlkxhCjwj1GoFA6aF+9cvWX8uh2Vrl6Vm28hTKnmTad\n" +
        "FB7nwDF4/mGuKVsiB+YTJJ/2Y6RpNqVF/Z6kET/BE0DtCLlKvY7iljbHc892YzI0\n" +
        "vhxlo4lOB3J4NhsQxJbq+mIlbbqZr+p4WA8hdnwI4UlktI4S7fXQzhA51JHVjZyX\n" +
        "f9XYTRUCAwEAAaNgMF4wHQYDVR0OBBYEFLTo6oCpK/VitSym+P9lvM9RQJwVMB8G\n" +
        "A1UdIwQYMBaAFEttsLDm70UVNbX8a+LH/KbmxOyVMA8GA1UdEwEB/wQFMAMBAf8w\n" +
        "CwYDVR0PBAQDAgEGMA0GCSqGSIb3DQEBCwUAA4IBAQCGrjnGs23pQkQoUu8+C2y/\n" +
        "OAT5k9uyPCcLxFPM+Hon5WI6DACxpj7mu2ekN0fswu6B7beQVygpnNSQFVqLrJw1\n" +
        "daYdhTMzkNCkPk6q0cUmj5k94jfCHBl4jw+qoZiIehuR9qFHhpLkT4zMTkFof+P+\n" +
        "Lfc92QJppUAOh3jTvHK01YwP2sxK3KXhcbofQnxGS4WHrqmmZC2YO/LQRoYDZdUY\n" +
        "zr4da2aIg9CKrH2QWoMkDfRKkJvrU3/VhVfVWpNbXFE2xZXftQl3hpFCJ3FkpciA\n" +
        "l3hKeq4byY3LXxhAClHpk1KkXJkMnQdOfA5aGekj/Cjuaz1/iKYAG2vRq7YcuM/o\n" +
        "-----END CERTIFICATE-----";
    static String certIssuerPrivateKey = // Private key in the format of PKCS#8
        "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQC1lDVpzmzwbKOL\n" +
        "yFWkjPjqtX9xLMq7SVqobvhBv+VChMGGjQbNQPbtczOcXNOcuMFyXxY++eXY7c37\n" +
        "MzhbdZHv4Y4aWEn+A3EiX2/fTAbxx165qxKiHbD2EmlKk/Q6yIvi9M9EXXr/viEC\n" +
        "Y4/Sdtd4KYtfETa0FpfF5/ZpZMYQo8I9RqBQOmhfvXL1l/Lodla5elZtvIUyp5k2\n" +
        "nRQe58AxeP5hrilbIgfmEySf9mOkaTalRf2epBE/wRNA7Qi5Sr2O4pY2x3PPdmMy\n" +
        "NL4cZaOJTgdyeDYbEMSW6vpiJW26ma/qeFgPIXZ8COFJZLSOEu310M4QOdSR1Y2c\n" +
        "l3/V2E0VAgMBAAECggEBAJjfVrjl2kHwtSCSYchQB6FTfSBDnctgTrtP8iMo9FO0\n" +
        "gVpOkVNtRndTbjhOzro7smIgPBJ5QlIIpErBLMmTinJza7gybNk2/KD7yKwuzgnw\n" +
        "2IdoyB9E8B+8EHmBZzW2ck953KaqLUvzPsdMG2IOPAomr/gx/eRQwScVzBefiEGo\n" +
        "sN+rGfUt/RNAHwWje1KuNDj21S84agQhN6hdYUnIMsvJLu/9mOwUb9ff+AzTUfFr\n" +
        "zyx2MJL4Cx59DkUUMESCfinlHUc21llQjFWmX/zOoGY0X0qV/YM/GRsv1ZDFHw9o\n" +
        "hQ6m8Ov7D9wB3TKZBI97sCyggjBfSeuYQlNbs99KWQECgYEA7IKNL0ME7FuIrKYu\n" +
        "FCQ/Duz1N3oQXLzrTGKUSU1qSbrU2Jwk4SfJ8ZYCW1TP6vZkaQsTXmXun3yyCAqZ\n" +
        "hcOtDBhI+b7Wpmmyf6nb83oYJtzHMRQZ5qS+9vOBfV9Uf1za8XI4p90EqkFHByCF\n" +
        "tHfjVbjK39zN4CvaO3tqpOaYtL0CgYEAxIrTAhGWy9nBsxf8QeqDou0rV5Cw50Kl\n" +
        "kQsE7KLmjvrMaFFpUc5lgWoC+pm/69VpNBUuN/38YozwxVjVi/nMJuuK150mhdWI\n" +
        "B28FI7ORnFmVeSvTrP4mBX1ct2Tny9zpchXn3rpHR5NZUs7oBhjudHSfRMrHxeBs\n" +
        "Kv2pr2s6uzkCgYAtrEh3iAm7WzHZpX3ghd9nknsIa5odTp5h8eeRAFI2Ss4vxneY\n" +
        "w4ZMERwDZy1/wnVBk9H5uNWMFxiKVQGww0j3vPjawe/R0zeVT8gaDMn9N0WARNF7\n" +
        "qPT3265196LptZTSa6xlPllYR6LfzXgEkeJk+3qyIIHheJZ8RikiDyYOQQKBgQC/\n" +
        "rxlegiMNC4KDldf7vanGxAKqcz5lPbXWQOX7mGC+f9HNx+Cs3VxYHDltiXgJnOju\n" +
        "191s1HRK9WR5REt5KhY2uzB9WxJQItJ5VYiwqhhQYXqLY/gdVv1kC0DayDndtMWk\n" +
        "88JhklGkeAv83DikgbpGr9sJr6+oyFkWkLDmmfD82QKBgQCMgkZJzrdSNNlB0n5x\n" +
        "xC3MzlsQ5aBJuUctnMfuyDi+11yLAuP1oLzGEJ7qEfFoGRO0V8zJWmHAfNhmVYEX\n" +
        "ow5g0WbPT16GoRCiOAzq+ewH+TEELMF6HWqnDuTnCg28Jg0dw2kdVTqeyzKOQlLG\n" +
        "ua9c2DY3PUTXQPNqLVhz+XxZKA==";

    // Certificate information:
    // Issuer: C=US, O=Java, OU=SunJSSE Test Serivce, CN=certissuer
    // Validity
    //     Not Before: Dec 20 13:13:45 2019 GMT
    //     Not After : Dec 17 13:13:45 2029 GMT
    // Subject: C=US, O=Java, OU=SunJSSE Test Serivce, CN=localhost
    // X509v3 Subject Key Identifier:
    //     46:FC:94:7A:61:6D:BF:5F:AE:D7:20:EC:BF:6A:74:2A:26:F1:D4:4C
    // X509v3 Authority Key Identifier:
    //     keyid:B4:E8:EA:80:A9:2B:F5:62:B5:2C:A6:F8:FF:65:BC:CF:51:40:9C:15
    static String serverCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDfDCCAmSgAwIBAgIUHsJi1HTWpR3FCiOiG/qLK6BDluwwDQYJKoZIhvcNAQEL\n" +
        "BQAwUDELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpT\n" +
        "U0UgVGVzdCBTZXJpdmNlMRMwEQYDVQQDDApjZXJ0aXNzdWVyMB4XDTE5MTIyMDEz\n" +
        "MTM0NVoXDTI5MTIxNzEzMTM0NVowTzELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEph\n" +
        "dmExHTAbBgNVBAsMFFN1bkpTU0UgVGVzdCBTZXJpdmNlMRIwEAYDVQQDDAlsb2Nh\n" +
        "bGhvc3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCaDgoxN2UQQero\n" +
        "oBQ4JlQP1BFaZEtIkdIU2VJs4whz85J0LSB/68iEOS5e8wCz9wiQWr4isor7sl3e\n" +
        "B2dnLGY28BthOTw2j/CYw/dRqyDbPZniooB233uLGarKjqQWXpRFQi6bgEQmNqWe\n" +
        "C32w+V+Oq3CTkinwgPvA5mnSe0P8gpF9NLZBFn0TtxaY0bQIie2WNk/HjrVQIhq3\n" +
        "qmG/zVxeBc3PVOOU/OKrwjHbim9YI+zdDRXjNm8siHi0RF2+fkxfyAm8Qg+mT8L4\n" +
        "xdtr0a+eP4oIvkymRURxIrXNnvoX+MhYKSOQnizpW0NMOZ5L9nyw1cYX8j9Ed6eM\n" +
        "kzxZwRrlAgMBAAGjTzBNMB0GA1UdDgQWBBRG/JR6YW2/X67XIOy/anQqJvHUTDAf\n" +
        "BgNVHSMEGDAWgBS06OqAqSv1YrUspvj/ZbzPUUCcFTALBgNVHQ8EBAMCA+gwDQYJ\n" +
        "KoZIhvcNAQELBQADggEBAGXHGefA1j136yenwK+j9K5VnG2kYGXCadi9bKtTXf/X\n" +
        "6Xasb7QE2QWEIlq+78AaV9Dwc7qk1TuBsN05LbQUSe7h5UAfS4AZ5l/XSay2cxrZ\n" +
        "TKoyuzh9kj38QkEBxZlrClyBzU8Mct0L9F8yEm4V7AqQOshn9gEQl9lzJUb2KHeZ\n" +
        "AxblrQhPQDrWhmQjQkl/xaiOiU31sHKTnB/L2CKvJtmsKIyBdrQCQTlIOcRu4/PQ\n" +
        "4z/sjecKP08Xkf5+p4RzPL+OZHkJoejSEjBndLC8BK9IZD94kHZYDz8ulWrQJ5Nr\n" +
        "u/inkyf8NcG7zLBJJyuKfUXO/OzGPD5QMviVc+PCGTY=\n" +
        "-----END CERTIFICATE-----";
    static String serverPrivateKey = // Private key in the format of PKCS#8
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCaDgoxN2UQQero\n" +
        "oBQ4JlQP1BFaZEtIkdIU2VJs4whz85J0LSB/68iEOS5e8wCz9wiQWr4isor7sl3e\n" +
        "B2dnLGY28BthOTw2j/CYw/dRqyDbPZniooB233uLGarKjqQWXpRFQi6bgEQmNqWe\n" +
        "C32w+V+Oq3CTkinwgPvA5mnSe0P8gpF9NLZBFn0TtxaY0bQIie2WNk/HjrVQIhq3\n" +
        "qmG/zVxeBc3PVOOU/OKrwjHbim9YI+zdDRXjNm8siHi0RF2+fkxfyAm8Qg+mT8L4\n" +
        "xdtr0a+eP4oIvkymRURxIrXNnvoX+MhYKSOQnizpW0NMOZ5L9nyw1cYX8j9Ed6eM\n" +
        "kzxZwRrlAgMBAAECggEBAIPF4p36ni3r1H2q/+CPmHP5l+ZTx7mJUcOXqNOO11on\n" +
        "TGyndRc2ncvMBYgeH8nQUrj3hY+0XQGyrmwOtTohVkVD2IevJ3wcX1asuU5YLMCb\n" +
        "zpd3HJ+RxeFT0S12GZEw0W70j11ft+tf7wZjGd5ZUI1+w8rWyZz5F18HOBlcauj/\n" +
        "iqMgrlVLZ7qXEb6WV9zP5hWx5nZwrnuuiM1zXLVuO9rg7qk+zCts2oyM8KRTfQIi\n" +
        "Zo3VDO0nwnEoxxTQ/2g2g/jBJ1GiFygiFm/i2SHQOJgaFS3Y3InjWEAiINIsdMIt\n" +
        "yZk6twMG6ODjy8agZ4LLhZSyCkC33AN7MIkSCtvFubkCgYEAyRm+yvYxwiHCzZV8\n" +
        "LZNuBBRliujgG41iuyUyRVBSaMyJMNRoOMm8XwDOF1BA44YPz4yCkfiiEk2ub/f0\n" +
        "hDhfBW3EWvYHrWkEbx9Th2YmFq20JlgcBGaM2TiiL+qx4ct687idPbJVZnwc4HtR\n" +
        "Kc0eTwRlFsf2O3rwIy52mvIf/48CgYEAxBxsllVz7+/nm0UcxwHNDN+bPyDBxsu9\n" +
        "QuSyR+zSnfcL6xaS4SClBLKxHSjbJ2Hi+UOXezO+eozDp/zEFI6BygNRKTaLTVKr\n" +
        "ezk9rbyKydRIXNlxFoX07U0KlD4lCrbrpsvcO/OlzJe6q5R0B/CIQmx2Y4wlRrE/\n" +
        "Tu+hsf3tBEsCgYBBltsKmXerKJW/tbS1rLMiM4DW6JNHiTqdbUlTIBpwwd0xBuYj\n" +
        "N3Dvz3RoWC2Bx9TaTaq8b0p1C88MB+RBR51+SMnVHQ9t+KWQlLgKnj9oACmUpAIn\n" +
        "UUc5BeaoGDUCPvqQCTOHzuVZsrs8YBwdtR/gh79sybU+ux8damcWrEfRcwKBgEsU\n" +
        "HrZHLMWU8PROtz+w/tGI4aR/Y/A5m9F6QI6sqc10AQoVcFHj74km6Auj0pL3NK/9\n" +
        "Ioc2Phwou9caO+8qx6GRN4cxrI8DsUbRmT1kSzYNoU56qILY8fXPYtdyGzhI41rN\n" +
        "/RiupLD4/awmf21ytpfHcmOWCcdQoE4WC69a6VyVAoGAboeogM5/TRKj80rXfUH2\n" +
        "lFZzgX246XGwNyOVVgOuv/Oxa61b5FeeCpnFQcjpZmC5vd63X3w7oYSDe2wUt+Wh\n" +
        "LhYunmcCEj+yb3of33loQb/FM2OLW9UoQakB7ewio9vtw+BAnWxnHFkEaqdxMXpy\n" +
        "TiSXLpQ1Q9GvDpzngDzJzzY=";

    // Certificate information:
    // Issuer: C=US, O=Java, OU=SunJSSE Test Serivce, CN=certissuer
    // Validity
    //     Not Before: Dec 20 14:21:29 2019 GMT
    //     Not After : Dec 17 14:21:29 2029 GMT
    // Subject: C=US, O=Java, OU=SunJSSE Test Serivce, CN=InterOp Tester
    // X509v3 Subject Key Identifier:
    //     1F:E4:C0:F5:B8:68:DB:D2:EB:9E:6F:BB:B5:9E:92:6D:BA:7D:97:3A
    // X509v3 Authority Key Identifier:
    //     keyid:B4:E8:EA:80:A9:2B:F5:62:B5:2C:A6:F8:FF:65:BC:CF:51:40:9C:15
    static String clientCertStr =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDgTCCAmmgAwIBAgIUHFQOStLURT5sQ57OWO2z8iNJ9P8wDQYJKoZIhvcNAQEL\n" +
        "BQAwUDELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEphdmExHTAbBgNVBAsMFFN1bkpT\n" +
        "U0UgVGVzdCBTZXJpdmNlMRMwEQYDVQQDDApjZXJ0aXNzdWVyMB4XDTE5MTIyMDE0\n" +
        "MjEyOVoXDTI5MTIxNzE0MjEyOVowVDELMAkGA1UEBhMCVVMxDTALBgNVBAoMBEph\n" +
        "dmExHTAbBgNVBAsMFFN1bkpTU0UgVGVzdCBTZXJpdmNlMRcwFQYDVQQDDA5JbnRl\n" +
        "ck9wIFRlc3RlcjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMXA3NV+\n" +
        "pDnwnQgXFQ7WeDtcTe4qDQV9tDj9cRZFqQXo94C30lkuXzdH761bZB84DESV0qLI\n" +
        "k6/n+D9SOsg7SPe7uejG24rph/VpPANrPXo8jxwh/KW+8y0pYNigFUZDi+mEDAOG\n" +
        "gyqaAbahQePDYTa09uY3MTTOcaUnKZEJkfVZnmrwmcH7qapCCz0N4Mv6Xddi87Fk\n" +
        "j9R225XXW5ZZ+jwVGi1WubjxqLpbQo9VwdTgozBfxwzjQQWDOlUIics3RRaV4Yz0\n" +
        "F3Sr4xZiq09O4x8ZT8jrQgduzVZhWjc7rHHbBeMmVBhOveSCvu54onZ2Y+G7+xU/\n" +
        "Zc1Z6s2Wb5N2I40CAwEAAaNPME0wHQYDVR0OBBYEFB/kwPW4aNvS655vu7Wekm26\n" +
        "fZc6MB8GA1UdIwQYMBaAFLTo6oCpK/VitSym+P9lvM9RQJwVMAsGA1UdDwQEAwID\n" +
        "6DANBgkqhkiG9w0BAQsFAAOCAQEAdgWs2wVkPoOrShdYTJM2/v7sDYENCsj3VGEq\n" +
        "NvTeL98FCjRZhRmozVi0mli6z2LjDM/858vZoJWDJ08O0XvhXT4yJWWHCJz4xTY1\n" +
        "GBern25Y8VjZGUwAIzK3EDjYzJCZpbhBREF8XZx46OxHt04BKtQwJKBtpJ1/6bRS\n" +
        "wvia3wGspFLW78P2Y5rFXzqptaqBD06Dcc4xBgvFLSocSKUzLc8BdNsixtPBQZNs\n" +
        "l3X3TUNYoYW677E7EWO8NHUJg+2Qbpo11tkb0AyScSxOu2aHuPfYIchRZXnDdq20\n" +
        "tL85OZz8ov7d2jVet/w7FD4M5XfcogsNtpX4kaMsctyvQbDYRA==\n" +
        "-----END CERTIFICATE-----";
    static String clientPrivateKey = // Private key in the format of PKCS#8
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDFwNzVfqQ58J0I\n" +
        "FxUO1ng7XE3uKg0FfbQ4/XEWRakF6PeAt9JZLl83R++tW2QfOAxEldKiyJOv5/g/\n" +
        "UjrIO0j3u7noxtuK6Yf1aTwDaz16PI8cIfylvvMtKWDYoBVGQ4vphAwDhoMqmgG2\n" +
        "oUHjw2E2tPbmNzE0znGlJymRCZH1WZ5q8JnB+6mqQgs9DeDL+l3XYvOxZI/UdtuV\n" +
        "11uWWfo8FRotVrm48ai6W0KPVcHU4KMwX8cM40EFgzpVCInLN0UWleGM9Bd0q+MW\n" +
        "YqtPTuMfGU/I60IHbs1WYVo3O6xx2wXjJlQYTr3kgr7ueKJ2dmPhu/sVP2XNWerN\n" +
        "lm+TdiONAgMBAAECggEBAK3PX8n+L1YFl9++efG6q55w+MX2C8/htn/IspbCz1a0\n" +
        "dqWZ67YavfGWtqCGDTArUQ0PKj2NUdFwb48oNSY8hVvIkhR4hApKTAd1YRwYK8a+\n" +
        "Z4JwlOERPidZkReVTF2fjN/IAc8vcSYGiq78eS85UL6Gu+OIayVgth5Ul4I1CSa8\n" +
        "+b0n/RAI+yk2HxKlkq40Ofn0VWiGg1dLP2MPwwPNIk+w7nKUysfPmXCHfyBr+CZv\n" +
        "1BQ0E/tVau9wsyCjO6wxFsAKteBGdYa0ToEeT0D8MEeY9leKhAAxRneBVCz9AfHj\n" +
        "wMGYucxwL0cDLi1IjZB5wlvm5JPqNCKrkHE2XE+UyTkCgYEA/iNP11cqHNPItoXP\n" +
        "D2wN4uX60kLNbzZ2dOF1ItybS8OcQvTxA1XulARiCVDIT/+QDETbDQclfhgMOfhe\n" +
        "ZCdMrL5RG0YTwg9OGbLcA+8gqd9e/3gs9g8pWNdCfuGIwsnJbpO7iBoCBzHaHHJJ\n" +
        "PbWDFS6jxvsqKIGPPwrhL9yp4VMCgYEAxzPKNLclBHorUs9rYRqiG9NTkLRNx4ll\n" +
        "LUh0FBItOnG85BxkjQaIlzimNvXZEzZnpOtblugAszxFyq2KTEE9qeB/V3w3FkXi\n" +
        "PSpDG5sdRHnl5Qu4PuQ9WsmN7g193tOEdtWQ4NKxPqlC72ehqVDOY7In2quYLUiq\n" +
        "C377esv0658CgYAJ0I1N0LT0pg0zV1mWy+KBZ8ZXBnNunxjWDLr8XK62r1hCkbkZ\n" +
        "GuF63+x1VaRWypTilGotR6BgDUezmW7zyTzB0xvIxN0QeozWmzy5/isxxEmj7h02\n" +
        "Z4F+R9nukoE4nJhl59ivOenoIzm8LYG8m1zznXh/v8VyCQbiNWZa9dettwKBgQDB\n" +
        "Yz4DP2noltJIaqXMd5a5fMe7y89Wz8Qx2g0XDy5pdtHygr37S0R/yrdS1AoR5Ndp\n" +
        "/DPGpSVI3FLFGQUSUqQSr6fwvt6b+OxShRzxR/155P2TB3WvWNVXtiTb3q08Dgyj\n" +
        "cWJdYS5BrwEUen8vaQt1LhgS6lOqYsjysCxkYm078QKBgEJuq4RzecgiGx8srWDb\n" +
        "pQKpxrdEt82Y7OXLVj+W9vixcW/xUYhDYGsfdUigZoOjo4nV8KVmMbuI48PIYwnw\n" +
        "haLwWrBWlki4x9MRwuZUdewOYoo7hDZToZmIDescdiwv8CA/Dg9kOX3YYLPW+cWl\n" +
        "i1pnyMPaloBOhz3Y07sWXxCz";

    static char passphrase[] = "passphrase".toCharArray();

    /*
     * Is the server ready to serve?
     */
    volatile static boolean serverReady = false;

    /*
     * Turn on SSL debugging?
     */
    static boolean debug = false;

    /*
     * Define the server side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doServerSide() throws Exception {
        SSLContext context = getSSLContext(true);
        SSLServerSocketFactory sslssf = context.getServerSocketFactory();

        SSLServerSocket sslServerSocket =
                (SSLServerSocket) sslssf.createServerSocket(serverPort);
        serverPort = sslServerSocket.getLocalPort();
        SSLSocket sslSocket = null;
        try {
            /*
             * Signal Client, we're ready for his connect.
             */
            serverReady = true;

            sslSocket = (SSLSocket) sslServerSocket.accept();
            sslSocket.setNeedClientAuth(true);

            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            sslIS.read();
            sslOS.write(85);
            sslOS.flush();
        } finally {
            if (sslSocket != null) {
                sslSocket.close();
            }
            sslServerSocket.close();
        }
    }

    /*
     * Define the client side of the test.
     *
     * If the server prematurely exits, serverReady will be set to true
     * to avoid infinite hangs.
     */
    void doClientSide() throws Exception {
        /*
         * Wait for server to get started.
         */
        while (!serverReady) {
            Thread.sleep(50);
        }

        SSLContext context = getSSLContext(false);
        SSLSocketFactory sslsf = context.getSocketFactory();

        SSLSocket sslSocket =
                (SSLSocket) sslsf.createSocket("localhost", serverPort);
        try {
            InputStream sslIS = sslSocket.getInputStream();
            OutputStream sslOS = sslSocket.getOutputStream();

            sslOS.write(280);
            sslOS.flush();
            sslIS.read();
        } finally {
            sslSocket.close();
        }
    }

    // get the ssl context
    private static SSLContext getSSLContext(boolean isServer) throws Exception {

        // generate certificate from cert string
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // create a key store
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);

        // import the trused cert
        ByteArrayInputStream is =
            new ByteArrayInputStream(trusedCertStr.getBytes());
        Certificate trusedCert = cf.generateCertificate(is);
        is.close();

        ks.setCertificateEntry("SunJSSE Test Serivce", trusedCert);

        // import the certificate chain and key
        Certificate[] chain = new Certificate[3];

        is = new ByteArrayInputStream(caSignerStr.getBytes());
        Certificate caSignerCert = cf.generateCertificate(is);
        is.close();
        chain[2] = caSignerCert;

        is = new ByteArrayInputStream(certIssuerStr.getBytes());
        Certificate certIssuerCert = cf.generateCertificate(is);
        is.close();
        chain[1] = certIssuerCert;

        PKCS8EncodedKeySpec priKeySpec = null;
        if (isServer) {
            priKeySpec = new PKCS8EncodedKeySpec(
                            Base64.getMimeDecoder().decode(serverPrivateKey));
            is = new ByteArrayInputStream(serverCertStr.getBytes());
        } else {
            priKeySpec = new PKCS8EncodedKeySpec(
                            Base64.getMimeDecoder().decode(clientPrivateKey));
            is = new ByteArrayInputStream(clientCertStr.getBytes());
        }
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateKey priKey = (RSAPrivateKey)kf.generatePrivate(priKeySpec);
        Certificate keyCert = cf.generateCertificate(is);
        is.close();
        chain[0] = keyCert;

        ks.setKeyEntry("End Entity", priKey, passphrase, chain);

        // check the certification path
        PKIXParameters paras = new PKIXParameters(ks);
        paras.setRevocationEnabled(false);
        CertPath path = cf.generateCertPath(Arrays.asList(chain));
        CertPathValidator cv = CertPathValidator.getInstance("PKIX");
        cv.validate(path, paras);

        // create SSL context
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmAlgorithm);
        tmf.init(ks);

        SSLContext ctx = SSLContext.getInstance("TLS");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
        kmf.init(ks, passphrase);

        ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ks = null;

        return ctx;
    }

    private static String tmAlgorithm;        // trust manager

    private static void parseArguments(String[] args) {
        tmAlgorithm = args[0];
    }

    /*
     * =============================================================
     * The remainder is just support stuff
     */

    // use any free port by default
    volatile int serverPort = 0;

    volatile Exception serverException = null;
    volatile Exception clientException = null;

    public static void main(String args[]) throws Exception {
        if (debug)
            System.setProperty("javax.net.debug", "all");

        /*
         * Get the customized arguments.
         */
        parseArguments(args);

        /*
         * Start the tests.
         */
        new BasicConstraints();
    }

    Thread clientThread = null;
    Thread serverThread = null;
    /*
     * Primary constructor, used to drive remainder of the test.
     *
     * Fork off the other side, then do your work.
     */
    BasicConstraints() throws Exception {
        if (separateServerThread) {
            startServer(true);
            startClient(false);
        } else {
            startClient(true);
            startServer(false);
        }

        /*
         * Wait for other side to close down.
         */
        if (separateServerThread) {
            serverThread.join();
        } else {
            clientThread.join();
        }

        /*
         * When we get here, the test is pretty much over.
         *
         * If the main thread excepted, that propagates back
         * immediately.  If the other thread threw an exception, we
         * should report back.
         */
        if (serverException != null)
            throw serverException;
        if (clientException != null)
            throw clientException;
    }

    void startServer(boolean newThread) throws Exception {
        if (newThread) {
            serverThread = new Thread() {
                public void run() {
                    try {
                        doServerSide();
                    } catch (Exception e) {
                        /*
                         * Our server thread just died.
                         *
                         * Release the client, if not active already...
                         */
                        System.err.println("Server died...");
                        serverReady = true;
                        serverException = e;
                    }
                }
            };
            serverThread.start();
        } else {
            doServerSide();
        }
    }

    void startClient(boolean newThread) throws Exception {
        if (newThread) {
            clientThread = new Thread() {
                public void run() {
                    try {
                        doClientSide();
                    } catch (Exception e) {
                        /*
                         * Our client thread just died.
                         */
                        System.err.println("Client died...");
                        clientException = e;
                    }
                }
            };
            clientThread.start();
        } else {
            doClientSide();
        }
    }
}
