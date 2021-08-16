/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary test ISO639-2 language codes
 * @library /java/text/testlib
 * @compile -encoding ascii Bug4175998Test.java
 * @run main Bug4175998Test
 * @bug 4175998
 */

/*
 *
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The original version of this source code and documentation is
 * copyrighted and owned by IBM. These materials are provided
 * under terms of a License Agreement between IBM and Sun.
 * This technology is protected by multiple US and International
 * patents. This notice and attribution to IBM may not be removed.
 *
 */

import java.util.*;

/**
 *  Bug4175998Test verifies that the following bug has been fixed:
 *  Bug 4175998 - The java.util.Locale.getISO3Language() returns wrong result for a locale with
 *           language code 'ta'(Tamil).
 */
public class Bug4175998Test extends IntlTest {
    public static void main(String[] args) throws Exception {
        new Bug4175998Test().run(args);
        //generateTables();    //uncomment this to regenerate data tables
    }

    public void testIt() throws Exception {
        boolean bad = false;
        for (int i = 0; i < CODES.length; i++) {
            final String[] localeCodes = CODES[i];
            final Locale l = new Locale(localeCodes[0], "");
            final String iso3 = l.getISO3Language();
            if (!iso3.equals(localeCodes[1]) /*&& !iso3.equals(localeCodes[2])*/) {
                logln("Locale("+l+") returned bad ISO3 language code."
                        +"   Got '"+iso3+"' instead of '"+localeCodes[1]+"'"/*+" or '"+localeCodes[2]+"'"*/);
                bad = true;
            }
        }
        if (bad) {
            errln("Bad ISO3 language codes detected.");
        }
    }

