-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity getelementptr_op_7_1_32_32_00000000000000000000000000000011 is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	dataInArray_1 :  in std_logic_vector(31 downto 0);
	dataInArray_2 :  in std_logic_vector(31 downto 0);
	dataInArray_3 :  in std_logic_vector(31 downto 0);
	dataInArray_4 :  in std_logic_vector(31 downto 0);
	dataInArray_5 :  in std_logic_vector(31 downto 0);
	dataInArray_6 :  in std_logic_vector(31 downto 0);
	pValidArray_0 :  in std_logic;
	pValidArray_1 :  in std_logic;
	pValidArray_2 :  in std_logic;
	pValidArray_3 :  in std_logic;
	pValidArray_4 :  in std_logic;
	pValidArray_5 :  in std_logic;
	pValidArray_6 :  in std_logic;
	readyArray_0 :  out std_logic;
	readyArray_1 :  out std_logic;
	readyArray_2 :  out std_logic;
	readyArray_3 :  out std_logic;
	readyArray_4 :  out std_logic;
	readyArray_5 :  out std_logic;
	readyArray_6 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(31 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end getelementptr_op_7_1_32_32_00000000000000000000000000000011;

architecture behavioral of getelementptr_op_7_1_32_32_00000000000000000000000000000011 is 

begin

getelementptr_op_sub: entity work.getelementptr_op(arch) generic map (7,1,32,32,3)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_0,
	dataInArray(1) => dataInArray_1,
	dataInArray(2) => dataInArray_2,
	dataInArray(3) => dataInArray_3,
	dataInArray(4) => dataInArray_4,
	dataInArray(5) => dataInArray_5,
	dataInArray(6) => dataInArray_6,
	pValidArray(0) => pValidArray_0,
	pValidArray(1) => pValidArray_1,
	pValidArray(2) => pValidArray_2,
	pValidArray(3) => pValidArray_3,
	pValidArray(4) => pValidArray_4,
	pValidArray(5) => pValidArray_5,
	pValidArray(6) => pValidArray_6,
	readyArray(0) => readyArray_0,
	readyArray(1) => readyArray_1,
	readyArray(2) => readyArray_2,
	readyArray(3) => readyArray_3,
	readyArray(4) => readyArray_4,
	readyArray(5) => readyArray_5,
	readyArray(6) => readyArray_6,
	nReadyArray(0) => nReadyArray_0,
	validArray(0) => validArray_0,
	dataOutArray(0) => dataOutArray_0
);

end behavioral; 
