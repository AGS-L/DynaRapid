-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Fork_1_3_32_32 is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	pValidArray_0 :  in std_logic;
	readyArray_0 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(31 downto 0);
	nReadyArray_1 :  in std_logic;
	validArray_1 :  out std_logic;
	dataOutArray_1 :  out std_logic_vector(31 downto 0);
	nReadyArray_2 :  in std_logic;
	validArray_2 :  out std_logic;
	dataOutArray_2 :  out std_logic_vector(31 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end Fork_1_3_32_32;

architecture behavioral of Fork_1_3_32_32 is 

begin

fork_sub: entity work.fork(arch) generic map (1,3,32,32)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_0,
	pValidArray(0) => pValidArray_0,
	readyArray(0) => readyArray_0,
	nReadyArray(0) => nReadyArray_0,
	nReadyArray(1) => nReadyArray_1,
	nReadyArray(2) => nReadyArray_2,
	validArray(0) => validArray_0,
	validArray(1) => validArray_1,
	validArray(2) => validArray_2,
	dataOutArray(0) => dataOutArray_0,
	dataOutArray(1) => dataOutArray_1,
	dataOutArray(2) => dataOutArray_2
);

end behavioral; 
