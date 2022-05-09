# TrjPrivacy
TKDE'22: A Survey and Experimental Study on Privacy-Preserving Trajectory Data Publishing (accepted by TKDE in May 2022)

## Compared Models
- Formal models
  - k-anonymity models: W4M [1], GLOVE [2], KLT [3]
  - differential privacy models: DPT [4], AdaTrace [5]
- Ad-hoc models
  - mixzone [6]
  - dummy [7]

## Source Codes
We would like to thank some authors for kindly providing the publicly-available runable source code:
- [W4M](https://kdd.isti.cnr.it/W4M/)
- [AdaTrace](https://github.com/git-disl/AdaTrace)
- DPT: we obtained it from the authors but don't have the right to make it public here. 
- GLOVE, KLT, mixzone, dummy: implemented by ourselves in Java and provided in this repository.

## Environment
Tested in CentOS Linux and MacOS Monterey (jdk 17.0.1)

## Datasets
[T-Drive](https://www.microsoft.com/en-us/research/publication/t-drive-trajectory-data-sample/) and [Geolife](https://www.microsoft.com/en-us/download/details.aspx?id=52367) are publicly available datasets provided by Microsoft.
A small subset of T-Drive is provided in this project for testing.

## Project Structure (to be revised later)
    Testing/                                  
    ├── RoadNetworkInfo/NNid2lnglat.csv        -- the road network information used for simple map-matching as the data preprosseing
    ├── TestData-tdrive/                       -- T-Drive dataset including 600 objects' one-week trajectory
                                                  Note that some objects could be discarded due to too few data points
    src/                                      
    ├── resources/config.properties            -- the program will read parameters here (modify it according to your requirements)
    ├── main/Main.java                         -- the entry of the program
    ├── 
    
    libs                                       -- some external library packages that are necessary to be included in the program
    ...                 

## Input Parameters (to be revised later)

    RoadNetworkFile = ./Testing/RoadNetworkInfo/NNid2lnglat.csv         -- fixed, used for the simple map-matching initially
    data_FolderInput = ./Testing/TestData-tdrive/                       -- the input data to be protected
    data_FolderOutput = ./Testing/outputs/                              -- the output folder, any log file or anonymized trajectries will be here
    ...

## References
[1] O. Abul, F. Bonchi, and M. Nanni, “Anonymization of moving objects databases by clustering and perturbation,” Inf. Syst., vol. 35, no. 8, pp. 884–910, 2010.

[2] M. Gramaglia and M. Fiore, “Hiding mobile traffic fingerprints with GLOVE,” in Proc. CoNEXT. ACM, 2015, pp. 26:1–26:13.

[3] Z. Tu, K. Zhao, F. Xu, Y. Li, L. Su, and D. Jin, “Protecting trajectory from semantic attack considering k-anonymity, l-diversity, and t-closeness,” IEEE Trans. Netw. and Service Management, vol. 16, no. 1, pp. 264–278, 2019

[4] X. He, G. Cormode, A. Machanavajjhala, C. M. Procopiuc, and D. Srivastava, “DPT: differentially private trajectory synthesis using hierarchical reference systems,” Proc. VLDB, vol. 8, no. 11, pp. 1154–1165, 2015.

[5] M. E. Gursoy, L. Liu, S. Truex, L. Yu, and W. Wei, “Utility-aware synthesis of differentially private and attack-resilient location traces,” in Proc. SIGSAC. ACM, 2018, pp. 196–211

[6] X. Liu, H. Zhao, M. Pan, H. Yue, X. Li, and Y. Fang, “Traffic-aware multiple mix zone placement for protecting location privacy,” in Proc. INFOCOM. IEEE, 2012, pp. 972–980.

[7] X. Liu, J. Chen, X. Xia, C. Zong, R. Zhu, and J. Li, “Dummy-based trajectory privacy protection against exposure location attacks,” in Proc. WISA, ser. Lecture Notes in Computer Science, vol. 11817. Springer, 2019, pp. 368–381

## Citation

If you find our library or the experimental results useful, please kindly cite the following paper:
```
@article{jin2022tkde,
  title={A Survey and Experimental Study on Privacy-Preserving Trajectory Data Publishing},
  author={Jin, Fengmei and Hua, Wen and Francia, Matteo and Chao, Pingfu and Orowska, Maria and Zhou, Xiaofang},
  journal={IEEE Transactions on Knowledge and Data Engineering},
  year={2022}
}
```

Please feel free to contact fengmei.jin@uq.edu.au if encountering any unexpected issues in this project.
