-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Exit_1_2_1_0_32 is 
port (
	dataInArray_0 :  in std_logic_vector(0 downto 0);
	dataInArray_1 :  in std_logic_vector(0 downto 0);
	dataInArray_2 :  in std_logic_vector(31 downto 0);
	pValidArray_0 :  in std_logic;
	pValidArray_1 :  in std_logic;
	pValidArray_2 :  in std_logic;
	readyArray_0 :  out std_logic;
	readyArray_1 :  out std_logic;
	readyArray_2 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(31 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end Exit_1_2_1_0_32;

architecture behavioral of Exit_1_2_1_0_32 is 

begin

end_node_sub: entity work.end_node(arch) generic map (1,2,1,0,32)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_2,
	eValidArray(0) => pValidArray_0,
	eValidArray(1) => pValidArray_1,
	pValidArray(0) => pValidArray_2,
	eReadyArray(0) => readyArray_0,
	eReadyArray(1) => readyArray_1,
	readyArray(0) => readyArray_2,
	dataOutArray(0) => dataOutArray_0,
	validArray(0) => validArray_0,
	nReadyArray(0) => nReadyArray_0
);

end behavioral; 
