MAIN = myCPU.MyTopLevelVerilog
BUILD = ./build
GEN = $(BUILD)/gen
GEN_TOP = $(GEN)/MyCPU.v
RELEASE_PACK = /mnt/d/coding/LA/nscscc-team-la32r
REG_INIT_FILE = MyCPU.v_toplevel_defaultClockArea_cpu_RegFilePlugin_regFile.bin
ABSOLUTE_PATH = d:\/coding\/LA\/nscscc-team-la32r\/func_test\/myCPU
verilog: 
	sbt "runMain $(MAIN)"
	sed -i "s/$(REG_INIT_FILE)/$(ABSOLUTE_PATH)\/$(REG_INIT_FILE)/g" $(GEN_TOP)
	sudo cp $(GEN_TOP) $(RELEASE_PACK)/func_test/myCPU