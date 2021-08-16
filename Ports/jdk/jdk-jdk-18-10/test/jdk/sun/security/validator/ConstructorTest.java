/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6996377
 * @summary shrink duplicate code in the constructor of PKIXValidator
 * @modules java.base/sun.security.validator
 */

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.TrustAnchor;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import javax.security.auth.x500.X500Principal;
import java.util.Date;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Collection;
import sun.security.validator.Validator;
import sun.security.validator.PKIXValidator;

public class ConstructorTest {

    // COMMON-OPTS: All certs created with the following common options:
    // -keystore <STORE> -storepass <PASS> -keypass <PASS> -keyalg rsa
    // -keysize 2048 -validity 720 -sigalg sha256withrsa

    // keytool <COMMON-OPTS> -alias root -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC3jCCAcagAwIBAgIEInKZgjANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDEwRS\n" +
        "b290MB4XDTE0MDUwODE4MjcwOFoXDTE2MDQyNzE4MjcwOFowDzENMAsGA1UEAxME\n" +
        "Um9vdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIzgMkrCZD7PuoFb\n" +
        "fmtAd2+Td6nA9sgBd8Z3NjQgP6nwyn79svaVV9XAVLTrLian72wV/1Kbq/6HUXQQ\n" +
        "AqyUAvobDwXeIAmE4+D7qcZxiEJgVNr2Ddv1bbS8Y0/Ta72qzjFiEPMO3Y2GP52C\n" +
        "ssKQpsdNttHfM9c73cKUspobc3p51k2lkynheshCSNOWxR/Rvsl/gcbEFg8vIEHV\n" +
        "oJPwKSrABc4sWiiXQj0yLVW+DKVEFuWNqqitcikQLZFpgOYv8P1SjhJFkcA9s0oN\n" +
        "sbvKO2VF141h161i0AFddTsGE85A3j42qEdwQ0cs9gyAoeU865TFvxCuhSqSgJ3a\n" +
        "Mdgn7ssCAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNVHQ8BAf8EBAMCAQYw\n" +
        "HQYDVR0OBBYEFFQY2UJynMSoS7Tf/+DvNPp/ZxXfMA0GCSqGSIb3DQEBCwUAA4IB\n" +
        "AQAeXRQnEhxNl8lrcGY1U1LbIdrNMlGnc0JbgwiVYwRlE3+u4GvDae1VueXyY6nw\n" +
        "8m63H3Q/Do9/72aw2Q0FSwvDg+k5ssj+gXQ3Gyx8xsVPJEG0TizOSwnWiZtWu65w\n" +
        "14p5TB8P8wdPEs6sfE9oheiKhDRjBZHIfqMd4DaBiM9N9qHpSwTJc02BB2KnGwga\n" +
        "yiYNJbce7GFKn/exryj972n/Nl4xy1WdZrRwTBbV21/GINw+xdXn1+FD95EGqGlr\n" +
        "Sb4+G7U7Ydo+xFpVQnrqxZe98pI5W2bG7VSKvIzcPxfL5/tjwtNaqhiD7wIBNHVx\n" +
        "ZeJevm41O9qFQEdXNyVGpB+u\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias int -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String INTERMED =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC/jCCAeagAwIBAgIEDkzdijANBgkqhkiG9w0BAQsFADAPMQ0wCwYDVQQDEwRS\n" +
        "b290MB4XDTE0MDUwODE4MjcyNFoXDTE2MDQyNzE4MjcyNFowDjEMMAoGA1UEAxMD\n" +
        "SW50MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwslILfgvXPxdRgu8\n" +
        "4SnrZJsSzb/XvYwYGAKTSvsDaI1nXypBbwDGz87+YPpZSJcExcS8I8GBKqN6kXIh\n" +
        "YvJ9yMGJX8wdwoMQpw2ZfJpzfw9Nqtlhv8/q5kPlaDghJ+nGNwy5lyYNOzDMVht0\n" +
        "1XQG65C+l7m52lDJ478tGRZEFkx0aTh2QUBI59iNji6r2Buyeiijhg4YBrvIlYLK\n" +
        "OAHxru4N/Y2Cq3ECUUvm7Lf8tM8yrINS8FLT+pmNcLj8AKkGW8cFFaiGPMyon0/m\n" +
        "4iJB7ZaeG+BGm9TvBv93cphAsM2tY+S+P/dLfI01ltucibPkSglmquUSA0xW9ilv\n" +
        "PEYWqQIDAQABo2MwYTAfBgNVHSMEGDAWgBRUGNlCcpzEqEu03//g7zT6f2cV3zAP\n" +
        "BgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQU1jeG+pzz\n" +
        "WnHa+0PfJNw9JTrZaoMwDQYJKoZIhvcNAQELBQADggEBABRshh0oJ8Dq87Tgcf3w\n" +
        "ERVZ/uDWKj76UXWQ3uvKMhnNxKN/vs1rCfhovkptn39CXndOb8m6UkvmMjDtJsiO\n" +
        "Oq/DiR6NngCy5yJ5hMuBsdQ2QVvdiqG4Sb+vOaQ2TNQNEHEWC7sB0ztImjxlqDtq\n" +
        "yvof5pd8pHeZJNyDo5cHw1cpoUI9GLz6CK5i0wUlBvsYERIX5aRqxqdtKgBefHFN\n" +
        "S2ChTRB16A5C1h+Lu79KnoeS33lZt1VeebIA7hvaHkqhGGpncutEYgT8QNFFpjM8\n" +
        "yFCjZG+ZuUD/s5hd/xHnPdJzR+RWVKMjjVCTpnni3+NHXo2fh0G8YFhdHQ2F/YFI\n" +
        "ig0=\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias user -ext ku:c=digitalSignature
    //         -ext eku=clientAuth
    private static final String USER =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIDAjCCAeqgAwIBAgIEKgsK2DANBgkqhkiG9w0BAQsFADAOMQwwCgYDVQQDEwNJ\n" +
        "bnQwHhcNMTQwNTA4MTgyNzI3WhcNMTYwNDI3MTgyNzI3WjAPMQ0wCwYDVQQDEwRV\n" +
        "c2VyMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArBFLJZ1liQAEkEyJ\n" +
        "9vAiViRXtDDV+lc62DR4DBj1/Vsw9djVOxmwDKM0+jj83F4Qn9vulr+xp2aZzx4Y\n" +
        "GiJgbtoxPvZmwNv4MPCNX+fgl/+C0nFKOoSYfHb/tK0Jj6u8HOmQqkbSmGJd/yRL\n" +
        "qavapRvhS94tFhiNK7wwLAK9AZ+r7cTEBtUSqfaS7mY7tUsERcZs6z3+rmsuxWw6\n" +
        "/xnNTIVWjdUSbEnjZCdkfZ0cjFONotL6aKoa6YXzohzgA5c3SJZqscEaz4yPkMvl\n" +
        "7bGy7cn6xjfbb5V3oNqo1dtF3Jm8zp0q8Zgvc47l+DAoGIHSpDhPGX+qSWOTwRla\n" +
        "QT6NDwIDAQABo2cwZTAfBgNVHSMEGDAWgBTWN4b6nPNacdr7Q98k3D0lOtlqgzAT\n" +
        "BgNVHSUEDDAKBggrBgEFBQcDAjAOBgNVHQ8BAf8EBAMCB4AwHQYDVR0OBBYEFE/p\n" +
        "UEn8+capIj2+V+7GoxUhdVnWMA0GCSqGSIb3DQEBCwUAA4IBAQBkEyFJ/1CCMoU3\n" +
        "C1sYoq4Wt36z3e4Z2rMjfpFXcagqOQaq+hq+/eG8gDE50tOp30nZF7BxSv0RKnxa\n" +
        "KSkrKcupwgPJOZZWVR6ycV3xWm4QleLuDJh3NdK0o8vvIwLQP47fXURzEXTpGodl\n" +
        "+hGx7jcghsOKftBDCaleywam4jcZ5YPfp5Ayev0pK/Euf0kZgZAhRM33uJVVfEns\n" +
        "UotoGK7SL6hZMCrreVlXygof19p531Ps5xMqu0y2u2xztjVQJ+gPU5zcYbjByUl+\n" +
        "pY+wDPb8XU1EoLl7J5UyayXlk0c3KG/5f+CrVi2HtRfCcKLBf8/MH6OFIpX9O77p\n" +
        "Qq3r+W/l\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias red-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String RED_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC5jCCAc6gAwIBAgIEWK8vRTANBgkqhkiG9w0BAQsFADATMREwDwYDVQQDEwhS\n" +
        "ZWQgUm9vdDAeFw0xNDA1MDgxODI3MTNaFw0xNjA0MjcxODI3MTNaMBMxETAPBgNV\n" +
        "BAMTCFJlZCBSb290MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjpMy\n" +
        "Qh5yq4R3DrwsWaBZoCR+gda4a1PjGpjlQW/Au6R3hwUDAunkQIz/sX1CnLLJ7cks\n" +
        "4m6ba2wjYE3NbVP9D3HozLAv2ErB75/F3evRu5UvzkGLkamyHJBY0xEyFyOaD4MC\n" +
        "hhlo8dDEY++YL8Od+m4i56fYXQlTT94u20I+6hZxeIpJxFSHyouZg06jb+URibi0\n" +
        "e7I3JApWghgcDfgEXZWlCmB8IswYPdd+XWRFDNc4rSWueRP+SeQOFx9x1jM6+skP\n" +
        "DGLpuaChO7cqsUxYnsEx9zhdxQ+v4V3vOye/GigpRaO7WvgPB4g5sYhFlwZ/tp+A\n" +
        "KQebXExXCGOOQUoFEwIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4GA1UdDwEB\n" +
        "/wQEAwIBBjAdBgNVHQ4EFgQUYL8o0Ku7Irg33xzCgA4q1PFz6IQwDQYJKoZIhvcN\n" +
        "AQELBQADggEBAGSVpI7Qmb0oupBCs+tXlJ4/+TX6eBGcHydaT/2hlgEEdT7S7y6O\n" +
        "iMC8C8wlEKTLZ6MJSxjeFTIO62OkTjCsGtk+BHTaE81QL5rxjGFkvtuQrwck8gHg\n" +
        "fAb7daF9ZVLz6B8+cX047xZHx9ZGM/ID+GJg/3fk17WA2BhW1Xkzskby5AWpBDol\n" +
        "i6+zEod0uZhpHiWwVSfHlEA+rnkhW632oVaVNNDkeUhsCxrU0k7nlQx8bG5bmUso\n" +
        "1MaPP1kRKvcy0UGx6q3s8pcrKw0X1S66n7HV+WbQebg83U0MVE1r/J0Cfi0jMS/x\n" +
        "ZUVXs7rjCGFhwfiT/kybKD8adrGHSmLhKs0=\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias orange-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String ORANGE_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC7DCCAdSgAwIBAgIEQPSszTANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDEwtP\n" +
        "cmFuZ2UgUm9vdDAeFw0xNDA1MDgxODI3MTRaFw0xNjA0MjcxODI3MTRaMBYxFDAS\n" +
        "BgNVBAMTC09yYW5nZSBSb290MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
        "AQEAknfh3lKWVQxl2w/eRSza6+0+zeTtMkQFCEFpGJsisDK5XOU1HcJMP4QUteWM\n" +
        "tg1SGO9bjpiKqJ7JVRnzOC3q6erBj2LmwpWW7p12tg6ENOQfsy6tRZLlQWMFGBkT\n" +
        "Tatsg9HwKpr6itvk2wERh18AcIqSjtN94kGTljP1qf9gMd31G5d/HyG6EwMZukJm\n" +
        "4/EFpzh3fVwr/EN1WzrYlsBOA+3Tru/k0p8wP6Bomrx1vAEUqRWSLWxsa7we76jL\n" +
        "H/kMkyWENyjd/A2c5CwscoG+KSx9cifYnSqrUAmpY88KKuZG2Y1+9ablUEwXW4Gh\n" +
        "RYLCGIgxp6NrtFG/eUcDBgtEwQIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4G\n" +
        "A1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUPvRE9j3GPGcc3dNGrVrQoWDb9RMwDQYJ\n" +
        "KoZIhvcNAQELBQADggEBADjPTuHXMbXc2Kn+i+dnBiQCWcjzaox4KWV4MNO7vkvi\n" +
        "ADBk5/vVM+HTzwL+gZNwE96/agcOzwHZ8/Dz4aA3zzmAmQB4bt+pUa0iyGvX6+F5\n" +
        "IH1kd4kBnSBMc76fRcEJnebhrXFgTlps5VELMVcEOn3Q4nt+gVfXmPStTkFjM1/4\n" +
        "fQggsScLpE2TVkk3oS52NIzG/vyBIt3W0gX20hlQobA2vziJDx8xy/+qe5igyp5F\n" +
        "WScwSQE8qeuoDJYJRxpxZ7kq8NiHxfGPw5Hjn518zBz2VKJOsJYmckAMFIdS//kM\n" +
        "NUysH6gFksW/PHy75QkbtD4OFtb2zp01ERuf5OoJavs=\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias yellow-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String YELLOW_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC7DCCAdSgAwIBAgIEfLA7azANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDEwtZ\n" +
        "ZWxsb3cgUm9vdDAeFw0xNDA1MDgxODI3MTZaFw0xNjA0MjcxODI3MTZaMBYxFDAS\n" +
        "BgNVBAMTC1llbGxvdyBSb290MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
        "AQEAgLMOpzIR8LyALox2AvItM42VjFDp1EyHU0faJZCpwVbQPJ2f+8Gr7XuTv1uZ\n" +
        "Ixe6JjcyGNHZG0NuFmMFbK2Y7cg3PdQBjcm+x68iSWzyEUuhytSKSLUt6i+xAg+9\n" +
        "h9UGXlBNarVjWq63tPt8HK/YHxt9Ber0iriF3SEUsgMOzRrLw1mw35SmgidRp19y\n" +
        "yNXlgQfylEAVtYD9IYhxTm/j9NL3rTgqXiKjvNAVjAUrD2I2nK5WQqO2hmQr9x/9\n" +
        "EqgIK03dw0ps7/XL+gpd+zwGZqDr9pbFnko4badiE4AJqPlm6u/Tdc0dSkLu/oXq\n" +
        "Ex4iqtM0TP5+oeDXGZv6EprzKQIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4G\n" +
        "A1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUp0/g/PqT9jDVGKSsBh997Kg9KBIwDQYJ\n" +
        "KoZIhvcNAQELBQADggEBAG4vr5UkWUEA9qNU6wBNg5yySS6KhNVyBDMReyX6qsz6\n" +
        "yUIeGU/UC8LwdB+Tl3S+FZyUlsx+gwh1n0ei7eOV58cCeWmZ3sUWvLTwsY9iBNyt\n" +
        "HkItOCDO+JEjgo7OhEBlzYkD4MkwAjaYnT4tU41BSnlTR4+jK77f/b1oMVzDv2tL\n" +
        "+JAiem04TEoGO97uZ94l6gKwwGO35uejGEUPhFPLtxo+yR2QQqX0S8smG88pCQ82\n" +
        "6XscdvRTjSfkuI3LiqNORS0fGZ3ykxDCkDLZZ1mSg1h2/3xOUEbFQ0phhMrnr2Rl\n" +
        "mWNGYCam2jns4qmMnbzPIwQduvRkz1O1lusbLNFpcdY=\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias green-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String GREEN_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC6jCCAdKgAwIBAgIENWwt8TANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDEwpH\n" +
        "cmVlbiBSb290MB4XDTE0MDUwODE4MjcxOFoXDTE2MDQyNzE4MjcxOFowFTETMBEG\n" +
        "A1UEAxMKR3JlZW4gUm9vdDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
        "AKHvh3FRJghpNtLqIk5UDSGkcA3rtEygRsFa51ucwNQ1x4SXOVnsNHZZW66RuKOn\n" +
        "6wjS8+xctNnMIy1XNXa2nlAswQVe75xX0jfGMB4w0MlaqLK9HrU479WrWmrBjz/P\n" +
        "vvHY8x1CIfTMjOtLO9yxbYQrXsEz6JKxAz6/+ErbkvUjBynezZdJNXgURVz5HmFx\n" +
        "e/SUbSALX+Kx+/+hXggaQdwlrpoDl/Nqm6S1iR5xtdZB1CEauIwFDSWOG1TjR1Hp\n" +
        "8OSGb0AhwwM5FzIxevwgKke6WHFKf5p4lcpiQZqmhgqyFbARUfUjYX3WzQTmrJ/q\n" +
        "87OMIJasvmkNEYkNbrSmI9kCAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\n" +
        "HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFHG7s0KrfUsnl/3/UGYbCrdXTZtcMA0GCSqG\n" +
        "SIb3DQEBCwUAA4IBAQBUe18kbDHeqsxM17ahr30hvjdxMkYLkLcGoSOlSI8sFBu6\n" +
        "jG4JZvvFXw2ZqMQTLSALvsSZ9wkfS8tDCNEHRvCB6aqW4tjp9ddkfe+56WINzTv6\n" +
        "Ibqwg2JGsOzWttpUA5OPLfODbMqOYzT101toF3sKteX0yyiF/SfMTXR5Jv0uo/dp\n" +
        "sFeJtWFfhy/Q0jiEAz945BBoSHCIF7Fs4vcls7gNJxfap66W8lamjzFyMDsnlz+b\n" +
        "sSLWZmvwM+R/RfL1Q3LPCcZWLiP9WSAO4hUoju1E9WeWHHjlPwJJ/iRECL9cnHRt\n" +
        "Z7/kOlNLGxKvpEbY4xqH0zE07UWPCCBlemk/6jlO\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias blue-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String BLUE_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC6DCCAdCgAwIBAgIEX3XC9zANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDEwlC\n" +
        "bHVlIFJvb3QwHhcNMTQwNTA4MTgyNzIwWhcNMTYwNDI3MTgyNzIwWjAUMRIwEAYD\n" +
        "VQQDEwlCbHVlIFJvb3QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCY\n" +
        "pc4r8mfgGGeiYlvHSrEIlp6djHS489P7eqoQRqmS5U/I0lLGNu7QZsY240a9a84S\n" +
        "2t6VpZID5juK8AF2v4psShsqgfj+RjVev8FJE/D5j8B4QZ+HmbLJIl80f+YOPaLG\n" +
        "HX1eNktLx3S2gkIKHVdn7q3o4DdXBO+YdaBA56lL4l+dWFtto65+5Sjy4yfyvWJz\n" +
        "MylXjP/wiC0T3C0NcQX3AIu2tjY2u9lrVbem2rIi0kPFIoYvstKiqXMc/sRf2CfO\n" +
        "it5k629HsbvdACVRZFxU3Lz25oP4HGz1kq1cpiIS+W3gQQmCKu9XqzpNRThG0SEL\n" +
        "jaH9E4pZDnZiRCr+Mxm1AgMBAAGjQjBAMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0P\n" +
        "AQH/BAQDAgEGMB0GA1UdDgQWBBS5ebHO4iamr3n0+KtxJYAitg9QcTANBgkqhkiG\n" +
        "9w0BAQsFAAOCAQEAGjh/UzERw+skEK4zM1zfL1RsZnUlFu7mTbOBGgccewHWC+MM\n" +
        "AQbLo0m4NTEbRoW6fFcAESgE61ZZBLkmhcjXBunNJq6O1hMDpppYA806eG6GcZmK\n" +
        "rLOZljxx4D1YC17vMEVsMF9XgIj5dLWceJjotZzNxe+miwXLEkxaGIMe/n2VtCoR\n" +
        "BSrGrAeCsFZ7G2NRWUxUEVJrhLnVZJDt6fHd43BCVnV191PyF5TuB08nijyCoJoS\n" +
        "/WJkYHxx3vUUfDE5E4UE+iY80BHnAPxiNKwO3XsWjeqbJ8PS+5AvShdG5QdFBhKe\n" +
        "/tJTZLs0UEubKdaWd5ZgsXP3913bJm/mBo+eJA==\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias indigo-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String INDIGO_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC7DCCAdSgAwIBAgIESdUmczANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDEwtJ\n" +
        "bmRpZ28gUm9vdDAeFw0xNDA1MDgxODI3MjFaFw0xNjA0MjcxODI3MjFaMBYxFDAS\n" +
        "BgNVBAMTC0luZGlnbyBSb290MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
        "AQEA2opDa3wDxQrX6GjffbDGtXyvKw0/vhZTeus4cxdZIYF3CWkGbeurDIhPUKRO\n" +
        "Azact0SECJuMXVxkB5vQKBmHRByNh8ugFfzXWi7/rteXTNjLNKnzVr8plbMvzwM7\n" +
        "zjIgm0mTRbwv6gZmUbgfmO9FCB8zlV4hYbYbFTJn7GlVPpqZkZNNMTyJkOPxMzXD\n" +
        "yaToxyR0uY3cMv9pmks3GxU2XoGTFuqptbL9XFSpwrm5BRfWuJDP1t8moLHQZ5iu\n" +
        "tkCz6MVYcrhTlV/UY0PSGcmUvAu83sNBfIGjme0RIiERy02gLJnSZ/M9r1ukCUJE\n" +
        "Z6At+9TsNCYNPgW5vcjNLO63/wIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4G\n" +
        "A1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQU8eJ+kMmanqF+IcAQTjxSMv+VR7kwDQYJ\n" +
        "KoZIhvcNAQELBQADggEBAGRB4C5qYXXJJnEGzJZf8S974SaeLmEvHlmaQPOuCxME\n" +
        "tCeBoWQqD9qTDVy39izzjA4uE/fCMVCkyr1QL+588dtMI8jJfbzx+TxnlDWlJcMM\n" +
        "5J8EJPNEy7eR6qqpFncvjmbXzf16XfzL9qSXwHYCvpo25nEEH801y2njJE2gGzZT\n" +
        "raYRFuwzsZLiSV5TyO5MbRXiZLebDXfE/wXukor87pjGpx1/kevjH/g66OpaIBzu\n" +
        "IfLePEOekTKXHF1zL89uYHwpUVCzfhO5hNQlSsTCuBkBifSTYm4ixoATi/C2kqze\n" +
        "WHUK179u1+7v6xRONLQxe1JDftdlHHVg7DSeTY59euo=\n" +
        "-----END CERTIFICATE-----\n";

