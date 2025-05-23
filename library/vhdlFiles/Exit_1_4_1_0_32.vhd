-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Exit_1_4_1_0_32 is 
port (
	dataInArray_0 :  in std_logic_vector(0 downto 0);
	dataInArray_1 :  in std_logic_vector(0 downto 0);
	dataInArray_2 :  in std_logic_vector(0 downto 0);
	dataInArray_3 :  in std_logic_vector(0 downto 0);
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
	clk:  in std_logic;
	rst:  in std_logic
);
end Exit_1_4_1_0_32;

architecture behavioral of Exit_1_4_1_0_32 is 

begin

end_node_sub: entity work.end_node(arch) generic map (1,4,1,0,32)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_4,
	eValidArray(0) => pValidArray_0,
	eValidArray(1) => pValidArray_1,
	eValidArray(2) => pValidArray_2,
	eValidArray(3) => pValidArray_3,
	pValidArray(0) => pValidArray_4,
	eReadyArray(0) => readyArray_0,
	eReadyArray(1) => readyArray_1,
	eReadyArray(2) => readyArray_2,
	eReadyArray(3) => readyArray_3,
	readyArray(0) => readyArray_4,
	dataOutArray(0) => dataOutArray_0,
	validArray(0) => validArray_0,
	nReadyArray(0) => nReadyArray_0
);

end behavioral; 
