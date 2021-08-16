/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8215769
 * @summary Java cannot probe pkcs12 files exported by Firefox
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

public class ProbeBER {

    public static final void main(String[] args) throws Exception {

        // This is a PKCS12 file using BER encoding
        String p12String =
            "3080020103308006092a864886f70d010701a0802480048208eb308030800609" +
            "2a864886f70d010701a08024800482054a3082054630820542060b2a864886f7" +
            "0d010c0a0102a08204f7308204f33025060a2a864886f70d010c010330170410" +
            "8dbfae75350a0f53d71bce1f8d7544b902030927c0048204c8d72c2675a105cb" +
            "7403547cce341b58b5c6943e69ae1ca597f5ea1777497adfbba455283524a46d" +
            "8d8301b2ba4d2304513ef4761e8a65a6e64f6d646a1501fa7f3c282d0f9a5a23" +
            "71b7fdf251464db00b617ed7017f11ab9286e10f7a5d30a21755c3aaf50473e4" +
            "41262bf103fa141f9caba66562f2a96aaf12a200c38aba11991ad73d9cee0008" +
            "2a7077a4a9a597336745895555cb24ca910be68c23ac5c4a5d1e47d387bf031d" +
            "3833c2cff88d6157da6152731b083ecfca20e3850f3fca4743d7ee0fb773be44" +
            "4e63b00c1096f34719dfef53912d01e477491bf0b7b01227c8fbbaf48fa399f7" +
            "f30d1ec0f622f66686d8a8baffdb811c201b408b89d483cc92f2bdcf7771b982" +
            "d43e821cfa45d789aeed87ca9ec9e3b283dc45395c6503f80e8d9e658e4e9160" +
            "5b141afec2ab065e1be39c0e67d665bedf2049561c583e3fd825688798bcbdb6" +
            "73cd2109cc20713fb6bab07648883e8f479e4877fc0c43cebdf0fe3a9e81b87a" +
            "ab4314cc2e8834360b3c9e49bf78fb2175112880191911846764b16eaf361584" +
            "039325e9c1a054f2c20cf4baf1ddd8f5636c4067cb411d4643031ce7299cc9dc" +
            "bc175b57e31bf5914c3e3cc490fb0538f34774898d3f5189ddf2261dadc25d89" +
            "bc3068dea6786023fe8a727e390e8b4a9b0a924490443d38962b57804b646169" +
            "427a42a36b2a9046e27f805f088516e9d07b0b77fc59650979364fe0cf44debf" +
            "6748d4b9c7fd54ec35fd51208e089a8b2a2117ee20df2225589595769137de28" +
            "a058e9a316828bb76499dfb66184e7fe6abb6840f664cf328e99126234a7c15d" +
            "b9c64acc2645e9e54b8a3888fea85054d9f3cea8586104b9de852bae696cb217" +
            "fca6e063e2272db81ae9ec9f4c291711c1bce51c72fed98f40a3dd6ba7858be1" +
            "1eda50f3270bbe753255e46b2dd0aface15a5ff034a2604b10f2afb8db2a74fd" +
            "9d3bd4b146386fa4b5b850fe857e79fc5e34d2e056c4f2eb9fdbf2922eabc36f" +
            "c7fe5fcdd5909a440b77e49256edd3ae77d317845d1dbbe7c3d05652540b35b8" +
            "2df52b7f120ec335fdc3ee52c888fdccdbffd83aae8544be2c6b8e3c9ee2fc05" +
            "3750833085dbcbd019e9584bec7b80cb95689459e5436819006c02dd844635a8" +
            "3fc565c8e5ddc1344a9a1cba9c9fcefe684cc75b4483f4e6072cc07eee72b1fe" +
            "4e93b23843969acdca4359a53a39a01da62ec154ef00491589c8f15a39b01b38" +
            "58c4dfdb13a939e7fd82228d8b2f99b3d59e595fc7b990ffa6b6fa82af64becd" +
            "5b9a98a6cca74f6b2f6812678c781bfa7ab0f764c29ca6c22f38bf57bfd9d063" +
            "0e69d8800b25f9c8aa3008522fbf68a4486cdd7d534cfc21ee5426dc0e5329c0" +
            "e7967d9b53b90638555f3d662bd539f8f235a35e1ed0546572a7a0395c325d23" +
            "373eef5b57bb937371947abffa806c8629c2cc32755153ca478ab97b3e76f187" +
            "5ab59bbcb7c57b5a96d0b26f133b46f0e2eca8471135b6d0f4b1ea3a6d8654d8" +
            "642a70a8f225fbffb199c051ff45ae91e50384f6b46b4373fa4fdca847dbc71e" +
            "c077269e7644805cd82c6624de1d1c376d646c6133d1718ad031216690913366" +
            "fc4eaa831830619d45dcc5335baf90855e5438ad5d2ac2e31cf2cc5a5b2867f4" +
            "193af8658097226bb4521061b2bef8dd0def00f86175d0b14301516f5a3c7119" +
            "7423cb327c3dc67efdc34605ab76a12895aba2582b5b4acc4b3dbb3a6d8679d0" +
            "2b3138301106092a864886f70d01091431041e020061302306092a864886f70d" +
            "01091531160414e5ae3d63720b42d660fcfb2453ebec069783b7f30000000000" +
            "00308006092a864886f70d010706a0803080020100308006092a864886f70d01" +
            "07013025060a2a864886f70d010c0106301704103b377c374d17c41a669fed4c" +
            "0fe3738a02030927c0a0800482032090c0a55a1c909e56310c3bc92a4b93cca1" +
            "b9e867c15216ce6bc2d397c30dc4c105d71b3f94dc599707f9d14abfe3d92572" +
            "e8dda6480879f3aba6b513f6c5db67ce0025c68af51c114a9fac664be2325965" +
            "7b04e2be92dde84cda6edc3c8e1a18b5c84c33691a5d1a4e3a203c74fe0cab62" +
            "85312454a0993fb9b30fbae0f20f19f307b4ad74d9501fa517d5d5ccc91903dd" +
            "3bdf9a438cbbebd5263dfb6605534aa2acc3ee6c0ce341533e5e74b1bf82f57d" +
            "a9254fc2f91eab013658eb06b6c0302c3bd674ecf30025d546bb1290d732ab8b" +
            "ba4bcf9c02e5774f7f522856acbef7159be6b7e6a2cb6119e3ac039fc93247d3" +
            "5f08281c1d50d0ea7275d75095ced1518f7c4ee1c072871139cf6cf6f9c67606" +
            "0396c430c0cbdae332af42ac3c2458c929644aa4e695a9050b6cf61563f16c0f" +
            "f552114df5d4ee22e0335413a3b93ec1f0fa43b00f36d4ef7efb849731229c2d" +
            "0b043d57504dff525383d927dde23c95c3d4e2546e3478220a25e56a37e7904a" +
            "323a4db9e2d94b268bee66a38490e34c7ed077bf20677b173e98ef3687e89310" +
            "e46a1b5ead88d0eb1d2b9d729854b002e70164844f50197e97e8b257ad275ee6" +
            "e127fae23bd6afd9e1fe61e3574380dc371ad7b2bb0e00501bdf543feaf461f6" +
            "b483154f984441a7c62469ab82da56d942d49773f6d271feacc0421c413f4e47" +
            "e88863e9ea879af6fda6c31b0a4ea715746234ef2b0e7dffa8aacfc98a9129c8" +
            "96b86f90655a776e817713828961cbf78b0bf81be29ccc7735203f971be2125a" +
            "f770bf7b0815c5f610a0a6f45e7455bfde110d41dc621898aa22f0c0391787bb" +
            "f4a7485a59d6cb3d2558ec5ca210ab45f0ee98abdeea555353d7fd9af80d953d" +
            "8795bb21b7ac6fb185cffbf8cf6409dc837860a463805658f6c9c6ac50722a94" +
            "05a7c44e9f71d41031c200c6e1ba2d7d34a7859011feaa9dbbaa26c585c7fea4" +
            "cf14e432f0d8277d6d4d2d86e25dc280922aa92802915f22236e588136b2ad45" +
            "df5145dcfb9f3dd5fa92e4353efbd75b90bb320c028abecccd1d31f9e087c094" +
            "70bae73647367e2bdd874ce19ca15b16fa1c96469518e646be3852e9e65f6035" +
            "e2ca9a59193c5c4e2edefe739d5cd50408abec9671d9574be800000000000000" +
            "0000000000000000000000303a3021300906052b0e03021a05000414d9248a44" +
            "786f0888dc8389bab0a5daa3246031ea0410247e59baa3f6e829425929a052e7" +
            "6c9a02030927c00000";
        byte[] p12 = new byte[p12String.length()/2];
        for (int i=0; i<p12.length; i++) {
            p12[i] = Integer.valueOf(
                    p12String.substring(2*i,2*i+2), 16).byteValue();
        }

        Files.write(Path.of("p12"), p12);
        KeyStore.getInstance(new File("p12"), "changeit".toCharArray())
                .getCertificate("a");
    }
}
