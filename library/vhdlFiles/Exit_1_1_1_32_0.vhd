-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Exit_1_1_1_32_0 is 
port (
	dataInArray_0 :  in std_logic_vector(0 downto 0);
	dataInArray_1 :  in std_logic_vector(0 downto 0);
	pValidArray_0 :  in std_logic;
	pValidArray_1 :  in std_logic;
	readyArray_0 :  out std_logic;
	readyArray_1 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(0 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end Exit_1_1_1_32_0;

architecture behavioral of Exit_1_1_1_32_0 is 

begin

end_node_sub: entity work.end_node(arch) generic map (1,1,1,32,1)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => "0",
	eValidArray(0) => pValidArray_0,
	pValidArray(0) => pValidArray_1,
	eReadyArray(0) => readyArray_0,
	readyArray(0) => readyArray_1,
	dataOutArray(0) => dataOutArray_0,
	validArray(0) => validArray_0,
	nReadyArray(0) => nReadyArray_0
);

end behavioral; 
