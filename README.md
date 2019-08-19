# BFA
Incremental Control Verification Based on ARC

The project is based on ARC[1] and Batfish[2].

The main file: newDriver.java.

The input can be as follows:

-var 0 -configs ./configs/examples/batfish-nsdi

-var 0 -configs ./configs/examples/batfish-nsdi2

The BFA algorithm is in the package edu.tsinghua.lyf

[1]Fogel, A., Fung, S., Pedrosa, L., Walraed-Sullivan, M., Govindan, R., Mahajan, R., & Millstein, T. D. (2015, May). A General Approach to Network Configuration Analysis. In NSDI(pp. 469-483).

[2]Gember-Jacobson, A., Viswanathan, R., Akella, A., & Mahajan, R. (2016, August). Fast control plane analysis using an abstract representation. In Proceedings of the 2016 ACM SIGCOMM Conference (pp. 300-313). ACM.
