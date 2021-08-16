<?xml version="1.0" encoding="UTF-8"?> 
<!-- 
Invoice Transfer XSLT 
--> 
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:exslt="http://exslt.org/common" exclude-result-prefixes="exslt" version="1.0"> 

<!--<xsl:output method="xml" indent="yes"/>--> 
<xsl:output method="text" encoding="iso-8859-1"/> 
<xsl:decimal-format name="european" decimal-separator="," grouping-separator="'"/> 

<!-- Definition of global constants --> 
<xsl:variable name="batchSTYPE">0</xsl:variable> 
<xsl:variable name="batchGROUP">fto-erech</xsl:variable>	
<xsl:variable name="batchMANDT">200</xsl:variable> 
<xsl:variable name="batchUSNAM">amos-batch</xsl:variable> 
<xsl:variable name="batchSTART">00000000</xsl:variable> 
<xsl:variable name="batchXKEEP">X</xsl:variable> 

<!-- KREDI constants --> 
<xsl:variable name="constTCODE">FB01</xsl:variable>	
<xsl:variable name="constBLART">TP</xsl:variable>	
<xsl:variable name="constBUKRS">EWF</xsl:variable>	

<!-- LZBKZ constants 
"566" for S-Orders and "950" for all the rest --> 
<xsl:variable name="constLZBZK_S_ORDER">566</xsl:variable> 
<xsl:variable name="constLZBZK_OTHER">950</xsl:variable> 

<!-- Tax code uised for charges flagged as "TAX" --> 
<xsl:variable name="taxCodeConst">2w</xsl:variable> 
<!-- Tax code used for all other occurences in file --> 
<xsl:variable name="taxCodeDefaultConst">V0</xsl:variable>	

<!-- entry point --> 
<xsl:template match="/"> 

<xsl:variable name="transformation_1"> 
<xsl:call-template name="transformation_1_elements"/> 
</xsl:variable> 

<xsl:variable name="transformation_2"> 
<transformation_2_elements> 
<batchInformation> 
<xsl:call-template name="batchInformation"/> 
</batchInformation> 
<xsl:for-each select="exslt:node-set($transformation_1)"> 
<xsl:apply-templates select="transformation_1_elements"/> 
</xsl:for-each> 
</transformation_2_elements> 
</xsl:variable> 

<xsl:variable name="transformation_3"> 
<transformation_3_elements> 
<xsl:for-each select="exslt:node-set($transformation_2)"> 
<xsl:apply-templates select="transformation_2_elements"/> 
</xsl:for-each> 
</transformation_3_elements> 
</xsl:variable> 

<!-- text output --> 
<xsl:for-each select="exslt:node-set($transformation_3)"> 
<xsl:apply-templates select="transformation_3_elements" /> 
</xsl:for-each> 

<!-- xml output 
<xsl:for-each select="exslt:node-set($transformation_3)"> 
<xsl:copy-of select="."/> 
</xsl:for-each>--> 

</xsl:template> 

<xsl:template match="transformation_3_elements"> 
<xsl:for-each select="*"> 
<xsl:call-template name="Fill_Up" /> 
</xsl:for-each> 
</xsl:template> 

<xsl:template name="Fill_Up"> 
<xsl:for-each select="*"> 
<xsl:variable name="align"> 
<xsl:value-of select="@Alignment" /> 
</xsl:variable> 
<xsl:variable name="filler"> 
<xsl:value-of select="@Filler" /> 
</xsl:variable> 
<xsl:variable name="length"> 
<xsl:value-of select="@Length" /> 
</xsl:variable> 
<xsl:variable name="tagValue"> 
<xsl:value-of select="." /> 
</xsl:variable> 

<xsl:variable name="defaultFiller"> 
<xsl:choose> 
<xsl:when test="$filler = 'blank'"> 
<!-- make empty fields to be filled with '/' --> 
<xsl:choose> 
<xsl:when test="$tagValue = '/'"> 
<xsl:value-of select="'/'" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="' '" /> 
</xsl:otherwise> 
</xsl:choose> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="$filler" /> 
</xsl:otherwise> 
</xsl:choose> 
</xsl:variable> 

<xsl:call-template name="justify"> 
<xsl:with-param name="align" select="$align" /> 
<xsl:with-param name="filler" select="$defaultFiller" /> 
<xsl:with-param name="value" select="$tagValue" /> 
<xsl:with-param name="width" select="$length" /> 
</xsl:call-template> 
</xsl:for-each> 
<xsl:text>&#xa;</xsl:text> 
</xsl:template> 

<!-- start transformation_3 --> 
<xsl:template match="transformation_2_elements"> 
<xsl:apply-templates select="batchInformation" /> 
<xsl:apply-templates select="invoice_elements" mode="transformation_3" /> 
</xsl:template> 

<xsl:template match="invoice_elements" mode="transformation_3"> 
<xsl:for-each select="*"> 
<xsl:if test="contains(name(),'header_elements')"> 
<xsl:call-template name="header_elements_copy" /> 
</xsl:if> 
<xsl:if test="contains(name(),'header_1_elements')"> 
<xsl:call-template name="header_1_elements_copy" /> 
</xsl:if> 
<xsl:if test="contains(name(),'invoiceCharges_1_elements')"> 
<xsl:call-template name="invoiceCharges_1_elements_copy" /> 
</xsl:if> 
<xsl:if test="contains(name(),'invoiceDetails_1_elements')"> 
<xsl:call-template name="invoiceDetails_1_elements_copy" /> 
</xsl:if> 
<xsl:if test="contains(name(),'invoiceDetails_DetailCharges_1_elements')"> 
<xsl:call-template name="invoiceDetails_DetailCharges_1_elements_copy" /> 
</xsl:if> 

</xsl:for-each> 
</xsl:template> 

<xsl:template match="batchInformation"> 
<xsl:copy-of select="." /> 
</xsl:template> 
<xsl:template name="header_elements_copy"> 
<xsl:copy-of select="." /> 
</xsl:template> 
<xsl:template name="header_1_elements_copy"> 
<xsl:copy-of select="." /> 
</xsl:template> 
<xsl:template name="invoiceCharges_1_elements_copy"> 
<xsl:copy-of select="." /> 
</xsl:template> 
<xsl:template name="invoiceDetails_1_elements_copy"> 
<xsl:copy-of select="." /> 
</xsl:template> 
<xsl:template name="invoiceDetails_DetailCharges_1_elements_copy"> 
<xsl:copy-of select="." /> 
</xsl:template> 
<!-- end transformation_3 --> 

<!-- start transformation_2 --> 
<xsl:template name="batchInformation"> 
<STYPE ID="1" Length="1" Alignment="left" Filler="blank"> 
<xsl:value-of select="$batchSTYPE" /> 
</STYPE> 
<GROUP ID="2" Length="12" Alignment="left" Filler="blank"> 
<xsl:value-of select="$batchGROUP" /> 
</GROUP> 
<MANDT ID="3" Length="3" Alignment="left" Filler="blank"> 
<xsl:value-of select="$batchMANDT" /> 
</MANDT> 
<USNAM ID="4" Length="12" Alignment="left" Filler="blank"> 
<xsl:value-of select="$batchUSNAM" /> 
</USNAM> 
<START ID="5" Length="8" Alignment="left" Filler="blank"> 
<xsl:value-of select="$batchSTART" /> 
</START> 
<XKEEP ID="6" Length="1" Alignment="left" Filler="blank"> 
<xsl:value-of select="$batchXKEEP" /> 
</XKEEP> 
<NODATA ID="7" Length="1" Alignment="left" Filler="blank">/</NODATA> 
</xsl:template> 

<xsl:template match="transformation_1_elements"> 
<xsl:apply-templates select="invoice_elements" mode="transformation_2"/> 
</xsl:template> 

<xsl:template match="invoice_elements" mode="transformation_2"> 
<invoice_elements> 
<xsl:for-each select="@*"> 
<xsl:attribute name="{local-name(.)}"> 
<xsl:value-of select="."/> 
</xsl:attribute> 
</xsl:for-each> 
<xsl:call-template name="header_elements"/> 
<xsl:call-template name="header_1_elements"/> 
<xsl:for-each select="*[count(./*) &gt; 0]"> 
<xsl:if test="contains(name(),'invoiceCharges_elements')"> 
<xsl:call-template name="invoiceCharges_1_elements"/> 
</xsl:if> 
<xsl:if test="contains(name(),'invoiceDetails_elements')"> 
<xsl:call-template name="invoiceDetails_1_elements"/> 
</xsl:if> 
<xsl:if test="contains(name(),'invoiceDetails_DetailCharges_elements')"> 
<xsl:call-template name="invoiceDetails_DetailCharges_1_elements"/> 
</xsl:if> 
</xsl:for-each> 
</invoice_elements> 
</xsl:template> 

<xsl:template name="header_elements"> 
<header_elements> 
<STYPE ID="1" Length="1" Alignment="left" Filler="blank">1</STYPE> 
<TCODE ID="2" Length="20" Alignment="left" Filler="blank"> 
<xsl:value-of select="$constTCODE" /> 
</TCODE> 
<!-- create invoice date --> 
<xsl:variable name="invoiceDate"> 
<xsl:call-template name="getFormatedDate"> 
<xsl:with-param name="date2format" select="invoiceDate"/> 
</xsl:call-template> 
</xsl:variable> 
<!-- create invoice date month --> 
<xsl:variable name="invoiceMonth"> 
<xsl:call-template name="getMonthOfDate"> 
<xsl:with-param name="date2format" select="invoiceDate"/> 
</xsl:call-template> 
</xsl:variable> 
<BLDAT ID="3" Length="8" Alignment="left" Filler="blank"> 
<xsl:value-of select="$invoiceDate" /> 
</BLDAT> 
<BLART ID="4" Length="2" Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="(invoiceNumber &gt; 0051053500) and (invoiceNumber &lt; 0051999999)">KA</xsl:when> 
<xsl:when test="(invoiceNumber &gt; 0054012000) and (invoiceNumber &lt; 0054999999)">EA</xsl:when> 
<xsl:otherwise> </xsl:otherwise> 
</xsl:choose> 
</BLART> 
<BUKRS ID="5" Length="4" Alignment="left" Filler="blank"> 
<xsl:value-of select="$constBUKRS" /> 
</BUKRS> 
<!-- create transfer date --> 
<xsl:variable name="transferDate"> 
<xsl:call-template name="getFormatedDate"> 
<xsl:with-param name="date2format" select="transferDate"/> 
</xsl:call-template> 
</xsl:variable> 
<BUDAT ID="6" Length="8" Alignment="left" Filler="blank"> 
<xsl:value-of select="$transferDate" /> 
</BUDAT> 
<MONAT ID="7" Length="2" Alignment="left" Filler="blank"> 
<xsl:value-of select="$invoiceMonth" /> 
</MONAT> 

<WAERS ID="8" Length="5" Alignment="left" Filler="blank"> 
<xsl:value-of select="targetCurrency" /> 
</WAERS> 

<KURSF ID="9" Length="10" Alignment="left" Filler="blank"> 
<xsl:value-of select="format-number(conversionRate, '0000,00000', 'european')"/>	
</KURSF> 
<!--<BELNR ID="10" Length="10" Alignment="left" Filler="blank">/</BELNR>--> 
<BELNR ID="10" Length="10" Alignment="left" Filler="blank"> 
<xsl:value-of select="invoiceNumber" /> 
</BELNR> 
<WWERT ID="11" Length="8" Alignment="left" Filler="blank">/</WWERT> 
<XBLNR ID="12" Length="16" Alignment="left" Filler="blank"> 
<xsl:value-of select="creditorInvoiceNumber" /> 
</XBLNR> 
<BVORG ID="13" Length="16" Alignment="left" Filler="blank">/</BVORG> 
<BKTXT ID="14" Length="25" Alignment="left" Filler="blank">/</BKTXT> 
<PARGB ID="15" Length="4" Alignment="left" Filler="blank">/</PARGB> 
<AUGLV ID="16" Length="8" Alignment="left" Filler="blank">/</AUGLV> 
<VBUND ID="17" Length="6" Alignment="left" Filler="blank">/</VBUND> 
<XMWST ID="18" Length="1" Alignment="left" Filler="blank">X</XMWST> 
<DOCID ID="19" Length="10" Alignment="left" Filler="blank">/</DOCID> 
<BARCD ID="20" Length="40" Alignment="left" Filler="blank">/</BARCD> 
<STODT ID="21" Length="8" Alignment="left" Filler="blank">/</STODT> 
<BRNCH ID="22" Length="4" Alignment="left" Filler="blank">/</BRNCH> 
<NUMPG ID="23" Length="3" Alignment="left" Filler="blank">/</NUMPG> 
<STGRD ID="24" Length="2" Alignment="left" Filler="blank">/</STGRD> 
<KURSF_M ID="25" Length="10" Alignment="left" Filler="blank">/</KURSF_M> 
<AUGTX ID="26" Length="50" Alignment="left" Filler="blank">/</AUGTX> 
<XPRFG ID="27" Length="1" Alignment="left" Filler="blank">/</XPRFG> 
<XBWAE ID="28" Length="1" Alignment="left" Filler="blank">/</XBWAE> 
<LDGRP ID="29" Length="4" Alignment="left" Filler="blank">/</LDGRP> 
<PROPMANO ID="30" Length="13" Alignment="left" Filler="blank">/</PROPMANO> 
<VATDATE ID="31" Length="8" Alignment="left" Filler="blank">/</VATDATE> 
<SENDE ID="32" Length="1" Alignment="left" Filler="blank">/</SENDE> 
</header_elements> 
</xsl:template> 

