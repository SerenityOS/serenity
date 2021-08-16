/* /nodynamiccopyright/ */
// combinations of methods defined in a base class
// and overridden in subtypes

// class should compile with no warnings

class P {
    /** @deprecated */ public void pDep_qDep_rDep() { }
    /** @deprecated */ public void pDep_qDep_rUnd() { }
    /** @deprecated */ public void pDep_qDep_rInh() { }
    /** @deprecated */ public void pDep_qUnd_rDep() { }
    /** @deprecated */ public void pDep_qUnd_rUnd() { }
    /** @deprecated */ public void pDep_qUnd_rInh() { }
    /** @deprecated */ public void pDep_qInh_rDep() { }
    /** @deprecated */ public void pDep_qInh_rUnd() { }
    /** @deprecated */ public void pDep_qInh_rInh() { }
                       public void pUnd_qDep_rDep() { }
                       public void pUnd_qDep_rUnd() { }
                       public void pUnd_qDep_rInh() { }
                       public void pUnd_qUnd_rDep() { }
                       public void pUnd_qUnd_rUnd() { }
                       public void pUnd_qUnd_rInh() { }
                       public void pUnd_qInh_rDep() { }
                       public void pUnd_qInh_rUnd() { }
                       public void pUnd_qInh_rInh() { }
}
