-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity CntrlMerge_2_2_0_0_1 is 
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
	nReadyArray_1 :  in std_logic;
	validArray_1 :  out std_logic;
	dataOutArray_1 :  out std_logic_vector(0 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end CntrlMerge_2_2_0_0_1;

architecture behavioral of CntrlMerge_2_2_0_0_1 is 

begin

CntrlMerge_sub: entity work.CntrlMerge(arch) generic map (2,2,1,1,1)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => "0",
	dataInArray(1) => "0",
	pValidArray(0) => pValidArray_0,
	pValidArray(1) => pValidArray_1,
	readyArray(0) => readyArray_0,
	readyArray(1) => readyArray_1,
	nReadyArray(0) => nReadyArray_0,
	nReadyArray(1) => nReadyArray_1,
	validArray(0) => validArray_0,
	validArray(1) => validArray_1,
	dataOutArray(0) => dataOutArray_0,
	Condition(0) => dataOutArray_1
);

end behavioral; 