<xsl:template name="header_1_elements"> 
<!-- KREDITOR SECTION --> 
<header_1_elements> 
<STYPE ID="1"	Length="1"	Alignment="left" Filler="blank">2</STYPE> 
<TBNAM ID="2"	Length="30"	Alignment="left" Filler="blank">BBSEG</TBNAM> 
<NEWBS ID="3"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:if test="type='I'">31</xsl:if> 
<xsl:if test="type='C'">21</xsl:if> 
</NEWBS> 
<DUMMYX	ID="4"	Length="10"	Alignment="left" Filler="blank">/</DUMMYX> 
<NEWUM	ID="5"	Length="1"	Alignment="left" Filler="blank">/</NEWUM> 
<NEWBK	ID="6"	Length="4"	Alignment="left" Filler="blank">/</NEWBK> 
<WRBTR	ID="7"	Length="16"	Alignment="left" Filler="blank"> 
<!--	<xsl:value-of select="format-number(totalAmount, '#0.00', 'european')"/>	--> 
<xsl:value-of select="format-number(totalAmount, '0000000000000,00', 'european')"/>	
</WRBTR> 
<DMBTR	ID="8"	Length="16"	Alignment="left" Filler="blank">/</DMBTR> 
<WMWST	ID="9"	Length="16"	Alignment="left" Filler="blank">/</WMWST> 
<MWSTS	ID="10"	Length="16"	Alignment="left" Filler="blank">/</MWSTS> 
<MWSKZ	ID="11"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:value-of select="financialTaxCode" /> 
</MWSKZ> 
<XSKRL	ID="12"	Length="1"	Alignment="left" Filler="blank">/</XSKRL> 
<FWZUZ	ID="13"	Length="16"	Alignment="left" Filler="blank">/</FWZUZ> 
<HWZUZ	ID="14"	Length="16"	Alignment="left" Filler="blank">/</HWZUZ> 
<GSBER	ID="15"	Length="4"	Alignment="left" Filler="blank">/</GSBER> 
<KOSTL	ID="16"	Length="10"	Alignment="left" Filler="blank">/</KOSTL> 
<DUMMY4	ID="17"	Length="4"	Alignment="left" Filler="blank">/</DUMMY4> 
<AUFNR	ID="18"	Length="12"	Alignment="left" Filler="blank">/</AUFNR> 
<EBELN	ID="19"	Length="10"	Alignment="left" Filler="blank">/</EBELN> 
<EBELP	ID="20"	Length="5"	Alignment="left" Filler="blank">/</EBELP> 
<PROJN	ID="21"	Length="16"	Alignment="left" Filler="blank">/</PROJN> 
<MATNR	ID="22"	Length="18"	Alignment="left" Filler="blank">/</MATNR> 
<WERKS	ID="23"	Length="4"	Alignment="left" Filler="blank">/</WERKS> 
<MENGE	ID="24"	Length="17"	Alignment="left" Filler="blank">/</MENGE> 
<MEINS	ID="25"	Length="3"	Alignment="left" Filler="blank">/</MEINS> 
<VBEL2	ID="26"	Length="10"	Alignment="left" Filler="blank">/</VBEL2> 
<POSN2	ID="27"	Length="6"	Alignment="left" Filler="blank">/</POSN2> 
<ETEN2	ID="28"	Length="4"	Alignment="left" Filler="blank">/</ETEN2> 
<PERNR	ID="29"	Length="8"	Alignment="left" Filler="blank">/</PERNR> 
<BEWAR	ID="30"	Length="3"	Alignment="left" Filler="blank">/</BEWAR> 
<VALUT	ID="31"	Length="8"	Alignment="left" Filler="blank">/</VALUT> 
<ZFBDT	ID="32"	Length="8"	Alignment="left" Filler="blank">/</ZFBDT> 
<ZINKZ	ID="33"	Length="2"	Alignment="left" Filler="blank">/</ZINKZ> 
<ZUONR	ID="34"	Length="18"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="invoiceText and string-length(invoiceText) &gt; 0"> 
<!-- <xsl:value-of select="invoiceText" /> --> 
<xsl:value-of select="normalize-space(invoiceText)"/> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</ZUONR> 
<FKONT	ID="35"	Length="3"	Alignment="left" Filler="blank">/</FKONT> 
<XAABG	ID="36"	Length="1"	Alignment="left" Filler="blank">/</XAABG> 
<SGTXT	ID="37"	Length="50"	Alignment="left" Filler="blank"> 
<!-- fa_period Month/fa_period Year Orderno projectno cost_type --> 
<xsl:variable name="twoDigitYear"> 
<xsl:value-of select="substring(financialPeriodYear, 3, 4)"/> 
</xsl:variable> 
<xsl:value-of select="$twoDigitYear" /> 
<xsl:value-of select="'/'" /> 
<xsl:value-of select="financialPeriodMonth" />	
<xsl:value-of select="' '" /> 
<xsl:value-of select="orderNumber" />	
</SGTXT> 
<BLNKZ	ID="38"	Length="2"	Alignment="left" Filler="blank">/</BLNKZ> 
<BLNBT	ID="39" Alignment="left" Filler="blank" Length="16">/</BLNBT> 
<BLNPZ	ID="40" Alignment="left" Filler="blank" Length="8">/</BLNPZ> 
<MABER	ID="41" Alignment="left" Filler="blank" Length="2">/</MABER> 
<SKFBT	ID="42" Alignment="left" Filler="blank" Length="16">/</SKFBT> 
<WSKTO	ID="43" Alignment="left" Filler="blank" Length="16">/</WSKTO> 
<ZTERM	ID="44" Alignment="left" Filler="blank" Length="4">/</ZTERM> 
<ZBD1T	ID="45" Alignment="left" Filler="blank" Length="3">/</ZBD1T> 
<ZBD1P	ID="46" Alignment="left" Filler="blank" Length="6">/</ZBD1P> 
<ZBD2T	ID="47" Alignment="left" Filler="blank" Length="3">/</ZBD2T> 
<ZBD2P	ID="48" Alignment="left" Filler="blank" Length="6">/</ZBD2P> 
<ZBD3T	ID="49" Alignment="left" Filler="blank" Length="3">/</ZBD3T> 
<ZLSPR	ID="50" Alignment="left" Filler="blank" Length="1">/</ZLSPR> 
<REBZG	ID="51" Alignment="left" Filler="blank" Length="10">/</REBZG> 
<REBZJ	ID="52" Alignment="left" Filler="blank" Length="4">/</REBZJ> 
<REBZZ	ID="53" Alignment="left" Filler="blank" Length="3">/</REBZZ> 
<ZLSCH	ID="54" Alignment="left" Filler="blank" Length="1">/</ZLSCH> 
<SAMNR	ID="55" Alignment="left" Filler="blank" Length="8">/</SAMNR> 
<ZBFIX	ID="56" Alignment="left" Filler="blank" Length="1">/</ZBFIX> 
<QSSKZ	ID="57" Alignment="left" Filler="blank" Length="2">/</QSSKZ> 
<QSSHB	ID="58" Alignment="left" Filler="blank" Length="16">/</QSSHB> 
<QSFBT	ID="59" Alignment="left" Filler="blank" Length="16">/</QSFBT> 
<ESRNR	ID="60" Alignment="left" Filler="blank" Length="11">/</ESRNR> 
<ESRPZ	ID="61" Alignment="left" Filler="blank" Length="2">/</ESRPZ> 
<ESRRE	ID="62" Alignment="left" Filler="blank" Length="27">/</ESRRE> 
<FDTAG	ID="63" Alignment="left" Filler="blank" Length="8">/</FDTAG> 
<FDLEV	ID="64" Alignment="left" Filler="blank" Length="2">/</FDLEV> 
<ANLN1	ID="65" Alignment="left" Filler="blank" Length="12">/</ANLN1> 
<ANLN2	ID="66" Alignment="left" Filler="blank" Length="4">/</ANLN2> 
<BZDAT	ID="67" Alignment="left" Filler="blank" Length="8">/</BZDAT> 
<ANBWA	ID="68" Alignment="left" Filler="blank" Length="3">/</ANBWA> 
<ABPER	ID="69" Alignment="left" Filler="blank" Length="7">/</ABPER> 
<GBETR	ID="70" Alignment="left" Filler="blank" Length="16">/</GBETR> 
<KURSR	ID="71" Alignment="left" Filler="blank" Length="10">/</KURSR> 
<MANSP	ID="72" Alignment="left" Filler="blank" Length="1">/</MANSP> 
<MSCHL	ID="73" Alignment="left" Filler="blank" Length="1">/</MSCHL> 
<HBKID	ID="74" Alignment="left" Filler="blank" Length="5">/</HBKID> 
<BVTYP	ID="75" Alignment="left" Filler="blank" Length="4">/</BVTYP> 
<ANFBN	ID="76" Alignment="left" Filler="blank" Length="10">/</ANFBN> 
<ANFBU	ID="77" Alignment="left" Filler="blank" Length="4">/</ANFBU> 
<ANFBJ	ID="78" Alignment="left" Filler="blank" Length="4">/</ANFBJ> 
<LZBKZ	ID="79" Alignment="left" Filler="blank" Length="3"> 
<xsl:choose> 
<xsl:when test="orderType='S'"> 
<xsl:value-of select="$constLZBZK_S_ORDER" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="$constLZBZK_OTHER" /> 
</xsl:otherwise> 
</xsl:choose> 
</LZBKZ> 
<LANDL	ID="80" Alignment="left" Filler="blank" Length="3">/</LANDL> 
<DIEKZ	ID="81" Alignment="left" Filler="blank" Length="1">/</DIEKZ> 
<ZOLLD	ID="82" Alignment="left" Filler="blank" Length="8">/</ZOLLD> 
<ZOLLT	ID="83" Alignment="left" Filler="blank" Length="8">/</ZOLLT> 
<VRSDT	ID="84" Alignment="left" Filler="blank" Length="8">/</VRSDT> 
<VRSKZ	ID="85" Alignment="left" Filler="blank" Length="1">/</VRSKZ> 
<HZUON	ID="86" Alignment="left" Filler="blank" Length="18">/</HZUON> 
<REGUL	ID="87" Alignment="left" Filler="blank" Length="1">/</REGUL> 
<NAME1	ID="88" Alignment="left" Filler="blank" Length="35">/</NAME1> 
<NAME2	ID="89" Alignment="left" Filler="blank" Length="35">/</NAME2> 
<NAME3	ID="90" Alignment="left" Filler="blank" Length="35">/</NAME3> 
<NAME4	ID="91" Alignment="left" Filler="blank" Length="35">/</NAME4> 
<STRAS	ID="92" Alignment="left" Filler="blank" Length="35">/</STRAS> 
<ORT01	ID="93" Alignment="left" Filler="blank" Length="35">/</ORT01> 
<PSTLZ	ID="94" Alignment="left" Filler="blank" Length="10">/</PSTLZ> 
<LAND1	ID="95" Alignment="left" Filler="blank" Length="3">/</LAND1> 
<REGIO	ID="96" Alignment="left" Filler="blank" Length="3">/</REGIO> 
<BANKL	ID="97" Alignment="left" Filler="blank" Length="15">/</BANKL> 
<BANKS	ID="98" Alignment="left" Filler="blank" Length="3">/</BANKS> 
<BANKN	ID="99" Alignment="left" Filler="blank" Length="18">/</BANKN> 
<BKONT	ID="100" Alignment="left" Filler="blank" Length="2">/</BKONT> 
<STCD1	ID="101" Alignment="left" Filler="blank" Length="16">/</STCD1> 
<STCD2	ID="102" Alignment="left" Filler="blank" Length="11">/</STCD2> 
<MADAT	ID="103" Alignment="left" Filler="blank" Length="8">/</MADAT> 
<MANST	ID="104" Alignment="left" Filler="blank" Length="1">/</MANST> 
<EGMLD	ID="105" Alignment="left" Filler="blank" Length="3">/</EGMLD> 
<DUMMY2	ID="106" Alignment="left" Filler="blank" Length="3">/</DUMMY2> 
<STCEG	ID="107" Alignment="left" Filler="blank" Length="20">/</STCEG> 
<STKZA	ID="108" Alignment="left" Filler="blank" Length="1">/</STKZA> 
<STKZU	ID="109" Alignment="left" Filler="blank" Length="1">/</STKZU> 
<PFACH	ID="110" Alignment="left" Filler="blank" Length="10">/</PFACH> 
<PSTL2	ID="111" Alignment="left" Filler="blank" Length="10">/</PSTL2> 
<SPRAS	ID="112" Alignment="left" Filler="blank" Length="1">/</SPRAS> 
<XINVE	ID="113" Alignment="left" Filler="blank" Length="1">/</XINVE> 
<NEWKO ID="114" Alignment="left" Filler="blank" Length="17"> 
<xsl:value-of select="vendorAccountNumber" /> 
</NEWKO> 
<NEWBW	Length="3"	Alignment="left" Filler="blank" ID="115">/</NEWBW> 
<KNRZE	Length="17"	Alignment="left" Filler="blank" ID="116">/</KNRZE> 
<HKONT	Length="10"	Alignment="left" Filler="blank" ID="117">/</HKONT> 
<PRCTR	Length="10"	Alignment="left" Filler="blank" ID="118">/</PRCTR> 
<VERTN	Length="13"	Alignment="left" Filler="blank" ID="119">/</VERTN> 
<VERTT	Length="1"	Alignment="left" Filler="blank" ID="120">/</VERTT> 
<VBEWA	Length="4"	Alignment="left" Filler="blank" ID="121">/</VBEWA> 
<HWBAS	Length="16"	Alignment="left" Filler="blank" ID="122">/</HWBAS> 
<FWBAS	Length="16"	Alignment="left" Filler="blank" ID="123">/</FWBAS> 
<FIPOS	Length="14"	Alignment="left" Filler="blank" ID="124">/</FIPOS> 
<VNAME	Length="6"	Alignment="left" Filler="blank" ID="125">/</VNAME> 
<EGRUP	Length="3"	Alignment="left" Filler="blank" ID="126">/</EGRUP> 
<BTYPE	Length="2"	Alignment="left" Filler="blank" ID="127">/</BTYPE> 
<PAOBJNR	Length="10"	Alignment="left" Filler="blank" ID="128">/</PAOBJNR> 
<KSTRG	Length="12"	Alignment="left" Filler="blank" ID="129">/</KSTRG> 
<IMKEY	Length="8"	Alignment="left" Filler="blank" ID="130">/</IMKEY> 
<DUMMY3	Length="8"	Alignment="left" Filler="blank" ID="131">/</DUMMY3> 
<VPTNR	Length="10"	Alignment="left" Filler="blank" ID="132">/</VPTNR> 
<NPLNR	Length="12"	Alignment="left" Filler="blank" ID="133">/</NPLNR> 
<VORNR	Length="4"	Alignment="left" Filler="blank" ID="134">/</VORNR> 
<XEGDR	Length="1"	Alignment="left" Filler="blank" ID="135">/</XEGDR> 
<RECID	Length="2"	Alignment="left" Filler="blank" ID="136">/</RECID> 
<PPRCT	Length="10"	Alignment="left" Filler="blank" ID="137">/</PPRCT> 
<PROJK	Length="24"	Alignment="left" Filler="blank" ID="138">/</PROJK> 
<UZAWE	Length="2"	Alignment="left" Filler="blank" ID="139">/</UZAWE> 
<TXJCD	Length="15"	Alignment="left" Filler="blank" ID="140">/</TXJCD> 
<FISTL	Length="16"	Alignment="left" Filler="blank" ID="141">/</FISTL> 
<GEBER	Length="10"	Alignment="left" Filler="blank" ID="142">/</GEBER> 
<DMBE2	Length="16"	Alignment="left" Filler="blank" ID="143">/</DMBE2> 
<DMBE3	Length="16"	Alignment="left" Filler="blank" ID="144">/</DMBE3> 
<PARGB	Length="4"	Alignment="left" Filler="blank" ID="145">/</PARGB> 
<XREF1	Length="12"	Alignment="left" Filler="blank" ID="146">/</XREF1> 
<XREF2	Length="12"	Alignment="left" Filler="blank" ID="147">/</XREF2> 
<KBLNR	Length="10"	Alignment="left" Filler="blank" ID="149">/</KBLNR> 
<KBLPOS	Length="3"	Alignment="left" Filler="blank" ID="150">/</KBLPOS> 
<WDATE	Length="8"	Alignment="left" Filler="blank" ID="151">/</WDATE> 
<WGBKZ	Length="1"	Alignment="left" Filler="blank" ID="152">/</WGBKZ> 
<XAKTZ	Length="1"	Alignment="left" Filler="blank" ID="153">/</XAKTZ> 
<WNAME	Length="30"	Alignment="left" Filler="blank" ID="154">/</WNAME> 
<WORT1	Length="30"	Alignment="left" Filler="blank" ID="155">/</WORT1> 
<WBZOG	Length="30"	Alignment="left" Filler="blank" ID="156">/</WBZOG> 
<WORT2	Length="30"	Alignment="left" Filler="blank" ID="157">/</WORT2> 
<WBANK	Length="60"	Alignment="left" Filler="blank" ID="158">/</WBANK> 
<WLZBP	Length="60"	Alignment="left" Filler="blank" ID="159">/</WLZBP> 
<DISKP	Length="8"	Alignment="left" Filler="blank" ID="160">/</DISKP> 
<DISKT	Length="3"	Alignment="left" Filler="blank" ID="161">/</DISKT> 
<WINFW	Length="16"	Alignment="left" Filler="blank" ID="162">/</WINFW> 
<WINHW	Length="16"	Alignment="left" Filler="blank" ID="163">/</WINHW> 
<WEVWV	Length="1"	Alignment="left" Filler="blank" ID="164">/</WEVWV> 
<WSTAT	Length="1"	Alignment="left" Filler="blank" ID="165">/</WSTAT> 
<WMWKZ	Length="2"	Alignment="left" Filler="blank" ID="166">/</WMWKZ> 
<WSTKZ	Length="1"	Alignment="left" Filler="blank" ID="167">/</WSTKZ> 
<RKE_ARTNR	Length="18"	Alignment="left" Filler="blank" ID="169">/</RKE_ARTNR> 
<RKE_BONUS	Length="2"	Alignment="left" Filler="blank" ID="170">/</RKE_BONUS> 
<RKE_BRSCH	Length="4"	Alignment="left" Filler="blank" ID="171">/</RKE_BRSCH> 
<RKE_BUKRS	Length="4"	Alignment="left" Filler="blank" ID="172">/</RKE_BUKRS> 
<RKE_BZIRK	Length="6"	Alignment="left" Filler="blank" ID="173">/</RKE_BZIRK> 
<RKE_EFORM	Length="5"	Alignment="left" Filler="blank" ID="174">/</RKE_EFORM> 
<RKE_FKART	Length="4"	Alignment="left" Filler="blank" ID="175">/</RKE_FKART> 
<RKE_GEBIE	Length="4"	Alignment="left" Filler="blank" ID="176">/</RKE_GEBIE> 
<RKE_GSBER	Length="4"	Alignment="left" Filler="blank" ID="177">/</RKE_GSBER> 
<RKE_KAUFN	Length="10"	Alignment="left" Filler="blank" ID="178">/</RKE_KAUFN> 
<RKE_KDGRP	Length="2"	Alignment="left" Filler="blank" ID="179">/</RKE_KDGRP> 
<RKE_KDPOS	Length="6"	Alignment="left" Filler="blank" ID="180">/</RKE_KDPOS> 
<RKE_KNDNR	Length="10"	Alignment="left" Filler="blank" ID="181">/</RKE_KNDNR> 
<RKE_KOKRS	Length="4"	Alignment="left" Filler="blank" ID="182">/</RKE_KOKRS> 
<RKE_KSTRG	Length="12"	Alignment="left" Filler="blank" ID="183">/</RKE_KSTRG> 
<RKE_LAND1	Length="3"	Alignment="left" Filler="blank" ID="184">/</RKE_LAND1> 
<RKE_MAABC	Length="1"	Alignment="left" Filler="blank" ID="185">/</RKE_MAABC> 
<RKE_MATKL	Length="9"	Alignment="left" Filler="blank" ID="186">/</RKE_MATKL> 
<RKE_PRCTR	Length="10"	Alignment="left" Filler="blank" ID="187">/</RKE_PRCTR> 
<RKE_PSPNR	Length="24"	Alignment="left" Filler="blank" ID="188">/</RKE_PSPNR> 
<RKE_RKAUFNR	Length="12"	Alignment="left" Filler="blank" ID="189">/</RKE_RKAUFNR> 
<RKE_SPART	Length="2"	Alignment="left" Filler="blank" ID="190">/</RKE_SPART> 
<RKE_VKBUR	Length="4"	Alignment="left" Filler="blank" ID="191">/</RKE_VKBUR> 
<RKE_VKGRP	Length="3"	Alignment="left" Filler="blank" ID="192">/</RKE_VKGRP> 
<RKE_VKORG	Length="4"	Alignment="left" Filler="blank" ID="193">/</RKE_VKORG> 
<RKE_VTWEG	Length="2"	Alignment="left" Filler="blank" ID="194">/</RKE_VTWEG> 
<RKE_WERKS	Length="4"	Alignment="left" Filler="blank" ID="195">/</RKE_WERKS> 
<RKE_KMBRND	Length="2"	Alignment="left" Filler="blank" ID="196">/</RKE_KMBRND> 
<RKE_KMCATG	Length="2"	Alignment="left" Filler="blank" ID="197">/</RKE_KMCATG> 
<RKE_KMHI01	Length="10"	Alignment="left" Filler="blank" ID="198">/</RKE_KMHI01> 
<RKE_KMHI02	Length="10"	Alignment="left" Filler="blank" ID="199">/</RKE_KMHI02> 
<RKE_KMHI03	Length="10"	Alignment="left" Filler="blank" ID="200">/</RKE_KMHI03> 
<RKE_KMKDGR	Length="2"	Alignment="left" Filler="blank" ID="201">/</RKE_KMKDGR> 
<RKE_KMLAND	Length="3"	Alignment="left" Filler="blank" ID="202">/</RKE_KMLAND> 
<RKE_KMMAKL	Length="9"	Alignment="left" Filler="blank" ID="203">/</RKE_KMMAKL> 
<RKE_KMNIEL	Length="2"	Alignment="left" Filler="blank" ID="204">/</RKE_KMNIEL> 
<RKE_KMSTGE	Length="2"	Alignment="left" Filler="blank" ID="205">/</RKE_KMSTGE> 
<RKE_KMVKBU	Length="4"	Alignment="left" Filler="blank" ID="206">/</RKE_KMVKBU> 
<RKE_KMVKGR	Length="3"	Alignment="left" Filler="blank" ID="207">/</RKE_KMVKGR> 
<RKE_KMVTNR	Length="8"	Alignment="left" Filler="blank" ID="208">/</RKE_KMVTNR> 
<RKE_PPRCTR	Length="10"	Alignment="left" Filler="blank" ID="209">/</RKE_PPRCTR> 

