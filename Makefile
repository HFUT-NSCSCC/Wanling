MAIN = myCPU.MyTopLevelVerilog
BUILD = ./build
GEN = $(BUILD)/gen
GEN_TOP = $(GEN)/MyCPU.v
REG_INIT_FILE = MyCPU.v_toplevel_defaultClockArea_cpu_RegFilePlugin_regFile.bin
ABSOLUTE_PATH = $(subst /,\/,$(TARGET_PATH))
veri: 
	sbt "runMain $(MAIN)"
	# sed -i "s/$(REG_INIT_FILE)/$(ABSOLUTE_PATH)\/thinpad_top.srcs\/sources_1\/new\/myCPU\/$(REG_INIT_FILE)/g" $(GEN_TOP)
	python3 ./replace.py
	# 复制寄存器初始化文件
	# sudo cp $(GEN)/$(REG_INIT_FILE) $(RELEASE_PACK)/thinpad_top.srcs/sources_1/new/myCPU
	# 复制生成的顶层文件
	sudo cp $(GEN_TOP) $(RELEASE_PACK)/thinpad_top.srcs/sources_1/new/myCPU

archive: 
	sbt "runMain $(MAIN)"
	python3 ./replace.py