-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Mux_3_1_32_32_1 is 
port (
	dataInArray_0 :  in std_logic_vector(0 downto 0);
	dataInArray_1 :  in std_logic_vector(31 downto 0);
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
end Mux_3_1_32_32_1;

architecture behavioral of Mux_3_1_32_32_1 is 

begin

Mux_sub: entity work.Mux(arch) generic map (3,1,32,32,1)
port map (
	clk => clk,
	rst => rst,
	Condition(0) => dataInArray_0,
	dataInArray(0) => dataInArray_1,
	dataInArray(1) => dataInArray_2,
	pValidArray(0) => pValidArray_0,
	pValidArray(1) => pValidArray_1,
	pValidArray(2) => pValidArray_2,
	readyArray(0) => readyArray_0,
	readyArray(1) => readyArray_1,
	readyArray(2) => readyArray_2,
	nReadyArray(0) => nReadyArray_0,
	validArray(0) => validArray_0,
	dataOutArray(0) => dataOutArray_0
);

end behavioral; 