<!-- START new RKE-elements --> 
<RKE_WW005	Length="5"	Alignment="left" Filler="blank" ID="276">/</RKE_WW005> 
<RKE_WW006	Length="5"	Alignment="left" Filler="blank" ID="277">/</RKE_WW006> 
<RKE_WW007	Length="3"	Alignment="left" Filler="blank" ID="278">/</RKE_WW007> 
<RKE_WW008	Length="3"	Alignment="left" Filler="blank" ID="279">/</RKE_WW008> 
<RKE_WW009	Length="1"	Alignment="left" Filler="blank" ID="280">/</RKE_WW009> 
<RKE_WW010	Length="3"	Alignment="left" Filler="blank" ID="281">/</RKE_WW010> 
<RKE_WW011	Length="2"	Alignment="left" Filler="blank" ID="282">/</RKE_WW011> 
<RKE_WW012	Length="6"	Alignment="left" Filler="blank" ID="283">/</RKE_WW012> 
<RKE_WW013	Length="10"	Alignment="left" Filler="blank" ID="284">/</RKE_WW013> 
<RKE_WW015	Length="1"	Alignment="left" Filler="blank" ID="285">/</RKE_WW015> 
<RKE_WW016	Length="2"	Alignment="left" Filler="blank" ID="286">/</RKE_WW016> 
<RKE_WW017	Length="7"	Alignment="left" Filler="blank" ID="287">/</RKE_WW017> 
<RKE_WW019	Length="6"	Alignment="left" Filler="blank" ID="289">/</RKE_WW019> 
<!-- END new RKE-elements --> 

<VBUND	Length="6"	Alignment="left" Filler="blank" ID="210">/</VBUND> 
<FKBER	Length="4"	Alignment="left" Filler="blank" ID="211">/</FKBER> 
<DABRZ	Length="8"	Alignment="left" Filler="blank" ID="212">/</DABRZ> 
<XSTBA	Length="1"	Alignment="left" Filler="blank" ID="213">/</XSTBA> 

<!-- START Additional (empty) tags --> 
<RSTGR	Length="3" Alignment="left" Filler="blank" ID="214">/</RSTGR> 
<FIPEX	Length="24" Alignment="left" Filler="blank" ID="215">/</FIPEX> 
<XNEGP	Length="1" Alignment="left" Filler="blank" ID="216">/</XNEGP> 
<GRICD	Length="2" Alignment="left" Filler="blank" ID="217">/</GRICD> 
<GRIRG	Length="3" Alignment="left" Filler="blank" ID="218">/</GRIRG> 
<GITYP	Length="2" Alignment="left" Filler="blank" ID="219">/</GITYP> 
<FITYP	Length="2" Alignment="left" Filler="blank" ID="220">/</FITYP> 
<STCDT	Length="2" Alignment="left" Filler="blank" ID="221">/</STCDT> 
<STKZN	Length="1" Alignment="left" Filler="blank" ID="222">/</STKZN> 
<STCD3	Length="18" Alignment="left" Filler="blank" ID="223">/</STCD3> 
<STCD4	Length="18" Alignment="left" Filler="blank" ID="224">/</STCD4> 
<XREF3	Length="20" Alignment="left" Filler="blank" ID="225">/</XREF3> 
<KIDNO	Length="30" Alignment="left" Filler="blank" ID="226">/</KIDNO> 
<DTWS1	Length="2" Alignment="left" Filler="blank" ID="227">/</DTWS1> 
<DTWS2	Length="2" Alignment="left" Filler="blank" ID="228">/</DTWS2> 
<DTWS3	Length="2" Alignment="left" Filler="blank" ID="229">/</DTWS3> 
<DTWS4	Length="2" Alignment="left" Filler="blank" ID="230">/</DTWS4> 
<DTAWS	Length="2" Alignment="left" Filler="blank" ID="231">/</DTAWS> 
<PYCUR	Length="5" Alignment="left" Filler="blank" ID="232">/</PYCUR> 
<PYAMT	Length="16" Alignment="left" Filler="blank" ID="233">/</PYAMT> 
<BUPLA	Length="4" Alignment="left" Filler="blank" ID="234">/</BUPLA> 
<SECCO	Length="4" Alignment="left" Filler="blank" ID="235">/</SECCO> 
<LSTAR	Length="6" Alignment="left" Filler="blank" ID="236">/</LSTAR> 
<EGDEB	Length="10" Alignment="left" Filler="blank" ID="237">/</EGDEB> 
<WENR	Length="8" Alignment="left" Filler="blank" ID="238">/</WENR> 
<GENR	Length="8" Alignment="left" Filler="blank" ID="239">/</GENR> 
<GRNR	Length="8" Alignment="left" Filler="blank" ID="240">/</GRNR> 
<MENR	Length="8" Alignment="left" Filler="blank" ID="241">/</MENR> 
<MIVE	Length="13" Alignment="left" Filler="blank" ID="242">/</MIVE> 
<NKSL	Length="4" Alignment="left" Filler="blank" ID="243">/</NKSL> 
<EMPSL	Length="5" Alignment="left" Filler="blank" ID="244">/</EMPSL> 
<SVWNR	Length="13" Alignment="left" Filler="blank" ID="245">/</SVWNR> 
<SBERI	Length="10" Alignment="left" Filler="blank" ID="246">/</SBERI> 
<KKBER	Length="4" Alignment="left" Filler="blank" ID="247">/</KKBER> 
<EMPFB	Length="10" Alignment="left" Filler="blank" ID="248">/</EMPFB> 
<KURSR_M	Length="10" Alignment="left" Filler="blank" ID="249">/</KURSR_M> 
<J_1KFREPRE	Length="10" Alignment="left" Filler="blank" ID="250">/</J_1KFREPRE> 
<J_1KFTBUS	Length="30" Alignment="left" Filler="blank" ID="251">/</J_1KFTBUS> 
<J_1KFTIND	Length="30" Alignment="left" Filler="blank" ID="252">/</J_1KFTIND> 
<IDXSP	Length="5" Alignment="left" Filler="blank" ID="253">/</IDXSP> 
<ANRED	Length="15" Alignment="left" Filler="blank" ID="254">/</ANRED> 
<RECNNR	Length="13" Alignment="left" Filler="blank" ID="255">/</RECNNR> 
<E_MIVE	Length="13" Alignment="left" Filler="blank" ID="256">/</E_MIVE> 
<BKREF	Length="20" Alignment="left" Filler="blank" ID="257">/</BKREF> 
<DTAMS	Length="1" Alignment="left" Filler="blank" ID="258">/</DTAMS> 
<CESSION_KZ	Length="2" Alignment="left" Filler="blank" ID="259">/</CESSION_KZ> 
<GRANT_NBR	Length="20" Alignment="left" Filler="blank" ID="260">/</GRANT_NBR> 
<FKBER_LONG	Length="16" Alignment="left" Filler="blank" ID="261">/</FKBER_LONG> 
<ERLKZ	Length="1" Alignment="left" Filler="blank" ID="262">/</ERLKZ> 
<IBAN	Length="34" Alignment="left" Filler="blank" ID="263">/</IBAN> 
<VALID_FROM	Length="8" Alignment="left" Filler="blank" ID="264">/</VALID_FROM> 
<SEGMENT	Length="10" Alignment="left" Filler="blank" ID="265">/</SEGMENT> 
<PSEGMENT	Length="10" Alignment="left" Filler="blank" ID="266">/</PSEGMENT> 
<HKTID	Length="5" Alignment="left" Filler="blank" ID="267">/</HKTID> 
<XSIWE	Length="1" Alignment="left" Filler="blank" ID="268">/</XSIWE> 
<TCNO	Length="16" Alignment="left" Filler="blank" ID="269">/</TCNO> 
<DATEOFSERVICE	Length="8" Alignment="left" Filler="blank" ID="270">/</DATEOFSERVICE> 
<NOTAXCORR	Length="1" Alignment="left" Filler="blank" ID="271">/</NOTAXCORR> 
<DIFFOPTRATE	Length="10" Alignment="left" Filler="blank" ID="272">/</DIFFOPTRATE> 
<HASDIFFOPTRATE	Length="1" Alignment="left" Filler="blank" ID="273">/</HASDIFFOPTRATE> 
<SENDE	Length="1" Alignment="left" Filler="blank" ID="274">/</SENDE> 
<PRODPER	Length="8" Alignment="left" Filler="blank" ID="275">/</PRODPER> 
<!-- END Additional tags --> 

</header_1_elements> 
</xsl:template> 

