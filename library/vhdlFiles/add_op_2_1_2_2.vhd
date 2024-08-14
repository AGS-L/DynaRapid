-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity add_op_2_1_2_2 is 
port (
	dataInArray_0 :  in std_logic_vector(1 downto 0);
	dataInArray_1 :  in std_logic_vector(1 downto 0);
	pValidArray_0 :  in std_logic;
	pValidArray_1 :  in std_logic;
	readyArray_0 :  out std_logic;
	readyArray_1 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(1 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end add_op_2_1_2_2;

architecture behavioral of add_op_2_1_2_2 is 

begin

add_op_sub: entity work.add_op(arch) generic map (2,1,2,2)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_0,
	dataInArray(1) => dataInArray_1,
	pValidArray(0) => pValidArray_0,
	pValidArray(1) => pValidArray_1,
	readyArray(0) => readyArray_0,
	readyArray(1) => readyArray_1,
	nReadyArray(0) => nReadyArray_0,
	validArray(0) => validArray_0,
	dataOutArray(0) => dataOutArray_0
);

end behavioral; 