     private static final String[][] CODES = {
        {"pt","por","por"},
        {"eu","eus","baq"},
        {"ps","pus","pus"},
        {"et","est","est"},
        {"ka","kat","geo"},
        {"es","spa","spa"},
        {"eo","epo","epo"},
        {"en","eng","eng"},
        {"pl","pol","pol"},
        {"el","ell","gre"},
        {"uz","uzb","uzb"},
        {"jv","jav","jav"},
        {"ur","urd","urd"},
        {"uk","ukr","ukr"},
        {"ug","uig","uig"},
        {"zu","zul","zul"},
        {"ja","jpn","jpn"},
        {"or","ori","ori"},
        {"om","orm","orm"},
        {"zh","zho","chi"},
        {"tw","twi","twi"},
        {"de","deu","ger"},
        {"oc","oci","oci"},
        {"za","zha","zha"},
        {"tt","tat","tat"},
        {"iu","iku","iku"},
        {"ts","tso","tso"},
        {"it","ita","ita"},
        {"tr","tur","tur"},
        {"da","dan","dan"},
        {"is","isl","ice"},
        {"to","ton","ton"},
        {"tl","tgl","tgl"},
        {"tk","tuk","tuk"},
        {"ik","ipk","ipk"},
        {"ti","tir","tir"},
        {"th","tha","tha"},
        {"tg","tgk","tgk"},
        {"te","tel","tel"},
        {"cy","cym","wel"},
        {"ie","ile","ile"},
        {"id","ind","ind"},
        {"ta","tam","tam"},
        {"ia","ina","ina"},
        {"cs","ces","cze"},
        {"yo","yor","yor"},
        {"no","nor","nor"},
        {"co","cos","cos"},
        {"nl","nld","dut"},
        {"yi","yid","yid"},
        {"hy","hye","arm"},
        {"sw","swa","swa"},
        {"ne","nep","nep"},
        {"sv","swe","swe"},
        {"su","sun","sun"},
        {"hu","hun","hun"},
        {"na","nau","nau"},
        {"sr","srp","scc"},
        {"ca","cat","cat"},
        {"sq","sqi","alb"},
        {"hr","hrv","scr"},
        {"so","som","som"},
        {"sn","sna","sna"},
        {"sm","smo","smo"},
        {"sl","slv","slv"},
        {"sk","slk","slo"},
        {"si","sin","sin"},
        {"hi","hin","hin"},
        {"my","mya","bur"},
        {"sd","snd","snd"},
        {"he","heb","heb"},
        {"sa","san","san"},
        {"mt","mlt","mlt"},
        {"ms","msa","may"},
        {"ha","hau","hau"},
        {"mr","mar","mar"},
        {"br","bre","bre"},
        {"mo","mol","mol"},
        {"bo","bod","tib"},
        {"mn","mon","mon"},
        {"bn","ben","ben"},
        {"ml","mal","mal"},
        {"mk","mkd","mac"},
        {"xh","xho","xho"},
        {"mi","mri","mao"},
        {"bi","bis","bis"},
        {"bh","bih","bih"},
        {"mg","mlg","mlg"},
        {"bg","bul","bul"},
        {"rw","kin","kin"},
        {"be","bel","bel"},
        {"ru","rus","rus"},
        {"gu","guj","guj"},
        {"ba","bak","bak"},
        {"ro","ron","rum"},
        {"rm","roh","roh"},
        {"gn","grn","grn"},
        {"az","aze","aze"},
        {"ay","aym","aym"},
        {"gd","gla","gla"},
        {"lv","lav","lav"},
        {"lt","lit","lit"},
        {"ga","gle","gle"},
        {"as","asm","asm"},
        {"ar","ara","ara"},
        {"wo","wol","wol"},
        {"ln","lin","lin"},
        {"am","amh","amh"},
        {"fy","fry","fry"},
        {"af","afr","afr"},
        {"qu","que","que"},
        {"ab","abk","abk"},
        {"la","lat","lat"},
        {"aa","aar","aar"},
        {"fr","fra","fre"},
        {"fo","fao","fao"},
        {"fj","fij","fij"},
        {"fi","fin","fin"},
        {"ky","kir","kir"},
        {"ku","kur","kur"},
        {"fa","fas","per"},
        {"ks","kas","kas"},
        {"vo","vol","vol"},
        {"ko","kor","kor"},
        {"kn","kan","kan"},
        {"kk","kaz","kaz"},
        {"vi","vie","vie"},
    };

/*
    The following code was used to generate the table above from the two ISO standards.
    It matches the language names (not the codes) from both standards to associate
    the two and three letter codes.

    private static final String ISO639 = "d:\\temp\\iso639.txt";
    private static final String ISO6392 = "d:\\temp\\iso-639-2.txt";
    private static void generateTables() {
        try {
            BufferedReader ISO639File = new BufferedReader(new FileReader(ISO639));
            Hashtable i639 = new Hashtable();
            for (String line = ISO639File.readLine(); line != null; line = ISO639File.readLine()) {
                if (!line.startsWith("#")) {
                    final int ndx = line.indexOf(' ');
                    final String arg1 = line.substring(0, ndx);
                    final int ndx2 = line.indexOf(' ', ndx+1);
                    final String arg2 = line.substring(ndx+1, ndx2 < 0 ? line.length() : ndx2);
                    i639.put(arg1, arg2);
                }
            }

            BufferedReader ISO6392File = new BufferedReader(new FileReader(ISO6392));
            Hashtable i6392 = new Hashtable();
            for (String line = ISO6392File.readLine(); line != null; line = ISO6392File.readLine()) {
                final int ndx = line.indexOf(' ');
                final int ndx2 = line.indexOf(' ', ndx+1);
                int ndx3 = line.indexOf(' ', ndx2+1);
                if (ndx3 < 0) ndx3 = line.length();
                final String arg1 = line.substring(0, ndx);
                final String arg2 = line.substring(ndx+1, ndx2);
                final String arg3 = line.substring(ndx2+1, ndx3);
                i6392.put(arg3, new ISO6392Entry(arg1, arg2));
            }

            Enumeration keys = i639.keys();
            while (keys.hasMoreElements()) {
                final Object key = keys.nextElement();
                final Object name = i639.get(key);
                final Object value = i6392.get(name);

                if (value != null) {
                    System.out.print("{");
                    System.out.print("\""+key+"\",");
                    System.out.print(value);
                    System.out.println("},");
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    private static final class ISO6392Entry {
        public final String code;
        public final String name;
        public ISO6392Entry(String code, String name) {
            this.code = code;
            this.name = name;
        }
        public String toString() {
            return "\""+code+"\",\""+name+"\"";
        }

    }
*/

}

