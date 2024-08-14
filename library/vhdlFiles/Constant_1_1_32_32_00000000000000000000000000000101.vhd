-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Constant_1_1_32_32_00000000000000000000000000000101 is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	pValidArray_0 :  in std_logic;
	readyArray_0 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(31 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end Constant_1_1_32_32_00000000000000000000000000000101;

architecture behavioral of Constant_1_1_32_32_00000000000000000000000000000101 is 

begin

Const_sub: entity work.Const(arch) generic map (1,1,32,32)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => "00000000000000000000000000000101",
	pValidArray(0) => pValidArray_0,
	readyArray(0) => readyArray_0,
	nReadyArray(0) => nReadyArray_0,
	validArray(0) => validArray_0,
	dataOutArray(0) => dataOutArray_0
);

end behavioral; 