    // keytool <COMMON-OPTS> -alias violet-ta-key -ext bc:critical=ca:true
    //         -ext ku:critical=keyCertSign,cRLSign
    private static final String VIOLET_ROOT =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIC7DCCAdSgAwIBAgIEXwgmLDANBgkqhkiG9w0BAQsFADAWMRQwEgYDVQQDEwtW\n" +
        "aW9sZXQgUm9vdDAeFw0xNDA1MDgxODI3MjNaFw0xNjA0MjcxODI3MjNaMBYxFDAS\n" +
        "BgNVBAMTC1Zpb2xldCBSb290MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC\n" +
        "AQEAvElr11MoHUNfnx6nBy4NSXFFzA68g57ohTt+sspEn3wzuPErugMypReHLhtH\n" +
        "CTrs45T0KU8P4Gi5QHnsBs8yC3QCHNPugo2A4zP+ciTqp+1gRNxQ9wzKSzCoseJg\n" +
        "RMQflGFzuEe7gWwYfrsDfD1sJCubfBtBUFCSYf1ZSZbdEMrc3RPtC35Ge+3XRxFZ\n" +
        "KdzH8l7gQTtgAmRQmK+i4jwzSHV/Iu2yiRdhjqIZUezf3pYFfJlmfAY5ruQBKkc+\n" +
        "KRgdmKanpLbmAo/+3q6snt8V09CoQ+6Cz+P9P0yOxiiwr/6jg9WtHA3ujvtf3dGj\n" +
        "EeB8SmzXHFZErQIn+QbrJ3/izwIDAQABo0IwQDAPBgNVHRMBAf8EBTADAQH/MA4G\n" +
        "A1UdDwEB/wQEAwIBBjAdBgNVHQ4EFgQUlxlHI8JTrX86r4ezgDLICo6rApowDQYJ\n" +
        "KoZIhvcNAQELBQADggEBALnfWZx6LC9vDMI8vBH/vbyk2ZQtiQFRt3kbwKtlrw65\n" +
        "/bqeGXcQ1Lh9gDzp+uGYSfuDNvtJO6xmfny0W5j5WQVJxs+iWyoJfYxeG0kBZut+\n" +
        "hbxJPlehBwhveuznZbeTN3RXeBi8MSxnBD/WC1e2rnfnrxoLfYZ1BSUP8LaIzC32\n" +
        "vd6WCgnJRXputlGvnOoAT1WduWonhd7lCoqbtZksw7o0smuAn2mSnod8j948rzzt\n" +
        "uDQVao/3tCyoX4NSom2hWooPltk5FTdF9cZKfbaU5TPV+U30RN7/UWY/dCvL1fMq\n" +
        "1kvtJbkh+UMHvszHOxlgjk+3J76Wx0PFjNaIfbj2bmk=\n" +
        "-----END CERTIFICATE-----\n";

