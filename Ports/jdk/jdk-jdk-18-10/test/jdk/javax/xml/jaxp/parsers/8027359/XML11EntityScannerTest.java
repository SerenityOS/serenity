/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8027359
 * @summary test that the XML11EntityScanner refreshes cache when it loads new data
 * @run main XML11EntityScannerTest
 */

import java.io.*;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;


/**
 * XML11EntityScanner functions similarly as XMLEntityScanner in handling data
 * cache
 */
public class XML11EntityScannerTest {
    static final String rawXML =
            "<?xml version=\"1.1\" encoding=\"UTF-8\" standalone=\"no\"?>"
            + "<WebOfTrustRC2 Version=\"4004\">"
            + "<Identity Name=\"maggot\" PublishesTrustList=\"true\" Version=\"1\">"
            + "<Context Name=\"Introduction\"/>"
            + "<Context Name=\"FreetalkRC2\"/>"
            + "<Property Name=\"IntroductionPuzzleCount\" Value=\"10\"/>"
            + "<TrustList>"
            + "<Trust Comment=\"\" Identity=\"USK@fdZ2In5mnLVG6RTc5hq9P~M1EG0WuH-itZ7mnQx2iuM,aUG-57VqxQLhQ4N1uNmH9kSI2syEfVFrVOIPAKTY2Yg,AQACAAE/WebOfTrustRC2/5\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@YwwRkHSo-xU8CvSFcLqlU2FFsQ3ztjr0X~xPXkX-klY,poB3tdcXrBU9naI0pyNVYp~zQmHaFkRRTj8xB8tuiPc,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@lsd~~79wrGvfb99FnAEY4VhJg2b5KFUloCOUff-Q2fk,71eV-F580euOtaCgim69Yw~2Rjh43DT49sl6zNamjk0,AQACAAE/WebOfTrustRC2/47\" Value=\"100\"/>"
            + "<Trust Comment=\"Automatically assigned trust to a seed identity.\" Identity=\"USK@xyzElfFQnwBb4ZuSEh1aSNsbRjEGCTa-2rcjeW58A4E,TiYrXSCcoGETPf0TWLNthaimJEP1PW7nJ2tYXKxdC4s,AQACAAE/WebOfTrustRC2/456\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@pTtYoCsMO-~~2Grqya6Y7cf7nIU3VlL4168-c6EIwA8,QabBgrH1LYKKyBROnWz1r6iI8N7WFTt-mKD-0Qxsw1w,AQACAAE/WebOfTrustRC2/133\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@m~lDSvnetPNy77wsoxDZDUf7OkgX6ZAd7ob9orm3J4Y,SEX20g148KGJg3bsxvXNPNbUsVs1yQ5LfVUOGMh~1Q0,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@ss5yQit~bQL5easM68d4sImnPAxHNUbi99XtDMhpzgQ,CnpuD8dO29KvpkQyxtz1llAxHCB7yXfqrQLNRIELZow,AQACAAE/WebOfTrustRC2/2\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@krPcyGts~~wZTbABTAJ59nSplmum2~EkSD5IzinrFko,7YEbm-YEx1LFoZVHtGmIa5q0KbEjw48Rgwx2NqwrH~o,AQACAAE/WebOfTrustRC2/15\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Usl-yNWc3VtuQWQ9srowZaWyfB6MiD9fzM5WexrlKE8,qMS83aGFsN~aFGajUmGrnbXvRIYZMd8N8IjnGmEvi-s,AQACAAE/WebOfTrustRC2/68\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@St0yKRdQJ3Lq17yoVt9h08bMfhqRhH1vtrcVVInoZVU,BaYM72qM3CYO1yzfVyO1UDUobL56CMbt9EQt3sEXabA,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@P8v4ZHUBPYGylYC-KHeWmeu5ZvB6RPYGgKcI5639Wz8,OM9PvjwMlt4L6jKRhqpvyblwpVMYumUgYFrAu3NxY~o,AQACAAE/WebOfTrustRC2/54\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@u2vn3Lh6Kte2-TgBSNKorbsKkuAt34ckoLmgx0ndXO0,4~q8q~3wIHjX9DT0yCNfQmr9oxmYrDZoQVLOdNg~yk0,AQACAAE/WebOfTrustRC2/2\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@MotrIucaZk37pJNS~HHpW5Vea3q0EZpfENPNGSjWh9s,RkFyDjl6-l7V-xYMWtGypmDYk-VehAU1LXmNYdIlHJs,AQACAAE/WebOfTrustRC2/108\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@KXAWN8DJxJ48XzZu7IBBpZ7SFc4n-OXqu4HhQC0V9Ww,DyklyACbgDCZyFpq-LeNqmuve7KWv-WDvJicd37ycn4,AQACAAE/WebOfTrustRC2/11\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@D6BZ1sSEmDMKNMPUN9I~7AalkkXockAdtbxONEN315w,0LiGUSSv4Ln4O7Xe4GQjpMEflNN7okKAH42Vlpv8d8s,AQACAAE/WebOfTrustRC2/56\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@sDehmSJoiaKYT455GIGHIVNKMs7KmiiwXXcCIcDhZ3o,upypWy-ze9Cz7WiCnbbJmoZOh7Xtveitftc5bE~p0Ug,AQACAAE/WebOfTrustRC2/6\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@GsTGXzIm9BTM5~KSZJknfTcPpJtlRs62aJtkNp05T5A,wuQcDjsdPQD9Fa21zWGB5GiiDlmf56vI9Niu5jl8eiM,AQACAAE/WebOfTrustRC2/31\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@eLFdT9VWP60iPNf8a3AkZrzgkcgje2n3Ca1yS8Tc5HU,5D0PISp19VkgzD4VSrRFTmo2CCvRoIuxlQzgMZBmj74,AQACAAE/WebOfTrustRC2/62\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@rz12UCXn-lG53i-6JswD98Kk9Zp0kt05gqIMNYc~9d0,42hoYZ5f~1fPuyvTOVYGJ~28MgYBSfoyzceR8-u1Z2I,AQACAAE/WebOfTrustRC2/126\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@5CYp3t28N5ZbPss9XhOkwUjD~T65q6-nc4aGwbV~-O0,CBfwMjYBiqKunzj3k6Ofpo9pyQsVFPz0OUWVzfd5a0c,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@dSSrRL9BY7JmTQD0dCpcIgVaLFF7pqU8EJzVUp1BYPA,EkTPkLrhEBQLPq-dRVPB1f2CAVdFGbXbUBlIww6re60,AQACAAE/WebOfTrustRC2/5\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@cw2rrCAcYTBjlO-I8DfEj-txttebZMG2LwuPGJV3Qlg,5ZcXLJW8G-R2SFqg4TEQX7IMs~e3Q4DjxUXGuHr9fxA,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@7zpRmhkkd5XcAUY6yUwp-53t7to3UN96w98~KQEwha0,30VpXev32s2mhmM5aBRJ---I0OlYbbN6~18j8rJ6qMU,AQACAAE/WebOfTrustRC2/8\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@YmCWYwlaSeE8o~8GawGYWeuPwsgVQtlBwlBkhGQFX04,H3DthY0MZXTe4rL0vsUOOMQDaj6UB9wK59yEwG8Q6No,AQACAAE/WebOfTrustRC2/13\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@aO9svsQEWmib~UyF1ihRUHA8i6uZprGy-tIS-Od9MMk,kwaMemPMVp-jcIRgGLAdF6PZimNE2cZFbvM6ShXAuZw,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@6J7FWPHwiCElTRXAO77tMGYAiLxerNF~5olAG0alQxQ,cz72iTz9FT~H57TgroVFv1eZlN5Ia5dhCtRa8bLh1KQ,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@kukYpNG56TNUnQzQe1RZvSYoqQ5B9lcoMZyDlh7xfO4,DNybejZVcWBAaByMRYYLZlgzUjQg28V8j6Zu1CtBc~g,AQACAAE/WebOfTrustRC2/27\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@a-tin0kMl1I~8xn5lkQDqYZRExKLzJITrxcNsr4T~fY,0VmnI67gAzIpdXlZFq~hYD8ikR5IEAg5QTwQv5Ifv5M,AQACAAE/WebOfTrustRC2/24\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@g~4XXw0hjp9TLocYZzunYWX6Don2AQhG-zplmzoCmY0,lyNjClSjRCrBZXVcmPKEAfvH01ySPgv1NWqL8wd11L8,AQACAAE/WebOfTrustRC2/115\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@UXgNMPe1YbVpXHOBXgMlcm9XcGY0G3gUtu43IO-YLLA,7EHSp20GHupg0rWrGPRY1g0TFJcRs-kubVtAcQRMxAc,AQACAAE/WebOfTrustRC2/69\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@lHrF2trsC9gyVeT2zpoL6IlZMz0aKlhN4xa2Ig70rmA,niASdKFC3nDfW6KMvzcv6VmRoHakE7GQAFpfz~2-v3Y,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@hp7IFNYSA97cHNAnHcreTJjQE5fF5sbsFlvbcZXaoxQ,O5h6cAcl5MvxuE2-xOkuvUP4JkT59NQNtaSmtuAS2Y8,AQACAAE/WebOfTrustRC2/16\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@QKPxALYrv6UyAGJs7tor8YlcikFzmy2msZ~42JMT~80,vx01piijkj0o3K8gzNaAOIIZ7NAtQVvPBxlRKtc4iP0,AQACAAE/WebOfTrustRC2/35\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@NmNfUNIr2WwIb9Ej1038Pk0M5gS1M0eHKvxdWqRUfTg,JUJYTsWuNQPOHK039Rczp6oPZDbfPdbO62gSaCuBXFQ,AQACAAE/WebOfTrustRC2/69\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@ONB7kRFo7mI0O3~QLRWlTbo1oB91XmGGS7KtSzz4XVI,yGiqMWRwR~i6ffAXOTBvrngHGC6nYOETUXj4L6Izj64,AQACAAE/WebOfTrustRC2/122\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@k-i0PmxoxO6Mahx8s850Rd7TEe0r4cnEohqC66aMDKw,IlTVYmQ9OSHjiu5pOLq4t-8r50SsVPNMlXn56zfpfDI,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@GtzvV-VHMGQILkpZ3O4CWBTWlhB423UUwpIlzRDgBLs,HUxBDT-Hhy6GqjBV24NRMjlK-o76YGMEgO~ZS6yighU,AQACAAE/WebOfTrustRC2/6\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Ws0yzkcOYw6ax~kCtnzyX4MgLBHiQd5a6u9FMUmqLG4,zqNjNNGLz7HE4pdSPTovX2AwzGQWJ3-LI7YZt7aq3gg,AQACAAE/WebOfTrustRC2/30\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@6brkdrKlglWHnqMjWG6wrdLMWGPooc~7wQ7ID-PIsJk,X3RRBIdOQ5zthpkk8FjLL33LyoVVI4csJ1~g~sZ1msc,AQACAAE/WebOfTrustRC2/63\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@4DO0Yp1a3RZeAUAjeoPVu~GqpKhaX8RYqe~mwcWgjsM,klsqky963KI6uG3JqE1crihSeBbKBgkJHkIZ2xkWJ4A,AQACAAE/WebOfTrustRC2/36\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@axtoU6zHIkx3bicWK-hLNOj0br4xi0HW1qZ8H6CEv0U,SuPIncAHyYXmR9jQkTFUJd-QgGm0g9lW5ESUjzFOl~Q,AQACAAE/WebOfTrustRC2/8\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@BYsosP8TA22rSz1uA0KGbp6OVFZXhdgAlpH4R4hX9zE,o~qV5IzMR3m7ZOnnG5FPnDqpdffEpu7yOM7VEU9j6lQ,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@EilKmVin5cVL7b4FoEQ7ZoHS932O8OI880Qkd~tmzWs,8WK34lo95u~b60GLVczYU6EiRpY0LH7130~ASP9F6dU,AQACAAE/WebOfTrustRC2/75\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@xDYiCplSPLvFGC2dQjAC6eeaYVyQMtV-HmkpuKIJPgQ,CYKZcPacSNfB67IK10xlq7~bAqR-aOnZIA~yhHs2Hj0,AQACAAE/WebOfTrustRC2/71\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@DfCUA1sEJgiGzijEO0BWgeGjjHi28GGgv76H4rLujp8,1lnaOtPiXmvhpRZkBamZNF47uETNKIBgjSzElxcLhZc,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@F45FWNz9rs6TmafG~6n5Bg3Sr69YCHY9v01KTyK17lw,ikIvUS079Qw3aQknvdM8yKgr0XwjcqHgW0pWLu-1osA,AQACAAE/WebOfTrustRC2/13\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@VTDWbj9C5FUEXZBQRXmSsV1aEdOfsB9QHKzZ7-CnCh4,aDjtAVMuGPpCmw1rnCAj5myEnq9HYZeIzrfrhJO2JNw,AQACAAE/WebOfTrustRC2/52\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@dhYMUELq~HtJOweiV2JkGcoFS6hulHd9O~7a9LpGIxM,M-H3ySL8BdCctDUvwvbFNwHUyO1zghSNyotLqkKowi8,AQACAAE/WebOfTrustRC2/20\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@oRpTFVvCqp8qFWiZyCh2QhQ85eh3eP8C8G4YTFhm~rw,kyyeUPuksdt7omGFYFxoWPz3lu6e0q~G1HBx8wFztFc,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@RINL4YHPkTUGBWSoohAoO7Knzk89XSnqnQ6pUZkRY~w,OvIyTUUf4T2Tm46-em~A7zn1zBksIwe-hJP3KKdCe6s,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@sEZ-eWgUcQSj-To7lClNN6QNoKuvt0Cz3iTGKv63wQo,krEwvob7MhJ-pGvIk-RM8pVDPFSuXYTZw58oc2Lc49o,AQACAAE/WebOfTrustRC2/1\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@gNTVpdblFLSKMzUBEQM8YTfrO2fQQNIZeV7yJRTqYTQ,EKJhCmbQ5hpMU3cd-J91uneBF7CxTLPqffPs6DxoSMs,AQACAAE/WebOfTrustRC2/21\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@AV8Ubs8pbsMQ1F028a~pSJqtatznLjvhnwfSAtm1QKg,lUHPp~mgzV-pVoG9lYpceL6oOUtFRpvyQjGQdfx1GjY,AQACAAE/WebOfTrustRC2/41\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@xb3QGWdoUpeX9Fn1ZKeDwGN884c2XAMTpYLM9z4OIKo,aWNEDvjhhacFKAjiYJLaUnK1e7dQ9sCk-cnqkGZd7F8,AQACAAE/WebOfTrustRC2/9\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@MrxyFFM~n-kZ4kYvOOZodsvAA2vwp2XtAQJCrkEEU6s,tSHvx3u7uJN8ciaTqBIzt3lLmonM9mj6I7pz88MtBXc,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@-tpkd0enlFMTnjANRjzMKyfE3uyXU-WDX8VUqk3MXEg,J6h7edIdQCsiuc53qahzMzxsyNayXL~9IR0x5QoJVxk,AQACAAE/WebOfTrustRC2/15\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@gBQsXbnA9HX6wQB8x7DIEJT6e7lOxq5jGF353Po~p90,HjdyCLfG9r-zSBN6-AXwA1he9blIO2WEN9u7dMQ7H1w,AQACAAE/WebOfTrustRC2/13\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@ye~rQ4m~pu2Iu3O2TH-GOLBbSeKoQ~QR~vC6tJbKmDg,YSuI-J1nKZjGB2zmIa9Bh2Wtud~jzYBuR7OVhXYh7qM,AQACAAE/WebOfTrustRC2/99\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@HwgX8mU9A7bd2sCsM9And7fotpMGqfba9LzxZtOHHOU,10nYjjfKDH5RB3YvCCi5bpq4GPR-myd8ea7n8DRvx~c,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@qQArPJZuODDeM6C2ndbSczsFLWmLDm2SoaE-9F1hwQU,E3x6TbIvBj~6D9GrMUWSYgnNkLRg85BXXy5~mncoNEI,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@4hm9fo4IN0akORyJECttbEGZ~rPCPQ9KKAAH2AWMy8E,klvTyVp-GJznJq8Sln4W2GUfV6tVX2TI2sPCGBFU4tM,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@x7SDH5rL2-tGOaOKFFNMRrylCq94OfiZTWt4t3nwRHk,~E-kX9VqlANo4MeccvNlXDIjJ5xTWTEDNW-Qk04Ke6U,AQACAAE/WebOfTrustRC2/17\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@A9RAAKmky5yroigSupz6Bvrmc4q5FN1SXSrSXa6oRUg,s3RfurouqPnKmlGRkERao6jOJv9kcrVBBKAHqZVuQH4,AQACAAE/WebOfTrustRC2/1\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@yEstdNIW54-3~YA7QnYodpyWgPZStskrN9WXLHujUrE,j4PciAtpUHOjR2wmaaCKwiryJAi~exjETJ13UPGJLTg,AQACAAE/WebOfTrustRC2/58\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@NZSwPe8RJQOTSQ-~cAuxy97ieuX9reSft0cyD0vfbJg,99y8KOdRJmVLCWqryDzB8NdBrQGY7V9JDFtgCQBuGsU,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Nq6iEvHP0B2VCszLCenQrfyJmkD0vas~00-9MoWgcTA,6dI2kB1z9mtjwITkhc9~V86QE5gM9CJBMjdpvRzKAoY,AQACAAE/WebOfTrustRC2/29\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@ZaNXnLCT7OQrwhp4I9V-rI1ZAV9WzegFj0HSaEJQ-Z4,i23K-dQupDenVE8O3xGv92vVanfDeyjLYWescevMbGg,AQACAAE/WebOfTrustRC2/14\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@53e9r9zEkJ0TRW~Tq8XIWjZ3WkL7Jv5Ez8LDhdwwzwM,7a0vL0OmWkHy3gC5Zs~MxB4k0QppwK4igrH6iuwls68,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@W1c~RYRC09xIHiHIucfV~Rj4J8uKAGrUeHmrH-q-U1c,niOrI75WMQ-Dtl9luIbKBmnvf6chkQEKvUvoKvSjhxI,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@G6zv74PaPcVZcf78lWp-e1TfrCSNZa1ZaPvmjm7DVzk,f4Ft3sLQ6cUEwpumpTMt5N17UORfZoXjGfmke5PJbdc,AQACAAE/WebOfTrustRC2/5\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@m5ILsPVAlcOY5D91J2iQu~PIntZb3L-B1VkONjEZs2k,592dl39JXIF1kpC4OMkw7ELOhdPrn~WXBsZln20pM7Q,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@iK69tf7gzQ53oZV60rF6ZSwwCfADMRsJeG2YmODdmJc,5czI5ZmWbWLDr2L9JdDmkt7qrr8fs55VDt3tXnrFw0E,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@bloE1LJ~qzSYUkU2nt7sB9kq060D4HTQC66pk5Q8NpA,DOOASUnp0kj6tOdhZJ-h5Tk7Ka50FSrUgsH7tCG1usU,AQACAAE/WebOfTrustRC2/58\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@0j-H~zKeP9mh6LEJUl4HSJYC-lQWstYRJeC~5E2F5~c,VbjnSixETRzKp80jYYXD-bqsTUWzxwcYmI9ZSWsBrik,AQACAAE/WebOfTrustRC2/13\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@~4ZW0eji3~hYAakiCI056soETGPf9O94GtoIpE2NGEg,GdtKsS~WeDlBS~OL1egxqZ4pr-iPXHjT2zcy8pjvEK4,AQACAAE/WebOfTrustRC2/88\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@YommlOi4fTYx9axhnl9iAi9BNHRvnM5XWjl21i0563k,5FswxS7hPf2erR8KkrBobG8R9bekaakeY1tM8DDNsjI,AQACAAE/WebOfTrustRC2/2\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@-ucM1bn8sICGRAemM8wZ~BVHgX3ZPUoutjZY64mBIcQ,Ko~kC54wZ-joCpfdc67Vds8LkAIxvTJGpMtfQSl4mAA,AQACAAE/WebOfTrustRC2/2\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@VwcZEhjtMuFcYIa8r0yksTs~FvMWB7swEz6tK67Vmsw,0blSLTQDms-WWbw1IBuKIoR2ZvSHpI21lMFeByQPuII,AQACAAE/WebOfTrustRC2/56\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@HAoXk-dW6~j5eLdAZCEjyKTVC974wkdl-4QqrPcbyzE,NlS05DkGK8QzL~EVUOfb~Jl1EZoVJTQBQBzA6N2qMn4,AQACAAE/WebOfTrustRC2/7\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@2PK-12eJFcVziT6eMpiUpOpTlmDNAoVOKZvA~8s7VSg,EaHdnKfKoWQDclX1mwcQeGr5jo1ijxyExYqL7Vsf-o8,AQACAAE/WebOfTrustRC2/33\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@8D3L5TDTyp5jnbT4imUWK12QC-B0iaSxFPOstMX2URg,ZtSShNkEVV8whaGQtIxiJ3FBroihw8YDyZrWvqncs0s,AQACAAE/WebOfTrustRC2/12\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@HKv7Hp94hFz8CKlINXCImq~XIBL9VfsPSgqfR0QIZFQ,vFM45qVvCajf3iqZm~ykZYwqM0KhaUSkU3RGaxJzHLI,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@NJu4xYtC3cgZo8s2xqmap3eh1dy60tFQFoxU8aeSnq8,fp~WLcrSSUKnr9wDDz-geb8FDwADCMCA9fjKt04Cyg8,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@IazjsmIK8VnIjDkq3-ecMEKHmi0dBT1QsNBq2mp2Fuo,uu24sWwNW~tjszdw19Mz18NBGx1MSADi03BHZnttwFo,AQACAAE/WebOfTrustRC2/8\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@IfxF-y7PNV97WUiHLqlIwXQyfVsHjI-Am4unZ0AK-Qk,Xm78gNC236A3wZ1RWBnft4oBGHU1R88Yt9AlphmtJPo,AQACAAE/WebOfTrustRC2/36\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@i1YvNnlMkd0i~0SiXiDHlGaxbtYd8ZqEUij~VzxyXmM,rxi19mph9KtHA6~gVGJ1rZ6kytff~kJDm6~NrA4YQaI,AQACAAE/WebOfTrustRC2/9\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@nXf5sbMSDqE~OkCQ~8JaMgPbj2LQJcuouvO56ADo28s,uEMqVaBzpA06TaC8A0cqKuv~lSfqZBiijasA3nlQ0Fc,AQACAAE/WebOfTrustRC2/8\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Y8fLVylNs~pB6sdA5Dl1l6T~hXQrLNZLMgfDl5-E5Z0,n0zdN5XUa-D5Puf0L9wu~xQQmU~A5TpfqJ4RFHJLjLs,AQACAAE/WebOfTrustRC2/1\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@F0ixRao2hQlvTQDkN6rPcwLTG2nA6x0r8RrTQ0443IQ,LxN-gYvq4eWRuhjAgejmgNqYJdIw0q8IJ2XNwwVHhmQ,AQACAAE/WebOfTrustRC2/12\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Pgk~nJzSVNAjFHh-qTovvm6muY08igdOCEWuyUrbFvk,NlwUDwfQlbXRjIaFAVljD5E1loXi31Xq6dG8YC7JOR0,AQACAAE/WebOfTrustRC2/5\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@m9gYemmVNXe32Ao6jeUgW8ttgGlMSIAW1608YZ19olE,LnEWPRzsPlPfq2~gIYQ5SVV-V2lthAx254dht9EZgyU,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Pwiulr-xlPJelS5CO4~do78zZHTL5TNIChF5hpz8nkY,z77V~KhcKZRWLSWtVzaWO-dt~TzCCJFCHUDhHXI715M,AQACAAE/WebOfTrustRC2/8\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@OH3ZkrLAkMJbQTjVW3e9ioH04F7jpUVNMekiQAAvRjs,J6qOC05GoqRgST93M1V90HWxq9VK4PCKIpltkL8GAhg,AQACAAE/WebOfTrustRC2/5\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@nirNdzBgVyqMpnuT5UrmX~TU354qDIY7XAp04Kutysg,vpf1npG3nUARPSkFRx9xbVT~w1ELw0jQhdiXbxWcafg,AQACAAE/WebOfTrustRC2/1\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@ozK579KUDjjxkve1gKeV8IDd5qe0NPtxlFVV2-8iXrM,hlU82qnHi5ZnIfSD5~hr4wmUjNFJjQYted7FJWBj8z8,AQACAAE/WebOfTrustRC2/12\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@D6JSlrnlDDXfnGhijswHC5a4-EBuYNFhg2IyoFhzRRM,tCJy6EvZRhk-Og9CIkR3jfLD4VYxwOY5Wf991XMLdmE,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@Bn1Ozb4~Q639N~GvSXrbzZoVx9ziFfbkiKf1akXr9dQ,DrwwRjH~WrErR595BqxyDeisgcTLRZjCsQUBQSdnPLk,AQACAAE/WebOfTrustRC2/10\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@~aHztTNobVhKiaJ2eE-k6LLMy3qrRzL0zvk1UZp1xAU,xs3tuQsYRrTgxoU5qrayiwoOYEOLE6bTHvIKBQLIwco,AQACAAE/WebOfTrustRC2/3\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@jr3FrHPhtxP-IYQ-A12A3jjNNK4-KcHsUH8qNpBGHJ0,WK9IlZuG17JZ3YvaRUR-3uHf1YqkxxyGxkpkLW56ZTY,AQACAAE/WebOfTrustRC2/1\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@iBBjdbuuabx3v7iy-Pt3WZjWuixqo2-rzH~2kV4FkwE,Hll6SNQ~FXY5mrOY2W0GTfyNhJSlOrYutJmoXeSKseE,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@TKOVixP3xx6nA3-cgl2fksatQzxg4LlReoK0GFl7uAo,r~MCdO2rhGxBgRYv2EPuIE-tP7-0z6vuwbS04oyoAyI,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@bK9LQT2mqilUY4DxwmlKvKi5Jfwf4Brr~EQGa8RLIRU,Ni90~c4q-VNFrxFS7cDimQwCcRsqdScHoElWQgBpoT8,AQACAAE/WebOfTrustRC2/6\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@DYXv~V1jDo0XBhaiaGeRvy-~snqMbBiFR~R8TK8VBNg,4YYHZfVVYZmudSCOetDlNxnvbLy6IULV1SOgeC1zXmI,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@CSWvDlm9RM31O9NPpZomFvpTVM8cV0brixDXxpwnJ7A,4wfD~qxb5hFgCBN0JWgK6p11wUCpOHGOWhJ4i2FgFhY,AQACAAE/WebOfTrustRC2/0\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@sFsqYnIVQ9FwULsp3e-q0M46fRw2jVsLJYF9PUyHVME,aDhfrzYPt4O0BTuuRF3aCPTR0A~zr2rqHtPpJXeSqFU,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "<Trust Comment=\"\" Identity=\"USK@1QwMtp0oc4jIh6DFUjec~U0O3ldWHrRmhh5OZd41MMo,GZ-QOMc4mAkHhG4LwJWU~-rl0zJIp90YcB5hn3M9tlg,AQACAAE/WebOfTrustRC2/4\" Value=\"100\"/>"
            + "</TrustList>"
            + "</Identity>"
            + "</WebOfTrustRC2>";

    /**
     * main method.
     *
     * @param args Standard args.
     */
    public static void main(String[] args) {
        try {
            final Document xmlDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(rawXML.getBytes("UTF-8")));
            final Element identityElement = (Element) xmlDoc.getElementsByTagName("Identity").item(0);
            final Element trustListElement = (Element) identityElement.getElementsByTagName("TrustList").item(0);
            final NodeList trustList = trustListElement.getElementsByTagName("Trust");
            final Pattern keyPattern = Pattern.compile("USK@[%,~" + "*-_./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz" + "]+");
            for (int i = 0; i < trustList.getLength(); ++i) {
                Element trustElement = (Element) trustList.item(i);
                final String identity = trustElement.getAttribute("Identity");
                if (!keyPattern.matcher(identity).matches()) {
                    throw new RuntimeException("Parsing failure: Instead of USK URI I got: " + identity);
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

}
