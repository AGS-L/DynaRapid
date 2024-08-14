-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Source_0_1_32_1 is 
port (
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(0 downto 0);
	useless_net :  out std_logic;
	clk:  in std_logic;
	rst:  in std_logic
);
end Source_0_1_32_1;

architecture behavioral of Source_0_1_32_1 is 

begin

source_sub: entity work.source(arch) generic map (0,1,32,1)
port map (
	clk => clk,
	rst => rst,
	nReadyArray(0) => nReadyArray_0,
	validArray(0) => validArray_0,
	dataOutArray(0) => dataOutArray_0
);
	useless_net <= not rst;

end behavioral; 