<xsl:template name="invoiceCharges_1_elements"> 
<invoiceCharges_1_elements> 
<STYPE ID="1"	Length="1"	Alignment="left" Filler="blank">2</STYPE> 
<TBNAM ID="2"	Length="30"	Alignment="left" Filler="blank">BBSEG</TBNAM> 
<NEWBS ID="3"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:if test="parent::node()/type='I'">40</xsl:if> 
<xsl:if test="parent::node()/type='C'">50</xsl:if> 
</NEWBS> 
<DUMMYX	ID="4"	Length="10"	Alignment="left" Filler="blank">/</DUMMYX> 
<NEWUM	ID="5"	Length="1"	Alignment="left" Filler="blank">/</NEWUM> 
<NEWBK	ID="6"	Length="4"	Alignment="left" Filler="blank">/</NEWBK> 
<WRBTR	ID="7"	Length="16"	Alignment="left" Filler="blank"> 
<!--	<xsl:value-of select="format-number(totalAmount, '#0.00', 'european')"/>	--> 
<xsl:value-of select="format-number(totalAmount, '0000000000000,00', 'european')"/>	
</WRBTR> 
<DMBTR	ID="8"	Length="16"	Alignment="left" Filler="blank">/</DMBTR> 
<WMWST	ID="9"	Length="16"	Alignment="left" Filler="blank">/</WMWST> 
<MWSTS	ID="10"	Length="16"	Alignment="left" Filler="blank">/</MWSTS> 
<MWSKZ	ID="11"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:value-of select="parent::node()/financialTaxCode" /> 
</MWSKZ> 
<XSKRL	ID="12"	Length="1"	Alignment="left" Filler="blank">/</XSKRL> 
<FWZUZ	ID="13"	Length="16"	Alignment="left" Filler="blank">/</FWZUZ> 
<HWZUZ	ID="14"	Length="16"	Alignment="left" Filler="blank">/</HWZUZ> 
<GSBER	ID="15"	Length="4"	Alignment="left" Filler="blank">/</GSBER> 
<KOSTL	ID="16"	Length="10"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="financialCostcenter and string-length(financialCostcenter) &gt; 0"> 
<xsl:value-of select="financialCostcenter" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</KOSTL> 
<DUMMY4	ID="17"	Length="4"	Alignment="left" Filler="blank">/</DUMMY4> 
<AUFNR	ID="18"	Length="12"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="costcenter2 and string-length(costcenter2) &gt; 0"> 
<xsl:value-of select="costcenter2" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</AUFNR> 
<EBELN	ID="19"	Length="10"	Alignment="left" Filler="blank">/</EBELN> 
<EBELP	ID="20"	Length="5"	Alignment="left" Filler="blank">/</EBELP> 
<PROJN	ID="21"	Length="16"	Alignment="left" Filler="blank">/</PROJN> 
<MATNR	ID="22"	Length="18"	Alignment="left" Filler="blank">/</MATNR> 
<WERKS	ID="23"	Length="4"	Alignment="left" Filler="blank">/</WERKS> 
<MENGE	ID="24"	Length="17"	Alignment="left" Filler="blank">/</MENGE> 
<MEINS	ID="25"	Length="3"	Alignment="left" Filler="blank">/</MEINS> 
<VBEL2	ID="26"	Length="10"	Alignment="left" Filler="blank">/</VBEL2> 
<POSN2	ID="27"	Length="6"	Alignment="left" Filler="blank">/</POSN2> 
<ETEN2	ID="28"	Length="4"	Alignment="left" Filler="blank">/</ETEN2> 
<PERNR	ID="29"	Length="8"	Alignment="left" Filler="blank">/</PERNR> 
<BEWAR	ID="30"	Length="3"	Alignment="left" Filler="blank">/</BEWAR> 
<VALUT	ID="31"	Length="8"	Alignment="left" Filler="blank">/</VALUT> 
<ZFBDT	ID="32"	Length="8"	Alignment="left" Filler="blank">/</ZFBDT> 
<ZINKZ	ID="33"	Length="2"	Alignment="left" Filler="blank">/</ZINKZ> 
<ZUONR	ID="34"	Length="18"	Alignment="left" Filler="blank"> 
<xsl:value-of select="orderNumber" /> 
</ZUONR> 
<FKONT	ID="35"	Length="3"	Alignment="left" Filler="blank">/</FKONT> 
<XAABG	ID="36"	Length="1"	Alignment="left" Filler="blank">/</XAABG> 
<SGTXT	ID="37"	Length="50"	Alignment="left" Filler="blank"> 
<xsl:variable name="twoDigitYear"> 
<xsl:value-of select="substring(parent::node()/financialPeriodYear, 3, 4)"/> 
</xsl:variable> 
<xsl:value-of select="$twoDigitYear" /> 
<xsl:value-of select="'/'" /> 
<xsl:value-of select="parent::node()/financialPeriodMonth" /> 
</SGTXT> 
<BLNKZ	ID="38"	Length="2"	Alignment="left" Filler="blank">/</BLNKZ> 
<BLNBT	ID="39" Alignment="left" Filler="blank" Length="16">/</BLNBT> 
<BLNPZ	ID="40" Alignment="left" Filler="blank" Length="8">/</BLNPZ> 
<MABER	ID="41" Alignment="left" Filler="blank" Length="2">/</MABER> 
<SKFBT	ID="42" Alignment="left" Filler="blank" Length="16">/</SKFBT> 
<WSKTO	ID="43" Alignment="left" Filler="blank" Length="16">/</WSKTO> 
<ZTERM	ID="44" Alignment="left" Filler="blank" Length="4">/</ZTERM> 
<ZBD1T	ID="45" Alignment="left" Filler="blank" Length="3">/</ZBD1T> 
<ZBD1P	ID="46" Alignment="left" Filler="blank" Length="6">/</ZBD1P> 
<ZBD2T	ID="47" Alignment="left" Filler="blank" Length="3">/</ZBD2T> 
<ZBD2P	ID="48" Alignment="left" Filler="blank" Length="6">/</ZBD2P> 
<ZBD3T	ID="49" Alignment="left" Filler="blank" Length="3">/</ZBD3T> 
<ZLSPR	ID="50" Alignment="left" Filler="blank" Length="1">/</ZLSPR> 
<REBZG	ID="51" Alignment="left" Filler="blank" Length="10">/</REBZG> 
<REBZJ	ID="52" Alignment="left" Filler="blank" Length="4">/</REBZJ> 
<REBZZ	ID="53" Alignment="left" Filler="blank" Length="3">/</REBZZ> 
<ZLSCH	ID="54" Alignment="left" Filler="blank" Length="1">/</ZLSCH> 
<SAMNR	ID="55" Alignment="left" Filler="blank" Length="8">/</SAMNR> 
<ZBFIX	ID="56" Alignment="left" Filler="blank" Length="1">/</ZBFIX> 
<QSSKZ	ID="57" Alignment="left" Filler="blank" Length="2">/</QSSKZ> 
<QSSHB	ID="58" Alignment="left" Filler="blank" Length="16">/</QSSHB> 
<QSFBT	ID="59" Alignment="left" Filler="blank" Length="16">/</QSFBT> 
<ESRNR	ID="60" Alignment="left" Filler="blank" Length="11">/</ESRNR> 
<ESRPZ	ID="61" Alignment="left" Filler="blank" Length="2">/</ESRPZ> 
<ESRRE	ID="62" Alignment="left" Filler="blank" Length="27">/</ESRRE> 
<FDTAG	ID="63" Alignment="left" Filler="blank" Length="8">/</FDTAG> 
<FDLEV	ID="64" Alignment="left" Filler="blank" Length="2">/</FDLEV> 
<ANLN1	ID="65" Alignment="left" Filler="blank" Length="12">/</ANLN1> 
<ANLN2	ID="66" Alignment="left" Filler="blank" Length="4">/</ANLN2> 
<BZDAT	ID="67" Alignment="left" Filler="blank" Length="8">/</BZDAT> 
<ANBWA	ID="68" Alignment="left" Filler="blank" Length="3">/</ANBWA> 
<ABPER	ID="69" Alignment="left" Filler="blank" Length="7">/</ABPER> 
<GBETR	ID="70" Alignment="left" Filler="blank" Length="16">/</GBETR> 
<KURSR	ID="71" Alignment="left" Filler="blank" Length="10">/</KURSR> 
<MANSP	ID="72" Alignment="left" Filler="blank" Length="1">/</MANSP> 
<MSCHL	ID="73" Alignment="left" Filler="blank" Length="1">/</MSCHL> 
<HBKID	ID="74" Alignment="left" Filler="blank" Length="5">/</HBKID> 
<BVTYP	ID="75" Alignment="left" Filler="blank" Length="4">/</BVTYP> 
<ANFBN	ID="76" Alignment="left" Filler="blank" Length="10">/</ANFBN> 
<ANFBU	ID="77" Alignment="left" Filler="blank" Length="4">/</ANFBU> 
<ANFBJ	ID="78" Alignment="left" Filler="blank" Length="4">/</ANFBJ> 
<LZBKZ	ID="79" Alignment="left" Filler="blank" Length="3"> 
<xsl:choose> 
<xsl:when test="parent::node()/orderType='S'"> 
<xsl:value-of select="$constLZBZK_S_ORDER" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="$constLZBZK_OTHER" /> 
</xsl:otherwise> 
</xsl:choose> 
</LZBKZ> 
<LANDL	ID="80" Alignment="left" Filler="blank" Length="3">/</LANDL> 
<DIEKZ	ID="81" Alignment="left" Filler="blank" Length="1">/</DIEKZ> 
<ZOLLD	ID="82" Alignment="left" Filler="blank" Length="8">/</ZOLLD> 
<ZOLLT	ID="83" Alignment="left" Filler="blank" Length="8">/</ZOLLT> 
<VRSDT	ID="84" Alignment="left" Filler="blank" Length="8">/</VRSDT> 
<VRSKZ	ID="85" Alignment="left" Filler="blank" Length="1">/</VRSKZ> 
<HZUON	ID="86" Alignment="left" Filler="blank" Length="18">/</HZUON> 
<REGUL	ID="87" Alignment="left" Filler="blank" Length="1">/</REGUL> 
<NAME1	ID="88" Alignment="left" Filler="blank" Length="35">/</NAME1> 
<NAME2	ID="89" Alignment="left" Filler="blank" Length="35">/</NAME2> 
<NAME3	ID="90" Alignment="left" Filler="blank" Length="35">/</NAME3> 
<NAME4	ID="91" Alignment="left" Filler="blank" Length="35">/</NAME4> 
<STRAS	ID="92" Alignment="left" Filler="blank" Length="35">/</STRAS> 
<ORT01	ID="93" Alignment="left" Filler="blank" Length="35">/</ORT01> 
<PSTLZ	ID="94" Alignment="left" Filler="blank" Length="10">/</PSTLZ> 
<LAND1	ID="95" Alignment="left" Filler="blank" Length="3">/</LAND1> 
<REGIO	ID="96" Alignment="left" Filler="blank" Length="3">/</REGIO> 
<BANKL	ID="97" Alignment="left" Filler="blank" Length="15">/</BANKL> 
<BANKS	ID="98" Alignment="left" Filler="blank" Length="3">/</BANKS> 
<BANKN	ID="99" Alignment="left" Filler="blank" Length="18">/</BANKN> 
<BKONT	ID="100" Alignment="left" Filler="blank" Length="2">/</BKONT> 
<STCD1	ID="101" Alignment="left" Filler="blank" Length="16">/</STCD1> 
<STCD2	ID="102" Alignment="left" Filler="blank" Length="11">/</STCD2> 
<MADAT	ID="103" Alignment="left" Filler="blank" Length="8">/</MADAT> 
<MANST	ID="104" Alignment="left" Filler="blank" Length="1">/</MANST> 
<EGMLD	ID="105" Alignment="left" Filler="blank" Length="3">/</EGMLD> 
<DUMMY2	ID="106" Alignment="left" Filler="blank" Length="3">/</DUMMY2> 
<STCEG	ID="107" Alignment="left" Filler="blank" Length="20">/</STCEG> 
<STKZA	ID="108" Alignment="left" Filler="blank" Length="1">/</STKZA> 
<STKZU	ID="109" Alignment="left" Filler="blank" Length="1">/</STKZU> 
<PFACH	ID="110" Alignment="left" Filler="blank" Length="10">/</PFACH> 
<PSTL2	ID="111" Alignment="left" Filler="blank" Length="10">/</PSTL2> 
<SPRAS	ID="112" Alignment="left" Filler="blank" Length="1">/</SPRAS> 
<XINVE	ID="113" Alignment="left" Filler="blank" Length="1">/</XINVE> 
<NEWKO ID="114" Length="17" Alignment="left" Filler="blank"> 
<xsl:value-of select="accountNumber" /> 
</NEWKO> 
<NEWBW	Length="3"	Alignment="left" Filler="blank" ID="115">/</NEWBW> 
<KNRZE	Length="17"	Alignment="left" Filler="blank" ID="116">/</KNRZE> 
<HKONT	Length="10"	Alignment="left" Filler="blank" ID="117">/</HKONT> 
<PRCTR	Length="10"	Alignment="left" Filler="blank" ID="118">/</PRCTR> 
<VERTN	Length="13"	Alignment="left" Filler="blank" ID="119">/</VERTN> 
<VERTT	Length="1"	Alignment="left" Filler="blank" ID="120">/</VERTT> 
<VBEWA	Length="4"	Alignment="left" Filler="blank" ID="121">/</VBEWA> 
<HWBAS	Length="16"	Alignment="left" Filler="blank" ID="122">/</HWBAS> 
<FWBAS	Length="16"	Alignment="left" Filler="blank" ID="123">/</FWBAS> 
<FIPOS	Length="14"	Alignment="left" Filler="blank" ID="124">/</FIPOS> 
<VNAME	Length="6"	Alignment="left" Filler="blank" ID="125">/</VNAME> 
<EGRUP	Length="3"	Alignment="left" Filler="blank" ID="126">/</EGRUP> 
<BTYPE	Length="2"	Alignment="left" Filler="blank" ID="127">/</BTYPE> 
<PAOBJNR	Length="10"	Alignment="left" Filler="blank" ID="128">/</PAOBJNR> 
<KSTRG	Length="12"	Alignment="left" Filler="blank" ID="129">/</KSTRG> 
<IMKEY	Length="8"	Alignment="left" Filler="blank" ID="130">/</IMKEY> 
<DUMMY3	Length="8"	Alignment="left" Filler="blank" ID="131">/</DUMMY3> 
<VPTNR	Length="10"	Alignment="left" Filler="blank" ID="132">/</VPTNR> 
<NPLNR	Length="12"	Alignment="left" Filler="blank" ID="133">/</NPLNR> 
<VORNR	Length="4"	Alignment="left" Filler="blank" ID="134">/</VORNR> 
<XEGDR	Length="1"	Alignment="left" Filler="blank" ID="135">/</XEGDR> 
<RECID	Length="2"	Alignment="left" Filler="blank" ID="136">/</RECID> 
<PPRCT	Length="10"	Alignment="left" Filler="blank" ID="137">/</PPRCT> 
<PROJK	Length="24"	Alignment="left" Filler="blank" ID="138">/</PROJK> 
<UZAWE	Length="2"	Alignment="left" Filler="blank" ID="139">/</UZAWE> 
<TXJCD	Length="15"	Alignment="left" Filler="blank" ID="140">/</TXJCD> 
<FISTL	Length="16"	Alignment="left" Filler="blank" ID="141">/</FISTL> 
<GEBER	Length="10"	Alignment="left" Filler="blank" ID="142">/</GEBER> 
<DMBE2	Length="16"	Alignment="left" Filler="blank" ID="143">/</DMBE2> 
<DMBE3	Length="16"	Alignment="left" Filler="blank" ID="144">/</DMBE3> 
<PARGB	Length="4"	Alignment="left" Filler="blank" ID="145">/</PARGB> 
<XREF1	Length="12"	Alignment="left" Filler="blank" ID="146">/</XREF1> 
<XREF2	Length="12"	Alignment="left" Filler="blank" ID="147">/</XREF2> 
<KBLNR	Length="10"	Alignment="left" Filler="blank" ID="149">/</KBLNR> 
<KBLPOS	Length="3"	Alignment="left" Filler="blank" ID="150">/</KBLPOS> 
<WDATE	Length="8"	Alignment="left" Filler="blank" ID="151">/</WDATE> 
<WGBKZ	Length="1"	Alignment="left" Filler="blank" ID="152">/</WGBKZ> 
<XAKTZ	Length="1"	Alignment="left" Filler="blank" ID="153">/</XAKTZ> 
<WNAME	Length="30"	Alignment="left" Filler="blank" ID="154">/</WNAME> 
<WORT1	Length="30"	Alignment="left" Filler="blank" ID="155">/</WORT1> 
<WBZOG	Length="30"	Alignment="left" Filler="blank" ID="156">/</WBZOG> 
<WORT2	Length="30"	Alignment="left" Filler="blank" ID="157">/</WORT2> 
<WBANK	Length="60"	Alignment="left" Filler="blank" ID="158">/</WBANK> 
<WLZBP	Length="60"	Alignment="left" Filler="blank" ID="159">/</WLZBP> 
<DISKP	Length="8"	Alignment="left" Filler="blank" ID="160">/</DISKP> 
<DISKT	Length="3"	Alignment="left" Filler="blank" ID="161">/</DISKT> 
<WINFW	Length="16"	Alignment="left" Filler="blank" ID="162">/</WINFW> 
<WINHW	Length="16"	Alignment="left" Filler="blank" ID="163">/</WINHW> 
<WEVWV	Length="1"	Alignment="left" Filler="blank" ID="164">/</WEVWV> 
<WSTAT	Length="1"	Alignment="left" Filler="blank" ID="165">/</WSTAT> 
<WMWKZ	Length="2"	Alignment="left" Filler="blank" ID="166">/</WMWKZ> 
<WSTKZ	Length="1"	Alignment="left" Filler="blank" ID="167">/</WSTKZ> 
<RKE_ARTNR	Length="18"	Alignment="left" Filler="blank" ID="169">/</RKE_ARTNR> 
<RKE_BONUS	Length="2"	Alignment="left" Filler="blank" ID="170">/</RKE_BONUS> 
<RKE_BRSCH	Length="4"	Alignment="left" Filler="blank" ID="171">/</RKE_BRSCH> 
<RKE_BUKRS	Length="4"	Alignment="left" Filler="blank" ID="172">/</RKE_BUKRS> 
<RKE_BZIRK	Length="6"	Alignment="left" Filler="blank" ID="173">/</RKE_BZIRK> 
<RKE_EFORM	Length="5"	Alignment="left" Filler="blank" ID="174">/</RKE_EFORM> 
<RKE_FKART	Length="4"	Alignment="left" Filler="blank" ID="175">/</RKE_FKART> 
<RKE_GEBIE	Length="4"	Alignment="left" Filler="blank" ID="176">/</RKE_GEBIE> 
<RKE_GSBER	Length="4"	Alignment="left" Filler="blank" ID="177">/</RKE_GSBER> 
<RKE_KAUFN	Length="10"	Alignment="left" Filler="blank" ID="178">/</RKE_KAUFN> 
<RKE_KDGRP	Length="2"	Alignment="left" Filler="blank" ID="179">/</RKE_KDGRP> 
<RKE_KDPOS	Length="6"	Alignment="left" Filler="blank" ID="180">/</RKE_KDPOS> 
<RKE_KNDNR	Length="10"	Alignment="left" Filler="blank" ID="181">/</RKE_KNDNR> 
<RKE_KOKRS	Length="4"	Alignment="left" Filler="blank" ID="182">/</RKE_KOKRS> 
<RKE_KSTRG	Length="12"	Alignment="left" Filler="blank" ID="183">/</RKE_KSTRG> 
<RKE_LAND1	Length="3"	Alignment="left" Filler="blank" ID="184">/</RKE_LAND1> 
<RKE_MAABC	Length="1"	Alignment="left" Filler="blank" ID="185">/</RKE_MAABC> 
<RKE_MATKL	Length="9"	Alignment="left" Filler="blank" ID="186">/</RKE_MATKL> 
<RKE_PRCTR	Length="10"	Alignment="left" Filler="blank" ID="187">/</RKE_PRCTR> 
<RKE_PSPNR	Length="24"	Alignment="left" Filler="blank" ID="188">/</RKE_PSPNR> 
<RKE_RKAUFNR	Length="12"	Alignment="left" Filler="blank" ID="189">/</RKE_RKAUFNR> 
<RKE_SPART	Length="2"	Alignment="left" Filler="blank" ID="190">/</RKE_SPART> 
<RKE_VKBUR	Length="4"	Alignment="left" Filler="blank" ID="191">/</RKE_VKBUR> 
<RKE_VKGRP	Length="3"	Alignment="left" Filler="blank" ID="192">/</RKE_VKGRP> 
<RKE_VKORG	Length="4"	Alignment="left" Filler="blank" ID="193">/</RKE_VKORG> 
<RKE_VTWEG	Length="2"	Alignment="left" Filler="blank" ID="194">/</RKE_VTWEG> 
<RKE_WERKS	Length="4"	Alignment="left" Filler="blank" ID="195">/</RKE_WERKS> 
<RKE_KMBRND	Length="2"	Alignment="left" Filler="blank" ID="196">/</RKE_KMBRND> 
<RKE_KMCATG	Length="2"	Alignment="left" Filler="blank" ID="197">/</RKE_KMCATG> 
<RKE_KMHI01	Length="10"	Alignment="left" Filler="blank" ID="198">/</RKE_KMHI01> 
<RKE_KMHI02	Length="10"	Alignment="left" Filler="blank" ID="199">/</RKE_KMHI02> 
<RKE_KMHI03	Length="10"	Alignment="left" Filler="blank" ID="200">/</RKE_KMHI03> 
<RKE_KMKDGR	Length="2"	Alignment="left" Filler="blank" ID="201">/</RKE_KMKDGR> 
<RKE_KMLAND	Length="3"	Alignment="left" Filler="blank" ID="202">/</RKE_KMLAND> 
<RKE_KMMAKL	Length="9"	Alignment="left" Filler="blank" ID="203">/</RKE_KMMAKL> 
<RKE_KMNIEL	Length="2"	Alignment="left" Filler="blank" ID="204">/</RKE_KMNIEL> 
<RKE_KMSTGE	Length="2"	Alignment="left" Filler="blank" ID="205">/</RKE_KMSTGE> 
<RKE_KMVKBU	Length="4"	Alignment="left" Filler="blank" ID="206">/</RKE_KMVKBU> 
<RKE_KMVKGR	Length="3"	Alignment="left" Filler="blank" ID="207">/</RKE_KMVKGR> 
<RKE_KMVTNR	Length="8"	Alignment="left" Filler="blank" ID="208">/</RKE_KMVTNR> 
<RKE_PPRCTR	Length="10"	Alignment="left" Filler="blank" ID="209">/</RKE_PPRCTR> 

<!-- START new RKE-elements --> 
<RKE_WW005	Length="5"	Alignment="left" Filler="blank" ID="276">/</RKE_WW005> 
<RKE_WW006	Length="5"	Alignment="left" Filler="blank" ID="277">/</RKE_WW006> 
<RKE_WW007	Length="3"	Alignment="left" Filler="blank" ID="278">/</RKE_WW007> 
<RKE_WW008	Length="3"	Alignment="left" Filler="blank" ID="279">/</RKE_WW008> 
<RKE_WW009	Length="1"	Alignment="left" Filler="blank" ID="280">/</RKE_WW009> 
<RKE_WW010	Length="3"	Alignment="left" Filler="blank" ID="281">/</RKE_WW010> 
<RKE_WW011	Length="2"	Alignment="left" Filler="blank" ID="282">/</RKE_WW011> 
<RKE_WW012	Length="6"	Alignment="left" Filler="blank" ID="283">/</RKE_WW012> 
<RKE_WW013	Length="10"	Alignment="left" Filler="blank" ID="284">/</RKE_WW013> 
<RKE_WW015	Length="1"	Alignment="left" Filler="blank" ID="285">/</RKE_WW015> 
<RKE_WW016	Length="2"	Alignment="left" Filler="blank" ID="286">/</RKE_WW016> 
<RKE_WW017	Length="7"	Alignment="left" Filler="blank" ID="287">/</RKE_WW017> 
<RKE_WW019	Length="6"	Alignment="left" Filler="blank" ID="289">/</RKE_WW019> 
<!-- END new RKE-elements --> 

<VBUND	Length="6"	Alignment="left" Filler="blank" ID="210">/</VBUND> 
<FKBER	Length="4"	Alignment="left" Filler="blank" ID="211">/</FKBER> 
<DABRZ	Length="8"	Alignment="left" Filler="blank" ID="212">/</DABRZ> 
<XSTBA	Length="1"	Alignment="left" Filler="blank" ID="213">/</XSTBA> 

<!-- Additional (empty) tags start --> 
<RSTGR	Length="3" Alignment="left" Filler="blank" ID="214">/</RSTGR> 
<FIPEX	Length="24" Alignment="left" Filler="blank" ID="215">/</FIPEX> 
<XNEGP	Length="1" Alignment="left" Filler="blank" ID="216">/</XNEGP> 
<GRICD	Length="2" Alignment="left" Filler="blank" ID="217">/</GRICD> 
<GRIRG	Length="3" Alignment="left" Filler="blank" ID="218">/</GRIRG> 
<GITYP	Length="2" Alignment="left" Filler="blank" ID="219">/</GITYP> 
<FITYP	Length="2" Alignment="left" Filler="blank" ID="220">/</FITYP> 
<STCDT	Length="2" Alignment="left" Filler="blank" ID="221">/</STCDT> 
<STKZN	Length="1" Alignment="left" Filler="blank" ID="222">/</STKZN> 
<STCD3	Length="18" Alignment="left" Filler="blank" ID="223">/</STCD3> 
<STCD4	Length="18" Alignment="left" Filler="blank" ID="224">/</STCD4> 
<XREF3	Length="20" Alignment="left" Filler="blank" ID="225">/</XREF3> 
<KIDNO	Length="30" Alignment="left" Filler="blank" ID="226">/</KIDNO> 
<DTWS1	Length="2" Alignment="left" Filler="blank" ID="227">/</DTWS1> 
<DTWS2	Length="2" Alignment="left" Filler="blank" ID="228">/</DTWS2> 
<DTWS3	Length="2" Alignment="left" Filler="blank" ID="229">/</DTWS3> 
<DTWS4	Length="2" Alignment="left" Filler="blank" ID="230">/</DTWS4> 
<DTAWS	Length="2" Alignment="left" Filler="blank" ID="231">/</DTAWS> 
<PYCUR	Length="5" Alignment="left" Filler="blank" ID="232">/</PYCUR> 
<PYAMT	Length="16" Alignment="left" Filler="blank" ID="233">/</PYAMT> 
<BUPLA	Length="4" Alignment="left" Filler="blank" ID="234">/</BUPLA> 
<SECCO	Length="4" Alignment="left" Filler="blank" ID="235">/</SECCO> 
<LSTAR	Length="6" Alignment="left" Filler="blank" ID="236">/</LSTAR> 
<EGDEB	Length="10" Alignment="left" Filler="blank" ID="237">/</EGDEB> 
<WENR	Length="8" Alignment="left" Filler="blank" ID="238">/</WENR> 
<GENR	Length="8" Alignment="left" Filler="blank" ID="239">/</GENR> 
<GRNR	Length="8" Alignment="left" Filler="blank" ID="240">/</GRNR> 
<MENR	Length="8" Alignment="left" Filler="blank" ID="241">/</MENR> 
<MIVE	Length="13" Alignment="left" Filler="blank" ID="242">/</MIVE> 
<NKSL	Length="4" Alignment="left" Filler="blank" ID="243">/</NKSL> 
<EMPSL	Length="5" Alignment="left" Filler="blank" ID="244">/</EMPSL> 
<SVWNR	Length="13" Alignment="left" Filler="blank" ID="245">/</SVWNR> 
<SBERI	Length="10" Alignment="left" Filler="blank" ID="246">/</SBERI> 
<KKBER	Length="4" Alignment="left" Filler="blank" ID="247">/</KKBER> 
<EMPFB	Length="10" Alignment="left" Filler="blank" ID="248">/</EMPFB> 
<KURSR_M	Length="10" Alignment="left" Filler="blank" ID="249">/</KURSR_M> 
<J_1KFREPRE	Length="10" Alignment="left" Filler="blank" ID="250">/</J_1KFREPRE> 
<J_1KFTBUS	Length="30" Alignment="left" Filler="blank" ID="251">/</J_1KFTBUS> 
<J_1KFTIND	Length="30" Alignment="left" Filler="blank" ID="252">/</J_1KFTIND> 
<IDXSP	Length="5" Alignment="left" Filler="blank" ID="253">/</IDXSP> 
<ANRED	Length="15" Alignment="left" Filler="blank" ID="254">/</ANRED> 
<RECNNR	Length="13" Alignment="left" Filler="blank" ID="255">/</RECNNR> 
<E_MIVE	Length="13" Alignment="left" Filler="blank" ID="256">/</E_MIVE> 
<BKREF	Length="20" Alignment="left" Filler="blank" ID="257">/</BKREF> 
<DTAMS	Length="1" Alignment="left" Filler="blank" ID="258">/</DTAMS> 
<CESSION_KZ	Length="2" Alignment="left" Filler="blank" ID="259">/</CESSION_KZ> 
<GRANT_NBR	Length="20" Alignment="left" Filler="blank" ID="260">/</GRANT_NBR> 
<FKBER_LONG	Length="16" Alignment="left" Filler="blank" ID="261">/</FKBER_LONG> 
<ERLKZ	Length="1" Alignment="left" Filler="blank" ID="262">/</ERLKZ> 
<IBAN	Length="34" Alignment="left" Filler="blank" ID="263">/</IBAN> 
<VALID_FROM	Length="8" Alignment="left" Filler="blank" ID="264">/</VALID_FROM> 
<SEGMENT	Length="10" Alignment="left" Filler="blank" ID="265">/</SEGMENT> 
<PSEGMENT	Length="10" Alignment="left" Filler="blank" ID="266">/</PSEGMENT> 
<HKTID	Length="5" Alignment="left" Filler="blank" ID="267">/</HKTID> 
<XSIWE	Length="1" Alignment="left" Filler="blank" ID="268">/</XSIWE> 
<TCNO	Length="16" Alignment="left" Filler="blank" ID="269">/</TCNO> 
<DATEOFSERVICE	Length="8" Alignment="left" Filler="blank" ID="270">/</DATEOFSERVICE> 
<NOTAXCORR	Length="1" Alignment="left" Filler="blank" ID="271">/</NOTAXCORR> 
<DIFFOPTRATE	Length="10" Alignment="left" Filler="blank" ID="272">/</DIFFOPTRATE> 
<HASDIFFOPTRATE	Length="1" Alignment="left" Filler="blank" ID="273">/</HASDIFFOPTRATE> 
<SENDE	Length="1" Alignment="left" Filler="blank" ID="274">/</SENDE> 
<PRODPER	Length="8" Alignment="left" Filler="blank" ID="275">/</PRODPER> 
<!-- Additional tags end --> 

