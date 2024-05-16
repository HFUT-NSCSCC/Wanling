# HFUT-NSCSCC
## 生成verilog
生成verilog代码并且将生成的代码移动到发布包中的指定目录, 首先需要配置好环境变量
在`.bashrc`或`.zshrc`(取决于你所使用的shell)中添加如下指令: 
```shell
export RELEASE_PACK=<发布包的绝对路径>
export TARGET_PATH=<发布包的路径>
```
以上两个变量用于不同的目的, 也可以一致, 第一个用于拷贝文件到发布包中, 第二个变量用于替换生成的verilog文件中的文本(这些文本被`readmem`类型的函数所使用, 所以有可能不同, 特别是在`wsl`环境下使用)
然后直接在该目录下运行`make verilog`即可生成代码