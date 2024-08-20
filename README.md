# HFUT-NSCSCC
第八届龙芯杯个人赛二等奖作品，整体架构图如下，本仓库中包含了MyCPU部分的所有实现，Confreg部分代码请见`verilog/src`目录。

![image-20240820202035366](https://jiunian-pic-1310185536.cos.ap-nanjing.myqcloud.com/image-20240820202035366.png)

处理器架构为LA32R，顺序单发射，八级流水线，包含ICache和简易分支预测功能（由于DCache对于性能测试来说是负优化，本项目中不添加），处理器核的简易架构图如下：

![image-20240820202225359](https://jiunian-pic-1310185536.cos.ap-nanjing.myqcloud.com/image-20240820202225359.png)

使用SpinalHDL进行构建，利用pipeline库，实现快速的流水线构建，参考自VexRiscv和NOP-Core项目。

尝试过的最高运行频率221MHz，性能测试的时间分别为：0.043s、0.069s和0.152s，总时间0.264s。<img src="https://jiunian-pic-1310185536.cos.ap-nanjing.myqcloud.com/image-20240820202238610.png" alt="image-20240820202238610" style="zoom:50%;" />

## 生成verilog
在项目目录下执行命令`make archive`即可在`/build/gen`目录下生成`MyCPU.v`文件，将其移动到个人赛指定的项目中即可。

项目中需要使用的到的ip核包括Multiplier和Divider，用于实现乘法器和除法器，具体配置在`/verilog/ip`目录下



## 项目的不足

1. 架构设计的不合理，没有很好的做到流水线的平衡；
2. 没有添加双发射（时间不够）, 导致发射阶段似乎没有太大的用处；



## 参考项目

- [SpinalHDL/VexRiscv: A FPGA friendly 32 bit RISC-V CPU implementation (github.com)](https://github.com/SpinalHDL/VexRiscv)
- [NOP-Processor/NOP-Core: High performance LA32R out-of-order processor core. (NSCSCC 2023 Special Prize) (github.com)](https://github.com/NOP-Processor/NOP-Core)

- [fluctlight001/cpu_for_nscscc2022_single: 2022年龙芯杯个人赛 单发射110M（含icache） (github.com)](https://github.com/fluctlight001/cpu_for_nscscc2022_single/tree/main)