</invoiceCharges_1_elements> 
</xsl:template> 

<xsl:template name="invoiceDetails_1_elements"> 
<invoiceDetails_1_elements> 
<STYPE ID="1"	Length="1"	Alignment="left" Filler="blank">2</STYPE> 
<TBNAM ID="2"	Length="30"	Alignment="left" Filler="blank">BBSEG</TBNAM> 
<NEWBS ID="3"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:if test="parent::node()/type='I'">40</xsl:if> 
<xsl:if test="parent::node()/type='C'">50</xsl:if> 
</NEWBS> 
<DUMMYX	ID="4"	Length="10"	Alignment="left" Filler="blank">/</DUMMYX> 
<NEWUM	ID="5"	Length="1"	Alignment="left" Filler="blank">/</NEWUM> 
<NEWBK	ID="6"	Length="4"	Alignment="left" Filler="blank">/</NEWBK> 
<WRBTR	ID="7"	Length="16"	Alignment="left" Filler="blank"> 
<xsl:value-of select="format-number(totalAmount, '0000000000000,00', 'european')"/>	
</WRBTR> 
<DMBTR	ID="8"	Length="16"	Alignment="left" Filler="blank">/</DMBTR> 
<WMWST	ID="9"	Length="16"	Alignment="left" Filler="blank">/</WMWST> 
<MWSTS	ID="10"	Length="16"	Alignment="left" Filler="blank">/</MWSTS> 
<MWSKZ	ID="11"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:value-of select="parent::node()/financialTaxCode" /> 
</MWSKZ> 
<XSKRL	ID="12"	Length="1"	Alignment="left" Filler="blank">/</XSKRL> 
<FWZUZ	ID="13"	Length="16"	Alignment="left" Filler="blank">/</FWZUZ> 
<HWZUZ	ID="14"	Length="16"	Alignment="left" Filler="blank">/</HWZUZ> 
<GSBER	ID="15"	Length="4"	Alignment="left" Filler="blank">/</GSBER> 
<KOSTL	ID="16"	Length="10"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="financialCostcenter and string-length(financialCostcenter) &gt; 0"> 
<xsl:value-of select="financialCostcenter" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</KOSTL> 
<DUMMY4	ID="17"	Length="4"	Alignment="left" Filler="blank">/</DUMMY4> 
<AUFNR	ID="18"	Length="12"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="costcenter2 and string-length(costcenter2) &gt; 0"> 
<xsl:value-of select="costcenter2" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</AUFNR> 
<EBELN	ID="19"	Length="10"	Alignment="left" Filler="blank">/</EBELN> 
<EBELP	ID="20"	Length="5"	Alignment="left" Filler="blank">/</EBELP> 
<PROJN	ID="21"	Length="16"	Alignment="left" Filler="blank">/</PROJN> 
<MATNR	ID="22"	Length="18"	Alignment="left" Filler="blank">/</MATNR> 
<WERKS	ID="23"	Length="4"	Alignment="left" Filler="blank">/</WERKS> 
<MENGE	ID="24"	Length="17"	Alignment="left" Filler="blank">/</MENGE> 
<MEINS	ID="25"	Length="3"	Alignment="left" Filler="blank">/</MEINS> 
<VBEL2	ID="26"	Length="10"	Alignment="left" Filler="blank">/</VBEL2> 
<POSN2	ID="27"	Length="6"	Alignment="left" Filler="blank">/</POSN2> 
<ETEN2	ID="28"	Length="4"	Alignment="left" Filler="blank">/</ETEN2> 
<PERNR	ID="29"	Length="8"	Alignment="left" Filler="blank">/</PERNR> 
<BEWAR	ID="30"	Length="3"	Alignment="left" Filler="blank">/</BEWAR> 
<VALUT	ID="31"	Length="8"	Alignment="left" Filler="blank">/</VALUT> 
<ZFBDT	ID="32"	Length="8"	Alignment="left" Filler="blank">/</ZFBDT> 
<ZINKZ	ID="33"	Length="2"	Alignment="left" Filler="blank">/</ZINKZ> 
<ZUONR	ID="34"	Length="18"	Alignment="left" Filler="blank"> 
<xsl:value-of select="orderNumber" /> 
<xsl:value-of select="'-'" /> 
<xsl:value-of select="itemNumber" /> 
</ZUONR> 
<FKONT	ID="35"	Length="3"	Alignment="left" Filler="blank">/</FKONT> 
<XAABG	ID="36"	Length="1"	Alignment="left" Filler="blank">/</XAABG> 
<SGTXT	ID="37"	Length="50"	Alignment="left" Filler="blank"> 
<xsl:variable name="twoDigitYear"> 
<xsl:value-of select="substring(parent::node()/financialPeriodYear, 3, 4)"/> 
</xsl:variable> 
<xsl:value-of select="$twoDigitYear" /> 
<xsl:value-of select="'/'" /> 
<xsl:value-of select="parent::node()/financialPeriodMonth" /> 
</SGTXT> 
<BLNKZ	ID="38"	Length="2"	Alignment="left" Filler="blank">/</BLNKZ> 
<BLNBT	ID="39" Alignment="left" Filler="blank" Length="16">/</BLNBT> 
<BLNPZ	ID="40" Alignment="left" Filler="blank" Length="8">/</BLNPZ> 
<MABER	ID="41" Alignment="left" Filler="blank" Length="2">/</MABER> 
<SKFBT	ID="42" Alignment="left" Filler="blank" Length="16">/</SKFBT> 
<WSKTO	ID="43" Alignment="left" Filler="blank" Length="16">/</WSKTO> 
<ZTERM	ID="44" Alignment="left" Filler="blank" Length="4">/</ZTERM> 
<ZBD1T	ID="45" Alignment="left" Filler="blank" Length="3">/</ZBD1T> 
<ZBD1P	ID="46" Alignment="left" Filler="blank" Length="6">/</ZBD1P> 
<ZBD2T	ID="47" Alignment="left" Filler="blank" Length="3">/</ZBD2T> 
<ZBD2P	ID="48" Alignment="left" Filler="blank" Length="6">/</ZBD2P> 
<ZBD3T	ID="49" Alignment="left" Filler="blank" Length="3">/</ZBD3T> 
<ZLSPR	ID="50" Alignment="left" Filler="blank" Length="1">/</ZLSPR> 
<REBZG	ID="51" Alignment="left" Filler="blank" Length="10">/</REBZG> 
<REBZJ	ID="52" Alignment="left" Filler="blank" Length="4">/</REBZJ> 
<REBZZ	ID="53" Alignment="left" Filler="blank" Length="3">/</REBZZ> 
<ZLSCH	ID="54" Alignment="left" Filler="blank" Length="1">/</ZLSCH> 
<SAMNR	ID="55" Alignment="left" Filler="blank" Length="8">/</SAMNR> 
<ZBFIX	ID="56" Alignment="left" Filler="blank" Length="1">/</ZBFIX> 
<QSSKZ	ID="57" Alignment="left" Filler="blank" Length="2">/</QSSKZ> 
<QSSHB	ID="58" Alignment="left" Filler="blank" Length="16">/</QSSHB> 
<QSFBT	ID="59" Alignment="left" Filler="blank" Length="16">/</QSFBT> 
<ESRNR	ID="60" Alignment="left" Filler="blank" Length="11">/</ESRNR> 
<ESRPZ	ID="61" Alignment="left" Filler="blank" Length="2">/</ESRPZ> 
<ESRRE	ID="62" Alignment="left" Filler="blank" Length="27">/</ESRRE> 
<FDTAG	ID="63" Alignment="left" Filler="blank" Length="8">/</FDTAG> 
<FDLEV	ID="64" Alignment="left" Filler="blank" Length="2">/</FDLEV> 
<ANLN1	ID="65" Alignment="left" Filler="blank" Length="12">/</ANLN1> 
<ANLN2	ID="66" Alignment="left" Filler="blank" Length="4">/</ANLN2> 
<BZDAT	ID="67" Alignment="left" Filler="blank" Length="8">/</BZDAT> 
<ANBWA	ID="68" Alignment="left" Filler="blank" Length="3">/</ANBWA> 
<ABPER	ID="69" Alignment="left" Filler="blank" Length="7">/</ABPER> 
<GBETR	ID="70" Alignment="left" Filler="blank" Length="16">/</GBETR> 
<KURSR	ID="71" Alignment="left" Filler="blank" Length="10">/</KURSR> 
<MANSP	ID="72" Alignment="left" Filler="blank" Length="1">/</MANSP> 
<MSCHL	ID="73" Alignment="left" Filler="blank" Length="1">/</MSCHL> 
<HBKID	ID="74" Alignment="left" Filler="blank" Length="5">/</HBKID> 
<BVTYP	ID="75" Alignment="left" Filler="blank" Length="4">/</BVTYP> 
<ANFBN	ID="76" Alignment="left" Filler="blank" Length="10">/</ANFBN> 
<ANFBU	ID="77" Alignment="left" Filler="blank" Length="4">/</ANFBU> 
<ANFBJ	ID="78" Alignment="left" Filler="blank" Length="4">/</ANFBJ> 
<LZBKZ	ID="79" Alignment="left" Filler="blank" Length="3"> 
<xsl:choose> 
<xsl:when test="orderType='S'"> 
<xsl:value-of select="$constLZBZK_S_ORDER" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="$constLZBZK_OTHER" /> 
</xsl:otherwise> 
</xsl:choose>	
</LZBKZ> 
<LANDL	ID="80" Alignment="left" Filler="blank" Length="3">/</LANDL> 
<DIEKZ	ID="81" Alignment="left" Filler="blank" Length="1">/</DIEKZ> 
<ZOLLD	ID="82" Alignment="left" Filler="blank" Length="8">/</ZOLLD> 
<ZOLLT	ID="83" Alignment="left" Filler="blank" Length="8">/</ZOLLT> 
<VRSDT	ID="84" Alignment="left" Filler="blank" Length="8">/</VRSDT> 
<VRSKZ	ID="85" Alignment="left" Filler="blank" Length="1">/</VRSKZ> 
<HZUON	ID="86" Alignment="left" Filler="blank" Length="18">/</HZUON> 
<REGUL	ID="87" Alignment="left" Filler="blank" Length="1">/</REGUL> 
<NAME1	ID="88" Alignment="left" Filler="blank" Length="35">/</NAME1> 
<NAME2	ID="89" Alignment="left" Filler="blank" Length="35">/</NAME2> 
<NAME3	ID="90" Alignment="left" Filler="blank" Length="35">/</NAME3> 
<NAME4	ID="91" Alignment="left" Filler="blank" Length="35">/</NAME4> 
<STRAS	ID="92" Alignment="left" Filler="blank" Length="35">/</STRAS> 
<ORT01	ID="93" Alignment="left" Filler="blank" Length="35">/</ORT01> 
<PSTLZ	ID="94" Alignment="left" Filler="blank" Length="10">/</PSTLZ> 
<LAND1	ID="95" Alignment="left" Filler="blank" Length="3">/</LAND1> 
<REGIO	ID="96" Alignment="left" Filler="blank" Length="3">/</REGIO> 
<BANKL	ID="97" Alignment="left" Filler="blank" Length="15">/</BANKL> 
<BANKS	ID="98" Alignment="left" Filler="blank" Length="3">/</BANKS> 
<BANKN	ID="99" Alignment="left" Filler="blank" Length="18">/</BANKN> 
<BKONT	ID="100" Alignment="left" Filler="blank" Length="2">/</BKONT> 
<STCD1	ID="101" Alignment="left" Filler="blank" Length="16">/</STCD1> 
<STCD2	ID="102" Alignment="left" Filler="blank" Length="11">/</STCD2> 
<MADAT	ID="103" Alignment="left" Filler="blank" Length="8">/</MADAT> 
<MANST	ID="104" Alignment="left" Filler="blank" Length="1">/</MANST> 
<EGMLD	ID="105" Alignment="left" Filler="blank" Length="3">/</EGMLD> 
<DUMMY2	ID="106" Alignment="left" Filler="blank" Length="3">/</DUMMY2> 
<STCEG	ID="107" Alignment="left" Filler="blank" Length="20">/</STCEG> 
<STKZA	ID="108" Alignment="left" Filler="blank" Length="1">/</STKZA> 
<STKZU	ID="109" Alignment="left" Filler="blank" Length="1">/</STKZU> 
<PFACH	ID="110" Alignment="left" Filler="blank" Length="10">/</PFACH> 
<PSTL2	ID="111" Alignment="left" Filler="blank" Length="10">/</PSTL2> 
<SPRAS	ID="112" Alignment="left" Filler="blank" Length="1">/</SPRAS> 
<XINVE	ID="113" Alignment="left" Filler="blank" Length="1">/</XINVE> 

<NEWKO ID="114" Length="17" Alignment="left" Filler="blank"> 
<xsl:value-of select="accountNumber" /> 
</NEWKO> 

