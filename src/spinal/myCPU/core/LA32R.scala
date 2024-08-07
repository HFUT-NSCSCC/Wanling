package myCPU.core
import spinal.core._

object LA32R {
    def PCWidth = 32
    def PC_INIT:Long = 0x80000000L

    def CYCLES_TO_WRITE = 3
    def CYCLES_TO_READ = 2

    def InstWidth = 32

    def NR_REG = 32
    def RegAddrWidth = log2Up(NR_REG)

    def DataWidth = 32
    def AddrWidth = 32
    def rdRange = 4 downto 0
    def rjRange = 9 downto 5
    def rkRange = 14 downto 10
    def raRange = 19 downto 15

    // def useCache = true

    private def MM(mask: String): MaskedLiteral = {
        val replace_mask = mask.replace(" ", "")
        assert(replace_mask.length == 32)
        MaskedLiteral(replace_mask)
    }

    def NONE      = MM("00000000000000000 00000 00000 00000")

    // 3R-type
    def ADD       = MM("00000000000100000 ----- ----- -----")
    def SUB       = MM("00000000000100010 ----- ----- -----")
    def SLT       = MM("00000000000100100 ----- ----- -----")
    def SLTU      = MM("00000000000100101 ----- ----- -----")
    def NOR       = MM("00000000000101000 ----- ----- -----")
    def AND       = MM("00000000000101001 ----- ----- -----")
    def OR        = MM("00000000000101010 ----- ----- -----")
    def XOR       = MM("00000000000101011 ----- ----- -----")
    def SLL       = MM("00000000000101110 ----- ----- -----")
    def SRL       = MM("00000000000101111 ----- ----- -----")
    def SRA       = MM("00000000000110000 ----- ----- -----")
    def MUL       = MM("00000000000111000 ----- ----- -----")
    
    // 2RI8-type 
    def SLLI      = MM("00000000010000 -------- ----- -----")
    def SRLI      = MM("00000000010001 -------- ----- -----")
    def SRAI      = MM("00000000010010 -------- ----- -----")

    // 2RI12-type
    def SLTI      = MM("0000001000 ------------ ----- -----")
    def SLTUI     = MM("0000001001 ------------ ----- -----")
    def ADDI      = MM("0000001010 ------------ ----- -----")
    def ANDI      = MM("0000001101 ------------ ----- -----")
    def ORI       = MM("0000001110 ------------ ----- -----")
    def XORI      = MM("0000001111 ------------ ----- -----")

    // 2RI12-type
    def LDB       = MM("0010100000 ------------ ----- -----")
    def LDH       = MM("0010100001 ------------ ----- -----")
    def LDW       = MM("0010100010 ------------ ----- -----")
    def STB       = MM("0010100100 ------------ ----- -----")
    def STH       = MM("0010100101 ------------ ----- -----")
    def STW       = MM("0010100110 ------------ ----- -----")
    def LDBU      = MM("0010101000 ------------ ----- -----")
    def LDHU      = MM("0010101001 ------------ ----- -----")

    // 2RI16-type
    def JIRL      = MM("010011 ---------------- ----- -----")
    def BEQ       = MM("010110 ---------------- ----- -----")
    def BNE       = MM("010111 ---------------- ----- -----")
    def BLT       = MM("011000 ---------------- ----- -----")
    def BGE       = MM("011001 ---------------- ----- -----")
    def BLTU      = MM("011010 ---------------- ----- -----")
    def BGEU      = MM("011011 ---------------- ----- -----")

    // 1R20I-type
    def LU12I     = MM("0001010 -------------------- -----")
    def PCADDU12I = MM("0001110 -------------------- -----")

    // I26-type
    def LA_B         = MM("010100 ---------------- ----------") // 避免与B(bit)混淆
    def BL        = MM("010101 ---------------- ----------")
}
