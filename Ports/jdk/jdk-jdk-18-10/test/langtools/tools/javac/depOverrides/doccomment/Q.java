/* /nodynamiccopyright/ */
// combinations of methods defined in a base class
// and overridden in subtypes

// class should compile with warnings as shown

class Q extends P {
    /** @deprecated */ public void pDep_qDep_rDep() { }
    /** @deprecated */ public void pDep_qDep_rUnd() { }
    /** @deprecated */ public void pDep_qDep_rInh() { }
                       public void pDep_qUnd_rDep() { } // warn
                       public void pDep_qUnd_rUnd() { } // warn
                       public void pDep_qUnd_rInh() { } // warn
    //                 public void pDep_qInh_rDep() { }
    //                 public void pDep_qInh_rUnd() { }
    //                 public void pDep_qInh_rInh() { }
    /** @deprecated */ public void pUnd_qDep_rDep() { }
    /** @deprecated */ public void pUnd_qDep_rUnd() { }
    /** @deprecated */ public void pUnd_qDep_rInh() { }
                       public void pUnd_qUnd_rDep() { }
                       public void pUnd_qUnd_rUnd() { }
                       public void pUnd_qUnd_rInh() { }
    //                 public void pUnd_qInh_rDep() { }
    //                 public void pUnd_qInh_rUnd() { }
    //                 public void pUnd_qInh_rInh() { }
}