<NEWBW	Length="3"	Alignment="left" Filler="blank" ID="115">/</NEWBW> 
<KNRZE	Length="17"	Alignment="left" Filler="blank" ID="116">/</KNRZE> 
<HKONT	Length="10"	Alignment="left" Filler="blank" ID="117">/</HKONT> 
<PRCTR	Length="10"	Alignment="left" Filler="blank" ID="118">/</PRCTR> 
<VERTN	Length="13"	Alignment="left" Filler="blank" ID="119">/</VERTN> 
<VERTT	Length="1"	Alignment="left" Filler="blank" ID="120">/</VERTT> 
<VBEWA	Length="4"	Alignment="left" Filler="blank" ID="121">/</VBEWA> 
<HWBAS	Length="16"	Alignment="left" Filler="blank" ID="122">/</HWBAS> 
<FWBAS	Length="16"	Alignment="left" Filler="blank" ID="123">/</FWBAS> 
<FIPOS	Length="14"	Alignment="left" Filler="blank" ID="124">/</FIPOS> 
<VNAME	Length="6"	Alignment="left" Filler="blank" ID="125">/</VNAME> 
<EGRUP	Length="3"	Alignment="left" Filler="blank" ID="126">/</EGRUP> 
<BTYPE	Length="2"	Alignment="left" Filler="blank" ID="127">/</BTYPE> 
<PAOBJNR	Length="10"	Alignment="left" Filler="blank" ID="128">/</PAOBJNR> 
<KSTRG	Length="12"	Alignment="left" Filler="blank" ID="129">/</KSTRG> 
<IMKEY	Length="8"	Alignment="left" Filler="blank" ID="130">/</IMKEY> 
<DUMMY3	Length="8"	Alignment="left" Filler="blank" ID="131">/</DUMMY3> 
<VPTNR	Length="10"	Alignment="left" Filler="blank" ID="132">/</VPTNR> 
<NPLNR	Length="12"	Alignment="left" Filler="blank" ID="133">/</NPLNR> 
<VORNR	Length="4"	Alignment="left" Filler="blank" ID="134">/</VORNR> 
<XEGDR	Length="1"	Alignment="left" Filler="blank" ID="135">/</XEGDR> 
<RECID	Length="2"	Alignment="left" Filler="blank" ID="136">/</RECID> 
<PPRCT	Length="10"	Alignment="left" Filler="blank" ID="137">/</PPRCT> 
<PROJK	Length="24"	Alignment="left" Filler="blank" ID="138">/</PROJK> 
<UZAWE	Length="2"	Alignment="left" Filler="blank" ID="139">/</UZAWE> 
<TXJCD	Length="15"	Alignment="left" Filler="blank" ID="140">/</TXJCD> 
<FISTL	Length="16"	Alignment="left" Filler="blank" ID="141">/</FISTL> 
<GEBER	Length="10"	Alignment="left" Filler="blank" ID="142">/</GEBER> 
<DMBE2	Length="16"	Alignment="left" Filler="blank" ID="143">/</DMBE2> 
<DMBE3	Length="16"	Alignment="left" Filler="blank" ID="144">/</DMBE3> 
<PARGB	Length="4"	Alignment="left" Filler="blank" ID="145">/</PARGB> 
<XREF1	Length="12"	Alignment="left" Filler="blank" ID="146">/</XREF1> 
<XREF2	Length="12"	Alignment="left" Filler="blank" ID="147">/</XREF2> 
<KBLNR	Length="10"	Alignment="left" Filler="blank" ID="149">/</KBLNR> 
<KBLPOS	Length="3"	Alignment="left" Filler="blank" ID="150">/</KBLPOS> 
<WDATE	Length="8"	Alignment="left" Filler="blank" ID="151">/</WDATE> 
<WGBKZ	Length="1"	Alignment="left" Filler="blank" ID="152">/</WGBKZ> 
<XAKTZ	Length="1"	Alignment="left" Filler="blank" ID="153">/</XAKTZ> 
<WNAME	Length="30"	Alignment="left" Filler="blank" ID="154">/</WNAME> 
<WORT1	Length="30"	Alignment="left" Filler="blank" ID="155">/</WORT1> 
<WBZOG	Length="30"	Alignment="left" Filler="blank" ID="156">/</WBZOG> 
<WORT2	Length="30"	Alignment="left" Filler="blank" ID="157">/</WORT2> 
<WBANK	Length="60"	Alignment="left" Filler="blank" ID="158">/</WBANK> 
<WLZBP	Length="60"	Alignment="left" Filler="blank" ID="159">/</WLZBP> 
<DISKP	Length="8"	Alignment="left" Filler="blank" ID="160">/</DISKP> 
<DISKT	Length="3"	Alignment="left" Filler="blank" ID="161">/</DISKT> 
<WINFW	Length="16"	Alignment="left" Filler="blank" ID="162">/</WINFW> 
<WINHW	Length="16"	Alignment="left" Filler="blank" ID="163">/</WINHW> 
<WEVWV	Length="1"	Alignment="left" Filler="blank" ID="164">/</WEVWV> 
<WSTAT	Length="1"	Alignment="left" Filler="blank" ID="165">/</WSTAT> 
<WMWKZ	Length="2"	Alignment="left" Filler="blank" ID="166">/</WMWKZ> 
<WSTKZ	Length="1"	Alignment="left" Filler="blank" ID="167">/</WSTKZ> 
<RKE_ARTNR	Length="18"	Alignment="left" Filler="blank" ID="169">/</RKE_ARTNR> 
<RKE_BONUS	Length="2"	Alignment="left" Filler="blank" ID="170">/</RKE_BONUS> 
<RKE_BRSCH	Length="4"	Alignment="left" Filler="blank" ID="171">/</RKE_BRSCH> 
<RKE_BUKRS	Length="4"	Alignment="left" Filler="blank" ID="172">/</RKE_BUKRS> 
<RKE_BZIRK	Length="6"	Alignment="left" Filler="blank" ID="173">/</RKE_BZIRK> 
<RKE_EFORM	Length="5"	Alignment="left" Filler="blank" ID="174">/</RKE_EFORM> 
<RKE_FKART	Length="4"	Alignment="left" Filler="blank" ID="175">/</RKE_FKART> 
<RKE_GEBIE	Length="4"	Alignment="left" Filler="blank" ID="176">/</RKE_GEBIE> 
<RKE_GSBER	Length="4"	Alignment="left" Filler="blank" ID="177">/</RKE_GSBER> 
<RKE_KAUFN	Length="10"	Alignment="left" Filler="blank" ID="178">/</RKE_KAUFN> 
<RKE_KDGRP	Length="2"	Alignment="left" Filler="blank" ID="179">/</RKE_KDGRP> 
<RKE_KDPOS	Length="6"	Alignment="left" Filler="blank" ID="180">/</RKE_KDPOS> 
<RKE_KNDNR	Length="10"	Alignment="left" Filler="blank" ID="181">/</RKE_KNDNR> 
<RKE_KOKRS	Length="4"	Alignment="left" Filler="blank" ID="182">/</RKE_KOKRS> 
<RKE_KSTRG	Length="12"	Alignment="left" Filler="blank" ID="183">/</RKE_KSTRG> 
<RKE_LAND1	Length="3"	Alignment="left" Filler="blank" ID="184">/</RKE_LAND1> 
<RKE_MAABC	Length="1"	Alignment="left" Filler="blank" ID="185">/</RKE_MAABC> 
<RKE_MATKL	Length="9"	Alignment="left" Filler="blank" ID="186">/</RKE_MATKL> 
<RKE_PRCTR	Length="10"	Alignment="left" Filler="blank" ID="187">/</RKE_PRCTR> 
<RKE_PSPNR	Length="24"	Alignment="left" Filler="blank" ID="188">/</RKE_PSPNR> 
<RKE_RKAUFNR	Length="12"	Alignment="left" Filler="blank" ID="189">/</RKE_RKAUFNR> 
<RKE_SPART	Length="2"	Alignment="left" Filler="blank" ID="190">/</RKE_SPART> 
<RKE_VKBUR	Length="4"	Alignment="left" Filler="blank" ID="191">/</RKE_VKBUR> 
<RKE_VKGRP	Length="3"	Alignment="left" Filler="blank" ID="192">/</RKE_VKGRP> 
<RKE_VKORG	Length="4"	Alignment="left" Filler="blank" ID="193">/</RKE_VKORG> 
<RKE_VTWEG	Length="2"	Alignment="left" Filler="blank" ID="194">/</RKE_VTWEG> 
<RKE_WERKS	Length="4"	Alignment="left" Filler="blank" ID="195">/</RKE_WERKS> 
<RKE_KMBRND	Length="2"	Alignment="left" Filler="blank" ID="196">/</RKE_KMBRND> 
<RKE_KMCATG	Length="2"	Alignment="left" Filler="blank" ID="197">/</RKE_KMCATG> 
<RKE_KMHI01	Length="10"	Alignment="left" Filler="blank" ID="198">/</RKE_KMHI01> 
<RKE_KMHI02	Length="10"	Alignment="left" Filler="blank" ID="199">/</RKE_KMHI02> 
<RKE_KMHI03	Length="10"	Alignment="left" Filler="blank" ID="200">/</RKE_KMHI03> 
<RKE_KMKDGR	Length="2"	Alignment="left" Filler="blank" ID="201">/</RKE_KMKDGR> 
<RKE_KMLAND	Length="3"	Alignment="left" Filler="blank" ID="202">/</RKE_KMLAND> 
<RKE_KMMAKL	Length="9"	Alignment="left" Filler="blank" ID="203">/</RKE_KMMAKL> 
<RKE_KMNIEL	Length="2"	Alignment="left" Filler="blank" ID="204">/</RKE_KMNIEL> 
<RKE_KMSTGE	Length="2"	Alignment="left" Filler="blank" ID="205">/</RKE_KMSTGE> 
<RKE_KMVKBU	Length="4"	Alignment="left" Filler="blank" ID="206">/</RKE_KMVKBU> 
<RKE_KMVKGR	Length="3"	Alignment="left" Filler="blank" ID="207">/</RKE_KMVKGR> 
<RKE_KMVTNR	Length="8"	Alignment="left" Filler="blank" ID="208">/</RKE_KMVTNR> 
<RKE_PPRCTR	Length="10"	Alignment="left" Filler="blank" ID="209">/</RKE_PPRCTR> 

<!-- START new RKE-elements --> 
<RKE_WW005	Length="5"	Alignment="left" Filler="blank" ID="276">/</RKE_WW005> 
<RKE_WW006	Length="5"	Alignment="left" Filler="blank" ID="277">/</RKE_WW006> 
<RKE_WW007	Length="3"	Alignment="left" Filler="blank" ID="278">/</RKE_WW007> 
<RKE_WW008	Length="3"	Alignment="left" Filler="blank" ID="279">/</RKE_WW008> 
<RKE_WW009	Length="1"	Alignment="left" Filler="blank" ID="280">/</RKE_WW009> 
<RKE_WW010	Length="3"	Alignment="left" Filler="blank" ID="281">/</RKE_WW010> 
<RKE_WW011	Length="2"	Alignment="left" Filler="blank" ID="282">/</RKE_WW011> 
<RKE_WW012	Length="6"	Alignment="left" Filler="blank" ID="283">/</RKE_WW012> 
<RKE_WW013	Length="10"	Alignment="left" Filler="blank" ID="284">/</RKE_WW013> 
<RKE_WW015	Length="1"	Alignment="left" Filler="blank" ID="285">/</RKE_WW015> 
<RKE_WW016	Length="2"	Alignment="left" Filler="blank" ID="286">/</RKE_WW016> 
<RKE_WW017	Length="7"	Alignment="left" Filler="blank" ID="287">/</RKE_WW017> 
<RKE_WW019	Length="6"	Alignment="left" Filler="blank" ID="289">/</RKE_WW019> 
<!-- END new RKE-elements --> 

<VBUND	Length="6"	Alignment="left" Filler="blank" ID="210">/</VBUND> 
<FKBER	Length="4"	Alignment="left" Filler="blank" ID="211">/</FKBER> 
<DABRZ	Length="8"	Alignment="left" Filler="blank" ID="212">/</DABRZ> 
<XSTBA	Length="1"	Alignment="left" Filler="blank" ID="213">/</XSTBA> 

<!-- START Additional (empty) tags start --> 
<RSTGR	Length="3" Alignment="left" Filler="blank" ID="214">/</RSTGR> 
<FIPEX	Length="24" Alignment="left" Filler="blank" ID="215">/</FIPEX> 
<XNEGP	Length="1" Alignment="left" Filler="blank" ID="216">/</XNEGP> 
<GRICD	Length="2" Alignment="left" Filler="blank" ID="217">/</GRICD> 
<GRIRG	Length="3" Alignment="left" Filler="blank" ID="218">/</GRIRG> 
<GITYP	Length="2" Alignment="left" Filler="blank" ID="219">/</GITYP> 
<FITYP	Length="2" Alignment="left" Filler="blank" ID="220">/</FITYP> 
<STCDT	Length="2" Alignment="left" Filler="blank" ID="221">/</STCDT> 
<STKZN	Length="1" Alignment="left" Filler="blank" ID="222">/</STKZN> 
<STCD3	Length="18" Alignment="left" Filler="blank" ID="223">/</STCD3> 
<STCD4	Length="18" Alignment="left" Filler="blank" ID="224">/</STCD4> 
<XREF3	Length="20" Alignment="left" Filler="blank" ID="225">/</XREF3> 
<KIDNO	Length="30" Alignment="left" Filler="blank" ID="226">/</KIDNO> 
<DTWS1	Length="2" Alignment="left" Filler="blank" ID="227">/</DTWS1> 
<DTWS2	Length="2" Alignment="left" Filler="blank" ID="228">/</DTWS2> 
<DTWS3	Length="2" Alignment="left" Filler="blank" ID="229">/</DTWS3> 
<DTWS4	Length="2" Alignment="left" Filler="blank" ID="230">/</DTWS4> 
<DTAWS	Length="2" Alignment="left" Filler="blank" ID="231">/</DTAWS> 
<PYCUR	Length="5" Alignment="left" Filler="blank" ID="232">/</PYCUR> 
<PYAMT	Length="16" Alignment="left" Filler="blank" ID="233">/</PYAMT> 
<BUPLA	Length="4" Alignment="left" Filler="blank" ID="234">/</BUPLA> 
<SECCO	Length="4" Alignment="left" Filler="blank" ID="235">/</SECCO> 
<LSTAR	Length="6" Alignment="left" Filler="blank" ID="236">/</LSTAR> 
<EGDEB	Length="10" Alignment="left" Filler="blank" ID="237">/</EGDEB> 
<WENR	Length="8" Alignment="left" Filler="blank" ID="238">/</WENR> 
<GENR	Length="8" Alignment="left" Filler="blank" ID="239">/</GENR> 
<GRNR	Length="8" Alignment="left" Filler="blank" ID="240">/</GRNR> 
<MENR	Length="8" Alignment="left" Filler="blank" ID="241">/</MENR> 
<MIVE	Length="13" Alignment="left" Filler="blank" ID="242">/</MIVE> 
<NKSL	Length="4" Alignment="left" Filler="blank" ID="243">/</NKSL> 
<EMPSL	Length="5" Alignment="left" Filler="blank" ID="244">/</EMPSL> 
<SVWNR	Length="13" Alignment="left" Filler="blank" ID="245">/</SVWNR> 
<SBERI	Length="10" Alignment="left" Filler="blank" ID="246">/</SBERI> 
<KKBER	Length="4" Alignment="left" Filler="blank" ID="247">/</KKBER> 
<EMPFB	Length="10" Alignment="left" Filler="blank" ID="248">/</EMPFB> 
<KURSR_M	Length="10" Alignment="left" Filler="blank" ID="249">/</KURSR_M> 
<J_1KFREPRE	Length="10" Alignment="left" Filler="blank" ID="250">/</J_1KFREPRE> 
<J_1KFTBUS	Length="30" Alignment="left" Filler="blank" ID="251">/</J_1KFTBUS> 
<J_1KFTIND	Length="30" Alignment="left" Filler="blank" ID="252">/</J_1KFTIND> 
<IDXSP	Length="5" Alignment="left" Filler="blank" ID="253">/</IDXSP> 
<ANRED	Length="15" Alignment="left" Filler="blank" ID="254">/</ANRED> 
<RECNNR	Length="13" Alignment="left" Filler="blank" ID="255">/</RECNNR> 
<E_MIVE	Length="13" Alignment="left" Filler="blank" ID="256">/</E_MIVE> 
<BKREF	Length="20" Alignment="left" Filler="blank" ID="257">/</BKREF> 
<DTAMS	Length="1" Alignment="left" Filler="blank" ID="258">/</DTAMS> 
<CESSION_KZ	Length="2" Alignment="left" Filler="blank" ID="259">/</CESSION_KZ> 
<GRANT_NBR	Length="20" Alignment="left" Filler="blank" ID="260">/</GRANT_NBR> 
<FKBER_LONG	Length="16" Alignment="left" Filler="blank" ID="261">/</FKBER_LONG> 
<ERLKZ	Length="1" Alignment="left" Filler="blank" ID="262">/</ERLKZ> 
<IBAN	Length="34" Alignment="left" Filler="blank" ID="263">/</IBAN> 
<VALID_FROM	Length="8" Alignment="left" Filler="blank" ID="264">/</VALID_FROM> 
<SEGMENT	Length="10" Alignment="left" Filler="blank" ID="265">/</SEGMENT> 
<PSEGMENT	Length="10" Alignment="left" Filler="blank" ID="266">/</PSEGMENT> 
<HKTID	Length="5" Alignment="left" Filler="blank" ID="267">/</HKTID> 
<XSIWE	Length="1" Alignment="left" Filler="blank" ID="268">/</XSIWE> 
<TCNO	Length="16" Alignment="left" Filler="blank" ID="269">/</TCNO> 
<DATEOFSERVICE	Length="8" Alignment="left" Filler="blank" ID="270">/</DATEOFSERVICE> 
<NOTAXCORR	Length="1" Alignment="left" Filler="blank" ID="271">/</NOTAXCORR> 
<DIFFOPTRATE	Length="10" Alignment="left" Filler="blank" ID="272">/</DIFFOPTRATE> 
<HASDIFFOPTRATE	Length="1" Alignment="left" Filler="blank" ID="273">/</HASDIFFOPTRATE> 
<SENDE	Length="1" Alignment="left" Filler="blank" ID="274">/</SENDE> 
<PRODPER	Length="8" Alignment="left" Filler="blank" ID="275">/</PRODPER> 
<!-- END Additional tags -->	

</invoiceDetails_1_elements> 
</xsl:template> 