/*

data from ftp://dkuug.dk on March 4, 1999
verified by http://www.triacom.com/archive/iso639-2.en.html

iso 639 data
aa Afar
ab Abkhazian
af Afrikaans
am Amharic
ar Arabic
as Assamese
ay Aymara
az Azerbaijani
ba Bashkir
be Belarussian
bg Bulgarian
bh Bihari
bi Bislama
bn Bengali
bo Tibetan
br Breton
ca Catalan
co Corsican
cs Czech
cy Welsh
da Danish
de German
dz Bhutani
el Greek
en English
eo Esperanto
es Spanish
et Estonian
eu Basque
fa Persian
fi Finnish
fj Fijian
fo Faroese
fr French
fy Frisian
ga Irish
gd Gaelic
gl Galician
gn Guarani
gu Gujarati
ha Hausa
he Hebrew
hi Hindi
hr Croatian
hu Hungarian
hy Armenian
ia Interlingua
id Indonesian
ie Interlingue
ik Inupiak
is Icelandic
it Italian
iu Inuktitut
ja Japanese
jw Javanese
ka Georgian
kk Kazakh
kl Greenlandic
km Cambodian
kn Kannada
ko Korean
ks Kashmiri
ku Kurdish
ky Kirghiz
la Latin
ln Lingala
lo Laothian
lt Lithuanian
lv Latvian
mg Malagasy
mi Maori
mk Macedonian
ml Malayalam
mn Mongolian
mo Moldavian
mr Marathi
ms Malay
mt Maltese
my Burmese
na Nauru
ne Nepali
nl Dutch
no Norwegian
oc Occitan
om Oromo
or Oriya
pa Punjabi
pl Polish
ps Pushto
pt Portuguese
qu Quechua
rm Raeto-Romance
rn Kirundi
ro Romanian
ru Russian
rw Kinyarwanda
sa Sanskrit
sd Sindhi
sg Sangho
sh Croatian (Serbo)
si Sinhalese
sk Slovak
sl Slovenian
sm Samoan
sn Shona
so Somali
sq Albanian
sr Serbian
ss Siswati
st Sesotho
su Sundanese
sv Swedish
sw Swahili
ta Tamil
te Telugu
tg Tajik
th Thai
ti Tigrinya
tk Turkmen
tl Tagalog
tn Setswana
to Tonga
tr Turkish
ts Tsonga
tt Tatar
tw Twi
ug Uighur
uk Ukrainian
ur Urdu
uz Uzbek
vi Vietnamese
vo Volapuk
wo Wolof
xh Xhosa
yi Yiddish
yo Yoruba
za Zhuang
zh Chinese
zu Zulu

ISO 639-2 data

aar aar Afar
abk abk Abkhazian
ace ace Achinese
ach ach Acoli
ada ada Adangme
afa afa Afro-Asiatic (Other)
afh afh Afrihili
afr afr Afrikaans
aka aka Akan
akk akk Akkadian
ale ale Aleut
alg alg Algonquian languages
amh amh Amharic
ang ang English-Old (ca. 450-1100)
apa apa Apache languages
ara ara Arabic
arc arc Aramaic
arn arn Araucanian
arp arp Arapaho
art art Artificial (Other)
arw arw Arawak
asm asm Assamese
ath ath Athapascan languages
aus aus Australian languages
ava ava Avaric
ave ave Avestan
awa awa Awadhi
aym aym Aymara
aze aze Azerbaijani
bad bad Banda
bai bai Bamileke languages
bak bak Bashkir
bal bal Baluchi
bam bam Bambara
ban ban Balinese
bas bas Basa
bat bat Baltic (Other)
bej bej Beja
bel bel Belarussian
bem bem Bemba
ben ben Bengali
ber ber Berber (Other)
bho bho Bhojpuri
bih bih Bihari
bik bik Bikol
bin bin Bini
bis bis Bislama
bla bla Siksika
bnt bnt Bantu (Other)
bod tib Tibetan
bra bra Braj
bre bre Breton
btk btk Batak (Indonesia)
bua bua Buriat
bug bug Buginese
bul bul Bulgarian
cad cad Caddo
cai cai Central-American-Indian (Other)
car car Carib
cat cat Catalan
cau cau Caucasian (Other)
ceb ceb Cebuano
cel cel Celtic (Other)
ces cze Czech
cha cha Chamorro
chb chb Chibcha
che che Chechen
chg chg Chagatai
chk chk Chuukese
chm chm Mari
chn chn Chinook-jargon
cho cho Choctaw
chp chp Chipewyan
chr chr Cherokee
chu chu Church-Slavic
chv chv Chuvash
chy chy Cheyenne
cmc cmc Chamic languages
cop cop Coptic
cor cor Cornish
cos cos Corsican
cpe cpe Creoles-and-pidgins-English-based (Other)
cpf cpf Creoles-and-pidgins-French-based (Other)
cpp cpp Creoles-and-pidgins-Portuguese-based (Other)
cre cre Cree
crp crp Creoles-and-pidgins (Other)
cus cus Cushitic (Other)
cym wel Welsh
dak dak Dakota
dan dan Danish
day day Dayak
del del Delaware
den den Slave (Athapascan)
deu ger German
dgr dgr Dogrib
din din Dinka
div div Divehi
doi doi Dogri
dra dra Dravidian (Other)
dua dua Duala
dum dum Dutch-Middle (ca. 1050-1350)
dyu dyu Dyula
dzo dzo Dzongkha
efi efi Efik
egy egy Egyptian (Ancient)
eka eka Ekajuk
ell gre Greek Modern (post 1453)
elx elx Elamite
eng eng English
enm enm English-Middle (1100-1500)
epo epo Esperanto
est est Estonian
eus baq Basque
ewe ewe Ewe
ewo ewo Ewondo
fan fan Fang
fao fao Faroese
fas per Persian
fat fat Fanti
fij fij Fijian
fin fin Finnish
fiu fiu Finno-Ugrian (Other)
fon fon Fon
fra fre French
frm frm French-Middle (ca. 1400-1600)
fro fro French-Old (842-ca. 1400)
fry fry Frisian
ful ful Fulah
fur fur Friulian
gaa gaa Ga
gay gay Gayo
gba gba Gbaya
gem gem Germanic (Other)
gez gez Geez
gil gil Gilbertese
gdh gae Gaelic
gai iri Irish
glg glg Gallegan
glv glv Manx
gmh gmh German-Middle High (ca. 1050-1500)
goh goh German-Old High (ca. 750-1050)
gon gon Gondi
gor gor Gorontalo
got got Gothic
grb grb Grebo
grc grc Greek-Ancient (to 1453)
grn grn Guarani
guj guj Gujarati
gwi gwi Gwich'in
hai hai Haida
hau hau Hausa
haw haw Hawaiian
heb heb Hebrew
her her Herero
hil hil Hiligaynon
him him Himachali
hin hin Hindi
hit hit Hittite
hmn hmn Hmong
hmo hmo Hiri Motu
hrv scr Croatian
hun hun Hungarian
hup hup Hupa
hye arm Armenian
iba iba Iban
ibo ibo Igbo
ijo ijo Ijo
iku iku Inuktitut
ile ile Interlingue
ilo ilo Iloko
ina ina Interlingua (International Auxilary Language Association)
inc inc Indic (Other)
ind ind Indonesian
ine ine Indo-European (Other)
ipk ipk Inupiak
ira ira Iranian (Other)
iro iro Iroquoian languages
isl ice Icelandic
ita ita Italian
jaw jav Javanese
jpn jpn Japanese
jpr jpr Judeo-Persian
jrb jrb Judeo-Arabic
kaa kaa Kara-Kalpak
kab kab Kabyle
kac kac Kachin
kal kal Kalaallisut
kam kam Kamba
kan kan Kannada
kar kar Karen
kas kas Kashmiri
kat geo Georgian
kau kau Kanuri
kaw kaw Kawi
kaz kaz Kazakh
kha kha Khasi
khi khi Khoisan (Other)
khm khm Khmer
kho kho Khotanese
kik kik Kikuyu
kin kin Kinyarwanda
kir kir Kirghiz
kmb kmb Kimbundu
kok kok Konkani
kom kom Komi
kon kon Kongo
kor kor Korean
kos kos Kosraean
kpe kpe Kpelle
kro kro Kru
kru kru Kurukh
kua kua Kuanyama
kum kum Kumyk
kur kur Kurdish
kut kut Kutenai
lad lad Ladino
lah lah Lahnda
lam lam Lamba
lao lao Lao
lat lat Latin
lav lav Latvian
lez lez Lezghian
lin lin Lingala
lit lit Lithuanian
lol lol Mongo
loz loz Lozi
ltz ltz Letzeburgesch
lua lua Luba-Lulua
lub lub Luba-Katanga
lug lug Ganda
lui lui Luiseno
lun lun Lunda
luo luo Luo (Kenya and Tanzania)
lus lus Lushai
mad mad Madurese
mag mag Magahi
mah mah Marshall
mai mai Maithili
mak mak Makasar
mal mal Malayalam
man man Mandingo
map map Austronesian (Other)
mar mar Marathi
mas mas Masai
mdr mdr Mandar
men men Mende
mga mga Irish-Middle (900-1200)
mic mic Micmac
min min Minangkabau
mis mis Miscellaneous languages
mkd mac Macedonian
mkh mkh Mon-Khmer (Other)
mlg mlg Malagasy
mlt mlt Maltese
mni mni Manipuri
mno mno Manobo languages
moh moh Mohawk
mol mol Moldavian
mon mon Mongolian
mos mos Mossi
mri mao Maori
msa may Malay
mul mul Multiple languages
mun mun Munda languages
mus mus Creek
mwr mwr Marwari
mya bur Burmese
myn myn Mayan languages
nah nah Nahuatl
nai nai North American Indian (Other)
nau nau Nauru
nav nav Navajo
nbl nbl Ndebele, South
nde nde Ndebele, North
ndo ndo Ndonga
nep nep Nepali
new new Newari
nia nia Nias
nic nic Niger-Kordofanian (Other)
niu niu Niuean
nld dut Dutch
non non Norse, Old
nor nor Norwegian
nso nso Sohto, Northern
nub nub Nubian languages
nya nya Nyanja
nym nym Nyamwezi
nyn nyn Nyankole
nyo nyo Nyoro
nzi nzi Nzima
oci oci Occitan (post 1500)
oji oji Ojibwa
ori ori Oriya
orm orm Oromo
osa osa Osage
oss oss Ossetic
ota ota Turkish, Ottoman (1500-1928)
oto oto Otomian languages
paa paa Papuan (Other)
pag pag Pangasinan
pal pal Pahlavi
pam pam Pampanga
pan pan Panjabi
pap pap Papiamento
pau pau Palauan
peo peo Persian, Old (ca. 600-400 B.C.)
phi phi Philippine (Other)
phn phn Phoenician
pli pli Pali
pol pol Polish
pon pon Pohnpeian
por por Portuguese
pra pra Prakrit languages
pro pro Proven\u00E7al, Old (to 1500)
pus pus Pushto
qaa-qtz qaa-qtz Reserved for local use
que que Quechua
raj raj Rajasthani
rap rap Rapanui
rar rar Rarotongan
roa roa Romance (Other)
roh roh Raeto-Romance
rom rom Romany
ron rum Romanian
run run Rundi
rus rus Russian
sad sad Sandawe
sag sag Sango
sah sah Yakut
sai sai South American Indian (Other)
sal sal Salishan languages
sam sam Samaritan Aramaic
san san Sanskrit
sas sas Sasak
sat sat Santali
sco sco Scots
sel sel Selkup
sem sem Semitic (Other)
sga sga Irish-Old (to 900)
shn shn Shan
sid sid Sidamo
sin sin Sinhalese
sio sio Siouan languages
sit sit Sino-Tibetan (Other)
sla sla Slavic (Other)
slk slo Slovak
slv slv Slovenian
smi smi Sami languages
smo smo Samoan
sna sna Shona
snd snd Sindhi
snk snk Soninke
sog sog Sogdian
som som Somali
son son Songhai
sot sot Sotho Southern
spa spa Spanish
sqi alb Albanian
srd srd Sardinian
srp scc Serbian
srr srr Serer
ssa ssa Nilo-Saharan (Other)
ssw ssw Swati
suk suk Sukuma
sun sun Sundanese
sus sus Susu
sux sux Sumerian
swa swa Swahili
swe swe Swedish
syr syr Syriac
tah tah Tahitian
tai tai Tai (Other)
tam tam Tamil
tat tat Tatar
tel tel Telugu
tem tem Timne
ter ter Tereno
tet tet Tetum
tgk tgk Tajik
tgl tgl Tagalog
tha tha Thai
tig tig Tigre
tir tir Tigrinya
tiv tiv Tiv
tkl tkl Tokelau
tli tli Tlingit
tmh tmh Tamashek
tog tog Tonga (Nyasa)
ton ton Tonga (Tonga Islands)
tpi tpi Tok Pisin
tsi tsi Tsimshian
tsn tsn Tswana
tso tso Tsonga
tuk tuk Turkmen
tum tum Tumbuka
tur tur Turkish
tut tut Altaic
tvl tvl Tuvalu
twi twi Twi
tyv tyv Tuvinian
uga uga Ugaritic
uig uig Uighur
ukr ukr Ukrainian
umb umb Umbundu
und und Undetermined
urd urd Urdu
uzb uzb Uzbek
vai vai Vai
ven ven Venda
vie vie Vietnamese
vol vol Volapuk
vot vot Votic
wak wak Wakashan
wal wal Walamo
war war Waray
was was Washo
wen wen Sorbian
wol wol Wolof
xho xho Xhosa
yao yao Yao
yap yap Yapese
yid yid Yiddish
yor yor Yoruba
ypk ypk Yupik
zap zap Zapotec
zen zen Zenaga
zha zha Zhuang
zho chi Chinese
znd znd Zande
zul zul Zulu
zun zun Zuni

*/
