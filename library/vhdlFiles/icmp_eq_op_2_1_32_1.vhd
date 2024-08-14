-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity icmp_eq_op_2_1_32_1_s is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	dataInArray_1 :  in std_logic_vector(31 downto 0);
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
end icmp_eq_op_2_1_32_1_s;

architecture behavioral of icmp_eq_op_2_1_32_1_s is 

begin

icmp_eq_op_sub: entity work.icmp_eq_op(arch) generic map (2,1,32,1)
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

-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity Buffer_1_1_1_1_s is 
port (
	dataInArray_0 :  in std_logic_vector(0 downto 0);
	pValidArray_0 :  in std_logic;
	readyArray_0 :  out std_logic;
	nReadyArray_0 :  in std_logic;
	validArray_0 :  out std_logic;
	dataOutArray_0 :  out std_logic_vector(0 downto 0);
	clk:  in std_logic;
	rst:  in std_logic
);
end Buffer_1_1_1_1_s;

architecture behavioral of Buffer_1_1_1_1_s is 

begin

elasticBuffer_sub: entity work.elasticBuffer(arch) generic map (1,1,1,1)
port map (
	clk => clk,
	rst => rst,
	dataInArray(0) => dataInArray_0,
	pValidArray(0) => pValidArray_0,
	readyArray(0) => readyArray_0,
	nReadyArray(0) => nReadyArray_0,
	validArray(0) => validArray_0,
	dataOutArray(0) => dataOutArray_0
);

end behavioral; 
-- ==============================================================
-- Generated Automatically by Dot2RapidWright 
-- ==============================================================
library IEEE; 
use IEEE.std_logic_1164.all; 
use IEEE.numeric_std.all; 
use work.customTypes.all; 
-- ==============================================================
entity icmp_eq_op_2_1_32_1 is 
port (
	dataInArray_0 :  in std_logic_vector(31 downto 0);
	dataInArray_1 :  in std_logic_vector(31 downto 0);
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
end icmp_eq_op_2_1_32_1;

architecture behavioral of icmp_eq_op_2_1_32_1 is 
	signal	i_nReadyArray_0 :   std_logic;
	signal	i_validArray_0 :   std_logic;
	signal	i_dataOutArray_0 : std_logic_vector(0 downto 0);
	signal  i_dataInArray_0 :   std_logic_vector(0 downto 0);
	signal  i_pValidArray_0 :   std_logic;
	signal  i_readyArray_0 :    std_logic;

begin
icmp_eq_op_2_1_32_1_s: entity work.icmp_eq_op_2_1_32_1_s(behavioral)
port map (
	clk => clk,
	rst => rst,
	dataInArray_0 => dataInArray_0,
	dataInArray_1 => dataInArray_1,
	pValidArray_0 => pValidArray_0,
	pValidArray_1 => pValidArray_1,
	readyArray_0 => readyArray_0,
	readyArray_1 => readyArray_1,
	nReadyArray_0 => i_nReadyArray_0,
	validArray_0 => i_validArray_0,
	dataOutArray_0 => i_dataOutArray_0
);
Buffer_1_1_1_1_s: entity work.Buffer_1_1_1_1_s(behavioral) 
port map (
	clk => clk,
	rst => rst,
	dataInArray_0 => i_dataInArray_0,
	pValidArray_0 => i_pValidArray_0,
	readyArray_0 => i_readyArray_0,
	nReadyArray_0 => nReadyArray_0,
	validArray_0 => validArray_0,
	dataOutArray_0 => dataOutArray_0
);

	i_nReadyArray_0 <= i_readyArray_0;
	i_validArray_0 <= i_pValidArray_0;
	i_dataOutArray_0 <= i_dataInArray_0;
end behavioral; 
