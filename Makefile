MAIN = myCPU.MyTopLevelVerilog
BUILD = ./build
GEN = $(BUILD)/gen
GEN_TOP = $(GEN)/MyCPU.v
REG_INIT_FILE = MyCPU.v_toplevel_defaultClockArea_cpu_RegFilePlugin_regFile.bin
ABSOLUTE_PATH = $(subst /,\/,$(TARGET_PATH))
verilog: 
	sbt "runMain $(MAIN)"
	sed -i "s/$(REG_INIT_FILE)/$(ABSOLUTE_PATH)\/func_test\/myCPU\/$(REG_INIT_FILE)/g" $(GEN_TOP)
	sudo cp $(GEN_TOP) $(RELEASE_PACK)/func_test/myCPU