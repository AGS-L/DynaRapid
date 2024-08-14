-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Sink_1_0_32_32 is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	pValidArray_0 :  in std_logic;
	readyArray_0 :  out std_logic;
	clk:  in std_logic;
	rst:  in std_logic
);
end Sink_1_0_32_32;

architecture behavioral of Sink_1_0_32_32 is 

begin

sink_sub: entity work.sink(arch) generic map (1,0,32,32)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_0,
	pValidArray(0) => pValidArray_0,
	readyArray(0) => readyArray_0
);

end behavioral; 