<xsl:template name="invoiceDetails_DetailCharges_1_elements"> 
<invoiceDetails_DetailCharges_1_elements> 
<STYPE ID="1"	Length="1"	Alignment="left" Filler="blank">2</STYPE> 
<TBNAM ID="2"	Length="30"	Alignment="left" Filler="blank">BBSEG</TBNAM> 
<NEWBS ID="3"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:if test="parent::node()/type='I'">40</xsl:if> 
<xsl:if test="parent::node()/type='C'">50</xsl:if> 
</NEWBS> 
<DUMMYX	ID="4"	Length="10"	Alignment="left" Filler="blank">/</DUMMYX> 
<NEWUM	ID="5"	Length="1"	Alignment="left" Filler="blank">/</NEWUM> 
<NEWBK	ID="6"	Length="4"	Alignment="left" Filler="blank">/</NEWBK> 
<WRBTR	ID="7"	Length="16"	Alignment="left" Filler="blank"> 
<!--	<xsl:value-of select="format-number(totalAmount, '#0.00', 'european')"/>	--> 
<xsl:value-of select="format-number(totalAmount, '0000000000000,00', 'european')"/>	
</WRBTR> 
<DMBTR	ID="8"	Length="16"	Alignment="left" Filler="blank">/</DMBTR> 
<WMWST	ID="9"	Length="16"	Alignment="left" Filler="blank">/</WMWST> 
<MWSTS	ID="10"	Length="16"	Alignment="left" Filler="blank">/</MWSTS> 
<MWSKZ	ID="11"	Length="2"	Alignment="left" Filler="blank"> 
<xsl:value-of select="parent::node()/financialTaxCode" /> 
</MWSKZ> 
<XSKRL	ID="12"	Length="1"	Alignment="left" Filler="blank">/</XSKRL> 
<FWZUZ	ID="13"	Length="16"	Alignment="left" Filler="blank">/</FWZUZ> 
<HWZUZ	ID="14"	Length="16"	Alignment="left" Filler="blank">/</HWZUZ> 
<GSBER	ID="15"	Length="4"	Alignment="left" Filler="blank">/</GSBER> 
<KOSTL	ID="16"	Length="10"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="financialCostcenter and string-length(financialCostcenter) &gt; 0"> 
<xsl:value-of select="financialCostcenter" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</KOSTL> 
<DUMMY4	ID="17"	Length="4"	Alignment="left" Filler="blank">/</DUMMY4> 
<AUFNR	ID="18"	Length="12"	Alignment="left" Filler="blank"> 
<xsl:choose> 
<xsl:when test="costcenter2 and string-length(costcenter2) &gt; 0"> 
<xsl:value-of select="costcenter2" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="'/'" /> 
</xsl:otherwise> 
</xsl:choose> 
</AUFNR> 
<EBELN	ID="19"	Length="10"	Alignment="left" Filler="blank">/</EBELN> 
<EBELP	ID="20"	Length="5"	Alignment="left" Filler="blank">/</EBELP> 
<PROJN	ID="21"	Length="16"	Alignment="left" Filler="blank">/</PROJN> 
<MATNR	ID="22"	Length="18"	Alignment="left" Filler="blank">/</MATNR> 
<WERKS	ID="23"	Length="4"	Alignment="left" Filler="blank">/</WERKS> 
<MENGE	ID="24"	Length="17"	Alignment="left" Filler="blank">/</MENGE> 
<MEINS	ID="25"	Length="3"	Alignment="left" Filler="blank">/</MEINS> 
<VBEL2	ID="26"	Length="10"	Alignment="left" Filler="blank">/</VBEL2> 
<POSN2	ID="27"	Length="6"	Alignment="left" Filler="blank">/</POSN2> 
<ETEN2	ID="28"	Length="4"	Alignment="left" Filler="blank">/</ETEN2> 
<PERNR	ID="29"	Length="8"	Alignment="left" Filler="blank">/</PERNR> 
<BEWAR	ID="30"	Length="3"	Alignment="left" Filler="blank">/</BEWAR> 
<VALUT	ID="31"	Length="8"	Alignment="left" Filler="blank">/</VALUT> 
<ZFBDT	ID="32"	Length="8"	Alignment="left" Filler="blank">/</ZFBDT> 
<ZINKZ	ID="33"	Length="2"	Alignment="left" Filler="blank">/</ZINKZ> 
<ZUONR	ID="34"	Length="18"	Alignment="left" Filler="blank"> 
<xsl:value-of select="orderNumber" /> 
<xsl:value-of select="'-'" /> 
<xsl:value-of select="itemNumber" /> 
</ZUONR> 
<FKONT	ID="35"	Length="3"	Alignment="left" Filler="blank">/</FKONT> 
<XAABG	ID="36"	Length="1"	Alignment="left" Filler="blank">/</XAABG> 
<SGTXT	ID="37"	Length="50"	Alignment="left" Filler="blank"> 
<xsl:variable name="twoDigitYear"> 
<xsl:value-of select="substring(parent::node()/financialPeriodYear, 3, 4)"/> 
</xsl:variable> 
<xsl:value-of select="$twoDigitYear" /> 
<xsl:value-of select="'/'" /> 
<xsl:value-of select="parent::node()/financialPeriodMonth" /> 
</SGTXT> 
<BLNKZ	ID="38"	Alignment="left" Filler="blank" Length="2">/</BLNKZ> 
<BLNBT	ID="39" Alignment="left" Filler="blank" Length="16">/</BLNBT> 
<BLNPZ	ID="40" Alignment="left" Filler="blank" Length="8">/</BLNPZ> 
<MABER	ID="41" Alignment="left" Filler="blank" Length="2">/</MABER> 
<SKFBT	ID="42" Alignment="left" Filler="blank" Length="16">/</SKFBT> 
<WSKTO	ID="43" Alignment="left" Filler="blank" Length="16">/</WSKTO> 
<ZTERM	ID="44" Alignment="left" Filler="blank" Length="4">/</ZTERM> 
<ZBD1T	ID="45" Alignment="left" Filler="blank" Length="3">/</ZBD1T> 
<ZBD1P	ID="46" Alignment="left" Filler="blank" Length="6">/</ZBD1P> 
<ZBD2T	ID="47" Alignment="left" Filler="blank" Length="3">/</ZBD2T> 
<ZBD2P	ID="48" Alignment="left" Filler="blank" Length="6">/</ZBD2P> 
<ZBD3T	ID="49" Alignment="left" Filler="blank" Length="3">/</ZBD3T> 
<ZLSPR	ID="50" Alignment="left" Filler="blank" Length="1">/</ZLSPR> 
<REBZG	ID="51" Alignment="left" Filler="blank" Length="10">/</REBZG> 
<REBZJ	ID="52" Alignment="left" Filler="blank" Length="4">/</REBZJ> 
<REBZZ	ID="53" Alignment="left" Filler="blank" Length="3">/</REBZZ> 
<ZLSCH	ID="54" Alignment="left" Filler="blank" Length="1">/</ZLSCH> 
<SAMNR	ID="55" Alignment="left" Filler="blank" Length="8">/</SAMNR> 
<ZBFIX	ID="56" Alignment="left" Filler="blank" Length="1">/</ZBFIX> 
<QSSKZ	ID="57" Alignment="left" Filler="blank" Length="2">/</QSSKZ> 
<QSSHB	ID="58" Alignment="left" Filler="blank" Length="16">/</QSSHB> 
<QSFBT	ID="59" Alignment="left" Filler="blank" Length="16">/</QSFBT> 
<ESRNR	ID="60" Alignment="left" Filler="blank" Length="11">/</ESRNR> 
<ESRPZ	ID="61" Alignment="left" Filler="blank" Length="2">/</ESRPZ> 
<ESRRE	ID="62" Alignment="left" Filler="blank" Length="27">/</ESRRE> 
<FDTAG	ID="63" Alignment="left" Filler="blank" Length="8">/</FDTAG> 
<FDLEV	ID="64" Alignment="left" Filler="blank" Length="2">/</FDLEV> 
<ANLN1	ID="65" Alignment="left" Filler="blank" Length="12">/</ANLN1> 
<ANLN2	ID="66" Alignment="left" Filler="blank" Length="4">/</ANLN2> 
<BZDAT	ID="67" Alignment="left" Filler="blank" Length="8">/</BZDAT> 
<ANBWA	ID="68" Alignment="left" Filler="blank" Length="3">/</ANBWA> 
<ABPER	ID="69" Alignment="left" Filler="blank" Length="7">/</ABPER> 
<GBETR	ID="70" Alignment="left" Filler="blank" Length="16">/</GBETR> 
<KURSR	ID="71" Alignment="left" Filler="blank" Length="10">/</KURSR> 
<MANSP	ID="72" Alignment="left" Filler="blank" Length="1">/</MANSP> 
<MSCHL	ID="73" Alignment="left" Filler="blank" Length="1">/</MSCHL> 
<HBKID	ID="74" Alignment="left" Filler="blank" Length="5">/</HBKID> 
<BVTYP	ID="75" Alignment="left" Filler="blank" Length="4">/</BVTYP> 
<ANFBN	ID="76" Alignment="left" Filler="blank" Length="10">/</ANFBN> 
<ANFBU	ID="77" Alignment="left" Filler="blank" Length="4">/</ANFBU> 
<ANFBJ	ID="78" Alignment="left" Filler="blank" Length="4">/</ANFBJ> 
<LZBKZ	ID="79" Alignment="left" Filler="blank" Length="3"> 
<xsl:choose> 
<xsl:when test="orderType='S'"> 
<xsl:value-of select="$constLZBZK_S_ORDER" /> 
</xsl:when> 
<xsl:otherwise> 
<xsl:value-of select="$constLZBZK_OTHER" /> 
</xsl:otherwise> 
</xsl:choose>	
</LZBKZ> 
<LANDL	ID="80" Alignment="left" Filler="blank" Length="3">/</LANDL> 
<DIEKZ	ID="81" Alignment="left" Filler="blank" Length="1">/</DIEKZ> 
<ZOLLD	ID="82" Alignment="left" Filler="blank" Length="8">/</ZOLLD> 
<ZOLLT	ID="83" Alignment="left" Filler="blank" Length="8">/</ZOLLT> 
<VRSDT	ID="84" Alignment="left" Filler="blank" Length="8">/</VRSDT> 
<VRSKZ	ID="85" Alignment="left" Filler="blank" Length="1">/</VRSKZ> 
<HZUON	ID="86" Alignment="left" Filler="blank" Length="18">/</HZUON> 
<REGUL	ID="87" Alignment="left" Filler="blank" Length="1">/</REGUL> 
<NAME1	ID="88" Alignment="left" Filler="blank" Length="35">/</NAME1> 
<NAME2	ID="89" Alignment="left" Filler="blank" Length="35">/</NAME2> 
<NAME3	ID="90" Alignment="left" Filler="blank" Length="35">/</NAME3> 
<NAME4	ID="91" Alignment="left" Filler="blank" Length="35">/</NAME4> 
<STRAS	ID="92" Alignment="left" Filler="blank" Length="35">/</STRAS> 
<ORT01	ID="93" Alignment="left" Filler="blank" Length="35">/</ORT01> 
<PSTLZ	ID="94" Alignment="left" Filler="blank" Length="10">/</PSTLZ> 
<LAND1	ID="95" Alignment="left" Filler="blank" Length="3">/</LAND1> 
<REGIO	ID="96" Alignment="left" Filler="blank" Length="3">/</REGIO> 
<BANKL	ID="97" Alignment="left" Filler="blank" Length="15">/</BANKL> 
<BANKS	ID="98" Alignment="left" Filler="blank" Length="3">/</BANKS> 
<BANKN	ID="99" Alignment="left" Filler="blank" Length="18">/</BANKN> 
<BKONT	ID="100" Alignment="left" Filler="blank" Length="2">/</BKONT> 
<STCD1	ID="101" Alignment="left" Filler="blank" Length="16">/</STCD1> 
<STCD2	ID="102" Alignment="left" Filler="blank" Length="11">/</STCD2> 
<MADAT	ID="103" Alignment="left" Filler="blank" Length="8">/</MADAT> 
<MANST	ID="104" Alignment="left" Filler="blank" Length="1">/</MANST> 
<EGMLD	ID="105" Alignment="left" Filler="blank" Length="3">/</EGMLD> 
<DUMMY2	ID="106" Alignment="left" Filler="blank" Length="3">/</DUMMY2> 
<STCEG	ID="107" Alignment="left" Filler="blank" Length="20">/</STCEG> 
<STKZA	ID="108" Alignment="left" Filler="blank" Length="1">/</STKZA> 
<STKZU	ID="109" Alignment="left" Filler="blank" Length="1">/</STKZU> 
<PFACH	ID="110" Alignment="left" Filler="blank" Length="10">/</PFACH> 
<PSTL2	ID="111" Alignment="left" Filler="blank" Length="10">/</PSTL2> 
<SPRAS	ID="112" Alignment="left" Filler="blank" Length="1">/</SPRAS> 
<XINVE	ID="113" Alignment="left" Filler="blank" Length="1">/</XINVE> 

<NEWKO ID="114" Length="17" Alignment="left" Filler="blank"> 
<xsl:value-of select="accountNumber" /> 
</NEWKO> 

<NEWBW	Length="3"	Alignment="left" Filler="blank" ID="115">/</NEWBW> 
<KNRZE	Length="17"	Alignment="left" Filler="blank" ID="116">/</KNRZE> 
<HKONT	Length="10"	Alignment="left" Filler="blank" ID="117">/</HKONT> 
<PRCTR	Length="10"	Alignment="left" Filler="blank" ID="118">/</PRCTR> 
<VERTN	Length="13"	Alignment="left" Filler="blank" ID="119">/</VERTN> 
<VERTT	Length="1"	Alignment="left" Filler="blank" ID="120">/</VERTT> 
<VBEWA	Length="4"	Alignment="left" Filler="blank" ID="121">/</VBEWA> 
<HWBAS	Length="16"	Alignment="left" Filler="blank" ID="122">/</HWBAS> 
<FWBAS	Length="16"	Alignment="left" Filler="blank" ID="123">/</FWBAS> 
<FIPOS	Length="14"	Alignment="left" Filler="blank" ID="124">/</FIPOS> 
<VNAME	Length="6"	Alignment="left" Filler="blank" ID="125">/</VNAME> 
<EGRUP	Length="3"	Alignment="left" Filler="blank" ID="126">/</EGRUP> 
<BTYPE	Length="2"	Alignment="left" Filler="blank" ID="127">/</BTYPE> 
<PAOBJNR	Length="10"	Alignment="left" Filler="blank" ID="128">/</PAOBJNR> 
<KSTRG	Length="12"	Alignment="left" Filler="blank" ID="129">/</KSTRG> 
<IMKEY	Length="8"	Alignment="left" Filler="blank" ID="130">/</IMKEY> 
<DUMMY3	Length="8"	Alignment="left" Filler="blank" ID="131">/</DUMMY3> 
<VPTNR	Length="10"	Alignment="left" Filler="blank" ID="132">/</VPTNR> 
<NPLNR	Length="12"	Alignment="left" Filler="blank" ID="133">/</NPLNR> 
<VORNR	Length="4"	Alignment="left" Filler="blank" ID="134">/</VORNR> 
<XEGDR	Length="1"	Alignment="left" Filler="blank" ID="135">/</XEGDR> 
<RECID	Length="2"	Alignment="left" Filler="blank" ID="136">/</RECID> 
<PPRCT	Length="10"	Alignment="left" Filler="blank" ID="137">/</PPRCT> 
<PROJK	Length="24"	Alignment="left" Filler="blank" ID="138">/</PROJK> 
<UZAWE	Length="2"	Alignment="left" Filler="blank" ID="139">/</UZAWE> 
<TXJCD	Length="15"	Alignment="left" Filler="blank" ID="140">/</TXJCD> 
<FISTL	Length="16"	Alignment="left" Filler="blank" ID="141">/</FISTL> 
<GEBER	Length="10"	Alignment="left" Filler="blank" ID="142">/</GEBER> 
<DMBE2	Length="16"	Alignment="left" Filler="blank" ID="143">/</DMBE2> 
<DMBE3	Length="16"	Alignment="left" Filler="blank" ID="144">/</DMBE3> 
<PARGB	Length="4"	Alignment="left" Filler="blank" ID="145">/</PARGB> 
<XREF1	Length="12"	Alignment="left" Filler="blank" ID="146">/</XREF1> 
<XREF2	Length="12"	Alignment="left" Filler="blank" ID="147">/</XREF2> 
<KBLNR	Length="10"	Alignment="left" Filler="blank" ID="149">/</KBLNR> 
<KBLPOS	Length="3"	Alignment="left" Filler="blank" ID="150">/</KBLPOS> 
<WDATE	Length="8"	Alignment="left" Filler="blank" ID="151">/</WDATE> 
<WGBKZ	Length="1"	Alignment="left" Filler="blank" ID="152">/</WGBKZ> 
<XAKTZ	Length="1"	Alignment="left" Filler="blank" ID="153">/</XAKTZ> 
<WNAME	Length="30"	Alignment="left" Filler="blank" ID="154">/</WNAME> 
<WORT1	Length="30"	Alignment="left" Filler="blank" ID="155">/</WORT1> 
<WBZOG	Length="30"	Alignment="left" Filler="blank" ID="156">/</WBZOG> 
<WORT2	Length="30"	Alignment="left" Filler="blank" ID="157">/</WORT2> 
<WBANK	Length="60"	Alignment="left" Filler="blank" ID="158">/</WBANK> 
<WLZBP	Length="60"	Alignment="left" Filler="blank" ID="159">/</WLZBP> 
<DISKP	Length="8"	Alignment="left" Filler="blank" ID="160">/</DISKP> 
<DISKT	Length="3"	Alignment="left" Filler="blank" ID="161">/</DISKT> 
<WINFW	Length="16"	Alignment="left" Filler="blank" ID="162">/</WINFW> 
<WINHW	Length="16"	Alignment="left" Filler="blank" ID="163">/</WINHW> 
<WEVWV	Length="1"	Alignment="left" Filler="blank" ID="164">/</WEVWV> 
<WSTAT	Length="1"	Alignment="left" Filler="blank" ID="165">/</WSTAT> 
<WMWKZ	Length="2"	Alignment="left" Filler="blank" ID="166">/</WMWKZ> 
<WSTKZ	Length="1"	Alignment="left" Filler="blank" ID="167">/</WSTKZ> 
<RKE_ARTNR	Length="18"	Alignment="left" Filler="blank" ID="169">/</RKE_ARTNR> 
<RKE_BONUS	Length="2"	Alignment="left" Filler="blank" ID="170">/</RKE_BONUS> 
<RKE_BRSCH	Length="4"	Alignment="left" Filler="blank" ID="171">/</RKE_BRSCH> 
<RKE_BUKRS	Length="4"	Alignment="left" Filler="blank" ID="172">/</RKE_BUKRS> 
<RKE_BZIRK	Length="6"	Alignment="left" Filler="blank" ID="173">/</RKE_BZIRK> 
<RKE_EFORM	Length="5"	Alignment="left" Filler="blank" ID="174">/</RKE_EFORM> 
<RKE_FKART	Length="4"	Alignment="left" Filler="blank" ID="175">/</RKE_FKART> 
<RKE_GEBIE	Length="4"	Alignment="left" Filler="blank" ID="176">/</RKE_GEBIE> 
<RKE_GSBER	Length="4"	Alignment="left" Filler="blank" ID="177">/</RKE_GSBER> 
<RKE_KAUFN	Length="10"	Alignment="left" Filler="blank" ID="178">/</RKE_KAUFN> 
<RKE_KDGRP	Length="2"	Alignment="left" Filler="blank" ID="179">/</RKE_KDGRP> 
<RKE_KDPOS	Length="6"	Alignment="left" Filler="blank" ID="180">/</RKE_KDPOS> 
<RKE_KNDNR	Length="10"	Alignment="left" Filler="blank" ID="181">/</RKE_KNDNR> 
<RKE_KOKRS	Length="4"	Alignment="left" Filler="blank" ID="182">/</RKE_KOKRS> 
<RKE_KSTRG	Length="12"	Alignment="left" Filler="blank" ID="183">/</RKE_KSTRG> 
<RKE_LAND1	Length="3"	Alignment="left" Filler="blank" ID="184">/</RKE_LAND1> 
<RKE_MAABC	Length="1"	Alignment="left" Filler="blank" ID="185">/</RKE_MAABC> 
<RKE_MATKL	Length="9"	Alignment="left" Filler="blank" ID="186">/</RKE_MATKL> 
<RKE_PRCTR	Length="10"	Alignment="left" Filler="blank" ID="187">/</RKE_PRCTR> 
<RKE_PSPNR	Length="24"	Alignment="left" Filler="blank" ID="188">/</RKE_PSPNR> 
<RKE_RKAUFNR	Length="12"	Alignment="left" Filler="blank" ID="189">/</RKE_RKAUFNR> 
<RKE_SPART	Length="2"	Alignment="left" Filler="blank" ID="190">/</RKE_SPART> 
<RKE_VKBUR	Length="4"	Alignment="left" Filler="blank" ID="191">/</RKE_VKBUR> 
<RKE_VKGRP	Length="3"	Alignment="left" Filler="blank" ID="192">/</RKE_VKGRP> 
<RKE_VKORG	Length="4"	Alignment="left" Filler="blank" ID="193">/</RKE_VKORG> 
<RKE_VTWEG	Length="2"	Alignment="left" Filler="blank" ID="194">/</RKE_VTWEG> 
<RKE_WERKS	Length="4"	Alignment="left" Filler="blank" ID="195">/</RKE_WERKS> 
<RKE_KMBRND	Length="2"	Alignment="left" Filler="blank" ID="196">/</RKE_KMBRND> 
<RKE_KMCATG	Length="2"	Alignment="left" Filler="blank" ID="197">/</RKE_KMCATG> 
<RKE_KMHI01	Length="10"	Alignment="left" Filler="blank" ID="198">/</RKE_KMHI01> 
<RKE_KMHI02	Length="10"	Alignment="left" Filler="blank" ID="199">/</RKE_KMHI02> 
<RKE_KMHI03	Length="10"	Alignment="left" Filler="blank" ID="200">/</RKE_KMHI03> 
<RKE_KMKDGR	Length="2"	Alignment="left" Filler="blank" ID="201">/</RKE_KMKDGR> 
<RKE_KMLAND	Length="3"	Alignment="left" Filler="blank" ID="202">/</RKE_KMLAND> 
<RKE_KMMAKL	Length="9"	Alignment="left" Filler="blank" ID="203">/</RKE_KMMAKL> 
<RKE_KMNIEL	Length="2"	Alignment="left" Filler="blank" ID="204">/</RKE_KMNIEL> 
<RKE_KMSTGE	Length="2"	Alignment="left" Filler="blank" ID="205">/</RKE_KMSTGE> 
<RKE_KMVKBU	Length="4"	Alignment="left" Filler="blank" ID="206">/</RKE_KMVKBU> 
<RKE_KMVKGR	Length="3"	Alignment="left" Filler="blank" ID="207">/</RKE_KMVKGR> 
<RKE_KMVTNR	Length="8"	Alignment="left" Filler="blank" ID="208">/</RKE_KMVTNR> 
<RKE_PPRCTR	Length="10"	Alignment="left" Filler="blank" ID="209">/</RKE_PPRCTR> 

