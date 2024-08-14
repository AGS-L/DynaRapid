-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Fork_1_6_1_1 is 
port (
	dataInArray_0 :  in std_logic_vector(0 downto 0);
	pValidArray_0 :  in std_logic;
	readyArray_0 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(0 downto 0);
	nReadyArray_1 :  in std_logic;
	validArray_1 :  out std_logic;
	dataOutArray_1 :  out std_logic_vector(0 downto 0);
	nReadyArray_2 :  in std_logic;
	validArray_2 :  out std_logic;
	dataOutArray_2 :  out std_logic_vector(0 downto 0);
	nReadyArray_3 :  in std_logic;
	validArray_3 :  out std_logic;
	dataOutArray_3 :  out std_logic_vector(0 downto 0);
	nReadyArray_4 :  in std_logic;
	validArray_4 :  out std_logic;
	dataOutArray_4 :  out std_logic_vector(0 downto 0);
	nReadyArray_5 :  in std_logic;
	validArray_5 :  out std_logic;
	dataOutArray_5 :  out std_logic_vector(0 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end Fork_1_6_1_1;

architecture behavioral of Fork_1_6_1_1 is 

begin

fork_sub: entity work.fork(arch) generic map (1,6,1,1)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_0,
	pValidArray(0) => pValidArray_0,
	readyArray(0) => readyArray_0,
	nReadyArray(0) => nReadyArray_0,
	nReadyArray(1) => nReadyArray_1,
	nReadyArray(2) => nReadyArray_2,
	nReadyArray(3) => nReadyArray_3,
	nReadyArray(4) => nReadyArray_4,
	nReadyArray(5) => nReadyArray_5,
	validArray(0) => validArray_0,
	validArray(1) => validArray_1,
	validArray(2) => validArray_2,
	validArray(3) => validArray_3,
	validArray(4) => validArray_4,
	validArray(5) => validArray_5,
	dataOutArray(0) => dataOutArray_0,
	dataOutArray(1) => dataOutArray_1,
	dataOutArray(2) => dataOutArray_2,
	dataOutArray(3) => dataOutArray_3,
	dataOutArray(4) => dataOutArray_4,
	dataOutArray(5) => dataOutArray_5
);

end behavioral; 
