-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity MC_32_32_1_2_1 is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	dataInArray_1 :  in std_logic_vector(31 downto 0);
	dataInArray_2 :  in std_logic_vector(31 downto 0);
	dataInArray_3 :  in std_logic_vector(31 downto 0);
	dataInArray_4 :  in std_logic_vector(31 downto 0);
	pValidArray_0 :  in std_logic;
	pValidArray_1 :  in std_logic;
	pValidArray_2 :  in std_logic;
	pValidArray_3 :  in std_logic;
	pValidArray_4 :  in std_logic;
	readyArray_0 :  out std_logic;
	readyArray_1 :  out std_logic;
	readyArray_2 :  out std_logic;
	readyArray_3 :  out std_logic;
	readyArray_4 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(31 downto 0);
	nReadyArray_1 :  in std_logic;
	validArray_1 :  out std_logic;
	dataOutArray_1 :  out std_logic_vector(31 downto 0);
	nReadyArray_2 :  in std_logic;
	validArray_2 :  out std_logic;
	dataOutArray_2 :  out std_logic_vector(0 downto 0);
	we0_ce0 :  out std_logic;

	dout0    : out std_logic_vector(31 downto 0);
	address0    : out std_logic_vector(31 downto 0);
	din1 : in std_logic_vector(31 downto 0);
	address1 : out std_logic_vector(31 downto 0);
	ce1 : out std_logic;

	clk:  in std_logic;
	rst:  in std_logic
);
end MC_32_32_1_2_1;

architecture behavioral of MC_32_32_1_2_1 is 

	signal addr0: std_logic_vector(31 downto 0); 
	signal addr1: std_logic_vector(31 downto 0); 

begin

MemCont_sub: entity work.MemCont(arch) generic map (32,32,1,2,1)
port map (
	clk => clk,
	rst => rst,

	io_bbReadyToPrevs(0) => readyArray_1,
	io_bbpValids(0) => '0',
	io_bb_stCountArray(0) => x"00000000",
	io_rdPortsPrev_ready(0) => readyArray_0,
	io_rdPortsPrev_valid(0) => pValidArray_0,
	io_rdPortsPrev_bits(0) => dataInArray_0,
	io_wrAddrPorts_ready(0) => readyArray_2,
	io_wrAddrPorts_valid(0) => pValidArray_2,
	io_wrAddrPorts_bits(0) => dataInArray_2,
	io_wrDataPorts_ready(0) => readyArray_3,
	io_wrDataPorts_valid(0) => pValidArray_3,
	io_wrDataPorts_bits(0) => dataInArray_3,
	io_rdPortsNext_ready(0) => nReadyArray_0,
	io_rdPortsNext_valid(0) => validArray_0,
	io_rdPortsNext_bits(0) => dataOutArray_0,
	io_Empty_Valid => validArray_1,
	io_Empty_Ready => nReadyArray_1,

	io_storeDataOut => dout0,
	io_storeAddrOut => addr0,
	io_storeEnable => we0_ce0,
	io_loadDataIn => din1,
	io_loadAddrOut => addr1,
	io_loadEnable => ce1

);
address0 <= std_logic_vector(resize(unsigned(addr0), address0'length));
address1 <= std_logic_vector(resize(unsigned(addr1), address1'length));

end behavioral; 