<!-- START new RKE-elements --> 
<RKE_WW005	Length="5"	Alignment="left" Filler="blank" ID="276">/</RKE_WW005> 
<RKE_WW006	Length="5"	Alignment="left" Filler="blank" ID="277">/</RKE_WW006> 
<RKE_WW007	Length="3"	Alignment="left" Filler="blank" ID="278">/</RKE_WW007> 
<RKE_WW008	Length="3"	Alignment="left" Filler="blank" ID="279">/</RKE_WW008> 
<RKE_WW009	Length="1"	Alignment="left" Filler="blank" ID="280">/</RKE_WW009> 
<RKE_WW010	Length="3"	Alignment="left" Filler="blank" ID="281">/</RKE_WW010> 
<RKE_WW011	Length="2"	Alignment="left" Filler="blank" ID="282">/</RKE_WW011> 
<RKE_WW012	Length="6"	Alignment="left" Filler="blank" ID="283">/</RKE_WW012> 
<RKE_WW013	Length="10"	Alignment="left" Filler="blank" ID="284">/</RKE_WW013> 
<RKE_WW015	Length="1"	Alignment="left" Filler="blank" ID="285">/</RKE_WW015> 
<RKE_WW016	Length="2"	Alignment="left" Filler="blank" ID="286">/</RKE_WW016> 
<RKE_WW017	Length="7"	Alignment="left" Filler="blank" ID="287">/</RKE_WW017> 
<RKE_WW019	Length="6"	Alignment="left" Filler="blank" ID="289">/</RKE_WW019> 
<!-- END new RKE-elements --> 

<VBUND	Length="6"	Alignment="left" Filler="blank" ID="210">/</VBUND> 
<FKBER	Length="4"	Alignment="left" Filler="blank" ID="211">/</FKBER> 
<DABRZ	Length="8"	Alignment="left" Filler="blank" ID="212">/</DABRZ> 
<XSTBA	Length="1"	Alignment="left" Filler="blank" ID="213">/</XSTBA> 

<!-- Additional (empty) tags start --> 
<RSTGR	Length="3" Alignment="left" Filler="blank" ID="214">/</RSTGR> 
<FIPEX	Length="24" Alignment="left" Filler="blank" ID="215">/</FIPEX> 
<XNEGP	Length="1" Alignment="left" Filler="blank" ID="216">/</XNEGP> 
<GRICD	Length="2" Alignment="left" Filler="blank" ID="217">/</GRICD> 
<GRIRG	Length="3" Alignment="left" Filler="blank" ID="218">/</GRIRG> 
<GITYP	Length="2" Alignment="left" Filler="blank" ID="219">/</GITYP> 
<FITYP	Length="2" Alignment="left" Filler="blank" ID="220">/</FITYP> 
<STCDT	Length="2" Alignment="left" Filler="blank" ID="221">/</STCDT> 
<STKZN	Length="1" Alignment="left" Filler="blank" ID="222">/</STKZN> 
<STCD3	Length="18" Alignment="left" Filler="blank" ID="223">/</STCD3> 
<STCD4	Length="18" Alignment="left" Filler="blank" ID="224">/</STCD4> 
<XREF3	Length="20" Alignment="left" Filler="blank" ID="225">/</XREF3> 
<KIDNO	Length="30" Alignment="left" Filler="blank" ID="226">/</KIDNO> 
<DTWS1	Length="2" Alignment="left" Filler="blank" ID="227">/</DTWS1> 
<DTWS2	Length="2" Alignment="left" Filler="blank" ID="228">/</DTWS2> 
<DTWS3	Length="2" Alignment="left" Filler="blank" ID="229">/</DTWS3> 
<DTWS4	Length="2" Alignment="left" Filler="blank" ID="230">/</DTWS4> 
<DTAWS	Length="2" Alignment="left" Filler="blank" ID="231">/</DTAWS> 
<PYCUR	Length="5" Alignment="left" Filler="blank" ID="232">/</PYCUR> 
<PYAMT	Length="16" Alignment="left" Filler="blank" ID="233">/</PYAMT> 
<BUPLA	Length="4" Alignment="left" Filler="blank" ID="234">/</BUPLA> 
<SECCO	Length="4" Alignment="left" Filler="blank" ID="235">/</SECCO> 
<LSTAR	Length="6" Alignment="left" Filler="blank" ID="236">/</LSTAR> 
<EGDEB	Length="10" Alignment="left" Filler="blank" ID="237">/</EGDEB> 
<WENR	Length="8" Alignment="left" Filler="blank" ID="238">/</WENR> 
<GENR	Length="8" Alignment="left" Filler="blank" ID="239">/</GENR> 
<GRNR	Length="8" Alignment="left" Filler="blank" ID="240">/</GRNR> 
<MENR	Length="8" Alignment="left" Filler="blank" ID="241">/</MENR> 
<MIVE	Length="13" Alignment="left" Filler="blank" ID="242">/</MIVE> 
<NKSL	Length="4" Alignment="left" Filler="blank" ID="243">/</NKSL> 
<EMPSL	Length="5" Alignment="left" Filler="blank" ID="244">/</EMPSL> 
<SVWNR	Length="13" Alignment="left" Filler="blank" ID="245">/</SVWNR> 
<SBERI	Length="10" Alignment="left" Filler="blank" ID="246">/</SBERI> 
<KKBER	Length="4" Alignment="left" Filler="blank" ID="247">/</KKBER> 
<EMPFB	Length="10" Alignment="left" Filler="blank" ID="248">/</EMPFB> 
<KURSR_M	Length="10" Alignment="left" Filler="blank" ID="249">/</KURSR_M> 
<J_1KFREPRE	Length="10" Alignment="left" Filler="blank" ID="250">/</J_1KFREPRE> 
<J_1KFTBUS	Length="30" Alignment="left" Filler="blank" ID="251">/</J_1KFTBUS> 
<J_1KFTIND	Length="30" Alignment="left" Filler="blank" ID="252">/</J_1KFTIND> 
<IDXSP	Length="5" Alignment="left" Filler="blank" ID="253">/</IDXSP> 
<ANRED	Length="15" Alignment="left" Filler="blank" ID="254">/</ANRED> 
<RECNNR	Length="13" Alignment="left" Filler="blank" ID="255">/</RECNNR> 
<E_MIVE	Length="13" Alignment="left" Filler="blank" ID="256">/</E_MIVE> 
<BKREF	Length="20" Alignment="left" Filler="blank" ID="257">/</BKREF> 
<DTAMS	Length="1" Alignment="left" Filler="blank" ID="258">/</DTAMS> 
<CESSION_KZ	Length="2" Alignment="left" Filler="blank" ID="259">/</CESSION_KZ> 
<GRANT_NBR	Length="20" Alignment="left" Filler="blank" ID="260">/</GRANT_NBR> 
<FKBER_LONG	Length="16" Alignment="left" Filler="blank" ID="261">/</FKBER_LONG> 
<ERLKZ	Length="1" Alignment="left" Filler="blank" ID="262">/</ERLKZ> 
<IBAN	Length="34" Alignment="left" Filler="blank" ID="263">/</IBAN> 
<VALID_FROM	Length="8" Alignment="left" Filler="blank" ID="264">/</VALID_FROM> 
<SEGMENT	Length="10" Alignment="left" Filler="blank" ID="265">/</SEGMENT> 
<PSEGMENT	Length="10" Alignment="left" Filler="blank" ID="266">/</PSEGMENT> 
<HKTID	Length="5" Alignment="left" Filler="blank" ID="267">/</HKTID> 
<XSIWE	Length="1" Alignment="left" Filler="blank" ID="268">/</XSIWE> 
<TCNO	Length="16" Alignment="left" Filler="blank" ID="269">/</TCNO> 
<DATEOFSERVICE	Length="8" Alignment="left" Filler="blank" ID="270">/</DATEOFSERVICE> 
<NOTAXCORR	Length="1" Alignment="left" Filler="blank" ID="271">/</NOTAXCORR> 
<DIFFOPTRATE	Length="10" Alignment="left" Filler="blank" ID="272">/</DIFFOPTRATE> 
<HASDIFFOPTRATE	Length="1" Alignment="left" Filler="blank" ID="273">/</HASDIFFOPTRATE> 
<SENDE	Length="1" Alignment="left" Filler="blank" ID="274">/</SENDE> 
<PRODPER	Length="8" Alignment="left" Filler="blank" ID="275">/</PRODPER> 
<!-- Additional tags end --> 

</invoiceDetails_DetailCharges_1_elements> 
</xsl:template> 
<!-- end transformation_2 --> 

<!-- start transformation_1 --> 
<xsl:template name="transformation_1_elements"> 
<xsl:apply-templates select="transferInvoice"/> 
</xsl:template> 

<xsl:template match="transferInvoice"> 
<transformation_1_elements> 
<xsl:apply-templates select="invoice"/> 
</transformation_1_elements> 
</xsl:template> 
<!-- end transformation_1 --> 

<xsl:template match="invoice"> 
<invoice_elements> 
<xsl:attribute name="invoiceNumber"> 
<xsl:value-of select="invoiceNumber"/> 
</xsl:attribute> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="invoiceHeader"/> 
<xsl:apply-templates select="invoiceCharges"/> 
<xsl:apply-templates select="invoiceDetails"/> 
</invoice_elements> 
</xsl:template> 

<!-- start invoiceHeader transformation_1 --> 
<xsl:template match="invoiceHeader"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="invoiceAmount"/> 
<xsl:apply-templates select="orderInformation"/> 
<xsl:apply-templates select="invoiceReversal"/> 
<xsl:apply-templates select="invoiceTransferInformation"/> 
<xsl:apply-templates select="paymentInformation"/> 
<xsl:apply-templates select="financialPeriodInformation"/> 
<xsl:apply-templates select="currencyConversionInformation"/> 
</xsl:template> 

<xsl:template match="invoiceAmount"> 
<internationalCurrencyCode> 
<xsl:value-of select="*[position() = 1]/@internationalCurrencyCode"/> 
</internationalCurrencyCode> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="taxation"/> 
</xsl:template> 

<xsl:template match="taxation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="orderInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="invoiceReversal"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="invoiceTransferInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="paymentInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="financialPeriodInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="currencyConversionInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 
<!-- end invoiceHeader transformation_1 --> 

<!-- start invoiceDetails transformation_1 --> 
<xsl:template match="invoiceDetails"> 
<invoiceDetails_elements> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="partInformation"/> 
<xsl:apply-templates select="detailAmount"/> 
<xsl:apply-templates select="orderInformation"/> 
</invoiceDetails_elements> 
<xsl:apply-templates select="detailCharges" mode="invoiceDetails"/> 
</xsl:template> 

<xsl:template match="orderInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="partInformation"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 

<xsl:template match="detailAmount"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="taxation" /> 
</xsl:template> 

<xsl:template match="chargeAmount"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="taxation" /> 
</xsl:template> 

<xsl:template match="detailCharges" mode="invoiceDetails"> 
<invoiceDetails_DetailCharges_elements> 
<xsl:apply-templates select="chargeType"/> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="chargeAmount"/> 
<xsl:apply-templates select="parent::node()/orderInformation"/> 
</invoiceDetails_DetailCharges_elements> 
</xsl:template> 

<xsl:template match="chargeType"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 
<!-- end invoiceDetails transformation_1 --> 

<!-- start invoiceCharges transformation_1 --> 
<xsl:template match="invoiceCharges"> 
<invoiceCharges_elements> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
<xsl:apply-templates select="chargeType" mode="invoiceCharges"/> 
<xsl:apply-templates select="chargeAmount"/> 
<xsl:apply-templates select="parent::node()/invoiceHeader/orderInformation"/>	
</invoiceCharges_elements> 
</xsl:template> 

<xsl:template match="chargeType" mode="invoiceCharges"> 
<xsl:for-each select="*[count(./*) = 0]"> 
<xsl:copy-of select="."/> 
</xsl:for-each> 
</xsl:template> 
<!-- end invoiceCharges transformation_1 --> 

<xsl:template name="getFormatedDate"> 
<xsl:param name="date2format"/> 
<xsl:variable name="y" select="substring($date2format, 1, 4)"/> 
<xsl:variable name="m" select="substring($date2format, 6, 2)"/> 
<xsl:variable name="d" select="substring($date2format, 9, 2)"/> 
<xsl:value-of select="concat($y,$m,$d)"/> 
</xsl:template> 

<xsl:template name="getMonthOfDate"> 
<xsl:param name="date2format"/> 
<xsl:variable name="y" select="substring($date2format, 1, 4)"/> 
<xsl:variable name="m" select="substring($date2format, 6, 2)"/> 
<xsl:variable name="d" select="substring($date2format, 9, 2)"/> 
<xsl:value-of select="$m"/> 
</xsl:template> 

<xsl:template name="getYearOfDate"> 
<xsl:param name="date2format"/> 
<xsl:variable name="y" select="substring($date2format, 1, 4)"/> 
<xsl:variable name="m" select="substring($date2format, 6, 2)"/> 
<xsl:variable name="d" select="substring($date2format, 9, 2)"/> 
<xsl:value-of select="$y"/> 
</xsl:template> 

<xsl:template name="dup"> 
<xsl:param name="input"/> 
<xsl:param name="count" select="2"/> 
<xsl:choose> 
<xsl:when test="not($count) or not($input)"/> 
<xsl:when test="$count = 1"> 
<xsl:value-of select="$input"/> 
</xsl:when> 
<xsl:otherwise> 
<!-- If $count is odd append an extra copy of input --> 
<xsl:if test="$count mod 2"> 
<xsl:value-of select="$input"/> 
</xsl:if> 
<!-- Recursively apply template after doubling input and halving count --> 
<xsl:call-template name="dup"> 
<xsl:with-param name="input" select="concat($input,$input)"/> 
<xsl:with-param name="count" select="floor($count div 2)"/> 
</xsl:call-template> 
</xsl:otherwise> 
</xsl:choose> 
</xsl:template> 

<xsl:template name="justify"> 
<xsl:param name="value"/> 
<xsl:param name="filler" select="' '"/> 
<xsl:param name="width" select="10"/> 
<xsl:param name="align" select=" 'left' "/> 
<!-- Truncate if too long --> 
<xsl:variable name="output" select="substring($value,1,$width)"/> 
<xsl:choose> 
<xsl:when test="$align = 'left' "> 
<xsl:value-of select="$output"/> 
<xsl:call-template name="dup"> 
<xsl:with-param name="input" select="$filler"/> 
<xsl:with-param name="count" select="$width - string-length($output)"/> 
</xsl:call-template> 
</xsl:when> 
<xsl:when test="$align = 'right' "> 
<xsl:call-template name="dup"> 
<xsl:with-param name="input" select="$filler"/> 
<xsl:with-param name="count" select="$width - string-length($output)"/> 
</xsl:call-template> 
<xsl:value-of select="$output"/> 
</xsl:when> 
<xsl:when test="$align = 'center' "> 
<xsl:call-template name="dup"> 
<xsl:with-param name="input" select="$filler"/> 
<xsl:with-param name="count" select="floor(($width - string-length($output)) div 2)"/> 
</xsl:call-template> 
<xsl:value-of select="$output"/> 
<xsl:call-template name="dup"> 
<xsl:with-param name="input" select="$filler"/> 
<xsl:with-param name="count" select="ceiling(($width - string-length($output)) div 2)"/> 
</xsl:call-template> 
</xsl:when> 
<xsl:otherwise>INVALID ALIGN</xsl:otherwise> 
</xsl:choose> 
</xsl:template> 

</xsl:stylesheet> 