    public static final String[] rootArrayPEM = { RED_ROOT, ORANGE_ROOT,
        YELLOW_ROOT, GREEN_ROOT, BLUE_ROOT, INDIGO_ROOT, VIOLET_ROOT, ROOT };

    /**
     * @param args {cacerts keystore, cert chain}
     */
    public static void main(String[] args) throws Exception {
        Set<X509Certificate> trustedCertSet = new HashSet<>();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        for (String pemCert : rootArrayPEM) {
            trustedCertSet.add(makeCertFromPEM(pemCert));
        }

        testCtorByCollection(trustedCertSet);

        testCtorByPKIXBuilderParams(trustedCertSet);
    }

    public static X509Certificate makeCertFromPEM(String pemCert)
            throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream is = new ByteArrayInputStream(pemCert.getBytes());

        return ((X509Certificate)cf.generateCertificate(is));
    }

    public static void showValidatedChain(Validator v, X509Certificate[] chain,
            Set<X509Certificate> otherCerts) throws Exception {
        for (X509Certificate cert : v.validate(chain, otherCerts)) {
            System.out.println("\tSubj: " + cert.getSubjectX500Principal() +
                    " Iss: " + cert.getIssuerX500Principal());
        }
    }

    public static Set<TrustAnchor> makeTrustAnchorSet(
            Set<X509Certificate> certSet) throws Exception {
        Set<TrustAnchor> anchors = new HashSet<>();

        for (Certificate cert : certSet) {
            anchors.add(new TrustAnchor((X509Certificate)cert, null));
        }

        return anchors;
    }

    public static void testCtorByCollection(Set<X509Certificate> certSet)
            throws Exception {
        Validator valOK;
        Validator valNoGood;
        X509Certificate[] chain = new X509Certificate[1];
        Set<X509Certificate> intermeds = new HashSet<>();

        // Case 1: Make a PKIXValidator with valid arguments
        // Expected result: Well-formed PKIXValidator
        System.out.println("Constructor test 1: Valid inputs");
        valOK = Validator.getInstance(Validator.TYPE_PKIX,
                Validator.VAR_GENERIC, certSet);

        // Convert our user cert from PEM format, then do the same for
        // its intermediate signer and add that as a helper for path building
        chain[0] = makeCertFromPEM(USER);
        intermeds.add(makeCertFromPEM(INTERMED));
        PKIXBuilderParameters pbParams = ((PKIXValidator)valOK).getParameters();
        pbParams.setDate(new Date(1426399200000L)); // 03-15-2014 6:00:00 GMT

        // See if we can build a trusted path to a root to make sure
        // everything still works as expected.
        showValidatedChain(valOK, chain, intermeds);

        // Case 2: Make a PKIXValidator with null anchor list.
        // Expected result: throw NullPointerException
        System.out.println("Constructor test 2: null trustedCerts");
        try {
            valNoGood = Validator.getInstance(Validator.TYPE_PKIX,
                    Validator.VAR_GENERIC, (Collection<X509Certificate>)null);
            // Throw something non Runtime-related to indicate we shouldn't
            // have succeeded on construction.
            throw new IOException(
                    "Constructor did not throw NullPointerException");
        } catch (NullPointerException npe) {
            System.out.println("\tCaught Exception (" + npe.toString() +
                    ") [PASS])");
        }

        // Case 3: Try putting a null reference into a populated TA List
        // Expected result: throw NullPointerException
        System.out.println("Constructor test 3: null in trustedCerts list");
        try {
            certSet.add(null);
            valNoGood = Validator.getInstance(Validator.TYPE_PKIX,
                    Validator.VAR_GENERIC, certSet);
            // Throw something non Runtime-related to indicate we shouldn't
            // have succeeded on construction.
            throw new IOException("Constructor did not throw RuntimeException");
        } catch (NullPointerException npe) {
            System.out.println("\tCaught Exception (" + npe.toString() +
                    ") [PASS])");
        } finally {
            // Return the certSet list to its original state
            certSet.remove(null);
        }

        // Case 4: Provide an empty List as the X509Certificate collection
        // Expected result: throw RuntimeException
        System.out.println("Constructor test 4: empty trustedCerts list");
        try {
            valNoGood = Validator.getInstance(Validator.TYPE_PKIX,
                    Validator.VAR_GENERIC, new ArrayList<X509Certificate>());
            // Throw something non Runtime-related to indicate we shouldn't
            // have succeeded on construction.
            throw new IOException("Constructor did not throw RuntimeException");
        } catch (RuntimeException re) {
            System.out.println("\tCaught RuntimeException (" + re.toString() +
                    ") [PASS])");
        }

        // Case 5: Provide an invalid variant
        // Expected result: successful construction.
        // Note: subsequent calls to validate may throw CertificateException
        // if the submitted chain has a length > 1.
        System.out.println("Constructor test 5: Unsupported variant");
        valNoGood = Validator.getInstance(Validator.TYPE_PKIX,
                "BogusVariant", certSet);
        System.out.println("\tSuccessful construction [PASS]");
    }

    public static void testCtorByPKIXBuilderParams(Set<X509Certificate> certSet)
            throws Exception {
        Set<TrustAnchor> taSet = makeTrustAnchorSet(certSet);
        Validator valOK;
        Validator valNoGood;
        X509Certificate[] chain = new X509Certificate[1];
        Set<X509Certificate> intermeds = new HashSet<>();

        // Case 6: Make a PKIXValidator with valid arguments
        // Expected result: Well-formed PKIXValidator object
        System.out.println("Constructor test 6: Valid inputs");

        // Set up the PKIXBuilderParameters
        X509CertSelector sel = new X509CertSelector();
        sel.setSubject("CN=User");
        PKIXBuilderParameters pbParams = new PKIXBuilderParameters(taSet, sel);
        pbParams.setRevocationEnabled(false);
        pbParams.setDate(new Date(1426399200000L)); // 03-15-2014 6:00:00 GMT

        valOK = Validator.getInstance(Validator.TYPE_PKIX,
                Validator.VAR_GENERIC, pbParams);

        // Convert our user cert from PEM format, then do the same for
        // its intermediate signer and add that as a helper for path building
        chain[0] = makeCertFromPEM(USER);
        intermeds.add(makeCertFromPEM(INTERMED));

        showValidatedChain(valOK, chain, intermeds);

        // Case 7: Make a PKIXValidator but provide a null PKIXBuilderParameters
        // Expected result: throw NullPointerException
        System.out.println("Constructor test 7: null params");
        try {
            valNoGood = Validator.getInstance(Validator.TYPE_PKIX,
                    Validator.VAR_GENERIC, (PKIXBuilderParameters)null);
            // Throw something non Runtime-related to indicate we shouldn't
            // have succeeded on construction.
            throw new IOException(
                    "Constructor did not throw NullPointerException");
        } catch (NullPointerException npe) {
            System.out.println("\tCaught RuntimeException (" + npe.toString() +
                    ") [PASS])");
        }
    }
}
